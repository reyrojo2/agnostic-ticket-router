package com.agnosticrouter.crm.gateway;

import com.agnosticrouter.crm.domain.CrmIntegration;
import com.agnosticrouter.crm.service.ResponseValueExtractor;
import com.agnosticrouter.crm.service.SecretResolver;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/** Authentication strategy for configured CRM endpoints, including OAuth2 tokens. */
@Component
public class CrmAuthenticationConfigurer {
    private final SecretResolver secrets;
    private final ResponseValueExtractor responseExtractor;
    private final HttpClient httpClient;

    public CrmAuthenticationConfigurer(SecretResolver secrets, ResponseValueExtractor responseExtractor) {
        this.secrets=secrets;
        this.responseExtractor=responseExtractor;
        this.httpClient=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public void applyAuth(Map<String, String> headers, CrmIntegration integration) {
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


}
