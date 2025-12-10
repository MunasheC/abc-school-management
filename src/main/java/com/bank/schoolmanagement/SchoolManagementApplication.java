package com.bank.schoolmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application Class
 * 
 * @SpringBootApplication is a convenience annotation that combines:
 * - @Configuration: Tags this class as a source of bean definitions
 * - @EnableAutoConfiguration: Tells Spring Boot to auto-configure based on dependencies
 * - @ComponentScan: Tells Spring to scan for components in this package and sub-packages
 */
@SpringBootApplication
public class SchoolManagementApplication {

    /**
     * Main method - entry point of the application
     * SpringApplication.run() bootstraps the application
     */
    public static void main(String[] args) {
        SpringApplication.run(SchoolManagementApplication.class, args);
        System.out.println("\n✅ School Management System Started Successfully!");
        System.out.println("🌐 Access the application at: http://localhost:8080");
        System.out.println("📚 Ready to learn Spring Boot!\n");
    }
}
