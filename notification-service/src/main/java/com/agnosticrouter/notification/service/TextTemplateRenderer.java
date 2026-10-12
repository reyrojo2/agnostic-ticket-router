package com.agnosticrouter.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TextTemplateRenderer {
    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");
    private final ObjectMapper objectMapper;

    public TextTemplateRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String render(String template, Object context) {
        if (template == null) {
            return null;
        }
        JsonNode root = objectMapper.valueToTree(context);
        Matcher matcher = TOKEN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            JsonNode node = resolve(root, matcher.group(1));
            String value = node == null || node.isNull() ? "" : node.asText();
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private JsonNode resolve(JsonNode root, String path) {
        JsonNode current = root;
        for (String segment : path.split("\\.")) {
            current = current == null ? null : current.get(segment);
        }
        return current;
    }
}
