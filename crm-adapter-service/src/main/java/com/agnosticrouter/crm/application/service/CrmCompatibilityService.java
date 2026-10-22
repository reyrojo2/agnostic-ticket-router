package com.agnosticrouter.crm.application.service;

import com.agnosticrouter.crm.application.port.*;
import com.agnosticrouter.crm.domain.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Read-only schema and references validation; does not create or modify CRM fields. */
@Service
public class CrmCompatibilityService {
    private final CrmOperationStore operations;
    private final CrmFieldMappingStore fields;
    private final CrmAssignmentMappingStore assignments;
    private final CrmCatalogPort catalog;
    private final ObjectMapper mapper;

    public CrmCompatibilityService(CrmOperationStore operations, CrmFieldMappingStore fields,
                                   CrmAssignmentMappingStore assignments, CrmCatalogPort catalog, ObjectMapper mapper) {
        this.operations = operations; this.fields = fields; this.assignments = assignments;
        this.catalog = catalog; this.mapper = mapper;
    }
    public CompatibilityResult validate(Long operationId) {
        CrmOperation op = operations.findById(operationId).orElseThrow();
        CrmIntegration integration = op.getIntegration();
        if (!op.getPathTemplate().matches("/api/v1/[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("La operación debe apuntar a un endpoint CRUD simple para validar su entidad");
        }
        String entity = op.getPathTemplate().substring("/api/v1/".length());
        Map<String, Map<String, Object>> definition = catalog.fields(integration, entity);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        try {
            JsonNode template = mapper.readTree(op.getBodyTemplate());
            if (!template.isObject()) throw new IllegalArgumentException("La plantilla debe ser un objeto JSON");
            template.fieldNames().forEachRemaining(keys::add);
        } catch (Exception ex) { errors.add("Plantilla JSON inválida"); }
        for (CrmFieldMapping field : fields.findByOperationIdOrderByIdAsc(operationId)) {
            if (!field.isActive()) continue;
            keys.add(field.getTargetField());
            validateOptions(field, definition.get(field.getTargetField()), errors, warnings);
        }
        for (String key : keys) {
            if (key.equals("teamsIds") || key.equals("assignedUserId")) continue;
            Map<String, Object> meta = definition.get(key);
            if (meta == null) errors.add("Campo inexistente en EspoCRM: " + key);
            else if (Boolean.TRUE.equals(meta.get("readOnly")) || Boolean.TRUE.equals(meta.get("notStorable"))) {
                errors.add("Campo de solo lectura o no persistente: " + key);
            }
        }
        for (CrmAssignmentMapping mapping : assignments.findByOperationIdOrderByIdAsc(operationId)) {
            if (!mapping.isActive()) continue;
            String kind = mapping.getAssignmentKind();
            if (!"TEAM".equals(kind) && !"USER".equals(kind)) { errors.add("Tipo de asignación inválido: " + kind); continue; }
            if (!catalog.recordExists(integration, "TEAM".equals(kind) ? "Team" : "User", mapping.getExternalId())) {
                errors.add("No existe en EspoCRM " + kind + " con ID " + mapping.getExternalId());
            }
        }
        if (fields.findByOperationIdOrderByIdAsc(operationId).isEmpty()) {
            warnings.add("No hay mapeos estructurados: la clasificación puede quedarse solo en la descripción");
        }
        return new CompatibilityResult(errors.isEmpty(), entity, errors, warnings, List.copyOf(definition.keySet()));
    }
    private void validateOptions(CrmFieldMapping f, Map<String, Object> meta, List<String> errors, List<String> warnings) {
        if (meta == null) return;
        Object opts = meta.get("options");
        if (!(opts instanceof List<?> allowed) || allowed.isEmpty()) return;
        try {
            JsonNode valueMap = mapper.readTree(f.getValueMapJson());
            if (!valueMap.isObject()) {errors.add("Equivalencias inválidas en " + f.getTargetField()); return;}
            valueMap.fields().forEachRemaining(e -> {
                if (!allowed.contains(e.getValue().asText())) {
                    errors.add("Opción no válida para " + f.getTargetField() + ": " + e.getValue().asText());
                }
            });
            if (valueMap.isEmpty()) warnings.add("El campo " + f.getTargetField() + " tiene opciones predefinidas: comprueba que las decisiones coincidan con los valores CRM");
        } catch (Exception ex) {errors.add("Equivalencias JSON inválidas en " + f.getTargetField());}
    }
    public record CompatibilityResult(boolean compatible, String entityType,
                                      List<String> errors, List<String> warnings, List<String> availableFields) {}
}
