package com.bank.schoolmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) - Student Upload Result
 * 
 * WHAT IS A DTO?
 * A DTO is a simple object that carries data between processes.
 * It's like a "messenger" that delivers information in a specific format.
 * 
 * WHY USE DTOs?
 * 1. Separation of Concerns: API responses don't have to match database structure
 * 2. Flexibility: Can combine data from multiple sources
 * 3. Security: Don't expose internal database structure
 * 4. Versioning: Can change API without changing database
 * 
 * THIS DTO represents the result of trying to import ONE student from Excel.
 * It tells us:
 * - Was the import successful?
 * - Which row in Excel was this?
 * - What was the student's name?
 * - If it failed, what was the error message?
 * 
 * LOMBOK ANNOTATIONS:
 * @Data - Generates getters, setters, toString, equals, hashCode
 * @AllArgsConstructor - Generates constructor with all fields
 * @NoArgsConstructor - Generates empty constructor (needed for JSON conversion)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentUploadResult {
    
    /**
     * The row number in the Excel file (starts from 2, since row 1 is headers)
     * Helps the bursar identify which student had an issue
     */
    private int rowNumber;
    
    /**
     * Student's first name from Excel
     */
    private String firstName;
    
    /**
     * Student's last name from Excel
     */
    private String lastName;
    
    /**
     * Whether this student was successfully imported
     * true = saved to database
     * false = validation failed or duplicate
     */
    private boolean success;
    
    /**
     * Error message if import failed
     * Examples:
     * - "Student ID already exists"
     * - "Invalid email format"
     * - "Phone number is required"
     * null if success = true
     */
    private String errorMessage;
    
    /**
     * Convenience constructor for successful imports
     * When import succeeds, we don't need an error message
     */
    public StudentUploadResult(int rowNumber, String firstName, String lastName, boolean success) {
        this.rowNumber = rowNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.success = success;
        this.errorMessage = null;
    }
}
