package com.bank.schoolmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * StudentFeeRecord Entity - Financial Information for a Student
 * 
 * LEARNING: Separate entity for fees because:
 * 1. Fee structure can change term-by-term or year-by-year
 * 2. Can track historical fee records (one per term/year)
 * 3. Financial calculations separate from student personal data
 * 4. Easier to generate financial reports
 * 
 * RELATIONSHIP: One Student has One FeeRecord (current term/year)
 * In future: One Student can have Many FeeRecords (historical)
 */
@Entity
@Table(name = "student_fee_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentFeeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relationship to School (Multi-Tenancy)
     * 
     * @ManyToOne - Many fee records belong to one school
     * 
     * LEARNING: Denormalized for performance
     * - Fee records already linked to student (who has school)
     * - Adding school here allows direct fee queries by school
     * - Avoids JOIN through student table for financial reports
     * - Critical for multi-tenant data isolation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    @JsonIgnoreProperties({"students", "hibernateLazyInitializer", "handler"})
    private School school;

    /**
     * Relationship to student
     * 
     * @ManyToOne - Many fee records can belong to one student (one per term/year)
     * @JoinColumn - Creates a foreign key column "student_id"
     * 
     * CHANGE: Previously @OneToOne, now @ManyToOne to support historical records
     * - One student can have multiple fee records
     * - Each fee record represents a different term/year
     * - Enables full financial history tracking
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnoreProperties({"school", "guardian", "feeRecords", "hibernateLazyInitializer", "handler"})
    private Student student;

    /** Academic year for this fee record (e.g., 2024, 2025) */
    @NotNull(message = "Academic year is required")
    @Min(value = 2000, message = "Year must be at least 2000")
    @Max(value = 2100, message = "Year must be before 2100")
    @Column(name = "year", nullable = false)
    private Integer year;

    /** Academic term for this fee record (1, 2, 3) */
    @NotNull(message = "Term is required")
    @Min(value = 1, message = "Term must be 1, 2, or 3")
    @Max(value = 3, message = "Term must be 1, 2, or 3")
    private Integer term;

    /** Fee category: DAY_SCHOLAR or BOARDING */
    @NotBlank(message = "Fee category is required")
    @Pattern(regexp = "^(DAY_SCHOLAR|BOARDING)$", message = "Fee category must be either 'DAY_SCHOLAR' or 'BOARDING'")
    @Column(name = "fee_category", nullable = false)
    private String feeCategory;
    
    /** Currency for this fee record: USD or ZWG */
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^(USD|ZWG)$", message = "Currency must be either 'USD' or 'ZWG'")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    /* ----------------------  FEE COMPONENTS  ------------------------- */

    @Column(name = "tuition_fee", precision = 10, scale = 2)
    private BigDecimal tuitionFee = BigDecimal.ZERO;

    @Column(name = "boarding_fee", precision = 10, scale = 2)
    private BigDecimal boardingFee = BigDecimal.ZERO;

    @Column(name = "development_levy", precision = 10, scale = 2)
    private BigDecimal developmentLevy = BigDecimal.ZERO;

    @Column(name = "exam_fee", precision = 10, scale = 2)
    private BigDecimal examFee = BigDecimal.ZERO;

    @Column(name = "other_fees", precision = 10, scale = 2)
    private BigDecimal otherFees = BigDecimal.ZERO;

    /* ----------------------  DISCOUNTS  ------------------------- */

    @Column(name = "has_scholarship")
    private Boolean hasScholarship = false;

    @Column(name = "scholarship_amount", precision = 12, scale = 2)
    private BigDecimal scholarshipAmount = BigDecimal.ZERO;

    @Column(name = "sibling_discount", precision = 12, scale = 2)
    private BigDecimal siblingDiscount = BigDecimal.ZERO;

    @Column(name = "early_payment_discount", precision = 12, scale = 2)
    private BigDecimal earlyPaymentDiscount = BigDecimal.ZERO;

    /* ----------------------  CALCULATED TOTALS  ------------------------- */

    /** Total fees before discounts */
    @Column(name = "gross_amount", precision = 12, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    /** Total after discounts = amount student must pay */
    @Column(name = "net_amount", precision = 12, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    /** Carried over from previous term/year */
    @Column(name = "previous_balance", precision = 12, scale = 2)
    private BigDecimal previousBalance = BigDecimal.ZERO;

    /** Total amount paid so far */
    @Column(name = "amount_paid", precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    /** Remaining balance */
    @Column(name = "outstanding_balance", precision = 12, scale = 2)
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    /** Payment status: PAID, PARTIALLY_PAID, ARREARS */
    @Column(name = "payment_status")
    private String paymentStatus;

    /* ----------------------  NOTES  ------------------------- */

    @Column(name = "bursar_notes", length = 1000)
    private String bursarNotes;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Calculate all financial totals
     * 
     * LEARNING: @PrePersist runs before saving to database
     * @PreUpdate runs before updating existing record
     * 
     * This ensures calculations are always up-to-date
     */
    @PrePersist
    @PreUpdate
    public void calculateTotals() {
        // Calculate gross amount (before discounts)
        this.grossAmount = tuitionFee
                .add(boardingFee)
                .add(developmentLevy)
                .add(examFee)
                .add(otherFees);

        // Calculate net amount (after discounts)
        this.netAmount = grossAmount
                .subtract(scholarshipAmount)
                .subtract(siblingDiscount)
                .subtract(earlyPaymentDiscount);

        // Calculate outstanding balance
        this.outstandingBalance = netAmount
                .add(previousBalance)
                .subtract(amountPaid);

        // Determine payment status
        if (outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            this.paymentStatus = "PAID";
        } else if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.paymentStatus = "PARTIALLY_PAID";
        } else {
            this.paymentStatus = "ARREARS";
        }
    }

    /**
     * Record a payment
     * 
     * @param amount - Payment amount
     */
    public void addPayment(BigDecimal amount) {
        this.amountPaid = this.amountPaid.add(amount);
        calculateTotals();  // Recalculate balances
    }

    /**
     * Check if fully paid
     */
    public boolean isFullyPaid() {
        return outstandingBalance.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Get payment percentage (how much of bill is paid)
     */
    public double getPaymentPercentage() {
        if (netAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 100.0;
        }
        return amountPaid.divide(netAmount, 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
}
