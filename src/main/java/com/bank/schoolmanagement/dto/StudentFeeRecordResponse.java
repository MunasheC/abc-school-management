package com.bank.schoolmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for StudentFeeRecord API responses
 * Avoids circular reference issues with entity relationships
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeeRecordResponse {
    
    private Long id;
    
    // Student info (simplified)
    private Long studentId;
    private String studentReference;
    private String studentName;
    
    // Academic info
    private String academicYear;
    private String term;
    private String termYear;
    private String feeCategory;
    
    // Fee components
    private BigDecimal tuitionFee;
    private BigDecimal boardingFee;
    private BigDecimal developmentLevy;
    private BigDecimal examFee;
    private BigDecimal otherFees;
    
    // Discounts
    private Boolean hasScholarship;
    private BigDecimal scholarshipAmount;
    private BigDecimal siblingDiscount;
    private BigDecimal earlyPaymentDiscount;
    
    // Calculated totals
    private BigDecimal grossAmount;
    private BigDecimal netAmount;
    private BigDecimal previousBalance;
    private BigDecimal amountPaid;
    private BigDecimal outstandingBalance;
    
    // Status
    private String paymentStatus;
    private Boolean isActive;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
