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
            name = "school_student",
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

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^[MF]$", message = "Gender must be either 'M' or 'F'")
    @Column(name = "gender", nullable = false)
    private String gender;  // M (Male) or F (Female)

    @NotBlank(message = "National ID is required")
    @Column(name = "national_id", nullable = false)
    private String nationalId;

    /* ----------------------  ACADEMIC INFORMATION  ------------------------- */

    @NotBlank(message = "Grade is required")
    @Column(name = "grade", nullable = false)
    private String grade;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "admission_number")
    private String admissionNumber;

    @NotBlank(message = "Class name is required")
    @Column(name = "class_name", nullable = false)
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
     * LEARNING: Uses guardian's national_id instead of database id
     * - More meaningful business key
     * - Guardian's national ID is unique per school
     * - Siblings share same guardian via national ID
     * 
     * LEARNING: fetch = FetchType.LAZY means:
     * - Guardian data not loaded automatically with student
     * - Only loaded when you call student.getGuardian()
     * - Improves performance for queries that don't need guardian info
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "guardian_national_id", referencedColumnName = "national_id")
    private Guardian guardian;

    /**
     * Relationship to Fee Records (UPDATED: Now supports multiple records)
     * 
     * @OneToMany - One student has many fee records (one per term/year)
     * mappedBy = "student" - StudentFeeRecord entity has a field called "student"
     * 
     * CHANGE: Previously @OneToOne, now @OneToMany
     * - Supports historical fee records
     * - Track fees across multiple terms/years
     * - Each promotion creates new fee record
     * 
     * NOTE: For backward compatibility, use getLatestFeeRecord() to get current term
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<StudentFeeRecord> feeRecords = new ArrayList<>();
    
    /**
     * Helper method to get the most recent fee record
     * @deprecated Use StudentFeeRecordService.getLatestFeeRecordForStudent() instead
     */
    @Deprecated
    public StudentFeeRecord getCurrentFeeRecord() {
        if (feeRecords == null || feeRecords.isEmpty()) {
            return null;
        }
        // Return the most recently created fee record
        return feeRecords.stream()
            .max((f1, f2) -> f1.getCreatedAt().compareTo(f2.getCreatedAt()))
            .orElse(null);
    }

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
    
    /**
     * Academic completion status
     * 
     * VALUES:
     * - null or "ACTIVE" - Currently enrolled
     * - "COMPLETED_PRIMARY" - Finished Grade 7 (primary school)
     * - "COMPLETED_O_LEVEL" - Finished Form 4 (O Level)
     * - "COMPLETED_A_LEVEL" - Finished Form 6 (A Level)
     * 
     * LEARNING: Track student's academic milestone completion
     * - Primary schools: Student completes at Grade 7
     * - Secondary schools: Student completes O Level at Form 4, A Level at Form 6
     * - Used to prevent further promotions after completion
     */
    @Column(name = "completion_status")
    private String completionStatus;

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
        StudentFeeRecord current = getCurrentFeeRecord();
        return current != null && !current.isFullyPaid();
    }

    /**
     * Get outstanding balance amount
     */
    public java.math.BigDecimal getOutstandingBalance() {
        StudentFeeRecord current = getCurrentFeeRecord();
        if (current == null) {
            return java.math.BigDecimal.ZERO;
        }
        return current.getOutstandingBalance();
    }

    /**
     * Get payment status
     */
    public String getPaymentStatus() {
        StudentFeeRecord current = getCurrentFeeRecord();
        if (current == null) {
            return "NO_FEE_RECORD";
        }
        return current.getPaymentStatus();
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
