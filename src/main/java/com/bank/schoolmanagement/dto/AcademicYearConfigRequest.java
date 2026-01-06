package com.bank.schoolmanagement.dto;

import com.bank.schoolmanagement.dto.YearEndPromotionRequest.FeeStructure;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * Request DTO for configuring academic year end-of-year promotion
 */
@Data
public class AcademicYearConfigRequest {
    
    /**
     * Academic year identifier
     * Example: "2025/2026"
     */
    private String academicYear;
    
    /**
     * Date when automatic promotion should trigger
     * Example: "2025-12-31"
     */
    private LocalDate endOfYearDate;
    
    /**
     * New academic year name for promoted students
     * Example: "2026 Academic Year"
     */
    private String nextAcademicYear;
    
    /**
     * Should balances carry forward?
     */
    private Boolean carryForwardBalances = true;
    
    /**
     * Fee structures per grade/form
     * Same format as YearEndPromotionRequest
     */
    private Map<String, FeeStructure> feeStructures;
    
    /**
     * Default fee structure fallback
     */
    private FeeStructure defaultFeeStructure;
    
    /**
     * Configuration notes
     */
    private String notes;
}
