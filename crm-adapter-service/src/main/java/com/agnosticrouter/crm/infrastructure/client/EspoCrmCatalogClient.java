package com.agnosticrouter.crm.infrastructure.client;

import com.agnosticrouter.crm.application.port.CrmCatalogPort;
import com.agnosticrouter.crm.domain.model.CrmIntegration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** EspoCRM-specific implementation. Generic CRM case flow does not depend on this adapter. */
@Component
public class EspoCrmCatalogClient implements CrmCatalogPort {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final CrmAuthenticationConfigurer authentication;
    private final ObjectMapper mapper;

    public EspoCrmCatalogClient(CrmAuthenticationConfigurer authentication, ObjectMapper mapper) {
        this.authentication = authentication;
        this.mapper = mapper;
    }

    public Map<String, Map<String, Object>> fields(CrmIntegration integration, String entityType) {
        requireEspo(integration);
        String entity = safeEntity(entityType);
        String key = "entityDefs." + entity + ".fields";
        JsonNode node = request(integration, "GET", "/api/v1/Metadata?key=" + encode(key), null);
        if (node.has("entityDefs")) node = node.path("entityDefs").path(entity).path("fields");
        else if (node.has("fields")) node = node.path("fields");
        if (!node.isObject()) {
            throw new IllegalStateException("EspoCRM no devolvió metadatos de campos. Revisa los permisos de lectura de Metadata.");
        }
        Map<String, Map<String, Object>> fields = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            if (entry.getValue().isObject()) {
                fields.put(entry.getKey(), mapper.convertValue(entry.getValue(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}));
            }
        });
        return fields;
    }

    public List<ExternalRecord> records(CrmIntegration integration, String entityType) {
        requireEspo(integration);
        String entity = safeReferenceEntity(entityType);
        List<ExternalRecord> result = new ArrayList<>();
        // Explicit pagination avoids silently creating duplicate teams when a page is full.
        for (int offset = 0; offset <= 1000; offset += 200) {
            JsonNode response = request(integration, "GET", "/api/v1/" + entity + "?maxSize=200&offset=" + offset, null);
            JsonNode list = response.path("list");
            if (!list.isArray()) throw new IllegalStateException("Respuesta inesperada de la lista EspoCRM " + entity);
            for (JsonNode item : list) {
                String id = item.path("id").asText("");
                if (!id.isBlank()) result.add(new ExternalRecord(id, item.path("name").asText(""), false));
            }
            if (list.size() < 200) return result;
        }
        throw new IllegalStateException("Demasiados registros EspoCRM; limita el catálogo para comprobar duplicados antes de crear equipos.");
    }

    /** Admin-initiated, not executed automatically when a ticket is processed. */
    public synchronized ExternalRecord ensureTeam(CrmIntegration integration, String name) {
        requireEspo(integration);
        String requested = name == null ? "" : name.trim();
        if (requested.isBlank() || requested.length() > 100 || requested.contains("\n") || requested.contains("\r")) {
            throw new IllegalArgumentException("Nombre de equipo inválido (1-100 caracteres)");
        }
        for (ExternalRecord team : records(integration, "Team")) {
            if (team.name().trim().equalsIgnoreCase(requested)) return team;
        }
        JsonNode result;
        try {
            result = request(integration, "POST", "/api/v1/Team", mapper.writeValueAsString(Map.of("name", requested)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo preparar el equipo", ex);
        }
        String id = result.path("id").asText("");
        if (id.isBlank()) throw new IllegalStateException("EspoCRM no devolvió ID del equipo creado");
        return new ExternalRecord(id, result.path("name").asText(requested), true);
    }

    public boolean recordExists(CrmIntegration integration, String entityType, String id) {
        requireEspo(integration);
        String entity = safeReferenceEntity(entityType);
        if (id == null || !id.matches("[A-Za-z0-9_-]{1,100}")) return false;
        Map<String, String> headers = headers(integration);
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri(integration, "/api/v1/" + entity + "/" + id))
                    .timeout(Duration.ofSeconds(12)).GET();
            headers.forEach(builder::header);
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) return false;
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("EspoCRM rechazó validación de " + entity + ": HTTP " + response.statusCode());
            }
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Validación EspoCRM interrumpida", ex);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("EspoCRM no está disponible para comprobar " + entity, ex);
        }
    }

    private JsonNode request(CrmIntegration integration, String method, String relativePath, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri(integration, relativePath))
                    .timeout(Duration.ofSeconds(15));
            headers(integration).forEach(builder::header);
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                // Never expose response bodies, API keys, tokens or HTTP request headers.
                throw new IllegalStateException("EspoCRM HTTP " + response.statusCode() + " en " + method + " " + relativePath.split("\\?")[0]);
            }
            return mapper.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación EspoCRM interrumpida", ex);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Error de conexión o respuesta EspoCRM", ex);
        }
    }
    private Map<String, String> headers(CrmIntegration integration) {
        Map<String, String> headers = new LinkedHashMap<>();
        authentication.applyAuth(headers, integration);
        return headers;
    }
    private URI uri(CrmIntegration integration, String relativePath) {
        String base = Objects.requireNonNull(integration.getBaseUrl(), "Base URL necesaria");
        return URI.create(base.replaceAll("/+$", "") + relativePath);
    }
    private void requireEspo(CrmIntegration integration) {
        if (integration == null || integration.getCode() == null || !integration.getCode().toUpperCase(java.util.Locale.ROOT).startsWith("ESPOCRM")) {
            throw new IllegalArgumentException("Catálogo y provisión de equipos disponibles únicamente para integraciones ESPOCRM");
        }
    }
    private String safeEntity(String code) {
        if (code == null || !code.matches("[A-Za-z][A-Za-z0-9_]{0,79}")) throw new IllegalArgumentException("Tipo de entidad inválido");
        return code;
    }
    private String safeReferenceEntity(String code) {
        if (!"Team".equals(code) && !"User".equals(code)) throw new IllegalArgumentException("Solo se permiten Team y User");
        return code;
    }
    private String encode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
