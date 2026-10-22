package com.agnosticrouter.crm.application.service;

import com.agnosticrouter.contracts.DecisionValue;
import com.agnosticrouter.contracts.StandardTicketEvent;
import com.agnosticrouter.crm.application.model.CrmExecutionPlan;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CrmPreviewService {
    private final CrmRouter router;
    private final CrmPayloadComposer composer;

    public CrmPreviewService(CrmRouter router, CrmPayloadComposer composer) {
        this.router = router; this.composer = composer;
    }
    /** Safe dry-run: NEVER contacts the CRM or publishes a ticket. */
    public PreviewResult preview(PreviewRequest request) {
        Map<String, DecisionValue> decisions = new LinkedHashMap<>();
        if (request.decisions() != null) {
            request.decisions().forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                    decisions.put(key, new DecisionValue(key, value, "PREVIEW", null, null, null, null, true, Map.of(), null));
                }
            });
        }
        StandardTicketEvent sample = new StandardTicketEvent(
                "PREVIEW-" + UUID.randomUUID(), request.profileCode(), 1,
                request.sender() == null ? "demo@example.test" : request.sender(),
                List.of(), List.of(), request.subject() == null ? "Caso de prueba" : request.subject(),
                request.body() == null ? "Solicitud de prueba" : request.body(), Map.of(), decisions, false, Instant.now());
        CrmExecutionPlan plan = router.resolve(sample);
        CrmPayloadComposer.RenderedPayload rendered = composer.compose(plan.operation(), sample);
        return new PreviewResult(plan.integration().getCode(), plan.operation().getCode(),
                plan.route().getId(), rendered.json(), rendered.mappedFields(), rendered.assignments());
    }
    public record PreviewRequest(String profileCode, String sender, String subject, String body, Map<String, String> decisions) {}
    public record PreviewResult(String integrationCode, String operationCode, Long routeId,
                                String bodyJson, List<String> mappedFields, List<String> assignments) {}
}
