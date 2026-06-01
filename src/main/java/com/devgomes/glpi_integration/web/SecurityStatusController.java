package com.devgomes.glpi_integration.web;

import com.devgomes.glpi_integration.config.GlpiSecurityProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/security")
public class SecurityStatusController {

    private final GlpiSecurityProperties securityProperties;

    public SecurityStatusController(GlpiSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /** Público: informa se a UI deve enviar X-API-Key (sem expor a chave). */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "apiKeyRequired", securityProperties.isApiKeyRequired(),
                "hint", securityProperties.isApiKeyRequired()
                        ? "Envie header X-API-Key nas requisições /api/**"
                        : "API aberta (defina GLPI_API_KEY em produção)"
        ));
    }
}
