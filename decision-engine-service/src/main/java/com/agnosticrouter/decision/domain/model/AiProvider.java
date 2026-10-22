package com.agnosticrouter.decision.domain.model;

/** Configuration owned by Decision Engine. Credentials are encrypted at rest. */
public record AiProvider(Long id, String name, String baseUrl, String endpointPath,
                         String model, String language, String authType, String apiKeyHeader,
                         String encryptedApiKey, boolean enabled, boolean defaultProvider) {
    @Override public String toString() { return "AiProvider[id=" + id + ", name=" + name + "]"; }
}
