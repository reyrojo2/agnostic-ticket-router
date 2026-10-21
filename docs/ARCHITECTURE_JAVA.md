# Arquitectura uniforme de microservicios Java

Todos los microservicios de negocio ejecutan Java 21 y Spring Boot, se empaquetan con Maven y se construyen mediante `Dockerfile.backend`. No se ejecutan `app.py` ni servidores FastAPI. `laya/` es una dependencia externa opcional (servidor de inferencia Python) y **no** es uno de los siete microservicios Java de AgnosticTicketRouter.

## Convención de paquetes

```
com.agnosticrouter.<contexto>/
  <Servicio>Application.java   # única clase de arranque
  api/                        # controladores HTTP, request/response y validación
  service/                    # casos de uso y coordinación de negocio
  domain/                     # entidades y tipos de dominio, sin JDBC/HTTP/RabbitMQ
  repo/                       # repositorios/persistencia
  gateway/                    # entrada/salida por RabbitMQ y servicios externos
  config/                     # configuración Spring, seguridad, clientes
```

Un servicio utiliza únicamente los paquetes que necesita. Ni Eureka Server ni API Gateway requieren capas de dominio ficticias. Los servicios no deben importar clases internas de otros microservicios; comparten solo eventos de `common-contracts` y contratos REST.

## Mapa de responsabilidades

| Módulo | API o consumidor | Caso de uso | Adaptadores y persistencia |
| --- | --- | --- | --- |
| Gmail Connector | `api/GmailController` | `service/GmailService` | `gateway/GoogleMailRestClient`, `gateway/RestIntakePublisher`, `repo/GmailRepository` |
| Mail Ingestion | `api/EmailIntakeController` | `service/EmailIntakeService` | `gateway/RabbitIncomingEmailPublisher` |
| Business Knowledge | `api/*Controller` | `service/RuntimeConfigService` | `repo/*Repository` |
| Decision Engine | `gateway/DecisionListener` | `GenericDecisionEngine` (motor de dominio pendiente de separación adicional) | `KnowledgeClient`, `SystemOneDecisionClient` |
| CRM Adapter | `api/CrmAdminController`, `gateway/CrmAdapterListener` | `service/CrmRouter`, `service/CrmHttpExecutor` | `repo/*Repository` |
| Notification Service | `api/NotificationAdminController`, `gateway/NotificationListener` | `service/NotificationDispatcher` | `repo/*Repository`, SMTP |
| Observability | `api/MetricsController`, `gateway/TelemetryConsumer` | `service/MetricsService` | `repo/RequestTraceRepository` |

## Reglas SOLID y límites de cambio

- **Responsabilidad única:** el controlador traduce HTTP; el servicio coordina; los repositorios gestionan datos; los adaptadores acceden a sistemas externos.
- **Inversión de dependencias:** los casos de uso dependen de contratos/puertos cuando la dependencia externa debe ser sustituible. La ingesta usa `IncomingEmailPublisher` y su implementación Rabbit.
- **Sustitución y extensión:** proveedores de IA y Gmail se intercambian mediante interfaces (`DecisionAiClient`, `GoogleMailClient`), sin alterar el flujo principal.
- **Métodos descriptivos:** nombres verbales (`receive`, `publish`, `resolveHard`, `dispatch`) indican una acción concreta; evitar abreviaturas al refactorizar.
- **Compatibilidad:** no modificar nombres de servicios Eureka, endpoints `/api/...`, routing keys RabbitMQ, esquemas PostgreSQL ni estructura de payloads.
- **Idempotencia:** conservar la detección de mensajes Gmail y `requestId`; no alterar clasificación HARD/SOFT ni cuotas de IA.
- **Verificación:** `mvn test`, `docker compose config`, construcción de imágenes y pruebas de extremo a extremo con Gmail/EspoCRM antes de desplegar.

La normalización inicial elimina implementaciones Python ya inactivas y coloca controladores, listeners y repositorios en capas coherentes. No convierte automáticamente las clases extensas restantes en una arquitectura hexagonal completa: por ejemplo, `GenericDecisionEngine`, `CrmHttpExecutor` y `NotificationDispatcher` aún necesitan particiones en cambios posteriores, con pruebas de contrato para no alterar su comportamiento.

## Segunda fase: casos de uso y adaptadores

La segunda fase separa los controladores administrativos de los casos de uso de Knowledge, CRM y Notificaciones. Los consumidores RabbitMQ no llaman directamente al transporte HTTP en CRM: dependen del puerto `CrmExecutionPort` con implementación `CrmHttpExecutor`.

- **Knowledge:** `KnowledgeAdminService` ejecuta administración; `AuthenticationService` autentica y emite JWT; los controllers adaptan HTTP.
- **Decision Engine:** `HardRuleDecisionResolver` convierte reglas HARD concluyentes en decisiones, sin invocar IA. `GenericDecisionEngine` mantiene la evaluación de dimensiones SOFT/IA y el orden de dependencias.
- **CRM Adapter:** `CrmExecutionPort` es el contrato interno que utiliza `CrmAdapterListener`; `CrmHttpExecutor` es la implementación que gestiona llamadas externas.
- **Notifications:** `NotificationSender` permite estrategias distintas sin condicionales de infraestructura en el caso de uso: `SmtpNotificationSender` y `LogNotificationSender`.
- **Gmail Connector:** `GmailOAuthService` administra consentimiento/tokens, mientras `GmailService` sincroniza mensajes y registra identificadores.
- **Observability:** `MetricsService` coordina la lectura y `MetricsReportBuilder` agrega indicadores puros.
- **Mail Ingestion:** `EmailIntakeService` construye el evento sin conocer RabbitMQ; lo publica mediante `IncomingEmailPublisher` (ya integrado en el PR anterior).

### Dependencias permitidas

```text
api ───────> service ───────> domain / puertos
gateway ───> service / puertos <──── config (inyección Spring)
repo ──────> domain (persistencia propia del contexto)
```

Los puertos son propios del microservicio, **no** se trasladan a `common-contracts`, que contiene solamente contratos de integración/eventos. No se importa `domain` o `repo` de otro servicio. Eureka y API Gateway siguen siendo infraestructura sin capas artificiales.

Se mantienen los mismos endpoints, tópicos RabbitMQ, tablas, formatos de eventos y nombres Eureka. No se introducen límites artificiales de Laya ni se modifica la semántica HARD/SOFT. La validación de CI (compilación, unit tests, Compose y build de imágenes) **no reemplaza** las pruebas funcionales con Gmail/EspoCRM en el entorno local.
