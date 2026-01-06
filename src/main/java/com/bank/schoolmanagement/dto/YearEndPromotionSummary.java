package com.bank.schoolmanagement.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Year-End Promotion Summary Response
 * 
 * PURPOSE: Detailed report of atomic year-end promotion results
 * 
 * STRUCTURE:
 * - Overall statistics (total processed, promoted, completed, excluded, errors)
 * - Breakdown per grade/form
 * - List of students who completed their education
 * - Error details for failed promotions
 */
@Data
public class YearEndPromotionSummary {
    
    // Overall Statistics
    private Integer totalStudentsProcessed;
    private Integer promotedCount;
    private Integer completedCount; // Students who finished (Grade 7, Form 4, Form 6)
    private Integer excludedCount; // Manually excluded students
    private Integer errorCount;
    
    // Academic year promoted to
    private String newAcademicYear;
    
    // Breakdown per grade/form
    private Map<String, GradePromotionStats> gradeBreakdown = new HashMap<>();
    
    // Lists of student IDs
    private List<String> promotedStudentIds = new ArrayList<>();
    private List<CompletedStudent> completedStudents = new ArrayList<>();
    private List<String> excludedStudentIds = new ArrayList<>();
    private List<PromotionError> errors = new ArrayList<>();
    
    // Summary message
    private String message;
    
    /**
     * Statistics for a specific grade/form
     */
    @Data
    public static class GradePromotionStats {
        private String fromGrade;
        private String toGrade; // or "COMPLETED"
        private Integer studentCount;
        private Integer successCount;
        private Integer errorCount;
    }
    
    /**
     * Student who completed their education
     */
    @Data
    public static class CompletedStudent {
        private Long studentId;
        private String studentReference;
        private String fullName;
        private String completionStatus; // COMPLETED_PRIMARY, COMPLETED_O_LEVEL, COMPLETED_A_LEVEL
        
        public CompletedStudent(Long id, String reference, String name, String status) {
            this.studentId = id;
            this.studentReference = reference;
            this.fullName = name;
            this.completionStatus = status;
        }
    }
    
    /**
     * Promotion error details
     */
    @Data
    public static class PromotionError {
        private Long studentId;
        private String studentName;
        private String currentGrade;
        private String error;
        
        public PromotionError(Long id, String name, String grade, String error) {
            this.studentId = id;
            this.studentName = name;
            this.currentGrade = grade;
            this.error = error;
        }
    }
}
