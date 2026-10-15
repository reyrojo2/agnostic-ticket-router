# AgnosticTicketRouter — Docker Compose y MVP académico

AgnosticTicketRouter recibe solicitudes no estructuradas, las transforma en eventos, aplica reglas/IA y envía tickets mediante un adaptador CRM configurable. El **arranque recomendado** para desarrollo y demostración es Docker Desktop, sin Eureka.

## Arranque en Windows con Docker Desktop

1. En la raíz del repositorio, copia `.env.example` a `.env` si todavía no existe. Completa `DB_PASSWORD`, `RABBITMQ_PASSWORD`, `ADMIN_PASSWORD`, `APP_JWT_SECRET` (mínimo 32 bytes) y los tres secretos `ESPOCRM_*` para inicializar una instalación nueva de EspoCRM. Mantén los secretos fuera de Git. Si ya usas los contenedores, **conserva las contraseñas de las bases existentes**: cambiarlas en `.env` no cambia automáticamente las credenciales persistidas.
2. Detén los JAR Java y `npm start` locales antes de ejecutar el stack, para liberar los puertos 8081–8085, 8090 y 4200. Mantén Docker Desktop abierto.
3. Desde PowerShell en la raíz:

```powershell
.\scripts\start.ps1
docker compose ps
```

4. Abre [Angular](http://localhost:4200), [EspoCRM](http://localhost:8080), [RabbitMQ](http://localhost:15672) y [Mailpit](http://localhost:8025).
5. Para detener **sin borrar datos**, ejecuta `.\scripts\stop.ps1`. No uses `docker compose down -v`.

El script mantiene **dos proyectos Compose**: el proyecto por defecto (`agnostic-ticket-router`) reúne PostgreSQL, RabbitMQ, Mailpit, seis microservicios Java y Angular; el proyecto separado `agnostic-espo` mantiene EspoCRM y su MariaDB, preservando sus volúmenes existentes. El primer build requiere descargar imágenes, dependencias Maven y paquetes npm, y puede tardar varios minutos.

### Puertos y nombres internos

| Componente | Windows | Desde otro contenedor del stack |
| --- | --- | --- |
| Angular + proxy Nginx | http://localhost:4200 | frontend-angular:80 |
| PostgreSQL | localhost:5433 | postgres:5432 |
| RabbitMQ / administración | localhost:5672 / http://localhost:15672 | rabbitmq:5672 |
| Mailpit SMTP / UI | localhost:1025 / http://localhost:8025 | mailpit:1025 |
| Business Knowledge | localhost:8082 | business-knowledge-service:8082 |
| Mail Ingestion | localhost:8081 | mail-ingestion-service:8081 |
| Decision Engine | localhost:8083 | decision-engine-service:8083 |
| CRM Adapter | localhost:8084 | crm-adapter-service:8084 |
| Notification Service | localhost:8085 | notification-service:8085 |
| Demo CRM | localhost:8090 | demo-crm-service:8090 |
| EspoCRM | http://localhost:8080 | host.docker.internal:8080 |
| Laya (opcional) | localhost:8000 | laya:8000 si usa perfil ai |

Angular usa URLs **relativas** y Nginx reenvía las llamadas a los microservicios. El comando `npm start` sigue disponible para desarrollo fuera de Docker mediante `frontend-angular/proxy.conf.json`.

### Laya opcional

Laya no forma parte del arranque básico porque su checkpoint requiere recursos y descarga propia. Si deseas intentar contenerizarlo, configura en `.env` `AI_BASE_URL_DOCKER=http://laya:8000` y ejecuta `.\scripts\start.ps1 -WithAI`. No se ha validado el comportamiento del paquete/checkpoint dentro de este entorno Docker; revisa logs de Laya y el endpoint `/health`. Si Laya ya funciona en Windows, deja `AI_BASE_URL_DOCKER=http://host.docker.internal:8000` y ejecuta sin `-WithAI`.

### Integraciones y datos existentes

Las rutas de integración CRM, canales SMTP y perfiles ya guardados en PostgreSQL **no se reescriben** al cambiar a Docker. Corrige manualmente en Angular las URLs antiguas `http://localhost:8090` (Demo CRM) y `http://localhost:8080` (EspoCRM): usa `http://demo-crm-service:8090` y `http://host.docker.internal:8080`, respectivamente. Para Mailpit en el canal de notificaciones usa host `mailpit`, puerto 1025, sin autenticación ni TLS.

EspoCRM sigue usando su MariaDB propia. Su correo SMTP saliente hacia Mailpit puede continuar usando `host.docker.internal:1025` con la excepción específica `emailServerAllowedAddressList` aplicada previamente al contenedor. No se conecta automáticamente un Gmail de entrada.

### Correcciones de arranque: Flyway y Decision Engine

Los servicios CRM Adapter y Notification fallaban al intentar crear sus tablas Flyway de historial en el esquema compartido `public`. Ahora cada módulo dispone de un esquema propio: `crm_adapter` y `notifications`. Business Knowledge conserva `public`, incluyendo el perfil `TELCO_DEMO`. Decision Engine también corrige el nombre duplicado del bean `knowledgeClient`.

**No se eliminan datos de `public`.** Si existen tablas heredadas `public.crm_*` o `public.notification_*` con configuraciones, **no se copian automáticamente** a los nuevos esquemas. Antes de migrarlas, revisa su contenido y respaldo. Los scripts Flyway crean registros de ejemplo en los nuevos esquemas.

Desde la rama `feat/docker-full-stack`, con Docker Desktop encendido:

```powershell
git pull
docker compose up -d --build crm-adapter-service notification-service decision-engine-service
docker compose ps
docker compose logs --tail=50 crm-adapter-service notification-service decision-engine-service
```

Comprobar los datos (ajusta el usuario si no es `router`):

```powershell
docker compose exec postgres psql -U router -d agnostic_router -c "SELECT table_schema, table_name FROM information_schema.tables WHERE table_name IN ('decision_profile','crm_integration','notification_channel') ORDER BY 1,2;"
docker compose exec postgres psql -U router -d agnostic_router -c "SELECT code, active FROM public.decision_profile;"
```

La configuración de `DEMO_CRM` puede necesitar sustituir su URL antigua `localhost:8090` por `http://demo-crm-service:8090` desde Angular. EspoCRM debe alcanzarse mediante `http://host.docker.internal:8080` desde el contenedor CRM Adapter.

Si Angular no muestra `TELCO_DEMO` aunque exista en SQL, inspecciona la respuesta HTTP de `/api/v1/admin/profiles` en DevTools antes de modificar la base de datos.

## Validación y resolución de problemas

```powershell
docker compose config --quiet
docker compose ps
docker compose logs --tail=100 business-knowledge-service
docker compose logs -f mail-ingestion-service decision-engine-service crm-adapter-service notification-service
docker compose -p agnostic-espo -f compose.espocrm.yml ps
```

Si falla la compilación, ejecuta `docker compose build --no-cache frontend-angular` o `docker compose build business-knowledge-service`, según el servicio afectado. Para las pruebas académicas usa Maven/JDK 21 y ejecuta `mvn clean test` fuera de Docker; el Dockerfile omite las pruebas **solo durante el empaquetado de imágenes**.

### Probar el flujo antes de integrar Gmail

Consulta [Plan de pruebas de correo y end-to-end](docs/EMAIL_TEST_PLAN.md). El formulario Angular **Demo de correo** publica una solicitud REST, no descarga mensajes de una bandeja real. Mailpit simula la **recepción SMTP saliente**. La futura lectura de Gmail/IMAP es una fase de desarrollo independiente, no una funcionalidad completada por esta Dockerización.

---

## Arquitectura y especificación funcional existente

AgnosticTicketRouter es una plataforma orientada a eventos para convertir solicitudes no estructuradas en **tickets estándar**, tomar decisiones configurables con reglas + IA y enviarlas a APIs externas sin acoplar el motor a un negocio o CRM concreto.

La versión 0.2 implementa tres niveles de configuración separados:

1. **Business Knowledge**: qué debe decidirse y qué significa cada alternativa.
2. **Decision Engine**: cómo se evalúan reglas e IA sin conocer el rubro.
3. **CRM Adapter / Notifications**: cómo se ejecutan integraciones y comunicaciones externas.

## Arquitectura

```text
Correo / REST
     |
     v
mail-ingestion-service :8081
     |
     v
RabbitMQ -> email.received
     |
     +---------------------> notification-service :8085
     |                           |
     v                           +--> LOG / SMTP
business-knowledge-service :8082
     ^
     | consulta perfiles
     |
decision-engine-service :8083 ----> System One (Laya / Kev / Jev compatible)
     |
     v
RabbitMQ -> ticket.standardized
     |
     v
crm-adapter-service :8084
     |
     +--> ruta configurable
     +--> auth configurable
     +--> plantilla JSON configurable
     +--> API CRM externa
     |
     v
RabbitMQ -> system.event
     |
     v
notification-service

Angular :4200
  -> Business Knowledge
  -> CRM Adapter Admin
  -> Notification Admin
  -> Demo de ingreso
```

## Módulos

- `common-contracts`: contratos de eventos compartidos.
- `mail-ingestion-service` `:8081`: recibe un correo por REST y publica `email.received`.
- `business-knowledge-service` `:8082`: perfiles, dimensiones, opciones y reglas en PostgreSQL. Emite JWT para el frontend.
- `decision-engine-service` `:8083`: aplica reglas HARD/SOFT y consulta un proveedor System One cuando corresponde.
- `crm-adapter-service` `:8084`: adaptador REST configurable para múltiples CRM/APIs.
- `notification-service` `:8085`: canales y plantillas configurables. Implementa `LOG` y `SMTP`.
- `demo-crm-service` `:8090`: API externa simulada para probar el adapter sin depender de un CRM real.
- `frontend-angular` `:4200`: administración visual y demo end-to-end.

## Requisitos

Backend:

- Java 21+
- Maven 3.9+
- PostgreSQL
- RabbitMQ
- Un proveedor compatible con `POST /v1/systemone` (por ejemplo Laya; el cliente queda desacoplado mediante `DecisionAiClient`)

Frontend:

- Node.js compatible con Angular 22. El proyecto usa Angular `^22.2.0`.
- `npm`

La infraestructura puede levantarse con Docker cuando esté disponible:

```bash
docker compose up -d
```

Incluye PostgreSQL, RabbitMQ y Mailpit. Docker **no es obligatorio para el código**: los servicios aceptan host/puerto por variables de entorno.

## Base de datos

Los tres servicios persistentes usan la misma base `agnostic_router`, pero cada dominio administra sus propias tablas:

- Business Knowledge: `decision_*`, `app_user`.
- CRM Adapter: `crm_*`.
- Notifications: `notification_*`.

Flyway usa historiales separados para CRM y Notification, mientras Business Knowledge conserva el historial estándar para mantener compatibilidad con el starter anterior.

> Si ya ejecutaste una versión intermedia que usó otra tabla de historial Flyway, lo más simple en desarrollo es recrear la base antes de probar v0.2.

## Seguridad y login

El usuario administrativo inicial se crea en PostgreSQL al primer arranque usando `ADMIN_USER` y `ADMIN_PASSWORD`. La contraseña se almacena únicamente como hash BCrypt. El repositorio no contiene una contraseña predeterminada.

Login:

```http
POST http://localhost:8082/api/auth/login
Content-Type: application/json

{
  "username": "<ADMIN_USER>",
  "password": "<ADMIN_PASSWORD>"
}
```

La respuesta contiene JWT. El mismo JWT se valida en:

- Business Knowledge Admin.
- CRM Adapter Admin.
- Notification Admin.

Todos deben compartir la misma variable:

```text
APP_JWT_SECRET=<valor-definido-en-el-entorno>
```

El valor debe tener al menos 32 bytes y no se almacena en el repositorio.

## Business Knowledge genérico

El motor no contiene clases `ProductClassifier`, `BankClassifier`, etc. Consume una colección de dimensiones.

El perfil demo contiene:

```text
TELCO_DEMO
  1. PRODUCTO
  2. TIPO_GESTION
  3. PRIORIDAD
  4. DESTINO
```

Otro perfil puede tener cualquier número de dimensiones.

Tipos soportados:

- `CHOICE`
- `SCORE`
- `NOUL`

Las opciones contienen:

- `description`
- `includeContext`
- `excludeContext`
- prioridad
- dependencias

Las reglas pueden ser:

- `HARD`: si existe un único resultado consistente, se decide sin IA.
- `SOFT`: no fuerza una decisión; se agrega como `business_hints` al estado enviado al motor de IA.

Tipos de regla:

- `CONTAINS_ANY`
- `CONTAINS_ALL`
- `REGEX`

El Decision Engine también envía a la IA las decisiones ya resueltas en `resolved_decisions`, permitiendo decisiones jerárquicas.

### CRUD Business Knowledge

```text
GET/POST        /api/v1/admin/profiles
PUT/DELETE      /api/v1/admin/profiles/{id}
GET/POST        /api/v1/admin/profiles/{profileId}/dimensions
PUT/DELETE      /api/v1/admin/dimensions/{id}
GET/POST        /api/v1/admin/dimensions/{dimensionId}/options
PUT/DELETE      /api/v1/admin/options/{id}
GET/POST        /api/v1/admin/dimensions/{dimensionId}/rules
PUT/DELETE      /api/v1/admin/rules/{id}
```

Runtime público usado por el Decision Engine:

```http
GET /api/v1/runtime/profiles/{profileCode}
```

## Decision Engine y proveedor de IA

El Decision Engine genera preguntas a partir de la configuración de Business Knowledge. La capa Java depende de `DecisionAiClient`, no de una clase de negocio específica. La implementación incluida usa el contrato `/v1/systemone`, por lo que puede apuntar a Laya y a otros proveedores compatibles cambiando configuración.

Para `CHOICE`, una decisión automática requiere inicialmente:

```text
p(primera) >= autoMinProbability
AND
p(primera) - p(segunda) >= minMargin
AND
el proveedor no marcó low_confidence
AND
el input no fue truncado
```

Los valores son configurables por dimensión.

Si una dimensión requerida no es aceptada:

```text
StandardTicketEvent.requiresHumanReview = true
```

En ese caso el CRM Adapter no ejecuta la API y emite `REVIEW_REQUIRED`.

## CRM Adapter genérico

El Adapter ya no contiene lógica `SalesforceAdapter`, `ZendeskAdapter`, etc. Se configura en PostgreSQL.

### `crm_integration`

Define:

- código y nombre
- `baseUrl`
- autenticación
- headers comunes
- referencia a secretos

Auth soportada:

- `NONE`
- `BASIC`
- `BEARER`
- `API_KEY_HEADER`
- `OAUTH2_CLIENT_CREDENTIALS`

Las credenciales no se resuelven por nombre de CRM. Se utilizan referencias:

```text
ENV:CRM_TOKEN
ENV:CRM_USER
```

Para producto real se recomienda sustituir `SecretResolver` por Secrets Manager, Vault o equivalente.

### `crm_operation`

Define una operación REST:

- método HTTP
- path
- `Content-Type`
- headers específicos
- body template
- códigos HTTP de éxito
- path del identificador externo
- timeout
- cantidad de intentos
- espera entre reintentos

Ejemplo de body configurable:

```json
{
  "title": "{{subject}}",
  "description": "{{body}}",
  "requester": "{{sender}}",
  "product": "{{decisions.PRODUCTO.value}}",
  "priority": "{{decisions.PRIORIDAD.value}}"
}
```

Si un valor JSON completo es un placeholder, el renderer conserva su tipo JSON. Si el placeholder forma parte de un texto, se interpola como texto.

### Lectura de respuesta

`externalIdPath` admite:

```text
data.caseNumber
```

o JSON Pointer:

```text
/data/caseNumber
```

### `crm_route`

Selecciona integración y operación según perfil y, opcionalmente, una decisión.

Ruta general:

```text
profileCode = TELCO_DEMO
dimensionCode = null
optionCode = null
integrationCode = DEMO_CRM
operationCode = CREATE_TICKET
```

Ruta condicional:

```text
profileCode = TELCO_DEMO
dimensionCode = DESTINO
optionCode = SOPORTE
integrationCode = CRM_SOPORTE
operationCode = CREATE_CASE
```

La prioridad menor se evalúa primero.

### CRUD CRM

```text
GET/POST        /api/v1/admin/crm/integrations
PUT/DELETE      /api/v1/admin/crm/integrations/{id}
GET/POST        /api/v1/admin/crm/integrations/{id}/operations
PUT/DELETE      /api/v1/admin/crm/operations/{id}
GET/POST        /api/v1/admin/crm/routes
PUT/DELETE      /api/v1/admin/crm/routes/{id}
```

## Demo CRM

El servicio `demo-crm-service` simula un CRM real:

```http
POST http://localhost:8090/api/tickets
```

Responde:

```json
{
  "success": true,
  "data": {
    "caseNumber": "CASE-1001"
  }
}
```

El seed del CRM Adapter ya está configurado para extraer:

```text
data.caseNumber
```

Esto permite demostrar que cambiar la API destino es configuración y no lógica del Decision Engine.

## Notification Service

La configuración está separada en:

### `notification_channel`

Tipos actuales:

- `LOG`: desarrollo/demo.
- `SMTP`: correo real.

SMTP soporta:

- host
- port
- username/password mediante referencias `ENV:`
- SMTP Auth
- STARTTLS
- from address

### `notification_template`

Configurable por:

- `profileCode`
- `eventType`
- `channelCode`
- destinatario
- asunto
- cuerpo

Eventos demo:

- `EMAIL_RECEIVED`
- `CRM_CREATED`
- `REVIEW_REQUIRED`
- `CRM_ERROR`

Ejemplo:

```text
recipientTemplate = {{recipient}}
subjectTemplate   = Ticket creado - {{requestId}}
bodyTemplate      = Ticket externo: {{data.externalTicketId}}
```

Si no existe plantilla específica para el perfil, se busca una con `profileCode = *`.

### CRUD Notification

```text
GET/POST        /api/v1/admin/notifications/channels
PUT/DELETE      /api/v1/admin/notifications/channels/{id}
GET/POST        /api/v1/admin/notifications/templates
PUT/DELETE      /api/v1/admin/notifications/templates/{id}
```

## Frontend Angular

Carpeta:

```text
frontend-angular/
```

Pantallas implementadas:

- Login JWT.
- Dashboard.
- Business Knowledge.
- Integraciones CRM.
- Notificaciones.
- Demo de ingreso de correo.

Instalación:

```bash
cd frontend-angular
npm install
npm start
```

Abrir:

```text
http://localhost:4200
```

Las URLs de los microservicios están centralizadas en:

```text
frontend-angular/src/app/core/api.config.ts
```

## Compilar backend

Desde la raíz:

```bash
mvn clean package
```

Se incluye una prueba JPA académica que valida insertar, listar, actualizar y eliminar `DecisionProfile`.

## Orden recomendado de ejecución

Infraestructura:

```bash
docker compose up -d
```

Luego el proveedor de IA. Para Laya consulta `laya/README.md`.

Backend, en terminales separadas:

```bash
mvn -pl business-knowledge-service spring-boot:run
mvn -pl decision-engine-service spring-boot:run
mvn -pl mail-ingestion-service spring-boot:run
mvn -pl crm-adapter-service spring-boot:run
mvn -pl notification-service spring-boot:run
mvn -pl demo-crm-service spring-boot:run
```

Frontend:

```bash
cd frontend-angular
npm install
npm start
```

Finalmente abre Angular y usa **Demo de correo**.

## Sin Docker / Windows sin administrador

Docker no forma parte del código de negocio. Puedes usar servicios instalados o remotos configurando:

```text
DB_URL
DB_USER
DB_PASSWORD
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USER
RABBITMQ_PASSWORD
AI_BASE_URL
AI_API_KEY
AI_MODEL
AI_LANG
```

La limitación práctica es que para ejecutar el flujo completo necesitas acceso a PostgreSQL y RabbitMQ en alguna máquina.

## Qué queda fuera de v0.2

La versión ya implementa configuración genérica para REST/JSON, notificación SMTP y frontend. Aún quedan como evoluciones:

- ingestión IMAP/Microsoft Graph/Gmail Webhook real;
- adjuntos binarios end-to-end;
- APIs SOAP o multipart específicas;
- API key por query string;
- OAuth2 flows distintos de Client Credentials;
- DLQ y reintentos administrados por RabbitMQ;
- idempotencia persistente ante reentregas;
- auditoría histórica de cambios de configuración;
- pantalla de revisión humana de tickets dudosos;
- gestión de usuarios/roles desde Angular;
- secret manager real;
- observabilidad distribuida (trace/correlation ID).

Estas limitaciones no afectan la demostración académica de Spring, REST, persistencia, seguridad, Angular, RabbitMQ y arquitectura de microservicios.
