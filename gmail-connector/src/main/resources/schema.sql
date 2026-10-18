CREATE SCHEMA IF NOT EXISTS gmail_connector;
CREATE TABLE IF NOT EXISTS gmail_connector.account (
 id INTEGER PRIMARY KEY CHECK(id=1),
 email TEXT NOT NULL, refresh_token TEXT NOT NULL, access_token TEXT,
 expires_at BIGINT NOT NULL DEFAULT 0,
 profile_code VARCHAR(80) NOT NULL DEFAULT 'TELCO_DEMO',
 query_text VARCHAR(240) NOT NULL DEFAULT 'in:inbox is:unread',
 enabled BOOLEAN NOT NULL DEFAULT FALSE,
 last_sync TIMESTAMPTZ, last_error TEXT
);
CREATE TABLE IF NOT EXISTS gmail_connector.oauth_state (
 token VARCHAR(100) PRIMARY KEY, expires_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS gmail_connector.processed (
 email TEXT NOT NULL, message_id TEXT NOT NULL,
 request_id TEXT NOT NULL, processed_at BIGINT NOT NULL,
 PRIMARY KEY(email,message_id)
);
