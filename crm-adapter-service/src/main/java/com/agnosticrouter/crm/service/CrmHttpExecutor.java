package com.agnosticrouter.crm.service;

import com.agnosticrouter.contracts.StandardTicketEvent;
import com.agnosticrouter.crm.domain.CrmIntegration;
import com.agnosticrouter.crm.domain.CrmOperation;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class CrmHttpExecutor {
    private final HttpClient httpClient;
    private final TemplateRenderer templates;
    private final SecretResolver secrets;
    private final ResponseValueExtractor responseExtractor;

    public CrmHttpExecutor(
            TemplateRenderer templates,
            SecretResolver secrets,
            ResponseValueExtractor responseExtractor
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.templates = templates;
        this.secrets = secrets;
        this.responseExtractor = responseExtractor;
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
        applyAuth(headers, integration);
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
                    return new CrmExecutionResult(
                            true,
                            response.statusCode(),
                            externalId,
                            response.body(),
                            attempt,
                            null
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
            return templates.renderJson(operation.getBodyTemplate(), ticket);
        }
        return templates.renderText(operation.getBodyTemplate(), ticket);
    }

    private void applyAuth(Map<String, String> headers, CrmIntegration integration) {
        String type = Optional.ofNullable(integration.getAuthType()).orElse("NONE").toUpperCase(Locale.ROOT);
        switch (type) {
            case "NONE" -> { }
            case "BEARER" -> headers.put(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + requireSecret(integration.getSecretRef())
            );
            case "BASIC" -> {
                String username = requireSecret(integration.getUsernameRef());
                String password = requireSecret(integration.getSecretRef());
                String token = Base64.getEncoder().encodeToString(
                        (username + ":" + password).getBytes(StandardCharsets.UTF_8)
                );
                headers.put(HttpHeaders.AUTHORIZATION, "Basic " + token);
            }
            case "API_KEY_HEADER" -> {
                if (integration.getApiKeyName() == null || integration.getApiKeyName().isBlank()) {
                    throw new IllegalStateException("apiKeyName es obligatorio para API_KEY_HEADER");
                }
                headers.put(integration.getApiKeyName(), requireSecret(integration.getSecretRef()));
            }
            case "OAUTH2_CLIENT_CREDENTIALS" -> headers.put(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + requestClientCredentialsToken(integration)
            );
            default -> throw new IllegalArgumentException("authType no soportado: " + type);
        }
    }

    private String requestClientCredentialsToken(CrmIntegration integration) {
        if (integration.getTokenUrl() == null || integration.getTokenUrl().isBlank()) {
            throw new IllegalStateException("tokenUrl es obligatorio para OAUTH2_CLIENT_CREDENTIALS");
        }
        String clientId = requireSecret(integration.getUsernameRef());
        String clientSecret = requireSecret(integration.getSecretRef());
        StringBuilder form = new StringBuilder();
        form.append("grant_type=client_credentials");
        if (integration.getScope() != null && !integration.getScope().isBlank()) {
            form.append("&scope=").append(java.net.URLEncoder.encode(integration.getScope(), StandardCharsets.UTF_8));
        }
        String basic = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)
        );
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(integration.getTokenUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OAuth token endpoint respondió HTTP " + response.statusCode());
            }
            String token = responseExtractor.extract(response.body(), "access_token");
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("El token endpoint no devolvió access_token");
            }
            return token;
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) {
                throw state;
            }
            throw new IllegalStateException("No fue posible obtener token OAuth2: " + ex.getMessage(), ex);
        }
    }

    private String requireSecret(String reference) {
        String value = secrets.resolve(reference);
        if (value == null) {
            throw new IllegalStateException("Falta configurar una credencial para la integración");
        }
        return value;
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
