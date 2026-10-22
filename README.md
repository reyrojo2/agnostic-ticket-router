# Agnostic Ticket Router

**Plataforma configurable de clasificación, enrutamiento y seguimiento de solicitudes.**

> **Estado:** MVP académico funcional, en evolución hacia un posible producto SaaS. Este repositorio **no es una edición SaaS multiempresa lista para producción**. Versión Maven: `0.2.0-SNAPSHOT`.
>
> **Tecnologías principales:** Java 21 · Spring Boot 3.4.5 · Spring Cloud 2024.0.3 · Angular 22 · PostgreSQL 16 · RabbitMQ · Docker Compose · EspoCRM (integración de demostración).

## ¿Qué es y qué problema resuelve?

Muchas organizaciones reciben solicitudes no estructuradas por correo, deben identificar manualmente su tipo, prioridad y área responsable y luego crear un caso en un CRM. Esto genera trabajo repetitivo, retrasos, errores de clasificación y escasa trazabilidad.

**Agnostic Ticket Router** separa la definición de las reglas de negocio del motor que toma decisiones y del adaptador que registra el caso en un CRM externo. Permite configurar perfiles de clasificación para distintos procesos sin crear una clase Java por cada tipología.

El sistema implementa:

- **Business Knowledge:** perfiles, dimensiones, opciones, dependencias y reglas HARD/SOFT.
- **Decision Engine:** decisiones determinísticas primero y proveedor de IA de decisión **SystemOne** cuando sea necesario; revisión cuando una dimensión obligatoria no se resuelve con confianza.
- **Gmail Connector / Mail Ingestion:** lectura autorizada de Gmail o ingreso REST, publicación en RabbitMQ y control de mensajes procesados.
- **CRM Adapter:** integración REST configurable, mapeo de dimensiones a campos reales, equivalencias de valores, asignaciones de equipos/responsables, catálogo EspoCRM, validación y vista previa del JSON.
- **Notifications:** canales LOG/SMTP, plantillas por evento y perfil, vista previa, envío del número público del caso y credenciales SMTP cifradas.
- **Observability:** estados, indicadores, seguimiento por `requestId` y diagnósticos de clasificación.

El proyecto incluye **Angular** para administración y demostración, **Spring Cloud Gateway** como entrada HTTP y **Eureka** para descubrimiento de servicios.

## Flujo de extremo a extremo

```text
Gmail (OAuth) ──> Gmail Connector ──┐
                                   ├──> Mail Ingestion ──> RabbitMQ (correo recibido)
REST / formulario de prueba ────────┘                              |
                                                                    v
                                               Decision Engine <── Business Knowledge
                                              reglas HARD / SOFT    perfiles y dimensiones
                                                    | 
                                           SystemOne (opcional)
                                                    |
                                       decisión válida o revisión
                                                    |
                                  RabbitMQ (ticket estandarizado)
                                                    |
                                                    v
                                             CRM Adapter
                                  rutas + mapeos + equipo/responsable
                                                    |
                                               REST EspoCRM
                                                    |
                                   eventos de resultado / CRM_CREATED
                                                    |
                                        Notifications + Observability

Angular ──> Nginx / Spring Cloud Gateway ──> Eureka / APIs administrativas
```

1. La ingesta acepta una solicitud y publica un evento; **HTTP 202 no confirma** que el caso exista en el CRM.
2. Decision Engine consulta la configuración del perfil y resuelve cada dimensión mediante reglas HARD concluyentes antes de solicitar IA.
3. Las reglas SOFT aportan pistas; el proveedor SystemOne puede completar dimensiones pendientes. Se comprueban los umbrales de confianza y margen.
4. Si el ticket requiere revisión, **no se crea automáticamente** un caso como si hubiese sido clasificado correctamente.
5. Si es apto, CRM Adapter selecciona una ruta, genera el JSON según los mapeos configurados y llama a la API externa.
6. El resultado CRM, incluido su **ID técnico** y su **número público** cuando exista, alimenta notificaciones y observabilidad.

### Correo Gmail: lectura e idempotencia

Gmail Connector identifica los mensajes mediante **cuenta + ID de Gmail**, consulta PostgreSQL y omite los que ya están registrados como publicados. La interfaz distingue IDs listados, omitidos y nuevos. **No marca automáticamente como leídos los correos**; el conector OAuth es de solo lectura. Volver a consultar la bandeja no implica reprocesar todos los mensajes.

La publicación en RabbitMQ y el registro de procesado no forman una sola transacción; pueden producirse reintentos en fallos excepcionales. La plataforma no promete procesamiento *exactly once*. Consulta [plan de pruebas del correo](docs/EMAIL_TEST_PLAN.md) y [migración de idempotencia](docs/DECISION_IDEMPOTENCY_MIGRATION.md) antes de utilizar bases históricas.

## Arquitectura y módulos

| Módulo / servicio | Función |
| --- | --- |
| `frontend-angular` | Administración y visualización: dashboard, conocimiento, IA, CRM, notificaciones y Gmail. |
| `api-gateway` | Spring Cloud Gateway: punto de entrada a las APIs, rutas hacia los servicios. |
| `eureka-server` | Registro y descubrimiento de instancias Java. |
| `common-contracts` | Contratos compartidos de eventos; no comparte repositorios entre servicios. |
| `mail-ingestion-service` | Recepción REST y publicación del correo. |
| `business-knowledge-service` | Perfiles, dimensiones, opciones, reglas, autenticación administrativa. |
| `decision-engine-service` | Evaluación HARD/SOFT/SystemOne, idempotencia y diagnósticos. |
| `crm-adapter-service` | Enrutamiento y ejecución CRM, mapeos, equipos, vista previa y auditoría. |
| `notification-service` | Plantillas, eventos y envíos LOG/SMTP. |
| `gmail-connector` | OAuth de Google, consulta de mensajes y deduplicación. |
| `observability-service` | Trazabilidad, indicadores e interpretación de los estados. |
| `demo-crm-service` | API CRM simulada para pruebas independientes de EspoCRM. |

**Separación de responsabilidades:** cada servicio de negocio utiliza, según necesidad, `api/controller`, `application/service`, `application/port`, `domain`, `infrastructure` y `config`. Los casos de uso dependen de puertos propios; los adaptadores implementan JDBC/JPA, HTTP, RabbitMQ y SMTP.

**Spring Cloud implementado:** Gateway, Eureka Discovery y soporte de LoadBalancer para rutas/cliente compatibles. **No se atribuyen** al sistema OpenFeign, Config Server ni Circuit Breaker Resilience4j como componentes implementados. RabbitMQ aporta mensajería asíncrona, pero no es Spring Cloud. El proyecto no demuestra por defecto distribución entre múltiples réplicas.

Detalles de paquetes, interfaces y métodos: [Arquitectura Java](docs/ARCHITECTURE_JAVA.md) · [Distribución de paquetes](docs/PACKAGE_LAYOUT.md).

## Ejecutar todo en Windows con Docker Desktop

### Requisitos

- Docker Desktop con Docker Compose.
- Git y PowerShell para los comandos de ejemplo.
- Conexión a Internet durante la primera construcción (imágenes, Maven y npm).
- **Opcional:** JDK 21 y Maven para ejecutar las pruebas Java fuera de Docker.

### 1. Obtener el código y preparar el entorno

En una instalación nueva:

```powershell
git clone https://github.com/reyrojo2/agnostic-ticket-router.git
cd agnostic-ticket-router
Copy-Item .env.example .env
notepad .env
```

Si ya tienes el repositorio:

```powershell
git switch main
git pull --ff-only origin main
```

**No reemplaces un `.env` existente.** Completa al menos estas variables según tu escenario:

| Variable | Utilidad |
| --- | --- |
| `DB_USER`, `DB_PASSWORD` | Base PostgreSQL de los microservicios. |
| `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | Broker de eventos. |
| `ADMIN_USER`, `ADMIN_PASSWORD`, `APP_JWT_SECRET` | Cuenta administrativa y JWT (secreto robusto de al menos 32 bytes). |
| `ESPOCRM_DB_ROOT_PASSWORD`, `ESPOCRM_DB_PASSWORD`, `ESPOCRM_ADMIN_PASSWORD` | Primera instalación de EspoCRM y MariaDB. |
| `ESPOCRM_API_KEY` | Usuario API autorizado para la integración CRM. |
| `AI_CONFIG_ENCRYPTION_KEY` | Clave maestra Base64 de **32 bytes** para cifrar claves de proveedores SystemOne. |
| `NOTIFICATION_ENCRYPTION_KEY` | Otra clave maestra Base64 de **32 bytes** para credenciales SMTP. |
| `GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`, `GMAIL_ENCRYPTION_KEY` | OAuth y protección de tokens de Gmail, si se utiliza el conector. |

Las claves maestras de IA y notificaciones deben ser **distintas**, generadas aleatoriamente y respaldadas fuera de Git. Si ya hay credenciales cifradas, **no cambies las claves maestras** sin plan de rotación: los datos guardados dejarían de poder descifrarse. Las claves de IA y las contraseñas SMTP se administran desde Angular; no debes introducir secretos en commits o capturas.

### 2. Iniciar los contenedores

```powershell
docker compose config --quiet
.\scripts\start.ps1
docker compose ps
docker compose -p agnostic-espo -f compose.espocrm.yml ps
```

`start.ps1` inicia dos proyectos Compose: la plataforma (`docker-compose.yml`) y EspoCRM con su MariaDB (`compose.espocrm.yml`). **No ejecutes simultáneamente** servidores Java/Angular locales que utilicen los mismos puertos.

| Interfaz / recurso | Dirección local |
| --- | --- |
| Angular | http://localhost:4200 |
| Spring Cloud Gateway | http://localhost:8088 |
| Eureka | http://localhost:8761 |
| EspoCRM | http://localhost:8080 |
| RabbitMQ (administración) | http://localhost:15672 |
| Mailpit (correos de prueba) | http://localhost:8025 |
| Business Knowledge | http://localhost:8082 |
| Decision Engine | http://localhost:8083 |
| CRM Adapter | http://localhost:8084 |
| Notifications | http://localhost:8085 |
| Mail Ingestion | http://localhost:8081 |
| Demo CRM | http://localhost:8090 |

PostgreSQL usa `localhost:5433` desde Windows y `postgres:5432` desde Docker. Gmail Connector y Observability operan en la red interna y se consultan mediante rutas del gateway.

Para detener **sin borrar datos**:

```powershell
.\scripts\stop.ps1
```

No ejecutes `docker compose down -v` si necesitas conservar PostgreSQL, el historial de Gmail y las configuraciones del CRM.

### 3. Reconstruir tras un `git pull`

```powershell
docker compose up -d --build
docker compose ps
```

Para reconstruir solo un servicio:

```powershell
docker compose up -d --build --no-deps decision-engine-service
```

Si un contenedor aparece como `Restarting`, revisa **sus propios registros**, no presupongas que el error es de Angular:

```powershell
docker compose logs --tail=150 decision-engine-service
docker compose logs --tail=150 notification-service
docker compose logs -f mail-ingestion-service decision-engine-service crm-adapter-service
```

Si necesitas Laya local existe el perfil opcional `ai` (`.\scripts\start.ps1 -WithAI`); su checkpoint y compatibilidad de inferencia deben validarse por separado. **No es obligatorio iniciar Laya** para las rutas que se resuelvan mediante reglas HARD.

## Configurar y demostrar la plataforma

### A. Business Knowledge

En **Business Knowledge**, crea o selecciona un perfil (p. ej. `TELCO_DEMO`) y configura decisiones como PRODUCTO, TIPO_GESTION, PRIORIDAD y DESTINO. Cada dimensión dispone de opciones, instrucciones, condiciones y reglas.

- **HARD:** clasifica sin IA cuando el resultado es concluyente.
- **SOFT:** orienta, pero no fuerza, la elección; se consideran el contexto y los umbrales.
- **Revisión:** una dimensión obligatoria no aceptada impide crear el caso automáticamente.

[Guía de conocimiento y proveedores IA](docs/KNOWLEDGE_AND_AI_GUIDE.md).

### B. Proveedores de IA de decisión (SystemOne)

En **Proveedores IA** puedes guardar nombre, URL base, ruta SystemOne, modelo, idioma y autenticación (Bearer, cabecera API Key o ninguna para servidor local), ejecutar una **prueba sintética** y elegir **Usar para decisiones**. Una conexión nueva **no se activa por guardarla**.

La selección se aplica a las próximas consultas de **todos los perfiles**; todavía no existe un proveedor distinto por perfil. Si no se ha seleccionado uno desde la aplicación, se utiliza la configuración del servidor. Un proveedor debe implementar el **contrato SystemOne**; una API genérica de chat no se vuelve compatible solo por proporcionar una URL. Una prueba de conexión puede consumir cuota.

Los errores de autenticación, facturación, cuotas, incompatibilidad o confianza insuficiente se explican en [diagnósticos de solicitudes](docs/REQUEST_DIAGNOSTICS.md). Los fallos de IA no se deben presentar como clasificaciones aceptadas.

### C. CRM y EspoCRM

En **Integraciones CRM**, elige una integración (`ESPOCRM_LOCAL` para la demostración) y configura la operación de creación de Case. Desde el contenedor CRM Adapter, EspoCRM se alcanza habitualmente en `http://host.docker.internal:8080`, **no** en `localhost:8080`. La cuenta de API requiere permisos para los recursos que consulta o modifica.

- **Mapeo dinámico:** relaciona una dimensión de Business Knowledge con un campo real del CRM, con equivalencias de valores opcionales.
- **Equipos y responsables:** vincula `DESTINO` a identificadores reales de `Team` o `User`; crear equipos desde la API es una **acción administrativa explícita**, no algo que ocurra por cada correo.
- **Compatibilidad:** consulta los metadatos disponibles y valida campos editables, enums, referencias y permisos.
- **Vista previa:** compone el **mismo JSON** que se empleará en el envío real, **sin crear** el caso.
- **Trazabilidad:** conserva resultado, mapeos, número de caso (cuando el CRM lo devuelve), HTTP y referencia de solicitud, sin guardar las credenciales.

EspoCRM **no crea automáticamente campos personalizados de Case** desde esta plataforma. Si requieres tipologías filtrables, primero crea el campo en **Administration → Entity Manager → Case → Fields**, obtén su nombre de API y selecciónalo desde Angular. La clasificación también puede añadirse como resumen a la descripción sin modificar el esquema.

[Guía de CRM](docs/CRM_CONFIGURATION_GUIDE.md) · [Mapeo y provisión](docs/CRM_MAPPING_AND_PROVISIONING.md) · [Validación](docs/CRM_MAPPING_VALIDATION.md).

### D. Notificaciones

En **Notificaciones**, configura canales LOG o SMTP y mensajes por perfil y evento: `EMAIL_RECEIVED`, `CRM_CREATED`, `REVIEW_REQUIRED` y `CRM_ERROR`. Puedes usar **Mailpit** sin enviar correos externos o SMTP autenticado (por ejemplo Gmail con contraseña de aplicación).

La notificación de creación puede incluir el **número público del Case** (`number` en EspoCRM); ese número **no es** el `requestId` de ATR ni el ID técnico interno del CRM. Una vista previa no entrega correos. Las contraseñas SMTP nuevas se cifran utilizando `NOTIFICATION_ENCRYPTION_KEY`.

[Guía de notificaciones](docs/NOTIFICATIONS_GUIDE.md).

### E. Gmail, dashboard y pruebas de recorrido

En **Buzón Gmail** conecta una cuenta de pruebas mediante OAuth, configura los filtros e inicia la sincronización. Repite la consulta: los IDs ya registrados deben contarse como omitidos, no como nuevas publicaciones. No actives el Email-to-Case nativo de EspoCRM si quieres demostrar el motor de decisiones.

En **Dashboard** revisa el `requestId`, decisiones, regla aplicada, confianza, necesidades de revisión y estado de creación CRM. Para una primera demostración, usa un correo ficticio y verifica: ingesta → clasificación → caso en EspoCRM → notificación Mailpit → observabilidad.

[Plan de pruebas E2E](docs/EMAIL_TEST_PLAN.md) · [Estados y diagnósticos](docs/REQUEST_DIAGNOSTICS.md).

## Pruebas y alcance de validación

```powershell
mvn -B -ntp test
docker compose config --quiet
docker compose -p agnostic-espo -f compose.espocrm.yml config --quiet
docker compose build
```

El repositorio cuenta con pruebas JUnit/ArchUnit y un flujo de CI que verifica Maven, Compose y la construcción de imágenes Java/Angular: [GitHub Actions](https://github.com/reyrojo2/agnostic-ticket-router/actions).

**Que CI esté en verde no certifica toda la integración real:** Gmail OAuth, proveedores SystemOne, permisos de EspoCRM, SMTP externo y el comportamiento de equipos/campos requieren pruebas en cada instalación con credenciales propias. No se garantiza transacción distribuida ni entrega exactamente una vez.

**Pendiente para convertirlo en SaaS comercial:** autenticación/roles y permisos operativos robustos, aislamiento de empresas (*multi-tenancy*), gestión de planes y cobros, observabilidad operacional, políticas de retención y privacidad, seguridad y respaldo de secretos, límites de consumo, migraciones y despliegue reproducible, auditoría empresarial y pruebas E2E automatizadas. Estas capacidades **no se consideran implementadas** por el mero uso de Docker o microservicios.

## Documentación técnica

| Documento | Contenido |
| --- | --- |
| [Arquitectura Java](docs/ARCHITECTURE_JAVA.md) | Capas, límites SOLID y responsabilidades de clases. |
| [Paquetes Java](docs/PACKAGE_LAYOUT.md) | Distribución de carpetas. |
| [Business Knowledge e IA](docs/KNOWLEDGE_AND_AI_GUIDE.md) | Configuración, conexión SystemOne y cifrado. |
| [Configuración CRM](docs/CRM_CONFIGURATION_GUIDE.md) | Integraciones y operaciones. |
| [Mapeo CRM y equipos](docs/CRM_MAPPING_AND_PROVISIONING.md) | Campos, equipos, IDs, vista previa y auditoría. |
| [Notificaciones](docs/NOTIFICATIONS_GUIDE.md) | SMTP, plantillas, número público y credenciales. |
| [Diagnósticos](docs/REQUEST_DIAGNOSTICS.md) | Explicaciones de fallos de IA y revisión. |
| [Pruebas de correo](docs/EMAIL_TEST_PLAN.md) | Demostración Gmail/REST/RabbitMQ/CRM. |
| [Idempotencia](docs/DECISION_IDEMPOTENCY_MIGRATION.md) | Precauciones con datos de versiones previas. |

## Origen académico y derechos sobre el código

Este repositorio se desarrolló como **proyecto académico** de Desarrollo de Aplicaciones Web II. La arquitectura se estudia como posible base para un producto **SaaS comercial**, pero este código no constituye todavía un servicio comercial ofrecido al público.

**Estado de licencia: derechos reservados.** Este repositorio **no contiene actualmente una licencia de software libre** (MIT, Apache 2.0, GPL, etc.). La publicación de los archivos para consulta académica **no otorga por sí sola una autorización general** para redistribuir, modificar o explotar comercialmente el software; siguen siendo aplicables las condiciones de GitHub y las excepciones legales que correspondan. Para cualquier acuerdo de uso, distribución o explotación comercial se requiere autorización de los titulares de los derechos.

La titularidad debe confirmarse con **todas las personas que hayan contribuido código original**, así como con las reglas académicas aplicables. Las dependencias y herramientas de terceros mantienen sus respectivas licencias. **No se ha creado todavía un archivo `LICENSE` propietario ni se ha modificado la visibilidad pública del repositorio.**

Para una futura operación SaaS, las licencias del código fuente, las condiciones de uso del servicio alojado, la política de privacidad y los acuerdos con clientes serán documentos distintos. Antes de comercializar, conviene obtener una revisión jurídica de titularidad, dependencias y condiciones contractuales.
