package com.bank.schoolmanagement.repository;

import com.bank.schoolmanagement.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Guardian Repository - Data access for guardians (parents)
 * 
 * LEARNING: Repository pattern for Guardian entity
 * - Provides database operations for parent/guardian records
 * - Can find guardians by phone (useful for bank integration)
 * - Can find siblings through shared guardian
 */
@Repository
public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    /**
     * Find guardian by primary phone number
     * 
     * LEARNING: Primary phone is unique, so this returns Optional (0 or 1)
     * Used for: "Check if this parent already exists before creating"
     */
    Optional<Guardian> findByPrimaryPhone(String primaryPhone);

    /**
     * Find guardian by email
     */
    Optional<Guardian> findByEmail(String email);

    /**
     * Check if guardian exists by phone
     * Useful for validation before creating
     */
    boolean existsByPrimaryPhone(String primaryPhone);

    /**
     * Search guardians by name
     */
    @Query("SELECT g FROM Guardian g WHERE LOWER(g.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Guardian> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Find active guardians
     */
    List<Guardian> findByIsActiveTrue();

    /**
     * Find guardians with multiple children (siblings)
     * 
     * LEARNING: Custom query using SIZE function
     * SIZE(g.students) counts how many students are linked to each guardian
     */
    @Query("SELECT g FROM Guardian g WHERE SIZE(g.students) > 1")
    List<Guardian> findGuardiansWithMultipleChildren();

    /**
     * Find guardians by occupation
     */
    List<Guardian> findByOccupation(String occupation);

    /**
     * Count guardians by occupation
     */
    long countByOccupation(String occupation);

    /* ----------------------  MULTI-TENANT QUERIES (School-Aware)  ------------------------- */

    /**
     * Find all guardians for a specific school
     * 
     * CRITICAL: Multi-tenant data isolation
     * - Returns only guardians from specified school
     * - School users see only their guardians
     */
    List<Guardian> findBySchool(com.bank.schoolmanagement.entity.School school);

    /**
     * Find guardian by school and primary phone
     * 
     * LEARNING: Phone numbers are unique per school, not globally
     * - School A can have guardian with "+263771234567"
     * - School B can have different guardian with same number
     * - Must always query with school context
     */
    Optional<Guardian> findBySchoolAndPrimaryPhone(
        com.bank.schoolmanagement.entity.School school,
        String primaryPhone
    );

    /**
     * Find guardian by school and email
     */
    Optional<Guardian> findBySchoolAndEmail(
        com.bank.schoolmanagement.entity.School school,
        String email
    );

    /**
     * Check if guardian exists by school and phone
     * 
     * Used before creating new guardian to prevent duplicates within school
     */
    boolean existsBySchoolAndPrimaryPhone(
        com.bank.schoolmanagement.entity.School school,
        String primaryPhone
    );

    /**
     * Search guardians by name within a school
     */
    @Query("SELECT g FROM Guardian g WHERE g.school = :school AND " +
           "LOWER(g.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Guardian> searchBySchoolAndName(
        @Param("school") com.bank.schoolmanagement.entity.School school,
        @Param("searchTerm") String searchTerm
    );

    /**
     * Find active guardians in a school
     */
    List<Guardian> findBySchoolAndIsActiveTrue(com.bank.schoolmanagement.entity.School school);

    /**
     * Find guardians with multiple children in a school
     * 
     * Used for: Sibling discount calculations
     */
    @Query("SELECT g FROM Guardian g WHERE g.school = :school AND SIZE(g.students) > 1")
    List<Guardian> findGuardiansWithMultipleChildrenBySchool(
        @Param("school") com.bank.schoolmanagement.entity.School school
    );

    /**
     * Count guardians for a school
     * 
     * Used for: School statistics
     */
    long countBySchool(com.bank.schoolmanagement.entity.School school);

    /**
     * Find guardians by school and occupation
     * 
     * Used for: Demographic analysis per school
     */
    List<Guardian> findBySchoolAndOccupation(
        com.bank.schoolmanagement.entity.School school,
        String occupation
    );
}
