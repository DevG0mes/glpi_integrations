package com.devgomes.glpi_integration.health;

import com.devgomes.glpi_integration.config.GlpiProperties;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "glpi.health", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GlpiHealthIndicator implements HealthIndicator {

    private final GlpiIntegrationService integrationService;
    private final GlpiProperties properties;

    public GlpiHealthIndicator(GlpiIntegrationService integrationService, GlpiProperties properties) {
        this.integrationService = integrationService;
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (!properties.hasCredentials()) {
            return Health.down()
                    .withDetail("reason", "Credenciais GLPI não configuradas")
                    .build();
        }
        try {
            integrationService.ensureSession();
            return Health.up().withDetail("api", properties.getBaseUrl()).build();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("api", properties.getBaseUrl())
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
