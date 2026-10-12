package com.agnosticrouter.notification.service;

import org.springframework.stereotype.Component;

@Component
public class SecretResolver {
    public String resolve(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        if (reference.startsWith("ENV:")) {
            String variable = reference.substring(4);
            String value = System.getenv(variable);
            if (value == null) {
                throw new IllegalStateException("No existe la variable de entorno " + variable);
            }
            return value;
        }
        throw new IllegalArgumentException("Referencia de secreto no soportada. Use ENV:VARIABLE");
    }
}
