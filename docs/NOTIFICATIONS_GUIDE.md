# Notificaciones y referencia del caso

## Qué recibe el cliente

El correo de recepción confirma que llegó la solicitud. Todavía no existe un número de caso CRM, por lo que este aviso no lo inventa ni presenta el identificador interno de seguimiento como si fuera un ticket.

Cuando el CRM confirma la creación, el mensaje recomendado utiliza la referencia real del caso. Para EspoCRM se toma el atributo `number`, que corresponde a la columna **Number** de Cases. Por ejemplo:

> Caso #5 registrado
>
> Tu solicitud fue registrada correctamente. Tu número de caso es 5. Conserva este número para consultar el estado de tu solicitud.

En EspoCRM puedes localizarlo por **Number**, añadiendo ese campo como filtro en la lista de Cases. El identificador técnico del registro, como `6ad4e35b79d20a154`, sigue siendo independiente y se conserva para las operaciones de API y auditoría. El identificador de solicitud de ATR se mantiene para la trazabilidad interna.

## Configuración desde Angular

1. Abre **Notificaciones → Canales de envío** y utiliza un canal existente o pulsa **Añadir canal**. El botón de Mailpit completa una conexión de pruebas locales; sus correos se consultan en `http://localhost:8025`.
2. Para un servidor real indica nombre, servidor SMTP, remitente, puerto y seguridad. Selecciona autenticación si necesita usuario y contraseña. Las credenciales se guardan cifradas; dejar los campos vacíos al editar conserva las anteriores.
3. Abre **Mensajes al cliente** y elige el proceso por su nombre. La opción de todos los perfiles configura mensajes generales.
4. Elige el momento: recepción, creación confirmada en CRM, revisión o error de registro. Selecciona el canal por su nombre y quién recibirá el aviso: el cliente o una dirección fija.
5. Usa el mensaje recomendado o personaliza el contenido. **Insertar dato** añade variables disponibles para ese momento sin escribir identificadores manualmente.
6. **Ver vista previa** utiliza datos ficticios y el número 12345. No envía un correo ni crea un caso. Guarda para aplicar el mensaje a próximos eventos.

Un mensaje específico del perfil sustituye al mensaje general del mismo momento. Deshabilitar explícitamente ese mensaje evita también el envío del mensaje general. Eliminarlo permite volver al mensaje general, si existe y está habilitado.

Los canales de registro de desarrollo escriben en los logs y no envían correos. No se puede eliminar un canal con mensajes asociados ni deshabilitarlo mientras tiene mensajes activos; primero cambia esos mensajes o desactívalos.

## Identificadores y variables

| Variable | Contenido |
| --- | --- |
| `ticketNumber` | Número público devuelto por el CRM; en EspoCRM, `number`. |
| `ticketReference` | Referencia del caso para el cliente. Prioriza el número; otros CRM pueden usar su ID si no se configuró un número público. |
| `confirmationSubject` | Asunto recomendado con la referencia real, o confirmación sin número cuando el CRM no lo devuelve. |
| `confirmationMessage` | Mensaje recomendado con la referencia real. |
| `requestId` | Referencia interna de ATR, disponible para seguimiento técnico. No es el número de caso CRM. |
| `data.externalTicketId` | ID técnico del registro CRM. |

El número solo está disponible en el evento de creación confirmada. Las variables de asunto y remitente del correo original se ofrecen en recepción. La API valida las variables y avisa si se utiliza una que no corresponde al momento elegido.

## Responsabilidades de los servicios

- CRM Adapter extrae por separado el ID técnico y el número público de la respuesta HTTP. Publica `externalTicketId`, `externalTicketNumber` y `ticketReference` en el evento de resultado y conserva el número en su auditoría.
- Notification Service recibe ese evento y construye el contexto del mensaje. Administra sus canales, plantillas, cifrado y envío. No consulta tablas ni credenciales de CRM Adapter.
- Observability conserva el número recibido en su propio modelo de lectura.
- Angular consulta los servicios mediante el gateway y usa Business Knowledge para mostrar nombres de perfiles.

La acción CRM incluye **Ruta del número de caso para el cliente** en sus opciones técnicas. La migración configura `number` para las operaciones existentes de creación de Case en las conexiones EspoCRM conocidas. Otras integraciones pueden configurar su propia ruta JSON.

## Despliegue e información histórica

`NOTIFICATION_ENCRYPTION_KEY` es una clave maestra Base64 de 32 bytes del servidor. En este entorno se generó una vez en `.env`, excluido de Git. Conserva esta clave junto con los respaldos de la base de datos. Es independiente de las contraseñas SMTP y de las claves de otros microservicios.

Las referencias `ENV:...` existentes siguen disponibles en opciones avanzadas. Las credenciales nuevas introducidas desde Angular se cifran con AES-GCM y no se devuelven al navegador, ni en texto claro ni cifrado.

Las migraciones solo actualizan el texto original de las plantillas de demostración. Conservan las personalizaciones, canales elegidos y estados de activación. Si personalizaste una confirmación que todavía usa `requestId`, selecciona el texto recomendado o inserta Referencia del caso CRM.

Los correos ya enviados no cambian ni se reenvían. Los casos históricos conservan los identificadores que se guardaron en su momento. La configuración se aplica a las próximas confirmaciones de creación.

## Validación realizada

- 99 pruebas backend aprobadas: extracción independiente de ID y número, publicación del evento, contexto y envío de la confirmación, mensajes deshabilitados, variables, vista previa, cifrado y compatibilidad con las demás funciones.
- Compilación Angular y Docker aprobada. Migraciones aplicadas y servicios actualizados en el entorno local.
- API real: canal SMTP temporal con credenciales ficticias cifradas en PostgreSQL, respuestas sin secretos y edición con campos vacíos. El canal temporal se eliminó después de comprobarlo.
- Chrome: selección de perfil, eventos y canales; texto recomendado y personalizado; inserción de número; vista previa; preset Mailpit; edición sin mostrar credenciales; pantalla de 650 px sin desbordamiento. Las escrituras de esta prueba visual se simularon para conservar la configuración real.
- Consulta de solo lectura a EspoCRM: el filtro de Cases por `number = 5` devuelve el mismo registro técnico verificado.
- Envío SMTP real a Mailpit con la plantilla guardada: asunto `[PRUEBA ATR] Caso #5 registrado` y cuerpo con el número 5, sin UUID de solicitud ni ID técnico CRM. Se utilizó un destinatario ficticio y no se creó ningún caso nuevo en EspoCRM.
