package com.tunisia.financial.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI Configuration
 * Provides interactive API documentation
 * Access at: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {
    
    @Value("${spring.application.name}")
    private String applicationName;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Financial Fraud Detection API")
                        .version("1.0.0")
                        .description("Secure AI-Powered Financial Fraud Detection and Risk Management System for Tunisian SMEs with Intelligent Health Monitoring")
                        .contact(new Contact()
                                .name("Financial Security Team")
                                .email("support@financial.tn")
                                .url("https://financial.tn"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.financial.tn")
                                .description("Production Server")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT token")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
