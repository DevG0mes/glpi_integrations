package com.devgomes.glpi_integration.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class GlpiStartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GlpiStartupLogger.class);

    private final GlpiProperties properties;
    private final GlpiSecurityProperties securityProperties;

    public GlpiStartupLogger(GlpiProperties properties, GlpiSecurityProperties securityProperties) {
        this.properties = properties;
        this.securityProperties = securityProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String resolved = GlpiApiUrlResolver.resolveBaseUrl(properties.getBaseUrl(), properties.getStyle());
        log.info("GLPI API configurada: style={}, baseUrl={}, credenciais={}, apiKeyMiddleware={}",
                properties.getStyle(),
                resolved,
                properties.hasCredentials() ? "presentes" : "ausentes",
                securityProperties.isApiKeyRequired() ? "obrigatória" : "desligada");
        if (!securityProperties.isApiKeyRequired()) {
            log.warn("GLPI_API_KEY não definida — API /api/** aberta. Defina chave em produção.");
        }
        if (resolved.contains("://localhost") || resolved.contains("://127.0.0.1")) {
            log.warn("GLPI apontando para localhost — se o servidor for remoto, crie application-local.properties "
                    + "com glpi.api.base-url=http://SEU_IP/apirest.php");
        }
    }
}
