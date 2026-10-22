package com.agnosticrouter.decision.infrastructure.client;

import com.agnosticrouter.decision.application.model.AiConnection;
import com.agnosticrouter.decision.application.port.AiEnvironmentSettings;
import com.agnosticrouter.decision.config.DecisionProperties;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentAiSettings implements AiEnvironmentSettings {
    private final DecisionProperties properties;
    public EnvironmentAiSettings(DecisionProperties properties) { this.properties = properties; }
    public AiConnection connection() {
        var ai = properties.ai();
        return new AiConnection(ai.baseUrl(), "/v1/systemone", ai.model(), ai.lang(),
                ai.apiKey() == null || ai.apiKey().isBlank() ? "NONE" : "BEARER", "X-Api-Key", ai.apiKey());
    }
}
