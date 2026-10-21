package com.agnosticrouter.crm.application.service;

import com.agnosticrouter.crm.application.model.CrmExecutionPlan;
import com.agnosticrouter.crm.application.model.CrmExecutionResult;
import com.agnosticrouter.crm.application.port.CrmExecutionPort;
import com.agnosticrouter.crm.application.port.CrmOutcomePublisher;

import com.agnosticrouter.contracts.StandardTicketEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Coordinates CRM routing and sends its business outcome through an output port. */
@Service
public class ProcessClassifiedTicket {
    private final CrmRouter router;
    private final CrmExecutionPort executor;
    private final CrmOutcomePublisher outcomes;

    public ProcessClassifiedTicket(CrmRouter router, CrmExecutionPort executor, CrmOutcomePublisher outcomes) {
        this.router=router;this.executor=executor;this.outcomes=outcomes;
    }

    public void process(StandardTicketEvent ticket) {
        if (ticket.requiresHumanReview()) {
            outcomes.publish(
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
                outcomes.publish(ticket, "CRM_CREATED", "Ticket creado correctamente en el CRM destino.", data);
            } else {
                outcomes.publish(ticket, "CRM_ERROR", "No fue posible crear el ticket en el CRM destino.", data);
            }
        } catch (Exception ex) {
            outcomes.publish(
                    ticket,
                    "CRM_ERROR",
                    "La configuración o ejecución del CRM produjo un error.",
                    Map.of("error", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage())
            );
        }
    }


}
