ALTER TABLE crm_operation ADD COLUMN external_number_path VARCHAR(500);
ALTER TABLE crm_delivery_audit ADD COLUMN external_number VARCHAR(150);

-- The public case number is separate from EspoCRM's technical record ID.
UPDATE crm_operation o SET external_number_path = 'number'
FROM crm_integration i
WHERE o.integration_id = i.id AND i.code LIKE 'ESPOCRM%'
  AND o.path_template = '/api/v1/Case' AND o.http_method = 'POST'
  AND o.external_number_path IS NULL;
