package com.bank.schoolmanagement.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Student Promotion Request DTO
 * 
 * PURPOSE: Handle student grade progression (e.g., Form 1 → Form 2)
 * 
 * USAGE SCENARIOS:
 * 1. Individual promotion - promote specific student
 * 2. Bulk promotion - promote entire grade at year-end
 * 
 * LEARNING: Grade progression workflow
 * - Update student's grade and className
 * - Create new fee record for new academic year/term
 * - Carry forward any outstanding balances (optional)
 * - Keep historical records intact for audit
 */
@Data
public class StudentPromotionRequest {
    
    /**
     * New grade/form level
     * Examples: "Form 2", "Grade 6", "Form 5"
     */
    private String newGrade;
    
    /**
     * New class name within grade (optional)
     * Examples: "2A", "2B", "6 Blue"
     */
    private String newClassName;
    
    /**
     * New academic term/year
     * Examples: "Term 1 2026", "2026 Academic Year"
     */
    private String newTermYear;
    
    /**
     * Should previous term's outstanding balance carry forward?
     * - true: Add previous balance to new fee record
     * - false: Start fresh (student paid up or balance waived)
     */
    private Boolean carryForwardBalance = true;
    
    /**
     * New fee amounts for the promoted grade
     * If null, system can use default fees for the grade
     */
    private BigDecimal tuitionFee;
    private BigDecimal boardingFee;
    private BigDecimal developmentLevy;
    private BigDecimal examFee;
    private BigDecimal otherFees;
    
    /**
     * Maintain or update scholarship
     */
    private BigDecimal scholarshipAmount;
    private BigDecimal siblingDiscount;
    
    /**
     * Fee category for new term
     */
    private String feeCategory;
    
    /**
     * Optional notes about promotion
     * Examples: "Promoted to Form 2 - excellent performance", "Repeat Form 3"
     */
    private String promotionNotes;
}
