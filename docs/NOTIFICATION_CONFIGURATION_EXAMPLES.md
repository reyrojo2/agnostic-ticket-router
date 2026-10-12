# Ejemplos de notificación

## Canal SMTP

```json
{
  "code": "SMTP_CORPORATIVO",
  "name": "SMTP Corporativo",
  "type": "SMTP",
  "host": "smtp.example.com",
  "port": 587,
  "usernameRef": "ENV:SMTP_USER",
  "passwordRef": "ENV:SMTP_PASSWORD",
  "fromAddress": "no-reply@example.com",
  "smtpAuth": true,
  "startTls": true,
  "active": true
}
```

## Confirmación de CRM

```json
{
  "profileCode": "TELCO_DEMO",
  "eventType": "CRM_CREATED",
  "channelCode": "SMTP_CORPORATIVO",
  "recipientTemplate": "{{recipient}}",
  "subjectTemplate": "Tu ticket fue creado - {{requestId}}",
  "bodyTemplate": "Tu caso fue registrado. Ticket externo: {{data.externalTicketId}}",
  "active": true
}
```
