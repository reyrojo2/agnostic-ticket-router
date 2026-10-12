package com.agnosticrouter.crm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateRenderer {
    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}", Pattern.UNICODE_CASE);
    private static final Pattern WHOLE_TOKEN = Pattern.compile("^\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}$");

    private final ObjectMapper objectMapper;

    public TemplateRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String renderText(String template, Object context) {
        if (template == null) {
            return null;
        }
        JsonNode root = objectMapper.valueToTree(context);
        Matcher matcher = TOKEN.matcher(template);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            JsonNode value = resolve(root, matcher.group(1));
            String replacement = value == null || value.isNull() ? "" : value.asText();
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    public String renderJson(String template, Object context) {
        if (template == null || template.isBlank()) {
            return null;
        }
        try {
            JsonNode templateNode = objectMapper.readTree(template);
            JsonNode contextNode = objectMapper.valueToTree(context);
            JsonNode rendered = renderNode(templateNode, contextNode);
            return objectMapper.writeValueAsString(rendered);
        } catch (Exception ex) {
            throw new IllegalArgumentException("La plantilla JSON no es válida: " + ex.getMessage(), ex);
        }
    }

    public Map<String, String> renderHeaders(String headersJson, Object context) {
        if (headersJson == null || headersJson.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode node = objectMapper.readTree(headersJson);
            if (!node.isObject()) {
                throw new IllegalArgumentException("Los headers deben ser un objeto JSON");
            }
            java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                result.put(field.getKey(), renderText(field.getValue().asText(), context));
            }
            return result;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Los headers configurados no son JSON válido: " + ex.getMessage(), ex);
        }
    }

    private JsonNode renderNode(JsonNode node, JsonNode context) {
        if (node.isObject()) {
            ObjectNode result = objectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> result.set(entry.getKey(), renderNode(entry.getValue(), context)));
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            node.forEach(item -> result.add(renderNode(item, context)));
            return result;
        }
        if (node.isTextual()) {
            String value = node.asText();
            Matcher whole = WHOLE_TOKEN.matcher(value);
            if (whole.matches()) {
                JsonNode resolved = resolve(context, whole.group(1));
                return resolved == null ? NullNode.getInstance() : resolved.deepCopy();
            }
            return TextNode.valueOf(renderText(value, context));
        }
        return node.deepCopy();
    }

    private JsonNode resolve(JsonNode root, String path) {
        JsonNode current = root;
        for (String segment : path.split("\\.")) {
            if (current == null || current.isNull()) {
                return NullNode.getInstance();
            }
            current = current.get(segment);
        }
        return current == null ? NullNode.getInstance() : current;
    }
}
