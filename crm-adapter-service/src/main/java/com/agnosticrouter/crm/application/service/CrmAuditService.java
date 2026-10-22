package com.agnosticrouter.crm.application.service;

import com.agnosticrouter.contracts.StandardTicketEvent;
import com.agnosticrouter.crm.application.model.CrmExecutionPlan;
import com.agnosticrouter.crm.application.model.CrmExecutionResult;
import com.agnosticrouter.crm.application.port.CrmDeliveryAuditStore;
import com.agnosticrouter.crm.domain.model.CrmDeliveryAudit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stores metadata and outcomes only: never API keys, tokens, raw email body or response payload. */
@Service
public class CrmAuditService {
    private final CrmDeliveryAuditStore store;
    private final ObjectMapper mapper;
    private final CrmPayloadComposer composer;
    public CrmAuditService(CrmDeliveryAuditStore store, ObjectMapper mapper, CrmPayloadComposer composer) {
        this.store = store; this.mapper = mapper; this.composer = composer;
    }
    public void save(StandardTicketEvent ticket, CrmExecutionPlan plan, String outcome,
                     CrmExecutionResult result, String error) {
        CrmDeliveryAudit audit = new CrmDeliveryAudit();
        audit.setRequestId(shortValue(ticket.requestId(), 150));
        audit.setProfileCode(shortValue(ticket.profileCode(), 80));
        audit.setIntegrationCode(plan == null ? null : shortValue(plan.integration().getCode(), 80));
        audit.setOperationCode(plan == null ? null : shortValue(plan.operation().getCode(), 80));
        audit.setOutcome(outcome);
        audit.setExternalId(result == null ? null : shortValue(result.externalId(), 150));
        audit.setHttpStatus(result == null ? null : result.statusCode());
        audit.setAttempts(result == null ? null : result.attempts());
        // No raw message content: only dimension code -> selected option code.
        Map<String, String> decisions = new LinkedHashMap<>();
        if (ticket.decisions() != null) ticket.decisions().forEach((k, v) -> decisions.put(k, v == null ? null : v.value()));
        try {
            audit.setDecisionValuesJson(mapper.writeValueAsString(decisions));
            if (plan != null) {
                audit.setMappedFieldsJson(mapper.writeValueAsString(composer.compose(plan.operation(), ticket).mappedFields()));
            }
        } catch (Exception ignored) {
            audit.setMappedFieldsJson("[]");
        }
        audit.setErrorSummary(shortValue(error, 1000));
        store.save(audit);
    }
    public List<CrmDeliveryAudit> latest() { return store.findTop50ByOrderByCreatedAtDesc(); }
    private String shortValue(String value, int limit) { return value == null ? null : value.substring(0, Math.min(value.length(), limit)); }
}
