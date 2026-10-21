package com.agnosticrouter.crm.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ResponseValueExtractor {
    private final ObjectMapper objectMapper;

    public ResponseValueExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extract(String responseBody, String path) {
        if (responseBody == null || responseBody.isBlank() || path == null || path.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            JsonNode value;
            if (path.startsWith("/")) {
                value = node.at(path);
            } else {
                value = node;
                for (String segment : path.split("\\.")) {
                    value = value == null ? null : value.get(segment);
                }
            }
            return value == null || value.isMissingNode() || value.isNull() ? null : value.asText();
        } catch (Exception ex) {
            return null;
        }
    }
}
