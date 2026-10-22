# Configurar el envío al CRM

La pantalla de Integraciones CRM utiliza los perfiles, dimensiones y opciones guardados en Business Knowledge. No es necesario volver a escribir sus códigos ni los IDs de los equipos.

## Selección inicial

Elige el perfil de solicitudes, la conexión y la acción en los tres selectores superiores. La primera conexión activa y su acción se seleccionan al abrir la pantalla. Los perfiles que comparten una misma acción también comparten los vínculos de esa acción.

## 1. Conexión

Si la conexión ya existe, continúa al paso Campos. Para una nueva conexión EspoCRM, guarda su nombre, dirección y referencia de credencial; luego pulsa Preparar creación de casos. La acción estándar se completa automáticamente. La autenticación, plantilla, cabeceras y tiempos de espera se encuentran en las opciones técnicas desplegables.

## 2. Campos

- Los vínculos actuales aparecen con los nombres de las dimensiones y campos.
- Para modificar uno, pulsa Editar. Para añadir otro, selecciona una dimensión y un campo disponible del CRM.
- La tabla de equivalencias muestra las opciones de Business Knowledge. Si el campo de EspoCRM tiene opciones predefinidas, selecciona el valor correspondiente en cada fila.
- Guarda el vínculo antes de simular. Actualizar campos vuelve a consultar EspoCRM si acabas de crear un campo allí.

## 3. Equipos

- Selecciona la dimensión que determina el área.
- Aparece una fila por opción: Backoffice, Soporte, etc.
- Elige un equipo y, si corresponde, un responsable por nombre. Las asignaciones existentes se cargan automáticamente.
- Pulsa Guardar equipos y responsables. Los vínculos existentes se actualizan; no se crean duplicados al editarlos.
- Elegir Sin equipo o Sin responsable en un vínculo existente propone quitar esa asignación al guardar, con confirmación.

Si no aparecen equipos, revisa el acceso del usuario API de EspoCRM y pulsa Actualizar equipos. Los vínculos guardados se conservan incluso si el catálogo no puede consultarse. La creación de equipos nuevos está en un desplegable separado y requiere una acción explícita.

## 4. Comprobar

Elige resultados de ejemplo en los selectores y pulsa Simular resultado. Se muestran el equipo, el responsable, los campos y la conexión que selecciona la ruta del perfil. La petición JSON queda en un detalle desplegable.

La simulación usa la configuración guardada. No clasifica un correo ni crea un caso real. La validación de campos y referencias es una comprobación adicional del catálogo; no demuestra todos los permisos de escritura.

## Rutas e historial

Rutas de envío permite elegir la conexión o acción para el perfil, opcionalmente según una dimensión y opción. Todos los valores se seleccionan de listas. Las rutas específicas nuevas reciben una prioridad anterior a la ruta general estándar; revisa el orden avanzado si ya tienes prioridades personalizadas.

Para distribuir solicitudes entre áreas del mismo CRM, utiliza Equipos. El historial muestra los resultados de entregas reales de todas las conexiones.

## Comprobación de la interfaz (22 de octubre de 2026)

Se verificaron la compilación de Angular y el recorrido en Chrome: edición visual de equivalencias, carga y actualización de asignaciones, simulación y creación de rutas con selectores dependientes. Las escrituras del recorrido automatizado se interceptaron y simularon para conservar los datos reales. También se comprobó la presentación a 650 px sin desbordamiento horizontal de la página.
