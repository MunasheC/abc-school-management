package com.bank.schoolmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for looking up students by name, school, and grade
 * Used when parents don't know the student reference code
 * All fields are optional - can search by any combination
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentSearchRequest {
    
    private String studentName;  // Searches both first and last name
    private String schoolName;
    private String grade;  // e.g., "GRADE_9", "FORM_1", etc.
}
