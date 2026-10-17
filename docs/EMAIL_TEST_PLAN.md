# Prueba de correo y flujo integral

## Situacion actual
- Implementado: ingreso REST (`POST /api/v1/intake/emails`), RabbitMQ, motor de decisiones, API CRM y notificaciones SMTP.
- Pendiente: lectura automatica desde Gmail API, IMAP o Microsoft Graph. **Mailpit solo captura SMTP saliente; no recibe correos por IMAP**.
- No habilitar Email-to-Case nativo de EspoCRM para evaluar el router: omitiría el motor.

## Validar sin Gmail primero
1. Iniciar: `./scripts/start.ps1`. Para Laya en Docker: configurar `AI_BASE_URL_DOCKER=http://laya:8000` y `./scripts/start.ps1 -WithAI`. El contenedor opcional requiere descargar un checkpoint y su compatibilidad está pendiente de validación.
2. Abrir `http://localhost:4200`, verificar login y configuraciones del perfil `TELCO_DEMO`.
3. En el adaptador CRM, configurar `http://host.docker.internal:8080` para EspoCRM (el adaptador corre en Docker; `localhost` sería el propio contenedor). Para Demo CRM usar `http://demo-crm-service:8090` en vez del localhost sembrado inicialmente en la BD.
4. Canal de notificación SMTP: host `mailpit`, puerto 1025, sin STARTTLS ni SMTP Auth.
5. Enviar desde Angular → Demo de correo; anotar el `requestId`. HTTP 202 significa aceptado, no ticket creado.
6. Consultar `docker compose logs -f mail-ingestion-service decision-engine-service crm-adapter-service notification-service`; confirmar Case en EspoCRM y correo en Mailpit.

## Fase posterior: lectura de correo real
1. Crear buzón de prueba dedicado y autenticar por OAuth 2.0 para Gmail API (o IMAP autorizado).
2. Implementar el adaptador de entrada y convertir cada mensaje en `IncomingEmailEvent`, con metadata de identificador de mensaje.
3. Persistir deduplicación/estado antes de marcar mensajes como tratados; reintentos, trazabilidad y revisión humana.
4. Probar con datos sintéticos y luego validar end-to-end antes de usar información de clientes.

No se afirma que esta entrega ya lea mensajes entrantes de Gmail.
