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
ALTER TABLE observability.request_trace ADD COLUMN IF NOT EXISTS outcome_message VARCHAR(500);
ALTER TABLE observability.request_trace ADD COLUMN IF NOT EXISTS crm_http_status INTEGER;
ALTER TABLE observability.request_trace ADD COLUMN IF NOT EXISTS crm_external_number VARCHAR(150);
