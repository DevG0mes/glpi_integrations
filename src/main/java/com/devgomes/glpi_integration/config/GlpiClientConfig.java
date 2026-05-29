package com.devgomes.glpi_integration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({GlpiProperties.class, GlpiSyncProperties.class})
public class GlpiClientConfig {

    @Bean
    RestClient glpiRestClient(GlpiProperties properties) {
        String baseUrl = GlpiApiUrlResolver.resolveBaseUrl(properties.getBaseUrl(), properties.getStyle());
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
