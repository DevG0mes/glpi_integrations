package com.devgomes.glpi_integration.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "glpi.sync", name = "enabled", havingValue = "true")
public class SchedulingConfig {
}
