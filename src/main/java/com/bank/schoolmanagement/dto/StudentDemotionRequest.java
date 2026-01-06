package com.bank.schoolmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request to demote a student back to a previous grade
 * 
 * USE CASE: After automatic year-end promotion, admin realizes student
 * should be repeating the grade
 * 
 * EXAMPLE: Student auto-promoted Form 1→Form 2, but should repeat Form 1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDemotionRequest {
    
    /**
     * Grade to demote student back to
     * Example: "Form 1", "Grade 6"
     */
    private String demotedGrade;
    
    /**
     * Class within the demoted grade
     * Example: "Form 1A"
     */
    private String demotedClassName;
    
    /**
     * Reason for demotion
     * Example: "Student failed end-of-year exams, must repeat Form 1"
     */
    private String reason;
    
    /**
     * Academic year for the repeated grade
     * Example: "2026 Academic Year"
     */
    private String academicYear;
    
    /**
     * Should we carry forward the outstanding balance from promoted fee record?
     * Default: true
     */
    private Boolean carryForwardBalance = true;
    
    // Fee structure for repeated grade
    private BigDecimal tuitionFee;
    private BigDecimal boardingFee;
    private BigDecimal developmentLevy;
    private BigDecimal examFee;
    private BigDecimal otherFees;
}
