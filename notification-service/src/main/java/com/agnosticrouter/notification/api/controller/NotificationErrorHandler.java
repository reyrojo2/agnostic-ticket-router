package com.agnosticrouter.notification.api.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestControllerAdvice(basePackageClasses=NotificationAdminController.class)
public class NotificationErrorHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String,String>> missing(NoSuchElementException e){return ResponseEntity.status(404).body(Map.of("message","No se encontró el canal o mensaje seleccionado."));}
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String,String>> state(IllegalStateException e){return ResponseEntity.status(422).body(Map.of("message",e.getMessage()));}
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,String>> conflict(){return ResponseEntity.status(409).body(Map.of("message","Ya existe un canal con ese identificador o un mensaje para ese perfil y momento."));}
}
