package com.devgomes.glpi_integration.web;

import com.devgomes.glpi_integration.config.GlpiApiUrlResolver;
import com.devgomes.glpi_integration.config.GlpiProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/glpi")
public class GlpiConfigController {

    private final GlpiProperties properties;

    public GlpiConfigController(GlpiProperties properties) {
        this.properties = properties;
    }

    /** Mostra URL efetiva da API (sem tokens) para diagnosticar localhost vs servidor real. */
    @GetMapping("/connection-info")
    public ResponseEntity<Map<String, Object>> connectionInfo() {
        String resolved = GlpiApiUrlResolver.resolveBaseUrl(properties.getBaseUrl(), properties.getStyle());
        boolean localhost = resolved.contains("://localhost") || resolved.contains("://127.0.0.1");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("apiStyle", properties.getStyle().name());
        body.put("baseUrlConfigured", properties.getBaseUrl());
        body.put("baseUrlResolved", resolved);
        body.put("credentialsPresent", properties.hasCredentials());
        body.put("localhostWarning", localhost);
        body.put("localPropertiesHint",
                "application-local.properties deve conter glpi.api.base-url E os tokens (não só tokens).");
        if (localhost) {
            body.put("hint",
                    "Crie application-local.properties com glpi.api.base-url=http://SEU_SERVIDOR/apirest.php "
                            + "(ex.: http://192.168.1.98/apirest.php) e reinicie a aplicação.");
        }
        return ResponseEntity.ok(body);
    }
}
