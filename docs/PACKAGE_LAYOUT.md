# Estructura de paquetes Java

Los servicios siguen la organización de capas:

- `api/controller`: adaptadores HTTP.
- `application/service`: casos de uso y coordinación.
- `application/port`: interfaces locales de las dependencias.
- `domain/model` y `domain/service`: entidades y reglas de negocio.
- `infrastructure`: adaptadores de clientes, persistencia, mensajería y seguridad.
- `config`: configuración de Spring.

Las dependencias apuntan hacia aplicación y dominio; infraestructura implementa puertos de aplicación. Los contratos compartidos están en `common-contracts`.

Verificar con Maven (`mvn -B -ntp test`) y Docker Compose antes de integrar el PR; la revisión estática no sustituye pruebas de compilación ni integración.
