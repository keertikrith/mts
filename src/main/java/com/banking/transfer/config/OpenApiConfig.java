package com.banking.transfer.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Money Transfer System API")
                        .version("1.0.0")
                        .description("""
                                REST API for the **TransferX** banking money transfer system.
                                
                                ## Authentication
                                Most endpoints are protected via HTTP Basic Auth (username + password).
                                Public endpoints (account creation and login) are open.
                                
                                ## Error Codes
                                | Code | HTTP | Description |
                                |---|---|---|
                                | AUTH-401 | 401 | Invalid credentials |
                                | ACC-403 | 403 | Account not active |
                                | ACC-404 | 404 | Account not found |
                                | ACC-409 | 409 | Duplicate username |
                                | TRX-400 | 400 | Insufficient balance |
                                | TRX-409 | 409 | Duplicate transfer (idempotency) |
                                | VAL-400 | 400 | Malformed JSON / invalid format |
                                | VAL-422 | 422 | Validation / business rule failure |
                                | SYS-500 | 500 | Unexpected internal error |
                                """)
                        .contact(new Contact()
                                .name("TransferX Team")
                                .email("support@transferx.io"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://api.transferx.io").description("Production")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .components(new Components()
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("HTTP Basic authentication using username and password")));
    }
}
