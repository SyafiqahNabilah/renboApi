package com.rbms.renbo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";
        final String apiTitle = "Renbo API";

        return new OpenAPI()
                .info(new Info()
                        .title(apiTitle)
                        .description("Rental Management System API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Renbo Team")
                        )
                )
                .servers(Arrays.asList(
                        new Server().url("http://localhost:8080").description("Local development server"),
                        new Server().url("https://api.renbo.com").description("Production server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token")
                        )
                );
    }
}

