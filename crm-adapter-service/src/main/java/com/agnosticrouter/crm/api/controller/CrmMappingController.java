package com.agnosticrouter.crm.api.controller;

import com.agnosticrouter.crm.application.port.CrmCatalogPort;
import com.agnosticrouter.crm.application.port.CrmIntegrationStore;
import com.agnosticrouter.crm.application.service.CrmAuditService;
import com.agnosticrouter.crm.application.service.CrmCompatibilityService;
import com.agnosticrouter.crm.application.service.CrmMappingAdminService;
import com.agnosticrouter.crm.application.service.CrmPreviewService;
import com.agnosticrouter.crm.application.service.CrmMappingAdminService.*;
import com.agnosticrouter.crm.domain.model.CrmAssignmentMapping;
import com.agnosticrouter.crm.domain.model.CrmDeliveryAudit;
import com.agnosticrouter.crm.domain.model.CrmFieldMapping;
import com.agnosticrouter.crm.domain.model.CrmIntegration;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** Administrative endpoints; protected by the service's JWT security configuration. */
@RestController
@RequestMapping("/api/v1/admin/crm")
public class CrmMappingController {
    private final CrmMappingAdminService mappings;
    private final CrmPreviewService previews;
    private final CrmCompatibilityService compatibility;
    private final CrmAuditService audits;
    private final CrmCatalogPort catalog;
    private final CrmIntegrationStore integrations;

    public CrmMappingController(CrmMappingAdminService mappings, CrmPreviewService previews,
                                CrmCompatibilityService compatibility, CrmAuditService audits,
                                CrmCatalogPort catalog, CrmIntegrationStore integrations) {
        this.mappings = mappings; this.previews = previews; this.compatibility = compatibility;
        this.audits = audits; this.catalog = catalog; this.integrations = integrations;
    }

    @GetMapping("/operations/{id}/field-mappings")
    public List<CrmFieldMapping> fields(@PathVariable Long id) { return mappings.fields(id); }

    @PostMapping("/operations/{id}/field-mappings")
    @ResponseStatus(HttpStatus.CREATED)
    public CrmFieldMapping addField(@PathVariable Long id, @Valid @RequestBody FieldRequest request) {
        return mappings.saveField(id, null, request);
    }

    @PutMapping("/operations/{operationId}/field-mappings/{id}")
    public CrmFieldMapping editField(@PathVariable Long operationId, @PathVariable Long id,
                                     @Valid @RequestBody FieldRequest request) {
        return mappings.saveField(operationId, id, request);
    }

    @DeleteMapping("/field-mappings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeField(@PathVariable Long id) { mappings.deleteField(id); }

    @GetMapping("/operations/{id}/assignments")
    public List<CrmAssignmentMapping> assignments(@PathVariable Long id) { return mappings.assignments(id); }

    @PostMapping("/operations/{id}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public CrmAssignmentMapping addAssignment(@PathVariable Long id, @Valid @RequestBody AssignmentRequest request) {
        return mappings.saveAssignment(id, null, request);
    }

    @PutMapping("/operations/{operationId}/assignments/{id}")
    public CrmAssignmentMapping editAssignment(@PathVariable Long operationId, @PathVariable Long id,
                                               @Valid @RequestBody AssignmentRequest request) {
        return mappings.saveAssignment(operationId, id, request);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAssignment(@PathVariable Long id) { mappings.deleteAssignment(id); }

    @GetMapping("/integrations/{integrationId}/catalog/fields")
    public Map<String, Map<String, Object>> fieldsCatalog(@PathVariable Long integrationId,
                                                           @RequestParam(defaultValue = "Case") String entity) {
        return catalog.fields(integration(integrationId), entity);
    }
    @GetMapping("/integrations/{integrationId}/catalog/teams")
    public List<CrmCatalogPort.ExternalRecord> teams(@PathVariable Long integrationId) {
        return catalog.records(integration(integrationId), "Team");
    }
    @GetMapping("/integrations/{integrationId}/catalog/users")
    public List<CrmCatalogPort.ExternalRecord> users(@PathVariable Long integrationId) {
        return catalog.records(integration(integrationId), "User");
    }

    /** Performs an explicitly requested upsert in EspoCRM and stores the resulting mapping. */
    @PostMapping("/operations/{id}/teams/ensure")
    public TeamProvisionResult ensureTeam(@PathVariable Long id, @Valid @RequestBody TeamRequest request) {
        return mappings.ensureTeam(id, request);
    }
    @PostMapping("/operations/{id}/validate")
    public CrmCompatibilityService.CompatibilityResult validate(@PathVariable Long id) {
        return compatibility.validate(id);
    }
    @PostMapping("/preview")
    public CrmPreviewService.PreviewResult preview(@RequestBody CrmPreviewService.PreviewRequest request) {
        return previews.preview(request);
    }
    @GetMapping("/audits")
    public List<CrmDeliveryAudit> latestAudit() { return audits.latest(); }

    private CrmIntegration integration(Long id) {
        return integrations.findById(id).orElseThrow(() -> new IllegalArgumentException("Integración CRM no encontrada"));
    }
}
