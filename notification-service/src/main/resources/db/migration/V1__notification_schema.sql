CREATE TABLE notification_channel (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'LOG',
    host VARCHAR(500),
    port INTEGER DEFAULT 587,
    username_ref VARCHAR(500),
    password_ref VARCHAR(500),
    from_address VARCHAR(320),
    smtp_auth BOOLEAN NOT NULL DEFAULT TRUE,
    start_tls BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE notification_template (
    id BIGSERIAL PRIMARY KEY,
    profile_code VARCHAR(80) NOT NULL DEFAULT '*',
    event_type VARCHAR(80) NOT NULL,
    channel_code VARCHAR(80) NOT NULL,
    recipient_template VARCHAR(1000) NOT NULL,
    subject_template TEXT NOT NULL,
    body_template TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(profile_code, event_type)
);
