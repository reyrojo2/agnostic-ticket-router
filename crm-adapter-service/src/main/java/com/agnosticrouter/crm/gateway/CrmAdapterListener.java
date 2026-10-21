package com.agnosticrouter.crm.gateway;

import com.agnosticrouter.contracts.*;
import com.agnosticrouter.crm.service.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CrmAdapterListener {
    private final RabbitTemplate rabbit;
    private final CrmRouter router;
    private final CrmHttpExecutor executor;

    public CrmAdapterListener(RabbitTemplate rabbit, CrmRouter router, CrmHttpExecutor executor) {
        this.rabbit = rabbit;
        this.router = router;
        this.executor = executor;
    }

    @RabbitListener(queues = RabbitNames.Q_CRM_STANDARD_TICKETS)
    public void handle(StandardTicketEvent ticket) {
        if (ticket.requiresHumanReview()) {
            publish(
                    ticket,
                    "REVIEW_REQUIRED",
                    "El ticket requiere validación humana antes de enviarse al CRM.",
                    Map.of("decisions", ticket.decisions())
            );
            return;
        }

        try {
            CrmExecutionPlan plan = router.resolve(ticket);
            CrmExecutionResult result = executor.execute(plan, ticket);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("integrationCode", plan.integration().getCode());
            data.put("operationCode", plan.operation().getCode());
            data.put("httpStatus", result.statusCode());
            data.put("attempts", result.attempts());
            if (result.externalId() != null) data.put("externalTicketId", result.externalId());
            if (result.error() != null) data.put("error", result.error());

            if (result.success()) {
                publish(ticket, "CRM_CREATED", "Ticket creado correctamente en el CRM destino.", data);
            } else {
                publish(ticket, "CRM_ERROR", "No fue posible crear el ticket en el CRM destino.", data);
            }
        } catch (Exception ex) {
            publish(
                    ticket,
                    "CRM_ERROR",
                    "La configuración o ejecución del CRM produjo un error.",
                    Map.of("error", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage())
            );
        }
    }

    private void publish(StandardTicketEvent ticket, String type, String message, Map<String, Object> data) {
        rabbit.convertAndSend(
                RabbitNames.EXCHANGE,
                RabbitNames.RK_SYSTEM_EVENT,
                new SystemEvent(
                        ticket.requestId(),
                        ticket.profileCode(),
                        ticket.sender(),
                        type,
                        message,
                        data,
                        Instant.now()
                )
        );
    }
}
