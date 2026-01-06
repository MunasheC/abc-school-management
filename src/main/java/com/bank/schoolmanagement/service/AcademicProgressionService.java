package com.bank.schoolmanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Academic Progression Service
 * 
 * PURPOSE: Handle grade/form progression logic based on school type
 * 
 * SCHOOL TYPES:
 * 1. PRIMARY - Uses Grades (1-7)
 * 2. SECONDARY - Uses Forms (1-6)
 * 3. COMBINED - Uses both (Grades 1-7, Forms 1-6)
 * 
 * PROGRESSION RULES:
 * 
 * PRIMARY SCHOOLS:
 * - Grade 1 → Grade 2
 * - Grade 2 → Grade 3
 * - ...
 * - Grade 6 → Grade 7
 * - Grade 7 → COMPLETED_PRIMARY
 * 
 * SECONDARY SCHOOLS:
 * - Form 1 → Form 2
 * - Form 2 → Form 3
 * - Form 3 → Form 4
 * - Form 4 → COMPLETED_O_LEVEL (can stop or continue to A Level)
 * - Form 4 → Form 5 (for those continuing)
 * - Form 5 → Form 6
 * - Form 6 → COMPLETED_A_LEVEL
 * 
 * LEARNING: Zimbabwe education system
 * - Primary: Grades 1-7 (ages 6-13)
 * - Secondary O Level: Forms 1-4 (ages 13-17)
 * - Secondary A Level: Forms 5-6 (ages 17-19)
 */
@Service
@Slf4j
public class AcademicProgressionService {
    
    /**
     * Get next grade/form based on current level and school type
     * 
     * @param currentGrade Current grade/form (e.g., "Grade 3", "Form 2")
     * @param schoolType School type (PRIMARY, SECONDARY, COMBINED)
     * @return Next level or completion status
     */
    public ProgressionResult getNextLevel(String currentGrade, String schoolType) {
        log.debug("Calculating next level for: {} in {} school", currentGrade, schoolType);
        
        if (currentGrade == null || currentGrade.isBlank()) {
            throw new IllegalArgumentException("Current grade cannot be null or empty");
        }
        
        String normalized = currentGrade.trim();
        
        // PRIMARY SCHOOL PROGRESSION
        if (schoolType != null && (schoolType.equalsIgnoreCase("PRIMARY") || schoolType.equalsIgnoreCase("COMBINED"))) {
            if (normalized.matches("(?i)grade\\s*1")) return new ProgressionResult("Grade 2", null);
            if (normalized.matches("(?i)grade\\s*2")) return new ProgressionResult("Grade 3", null);
            if (normalized.matches("(?i)grade\\s*3")) return new ProgressionResult("Grade 4", null);
            if (normalized.matches("(?i)grade\\s*4")) return new ProgressionResult("Grade 5", null);
            if (normalized.matches("(?i)grade\\s*5")) return new ProgressionResult("Grade 6", null);
            if (normalized.matches("(?i)grade\\s*6")) return new ProgressionResult("Grade 7", null);
            if (normalized.matches("(?i)grade\\s*7")) return new ProgressionResult(null, "COMPLETED_PRIMARY");
        }
        
        // SECONDARY SCHOOL PROGRESSION
        if (schoolType != null && (schoolType.equalsIgnoreCase("SECONDARY") || schoolType.equalsIgnoreCase("COMBINED"))) {
            if (normalized.matches("(?i)form\\s*1")) return new ProgressionResult("Form 2", null);
            if (normalized.matches("(?i)form\\s*2")) return new ProgressionResult("Form 3", null);
            if (normalized.matches("(?i)form\\s*3")) return new ProgressionResult("Form 4", null);
            if (normalized.matches("(?i)form\\s*4")) {
                // Form 4 → Completed O Level (most students finish here)
                // However, some continue to A Level (Form 5)
                // Default to completion, but can be overridden for A Level students
                return new ProgressionResult(null, "COMPLETED_O_LEVEL");
            }
            if (normalized.matches("(?i)form\\s*5")) return new ProgressionResult("Form 6", null);
            if (normalized.matches("(?i)form\\s*6")) return new ProgressionResult(null, "COMPLETED_A_LEVEL");
        }
        
        // Fallback: couldn't determine progression
        log.warn("Could not determine progression for grade: {} in school type: {}", currentGrade, schoolType);
        throw new IllegalArgumentException("Unknown grade/form progression: " + currentGrade);
    }
    
    /**
     * Check if a student should continue to A Level after Form 4
     * 
     * LOGIC: This can be based on:
     * - Academic performance
     * - Student/parent preference
     * - School policy
     * 
     * For now, this is a placeholder that can be enhanced with business logic
     * 
     * @param studentId Student database ID
     * @return true if student continues to Form 5, false if stops at O Level
     */
    public boolean shouldContinueToALevel(Long studentId) {
        // TODO: Implement business logic
        // - Check academic results
        // - Check student/parent preference
        // - Check fee payment status
        // - Check school capacity for A Level
        
        // For now, default to: students continue to A Level
        // Schools can override this per student manually
        return true;
    }
    
    /**
     * Get standard grade/form name for a level
     * 
     * Normalizes variations like "grade1", "Grade 1", "GRADE 1" → "Grade 1"
     * 
     * @param grade Input grade/form
     * @return Standardized name
     */
    public String normalizeGradeName(String grade) {
        if (grade == null || grade.isBlank()) {
            return grade;
        }
        
        String normalized = grade.trim();
        
        // Grade patterns
        if (normalized.matches("(?i)grade\\s*1")) return "Grade 1";
        if (normalized.matches("(?i)grade\\s*2")) return "Grade 2";
        if (normalized.matches("(?i)grade\\s*3")) return "Grade 3";
        if (normalized.matches("(?i)grade\\s*4")) return "Grade 4";
        if (normalized.matches("(?i)grade\\s*5")) return "Grade 5";
        if (normalized.matches("(?i)grade\\s*6")) return "Grade 6";
        if (normalized.matches("(?i)grade\\s*7")) return "Grade 7";
        
        // Form patterns
        if (normalized.matches("(?i)form\\s*1")) return "Form 1";
        if (normalized.matches("(?i)form\\s*2")) return "Form 2";
        if (normalized.matches("(?i)form\\s*3")) return "Form 3";
        if (normalized.matches("(?i)form\\s*4")) return "Form 4";
        if (normalized.matches("(?i)form\\s*5")) return "Form 5";
        if (normalized.matches("(?i)form\\s*6")) return "Form 6";
        
        // Return as-is if doesn't match patterns
        return grade;
    }
    
    /**
     * Validate grade/form is appropriate for school type
     * 
     * @param grade Grade/form to validate
     * @param schoolType School type
     * @return true if valid, false otherwise
     */
    public boolean isValidGradeForSchoolType(String grade, String schoolType) {
        if (grade == null || schoolType == null) {
            return false;
        }
        
        String normalized = grade.trim().toLowerCase();
        String type = schoolType.trim().toUpperCase();
        
        boolean isGrade = normalized.startsWith("grade");
        boolean isForm = normalized.startsWith("form");
        
        if (type.equals("PRIMARY")) {
            return isGrade; // Primary schools use Grades only
        } else if (type.equals("SECONDARY")) {
            return isForm; // Secondary schools use Forms only
        } else if (type.equals("COMBINED")) {
            return isGrade || isForm; // Combined schools use both
        }
        
        return false;
    }
    
    /**
     * Progression result container
     */
    public static class ProgressionResult {
        private final String nextGrade;
        private final String completionStatus;
        
        public ProgressionResult(String nextGrade, String completionStatus) {
            this.nextGrade = nextGrade;
            this.completionStatus = completionStatus;
        }
        
        public String getNextGrade() {
            return nextGrade;
        }
        
        public String getCompletionStatus() {
            return completionStatus;
        }
        
        public boolean isCompleted() {
            return completionStatus != null;
        }
    }
}
