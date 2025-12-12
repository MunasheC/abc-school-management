package com.bank.schoolmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration
 * 
 * This configuration creates interactive API documentation accessible at:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 * 
 * The documentation is auto-generated from your controllers and includes:
 * - All REST endpoints with HTTP methods
 * - Request/response examples
 * - Parameter descriptions
 * - Try-it-out functionality
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI schoolManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("School Management System API")
                        .description("REST API for School Management System with Bank Integration\n\n" +
                                "**Features:**\n" +
                                "- Student enrollment (single and bulk via Excel)\n" +
                                "- Fee management and payment tracking\n" +
                                "- Bank payment processing\n" +
                                "- Multi-tenant architecture (school isolation)\n" +
                                "- Guardian management with sibling detection\n\n" +
                                "**Multi-tenant Note:**\n" +
                                "School-specific endpoints require the `X-School-ID` header. " +
                                "Bank endpoints do NOT require this header (cross-school access).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("School Management Team")
                                .email("support@schoolmanagement.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.schoolmanagement.com")
                                .description("Production Server (if deployed)")
                ))
                .components(new Components()
                        .addSecuritySchemes("X-School-ID", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-School-ID")
                                .description("School ID for multi-tenant isolation. " +
                                        "Required for school-specific endpoints. " +
                                        "Example: 1, 2, 3")));
    }
}
