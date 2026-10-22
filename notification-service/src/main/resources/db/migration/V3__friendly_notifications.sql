ALTER TABLE notification_channel ADD COLUMN encrypted_username TEXT;
ALTER TABLE notification_channel ADD COLUMN encrypted_password TEXT;
ALTER TABLE notification_channel ADD COLUMN ssl_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Upgrade only the original demo text. Preserve administrator customizations and channel selection.
UPDATE notification_template SET subject_template = 'Hemos recibido tu solicitud'
WHERE event_type = 'EMAIL_RECEIVED' AND subject_template = 'Solicitud recibida - {{requestId}}';
UPDATE notification_template SET subject_template = '{{confirmationSubject}}'
WHERE event_type = 'CRM_CREATED' AND subject_template = 'Ticket creado - {{requestId}}';
UPDATE notification_template SET body_template = '{{confirmationMessage}}'
WHERE event_type = 'CRM_CREATED' AND body_template = 'Tu solicitud fue registrada correctamente en el CRM. Identificador externo: {{data.externalTicketId}}';
UPDATE notification_template SET subject_template = 'Tu solicitud necesita una revisión'
WHERE event_type = 'REVIEW_REQUIRED' AND subject_template = 'Solicitud en revisión - {{requestId}}';
UPDATE notification_template SET subject_template = 'Estamos revisando el registro de tu solicitud'
WHERE event_type = 'CRM_ERROR' AND subject_template = 'Incidencia procesando solicitud - {{requestId}}';
