# Business Knowledge -> Decision Engine -> EspoCRM (mapeo de clasificación)

**Diagnóstico**: `StandardTicketEvent.decisions` lleva una decisión por dimensión (`dimensionCode`, `value`, `source`, aceptación/probabilidades). `CrmRouter.matches` utiliza esas decisiones para elegir la ruta CRM. No las copia automáticamente a EspoCRM. `CrmHttpExecutor` envía exclusivamente los campos definidos en `crm_operation.body_template`. Por tanto, con `name` y `description` originales EspoCRM solo muestra asunto y cuerpo, aunque se haya clasificado correctamente.

## Solución segura sin modificar EspoCRM

`TemplateRenderer` ofrece `{{classificationSummary}}` y sigue admitiendo tokens como `{{decisions.DESTINO.value}}`, `{{requestId}}`, `{{profileCode}}`. El resumen es dinámico: muestra *todas* las dimensiones que contenga el evento, ordenadas por código; no presupone que todas las organizaciones utilicen PRODUCTO/TIPO_GESTION/PRIORIDAD/DESTINO.

```json
{
  "name": "{{subject}}",
  "description": "{{body}}\n\n--- Clasificación AgnosticTicketRouter ---\n{{classificationSummary}}\nSolicitud: {{requestId}}\nPerfil: {{profileCode}}"
}
```

Flyway V4 solo actualiza la **plantilla original exacta** sembrada por V3, no sobreescribe plantillas modificadas por administradores. Si en Angular ves `title`/`description` como en la captura, edita la operación `CREATE_CASE`, pulsa **Usar plantilla EspoCRM con clasificación** y **Guardar operación**. Las modificaciones aplican a **casos futuros**, no a Cases ya creados.

## Campos separados y asignación de equipos

Para ver dimensiones como columnas/campos de Cases en EspoCRM, un administrador debe crearlos en Entity Manager → Case → Fields (por ejemplo un campo `cProductCode`), y agregar sus nombres API correctos a la plantilla JSON. *No inventes nombres*: verifica el esquema de tu instancia. Ejemplo, solo si ya existe el campo:

```json
{"name":"{{subject}}","description":"{{body}}","cProductCode":"{{decisions.PRODUCTO.value}}"}
```

La opción DESTINO=BACKOFFICE en Business Knowledge **no equivale automáticamente** a un `Team` de EspoCRM. La asignación requiere crear equipos y/o usuarios en EspoCRM, obtener sus IDs reales y mapearlos mediante rutas/operaciones, o implementar una capa configurada de resolución de IDs. EspoCRM acepta enlaces a equipos por campos/API específicos de su versión, pero no se debe inventar un ID.

## Gmail: una consulta no es un reprocesamiento

Gmail lista ID por el filtro (`in:inbox` y variantes). El conector consulta `gmail_connector.processed` por (`email`, `message_id`) **antes de descargar** el mensaje y de publicarlo. Luego del `202` guarda ese ID en PostgreSQL. La respuesta de `/sync` muestra `listed`, `alreadyProcessed` y `processed` para distinguir consultas repetidas de publicaciones nuevas. Los mensajes no se marcan como leídos (scope OAuth de solo lectura). Las consultas reiteradas de la misma bandeja pueden costar llamadas a Gmail, pero no deben producir casos nuevos mientras el historial de IDs esté intacto. La operación no es exactamente-una-vez si falla después de publicar pero antes de registrar; mantén respaldo, idempotencia aguas abajo y un buzón de pruebas.
