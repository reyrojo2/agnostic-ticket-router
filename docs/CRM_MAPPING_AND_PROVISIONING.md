# CRM Adapter: clasificación, validación y asignación

## Capas

- Business Knowledge contiene *dimensiones*, *opciones* y reglas; no almacena IDs del CRM.
- Decision Engine genera `StandardTicketEvent.decisions` y no conoce campos EspoCRM.
- `CrmRouter` elige la integración/operación por perfil y una condición priorizada.
- `CrmPayloadComposer` combina `bodyTemplate` con `crm_field_mapping` y `crm_assignment_mapping`.
- `CrmHttpExecutor` **usa el mismo compositor** que `CrmPreviewService`; no mantiene una implementación distinta del mapeo.
- `CrmAuditService` guarda estado, requestId, dimensiones/opciones, campos mapeados, código HTTP e ID externo, no el cuerpo del correo, token, contraseña ni respuesta HTTP.

## Uso desde Angular

1. Abrir **Integraciones CRM**, seleccionar `ESPOCRM_LOCAL` y pulsar `CREATE_CASE`.
2. En **Mapeo de clasificación al CRM**, pulsar **Leer campos y equipos de EspoCRM**. Se lee `GET /api/v1/Metadata?key=entityDefs.Case.fields` (la API Key necesita acceso a metadatos). Los nombres de campos del CRM deben ser exactos.
3. Vincular una dimensión (p. ej. `TIPO_GESTION`) a un campo API (p. ej. `cTipologia`), y registrar equivalencias JSON opcionales (`{"CAMBIO_RUC":"Cambio de RUC"}`). Seleccionar si el valor es obligatorio. El campo se guarda como configuración en PostgreSQL del adaptador, **no se crea automáticamente en EspoCRM**.
4. Para áreas y asignaciones, seleccionar un equipo ya existente o pulsar **Crear o vincular equipo**, que consulta `GET /api/v1/Team` y usa `POST /api/v1/Team` *únicamente si no existe*. Es una acción explícita con confirmación. También puede importar hasta 30 equipos por vez desde las opciones activas de una dimensión en Business Knowledge, tras mostrar una confirmación con todos los nombres. Para responsables, seleccionar un `User` existente y registrar la equivalencia de dimensión+opción a su ID real. **No se crean usuarios de acceso ni contraseñas.**
5. Pulsar **Validar compatibilidad** para confirmar campos remotos (`readOnly`, `notStorable`, opciones de enum) e IDs de Team/User. Una API Key insuficiente genera error visible; el adaptador no desactiva la validación silenciosamente.
6. Ejecutar la **Vista previa del JSON** ingresando un perfil, asunto, cuerpo y decisiones simuladas; resuelve `CrmRouter`, mapea el body, añade `teamsIds` y `assignedUserId`, y muestra el JSON **sin hacer POST a EspoCRM**.
7. Enviar una solicitud de prueba mediante el flujo normal; comprobar EspoCRM y la tabla **Trazabilidad**. Los casos existentes no se modifican.

## ¿Se pueden crear automáticamente tipologías/campos en EspoCRM?

EspoCRM documenta operaciones CRUD sobre registros estándar (`POST /api/v1/Team`, `GET /api/v1/User`) y lectura de metadatos (`GET /api/v1/Metadata?key=...`). Sin embargo, **no se garantiza una API de escritura de metadatos estable para definir nuevos campos personalizados por versiones**. No hacemos llamadas internas no documentadas al Entity Manager para cambiar el esquema: podrían romperlo.

Si quieres que `TIPO_GESTION` sea filtrable, en EspoCRM abre **Administration → Entity Manager → Case → Fields → Create Field**; elige Varchar o Enum, define los valores y agrega el campo al layout. Revisa el nombre API exacto (p. ej. `cTipologia`) y después elige ese campo desde el panel de mapeo en Angular. Para otra dimensión repite únicamente la creación del campo: todas las opciones y sus equivalencias se gestionan desde Agnostic Ticket Router. Si solo necesitas una demostración sin campos nuevos, `{{classificationSummary}}` ya muestra todas las dimensiones en `description`.

No confundir el campo textual `DESTINO=BACKOFFICE` con una asignación: `teamsIds` requiere un ID de `Team` existente, y `assignedUserId` un ID de `User` existente. El adaptador los resuelve por mapeos persistidos, sin meterlos en el motor de decisiones.

## Seguridad y limitaciones

- Los endpoints de configuración son administrativos y requieren JWT; el rol de provisión en EspoCRM depende de una API Key específica con permisos adecuados. Recomendada una cuenta de API para desarrollo con privilegios mínimos. En producción conviene aplicar un rol admin explícito también a los endpoints JWT.
- La consulta `Metadata` puede depender de la versión/roles. La lectura de catálogos está limitada a `Team` y `User`, paginada y con límite protector de 1200 registros.
- La creación de equipos es idempotente por nombre dentro de los equipos visibles para la API Key; dos administradores concurrentes u ocultamiento por permisos pueden causar colisiones en el CRM.
- **La vista previa no envía tickets; la validación no garantiza que una solicitud de negocio posterior sea aceptada por reglas, ACL o validaciones en CRM.** Prueba siempre un Case de ejemplo.
- El registro de auditoría no implementa un outbox transaccional; podría persistirse un Case si RabbitMQ falla al publicar el resultado. Para operación de producción se recomienda outbox e idempotencia extremo a extremo.
- Se conservan intactos los volúmenes PostgreSQL, el historial Gmail y las migraciones Flyway V1–V4. V5 solo agrega tablas nuevas en el esquema `crm_adapter`.

### API documental EspoCRM

- https://docs.espocrm.com/development/api/crud/
- https://docs.espocrm.com/development/api/metadata/
- https://docs.espocrm.com/administration/entity-manager/
