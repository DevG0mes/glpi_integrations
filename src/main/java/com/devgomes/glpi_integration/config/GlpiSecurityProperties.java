package com.devgomes.glpi_integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Proteção opcional da API com chave compartilhada ({@code GLPI_API_KEY}).
 * Se vazia, a API permanece aberta (apenas para desenvolvimento local).
 */
@ConfigurationProperties(prefix = "glpi.security")
public class GlpiSecurityProperties {

    /**
     * Chave exigida no header {@code X-API-Key} ou {@code Authorization: Bearer}.
     */
    private String apiKey = "";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey != null ? apiKey : "";
    }

    public boolean isApiKeyRequired() {
        return apiKey != null && !apiKey.isBlank();
    }
}
