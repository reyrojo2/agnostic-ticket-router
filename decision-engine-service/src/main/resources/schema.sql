CREATE SCHEMA IF NOT EXISTS decision_engine;
CREATE TABLE IF NOT EXISTS decision_engine.processed_request(
 request_id VARCHAR(100) PRIMARY KEY,
 processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
