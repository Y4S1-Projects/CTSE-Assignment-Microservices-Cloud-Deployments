package com.example.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        Contact contact = new Contact()
                .name("CTSE Team")
                .email("ctse@sliit.lk");

        Info info = new Info()
                .title("Auth Service API")
                .version("1.0.0")
                .description("Authentication and Authorization Service for Food Ordering System")
                .contact(contact)
                .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT"));

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // Don't add global security requirement - let individual endpoints decide
        // SecurityRequirement securityRequirement = new SecurityRequirement()
        //         .addList("bearerAuth");

        return new OpenAPI()
                .info(info)
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme));
                // .addSecurityItem(securityRequirement); // Removed global security
    }
}
