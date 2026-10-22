package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.decision.application.model.AiConnection;
import com.agnosticrouter.decision.application.port.*;
import com.agnosticrouter.decision.domain.model.AiProvider;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class AiProviderAdminService {
    private final AiProviderStore providers;
    private final AiSecretProtector secrets;
    private final AiEnvironmentSettings environment;
    public AiProviderAdminService(AiProviderStore providers, AiSecretProtector secrets, AiEnvironmentSettings environment) {
        this.providers = providers; this.secrets = secrets; this.environment = environment;
    }
    public List<ProviderView> list() { return providers.findAll().stream().map(ProviderView::from).toList(); }
    public SettingsView settings() {
        AiConnection env = environment.connection();
        return new SettingsView(secrets.available(), providers.findDefault().map(AiProvider::id).orElse(null),
                env.baseUrl(), env.model(), env.language(), env.apiKey() != null && !env.apiKey().isBlank());
    }
    public ProviderView save(Long id, SaveProvider request) {
        AiProvider existing = id == null ? null : require(id);
        String name = clean(request.name()), base = clean(request.baseUrl());
        if (name.isBlank() || name.length() > 160) throw new IllegalArgumentException("Indica un nombre de hasta 160 caracteres.");
        URI uri;
        try { uri = URI.create(base); } catch (RuntimeException ex) { throw new IllegalArgumentException("La dirección del proveedor no es válida."); }
        if ((!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null || base.length() > 1000) {
            throw new IllegalArgumentException("Usa una dirección HTTP o HTTPS sin credenciales ni parámetros.");
        }
        String endpoint = clean(request.endpointPath());
        if (endpoint.isBlank()) endpoint = "/v1/systemone";
        if (!endpoint.matches("/[A-Za-z0-9/_-]+") || endpoint.startsWith("//") || endpoint.length() > 200) throw new IllegalArgumentException("La ruta SystemOne no es válida.");
        String auth = clean(request.authType());
        if (!Set.of("NONE", "BEARER", "API_KEY_HEADER").contains(auth)) throw new IllegalArgumentException("Selecciona una forma de autenticación válida.");
        String header = clean(request.apiKeyHeader());
        if (header.isBlank()) header = "X-Api-Key";
        if (!header.matches("[A-Za-z][A-Za-z0-9-]{0,79}") || Set.of("host", "content-length", "content-type").contains(header.toLowerCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("El nombre de cabecera de la API key no es válido.");
        }
        String key = clean(request.apiKey());
        if (key.length() > 4096 || key.contains("\r") || key.contains("\n")) throw new IllegalArgumentException("La API key no tiene un formato válido.");
        String encrypted = existing == null ? null : existing.encryptedApiKey();
        if (request.removeApiKey()) encrypted = null;
        if (!key.isBlank()) encrypted = secrets.encrypt(key);
        if (!"NONE".equals(auth) && encrypted == null) throw new IllegalArgumentException("Introduce una API key o selecciona Sin autenticación.");
        if (existing != null && existing.defaultProvider() && !request.enabled()) throw new IllegalArgumentException("Selecciona otro proveedor antes de deshabilitar el que usa el motor.");
        String model = clean(request.model()), language = clean(request.language());
        if (model.length() > 160 || language.length() > 30) throw new IllegalArgumentException("El modelo o idioma supera la longitud permitida.");
        AiProvider saved = providers.save(new AiProvider(id, name, base.replaceAll("/+$", ""), endpoint,
                model, language, auth, header, encrypted, request.enabled(), existing != null && existing.defaultProvider()));
        return ProviderView.from(saved);
    }
    public void select(Long id) { if (!require(id).enabled()) throw new IllegalArgumentException("Habilita el proveedor antes de seleccionarlo."); providers.selectDefault(id); }
    public void useEnvironment() { providers.clearDefault(); }
    public void delete(Long id) { if (require(id).defaultProvider()) throw new IllegalArgumentException("Selecciona otro proveedor o usa la configuración del servidor antes de eliminarlo."); providers.delete(id); }
    private AiProvider require(Long id) { return providers.findById(id).orElseThrow(() -> new NoSuchElementException("Proveedor no encontrado.")); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    public record SaveProvider(String name, String baseUrl, String endpointPath, String model, String language,
                               String authType, String apiKeyHeader, String apiKey, boolean removeApiKey, boolean enabled) {
        @Override public String toString() { return "SaveProvider[credentials=REDACTED]"; }
    }
    public record ProviderView(Long id, String name, String baseUrl, String endpointPath, String model, String language,
                               String authType, String apiKeyHeader, boolean apiKeyConfigured, boolean enabled, boolean defaultProvider) {
        public static ProviderView from(AiProvider p) { return new ProviderView(p.id(), p.name(), p.baseUrl(), p.endpointPath(), p.model(), p.language(), p.authType(), p.apiKeyHeader(), p.encryptedApiKey() != null, p.enabled(), p.defaultProvider()); }
    }
    public record SettingsView(boolean keyStorageAvailable, Long defaultProviderId, String environmentBaseUrl,
                               String environmentModel, String environmentLanguage, boolean environmentKeyConfigured) {}
}
