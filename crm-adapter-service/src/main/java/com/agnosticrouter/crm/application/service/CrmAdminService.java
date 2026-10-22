package com.agnosticrouter.crm.application.service;
import com.agnosticrouter.crm.domain.model.CrmIntegration;
import com.agnosticrouter.crm.domain.model.CrmOperation;
import com.agnosticrouter.crm.domain.model.CrmRoute;
import com.agnosticrouter.crm.application.port.CrmIntegrationStore;
import com.agnosticrouter.crm.application.port.CrmOperationStore;
import com.agnosticrouter.crm.application.port.CrmRouteStore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;


import java.util.List;
@Service
public class CrmAdminService {
    private final CrmIntegrationStore integrations;
    private final CrmOperationStore operations;
    private final CrmRouteStore routes;

    public CrmAdminService(
            CrmIntegrationStore integrations,
            CrmOperationStore operations,
            CrmRouteStore routes
    ) {
        this.integrations = integrations;
        this.operations = operations;
        this.routes = routes;
    }
    public List<CrmIntegration> integrations() {
        return integrations.findAll();
    }
    public CrmIntegration createIntegration(IntegrationRequest request) {
        CrmIntegration integration = new CrmIntegration();
        apply(integration, request);
        return integrations.save(integration);
    }
    public CrmIntegration updateIntegration(Long id, IntegrationRequest request) {
        CrmIntegration integration = integrations.findById(id).orElseThrow();
        apply(integration, request);
        return integrations.save(integration);
    }
    public void deleteIntegration(Long id) {
        integrations.deleteById(id);
    }
    public List<CrmOperation> operations(Long integrationId) {
        return operations.findByIntegrationIdOrderByCodeAsc(integrationId);
    }
    public CrmOperation createOperation(
            Long integrationId,
            OperationRequest request
    ) {
        CrmOperation operation = new CrmOperation();
        operation.setIntegration(integrations.findById(integrationId).orElseThrow());
        apply(operation, request);
        return operations.save(operation);
    }
    public CrmOperation updateOperation(Long id, OperationRequest request) {
        CrmOperation operation = operations.findById(id).orElseThrow();
        apply(operation, request);
        return operations.save(operation);
    }
    public void deleteOperation(Long id) {
        operations.deleteById(id);
    }
    public List<CrmRoute> routes() {
        return routes.findAll();
    }
    public CrmRoute createRoute(RouteRequest request) {
        CrmRoute route = new CrmRoute();
        apply(route, request);
        return routes.save(route);
    }
    public CrmRoute updateRoute(Long id, RouteRequest request) {
        CrmRoute route = routes.findById(id).orElseThrow();
        apply(route, request);
        return routes.save(route);
    }
    public void deleteRoute(Long id) {
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
