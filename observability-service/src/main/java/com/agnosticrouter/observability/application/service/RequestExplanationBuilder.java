package com.agnosticrouter.observability.application.service;

import java.util.*;

/** Explains the event read model without consulting another service's tables or current configuration. */
public final class RequestExplanationBuilder {
    private RequestExplanationBuilder() {}

    public static Map<?, ?> decisions(Map<String, Object> row) {
        return row.get("decisions") instanceof Map<?, ?> map ? map : Map.of();
    }

    public static boolean unavailable(Map<?, ?> decision) {
        if (!"UNAVAILABLE".equals(decision.get("source"))) return false;
        var diagnostic = diagnostic(decision);
        return diagnostic.isEmpty() || String.valueOf(diagnostic.get("code")).startsWith("AI_");
    }

    public static boolean lowConfidence(Map<?, ?> decision) {
        if (!"AI".equals(decision.get("source")) || !Boolean.FALSE.equals(decision.get("accepted"))) return false;
        var diagnostic = diagnostic(decision);
        return diagnostic.isEmpty() || Set.of("AI_LOW_CONFIDENCE", "AI_LOW_MARGIN", "AI_ABSTAINED").contains(diagnostic.get("code"));
    }

    public static Map<String, Object> explain(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>(row);
        List<Map<String, Object>> issues = new ArrayList<>();
        for (var entry : decisions(row).entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> decision) || !Boolean.FALSE.equals(decision.get("accepted"))) continue;
            var diagnostic = diagnostic(decision);
            String code = text(diagnostic, "code", unavailable(decision) ? "AI_UNAVAILABLE_LEGACY" : "DECISION_NOT_ACCEPTED");
            String message = text(diagnostic, "message", unavailable(decision)
                    ? "La IA no estuvo disponible. Esta solicitud histórica no conserva la causa HTTP; prueba la conexión actual del proveedor."
                    : "La decisión no fue aceptada. Esta solicitud histórica no conserva los umbrales ni el motivo detallado.");
            Map<String, Object> issue = new LinkedHashMap<>();
            issue.put("dimensionCode", entry.getKey());
            issue.put("dimensionName", text(diagnostic, "dimensionName", entry.getKey().toString()));
            issue.put("code", code); issue.put("message", message);
            issue.put("required", diagnostic.get("required"));
            issue.put("providerStatus", diagnostic.get("providerStatus"));
            issue.put("action", code.startsWith("AI_") && !lowConfidence(decision) && !Set.of("AI_TRUNCATED", "AI_INVALID_OPTION").contains(code) ? "AI" : "KNOWLEDGE");
            issues.add(issue);
        }
        String state = String.valueOf(row.get("state"));
        String summary;
        if ("CRM_CREATED".equals(state)) summary = "Caso creado y confirmado por el CRM.";
        else if ("CRM_ERROR".equals(state)) summary = "Falló el envío al CRM. Revisa el historial de la integración.";
        else if (!issues.isEmpty()) {
            Map<String, Object> first = issues.stream().filter(issue -> !"AI_SKIPPED_AFTER_FAILURE".equals(issue.get("code"))).findFirst().orElse(issues.getFirst());
            summary = first.get("dimensionName") + ": " + first.get("message");
        } else if ("REVIEW_REQUIRED".equals(state)) summary = "Se requiere revisión antes del envío al CRM. No se registró un motivo detallado.";
        else summary = "Esperando la siguiente etapa del procesamiento.";
        result.put("summary", summary); result.put("issues", issues);
        return result;
    }

    private static Map<?, ?> diagnostic(Map<?, ?> decision) {
        return decision.get("diagnostic") instanceof Map<?, ?> map ? map : Map.of();
    }

    private static String text(Map<?, ?> map, String key, String fallback) {
        return map.get(key) instanceof String value && !value.isBlank() ? value : fallback;
    }
}
