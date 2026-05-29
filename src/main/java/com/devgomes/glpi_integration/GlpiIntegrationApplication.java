package com.devgomes.glpi_integration;

import com.devgomes.glpi_integration.config.GlpiProperties;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GlpiIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlpiIntegrationApplication.class, args);
    }

    @Bean
    @ConditionalOnProperty(prefix = "glpi", name = "test-on-startup", havingValue = "true")
    public CommandLineRunner testarConexao(GlpiIntegrationService glpiService, GlpiProperties properties) {
        return args -> {
            System.out.println("=========================================");
            System.out.println("Iniciando teste de conexão com o GLPI...");
            System.out.println("=========================================");

            try {
                String sessionToken = glpiService.initSession();
                System.out.println("SUCESSO! Session token obtido (parcial): "
                        + sessionToken.substring(0, Math.min(8, sessionToken.length())) + "...");
                System.out.println("=========================================");
            } catch (Exception e) {
                System.err.println("FALHA NA CONEXÃO: " + e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("localhost")) {
                    System.err.println();
                    System.err.println("O GLPI não está acessível em localhost.");
                    System.err.println("Ajuste glpi.api.base-url em application.properties com o host real,");
                    System.err.println("ex.: http://SEU_SERVIDOR/glpi/api.php/v1  ou  http://SEU_SERVIDOR/glpi/apirest.php");
                    System.err.println("URL atual: " + properties.getBaseUrl() + " (style=" + properties.getStyle() + ")");
                    System.err.println("Veja docs/API_VALIDATION.md para testar com curl antes de rodar o app.");
                }
            }
        };
    }
}
