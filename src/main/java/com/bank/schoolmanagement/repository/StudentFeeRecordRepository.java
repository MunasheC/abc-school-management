package com.bank.schoolmanagement.repository;

import com.bank.schoolmanagement.entity.StudentFeeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;


/**
 * StudentFeeRecord Repository - Financial data access
 * 
 * LEARNING: Repository for fee records
 * - Query by payment status (PAID, PARTIALLY_PAID, ARREARS)
 * - Calculate financial totals across all students
 * - Find records by term/year
 */
@Repository
public interface StudentFeeRecordRepository extends JpaRepository<StudentFeeRecord, Long> {

    /**
     * Find ALL fee records for a student by database ID (OneToMany relationship)
     * Returns all historical fee records for the student
     */
    List<StudentFeeRecord> findByStudentId(Long studentId);

    /**
     * Find ALL fee records by student's studentId field (e.g., STU1733838975353)
     * Returns all historical fee records for the student
     * WARNING: NOT school-aware - use findBySchoolAndStudent_StudentId instead
     * Spring generates: SELECT * FROM student_fee_records WHERE student_id IN 
     *                   (SELECT id FROM students WHERE student_id = ?)
     */
    List<StudentFeeRecord> findByStudent_StudentId(String studentId);
    
    /**
     * Find ALL fee records by school and student's studentId field (SCHOOL-AWARE)
     * Returns all historical fee records for the student within the specified school
     * 
     * MULTI-TENANT SAFE: Filters by both school and studentId
     * - Prevents School A from seeing School B's student records
     * - Student ID "2025001" in School A ≠ "2025001" in School B
     */
    List<StudentFeeRecord> findBySchoolAndStudent_StudentId(
        com.bank.schoolmanagement.entity.School school,
        String studentId
    );
    
    /**
     * Find fee records by student ID ordered by creation date (newest first)
     * Useful for getting latest fee record
     */
    List<StudentFeeRecord> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    /**
     * Find fee records by year and term
     * Example: year=2024, term=1
     */
    List<StudentFeeRecord> findByYearAndTerm(Integer year, Integer term);

    /**
     * Find fee records by payment status
     */
    List<StudentFeeRecord> findByPaymentStatusAndIsActiveTrue(String paymentStatus);

    /**
     * Find fee records by fee category
     */
    List<StudentFeeRecord> findByFeeCategory(String feeCategory);

    /**
     * Find records with scholarships
     */
    List<StudentFeeRecord> findByHasScholarshipTrue();

    /**
     * Find records with outstanding balance
     */
    @Query("SELECT f FROM StudentFeeRecord f WHERE f.outstandingBalance > 0 AND f.isActive = true ORDER BY f.outstandingBalance DESC")
    List<StudentFeeRecord> findRecordsWithOutstandingBalance();

    /**
     * Find fully paid records
     */
    @Query("SELECT f FROM StudentFeeRecord f WHERE f.outstandingBalance <= 0 AND f.isActive = true")
    List<StudentFeeRecord> findFullyPaidRecords();

    /**
     * Calculate total outstanding fees across all active records
     * 
     * LEARNING: Aggregate function - returns single number
     * COALESCE returns 0 if SUM is null (no records)
     */
    @Query("SELECT COALESCE(SUM(f.outstandingBalance), 0) FROM StudentFeeRecord f WHERE f.isActive = true")
    BigDecimal calculateTotalOutstandingFees();

    /**
     * Calculate total collected fees (amount paid)
     */
    @Query("SELECT COALESCE(SUM(f.amountPaid), 0) FROM StudentFeeRecord f WHERE f.isActive = true")
    BigDecimal calculateTotalCollectedFees();

    /**
     * Calculate total gross amount billed
     */
    @Query("SELECT COALESCE(SUM(f.grossAmount), 0) FROM StudentFeeRecord f WHERE f.isActive = true")
    BigDecimal calculateTotalGrossAmount();

    /**
     * Calculate total scholarships given
     */
    @Query("SELECT COALESCE(SUM(f.scholarshipAmount), 0) FROM StudentFeeRecord f WHERE f.isActive = true")
    BigDecimal calculateTotalScholarships();

    /**
     * Count records by payment status
     */
    long countByPaymentStatus(String paymentStatus);

    /**
     * Find records by fee category and payment status
     */
    List<StudentFeeRecord> findByFeeCategoryAndPaymentStatus(String feeCategory, String paymentStatus);

    /**
     * Find records by year, term, and payment status
     */
    List<StudentFeeRecord> findByYearAndTermAndPaymentStatus(Integer year, Integer term, String paymentStatus);

    /* ----------------------  MULTI-TENANT QUERIES (School-Aware)  ------------------------- */

    /**
     * Find all fee records for a specific school
     * 
     * CRITICAL: Multi-tenant financial data isolation
     * - Returns only fee records from specified school
     * - Essential for school-level financial reports
     */
    List<StudentFeeRecord> findBySchool(com.bank.schoolmanagement.entity.School school);

    /**
     * Find fee records by school, year, and term
     * 
     * Example: Get all Term 1 2025 fee records for School A
     */
    List<StudentFeeRecord> findBySchoolAndYearAndTerm(
        com.bank.schoolmanagement.entity.School school,
        Integer year,
        Integer term
    );

    /**
     * Find fee records by school and payment status
     * 
     * Use cases:
     * - Get all ARREARS records for a school
     * - Get all PAID records for reporting
     */
    List<StudentFeeRecord> findBySchoolAndPaymentStatus(
        com.bank.schoolmanagement.entity.School school,
        String paymentStatus
    );

    /**
     * Find fee records by school and fee category
     * 
     * Example: Get all "Boarder" fee records for School A
     */
    List<StudentFeeRecord> findBySchoolAndFeeCategory(
        com.bank.schoolmanagement.entity.School school,
        String feeCategory
    );

    /**
     * Find fee records by school, year, term, and payment status
     * 
     * Example: Get all ARREARS for Term 1 2025 in School A
     */
    List<StudentFeeRecord> findBySchoolAndYearAndTermAndPaymentStatus(
        com.bank.schoolmanagement.entity.School school,
        Integer year,
        Integer term,
        String paymentStatus
    );

    /**
     * Find records with outstanding balance for a school
     * 
     * CRITICAL: Financial reporting per school
     */
    @Query("SELECT f FROM StudentFeeRecord f WHERE f.school = :school AND " +
           "f.outstandingBalance > 0 AND f.isActive = true ORDER BY f.outstandingBalance DESC")
    List<StudentFeeRecord> findRecordsWithOutstandingBalanceBySchool(
        @Param("school") com.bank.schoolmanagement.entity.School school
    );

    /**
     * Find fully paid records for a school
     */
    @Query("SELECT f FROM StudentFeeRecord f WHERE f.school = :school AND " +
           "f.outstandingBalance <= 0 AND f.isActive = true")
    List<StudentFeeRecord> findFullyPaidRecordsBySchool(
        @Param("school") com.bank.schoolmanagement.entity.School school
    );

    /**
     * Find records with scholarships for a school
     */
    List<StudentFeeRecord> findBySchoolAndHasScholarshipTrue(
        com.bank.schoolmanagement.entity.School school
    );

    /**
     * Calculate total outstanding balance for a school
     * 
     * CRITICAL: School-level financial summary
     */
    @Query("SELECT COALESCE(SUM(f.outstandingBalance), 0) FROM StudentFeeRecord f " +
           "WHERE f.school = :school AND f.isActive = true")
    BigDecimal calculateTotalOutstandingBalanceBySchool(
        @Param("school") com.bank.schoolmanagement.entity.School school
    );

    /**
     * Calculate total amount paid for a school
     */
    @Query("SELECT COALESCE(SUM(f.amountPaid), 0) FROM StudentFeeRecord f " +
           "WHERE f.school = :school AND f.isActive = true")
    BigDecimal calculateTotalAmountPaidBySchool(
        @Param("school") com.bank.schoolmanagement.entity.School school
    );

    /**
     * Calculate total gross amount (total fees before payments) for a school
     */
    @Query("SELECT COALESCE(SUM(f.grossAmount), 0) FROM StudentFeeRecord f " +
           "WHERE f.school = :school AND f.isActive = true")
    BigDecimal calculateTotalGrossAmountBySchool(
        @Param("school") com.bank.schoolmanagement.entity.School school
    );

    /**
     * Calculate total scholarships given by a school
     */
    @Query("SELECT COALESCE(SUM(f.scholarshipAmount), 0) FROM StudentFeeRecord f " +
           "WHERE f.school = :school AND f.isActive = true")
    BigDecimal calculateTotalScholarshipsBySchool(
        @Param("school") com.bank.schoolmanagement.entity.School school
    );

    /**
     * Count fee records by school and payment status
     * 
     * Example: How many students in arrears in School A?
     */
    long countBySchoolAndPaymentStatus(
        com.bank.schoolmanagement.entity.School school,
        String paymentStatus
    );

    /**
     * Count total fee records for a school
     */
    long countBySchool(com.bank.schoolmanagement.entity.School school);
    
    /**
     * Find fee records for a student ordered by ID descending (most recent first)
     * 
     * USAGE: Get latest fee record for promotion workflow
     * - Most recent record is first in list
     * - Helps identify current term before creating new promotion record
     */
    List<StudentFeeRecord> findByStudentIdOrderByIdDesc(Long studentId);
}
