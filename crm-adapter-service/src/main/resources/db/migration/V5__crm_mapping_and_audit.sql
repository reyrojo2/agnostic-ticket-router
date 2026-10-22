CREATE TABLE crm_field_mapping (
    id BIGSERIAL PRIMARY KEY,
    operation_id BIGINT NOT NULL REFERENCES crm_operation(id) ON DELETE CASCADE,
    dimension_code VARCHAR(80) NOT NULL,
    target_field VARCHAR(120) NOT NULL,
    value_map_json TEXT NOT NULL DEFAULT '{}',
    required BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_crm_field_target UNIQUE (operation_id, target_field)
);

CREATE TABLE crm_assignment_mapping (
    id BIGSERIAL PRIMARY KEY,
    operation_id BIGINT NOT NULL REFERENCES crm_operation(id) ON DELETE CASCADE,
    dimension_code VARCHAR(80) NOT NULL,
    option_code VARCHAR(120) NOT NULL,
    assignment_kind VARCHAR(12) NOT NULL CHECK (assignment_kind IN ('TEAM', 'USER')),
    external_id VARCHAR(100) NOT NULL,
    external_name VARCHAR(160),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_crm_assignment UNIQUE (operation_id, dimension_code, option_code, assignment_kind)
);

CREATE TABLE crm_delivery_audit (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(150) NOT NULL,
    profile_code VARCHAR(80),
    integration_code VARCHAR(80),
    operation_code VARCHAR(80),
    outcome VARCHAR(32) NOT NULL,
    external_id VARCHAR(150),
    http_status INTEGER,
    attempts INTEGER,
    decision_values_json TEXT,
    mapped_fields_json TEXT,
    error_summary VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_crm_audit_created ON crm_delivery_audit(created_at DESC);
CREATE INDEX ix_crm_audit_request ON crm_delivery_audit(request_id);
