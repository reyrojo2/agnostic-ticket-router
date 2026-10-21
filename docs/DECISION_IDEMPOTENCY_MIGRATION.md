# Migración de idempotencia y alcance de la refactorización

## Importante antes de arrancar Decision Engine

El procesador ahora consulta **solo** `decision_engine.processed_request`. La versión anterior consultaba adicionalmente `observability.request_trace`, lo que acoplaba dos servicios por SQL.

Para que los correos anteriores no vuelvan a clasificarse, ejecutar **una sola vez, con Decision Engine detenido**, esta migración administrativa (no corresponde al código de ejecución del microservicio):

```sql
INSERT INTO decision_engine.processed_request(request_id, processed_at)
SELECT request_id, COALESCE(classified_at, now())
FROM observability.request_trace
WHERE classified_at IS NOT NULL
ON CONFLICT (request_id) DO NOTHING;
```

La migración es idempotente y no borra registros. Hacer respaldo de PostgreSQL antes de ejecutarla. Mientras dure la transición, mantener Gmail Connector pausado y Decision Engine detenido. Después se puede comprobar la cantidad de IDs con:
```sql
SELECT count(*) FROM decision_engine.processed_request;
```

**Precaución:** estas modificaciones no ofrecen entrega exactamente una vez. Publicar el evento y marcarlo en PostgreSQL aún son operaciones separadas; un fallo entre ambas puede duplicar la publicación. La siguiente evolución técnica debe usar outbox transaccional, confirmaciones de publicación y deduplicación también en los consumidores CRM y Notificaciones. No aumentar concurrencia del consumidor hasta que ese mecanismo esté implementado.

## Separación aplicada

- `GenericDecisionEngine` coordina el flujo HARD > SOFT/IA sin transformar clases HTTP ni RabbitMQ.
- `HardRuleDecisionResolver` resuelve HARD concluyentes.
- `AiDimensionEvaluator` crea preguntas y transforma respuestas por tipo de dimensión.
- `DecisionContextBuilder` prepara contexto y pistas SOFT.
- `DimensionDependencyPolicy` valida dependencias.
- `ProcessIncomingEmail` implementa el caso de uso de clasificación y deduplicación local.
- `DecisionListener` adapta RabbitMQ; `ProcessedDecisionRepository` contiene SQL exclusivamente propio.

No se introducen cuotas artificiales ni llamadas adicionales a Laya. No se cambian contratos JSON ni nombres de colas.
