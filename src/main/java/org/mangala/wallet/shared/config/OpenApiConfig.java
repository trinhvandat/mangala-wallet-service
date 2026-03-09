package org.mangala.wallet.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:wallet-service}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Mangala Wallet Service API")
                        .description("""
                                API for managing cryptocurrency wallets across multiple blockchain networks.

                                ## Features
                                - Multi-chain wallet management (Ethereum, BSC, Polygon, Arbitrum, Solana)
                                - Address validation per chain type
                                - Wallet balance tracking and synchronization

                                ## Authentication
                                All endpoints require a valid JWT Bearer token in the Authorization header.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Mangala Team")
                                .email("support@mangala.io")
                                .url("https://mangala.io"))
                        .license(new License()
                                .name("Private")
                                .url("https://mangala.io/terms")))
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("Current Server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token from authentication service")));
    }
}
