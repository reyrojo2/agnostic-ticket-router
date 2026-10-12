CREATE TABLE app_user (
 id BIGSERIAL PRIMARY KEY, username VARCHAR(80) NOT NULL UNIQUE, password_hash VARCHAR(120) NOT NULL,
 role VARCHAR(40) NOT NULL DEFAULT 'ADMIN', active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE decision_profile (
 id BIGSERIAL PRIMARY KEY, code VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(160) NOT NULL,
 version INTEGER NOT NULL DEFAULT 1, active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE decision_dimension (
 id BIGSERIAL PRIMARY KEY, profile_id BIGINT NOT NULL REFERENCES decision_profile(id), code VARCHAR(80) NOT NULL,
 name VARCHAR(160) NOT NULL, primitive_type VARCHAR(20) NOT NULL, instructions TEXT NOT NULL,
 execution_order INTEGER NOT NULL DEFAULT 100, required BOOLEAN NOT NULL DEFAULT TRUE,
 auto_min_probability DOUBLE PRECISION NOT NULL DEFAULT .70, min_margin DOUBLE PRECISION NOT NULL DEFAULT .20,
 active BOOLEAN NOT NULL DEFAULT TRUE, depends_on_dimension_code VARCHAR(80), depends_on_option_code VARCHAR(80),
 UNIQUE(profile_id,code)
);
CREATE TABLE decision_option (
 id BIGSERIAL PRIMARY KEY, dimension_id BIGINT NOT NULL REFERENCES decision_dimension(id) ON DELETE CASCADE,
 code VARCHAR(80) NOT NULL, name VARCHAR(160) NOT NULL, description TEXT NOT NULL,
 include_context TEXT, exclude_context TEXT, priority INTEGER NOT NULL DEFAULT 100, active BOOLEAN NOT NULL DEFAULT TRUE,
 depends_on_dimension_code VARCHAR(80), depends_on_option_code VARCHAR(80), UNIQUE(dimension_id,code)
);
CREATE TABLE decision_rule (
 id BIGSERIAL PRIMARY KEY, dimension_id BIGINT NOT NULL REFERENCES decision_dimension(id) ON DELETE CASCADE,
 code VARCHAR(120) NOT NULL, target_option_code VARCHAR(80) NOT NULL, rule_type VARCHAR(30) NOT NULL,
 pattern TEXT NOT NULL, strength VARCHAR(20) NOT NULL DEFAULT 'SOFT', priority INTEGER NOT NULL DEFAULT 100,
 active BOOLEAN NOT NULL DEFAULT TRUE, UNIQUE(dimension_id,code)
);
