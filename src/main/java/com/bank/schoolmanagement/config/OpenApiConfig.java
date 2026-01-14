package com.bank.schoolmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
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
 * 
 * FILTERED: Only shows endpoints currently in use (from Postman collection)
 * 
 * Server URL is configurable via application.properties:
 * - api.server.url
 * - api.server.description
 */
@Configuration
public class OpenApiConfig {

    @Value("${api.server.url:http://localhost:8080}")
    private String serverUrl;
    
    @Value("${api.server.description:Development Server}")
    private String serverDescription;

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
                                .url(serverUrl)
                                .description(serverDescription)
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

    /**
     * Filter endpoints to show only those currently in use
     * Based on Postman collection endpoints
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("active-endpoints")
                .pathsToMatch(
                        // Fee Records endpoints
                        "/api/school/fee-records",
                        "/api/school/fee-records/{id}",
                        "/api/school/fee-records/student-id/{studentId}",
                        "/api/school/fee-records/category/{category}",
                        "/api/school/fee-records/bulk/forms",
                        "/api/school/fee-records/bulk/grade/{grade}/class/{className}",
                        
                        // Student endpoints
                        "/api/school/students",
                        "/api/school/students/{id}",
                        "/api/school/students/upload-excel",
                        "/api/school/students/by-student-id/{studentId}",
                        "/api/school/students/by-studentID/{studentId}/promote",
                        "/api/school/students/year-end-promotion",
                        
                        // Bank Admin endpoints
                        "/api/bank/admin/schools",
                        
                        // Guardian endpoints
                        "/api/school/guardians",
                        
                        // Fee Payment endpoints
                        "/api/bank/lookup",
                        "/api/bank/reconciliation/today",
                        "/api/bank/payment/counter",
                        
                        // Audit Trail endpoints
                        "/api/admin/audit-trail"
                )
                .build();
    }
}
