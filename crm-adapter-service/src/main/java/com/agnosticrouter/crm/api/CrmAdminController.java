package com.agnosticrouter.crm.api;

import com.agnosticrouter.crm.domain.*;
import com.agnosticrouter.crm.repo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/crm")
public class CrmAdminController {
    private final CrmIntegrationRepository integrations;
    private final CrmOperationRepository operations;
    private final CrmRouteRepository routes;

    public CrmAdminController(
            CrmIntegrationRepository integrations,
            CrmOperationRepository operations,
            CrmRouteRepository routes
    ) {
        this.integrations = integrations;
        this.operations = operations;
        this.routes = routes;
    }

    @GetMapping("/integrations")
    public List<CrmIntegration> integrations() {
        return integrations.findAll();
    }

    @PostMapping("/integrations")
    @ResponseStatus(HttpStatus.CREATED)
    public CrmIntegration createIntegration(@Valid @RequestBody IntegrationRequest request) {
        CrmIntegration integration = new CrmIntegration();
        apply(integration, request);
        return integrations.save(integration);
    }

    @PutMapping("/integrations/{id}")
    public CrmIntegration updateIntegration(@PathVariable Long id, @Valid @RequestBody IntegrationRequest request) {
        CrmIntegration integration = integrations.findById(id).orElseThrow();
        apply(integration, request);
        return integrations.save(integration);
    }

    @DeleteMapping("/integrations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIntegration(@PathVariable Long id) {
        integrations.deleteById(id);
    }

    @GetMapping("/integrations/{integrationId}/operations")
    public List<CrmOperation> operations(@PathVariable Long integrationId) {
        return operations.findByIntegrationIdOrderByCodeAsc(integrationId);
    }

    @PostMapping("/integrations/{integrationId}/operations")
    @ResponseStatus(HttpStatus.CREATED)
    public CrmOperation createOperation(
            @PathVariable Long integrationId,
            @Valid @RequestBody OperationRequest request
    ) {
        CrmOperation operation = new CrmOperation();
        operation.setIntegration(integrations.findById(integrationId).orElseThrow());
        apply(operation, request);
        return operations.save(operation);
    }

    @PutMapping("/operations/{id}")
    public CrmOperation updateOperation(@PathVariable Long id, @Valid @RequestBody OperationRequest request) {
        CrmOperation operation = operations.findById(id).orElseThrow();
        apply(operation, request);
        return operations.save(operation);
    }

    @DeleteMapping("/operations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOperation(@PathVariable Long id) {
        operations.deleteById(id);
    }

    @GetMapping("/routes")
    public List<CrmRoute> routes() {
        return routes.findAll();
    }

    @PostMapping("/routes")
    @ResponseStatus(HttpStatus.CREATED)
    public CrmRoute createRoute(@Valid @RequestBody RouteRequest request) {
        CrmRoute route = new CrmRoute();
        apply(route, request);
        return routes.save(route);
    }

    @PutMapping("/routes/{id}")
    public CrmRoute updateRoute(@PathVariable Long id, @Valid @RequestBody RouteRequest request) {
        CrmRoute route = routes.findById(id).orElseThrow();
        apply(route, request);
        return routes.save(route);
    }

    @DeleteMapping("/routes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoute(@PathVariable Long id) {
        routes.deleteById(id);
    }

    private void apply(CrmIntegration integration, IntegrationRequest request) {
        integration.setCode(request.code());
        integration.setName(request.name());
        integration.setBaseUrl(request.baseUrl());
        integration.setAuthType(request.authType());
        integration.setUsernameRef(request.usernameRef());
        integration.setSecretRef(request.secretRef());
        integration.setApiKeyName(request.apiKeyName());
        integration.setTokenUrl(request.tokenUrl());
        integration.setScope(request.scope());
        integration.setDefaultHeadersJson(request.defaultHeadersJson());
        integration.setActive(request.active());
    }

    private void apply(CrmOperation operation, OperationRequest request) {
        operation.setCode(request.code());
        operation.setHttpMethod(request.httpMethod());
        operation.setPathTemplate(request.pathTemplate());
        operation.setContentType(request.contentType());
        operation.setHeadersTemplateJson(request.headersTemplateJson());
        operation.setBodyTemplate(request.bodyTemplate());
        operation.setSuccessStatusCodes(request.successStatusCodes());
        operation.setExternalIdPath(request.externalIdPath());
        operation.setTimeoutMs(request.timeoutMs());
        operation.setMaxAttempts(request.maxAttempts());
        operation.setRetryDelayMs(request.retryDelayMs());
        operation.setActive(request.active());
    }

    private void apply(CrmRoute route, RouteRequest request) {
        route.setProfileCode(request.profileCode());
        route.setDimensionCode(request.dimensionCode());
        route.setOptionCode(request.optionCode());
        route.setIntegrationCode(request.integrationCode());
        route.setOperationCode(request.operationCode());
        route.setPriority(request.priority());
        route.setActive(request.active());
    }

    public record IntegrationRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String baseUrl,
            String authType,
            String usernameRef,
            String secretRef,
            String apiKeyName,
            String tokenUrl,
            String scope,
            String defaultHeadersJson,
            Boolean active
    ) {
        public IntegrationRequest {
            if (authType == null) authType = "NONE";
            if (active == null) active = true;
        }
    }

    public record OperationRequest(
            @NotBlank String code,
            String httpMethod,
            @NotBlank String pathTemplate,
            String contentType,
            String headersTemplateJson,
            String bodyTemplate,
            String successStatusCodes,
            String externalIdPath,
            Integer timeoutMs,
            Integer maxAttempts,
            Long retryDelayMs,
            Boolean active
    ) {
        public OperationRequest {
            if (httpMethod == null) httpMethod = "POST";
            if (contentType == null) contentType = "application/json";
            if (successStatusCodes == null) successStatusCodes = "200,201,202";
            if (timeoutMs == null) timeoutMs = 15000;
            if (maxAttempts == null) maxAttempts = 3;
            if (retryDelayMs == null) retryDelayMs = 1000L;
            if (active == null) active = true;
        }
    }

    public record RouteRequest(
            @NotBlank String profileCode,
            String dimensionCode,
            String optionCode,
            @NotBlank String integrationCode,
            String operationCode,
            Integer priority,
            Boolean active
    ) {
        public RouteRequest {
            if (operationCode == null) operationCode = "CREATE_TICKET";
            if (priority == null) priority = 100;
            if (active == null) active = true;
        }
    }
}
