package com.devgomes.glpi_integration.config;

import com.devgomes.glpi_integration.security.ApiKeyAuthenticationFilter;
import com.devgomes.glpi_integration.security.SecurityHeadersFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityWebConfig {

    @Bean
    FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new SecurityHeadersFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(1);
        return bean;
    }

    @Bean
    FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyAuthenticationFilter(
            GlpiSecurityProperties securityProperties) {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ApiKeyAuthenticationFilter(securityProperties));
        // Apenas /api/** — Swagger UI e OpenAPI JSON ficam públicos quando habilitados;
        // o "Try it out" do Swagger envia X-API-Key nas rotas /api (configurado em OpenApiConfig).
        bean.addUrlPatterns("/api/*");
        bean.setOrder(2);
        return bean;
    }
}
