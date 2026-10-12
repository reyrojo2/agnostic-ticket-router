INSERT INTO crm_integration(code,name,base_url,auth_type,default_headers_json,active)
VALUES ('DEMO_CRM','CRM externo de demostración','http://localhost:8090','NONE','{"X-Source":"AgnosticTicketRouter"}',true);

INSERT INTO crm_operation(
    integration_id,code,http_method,path_template,content_type,body_template,
    success_status_codes,external_id_path,timeout_ms,max_attempts,retry_delay_ms,active
)
SELECT id,'CREATE_TICKET','POST','/api/tickets','application/json',
'{
  "requestId": "{{requestId}}",
  "title": "{{subject}}",
  "description": "{{body}}",
  "requester": "{{sender}}",
  "product": "{{decisions.PRODUCTO.value}}",
  "managementType": "{{decisions.TIPO_GESTION.value}}",
  "priority": "{{decisions.PRIORIDAD.value}}",
  "destination": "{{decisions.DESTINO.value}}"
}',
'200,201','data.caseNumber',15000,3,500,true
FROM crm_integration WHERE code='DEMO_CRM';

INSERT INTO crm_route(profile_code,dimension_code,option_code,integration_code,operation_code,priority,active)
VALUES ('TELCO_DEMO',NULL,NULL,'DEMO_CRM','CREATE_TICKET',100,true);
