package com.agnosticrouter.knowledge.api.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice(assignableTypes = AdminKnowledgeController.class)
public class KnowledgeErrorHandler {
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> missing(NoSuchElementException ex) { return ResponseEntity.status(404).body(Map.of("detail", "El elemento ya no existe. Actualiza el perfil.")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflict(DataIntegrityViolationException ex) { return ResponseEntity.status(409).body(Map.of("detail", "No se pudo guardar o eliminar: el identificador ya existe o hay datos relacionados. Revisa las decisiones, respuestas y reglas; también puedes deshabilitar el elemento.")); }
}
