package com.devgomes.glpi_integration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI glpiIntegrationOpenApi(
            @Value("${server.port:8081}") int serverPort) {
        return new OpenAPI()
                .info(new Info()
                        .title("GLPI Integration API")
                        .version("0.0.1-SNAPSHOT")
                        .description("""
                                Middleware para integração com GLPI (API legada apirest.php).
                                
                                - **Sincronização:** upload CSV/XLSX (`multipart`, campo `file`)
                                - **Ativos customizados:** Starlink, Chip, Celular
                                - Documentação estática: docs/API_REFERENCE.md
                                """)
                        .contact(new Contact().name("PSI Energy / DevGomes")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Ambiente local")));
    }
}
