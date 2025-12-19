package com.bank.schoolmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Student Entity - Core Student Information
 * 
 * REFACTORED DESIGN - Separation of Concerns:
 * 
 * BEFORE: One massive entity with 30+ fields mixing personal, parent, and financial data
 * 
 * AFTER: Clean entity with relationships:
 * - Student (this) - Personal/academic information only
 * - Guardian - Parent/guardian information (can be shared by siblings)
 * - StudentFeeRecord - Financial information (fees, payments, balances)
 * - Payment - Individual payment transactions
 * 
 * BENEFITS:
 * 1. Easier to maintain - changes to fees don't affect student info
 * 2. Better performance - can load student without loading all payments
 * 3. Reusability - siblings share one Guardian record
 * 4. Historical tracking - multiple fee records per student (term by term)
 * 5. Audit trail - individual payment records
 */
@Entity
@Table(name = "students", 
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_school_student", 
            columnNames = {"school_id", "student_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 
     * School-generated student number
     * 
     * LEARNING: Multi-tenant uniqueness
     * - Student ID is unique per school, NOT globally
     * - School A can have student "2025001"
     * - School B can also have student "2025001" (different person)
     * - Enforced via unique constraint (school_id + student_id)
     */
    @Column(name = "student_id", nullable = false)
    private String studentId;

    /* ----------------------  PERSONAL INFORMATION  ------------------------- */

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Past(message = "Date of birth must be in the past")
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;  // MALE, FEMALE, OTHER

    @Column(name = "national_id")
    private String nationalId;

    /* ----------------------  ACADEMIC INFORMATION  ------------------------- */

    @Column(name = "grade")
    private String grade;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "admission_number")
    private String admissionNumber;

    @Column(name = "class_name")
    private String className;  // e.g., "5A", "4B"

    /* ----------------------  RELATIONSHIPS  ------------------------- */

    /**
     * Relationship to School (Multi-Tenancy)
     * 
     * @ManyToOne - Many students belong to one school
     * 
     * LEARNING: Multi-tenant architecture
     * - CRITICAL: Every student MUST belong to a school (nullable = false)
     * - This system is a SaaS platform where multiple schools are onboarded
     * - Each school only sees their own students (data isolation)
     * - All queries must filter by school to prevent data leakage
     * 
     * Why nullable = false?
     * - Students cannot exist without a school
     * - Ensures referential integrity in multi-tenant system
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    /**
     * Relationship to Guardian (Parent)
     * 
     * @ManyToOne - Many students can have one guardian (siblings)
     * This allows siblings to share parent information
     * 
     * LEARNING: fetch = FetchType.LAZY means:
     * - Guardian data not loaded automatically with student
     * - Only loaded when you call student.getGuardian()
     * - Improves performance for queries that don't need guardian info
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "guardian_id")
    private Guardian guardian;

    /**
     * Relationship to Fee Record
     * 
     * @OneToOne - One student has one current fee record
     * 
     * LEARNING: In future, could change to @OneToMany for historical records
     * - Keep separate fee record for each term/year
     * - Track payment history over time
     */
    @OneToOne(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private StudentFeeRecord currentFeeRecord;

    /**
     * Relationship to Payments
     * 
     * @OneToMany - One student has many payments
     * mappedBy = "student" - Payment entity has a field called "student"
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Payment> payments = new ArrayList<>();

    /* ----------------------  STATUS & AUDIT  ------------------------- */

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "withdrawal_date")
    private LocalDate withdrawalDate;

    @Column(name = "withdrawal_reason", length = 500)
    private String withdrawalReason;

    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* ----------------------  HELPER METHODS  ------------------------- */

    @PrePersist
    public void autoGenerateFields() {
        if (this.studentId == null || this.studentId.isBlank()) {
            this.studentId = "STU" + System.currentTimeMillis();
        }

        if (this.enrollmentDate == null) {
            this.enrollmentDate = LocalDate.now();
        }
    }

    /**
     * Get student's full name
     */
    public String getFullName() {
        if (middleName != null && !middleName.isBlank()) {
            return firstName + " " + middleName + " " + lastName;
        }
        return firstName + " " + lastName;
    }

    /**
     * Calculate student's age
     */
    public Integer getAge() {
        if (dateOfBirth == null) {
            return null;
        }
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }

    /**
     * Check if student has outstanding fees
     * Delegates to fee record
     */
    public boolean hasOutstandingBalance() {
        return currentFeeRecord != null && !currentFeeRecord.isFullyPaid();
    }

    /**
     * Get outstanding balance amount
     */
    public java.math.BigDecimal getOutstandingBalance() {
        if (currentFeeRecord == null) {
            return java.math.BigDecimal.ZERO;
        }
        return currentFeeRecord.getOutstandingBalance();
    }

    /**
     * Get payment status
     */
    public String getPaymentStatus() {
        if (currentFeeRecord == null) {
            return "NO_FEES";
        }
        return currentFeeRecord.getPaymentStatus();
    }

    /**
     * Withdraw student from school
     */
    public void withdraw(String reason) {
        this.isActive = false;
        this.withdrawalDate = LocalDate.now();
        this.withdrawalReason = reason;
    }

    /**
     * Re-activate student
     */
    public void reactivate() {
        this.isActive = true;
        this.withdrawalDate = null;
        this.withdrawalReason = null;
    }
}
