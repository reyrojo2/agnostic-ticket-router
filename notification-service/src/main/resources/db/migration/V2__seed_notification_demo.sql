INSERT INTO notification_channel(code,name,type,active)
VALUES ('LOG_DEFAULT','Canal log para desarrollo','LOG',true);

INSERT INTO notification_template(profile_code,event_type,channel_code,recipient_template,subject_template,body_template,active)
VALUES
('TELCO_DEMO','EMAIL_RECEIVED','LOG_DEFAULT','{{from}}','Solicitud recibida - {{requestId}}','Hemos recibido tu solicitud "{{subject}}". La estamos evaluando.',true),
('TELCO_DEMO','CRM_CREATED','LOG_DEFAULT','{{recipient}}','Ticket creado - {{requestId}}','Tu solicitud fue registrada correctamente en el CRM. Identificador externo: {{data.externalTicketId}}',true),
('TELCO_DEMO','REVIEW_REQUIRED','LOG_DEFAULT','{{recipient}}','Solicitud en revisión - {{requestId}}','Tu solicitud requiere una validación adicional antes de registrarse en el CRM.',true),
('TELCO_DEMO','CRM_ERROR','LOG_DEFAULT','{{recipient}}','Incidencia procesando solicitud - {{requestId}}','No fue posible completar automáticamente el registro. El caso quedó identificado para revisión.',true);
