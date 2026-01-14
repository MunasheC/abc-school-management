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
 * Guardian Entity - Parent/Guardian Information
 * 
 * LEARNING: Separate entity for guardians because:
 * 1. Multiple students (siblings) can share the same parent
 * 2. Parent contact info changes independently of student info
 * 3. Can track which parent is paying for multiple children
 * 4. Bank integration: One parent account can pay for all their children
 * 
 * RELATIONSHIP: One Guardian can have Many Students (siblings)
 */
@Entity
@Table(name = "guardians",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_school_guardian_phone",
            columnNames = {"school_id", "primary_phone"}
        ),
        @UniqueConstraint(
            name = "uk_school_guardian_national_id",
            columnNames = {"school_id", "national_id"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "National ID is required")
    @Column(name = "national_id", nullable = false)
    private String nationalId;

    @NotBlank(message = "Guardian name is required")
    @Size(max = 100)
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    @Column(name = "primary_phone", nullable = false)
    private String primaryPhone;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    @Column(name = "secondary_phone")
    private String secondaryPhone;

    @Email(message = "Invalid email address")
    @Column(name = "email")
    private String email;

    @NotBlank(message = "Address is required")
    @Column(name = "address", length = 500, nullable = false)
    private String address;

    @NotBlank(message = "Occupation is required")
    @Column(name = "occupation", nullable = false)
    private String occupation;

    @NotBlank(message = "Employer is required")
    @Column(name = "employer", nullable = false)
    private String employer;

    /* ----------------------  BANKING INFORMATION  ------------------------- */

    /**
     * Guardian's bank account number for payment processing
     * Used for automatic fee payments and reconciliation
     */
    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    /**
     * Name of the bank where guardian holds account
     * Example: "FBC Bank", "CBZ Bank"
     */
    @Column(name = "bank_name", length = 100)
    private String bankName;

    /**
     * Bank branch where account is held
     * Example: "Harare Main Branch", "Bulawayo Branch"
     */
    @Column(name = "bank_branch", length = 100)
    private String bankBranch;

    /* ----------------------  RELATIONSHIPS  ------------------------- */

    /**
     * Relationship to School (Multi-Tenancy)
     * 
     * @ManyToOne - Many guardians belong to one school
     * 
     * LEARNING: Multi-tenant data isolation
     * - Guardians are scoped to a school
     * - School A cannot see School B's guardians
     * - Guardian phone numbers only need to be unique within a school
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    /**
     * Relationship to students
     * 
     * @OneToMany - One guardian has many students (children)
     * mappedBy = "guardian" - The Student entity has a field called "guardian"
     * cascade = CascadeType.ALL - If guardian deleted, what happens to students?
     * orphanRemoval = false - Don't delete students if guardian removed
     * 
     * @JsonIgnore prevents circular reference when serializing Student objects
     * - Student has Guardian reference
     * - Guardian has Students collection
     * - Without @JsonIgnore, Jackson would try to serialize infinitely
     */
    @JsonIgnore
    @OneToMany(mappedBy = "guardian", cascade = CascadeType.PERSIST)
    private List<Student> students = new ArrayList<>();

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Helper method to add a student to this guardian
     * Also sets the bidirectional relationship
     */
    public void addStudent(Student student) {
        students.add(student);
        student.setGuardian(this);
    }

    /**
     * Get count of children (students) under this guardian
     */
    public int getChildrenCount() {
        return students.size();
    }
}
