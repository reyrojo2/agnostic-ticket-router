package com.agnosticrouter.decision.infrastructure.client;

import com.agnosticrouter.decision.application.port.SystemOneTransport;
import com.agnosticrouter.decision.application.model.AiConnection;
import com.agnosticrouter.decision.application.model.AiProviderFailure;
import com.agnosticrouter.decision.application.model.SystemOneModels;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SystemOneDecisionClient implements SystemOneTransport {

    @Override
    public SystemOneModels.Response decide(
            AiConnection connection,
            Object state,
            String questionId,
            SystemOneModels.Question question,
            double minConfidence
    ) {
        SystemOneModels.Request request = new SystemOneModels.Request(
                state,
                Map.of(questionId, question),
                blankToNull(connection.model()),
                blankToNull(connection.language()),
                minConfidence
        );
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); factory.setReadTimeout(10000);
        RestClient.Builder builder = RestClient.builder().requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        if (!"NONE".equals(connection.authType()) && connection.apiKey() != null && !connection.apiKey().isBlank()) {
            if ("API_KEY_HEADER".equals(connection.authType())) builder.defaultHeader(connection.apiKeyHeader(), connection.apiKey());
            else builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + connection.apiKey());
        }
        try {
            return builder.build().post()
                .uri(connection.baseUrl().replaceAll("/+$", "") + connection.endpointPath())
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (requestHttp, response) -> {
                    int status = response.getStatusCode().value();
                    boolean modelRequired = false;
                    if (status == 400 || status == 422) {
                        // Inspect only known validation metadata; discard echoed inputs and remote messages.
                        try {
                            var detail = new ObjectMapper().readTree(response.getBody().readNBytes(8192)).path("detail");
                            if (detail.isArray()) for (var item : detail) {
                                if ("missing".equals(item.path("type").asText())) {
                                    for (var location : item.path("loc")) if ("model".equals(location.asText())) modelRequired = true;
                                }
                            }
                        } catch (Exception ignored) { /* Generic status remains useful for other error formats. */ }
                    }
                    throw new AiProviderFailure(status, modelRequired);
                })
                .body(SystemOneModels.Response.class);
        } catch (AiProviderFailure ex) { throw ex; }
        catch (org.springframework.web.client.ResourceAccessException ex) { throw new AiProviderFailure(0); }
        catch (org.springframework.web.client.RestClientException ex) { throw new AiProviderFailure(502); }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
