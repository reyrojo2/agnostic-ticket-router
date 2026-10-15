-- EspoCRM es el CRM principal del perfil de demostración.
-- NO modificar V1/V2: Flyway ya ha ejecutado esas migraciones en instalaciones existentes.
-- Las credenciales se resuelven en tiempo de ejecución desde ENV:ESPOCRM_API_KEY.

INSERT INTO crm_integration(
    code, name, base_url, auth_type, secret_ref, api_key_name, default_headers_json, active
) VALUES (
    'ESPOCRM_LOCAL', 'EspoCRM local', 'http://host.docker.internal:8080',
    'API_KEY_HEADER', 'ENV:ESPOCRM_API_KEY', 'X-Api-Key', '{}', true
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO crm_operation(
    integration_id, code, http_method, path_template, content_type,
    headers_template_json, body_template, success_status_codes,
    external_id_path, timeout_ms, max_attempts, retry_delay_ms, active
)
SELECT id, 'CREATE_CASE', 'POST', '/api/v1/Case', 'application/json',
       '{}',
       '{"name":"{{subject}}","description":"{{body}}"}',
       '200,201', 'id', 15000, 1, 0, true
FROM crm_integration WHERE code = 'ESPOCRM_LOCAL'
ON CONFLICT (integration_id, code) DO NOTHING;

-- Reorientar la ruta general sembrada para el CRM de demostración.
UPDATE crm_route
SET integration_code = 'ESPOCRM_LOCAL', operation_code = 'CREATE_CASE',
    active = true
WHERE profile_code = 'TELCO_DEMO'
  AND integration_code = 'DEMO_CRM'
  AND operation_code = 'CREATE_TICKET'
  AND dimension_code IS NULL;

-- Si la instalación ya no tiene la ruta sembrada, crear una ruta por defecto.
INSERT INTO crm_route(
    profile_code, dimension_code, option_code,
    integration_code, operation_code, priority, active
)
SELECT 'TELCO_DEMO', NULL, NULL, 'ESPOCRM_LOCAL', 'CREATE_CASE', 100, true
WHERE NOT EXISTS (
    SELECT 1 FROM crm_route
    WHERE profile_code = 'TELCO_DEMO'
      AND integration_code = 'ESPOCRM_LOCAL'
      AND operation_code = 'CREATE_CASE'
      AND dimension_code IS NULL
);

-- Conservar la integración histórica pero desactivarla.
UPDATE crm_integration SET active = false WHERE code = 'DEMO_CRM';
UPDATE crm_route SET active = false WHERE integration_code = 'DEMO_CRM';
