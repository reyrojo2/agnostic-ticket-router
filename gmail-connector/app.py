"""Gmail OAuth connector for test mailboxes. No token is exposed to Angular.

The existing mail-ingestion REST endpoint remains the single entry point for
IncomingEmailEvent. This connector is optional and stores tokens encrypted.
"""
import asyncio
import base64
from contextlib import asynccontextmanager
from email import policy
from email.parser import BytesParser
from email.utils import parseaddr
import os
import re
import secrets
import sqlite3
import threading
import time
from urllib.parse import urlencode

from cryptography.fernet import Fernet, InvalidToken
from fastapi import Depends, FastAPI, HTTPException, Query
from fastapi.responses import RedirectResponse
import httpx
import jwt
from pydantic import BaseModel, Field

DB = os.getenv("GMAIL_DB", "/data/gmail.sqlite")
JWT_SECRET = os.getenv("APP_JWT_SECRET", "")
ENCRYPTION_KEY = os.getenv("GMAIL_ENCRYPTION_KEY", "")
CLIENT_ID = os.getenv("GMAIL_CLIENT_ID", "")
CLIENT_SECRET = os.getenv("GMAIL_CLIENT_SECRET", "")
REDIRECT_URI = os.getenv("GMAIL_REDIRECT_URI", "http://localhost:4200/api/v1/gmail/oauth/callback")
INTAKE_URL = os.getenv("MAIL_INGESTION_URL", "http://mail-ingestion-service:8081/api/v1/intake/emails")
POLL_SECONDS = max(60, int(os.getenv("GMAIL_POLL_SECONDS", "90")))
SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
LOCK = threading.Lock()

def conn():
    db = sqlite3.connect(DB, timeout=20)
    db.row_factory = sqlite3.Row
    return db

def initialize():
    os.makedirs(os.path.dirname(DB), exist_ok=True)
    with conn() as db:
        db.execute("""CREATE TABLE IF NOT EXISTS account (
            id INTEGER PRIMARY KEY CHECK (id = 1),
            email TEXT NOT NULL, refresh_token TEXT NOT NULL,
            access_token TEXT, expires_at INTEGER NOT NULL DEFAULT 0,
            profile_code TEXT NOT NULL DEFAULT 'TELCO_DEMO',
            query_text TEXT NOT NULL DEFAULT 'in:inbox is:unread',
            enabled INTEGER NOT NULL DEFAULT 0, last_sync TEXT, last_error TEXT
        )""")
        db.execute("""CREATE TABLE IF NOT EXISTS oauth_state (
            token TEXT PRIMARY KEY, expires_at INTEGER NOT NULL
        )""")
        db.execute("""CREATE TABLE IF NOT EXISTS processed (
            email TEXT NOT NULL, message_id TEXT NOT NULL,
            request_id TEXT NOT NULL, processed_at INTEGER NOT NULL,
            PRIMARY KEY(email, message_id)
        )""")

def cipher():
    if not ENCRYPTION_KEY:
        raise HTTPException(503, "Configura GMAIL_ENCRYPTION_KEY en .env y reinicia el conector")
    try:
        return Fernet(ENCRYPTION_KEY.encode())
    except (ValueError, TypeError) as exc:
        raise HTTPException(503, "GMAIL_ENCRYPTION_KEY no es una clave Fernet válida") from exc

def admin(authorization: str | None = None):
    # Bound by FastAPI Header dependency below.
    raise RuntimeError("Use require_admin")

from fastapi import Header

def require_admin(authorization: str | None = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(401, "Autenticación requerida")
    try:
        claims = jwt.decode(authorization[7:], JWT_SECRET, algorithms=["HS256"],
                            issuer="agnostic-ticket-router",
                            options={"require": ["exp", "iss", "sub"]})
        if "ROLE_ADMIN" not in claims.get("roles", "").split():
            raise HTTPException(403, "Permisos insuficientes")
        return claims
    except jwt.PyJWTError as exc:
        raise HTTPException(401, "Sesión inválida o caducada") from exc

def configured():
    return bool(CLIENT_ID and CLIENT_SECRET and ENCRYPTION_KEY and JWT_SECRET)

def get_account():
    with conn() as db:
        row = db.execute("SELECT * FROM account WHERE id=1").fetchone()
    return dict(row) if row else None

def status():
    row = get_account()
    return {
        "configured": configured(),
        "connected": bool(row),
        "email": row["email"] if row else None,
        "enabled": bool(row["enabled"]) if row else False,
        "profileCode": row["profile_code"] if row else "TELCO_DEMO",
        "query": row["query_text"] if row else "in:inbox is:unread",
        "lastSync": row["last_sync"] if row else None,
        "lastError": row["last_error"] if row else None,
        "pollSeconds": POLL_SECONDS
    }

async def refresh_token(account):
    if account["access_token"] and account["expires_at"] > int(time.time()) + 60:
        return cipher().decrypt(account["access_token"].encode()).decode()
    token = cipher().decrypt(account["refresh_token"].encode()).decode()
    async with httpx.AsyncClient(timeout=25) as client:
        r = await client.post("https://oauth2.googleapis.com/token", data={
            "client_id": CLIENT_ID, "client_secret": CLIENT_SECRET,
            "refresh_token": token, "grant_type": "refresh_token"
        })
        r.raise_for_status()
        data = r.json()
    access = data["access_token"]
    with conn() as db:
        db.execute("UPDATE account SET access_token=?,expires_at=? WHERE id=1",
                   (cipher().encrypt(access.encode()).decode(), int(time.time()) + data.get("expires_in", 3600)))
    return access

def plain_text(raw):
    decoded = base64.urlsafe_b64decode(raw + "=" * (-len(raw) % 4))
    message = BytesParser(policy=policy.default).parsebytes(decoded)
    from_value = parseaddr(str(message.get("From", "")))[1]
    subject = str(message.get("Subject", ""))[:500]
    body = message.get_body(preferencelist=("plain",))
    if body:
        content = body.get_content()
    else:
        content = message.get_content() if not message.is_multipart() else ""
    if not isinstance(content, str):
        content = ""
    return from_value, subject, content[:50000]

async def sync_now():
    if not LOCK.acquire(blocking=False):
        return {"processed": 0, "status": "busy"}
    processed = 0
    try:
        row = get_account()
        if not row or not row["enabled"]:
            return {"processed": 0, "status": "disabled"}
        token = await refresh_token(row)
        headers = {"Authorization": "Bearer " + token}
        async with httpx.AsyncClient(timeout=30) as client:
            page = None
            for _ in range(3):  # 3 x 50 maximum; intentional rate limit for demo accounts
                params = {"q": row["query_text"], "maxResults": 50}
                if page:
                    params["pageToken"] = page
                r = await client.get("https://gmail.googleapis.com/gmail/v1/users/me/messages",
                                     headers=headers, params=params)
                r.raise_for_status()
                data = r.json()
                for item in reversed(data.get("messages", [])):
                    mid = item["id"]
                    with conn() as db:
                        known = db.execute("SELECT 1 FROM processed WHERE email=? AND message_id=?",
                                           (row["email"], mid)).fetchone()
                    if known:
                        continue
                    rr = await client.get(f"https://gmail.googleapis.com/gmail/v1/users/me/messages/{mid}",
                                          headers=headers, params={"format": "raw"})
                    rr.raise_for_status()
                    sender, subject, body = plain_text(rr.json()["raw"])
                    if not sender or not re.fullmatch(r"[^\s@]+@[^\s@]+\.[^\s@]+", sender):
                        continue
                    if not subject or not body.strip():
                        continue
                    payload = {"profileCode": row["profile_code"], "from": sender,
                               "subject": subject, "body": body,
                               "metadata": {"source": "gmail", "gmailMessageId": mid}}
                    out = await client.post(INTAKE_URL, json=payload)
                    out.raise_for_status()
                    request_id = out.json()["requestId"]
                    # At-least-once semantics: crash after publish could duplicate.
                    with conn() as db:
                        db.execute("INSERT OR IGNORE INTO processed VALUES(?,?,?,?)",
                                   (row["email"], mid, request_id, int(time.time())))
                    processed += 1
                page = data.get("nextPageToken")
                if not page:
                    break
        with conn() as db:
            db.execute("UPDATE account SET last_sync=datetime('now'),last_error=NULL WHERE id=1")
        return {"processed": processed, "status": "ok"}
    except Exception as exc:
        # Do not expose refresh/access tokens or full HTTP response bodies in UI.
        category = type(exc).__name__
        with conn() as db:
            db.execute("UPDATE account SET last_error=? WHERE id=1",
                       (f"Falló la sincronización ({category}); revisa credenciales, alcance y red.",))
        return {"processed": processed, "status": "error", "detail": category}
    finally:
        LOCK.release()

async def poll_loop():
    while True:
        await asyncio.sleep(POLL_SECONDS)
        await sync_now()

@asynccontextmanager
async def lifespan(app: FastAPI):
    initialize()
    task = asyncio.create_task(poll_loop())
    yield
    task.cancel()
    try:
        await task
    except asyncio.CancelledError:
        pass

app = FastAPI(lifespan=lifespan, docs_url=None, redoc_url=None, openapi_url=None)

@app.get("/health")
def health():
    return {"status": "up"}

@app.get("/api/v1/admin/gmail/status")
def gmail_status(_=Depends(require_admin)):
    return status()

class Settings(BaseModel):
    profileCode: str = Field(pattern=r"^[A-Z][A-Z0-9_]{1,79}$")
    query: str = Field(min_length=1, max_length=240)
    enabled: bool = False

@app.put("/api/v1/admin/gmail/settings")
def gmail_settings(value: Settings, _=Depends(require_admin)):
    if not get_account():
        raise HTTPException(409, "Conecta primero una cuenta Gmail mediante OAuth")
    with conn() as db:
        db.execute("UPDATE account SET profile_code=?,query_text=?,enabled=? WHERE id=1",
                   (value.profileCode, value.query, int(value.enabled)))
    return status()

@app.get("/api/v1/admin/gmail/authorize")
def gmail_authorize(_=Depends(require_admin)):
    if not configured():
        raise HTTPException(503, "Configura GMAIL_CLIENT_ID, GMAIL_CLIENT_SECRET y GMAIL_ENCRYPTION_KEY")
    state = secrets.token_urlsafe(32)
    with conn() as db:
        db.execute("DELETE FROM oauth_state WHERE expires_at < ?", (int(time.time()),))
        db.execute("INSERT INTO oauth_state VALUES (?,?)", (state, int(time.time()) + 600))
    return {"url": "https://accounts.google.com/o/oauth2/v2/auth?" + urlencode({
        "client_id": CLIENT_ID, "redirect_uri": REDIRECT_URI, "response_type": "code",
        "scope": SCOPE, "access_type": "offline", "prompt": "consent",
        "state": state, "include_granted_scopes": "false"
    })}

@app.get("/api/v1/gmail/oauth/callback")
async def gmail_callback(code: str | None = None, state: str | None = None,
                         error: str | None = None):
    if error or not code or not state:
        return RedirectResponse("/gmail?error=consent", status_code=303)
    with conn() as db:
        row = db.execute("SELECT expires_at FROM oauth_state WHERE token=?", (state,)).fetchone()
        db.execute("DELETE FROM oauth_state WHERE token=?", (state,))
    if not row or row["expires_at"] < int(time.time()):
        return RedirectResponse("/gmail?error=state", status_code=303)
    try:
        async with httpx.AsyncClient(timeout=25) as client:
            response = await client.post("https://oauth2.googleapis.com/token", data={
                "code": code, "client_id": CLIENT_ID, "client_secret": CLIENT_SECRET,
                "redirect_uri": REDIRECT_URI, "grant_type": "authorization_code"
            })
            response.raise_for_status()
            data = response.json()
            if "refresh_token" not in data:
                raise ValueError("No refresh_token")
            profile = await client.get("https://gmail.googleapis.com/gmail/v1/users/me/profile",
                                       headers={"Authorization": "Bearer " + data["access_token"]})
            profile.raise_for_status()
            email = profile.json()["emailAddress"]
        with conn() as db:
            # Reconnection should not silently resume polling.
            db.execute("""INSERT INTO account (id,email,refresh_token,access_token,expires_at,enabled)
            VALUES (1,?,?,?, ?,0)
            ON CONFLICT(id) DO UPDATE SET email=excluded.email,
                refresh_token=excluded.refresh_token,access_token=excluded.access_token,
                expires_at=excluded.expires_at,enabled=0,last_error=NULL""",
                       (email, cipher().encrypt(data["refresh_token"].encode()).decode(),
                        cipher().encrypt(data["access_token"].encode()).decode(),
                        int(time.time()) + data.get("expires_in", 3600)))
        return RedirectResponse("/gmail?connected=1", status_code=303)
    except (httpx.HTTPError, KeyError, ValueError, InvalidToken):
        return RedirectResponse("/gmail?error=oauth", status_code=303)

@app.post("/api/v1/admin/gmail/sync")
async def gmail_sync(_=Depends(require_admin)):
    return await sync_now()

@app.delete("/api/v1/admin/gmail/connection")
def gmail_disconnect(_=Depends(require_admin)):
    with conn() as db:
        db.execute("DELETE FROM account WHERE id=1")
        db.execute("DELETE FROM oauth_state")
    return {"connected": False}
