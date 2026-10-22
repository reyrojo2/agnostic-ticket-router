# Seguimiento y revisión de solicitudes

El dashboard ofrece **Ver detalle** en cada solicitud reciente. Permite revisar el resultado de cada decisión, la regla aplicada, la confianza y los umbrales utilizados, además del motivo que impidió continuar y enlaces a la configuración correspondiente.

## Qué significa cada estado

- **Recibida:** la ingesta registró el correo; todavía no hay resultado de clasificación.
- **Clasificada · pendiente de CRM:** las decisiones se evaluaron y falta la confirmación del adaptador.
- **Necesita revisión:** alguna decisión obligatoria no pudo aceptarse. El adaptador detiene el envío al CRM.
- **Caso creado:** el CRM confirmó el registro.
- **Error al enviar al CRM:** el adaptador intentó tramitar la solicitud y encontró un problema. Revisa la integración y su historial.

Una revisión no implica necesariamente un fallo de EspoCRM. Por ejemplo, si las reglas reconocen el producto y el tipo de gestión, pero el proveedor IA rechaza la consulta de prioridad, el correo se detiene antes del envío.

## Motivos y siguientes pasos

| Motivo | Acción |
| --- | --- |
| El proveedor exige un modelo | Completa Modelo en Proveedores IA con el identificador autorizado para tu cuenta y prueba la conexión. |
| HTTP 401/403 | Revisa la API key y sus permisos. |
| HTTP 404 | Revisa dirección base y ruta SystemOne. |
| HTTP 400/422 | Revisa modelo y compatibilidad de la solicitud. |
| HTTP 402/429 | Revisa cuota, saldo y límites en el proveedor. |
| Sin conexión o tiempo de espera agotado | Comprueba que el servidor del motor puede alcanzar al proveedor. |
| Confianza insuficiente | Revisa instrucciones y descripciones; compara la confianza con el mínimo registrado. |
| Alternativas demasiado cercanas | Revisa respuestas que se solapan y el margen mínimo registrado. |
| Sin respuestas elegibles | Configura respuestas habilitadas y comprueba sus condiciones previas. |
| Respuesta truncada o inválida | Revisa el tamaño de contexto y la compatibilidad SystemOne del proveedor. |

Después de un fallo de IA, el motor evita repetir consultas en el mismo correo. Continúa aplicando reglas determinísticas y registra cuáles de las decisiones restantes quedaron sin consultar por ese fallo.

**Sin respuesta IA** cuenta solicitudes donde falló o se omitió la consulta por una indisponibilidad. **Confianza insuficiente** cuenta respuestas IA recibidas con incertidumbre; no cuenta las desconexiones como si fueran probabilidades bajas. Las categorías pueden coincidir si distintas decisiones tuvieron problemas diferentes.

## Datos históricos y límites

Los correos anteriores al cambio conservan su resultado y origen, pero no sus causas HTTP ni umbrales originales. El dashboard indica esa limitación sin atribuirles el resultado de una prueba de conexión posterior. Las etiquetas de dimensiones y respuestas nuevas se guardan con la evaluación, de modo que editar el conocimiento después no cambia la explicación del pasado.

Cambiar una clave, modelo o regla no reprocesa correos anteriores. Envía un correo nuevo para comprobar el recorrido completo. Sincronizar de nuevo Gmail no repite mensajes ya procesados. La pantalla explica las revisiones; no añade una bandeja para aprobar o reenviar solicitudes históricas.

## Responsabilidades y protección de datos

Decision Engine produce los diagnósticos en el contrato compartido `DecisionDiagnostic`. Observability los conserva en su propio modelo de lectura y construye las explicaciones y métricas. Angular presenta esos datos; no consulta otras bases ni toma decisiones de enrutamiento.

Los diagnósticos usan mensajes controlados por la aplicación y códigos HTTP. No se conservan cuerpos de errores del proveedor ni cabeceras de autenticación. Para errores de validación de modelo se extrae solo el metadato conocido de campo obligatorio, descartando los valores que el proveedor pudiera devolver.

La prueba de conexión usa una pregunta sintética de sí/no para no depender de un catálogo de una sola alternativa, que algunos proveedores no admiten. No envía correos del usuario ni crea casos.

## Validación realizada

- 83 pruebas Maven aprobadas, incluidos escenarios de fallo IA, confianza, margen, respuestas inválidas, compatibilidad con eventos anteriores, persistencia del diagnóstico y protección de credenciales.
- Compilación Angular y Docker aprobada; Decision Engine, Observability y Angular actualizados en el entorno local.
- Chrome: detalle de una solicitud histórica real, mensajes nuevos mediante datos simulados, enlaces de configuración, porcentajes, actualización, cierre del detalle y pantalla de 650 px sin desbordamiento de página.
- Proveedor configurado: conexión sintética correcta y evaluación aislada del motor con cuatro decisiones aceptadas. La evaluación no publicó eventos ni creó casos CRM. Una consulta sintética puede consumir cuota del proveedor.
