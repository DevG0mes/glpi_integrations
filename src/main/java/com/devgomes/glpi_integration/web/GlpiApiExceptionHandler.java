package com.devgomes.glpi_integration.web;

import com.devgomes.glpi_integration.client.GlpiApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlpiApiExceptionHandler {

    @ExceptionHandler(GlpiApiException.class)
    public ResponseEntity<Map<String, Object>> handleGlpiError(GlpiApiException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "error", "GLPI_API_ERROR",
                "status", ex.getStatusCode().value(),
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "BAD_REQUEST",
                "message", ex.getMessage()
        ));
    }
}
