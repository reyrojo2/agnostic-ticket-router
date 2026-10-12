package com.agnosticrouter.decision;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class SystemOneDecisionClient implements DecisionAiClient {
    private final RestClient client;
    private final DecisionProperties properties;

    public SystemOneDecisionClient(
            @Qualifier("decisionAiClient") RestClient client,
            DecisionProperties properties
    ) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public SystemOneModels.Response decide(
            Object state,
            String questionId,
            SystemOneModels.Question question,
            double minConfidence
    ) {
        SystemOneModels.Request request = new SystemOneModels.Request(
                state,
                Map.of(questionId, question),
                blankToNull(properties.ai().model()),
                blankToNull(properties.ai().lang()),
                minConfidence
        );
        return client.post()
                .uri("/v1/systemone")
                .body(request)
                .retrieve()
                .body(SystemOneModels.Response.class);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
