package com.agnosticrouter.crm.application.service;

import com.agnosticrouter.contracts.DecisionValue;
import com.agnosticrouter.contracts.StandardTicketEvent;
import com.agnosticrouter.crm.application.port.CrmAssignmentMappingStore;
import com.agnosticrouter.crm.application.port.CrmFieldMappingStore;
import com.agnosticrouter.crm.domain.model.CrmAssignmentMapping;
import com.agnosticrouter.crm.domain.model.CrmFieldMapping;
import com.agnosticrouter.crm.domain.model.CrmOperation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds one payload from an operation + a ticket. Used by preview AND actual delivery. */
@Service
public class CrmPayloadComposer {
    private final TemplateRenderer template;
    private final CrmFieldMappingStore fields;
    private final CrmAssignmentMappingStore assignments;
    private final ObjectMapper mapper;

    public CrmPayloadComposer(TemplateRenderer template, CrmFieldMappingStore fields,
                              CrmAssignmentMappingStore assignments, ObjectMapper mapper) {
        this.template = template;
        this.fields = fields;
        this.assignments = assignments;
        this.mapper = mapper;
    }

    public RenderedPayload compose(CrmOperation operation, StandardTicketEvent ticket) {
        if (ticket.decisions() == null) throw new IllegalArgumentException("El ticket no contiene decisiones");
        try {
            String original = template.renderJson(operation.getBodyTemplate(), ticket);
            if (original == null) throw new IllegalArgumentException("La operación no tiene plantilla JSON");
            JsonNode parsed = mapper.readTree(original);
            if (!(parsed instanceof ObjectNode body)) throw new IllegalArgumentException("La plantilla CRM debe ser un objeto JSON");
            Set<String> applied = new LinkedHashSet<>();
            for (CrmFieldMapping rule : fields.findByOperationIdOrderByIdAsc(operation.getId())) {
                if (!rule.isActive()) continue;
                validateFieldName(rule.getTargetField());
                DecisionValue decision = ticket.decisions().get(rule.getDimensionCode());
                if (decision == null || decision.value() == null || decision.value().isBlank()) {
                    if (rule.isRequired()) throw new IllegalArgumentException("Sin valor para dimensión obligatoria " + rule.getDimensionCode());
                    continue;
                }
                JsonNode renderedValue = mappedValue(rule.getValueMapJson(), decision.value());
                body.set(rule.getTargetField(), renderedValue);
                applied.add(rule.getTargetField());
            }

            List<String> assigned = new ArrayList<>();
            Set<String> teams = new LinkedHashSet<>();
            JsonNode existingTeams = body.get("teamsIds");
            if (existingTeams != null && existingTeams.isArray()) {
                existingTeams.forEach(x -> { if (x.isTextual() && !x.asText().isBlank()) teams.add(x.asText()); });
            }
            String selectedUser = null;
            for (CrmAssignmentMapping mapping : assignments.findByOperationIdOrderByIdAsc(operation.getId())) {
                if (!mapping.isActive()) continue;
                DecisionValue decision = ticket.decisions().get(mapping.getDimensionCode());
                if (decision == null || !mapping.getOptionCode().equals(decision.value())) continue;
                if ("TEAM".equals(mapping.getAssignmentKind())) {
                    teams.add(mapping.getExternalId());
                    assigned.add("TEAM " + mapping.getDimensionCode() + "=" + mapping.getOptionCode());
                } else if ("USER".equals(mapping.getAssignmentKind())) {
                    if (selectedUser != null && !selectedUser.equals(mapping.getExternalId())) {
                        throw new IllegalArgumentException("Más de un responsable coincide con el ticket; ajusta los mapas");
                    }
                    selectedUser = mapping.getExternalId();
                    assigned.add("USER " + mapping.getDimensionCode() + "=" + mapping.getOptionCode());
                } else throw new IllegalArgumentException("Tipo de asignación no admitido: " + mapping.getAssignmentKind());
            }
            if (!teams.isEmpty()) {
                ArrayNode teamIds = mapper.createArrayNode();
                teams.forEach(teamIds::add);
                body.set("teamsIds", teamIds);
                applied.add("teamsIds");
            }
            if (selectedUser != null) {
                body.put("assignedUserId", selectedUser);
                applied.add("assignedUserId");
            }
            return new RenderedPayload(mapper.writeValueAsString(body), List.copyOf(applied), assigned);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Mapeo CRM inválido: " + ex.getOriginalMessage(), ex);
        }
    }

    private JsonNode mappedValue(String json, String code) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (json == null || json.isBlank()) return TextNode.valueOf(code);
        JsonNode values = mapper.readTree(json);
        if (!values.isObject()) throw new IllegalArgumentException("valueMapJson debe ser objeto JSON");
        JsonNode found = values.get(code);
        return found == null || found.isNull() ? TextNode.valueOf(code) : found.deepCopy();
    }

    private void validateFieldName(String field) {
        if (field == null || !field.matches("[A-Za-z][A-Za-z0-9_]{0,119}")) {
            throw new IllegalArgumentException("Campo de destino inválido: " + field);
        }
        if (Set.of("id", "createdAt", "modifiedAt", "deleted", "createdById", "teamsIds", "assignedUserId").contains(field)) {
            throw new IllegalArgumentException("No se puede mapear a un atributo reservado: " + field);
        }
    }
    public record RenderedPayload(String json, List<String> mappedFields, List<String> assignments) {}
}
