package com.agnosticrouter.decision;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(DecisionProperties.class)
public class ClientConfig {
    @Bean
    @LoadBalanced
    RestClient.Builder knowledgeRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean("knowledgeRestClient")
    RestClient knowledgeRestClient(@LoadBalanced RestClient.Builder builder, DecisionProperties properties) {
        return builder.baseUrl(properties.knowledge().baseUrl()).build();
    }

    @Bean("decisionAiClient")
    RestClient decisionAiClient(DecisionProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.ai().baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        if (properties.ai().apiKey() != null && !properties.ai().apiKey().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.ai().apiKey());
        }
        return builder.build();
    }
}
