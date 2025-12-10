package com.bank.schoolmanagement.entity;

import com.bank.schoolmanagement.enums.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Entity - Individual Payment Transactions
 * 
 * LEARNING: Track each payment separately because:
 * 1. Audit trail - who paid what, when
 * 2. Can link to bank transactions
 * 3. Can reverse/refund payments
 * 4. Generate payment receipts
 * 5. Track payment methods (cash, mobile, bank transfer)
 * 
 * RELATIONSHIP: One Student has Many Payments over time
 */
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique payment reference number */
    @Column(name = "payment_reference", unique = true, nullable = false)
    private String paymentReference;

    /**
     * Relationship to School (Multi-Tenancy)
     * 
     * @ManyToOne - Many payments belong to one school
     * 
     * LEARNING: Denormalized for performance and security
     * - Payments already linked to student (who has school)
     * - Adding school here enables:
     *   1. Fast financial reports per school (no JOIN needed)
     *   2. Bank reconciliation per school
     *   3. Direct data isolation check
     * - Critical for multi-tenant payment processing
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    /**
     * Relationship to student
     * 
     * @ManyToOne - Many payments belong to one student
     * A student can make multiple payments over time
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Relationship to fee record (optional)
     * Links payment to specific term/year
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_record_id")
    private StudentFeeRecord feeRecord;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    /**
     * Payment method enum
     * Supports both school-based and bank-based channels
     * 
     * School channels: CASH, MOBILE_MONEY, BANK_TRANSFER, CHEQUE, CARD
     * Bank channels: BANK_COUNTER, MOBILE_BANKING, INTERNET_BANKING, USSD, STANDING_ORDER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    /** Bank/mobile money transaction reference */
    @Column(name = "transaction_reference")
    private String transactionReference;

    /** Who received the payment (bursar name) */
    @Column(name = "received_by")
    private String receivedBy;

    @Column(name = "payment_notes", length = 500)
    private String paymentNotes;

    /** Payment status: COMPLETED, PENDING, REVERSED, FAILED */
    @Column(name = "status")
    private String status = "COMPLETED";

    @CreationTimestamp
    @Column(name = "payment_date", nullable = false, updatable = false)
    private LocalDateTime paymentDate;

    /** If payment was reversed/refunded */
    @Column(name = "is_reversed")
    private Boolean isReversed = false;

    @Column(name = "reversal_reason")
    private String reversalReason;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    // ========== BANK PAYMENT FIELDS (Option A & C) ==========

    /**
     * Bank branch where payment was processed
     * Example: "Branch 005 - Harare Central"
     * Used for: Bank teller payments (Option A)
     */
    @Column(name = "bank_branch", length = 100)
    private String bankBranch;

    /**
     * Bank teller who processed the payment
     * Example: "John Ncube"
     * Used for: Accountability and audit trail
     */
    @Column(name = "teller_name", length = 100)
    private String tellerName;

    /**
     * Parent's bank account number
     * Used for: Reconciliation and fraud prevention
     * Helps track which parent made payment
     */
    @Column(name = "parent_account_number", length = 50)
    private String parentAccountNumber;

    /**
     * Bank's internal transaction ID
     * Example: "BNK-TXN-789456"
     * Critical for: Bank-to-school reconciliation
     */
    @Column(name = "bank_transaction_id", length = 100)
    private String bankTransactionId;

    /**
     * Timestamp when bank processed the payment
     * May differ from paymentDate (when recorded in school system)
     * Used for: Reconciliation and dispute resolution
     */
    @Column(name = "bank_processed_time")
    private LocalDateTime bankProcessedTime;

    /**
     * Generate payment reference before saving
     */
    @PrePersist
    public void generatePaymentReference() {
        if (this.paymentReference == null || this.paymentReference.isBlank()) {
            this.paymentReference = "PAY" + System.currentTimeMillis();
        }
    }

    /**
     * Reverse this payment
     * 
     * @param reason - Why the payment is being reversed
     */
    public void reverse(String reason) {
        this.isReversed = true;
        this.reversalReason = reason;
        this.reversedAt = LocalDateTime.now();
        this.status = "REVERSED";
    }

    /**
     * Check if payment is active (not reversed)
     */
    public boolean isActive() {
        return !isReversed && "COMPLETED".equals(status);
    }

    /**
     * Check if payment was made through bank channel
     * (teller, mobile banking, internet banking, etc.)
     */
    public boolean isBankChannelPayment() {
        return paymentMethod != null && paymentMethod.isBankChannel();
    }

    /**
     * Check if bank transaction details are complete
     * Used for validation before saving bank payments
     */
    public boolean hasBankTransactionDetails() {
        return bankTransactionId != null && !bankTransactionId.isBlank();
    }
}
