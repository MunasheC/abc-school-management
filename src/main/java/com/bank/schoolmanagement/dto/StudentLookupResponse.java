package com.bank.schoolmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for student lookup response from bank endpoints
 * Avoids circular reference issues with entity relationships
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentLookupResponse {
    
    // Student info
    private String studentId;
    private String studentName;
    private String grade;
    private String className;
    
    // School info
    private String schoolCode;
    private String schoolName;
    
    // Fee info
    private String feeCategory;
    private BigDecimal totalFees;
    private BigDecimal amountPaid;
    private BigDecimal outstandingBalance;
    private String paymentStatus;
}
