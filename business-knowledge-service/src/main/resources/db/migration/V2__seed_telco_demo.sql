INSERT INTO decision_profile(code,name,version,active) VALUES ('TELCO_DEMO','Telecom - clasificación académica',1,true);

INSERT INTO decision_dimension(profile_id,code,name,primitive_type,instructions,execution_order,required,auto_min_probability,min_margin)
SELECT id,'PRODUCTO','Producto','CHOICE','¿Cuál es el producto principal afectado por la solicitud?',1,true,.70,.20 FROM decision_profile WHERE code='TELCO_DEMO';
INSERT INTO decision_dimension(profile_id,code,name,primitive_type,instructions,execution_order,required,auto_min_probability,min_margin)
SELECT id,'TIPO_GESTION','Tipo de gestión','CHOICE','¿Cuál es el tipo principal de gestión solicitada?',2,true,.70,.20 FROM decision_profile WHERE code='TELCO_DEMO';
INSERT INTO decision_dimension(profile_id,code,name,primitive_type,instructions,execution_order,required,auto_min_probability,min_margin)
SELECT id,'PRIORIDAD','Prioridad','CHOICE','¿Qué prioridad operativa corresponde a la solicitud?',3,true,.65,.15 FROM decision_profile WHERE code='TELCO_DEMO';
INSERT INTO decision_dimension(profile_id,code,name,primitive_type,instructions,execution_order,required,auto_min_probability,min_margin)
SELECT id,'DESTINO','Destino','CHOICE','¿Qué equipo debe atender principalmente la solicitud?',4,true,.70,.20 FROM decision_profile WHERE code='TELCO_DEMO';

-- PRODUCTO
INSERT INTO decision_option(dimension_id,code,name,description,include_context,exclude_context,priority)
SELECT id,'MOVIL','Móvil','Líneas celulares, SIM, eSIM, números celulares y planes móviles corporativos.','Cambio de RUC, titularidad, migración entre cuentas, activación, suspensión y facturación de líneas móviles.','No usar para SIM M2M de GPS, sensores o máquinas.',10 FROM decision_dimension WHERE code='PRODUCTO';
INSERT INTO decision_option(dimension_id,code,name,description,include_context,exclude_context,priority)
SELECT id,'FIJO','Fijo','Telefonía fija empresarial y numeración fija.','Líneas telefónicas fijas asociadas a una ubicación.','No incluye SIM, números celulares ni internet empresarial.',20 FROM decision_dimension WHERE code='PRODUCTO';
INSERT INTO decision_option(dimension_id,code,name,description,include_context,exclude_context,priority)
SELECT id,'CONECTIVIDAD','Conectividad','Internet empresarial, enlaces de datos, MPLS, WAN o SD-WAN.','Internet dedicado y conectividad entre sedes.','No usar para líneas celulares de personas.',30 FROM decision_dimension WHERE code='PRODUCTO';
INSERT INTO decision_option(dimension_id,code,name,description,include_context,exclude_context,priority)
SELECT id,'IOT','IoT','Servicios M2M o Internet de las Cosas.','GPS, telemetría, sensores, POS, rastreadores y dispositivos conectados.','No usar para líneas celulares normales de empleados.',40 FROM decision_dimension WHERE code='PRODUCTO';
INSERT INTO decision_option(dimension_id,code,name,description,include_context,exclude_context,priority)
SELECT id,'COMUNICACION','Comunicación','Comunicaciones unificadas, PBX, videoconferencia y colaboración.','Central virtual y soluciones colaborativas.','No usar solo porque el texto mencione líneas o teléfonos; líneas celulares son MÓVIL.',50 FROM decision_dimension WHERE code='PRODUCTO';
INSERT INTO decision_option(dimension_id,code,name,description,include_context,exclude_context,priority)
SELECT id,'CLOUD','Cloud','Infraestructura y servicios alojados en nube.','Máquinas virtuales, storage, backup cloud e IaaS.','No usar para internet empresarial.',60 FROM decision_dimension WHERE code='PRODUCTO';
INSERT INTO decision_option(dimension_id,code,name,description,include_context,exclude_context,priority)
SELECT id,'CYBER_SEGURIDAD','Cyber Seguridad','Servicios de ciberseguridad empresarial.','Firewall, SOC, antivirus, protección y detección de amenazas.','',70 FROM decision_dimension WHERE code='PRODUCTO';
INSERT INTO decision_option(dimension_id,code,name,description,include_context,exclude_context,priority)
SELECT id,'DATACENTER','Datacenter','Servicios de centro de datos.','Housing, colocation, racks y hosting físico.','',80 FROM decision_dimension WHERE code='PRODUCTO';
INSERT INTO decision_option(dimension_id,code,name,description,include_context,exclude_context,priority)
SELECT id,'AUTOSERVICIOS','Autoservicios','Portales o canales donde el cliente realiza operaciones directamente.','','No usar para trámites ejecutados por asesores sobre otros productos.',90 FROM decision_dimension WHERE code='PRODUCTO';
INSERT INTO decision_option(dimension_id,code,name,description,include_context,exclude_context,priority)
SELECT id,'DTH','DTH','Televisión satelital DTH.','','',100 FROM decision_dimension WHERE code='PRODUCTO';
INSERT INTO decision_option(dimension_id,code,name,description,include_context,exclude_context,priority)
SELECT id,'SVA','SVA','Servicios de valor agregado adicionales a un producto principal.','','No usar cuando el caso trata directamente el producto principal.',110 FROM decision_dimension WHERE code='PRODUCTO';
INSERT INTO decision_option(dimension_id,code,name,description,include_context,exclude_context,priority)
SELECT id,'GESTION_INTERNA','Gestión Interna','Proceso exclusivamente interno sin un producto de cliente afectado.','Tareas administrativas internas sin producto comercial.','No usar cuando un cambio de RUC, contrato o facturación afecta MÓVIL, FIJO, CONECTIVIDAD u otro producto.',120 FROM decision_dimension WHERE code='PRODUCTO';

-- TIPO_GESTION
INSERT INTO decision_option(dimension_id,code,name,description,priority)
SELECT id,'CAMBIO_RUC','Cambio de RUC','Cambio, traslado o traspaso de productos entre RUC, cuentas o razones sociales.',10 FROM decision_dimension WHERE code='TIPO_GESTION';
INSERT INTO decision_option(dimension_id,code,name,description,priority)
SELECT id,'FACTURACION','Facturación','Consultas o modificaciones principalmente relacionadas con facturación, cobros o separación de facturas.',20 FROM decision_dimension WHERE code='TIPO_GESTION';
INSERT INTO decision_option(dimension_id,code,name,description,priority)
SELECT id,'ACTIVACION','Activación','Alta o activación de un producto o servicio.',30 FROM decision_dimension WHERE code='TIPO_GESTION';
INSERT INTO decision_option(dimension_id,code,name,description,priority)
SELECT id,'INCIDENTE','Incidente','Falla o indisponibilidad de un producto que ya debería estar operativo.',40 FROM decision_dimension WHERE code='TIPO_GESTION';
INSERT INTO decision_option(dimension_id,code,name,description,priority)
SELECT id,'OTRO','Otro','Otra gestión no cubierta por las alternativas anteriores.',100 FROM decision_dimension WHERE code='TIPO_GESTION';

-- PRIORIDAD
INSERT INTO decision_option(dimension_id,code,name,description,priority) SELECT id,'BAJA','Baja','Sin impacto inmediato o puede esperar.',10 FROM decision_dimension WHERE code='PRIORIDAD';
INSERT INTO decision_option(dimension_id,code,name,description,priority) SELECT id,'MEDIA','Media','Requiere atención normal, sin interrupción crítica.',20 FROM decision_dimension WHERE code='PRIORIDAD';
INSERT INTO decision_option(dimension_id,code,name,description,priority) SELECT id,'ALTA','Alta','Afectación importante, urgencia explícita o riesgo operativo relevante.',30 FROM decision_dimension WHERE code='PRIORIDAD';
INSERT INTO decision_option(dimension_id,code,name,description,priority) SELECT id,'CRITICA','Crítica','Interrupción severa o impacto masivo que requiere respuesta inmediata.',40 FROM decision_dimension WHERE code='PRIORIDAD';

-- DESTINO
INSERT INTO decision_option(dimension_id,code,name,description,priority) SELECT id,'BACKOFFICE','Backoffice','Cambios administrativos o de configuración sobre productos existentes.',10 FROM decision_dimension WHERE code='DESTINO';
INSERT INTO decision_option(dimension_id,code,name,description,priority) SELECT id,'SOPORTE','Soporte','Incidentes técnicos, fallas o indisponibilidad.',20 FROM decision_dimension WHERE code='DESTINO';
INSERT INTO decision_option(dimension_id,code,name,description,priority) SELECT id,'FACTURACION','Facturación','Cobros, facturas, notas de crédito o separación de facturación.',30 FROM decision_dimension WHERE code='DESTINO';
INSERT INTO decision_option(dimension_id,code,name,description,priority) SELECT id,'COMERCIAL','Comercial','Altas, ofertas, contratación y cambios comerciales.',40 FROM decision_dimension WHERE code='DESTINO';

-- Reglas fuertes de ejemplo. Se pueden cambiar desde BD sin recompilar.
INSERT INTO decision_rule(dimension_id,code,target_option_code,rule_type,pattern,strength,priority)
SELECT id,'PROD_IOT_GPS','IOT','CONTAINS_ANY','gps|telemetría|m2m|rastreador','HARD',10 FROM decision_dimension WHERE code='PRODUCTO';
INSERT INTO decision_rule(dimension_id,code,target_option_code,rule_type,pattern,strength,priority)
SELECT id,'PROD_CONECT_DEDICADO','CONECTIVIDAD','CONTAINS_ANY','internet dedicado|mpls|sd-wan|enlace dedicado','HARD',20 FROM decision_dimension WHERE code='PRODUCTO';
INSERT INTO decision_rule(dimension_id,code,target_option_code,rule_type,pattern,strength,priority)
SELECT id,'GEST_CAMBIO_RUC','CAMBIO_RUC','CONTAINS_ANY','cambio de ruc|traspaso de ruc|traslado de ruc|migrar al ruc','HARD',10 FROM decision_dimension WHERE code='TIPO_GESTION';

INSERT INTO decision_rule(dimension_id,code,target_option_code,rule_type,pattern,strength,priority)
SELECT id,'PROD_MOVIL_EXPLICITO','MOVIL','CONTAINS_ANY','línea celular|líneas celulares|sim móvil|esim|plan móvil','SOFT',30 FROM decision_dimension WHERE code='PRODUCTO';
