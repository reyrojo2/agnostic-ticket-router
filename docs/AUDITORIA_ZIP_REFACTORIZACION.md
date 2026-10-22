# Auditoría de la refactorización — ZIP revisado

## Alcance

Se revisó la versión `agnostic-ticket-router-refactor-completo.zip` entregada previamente; todos los cambios de esta auditoría se aplicaron **sobre una copia local**. No se modificó la rama remota, ni el PR #8.

El proyecto contiene 7 microservicios Java de negocio (`business-knowledge`, `crm-adapter`, `decision-engine`, `gmail-connector`, `mail-ingestion`, `notification`, `observability`), además de Eureka, API Gateway, un demo CRM, `common-contracts` y el frontend Angular.

## Hallazgos y correcciones

| Hallazgo | Corrección |
| --- | --- |
| Casos de uso dependían directamente de Spring Data/JDBC (`knowledge`, CRM, notifications, Gmail, decision, observability) | Se introdujeron puertos locales en `application/port` y sus adaptadores en `infrastructure/*` implementan dichos contratos. |
| `GenericDecisionEngine` en `domain/service` dependía de HTTP (`KnowledgeClient`) y `AiDimensionEvaluator` dependía de un puerto de aplicación | Se movieron ambos a `application/service`; ahora `KnowledgeProfileProvider` abstrae el cliente REST y el dominio conserva solamente reglas y políticas. |
| `SystemOneModels` era un DTO del proveedor de IA ubicado en `domain/model` | Se movió a `application/model` para no mezclar el contrato del proveedor con reglas puras de dominio. |
| Políticas de dominio estaban anotadas con `@Component` de Spring | Se eliminó esa dependencia de las cuatro políticas de Decision Engine y se registraron los beans desde `config/DecisionPolicyConfig`. |
| `AuthenticationService` importaba un controller y JWT concreto | Se eliminó dependencia HTTP y se creó `AccessTokenIssuer`, implementado por `JwtService`. |
| La aplicación Gmail usaba `GmailRepository.Account` y `TokenCipher` directamente | Se incorporaron `GmailAccount`, `GmailStore`, `TokenProtector` y `GmailRuntimeSettings`. |
| `DemoCrmController` estaba suelto en el paquete raíz de Demo CRM | Se movió a `api/controller` sin alterar `/api/tickets`. |
| ArchUnit no prohibía acceso de casos de uso a infraestructura; reglas con `allowEmptyShould(true)` poco exigentes | Se ampliaron las pruebas con dependencias por capa y límites entre servicios. |
| Documentación conservaba paquetes antiguos (`service`, `repo`, `gateway`) | Se actualizó `ARCHITECTURE_JAVA.md` y `PACKAGE_LAYOUT.md`. |

## Verificaciones realizadas en este entorno

- **156 archivos Java** analizados por el parser oficial de Java 21 (`JavacTask.parse()`): sin errores de sintaxis.
- **7 microservicios Java** sometidos al verificador `python scripts/verify_architecture.py`: rutas, paquetes, imports internos, control de dependencias y clases de arranque correctos.
- Se compilaron **las interfaces de repositorio JPA y sus puertos** contra un contrato mínimo equivalente a las firmas de `JpaRepository`. Esto solo verifica firmas y herencia, **no** Spring Data real.
- Se compiló el núcleo de Decision Engine con Java 21 y sustitutos temporales de Spring/Jackson/SLF4J. Se ejecutó una prueba de comportamiento: HARD antes que IA; SOFT llama IA; fallo IA requiere revisión; duplicado no se publica dos veces; primero se publica y después se marca procesado. Es una **prueba parcial aislada**, no una prueba Maven/JUnit ni con RabbitMQ.
- XML de **12 archivos POM**, YAML de **13 archivos**, JSON de **7 archivos** y sintaxis de **16 archivos TypeScript**: correcto.
- Se compararon archivos de infraestructura con el ZIP original: **SQL, Docker Compose y código Angular quedaron sin cambios**.

## Verificaciones NO realizadas (bloquean la aprobación definitiva)

- `mvn -B -ntp clean verify` no se ejecutó porque Maven y sus dependencias no están instaladas ni descargables en el entorno de auditoría.
- `docker compose config` y compilación/arranque de imágenes no se ejecutaron porque Docker no está disponible aquí.
- Pruebas con PostgreSQL, RabbitMQ, Gmail OAuth, Laya/SystemOne y EspoCRM en conjunto: no ejecutadas.
- No se puede afirmar equivalencia de integración, cero errores de Spring Data ni disponibilidad en producción a partir de pruebas estáticas.

## Procedimiento de validación en Windows 10

1. Descomprimir el ZIP revisado en una **carpeta nueva**. No sobrescribir a ciegas el workspace con cambios sin registrar.
2. Ejecutar en la raíz: `python scripts/verify_architecture.py`.
3. Con Java 21 y Maven: `mvn -B -ntp clean verify` (o desde la ventana Maven de IntelliJ, ciclo `verify`). **Resolver cualquier error antes de fusionar**.
4. Usar credenciales de prueba en `.env` (no las de producción) y comprobar `docker compose config --quiet` y `docker compose -f compose.espocrm.yml config --quiet`.
5. Construir imágenes: `docker compose build` y ejecutar los escenarios de `docs/EMAIL_TEST_PLAN.md` para Gmail → Ingesta → clasificación → CRM → notificaciones y observabilidad.
6. Antes de arrancar Decision Engine contra datos reales, seguir `docs/DECISION_IDEMPOTENCY_MIGRATION.md`. **No eliminar las colas RabbitMQ ni truncar datos.**
7. Tras pasar todas las pruebas, integrar los archivos en la rama del PR #8 y revisar el diff en GitHub; **el ZIP no actualiza el PR automáticamente**.

## Riesgos arquitectónicos aún existentes

- Las entidades en `domain/model` mantienen anotaciones JPA/Lombok. Las dependencias entre módulos están aisladas, pero un dominio 100 % independiente de frameworks requeriría separar entidades persistentes de objetos de negocio.
- Publicación RabbitMQ y registro de idempotencia siguen siendo operaciones no atómicas. No se garantiza *exactly-once*. Para reducir duplicados e inconsistencias faltan patrones de **outbox, publisher confirms e idempotencia del consumidor**.
- Aunque la política SOLID mejoró, no se ha rediseñado el comportamiento de negocio ni la configuración de seguridad. Es imprescindible validar con servicios externos antes de dar aprobación productiva.
