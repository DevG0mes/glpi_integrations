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
        bean.addUrlPatterns("/api/*", "/v3/api-docs", "/v3/api-docs/*", "/swagger-ui", "/swagger-ui/*");
        bean.setOrder(2);
        return bean;
    }
}
