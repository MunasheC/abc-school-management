package com.bank.schoolmanagement.repository;

import com.bank.schoolmanagement.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * SchoolRepository - Multi-Tenant School Management
 * 
 * PURPOSE: Manage schools in the bank's SaaS platform
 * - Schools are onboarded by bank relationship managers
 * - Each school is a separate tenant with isolated data
 * - Supports subscription tiers, capacity management, license tracking
 * 
 * LEARNING: This is the foundation of multi-tenancy
 * - Every school has unique schoolCode (tenant identifier)
 * - All student/guardian/fee queries will filter by school
 * - Prevents School A from seeing School B's data
 */
@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {

    /* ----------------------  TENANT LOOKUP  ------------------------- */

    /**
     * Count active schools
     */
    long countByIsActive(Boolean isActive);

    /**
     * Find school by unique code
     * 
     * CRITICAL: Primary method for tenant identification
     * - School code is like a tenant ID
     * - Used in authentication/authorization
     * - Must be unique across platform
     * 
     * Example: findBySchoolCode("SCH001")
     */
    Optional<School> findBySchoolCode(String schoolCode);

    /**
     * Check if school code exists
     * 
     * Use case: Validate uniqueness during onboarding
     * - Prevent duplicate school codes
     * - Called before creating new school
     */
    boolean existsBySchoolCode(String schoolCode);

    /* ----------------------  ACTIVE SCHOOLS  ------------------------- */

    /**
     * Get all active schools
     * 
     * Use case: Bank admin dashboard
     * - Show only operational schools
     * - Exclude deactivated/suspended schools
     */
    List<School> findByIsActiveTrue();

    /**
     * Get inactive schools
     * 
     * Use case: Review suspended schools
     * - Check deactivation reasons
     * - Consider reactivation
     */
    List<School> findByIsActiveFalse();

    /* ----------------------  SUBSCRIPTION MANAGEMENT  ------------------------- */

    /**
     * Find schools by subscription tier
     * 
     * Use case: Tier-based analytics
     * - Count FREE vs BASIC vs PREMIUM schools
     * - Target schools for upgrades
     * - Enforce tier-specific features
     * 
     * Example: findBySubscriptionTier("PREMIUM")
     */
    List<School> findBySubscriptionTier(String subscriptionTier);

    /**
     * Find schools at capacity
     * 
     * Use case: Capacity management
     * - Alert schools reaching student limit
     * - Suggest subscription upgrades
     * - Prevent over-enrollment
     * 
     * Example: Find schools where currentStudentCount >= maxStudents
     */
    @Query("SELECT s FROM School s WHERE s.currentStudentCount >= s.maxStudents AND s.isActive = true")
    List<School> findSchoolsAtCapacity();

    /**
     * Find schools near capacity (90% full)
     * 
     * Use case: Proactive capacity planning
     * - Warn schools before hitting limit
     * - Give time to upgrade subscription
     */
    @Query("SELECT s FROM School s WHERE (s.currentStudentCount * 1.0 / s.maxStudents) >= 0.9 AND s.isActive = true")
    List<School> findSchoolsNearCapacity();

    /* ----------------------  LICENSE TRACKING  ------------------------- */

    /**
     * Find schools with expired licenses
     * 
     * Use case: Compliance enforcement
     * - Automatically disable expired schools
     * - Send renewal reminders
     * - Block access until renewed
     */
    @Query("SELECT s FROM School s WHERE s.licenseExpiryDate < :today AND s.isActive = true")
    List<School> findSchoolsWithExpiredLicenses(@Param("today") LocalDate today);

    /**
     * Find schools with licenses expiring soon
     * 
     * Use case: Proactive renewal reminders
     * - Send 30/60/90 day warnings
     * - Give time to process renewals
     * 
     * Example: Find licenses expiring within next 30 days
     */
    @Query("SELECT s FROM School s WHERE s.licenseExpiryDate BETWEEN :startDate AND :endDate AND s.isActive = true")
    List<School> findSchoolsWithLicensesExpiringSoon(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /* ----------------------  RELATIONSHIP MANAGER  ------------------------- */

    /**
     * Find schools managed by specific relationship manager
     * 
     * Use case: Bank employee dashboard
     * - Each relationship manager sees their schools
     * - Track portfolio performance
     * - Personalized school support
     * 
     * Example: findByRelationshipManager("John Doe")
     */
    List<School> findByRelationshipManager(String relationshipManager);

    /**
     * Count schools per relationship manager
     * 
     * Use case: Workload balancing
     * - Ensure even distribution
     * - Identify overloaded managers
     */
    @Query("SELECT s.relationshipManager, COUNT(s) FROM School s WHERE s.isActive = true GROUP BY s.relationshipManager")
    List<Object[]> countSchoolsPerRelationshipManager();

    /* ----------------------  SEARCH & FILTER  ------------------------- */

    /**
     * Search schools by name (case-insensitive, partial match)
     * 
     * Use case: Bank admin search
     * - Quick school lookup
     * - Filter dropdown in dashboards
     * 
     * Example: findBySchoolNameContainingIgnoreCase("High")
     * Matches: "Highlands High School", "Highfield Primary"
     */
    List<School> findBySchoolNameContainingIgnoreCase(String schoolName);

    /**
     * Find schools by type
     * 
     * Use case: Type-specific analytics
     * - Compare primary vs secondary schools
     * - Different fee structures per type
     * - Targeted marketing campaigns
     * 
     * Example: findBySchoolType("SECONDARY")
     */
    List<School> findBySchoolType(String schoolType);

    /**
     * Find schools by location (city/province)
     * 
     * Use case: Geographic analytics
     * - Regional performance comparison
     * - Localized support/training
     * - Branch-specific reporting
     */
    List<School> findByCity(String city);
    List<School> findByProvince(String province);

    /* ----------------------  REGISTRATION & COMPLIANCE  ------------------------- */

    /**
     * Find school by ministry registration number
     * 
     * Use case: Compliance verification
     * - Validate government registration
     * - Cross-reference official records
     */
    Optional<School> findByMinistryRegistrationNumber(String registrationNumber);

    /**
     * Find school by ZIMSEC center number
     * 
     * Use case: Exam center validation
     * - Verify secondary schools
     * - Link to national exam system
     */
    Optional<School> findByZimsecCenterNumber(String zimsecNumber);

    /* ----------------------  ANALYTICS QUERIES  ------------------------- */

    /**
     * Get total student count across all active schools
     * 
     * Use case: Platform-wide statistics
     * - Total users on platform
     * - Growth tracking over time
     */
    @Query("SELECT SUM(s.currentStudentCount) FROM School s WHERE s.isActive = true")
    Long getTotalStudentCountAcrossPlatform();

    /**
     * Count active schools
     * 
     * Use case: Key platform metric
     * - Track school growth
     * - Compare to target goals
     */
    @Query("SELECT COUNT(s) FROM School s WHERE s.isActive = true")
    Long countActiveSchools();

    /**
     * Get schools onboarded in date range
     * 
     * Use case: Growth analysis
     * - Monthly/quarterly onboarding reports
     * - Track acquisition trends
     * 
     * Example: Schools onboarded in January 2025
     */
    @Query("SELECT s FROM School s WHERE s.onboardingDate BETWEEN :startDate AND :endDate")
    List<School> findSchoolsOnboardedBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get schools by onboarding date (most recent first)
     * 
     * Use case: Recent schools dashboard
     * - Prioritize new schools for support
     * - Track onboarding success
     */
    List<School> findTop10ByIsActiveTrueOrderByOnboardingDateDesc();

    /* ----------------------  BANKING INTEGRATION  ------------------------- */

    /**
     * Find school by bank account number
     * 
     * Use case: Payment reconciliation
     * - Link transactions to schools
     * - Verify banking relationships
     */
    Optional<School> findByBankAccountNumber(String accountNumber);

    /**
     * Find schools with missing bank accounts
     * 
     * Use case: Onboarding completion tracking
     * - Follow up on incomplete registrations
     * - Block payment features until account added
     */
    @Query("SELECT s FROM School s WHERE s.bankAccountNumber IS NULL OR s.bankAccountNumber = ''")
    List<School> findSchoolsWithoutBankAccount();
}
