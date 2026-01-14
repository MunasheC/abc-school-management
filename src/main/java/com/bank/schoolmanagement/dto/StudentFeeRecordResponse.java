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
    private String studentId;
    private String studentName;
    private String grade;
    private String className;
    private String nationalId;
    // Academic info
    private Integer year;
    private Integer term;
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
    private String currency;
    // Status
    private String paymentStatus;
    private Boolean isActive;
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StudentFeeRecordResponse fromEntity(com.bank.schoolmanagement.entity.StudentFeeRecord entity) {
        StudentFeeRecordResponse dto = new StudentFeeRecordResponse();
        dto.setId(entity.getId());
        if (entity.getStudent() != null) {
            dto.setStudentId(entity.getStudent().getStudentId());
            dto.setStudentName(entity.getStudent().getFirstName() + " " + entity.getStudent().getLastName());
            dto.setGrade(entity.getStudent().getGrade());
            dto.setClassName(entity.getStudent().getClassName());
            dto.setNationalId(entity.getStudent().getNationalId());
        }
        dto.setYear(entity.getYear());
        dto.setTerm(entity.getTerm());
        dto.setFeeCategory(entity.getFeeCategory());
        dto.setTuitionFee(entity.getTuitionFee());
        dto.setBoardingFee(entity.getBoardingFee());
        dto.setDevelopmentLevy(entity.getDevelopmentLevy());
        dto.setExamFee(entity.getExamFee());
        dto.setOtherFees(entity.getOtherFees());
        dto.setHasScholarship(entity.getHasScholarship());
        dto.setScholarshipAmount(entity.getScholarshipAmount());
        dto.setSiblingDiscount(entity.getSiblingDiscount());
        dto.setEarlyPaymentDiscount(entity.getEarlyPaymentDiscount());
        dto.setGrossAmount(entity.getGrossAmount());
        dto.setNetAmount(entity.getNetAmount());
        dto.setPreviousBalance(entity.getPreviousBalance());
        dto.setAmountPaid(entity.getAmountPaid());
        dto.setOutstandingBalance(entity.getOutstandingBalance());
        dto.setCurrency(entity.getCurrency());
        dto.setPaymentStatus(entity.getPaymentStatus());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
