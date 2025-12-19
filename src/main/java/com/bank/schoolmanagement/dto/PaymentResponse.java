package com.bank.schoolmanagement.dto;

import com.bank.schoolmanagement.entity.Payment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long id;
    private String paymentReference;
    private Long studentId;
    private String studentName;
    private Long schoolId;
    private String schoolName;
    private Long feeRecordId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String transactionReference;
    private String bankTransactionId;
    private LocalDateTime paymentDate;
    private BigDecimal newOutstandingBalance;
    private String receivedBy;
    private String paymentNotes;

    public static PaymentResponse fromEntity(Payment payment) {
        PaymentResponse dto = new PaymentResponse();
        dto.setId(payment.getId());
        dto.setPaymentReference(payment.getPaymentReference());
        if (payment.getStudent() != null) {
            dto.setStudentId(payment.getStudent().getId());
            dto.setStudentName(payment.getStudent().getFullName());
        }
        if (payment.getSchool() != null) {
            dto.setSchoolId(payment.getSchool().getId());
            dto.setSchoolName(payment.getSchool().getSchoolName());
        }
        if (payment.getFeeRecord() != null) {
            dto.setFeeRecordId(payment.getFeeRecord().getId());
            dto.setNewOutstandingBalance(payment.getFeeRecord().getOutstandingBalance());
        }
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null);
        dto.setStatus(payment.getStatus());
        dto.setTransactionReference(payment.getTransactionReference());
        dto.setBankTransactionId(payment.getBankTransactionId());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setReceivedBy(payment.getReceivedBy());
        dto.setPaymentNotes(payment.getPaymentNotes());
        return dto;
    }
}
