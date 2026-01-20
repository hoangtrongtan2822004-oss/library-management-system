package com.ibizabroker.lms.configuration;

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

@Configuration
public class OpenApiConfig {

    @Value("${app.api.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${app.api.prod-url:https://api.lms.com}")
    private String prodUrl;

    @Bean
    public OpenAPI lmsOpenAPI() {
        SecurityScheme bearerAuth = new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT authentication token. Get token from /api/auth/login endpoint.");

        Server devServer = new Server()
                .url(devUrl)
                .description("Development Server (Local)");

        Server prodServer = new Server()
                .url(prodUrl)
                .description("Production Server");

        return new OpenAPI()
                .info(new Info()
                        .title("Library Management System API")
                        .description("JWT protected REST APIs for Library Management System with AI Chatbot support. " +
                                    "Includes book management, user management, loan tracking, fines, reviews, renewals, and RAG-based chatbot.")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("LMS Development Team")
                                .email("support@lms.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(devServer, prodServer))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth", bearerAuth));
    }
}
