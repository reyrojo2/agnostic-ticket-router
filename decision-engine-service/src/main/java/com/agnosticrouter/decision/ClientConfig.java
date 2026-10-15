package com.agnosticrouter.decision;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(DecisionProperties.class)
public class ClientConfig {
    @Bean("knowledgeRestClient")
    RestClient knowledgeRestClient(DecisionProperties properties) {
        return RestClient.builder().baseUrl(properties.knowledge().baseUrl()).build();
    }

    @Bean("decisionAiClient")
    RestClient decisionAiClient(DecisionProperties properties) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.ai().baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        if (properties.ai().apiKey() != null && !properties.ai().apiKey().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.ai().apiKey());
        }
        return builder.build();
    }
}
