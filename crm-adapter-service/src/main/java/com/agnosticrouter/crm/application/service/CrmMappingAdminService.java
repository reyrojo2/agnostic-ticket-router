package com.agnosticrouter.crm.application.service;

import com.agnosticrouter.crm.application.port.*;
import com.agnosticrouter.crm.domain.model.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;
import java.util.List;

/** Manages mappings without requiring customers to edit SQL or Java source. */
@Service
public class CrmMappingAdminService {
    private final CrmOperationStore operations;
    private final CrmFieldMappingStore fields;
    private final CrmAssignmentMappingStore assignments;
    private final CrmCatalogPort catalog;

    public CrmMappingAdminService(CrmOperationStore operations, CrmFieldMappingStore fields,
                                  CrmAssignmentMappingStore assignments, CrmCatalogPort catalog) {
        this.operations = operations; this.fields = fields; this.assignments = assignments; this.catalog = catalog;
    }
    public List<CrmFieldMapping> fields(Long operationId) { requireOperation(operationId); return fields.findByOperationIdOrderByIdAsc(operationId); }
    public CrmFieldMapping saveField(Long operationId, Long id, FieldRequest r) {
        CrmFieldMapping entity = id == null ? new CrmFieldMapping() : fields.findById(id).orElseThrow();
        if (id != null && !entity.getOperation().getId().equals(operationId)) throw new IllegalArgumentException("El mapeo pertenece a otra operación");
        if (!r.dimensionCode().matches("[A-Z][A-Z0-9_]{0,79}")) throw new IllegalArgumentException("Código de dimensión inválido");
        if (!r.targetField().matches("[A-Za-z][A-Za-z0-9_]{0,119}")) throw new IllegalArgumentException("Campo de destino inválido");
        if (java.util.Set.of("id", "createdAt", "modifiedAt", "deleted", "createdById", "teamsIds", "assignedUserId").contains(r.targetField())) throw new IllegalArgumentException("Atributo protegido");
        String valueMap = r.valueMapJson() == null || r.valueMapJson().isBlank() ? "{}" : r.valueMapJson();
        try {
            if (!new com.fasterxml.jackson.databind.ObjectMapper().readTree(valueMap).isObject()) throw new IllegalArgumentException("Las equivalencias deben ser un objeto JSON");
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalArgumentException("Equivalencias JSON inválidas", ex); }
        entity.setOperation(requireOperation(operationId));
        entity.setDimensionCode(r.dimensionCode()); entity.setTargetField(r.targetField());
        entity.setValueMapJson(valueMap); entity.setRequired(r.required()); entity.setActive(r.active());
        return fields.save(entity);
    }
    public void deleteField(Long id) { fields.deleteById(id); }
    public List<CrmAssignmentMapping> assignments(Long operationId) { requireOperation(operationId); return assignments.findByOperationIdOrderByIdAsc(operationId); }
    public CrmAssignmentMapping saveAssignment(Long operationId, Long id, AssignmentRequest r) {
        CrmAssignmentMapping entity = id == null ? new CrmAssignmentMapping() : assignments.findById(id).orElseThrow();
        if (id != null && !entity.getOperation().getId().equals(operationId)) throw new IllegalArgumentException("El mapeo pertenece a otra operación");
        if (!r.dimensionCode().matches("[A-Z][A-Z0-9_]{0,79}")) throw new IllegalArgumentException("Dimensión inválida");
        if (!r.optionCode().matches("[A-Za-z0-9_-]{1,120}")) throw new IllegalArgumentException("Código de opción inválido");
        if (!"TEAM".equals(r.assignmentKind()) && !"USER".equals(r.assignmentKind())) throw new IllegalArgumentException("Tipo de asignación debe ser TEAM o USER");
        if (!r.externalId().matches("[A-Za-z0-9_-]{1,100}")) throw new IllegalArgumentException("Identificador externo inválido");
        entity.setOperation(requireOperation(operationId));
        entity.setDimensionCode(r.dimensionCode()); entity.setOptionCode(r.optionCode());
        entity.setAssignmentKind(r.assignmentKind()); entity.setExternalId(r.externalId());
        entity.setExternalName(r.externalName()); entity.setActive(r.active());
        return assignments.save(entity);
    }
    public void deleteAssignment(Long id) { assignments.deleteById(id); }

    /** Explicit admin action: link existing team or create it through EspoCRM if absent. */
    public TeamProvisionResult ensureTeam(Long operationId, TeamRequest request) {
        if (request.dimensionCode() == null || !request.dimensionCode().matches("[A-Z][A-Z0-9_]{0,79}")
                || request.optionCode() == null || !request.optionCode().matches("[A-Za-z0-9_-]{1,120}")) {
            throw new IllegalArgumentException("Dimensión u opción inválida; no se ha modificado EspoCRM");
        }
        CrmOperation operation = requireOperation(operationId);
        CrmCatalogPort.ExternalRecord team = catalog.ensureTeam(operation.getIntegration(), request.name());
        CrmAssignmentMapping existing = assignments.findByOperationIdOrderByIdAsc(operationId).stream()
                .filter(x -> x.getDimensionCode().equals(request.dimensionCode()) && x.getOptionCode().equals(request.optionCode())
                        && "TEAM".equals(x.getAssignmentKind())).findFirst().orElse(null);
        CrmAssignmentMapping saved = saveAssignment(operationId, existing == null ? null : existing.getId(),
                new AssignmentRequest(request.dimensionCode(), request.optionCode(), "TEAM", team.id(), team.name(), true));
        return new TeamProvisionResult(team.id(), team.name(), team.created(), saved.getId());
    }
    private CrmOperation requireOperation(Long operationId) { return operations.findById(operationId).orElseThrow(() -> new IllegalArgumentException("Operación CRM no encontrada")); }
    public record FieldRequest(@NotBlank String dimensionCode, @NotBlank String targetField,
                               String valueMapJson, boolean required, boolean active) {}
    public record AssignmentRequest(@NotBlank String dimensionCode, @NotBlank String optionCode,
                                    @NotBlank String assignmentKind, @NotBlank String externalId,
                                    String externalName, boolean active) {}
    public record TeamRequest(@NotBlank String dimensionCode, @NotBlank String optionCode, @NotBlank String name) {}
    public record TeamProvisionResult(String id, String name, boolean created, Long mappingId) {}
}
