# Validación del mapeo EspoCRM

Revisión local del 21 de octubre de 2026, sobre `732f75f` y las correcciones de esta revisión.

## Errores 500 confirmados

`POST /api/v1/admin/crm/operations/2/validate` y la creación/vinculación de equipos utilizaban una operación cuya relación `integration` seguía siendo un proxy JPA. Con `open-in-view: false`, la transacción del repositorio ya había terminado cuando el cliente EspoCRM consultaba las credenciales y la URL. El resultado era `LazyInitializationException`, antes de contactar al CRM.

La consulta `CrmOperationRepository.findById` ahora carga también `integration` mediante `@EntityGraph`. Se conserva la relación LAZY en la entidad y no se mantiene una transacción abierta durante las llamadas HTTP.

La prueba de persistencia se ejecuta fuera de una transacción de test: falló con la misma excepción antes de la corrección y pasa con ella. También se prueban la reutilización de una asignación existente, el rechazo de dimensiones inválidas antes de contactar EspoCRM y las respuestas HTTP 404/409 de recursos ausentes/conflictos.

## Estado observado de la configuración

| Elemento | Resultado |
| --- | --- |
| Motor de decisiones | Arranca; el problema anterior de DataSource está corregido en el pull. |
| Operación 2 | `ESPOCRM_LOCAL / CREATE_CASE`, `/api/v1/Case`. |
| Ruta TELCO_DEMO | Activa y dirigida a esa operación. |
| Catálogo Case | 22 campos; sin campos personalizados con prefijo `c` seguido de mayúscula. |
| Catálogo de equipos/usuarios | Las consultas devuelven listas vacías con la cuenta API configurada. Esto no demuestra permiso para crear equipos. |
| Mapeos de campos/asignaciones de la operación 2 | Vacíos. |
| Vista previa | Resuelve CREATE_CASE y genera `name` y `description`; no aplica campos estructurados ni equipos. |
| Validación directa del adaptador corregido | HTTP 200, compatible, con advertencia de ausencia de mapeos estructurados. |
| Validación y vista previa por `localhost:4200` | Ambas HTTP 200 después de actualizar el registro de Eureka. Angular sirve el bundle nuevo. |
| Gmail | Configurado, conectado, habilitado, sin último error informado en el momento de la consulta. |
| IA | Los logs recientes del motor contienen errores HTTP al consultar el proveedor; queda por comprobar una clasificación real satisfactoria. |

## Qué hace cada acción de Angular

1. **Guardar campo:** guarda una regla en el router para rellenar un campo ya existente de EspoCRM. No crea el campo externo.
2. **Guardar asignación:** relaciona una dimensión/opción con el ID de un equipo o usuario existente.
3. **Crear o vincular equipo:** consulta el CRM, crea el equipo si no lo encuentra y guarda su asignación. No requiere introducir antes un ID externo.
4. **Sincronizar equipos:** repite la acción anterior para las opciones activas de una dimensión. Puede completar solo parte del lote si una llamada falla.
5. **Validar compatibilidad:** consulta metadatos y referencias de la operación guardada. No crea un caso ni demuestra permiso de creación de casos/equipos.
6. **Previsualizar:** resuelve las rutas guardadas del perfil y compone el JSON sin enviarlo. Puede seleccionar una operación diferente de la editada arriba.

La interfaz ahora distingue estas acciones, muestra la operación guardada, separa el perfil de sincronización del perfil de vista previa, limpia los catálogos al cambiar de operación y consulta campos de la entidad de esa operación. La carga del catálogo espera a las tres consultas y conserva los resultados disponibles si una falla. Los lotes interrumpidos informan cuántas asignaciones se confirmaron.

## Configuración pendiente para una demostración

1. Seleccionar `ESPOCRM_LOCAL` y `CREATE_CASE`; guardar cualquier cambio de plantilla antes de validar.
2. Para mostrar clasificación como campos separados, crear los campos personalizados en EspoCRM y guardar sus mapeos en Angular. La creación automática de campos no está implementada.
3. Para un ejemplo con campos estándar, mapear `PRIORIDAD` a `priority`. Los valores admitidos comprobados son `Low`, `Normal`, `High`, `Urgent`; deben definirse las equivalencias para los códigos reales del perfil.
4. Para asignación por áreas, crear/vincular equipos desde `DESTINO`, o seleccionar IDs existentes. Comprobar permisos de lectura/creación de Team y asignación en Case de la cuenta API. No asumir que leer Metadata permite crear equipos.
5. Previsualizar y comprobar la integración/operación resuelta, los campos estructurados y `teamsIds`/`assignedUserId` que correspondan.
6. Validar compatibilidad. Una respuesta compatible con cero mapeos solo confirma la plantilla actual.
7. Resolver las llamadas a IA o preparar un escenario completamente resuelto por reglas HARD; después comprobar un correo real hasta el Case y su auditoría.

## Límites que siguen pendientes

- No se ejecutó una creación real de Team o Case durante esta revisión. Las escrituras de provisión se comprobaron con catálogo simulado, además de la regresión JPA con H2. La conexión real se comprobó con consultas y vista previa.
- La validación no comprueba exhaustivamente campos obligatorios, tipos de todos los valores, existencia de las dimensiones/opciones de origen ni cobertura completa de equivalencias enum.
- Crear un equipo externo y guardar la asignación local son acciones separadas: un fallo local posterior puede dejar el equipo creado. La reutilización por nombre ayuda a reintentar; no es una transacción distribuida.
- La comprobación de duplicados de equipos solo se sincroniza dentro de una instancia; la entrega CRM tampoco garantiza exactamente una creación por solicitud.
- La auditoría recompone los nombres de campos a partir de la configuración actual; no conserva una versión inmutable de todo el mapeo utilizado.
- La configuración aún usa códigos y JSON manuales. Selectores perfil → dimensión → opción y un editor de equivalencias reducirían errores de uso.

## Verificación ejecutada

- Maven `test` con Java 21: **56 pruebas, cero fallos y cero errores**.
- Angular `npm run build`: correcto.
- Imágenes Docker de CRM Adapter y Angular: compiladas y recreadas localmente.
- Sin migraciones nuevas ni cambios a las credenciales.

Después de recrear un servicio puede aparecer temporalmente un 503 en el gateway mientras Eureka y la caché de descubrimiento actualizan la instancia. La respuesta directa del adaptador permite distinguir este intervalo del error de aplicación original.
