package com.agnosticrouter.decision.application.model;

/** Maps internal failures to a fixed, safe vocabulary shared by probes and classification. */
public record AiFailureExplanation(String code, String message, Integer status) {
    public static AiFailureExplanation from(RuntimeException failure) {
        if (failure instanceof AiProviderFailure provider) {
            int status = provider.status();
            if (provider.modelRequired()) return new AiFailureExplanation("AI_MODEL_REQUIRED",
                    "El proveedor exige un modelo. Completa Modelo en Proveedores IA y prueba la conexión.", status);
            return switch (status) {
                case 401, 403 -> new AiFailureExplanation("AI_AUTHENTICATION", "El proveedor rechazó la credencial o sus permisos. Revisa la API key y la autenticación.", status);
                case 404 -> new AiFailureExplanation("AI_ENDPOINT", "No se encontró la ruta SystemOne. Revisa la dirección y la ruta del proveedor.", status);
                case 400, 422 -> new AiFailureExplanation("AI_INVALID_REQUEST", "El proveedor rechazó la solicitud. Revisa el modelo y la compatibilidad con SystemOne.", status);
                case 402, 429 -> new AiFailureExplanation("AI_QUOTA", "El proveedor indica un problema de cuota, saldo o límite de solicitudes.", status);
                case 0 -> new AiFailureExplanation("AI_CONNECTION", "No se pudo conectar con el proveedor o se agotó el tiempo de espera.", null);
                default -> new AiFailureExplanation("AI_PROVIDER_ERROR", "El proveedor respondió con un error. Prueba la conexión antes de enviar otro correo.", status);
            };
        }
        if (failure instanceof IllegalStateException || failure instanceof IllegalArgumentException) {
            return new AiFailureExplanation("AI_CONFIGURATION", "No se pudo preparar la conexión IA. Revisa el proveedor habilitado y el cifrado de su credencial.", null);
        }
        return new AiFailureExplanation("AI_INVALID_RESPONSE", "La IA no devolvió una respuesta válida para esta decisión. Revisa la compatibilidad del proveedor.", null);
    }
}
