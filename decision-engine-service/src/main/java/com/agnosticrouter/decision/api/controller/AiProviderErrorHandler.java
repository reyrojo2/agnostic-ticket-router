package com.agnosticrouter.decision.api.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice(assignableTypes = AiProviderController.class)
public class AiProviderErrorHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException ex) { return ResponseEntity.badRequest().body(Map.of("detail", ex.getMessage() == null ? "Configuración no válida." : ex.getMessage())); }
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> missing(NoSuchElementException ex) { return ResponseEntity.status(404).body(Map.of("detail", "El proveedor ya no existe. Actualiza la lista.")); }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> unavailable(IllegalStateException ex) { return ResponseEntity.status(422).body(Map.of("detail", ex.getMessage() == null ? "La configuración no está disponible." : ex.getMessage())); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflict(DataIntegrityViolationException ex) { return ResponseEntity.status(409).body(Map.of("detail", "La configuración cambió durante la operación. Actualiza y vuelve a intentarlo.")); }
}
