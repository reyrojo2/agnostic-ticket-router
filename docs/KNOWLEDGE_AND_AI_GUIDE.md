# Conocimiento del negocio y proveedores IA

## Business Knowledge

La configuración se divide en cuatro pasos en la misma pantalla:

1. **Perfil:** el proceso que quieres configurar. El nombre se introduce una vez y su identificador se genera automáticamente.
2. **Decisiones:** los datos que debe reconocer el motor. Una dimensión puede elegir una respuesta, calcular una puntuación o responder sí/no.
3. **Respuestas:** las alternativas y sus descripciones. Los ejemplos, exclusiones y condiciones previas están en opciones avanzadas.
4. **Reglas:** elige una respuesta existente y escribe las expresiones, una por línea. Una regla puede aportar una sugerencia a la IA o resolver directamente cuando no hay coincidencias contradictorias.

Los selectores superiores cambian el perfil y la decisión que estás configurando. Los identificadores existentes se conservan y se muestran como solo lectura al editar, para mantener las relaciones con CRM, Gmail y otras configuraciones. Las condiciones usan decisiones anteriores y sus respuestas por nombre; confianza y margen se expresan como porcentajes.

Los cambios solo se aplican al pulsar Guardar. Las pestañas conservan el modelo del formulario mientras se mantiene el contexto seleccionado. Cambiar a otro perfil o decisión carga su configuración.

## Proveedores SystemOne

El menú **Proveedores IA** administra las conexiones del motor de decisiones:

1. Pulsa **Añadir proveedor**.
2. Introduce un nombre y la dirección base del servidor compatible con SystemOne.
3. Elige autenticación: Bearer, API key en una cabecera o sin autenticación para un servidor local. Para cabeceras personalizadas, indica el nombre que exige el proveedor.
4. Introduce la API key y el identificador exacto del modelo indicado por tu proveedor. Deja el modelo vacío solo si tu servidor admite un modelo predeterminado; algunos proveedores rechazan esa solicitud con HTTP 422. La ruta predeterminada es `/v1/systemone` y puede cambiarse en opciones avanzadas.
5. Guarda y pulsa **Probar conexión**. Se envía una petición sintética y puede consumir cuota; no se usan correos reales ni se crean casos CRM.
6. Pulsa **Usar para decisiones** para seleccionar la conexión que utilizará el motor.

Guardar una conexión nueva no cambia la selección actual. Cambiar el proveedor o editar uno en uso afecta a las próximas consultas de IA de todos los perfiles, sin reiniciar el servicio. No es una configuración distinta por perfil.

Al editar, la clave no se muestra. Deja el campo vacío para conservarla o escribe una nueva para reemplazarla. Eliminarla requiere marcar la opción correspondiente; una conexión con autenticación necesita una clave. Para eliminar o deshabilitar un proveedor en uso, selecciona otro primero o vuelve a la configuración del servidor.

## Responsabilidades de los microservicios

- **Business Knowledge** conserva perfiles, dimensiones, respuestas y reglas.
- **Decision Engine** conserva sus proveedores en `decision_engine.ai_provider`, selecciona la conexión y ejecuta el contrato SystemOne.
- **API Gateway** enruta `/api/v1/admin/ai/**` al motor de decisiones.
- **Angular** utiliza los endpoints administrativos; no consulta bases de datos directamente.

La selección, administración, comprobación de conexión, transporte HTTP, persistencia y cifrado tienen clases y puertos separados. El motor de clasificación sigue dependiendo de `DecisionAiClient`, no de un proveedor concreto.

## Credenciales y despliegue

Las API keys se guardan con AES-GCM. Los endpoints requieren JWT de administrador y devuelven únicamente si existe una clave, nunca su valor o texto cifrado. Angular no añade las claves a almacenamiento local.

`AI_CONFIG_ENCRYPTION_KEY` es una clave maestra Base64 de 32 bytes configurada en el entorno del servidor. En este entorno local se generó una vez en `.env`, archivo excluido de Git. **Conserva esa clave con los respaldos de la base de datos:** sustituirla sin recifrar las credenciales impide descifrarlas. Es distinta de la API key de cada proveedor y de la clave de firma JWT.

Si no hay proveedor seleccionado en base de datos, se utiliza la configuración de entorno existente (`AI_BASE_URL`, `AI_API_KEY`, `AI_MODEL`, `AI_LANG`). El botón para volver a la configuración del servidor restaura ese comportamiento.

URLs y modelos deben corresponder a un servicio compatible con el contrato SystemOne. Añadir una API key no convierte una API con otro formato en SystemOne.

## Validación

- 83 pruebas Maven aprobadas, incluidos límites arquitectónicos, cifrado, permisos, credenciales ocultas, cambio de proveedor, transporte HTTP y diagnóstico de solicitudes.
- Compilación Angular y de las imágenes Docker aprobada.
- Recorrido Chrome de Business Knowledge y proveedores IA, con escrituras simuladas para conservar los datos del usuario.
- Prueba real de un proveedor temporal contra un servidor local simulado: persistencia cifrada en PostgreSQL, cabecera/ruta personalizadas y actualización del modelo sin reiniciar. El proveedor temporal se eliminó sin cambiar la selección activa.
- Validación adicional con el proveedor real configurado: prueba sintética de conexión y ejecución aislada del motor con cuatro decisiones aceptadas, sin publicar eventos ni crear casos CRM. Estas consultas pueden consumir cuota del proveedor.

Consulta [Seguimiento y revisión de solicitudes](REQUEST_DIAGNOSTICS.md) para interpretar los motivos que muestra el dashboard.
