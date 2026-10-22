package com.agnosticrouter.decision.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

}
