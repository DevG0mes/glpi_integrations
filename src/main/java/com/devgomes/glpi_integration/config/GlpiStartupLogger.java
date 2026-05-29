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

    public GlpiStartupLogger(GlpiProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String resolved = GlpiApiUrlResolver.resolveBaseUrl(properties.getBaseUrl(), properties.getStyle());
        log.info("GLPI API configurada: style={}, baseUrl={}, credenciais={}",
                properties.getStyle(),
                resolved,
                properties.hasCredentials() ? "presentes" : "ausentes");
        if (resolved.contains("://localhost") || resolved.contains("://127.0.0.1")) {
            log.warn("GLPI apontando para localhost — se o servidor for remoto, crie application-local.properties "
                    + "com glpi.api.base-url=http://SEU_IP/apirest.php");
        }
    }
}
