package com.agnosticrouter.crm.infrastructure.client;
import com.agnosticrouter.crm.application.model.CrmExecutionPlan;
import com.agnosticrouter.crm.application.model.CrmExecutionResult;
import com.agnosticrouter.crm.application.port.CrmExecutionPort;
import com.agnosticrouter.crm.application.service.ResponseValueExtractor;
import com.agnosticrouter.crm.application.service.TemplateRenderer;
import com.agnosticrouter.crm.application.service.CrmPayloadComposer;

import com.agnosticrouter.contracts.StandardTicketEvent;
import com.agnosticrouter.crm.domain.model.CrmIntegration;
import com.agnosticrouter.crm.domain.model.CrmOperation;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class CrmHttpExecutor implements CrmExecutionPort {
    private final HttpClient httpClient;
    private final TemplateRenderer templates;
    private final CrmPayloadComposer composer;
    private final ResponseValueExtractor responseExtractor;
    private final CrmAuthenticationConfigurer auth;

    public CrmHttpExecutor(
            TemplateRenderer templates,
            CrmPayloadComposer composer,
            ResponseValueExtractor responseExtractor,
            CrmAuthenticationConfigurer auth
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.templates = templates;
        this.composer = composer;
        this.responseExtractor = responseExtractor;
        this.auth = auth;
    }

    public CrmExecutionResult execute(CrmExecutionPlan plan, StandardTicketEvent ticket) {
        CrmIntegration integration = plan.integration();
        CrmOperation operation = plan.operation();
        String path = templates.renderText(operation.getPathTemplate(), ticket);
        String url = joinUrl(integration.getBaseUrl(), path);
        String body = renderBody(operation, ticket);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.putAll(templates.renderHeaders(integration.getDefaultHeadersJson(), ticket));
        headers.putAll(templates.renderHeaders(operation.getHeadersTemplateJson(), ticket));
        auth.applyAuth(headers, integration);
        if (operation.getContentType() != null && !operation.getContentType().isBlank()) {
            headers.putIfAbsent(HttpHeaders.CONTENT_TYPE, operation.getContentType());
        }

        Set<Integer> successStatuses = parseStatuses(operation.getSuccessStatusCodes());
        int maxAttempts = Math.max(1, operation.getMaxAttempts());
        String lastError = null;
        int lastStatus = 0;
        String lastBody = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest request = buildRequest(url, operation, headers, body);
                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );
                lastStatus = response.statusCode();
                lastBody = response.body();

                if (successStatuses.contains(response.statusCode())) {
                    String externalId = responseExtractor.extract(response.body(), operation.getExternalIdPath());
                    String externalNumber = responseExtractor.extract(response.body(), operation.getExternalNumberPath());
                    if (externalNumber == null) {
                        externalNumber = readCreatedNumber(operation, path, url, headers, externalId);
                    }
                    return new CrmExecutionResult(
                            true,
                            response.statusCode(),
                            externalId,
                            response.body(),
                            attempt,
                            null,
                            externalNumber
                    );
                }

                lastError = "CRM respondió HTTP " + response.statusCode();
                if (response.statusCode() < 500) {
                    break;
                }
            } catch (Exception ex) {
                lastError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }

            if (attempt < maxAttempts) {
                sleep(operation.getRetryDelayMs());
            }
        }

        return new CrmExecutionResult(
                false,
                lastStatus,
                null,
                lastBody,
                maxAttempts,
                lastError == null ? "No fue posible completar la operación CRM" : lastError
        );
    }

    /** Some CRMs assign their visible case number only after the create response is written. */
    private String readCreatedNumber(CrmOperation operation, String path, String url,
                                     Map<String, String> headers, String externalId) {
        if (operation.getExternalNumberPath() == null || operation.getExternalNumberPath().isBlank()
                || !"POST".equalsIgnoreCase(operation.getHttpMethod())
                || path == null || !path.matches("/api/v1/[A-Za-z][A-Za-z0-9_]*")
                || externalId == null || !externalId.matches("[A-Za-z0-9_-]{1,150}")) return null;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url + "/" + externalId))
                    .timeout(Duration.ofMillis(Math.max(1000, operation.getTimeoutMs()))).GET();
            headers.forEach(builder::header);
            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
            return responseExtractor.extract(response.body(), operation.getExternalNumberPath());
        } catch (Exception ignored) {
            if (Thread.currentThread().isInterrupted()) Thread.currentThread().interrupt();
            return null;
        }
    }

    private HttpRequest buildRequest(
            String url,
            CrmOperation operation,
            Map<String, String> headers,
            String body
    ) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(Math.max(1000, operation.getTimeoutMs())));

        headers.forEach(builder::header);

        String method = operation.getHttpMethod().toUpperCase(Locale.ROOT);
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);

        return builder.method(method, publisher).build();
    }

    private String renderBody(CrmOperation operation, StandardTicketEvent ticket) {
        if (operation.getBodyTemplate() == null || operation.getBodyTemplate().isBlank()) {
            return null;
        }
        if (operation.getContentType() != null
                && operation.getContentType().toLowerCase(Locale.ROOT).contains("json")) {
            return composer.compose(operation, ticket).json();
        }
        return templates.renderText(operation.getBodyTemplate(), ticket);
    }

    private Set<Integer> parseStatuses(String configured) {
        Set<Integer> result = new LinkedHashSet<>();
        for (String value : configured.split(",")) {
            result.add(Integer.parseInt(value.trim()));
        }
        return result;
    }

    private String joinUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(Math.max(0, milliseconds));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
