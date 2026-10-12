# Ejemplos de configuración CRM

## Bearer Token

```json
{
  "code": "CRM_A",
  "name": "CRM A",
  "baseUrl": "https://crm-a.example.com",
  "authType": "BEARER",
  "secretRef": "ENV:CRM_A_TOKEN",
  "defaultHeadersJson": "{\"Accept\":\"application/json\"}",
  "active": true
}
```

## API Key en header

```json
{
  "code": "CRM_B",
  "name": "CRM B",
  "baseUrl": "https://crm-b.example.com",
  "authType": "API_KEY_HEADER",
  "apiKeyName": "X-API-Key",
  "secretRef": "ENV:CRM_B_API_KEY",
  "active": true
}
```

## OAuth2 Client Credentials

`usernameRef` representa el client id y `secretRef` el client secret.

```json
{
  "code": "CRM_OAUTH",
  "name": "CRM OAuth",
  "baseUrl": "https://api.example.com",
  "authType": "OAUTH2_CLIENT_CREDENTIALS",
  "usernameRef": "ENV:CRM_CLIENT_ID",
  "secretRef": "ENV:CRM_CLIENT_SECRET",
  "tokenUrl": "https://identity.example.com/oauth/token",
  "scope": "tickets.write",
  "active": true
}
```

## Operación CREATE_TICKET

```json
{
  "code": "CREATE_TICKET",
  "httpMethod": "POST",
  "pathTemplate": "/v2/cases",
  "contentType": "application/json",
  "headersTemplateJson": "{\"X-Request-Id\":\"{{requestId}}\"}",
  "bodyTemplate": "{\"summary\":\"{{subject}}\",\"description\":\"{{body}}\",\"category\":\"{{decisions.PRODUCTO.value}}\"}",
  "successStatusCodes": "200,201",
  "externalIdPath": "data.case.id",
  "timeoutMs": 15000,
  "maxAttempts": 3,
  "retryDelayMs": 1000,
  "active": true
}
```

## Ruta por decisión

```json
{
  "profileCode": "TELCO_DEMO",
  "dimensionCode": "DESTINO",
  "optionCode": "SOPORTE",
  "integrationCode": "CRM_OAUTH",
  "operationCode": "CREATE_TICKET",
  "priority": 10,
  "active": true
}
```
