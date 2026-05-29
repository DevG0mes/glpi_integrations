package com.devgomes.glpi_integration.web;

import com.devgomes.glpi_integration.client.GlpiApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.util.LinkedHashMap;
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

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleGlpiUnreachable(ResourceAccessException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "GLPI_UNREACHABLE");
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("message", "Não foi possível conectar ao GLPI. A URL configurada está incorreta ou o servidor está offline.");
        body.put("detail", ex.getMessage());
        body.put("hint", "Confira GET /api/glpi/connection-info — use application-local.properties com "
                + "glpi.api.base-url=http://192.168.1.98/apirest.php (não localhost, salvo se o GLPI rode na mesma máquina).");
        body.put("checklist", java.util.List.of(
                "Arquivo src/main/resources/application-local.properties existe?",
                "glpi.api.base-url aponta para o IP/host do GLPI?",
                "glpi.api.app-token e glpi.api.user-token preenchidos?",
                "GLPI acessível no navegador na mesma URL?"
        ));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
