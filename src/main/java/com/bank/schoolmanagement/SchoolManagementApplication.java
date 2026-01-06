package com.bank.schoolmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Application Class
 * 
 * @SpringBootApplication is a convenience annotation that combines:
 * - @Configuration: Tags this class as a source of bean definitions
 * - @EnableAutoConfiguration: Tells Spring Boot to auto-configure based on dependencies
 * - @ComponentScan: Tells Spring to scan for components in this package and sub-packages
 * 
 * @EnableScheduling enables Spring's scheduled task execution capability
 * Required for @Scheduled annotations to work (e.g., automatic year-end promotion)
 */
@SpringBootApplication
@EnableScheduling
public class SchoolManagementApplication {

    /**
     * Main method - entry point of the application
     * SpringApplication.run() bootstraps the application
     */
    public static void main(String[] args) {
        SpringApplication.run(SchoolManagementApplication.class, args);
        System.out.println("\n✅ School Management System Started Successfully!");
        
        
    }
}
