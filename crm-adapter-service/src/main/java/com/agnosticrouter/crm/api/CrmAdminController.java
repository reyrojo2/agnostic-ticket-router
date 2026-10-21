package com.agnosticrouter.crm.api;

import com.agnosticrouter.crm.service.CrmAdminService;
import com.agnosticrouter.crm.service.CrmAdminService.*;
import com.agnosticrouter.crm.domain.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/crm")
public class CrmAdminController {
    private final CrmAdminService service;
    public CrmAdminController(CrmAdminService service) { this.service = service; }
    @GetMapping("/integrations")
    public List<CrmIntegration> integrations() {
        return service.integrations();
    }

    @PostMapping("/integrations")
    @ResponseStatus(HttpStatus.CREATED)
    public CrmIntegration createIntegration(@Valid @RequestBody IntegrationRequest request) {
        return service.createIntegration(request);
    }

    @PutMapping("/integrations/{id}")
    public CrmIntegration updateIntegration(@PathVariable Long id, @Valid @RequestBody IntegrationRequest request) {
        return service.updateIntegration(id, request);
    }

    @DeleteMapping("/integrations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIntegration(@PathVariable Long id) {
        service.deleteIntegration(id);
    }

    @GetMapping("/integrations/{integrationId}/operations")
    public List<CrmOperation> operations(@PathVariable Long integrationId) {
        return service.operations(integrationId);
    }

    @PostMapping("/integrations/{integrationId}/operations")
    @ResponseStatus(HttpStatus.CREATED)
    public CrmOperation createOperation(@PathVariable Long integrationId, @Valid @RequestBody OperationRequest request) {
        return service.createOperation(integrationId, request);
    }

    @PutMapping("/operations/{id}")
    public CrmOperation updateOperation(@PathVariable Long id, @Valid @RequestBody OperationRequest request) {
        return service.updateOperation(id, request);
    }

    @DeleteMapping("/operations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOperation(@PathVariable Long id) {
        service.deleteOperation(id);
    }

    @GetMapping("/routes")
    public List<CrmRoute> routes() {
        return service.routes();
    }

    @PostMapping("/routes")
    @ResponseStatus(HttpStatus.CREATED)
    public CrmRoute createRoute(@Valid @RequestBody RouteRequest request) {
        return service.createRoute(request);
    }

    @PutMapping("/routes/{id}")
    public CrmRoute updateRoute(@PathVariable Long id, @Valid @RequestBody RouteRequest request) {
        return service.updateRoute(id, request);
    }

    @DeleteMapping("/routes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoute(@PathVariable Long id) {
        service.deleteRoute(id);
    }

}
