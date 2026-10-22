CREATE SCHEMA IF NOT EXISTS decision_engine;
CREATE TABLE IF NOT EXISTS decision_engine.processed_request(
 request_id VARCHAR(100) PRIMARY KEY,
 processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS decision_engine.ai_provider (
 id BIGSERIAL PRIMARY KEY,
 name VARCHAR(160) NOT NULL,
 base_url VARCHAR(1000) NOT NULL,
 endpoint_path VARCHAR(200) NOT NULL DEFAULT '/v1/systemone',
 model VARCHAR(160) NOT NULL DEFAULT '',
 language VARCHAR(30) NOT NULL DEFAULT '',
 auth_type VARCHAR(30) NOT NULL,
 api_key_header VARCHAR(80) NOT NULL DEFAULT 'X-Api-Key',
 encrypted_api_key TEXT,
 enabled BOOLEAN NOT NULL DEFAULT TRUE,
 default_slot INTEGER UNIQUE,
 CONSTRAINT ai_default_slot CHECK (default_slot IS NULL OR (default_slot=1 AND enabled=TRUE))
);
