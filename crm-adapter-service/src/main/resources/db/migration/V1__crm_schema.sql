CREATE TABLE crm_integration (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    base_url VARCHAR(1000) NOT NULL,
    auth_type VARCHAR(30) NOT NULL DEFAULT 'NONE',
    username_ref VARCHAR(500),
    secret_ref VARCHAR(500),
    api_key_name VARCHAR(160),
    token_url VARCHAR(1000),
    scope VARCHAR(500),
    default_headers_json TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE crm_operation (
    id BIGSERIAL PRIMARY KEY,
    integration_id BIGINT NOT NULL REFERENCES crm_integration(id) ON DELETE CASCADE,
    code VARCHAR(80) NOT NULL,
    http_method VARCHAR(12) NOT NULL DEFAULT 'POST',
    path_template VARCHAR(1000) NOT NULL,
    content_type VARCHAR(120) NOT NULL DEFAULT 'application/json',
    headers_template_json TEXT,
    body_template TEXT,
    success_status_codes VARCHAR(120) NOT NULL DEFAULT '200,201,202',
    external_id_path VARCHAR(500),
    timeout_ms INTEGER NOT NULL DEFAULT 15000,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    retry_delay_ms BIGINT NOT NULL DEFAULT 1000,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(integration_id, code)
);

CREATE TABLE crm_route (
    id BIGSERIAL PRIMARY KEY,
    profile_code VARCHAR(80) NOT NULL,
    dimension_code VARCHAR(80),
    option_code VARCHAR(80),
    integration_code VARCHAR(80) NOT NULL,
    operation_code VARCHAR(80) NOT NULL DEFAULT 'CREATE_TICKET',
    priority INTEGER NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
