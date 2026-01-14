package com.bank.schoolmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * School Entity - Multi-Tenant Support
 * 
 * LEARNING: Multi-tenancy pattern
 * 
 * This system is a PLATFORM (SaaS) where:
 * - The bank operates the platform
 * - Multiple schools are onboarded as tenants
 * - Each school's data is isolated
 * - Schools can be activated/deactivated
 * 
 * BENEFITS:
 * 1. Single deployment serves multiple schools
 * 2. Bank can onboard schools as part of banking relationship
 * 3. Centralized management and updates
 * 4. Each school has separate branding/configuration
 * 5. Easy billing per school
 */
@Entity
@Table(name = "schools")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique school code/identifier
     * Example: "CHHS001", "STMARY002"
     */
    @NotBlank(message = "School code is required")
    @Column(name = "school_code", unique = true, nullable = false)
    private String schoolCode;

    /* ----------------------  SCHOOL INFORMATION  ------------------------- */

    @NotBlank(message = "School name is required")
    @Size(min = 3, max = 200)
    @Column(name = "school_name", nullable = false)
    private String schoolName;

    @NotBlank(message = "School type is required (PRIMARY, SECONDARY, COMBINED)")
    @Pattern(regexp = "PRIMARY|SECONDARY|COMBINED", message = "Invalid school type")
    @Column(name = "school_type", nullable = false)
    private String schoolType;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "province")
    private String province;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    private String country = "Zimbabwe";

    /* ----------------------  CONTACT INFORMATION  ------------------------- */

    @NotBlank(message = "Primary contact phone is required")
    @Column(name = "primary_phone", nullable = false)
    private String primaryPhone;

    @Column(name = "secondary_phone")
    private String secondaryPhone;

    @Email(message = "Invalid email format")
    @Column(name = "email")
    private String email;

    @Column(name = "website")
    private String website;

    /* ----------------------  ADMINISTRATION  ------------------------- */

    @Column(name = "head_teacher_name")
    private String headTeacherName;

    @Column(name = "bursar_name")
    private String bursarName;

    @Column(name = "bursar_phone")
    private String bursarPhone;

    @Column(name = "bursar_email")
    private String bursarEmail;

    /* ----------------------  REGISTRATION & LICENSING  ------------------------- */

    @NotBlank(message = "Ministry registration number is required")
    @Column(name = "ministry_registration_number", nullable = false)
    private String ministryRegistrationNumber;
    
    @Column(name = "license_expiry_date")
    private java.time.LocalDate licenseExpiryDate;

    @Column(name = "zimsec_center_number")
    private String zimsecCenterNumber;  // For schools offering O/A Level

    /* ----------------------  BANKING INFORMATION  ------------------------- */
    @NotBlank(message = "Bank account number is required")
    @Column(name = "bank_account_number", nullable = false)
    private String bankAccountNumber;

    @Column(name = "bank_branch")
    private String bankBranch;

    @Column(name = "bank_account_name")
    private String bankAccountName;

    /**
     * Bank relationship manager assigned to this school
     */
    @Column(name = "relationship_manager")
    private String relationshipManager;

    @Column(name = "relationship_manager_phone")
    private String relationshipManagerPhone;

    /* ----------------------  SUBSCRIPTION & STATUS  ------------------------- */

    @Column(name = "subscription_tier")
    private String subscriptionTier;  // FREE, BASIC, PREMIUM

    @Column(name = "onboarding_date")
    private java.time.LocalDate onboardingDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deactivation_date")
    private LocalDateTime deactivationDate;

    @Column(name = "deactivation_reason", length = 500)
    private String deactivationReason;

    /* ----------------------  CONFIGURATION  ------------------------- */

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color")
    private String primaryColor;  // Branding color for school

    @Column(name = "max_students")
    private Integer maxStudents;  // License limit

    @Column(name = "current_student_count")
    private Integer currentStudentCount = 0;

    /* ----------------------  NOTES & METADATA  ------------------------- */

    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* ----------------------  RELATIONSHIPS  ------------------------- */

    /**
     * One school has many students
     * 
     * LEARNING: Multi-tenancy relationship
     * - Every student belongs to exactly one school
     * - Queries always filter by school to ensure data isolation
     * 
     * @JsonIgnore prevents circular reference when serializing Student objects
     * - Student has School reference
     * - School has Students collection
     * - Without @JsonIgnore, Jackson would try to serialize infinitely
     * - Also prevents LazyInitializationException when session is closed
     */
    @JsonIgnore
    @OneToMany(mappedBy = "school", cascade = CascadeType.ALL)
    private List<Student> students = new ArrayList<>();

    /* ----------------------  HELPER METHODS  ------------------------- */

    /**
     * Check if school has reached student capacity
     */
    public boolean isAtCapacity() {
        return maxStudents != null && currentStudentCount >= maxStudents;
    }

    /**
     * Check if school license is expired
     */
    public boolean isLicenseExpired() {
        return licenseExpiryDate != null && 
               licenseExpiryDate.isBefore(java.time.LocalDate.now());
    }

    /**
     * Deactivate school with reason
     */
    public void deactivate(String reason) {
        this.isActive = false;
        this.deactivationDate = LocalDateTime.now();
        this.deactivationReason = reason;
    }

    /**
     * Reactivate school
     */
    public void reactivate() {
        this.isActive = true;
        this.deactivationDate = null;
        this.deactivationReason = null;
    }

    /**
     * Increment student count
     */
    public void incrementStudentCount() {
        if (currentStudentCount == null) {
            currentStudentCount = 0;
        }
        currentStudentCount++;
    }

    /**
     * Decrement student count
     */
    public void decrementStudentCount() {
        if (currentStudentCount != null && currentStudentCount > 0) {
            currentStudentCount--;
        }
    }
}
