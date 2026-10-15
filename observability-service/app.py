"""Operational trace store. Only identifiers, status and decision metadata, never email bodies."""
import asyncio
import json
import logging
import os
import threading
import time
from contextlib import asynccontextmanager
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

import jwt
import pika
import psycopg
from fastapi import Depends, FastAPI, Header, HTTPException
from psycopg.rows import dict_row

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("observability")
DB = os.environ["DB_URL_PYTHON"]
SECRET = os.environ["APP_JWT_SECRET"]
TIMEZONE = os.getenv("REPORT_TIMEZONE", "America/Lima")
BROKER = os.getenv("RABBITMQ_HOST", "rabbitmq")
USER = os.getenv("RABBITMQ_USER", "router")
PASSWORD = os.environ["RABBITMQ_PASSWORD"]

SCHEMA = """
CREATE SCHEMA IF NOT EXISTS observability;
CREATE TABLE IF NOT EXISTS observability.request_trace (
 request_id VARCHAR(100) PRIMARY KEY,
 received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 profile_code VARCHAR(80),
 source VARCHAR(40) NOT NULL DEFAULT 'api',
 state VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
 classified_at TIMESTAMPTZ,
 deterministic_count INTEGER NOT NULL DEFAULT 0,
 ai_count INTEGER NOT NULL DEFAULT 0,
 rejected_count INTEGER NOT NULL DEFAULT 0,
 requires_review BOOLEAN NOT NULL DEFAULT FALSE,
 decisions JSONB NOT NULL DEFAULT '{}'::jsonb,
 crm_external_id VARCHAR(120),
 crm_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_trace_received ON observability.request_trace(received_at DESC);
"""

def database():
    return psycopg.connect(DB, row_factory=dict_row, connect_timeout=10)

def init_db():
    with database() as conn:
        conn.execute(SCHEMA)

def write_event(kind: str, event: dict):
    request_id = str(event.get("requestId") or "")
    if not request_id or len(request_id) > 100:
        log.warning("Ignoring event without valid requestId")
        return
    profile = event.get("profileCode")
    with database() as conn:
        if kind == "email.received":
            meta = event.get("metadata") or {}
            source = str(meta.get("source") or "api")[:40]
            conn.execute("""
                INSERT INTO observability.request_trace(request_id, profile_code, source)
                VALUES (%s,%s,%s)
                ON CONFLICT (request_id) DO UPDATE SET
                 profile_code=COALESCE(observability.request_trace.profile_code,EXCLUDED.profile_code),
                 source=EXCLUDED.source
            """, (request_id, profile, source))
        elif kind == "ticket.standardized":
            decisions = event.get("decisions") or {}
            values = {
                key: {"value": item.get("value"), "source": item.get("source"),
                      "accepted": bool(item.get("accepted")),
                      "confidence": item.get("probability")}
                for key, item in decisions.items() if isinstance(item, dict)
            }
            rules = sum(x["source"] == "RULE" for x in values.values())
            ais = sum(x["source"] == "AI" for x in values.values())
            rejected = sum(not x["accepted"] for x in values.values())
            review = bool(event.get("requiresHumanReview"))
            conn.execute("""
                INSERT INTO observability.request_trace (
                    request_id,profile_code,state,classified_at,
                    deterministic_count,ai_count,rejected_count,requires_review,decisions
                ) VALUES (%s,%s,%s,now(),%s,%s,%s,%s,%s::jsonb)
                ON CONFLICT(request_id) DO UPDATE SET
                 state=EXCLUDED.state, classified_at=EXCLUDED.classified_at,
                 deterministic_count=EXCLUDED.deterministic_count,ai_count=EXCLUDED.ai_count,
                 rejected_count=EXCLUDED.rejected_count,requires_review=EXCLUDED.requires_review,
                 decisions=EXCLUDED.decisions
            """,(request_id, profile, "REVIEW_REQUIRED" if review else "CLASSIFIED",
                 rules, ais, rejected, review, json.dumps(values)))
        elif kind == "system.event":
            state = str(event.get("type"))
            if state not in ("CRM_CREATED", "CRM_ERROR", "REVIEW_REQUIRED"):
                return
            data = event.get("data") or {}
            external_id = str(data.get("externalTicketId") or "")[:120] or None
            conn.execute("""
                INSERT INTO observability.request_trace(
                    request_id,profile_code,state,crm_at,crm_external_id,requires_review
                ) VALUES (%s,%s,%s,now(),%s,%s)
                ON CONFLICT(request_id) DO UPDATE SET
                 state=EXCLUDED.state,crm_at=EXCLUDED.crm_at,
                 crm_external_id=EXCLUDED.crm_external_id,
                 requires_review=observability.request_trace.requires_review OR EXCLUDED.requires_review
            """,(request_id, profile, state, external_id, state=="REVIEW_REQUIRED"))

def listen_forever():
    while True:
        try:
            parameters = pika.ConnectionParameters(
                host=BROKER, port=int(os.getenv("RABBITMQ_PORT", "5672")),
                credentials=pika.PlainCredentials(USER, PASSWORD),
                heartbeat=45, blocked_connection_timeout=30
            )
            with pika.BlockingConnection(parameters) as connection:
                channel = connection.channel()
                channel.exchange_declare(exchange="ticket.events", exchange_type="topic", durable=True)
                for name, key in (("email","email.received"),
                                  ("ticket","ticket.standardized"),
                                  ("system","system.event")):
                    queue = "observability." + name + ".q"
                    channel.queue_declare(queue=queue, durable=True)
                    channel.queue_bind(queue=queue, exchange="ticket.events", routing_key=key)
                channel.basic_qos(prefetch_count=20)
                def callback(ch, method, properties, body):
                    try:
                        event = json.loads(body)
                        write_event(method.routing_key, event)
                        ch.basic_ack(method.delivery_tag)
                    except Exception:
                        log.exception("Telemetry write failed; message will be retried")
                        ch.basic_nack(method.delivery_tag, requeue=True)
                        time.sleep(1)
                for queue in ("observability.email.q","observability.ticket.q",
                              "observability.system.q"):
                    channel.basic_consume(queue, callback)
                log.info("Observability subscribed to pipeline events")
                channel.start_consuming()
        except Exception:
            log.exception("Observability broker reconnecting")
            time.sleep(5)

@asynccontextmanager
async def lifespan(app):
    for _ in range(30):
        try:
            init_db()
            break
        except Exception:
            log.warning("Waiting for PostgreSQL")
            await asyncio.sleep(2)
    else:
        raise RuntimeError("PostgreSQL unavailable")
    threading.Thread(target=listen_forever, daemon=True).start()
    yield

app = FastAPI(lifespan=lifespan, docs_url=None, openapi_url=None)

def authorize(authorization: str | None = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(401, "Authentication required")
    try:
        payload = jwt.decode(authorization[7:], SECRET, algorithms=["HS256"],
                             issuer="agnostic-ticket-router",
                             options={"require": ["exp", "iss", "sub"]})
        if "ROLE_ADMIN" not in str(payload.get("roles", "")).split():
            raise HTTPException(403, "Admin role required")
        return payload
    except jwt.PyJWTError as exc:
        raise HTTPException(401, "Expired or invalid token") from exc

@app.get("/health")
def health():
    return {"status": "up"}

@app.get("/api/v1/admin/metrics/overview")
def overview(_=Depends(authorize)):
    zone = ZoneInfo(TIMEZONE)
    today = datetime.now(zone).date()
    start = today - timedelta(days=6)
    with database() as conn:
        rows = conn.execute("""
            SELECT request_id, received_at, profile_code, source, state,
                   deterministic_count, ai_count, rejected_count,
                   requires_review, crm_external_id, decisions
            FROM observability.request_trace
            WHERE (received_at AT TIME ZONE %s)::date >= %s
            ORDER BY received_at DESC
        """, (TIMEZONE, start)).fetchall()

    days = {(start + timedelta(days=i)).isoformat(): {
        "day": (start + timedelta(days=i)).isoformat(),
        "received": 0, "created": 0, "review": 0, "errors": 0
    } for i in range(7)}
    rule_dimensions = ai_dimensions = 0
    rule_only = ai_used = low_confidence = crm_created = crm_error = review = pending = 0
    today_received = 0
    for r in rows:
        day = r["received_at"].astimezone(zone).date().isoformat()
        if day not in days:
            continue
        d = days[day]
        d["received"] += 1
        if day == today.isoformat():
            today_received += 1
        rule_dimensions += r["deterministic_count"]
        ai_dimensions += r["ai_count"]
        if r["ai_count"] > 0:
            ai_used += 1
        elif r["deterministic_count"] > 0:
            rule_only += 1
        if r["rejected_count"] > 0:
            low_confidence += 1
        state = r["state"]
        if state == "CRM_CREATED":
            crm_created += 1
            d["created"] += 1
        elif state == "CRM_ERROR":
            crm_error += 1
            d["errors"] += 1
        elif state == "REVIEW_REQUIRED":
            review += 1
            d["review"] += 1
        else:
            pending += 1

    recent = [{
        "requestId": r["request_id"], "at": r["received_at"].isoformat(),
        "profile": r["profile_code"], "source": r["source"],
        "state": r["state"], "rules": r["deterministic_count"],
        "ai": r["ai_count"], "rejected": r["rejected_count"],
        "review": r["requires_review"], "externalId": r["crm_external_id"],
        "decisions": r["decisions"]
    } for r in rows[:30]]
    return {
        "timezone": TIMEZONE, "windowDays": 7,
        "todayReceived": today_received, "received": len(rows),
        "ruleOnly": rule_only, "aiUsed": ai_used,
        "ruleDimensions": rule_dimensions, "aiDimensions": ai_dimensions,
        "lowConfidence": low_confidence, "review": review,
        "crmCreated": crm_created, "crmError": crm_error, "pending": pending,
        "daily": list(days.values()), "recent": recent
    }
