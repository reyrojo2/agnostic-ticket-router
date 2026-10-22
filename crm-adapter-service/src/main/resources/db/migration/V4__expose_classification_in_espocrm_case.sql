-- Preserve administrator-customized CRM mappings. Update only the original V3 seed.
-- EspoCRM Case.description is standard; no new EspoCRM fields required.
UPDATE crm_operation
SET body_template = '{"name":"{{subject}}","description":"{{body}}\n\n--- Clasificación AgnosticTicketRouter ---\n{{classificationSummary}}\nSolicitud: {{requestId}}\nPerfil: {{profileCode}}"}'
WHERE code = 'CREATE_CASE'
  AND integration_id IN (SELECT id FROM crm_integration WHERE code = 'ESPOCRM_LOCAL')
  AND body_template = '{"name":"{{subject}}","description":"{{body}}"}';
