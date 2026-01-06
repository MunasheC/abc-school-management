package com.bank.schoolmanagement.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Bulk Promotion Summary Response
 * 
 * PURPOSE: Report results of bulk grade promotion operation
 * 
 * USAGE: When promoting entire grade (e.g., all Form 1 students to Form 2)
 */
@Data
public class BulkPromotionSummary {
    
    /**
     * Total students processed in bulk promotion
     */
    private Integer totalStudents;
    
    /**
     * Number successfully promoted
     */
    private Integer successCount;
    
    /**
     * Number that failed promotion
     */
    private Integer failureCount;
    
    /**
     * List of successfully promoted student IDs
     */
    private List<String> promotedStudentIds = new ArrayList<>();
    
    /**
     * List of errors encountered during promotion
     */
    private List<PromotionError> errors = new ArrayList<>();
    
    /**
     * Summary message
     */
    private String message;
    
    /**
     * Individual error details
     */
    @Data
    public static class PromotionError {
        private Long studentId;
        private String studentName;
        private String error;
        
        public PromotionError(Long studentId, String studentName, String error) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.error = error;
        }
    }
}
