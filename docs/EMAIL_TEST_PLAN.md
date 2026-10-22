# Prueba de correo y flujo integral

## Situacion actual
- Implementado: ingreso REST (`POST /api/v1/intake/emails`), RabbitMQ, motor de decisiones, API CRM y notificaciones SMTP.
- Implementado: lectura automática desde Gmail API mediante OAuth2 de solo lectura, sincronización configurable desde Angular y deduplicación por (correo, Gmail message ID) en PostgreSQL. **Mailpit solo captura SMTP saliente; no recibe correos por IMAP.**
- No habilitar Email-to-Case nativo de EspoCRM para evaluar el router: omitiría el motor.

## Validar sin Gmail primero
1. Iniciar: `./scripts/start.ps1`. Para Laya en Docker: configurar `AI_BASE_URL_DOCKER=http://laya:8000` y `./scripts/start.ps1 -WithAI`. El contenedor opcional requiere descargar un checkpoint y su compatibilidad está pendiente de validación.
2. Abrir `http://localhost:4200`, verificar login y configuraciones del perfil `TELCO_DEMO`.
3. En el adaptador CRM, configurar `http://host.docker.internal:8080` para EspoCRM (el adaptador corre en Docker; `localhost` sería el propio contenedor). Para Demo CRM usar `http://demo-crm-service:8090` en vez del localhost sembrado inicialmente en la BD.
4. Canal de notificación SMTP: host `mailpit`, puerto 1025, sin STARTTLS ni SMTP Auth.
5. Enviar desde Angular → Demo de correo; anotar el `requestId`. HTTP 202 significa aceptado, no ticket creado.
6. Consultar `docker compose logs -f mail-ingestion-service decision-engine-service crm-adapter-service notification-service`; confirmar Case en EspoCRM y correo en Mailpit.

## Validación de Gmail OAuth y deduplicación en ejecución real
1. Crear buzón Gmail de prueba dedicado y autenticar con OAuth2 usando el panel Angular /gmail.
2. Desde Angular /gmail elegir No leídos / Últimas 24 horas / Toda la bandeja y, de ser necesario, filtrar por asunto. Guardar con lectura habilitada.
3. Pulsar Consultar ahora dos veces: la primera ejecución puede publicar mensajes nuevos; la segunda debe mostrar los IDs existentes como omitidos y 0 nuevas publicaciones. El conector NO marca los mensajes como leídos.
4. Verificar el Case en EspoCRM y la fila (`email`, `message_id`, `request_id`) en `gmail_connector.processed`; si una publicación falla antes del registro puede ser reintentada.

La lectura Gmail API está implementada; su funcionamiento real requiere configurar OAuth y ejecutar la integración en tu entorno. El conector lista IDs cada vez, pero omite aquellos registrados en gmail_connector.processed.
