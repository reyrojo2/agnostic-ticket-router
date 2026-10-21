package com.agnosticrouter.decision;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record DecisionProperties(Knowledge knowledge, Ai ai) {
    public record Knowledge(String baseUrl) {}
    public record Ai(String provider, String baseUrl, String apiKey, String model, String lang, boolean enabled, int maxCallsPerMessage, int maxCallsPerDay) {}
}
