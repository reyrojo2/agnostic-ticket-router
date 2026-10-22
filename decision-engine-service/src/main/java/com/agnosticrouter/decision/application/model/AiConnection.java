package com.agnosticrouter.decision.application.model;

/** Transient connection passed only to the transport. Never returned by the admin API. */
public record AiConnection(String baseUrl, String endpointPath, String model, String language,
                           String authType, String apiKeyHeader, String apiKey) {
    @Override public String toString() { return "AiConnection[credentials=REDACTED]"; }
}
