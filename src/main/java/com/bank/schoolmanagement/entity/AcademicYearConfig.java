package com.bank.schoolmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Academic Year Configuration Entity
 * 
 * PURPOSE: Configure end-of-year dates for automatic student promotion
 * 
 * WORKFLOW:
 * 1. School admin sets end-of-year date (e.g., December 31, 2025)
 * 2. Scheduled task checks daily if date is reached
 * 3. On end-of-year date, automatic promotion is triggered
 * 4. Admin can still exclude students or demote after promotion
 * 
 * FEATURES:
 * - One configuration per school per academic year
 * - Tracks promotion status (scheduled, completed, cancelled)
 * - Stores fee structures for automatic fee record creation
 * - Allows configuring exclusion rules
 */
@Entity
@Table(name = "academic_year_configs", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"school_id", "academic_year"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * School this configuration belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;
    
    /**
     * Academic year identifier
     * Example: "2025/2026", "2026 Academic Year"
     */
    @Column(name = "academic_year", nullable = false, length = 50)
    private String academicYear;
    
    /**
     * Date when year-end promotion should be triggered
     * Example: 2025-12-31
     */
    @Column(name = "end_of_year_date", nullable = false)
    private LocalDate endOfYearDate;
    
    /**
     * New academic year name for promoted students
     * Example: "2026 Academic Year"
     */
    @Column(name = "next_academic_year", length = 50)
    private String nextAcademicYear;
    
    /**
     * Should outstanding balances carry forward to next year?
     */
    @Column(name = "carry_forward_balances")
    private Boolean carryForwardBalances = true;
    
    /**
     * Promotion status
     * Values: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, FAILED
     */
    @Column(name = "promotion_status", length = 20)
    private String promotionStatus = "SCHEDULED";
    
    /**
     * When the promotion was actually executed
     */
    @Column(name = "promotion_executed_at")
    private LocalDateTime promotionExecutedAt;
    
    /**
     * How many students were promoted
     */
    @Column(name = "students_promoted")
    private Integer studentsPromoted = 0;
    
    /**
     * How many students completed their education
     */
    @Column(name = "students_completed")
    private Integer studentsCompleted = 0;
    
    /**
     * How many errors occurred during promotion
     */
    @Column(name = "promotion_errors")
    private Integer promotionErrors = 0;
    
    /**
     * Notes about this academic year configuration
     */
    @Column(name = "notes", length = 1000)
    private String notes;
    
    /**
     * Fee structure as JSON for promoted students
     * Stores the complete YearEndPromotionRequest.feeStructures
     */
    @Column(name = "fee_structures", columnDefinition = "TEXT")
    private String feeStructuresJson;
    
    /**
     * Default fee structure as JSON
     */
    @Column(name = "default_fee_structure", columnDefinition = "TEXT")
    private String defaultFeeStructureJson;
    
    /**
     * Is this configuration active?
     */
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Who created this configuration
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    /**
     * Check if promotion date has been reached
     */
    public boolean shouldTriggerPromotion() {
        return isActive 
            && "SCHEDULED".equals(promotionStatus) 
            && endOfYearDate != null 
            && !LocalDate.now().isBefore(endOfYearDate);
    }
    
    /**
     * Mark promotion as completed
     */
    public void markPromotionCompleted(int promoted, int completed, int errors) {
        this.promotionStatus = "COMPLETED";
        this.promotionExecutedAt = LocalDateTime.now();
        this.studentsPromoted = promoted;
        this.studentsCompleted = completed;
        this.promotionErrors = errors;
    }
    
    /**
     * Mark promotion as failed
     */
    public void markPromotionFailed(String errorMessage) {
        this.promotionStatus = "FAILED";
        this.promotionExecutedAt = LocalDateTime.now();
        this.notes = (this.notes != null ? this.notes + "\n" : "") + 
                    "FAILED: " + errorMessage;
    }
    
    /**
     * Cancel scheduled promotion
     */
    public void cancel(String reason) {
        this.promotionStatus = "CANCELLED";
        this.notes = (this.notes != null ? this.notes + "\n" : "") + 
                    "CANCELLED: " + reason;
    }
}
