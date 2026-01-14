package com.bank.schoolmanagement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Year-End Promotion Request DTO
 * 
 * PURPOSE: Handle atomic year-end promotions for all grades/forms
 * 
 * CRITICAL: This prevents double-promotion bug by processing all promotions atomically
 * 
 * WORKFLOW:
 * 1. Primary School: Grade 1→2, 2→3, ..., 6→7, 7→Completed
 * 2. Secondary School: Form 1→2, 2→3, 3→4→Completed O Level, 4→5, 5→6, 6→Completed A Level
 * 3. All promotions happen together (not sequentially)
 * 4. Excluded students maintain current grade/form
 * 
 * LEARNING: Atomic promotion prevents this bug:
 * - BAD: Promote Form 1→2 first, then Form 2→3 (newly promoted Form 2s get promoted again!)
 * - GOOD: Take snapshot of all students, then promote all at once based on snapshot
 */
@Data
public class YearEndPromotionRequest {
    
    /**
     * New academic year/term for ALL promoted students
     * Examples: "2026 Academic Year", "Term 1 2026"
     */
    private Integer newYear;

    private Integer newTerm;
    
    /**
     * Should outstanding balances carry forward?
     * - true: Previous term's outstanding balance added to new fee record
     * - false: Start fresh (balances waived or student paid up)
     */
    private Boolean carryForwardBalances = true;
    
    /**
     * Student IDs to EXCLUDE from promotion
     * 
     * USE CASES:
     * - Students repeating a grade/form due to poor performance
     * - Students who transferred out mid-year
     * - Students with incomplete requirements
     * 
     * Example: [123, 456, 789] - these students stay in current grade
     */
    private List<Long> excludedStudentIds;
    
    /**
     * Fee structure per grade/form
     * 
     * KEY STRUCTURE:
     * Map<String, FeeStructure>
     * - Key: Grade or Form level (e.g., "Grade 2", "Form 3")
     * - Value: Fee amounts for that level
     * 
     * EXAMPLE:
     * {
     *   "Grade 2": { tuitionFee: 400, boardingFee: 250, ... },
     *   "Grade 3": { tuitionFee: 450, boardingFee: 250, ... },
     *   "Form 1": { tuitionFee: 600, boardingFee: 400, ... }
     * }
     */
    private java.util.Map<String, FeeStructure> feeStructures;
    
    /**
     * Default fee structure if specific grade/form not found
     * Fallback to ensure all students get a fee record
     */
    private FeeStructure defaultFeeStructure;
    
    /**
     * Optional notes about this year-end promotion
     * Example: "2025→2026 year-end promotion, 95% pass rate"
     */
    private String promotionNotes;
    
    /**
     * Fee Structure for a specific grade/form
     */
    @Data
    public static class FeeStructure {
        private BigDecimal tuitionFee;
        private BigDecimal boardingFee;
        private BigDecimal developmentLevy;
        private BigDecimal examFee;
        private BigDecimal otherFees;
        private BigDecimal defaultScholarship;
        private BigDecimal defaultSiblingDiscount;
        private String feeCategory; // BOARDING, DAY_SCHOLAR, etc.
    }
}
