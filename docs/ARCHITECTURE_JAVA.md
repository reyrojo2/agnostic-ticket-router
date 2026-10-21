# Arquitectura uniforme de microservicios Java

Todos los microservicios de negocio ejecutan Java 21 y Spring Boot, se empaquetan con Maven y se construyen mediante `Dockerfile.backend`. No se ejecutan `app.py` ni servidores FastAPI. `laya/` es una dependencia externa opcional (servidor de inferencia Python) y **no** es uno de los siete microservicios Java de AgnosticTicketRouter.

## Convención de paquetes vigente

La estructura actual, la distribución real de clases y los puertos están documentados en
[`PACKAGE_LAYOUT.md`](PACKAGE_LAYOUT.md). A diferencia de la versión inicial, los casos de
uso dependen de interfaces de `application/port`, implementadas por adaptadores en
`infrastructure/`. Los controladores permanecen en `api/controller`.

`common-contracts` incluye solo eventos y tipos de integración compartidos, nunca entidades
JPA, repositorios o servicios de dominio de otros microservicios.

## Reglas SOLID y límites de cambio

- **Responsabilidad única:** el controlador traduce HTTP; el servicio coordina; los repositorios gestionan datos; los adaptadores acceden a sistemas externos.
- **Inversión de dependencias:** los casos de uso dependen de contratos/puertos cuando la dependencia externa debe ser sustituible. La ingesta usa `IncomingEmailPublisher` y su implementación Rabbit.
- **Sustitución y extensión:** proveedores de IA y Gmail se intercambian mediante interfaces (`DecisionAiClient`, `GoogleMailClient`), sin alterar el flujo principal.
- **Métodos descriptivos:** nombres verbales (`receive`, `publish`, `resolveHard`, `dispatch`) indican una acción concreta; evitar abreviaturas al refactorizar.
- **Compatibilidad:** no modificar nombres de servicios Eureka, endpoints `/api/...`, routing keys RabbitMQ, esquemas PostgreSQL ni estructura de payloads.
- **Idempotencia:** conservar la detección de mensajes Gmail y `requestId`; no alterar clasificación HARD/SOFT ni cuotas de IA.
- **Verificación:** `mvn test`, `docker compose config`, construcción de imágenes y pruebas de extremo a extremo con Gmail/EspoCRM antes de desplegar.

El reordenamiento conserva los contratos y traslada las integraciones detrás de puertos. Esta actualización no garantiza transacciones distribuidas ni idempotencia de efectos exactamente una vez; las pruebas de integración y la estrategia outbox siguen siendo necesarias.

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
api/controller ──────┐
infrastructure/messaging ──┼──> application/service ──> application/port + domain
                          │                               ^
infrastructure/client ───┘                      infrastructure/* (adapters)
config ───────────────────────────────────> configuración/inyección Spring
```

Los puertos son propios del microservicio, **no** se trasladan a `common-contracts`, que contiene solamente contratos de integración/eventos. No se importa `domain` o `repo` de otro servicio. Eureka y API Gateway siguen siendo infraestructura sin capas artificiales.

Se mantienen los mismos endpoints, tópicos RabbitMQ, tablas, formatos de eventos y nombres Eureka. No se introducen límites artificiales de Laya ni se modifica la semántica HARD/SOFT. La validación de CI (compilación, unit tests, Compose y build de imágenes) **no reemplaza** las pruebas funcionales con Gmail/EspoCRM en el entorno local.

## Tercera fase: núcleo, puertos y controles automatizados

El código del motor de decisiones aplica la siguiente separación:

- `GenericDecisionEngine.evaluate()`: coordina el orden de las dimensiones y conserva HARD > SOFT/IA.
- `HardRuleDecisionResolver.resolve()`: política determinística; una HARD concluyente evita cualquier consulta a IA en esa dimensión.
- `DimensionDependencyPolicy.satisfied()`: restricciones entre dimensiones y opciones.
- `DecisionContextBuilder.buildState()`: contexto y pistas SOFT para la IA.
- `AiDimensionEvaluator.evaluate()`: construcción e interpretación de solicitudes `SystemOne`; usa `DecisionAiClient` sustituible.
- `ProcessIncomingEmail.process()`: caso de uso y comprobación de idempotencia propia.
- `ProcessedDecisionRepository`: solo consulta `decision_engine.processed_request`, nunca esquemas internos de Observability.
- `ClassifiedTicketPublisher`: puerto de salida implementado por `RabbitClassifiedTicketPublisher`.
- `DecisionListener.handle()`: adaptador de entrada AMQP y cuarentena de fallos.

La ejecución CRM sigue el mismo principio: `CrmAdapterListener` delega en `ProcessClassifiedTicket`, que depende de los puertos `CrmExecutionPort` y `CrmOutcomePublisher`. `CrmHttpExecutor`, `CrmAuthenticationConfigurer` y `RabbitCrmOutcomePublisher` son adaptadores técnicos propios del servicio.

Gmail separa `GmailOAuthService` (tokens), `GmailService` (planificación/sincronización) y `GmailMessageProcessor` (validación, deduplicación, publicación de una entrada).

Los siete módulos de negocio incluyen pruebas ArchUnit de separación de capas (controladores ↛ persistencia, aplicación ↛ infraestructura/API/config, dominio ↛ aplicación/infraestructura/API/config) y aislamiento entre microservicios. Las pruebas no reemplazan las pruebas de contrato ni las de integración.

### Requisitos de despliegue y garantías

**Antes de arrancar el nuevo Decision Engine**, aplicar la migración de IDs históricos descrita en `docs/DECISION_IDEMPOTENCY_MIGRATION.md`. La aplicación no ejecuta SQL sobre Observability durante operación normal.

Este cambio mejora SRP y DIP, pero **no garantiza entrega exactamente una vez**: publicación AMQP y registro de procesado no comparten transacción. Para ese requisito aún se necesitan outbox transaccional, confirmaciones de RabbitMQ y deduplicación del lado receptor (especialmente CRM y notificaciones). La ejecución real con PostgreSQL, RabbitMQ, Gmail y EspoCRM debe validarse después de desplegar; las pruebas Maven/CI no sustituyen esa validación.
