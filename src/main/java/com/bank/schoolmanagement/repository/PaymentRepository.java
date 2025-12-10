package com.bank.schoolmanagement.repository;

import com.bank.schoolmanagement.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Payment Repository - Transaction history data access
 * 
 * LEARNING: Repository for payment transactions
 * - Track individual payments
 * - Find payments by student, method, date range
 * - Calculate payment totals
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by reference number
     */
    Optional<Payment> findByPaymentReference(String paymentReference);

    /**
     * Find all payments for a student
     * Ordered by most recent first
     */
    @Query("SELECT p FROM Payment p WHERE p.student.id = :studentId ORDER BY p.paymentDate DESC")
    List<Payment> findByStudentId(@Param("studentId") Long studentId);

    /**
     * Find payments for a student that are active (not reversed)
     */
    @Query("SELECT p FROM Payment p WHERE p.student.id = :studentId AND p.isReversed = false AND p.status = 'COMPLETED' ORDER BY p.paymentDate DESC")
    List<Payment> findActivePaymentsByStudentId(@Param("studentId") Long studentId);

    /**
     * Find payments by payment method
     */
    List<Payment> findByPaymentMethod(String paymentMethod);

    /**
     * Find payments by status
     */
    List<Payment> findByStatus(String status);

    /**
     * Find reversed payments
     */
    List<Payment> findByIsReversedTrue();

    /**
     * Find payments in date range
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<Payment> findPaymentsBetweenDates(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find payments between dates (alternate method name)
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<Payment> findByPaymentDateBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find payments by transaction reference
     * Useful for linking to bank transactions
     */
    Optional<Payment> findByTransactionReference(String transactionReference);

    /**
     * Calculate total payments for a student
     * Only count active (not reversed) payments
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.student.id = :studentId AND p.isReversed = false AND p.status = 'COMPLETED'")
    BigDecimal calculateTotalPaymentsByStudent(@Param("studentId") Long studentId);

    /**
     * Calculate total payments by method
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentMethod = :method AND p.isReversed = false AND p.status = 'COMPLETED'")
    BigDecimal calculateTotalByPaymentMethod(@Param("method") String method);

    /**
     * Calculate total payments in date range
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate AND p.isReversed = false AND p.status = 'COMPLETED'")
    BigDecimal calculateTotalInDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count payments by method
     */
    long countByPaymentMethod(String paymentMethod);

    /**
     * Find recent payments (last N days)
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentDate >= :since ORDER BY p.paymentDate DESC")
    List<Payment> findRecentPayments(@Param("since") LocalDateTime since);

    /**
     * Find payments received by specific bursar
     */
    List<Payment> findByReceivedBy(String receivedBy);

    /* ----------------------  MULTI-TENANT QUERIES (School-Aware)  ------------------------- */

    /**
     * Find all payments for a specific school
     * 
     * CRITICAL: Multi-tenant payment data isolation
     * - Returns only payments from specified school
     * - Essential for school-level financial reconciliation
     */
    List<Payment> findBySchool(com.bank.schoolmanagement.entity.School school);

    /**
     * Find payments by school and student
     * 
     * Get payment history for a specific student in a school
     */
    @Query("SELECT p FROM Payment p WHERE p.school = :school AND p.student.id = :studentId " +
           "ORDER BY p.paymentDate DESC")
    List<Payment> findBySchoolAndStudentId(
        @Param("school") com.bank.schoolmanagement.entity.School school,
        @Param("studentId") Long studentId
    );

    /**
     * Find active payments by school and student
     * 
     * Excludes reversed payments, only completed
     */
    @Query("SELECT p FROM Payment p WHERE p.school = :school AND p.student.id = :studentId AND " +
           "p.isReversed = false AND p.status = 'COMPLETED' ORDER BY p.paymentDate DESC")
    List<Payment> findActivePaymentsBySchoolAndStudentId(
        @Param("school") com.bank.schoolmanagement.entity.School school,
        @Param("studentId") Long studentId
    );

    /**
     * Find payments by school and payment method
     * 
     * Use cases:
     * - How much collected via MOBILE_MONEY in School A?
     * - Cash vs digital payment analysis per school
     */
    List<Payment> findBySchoolAndPaymentMethod(
        com.bank.schoolmanagement.entity.School school,
        String paymentMethod
    );

    /**
     * Find payments by school and status
     */
    List<Payment> findBySchoolAndStatus(
        com.bank.schoolmanagement.entity.School school,
        String status
    );

    /**
     * Find reversed payments for a school
     * 
     * Used for: Audit trail, refund tracking
     */
    List<Payment> findBySchoolAndIsReversedTrue(
        com.bank.schoolmanagement.entity.School school
    );

    /**
     * Find payments in date range for a school
     * 
     * CRITICAL: School-level daily/monthly financial reports
     */
    @Query("SELECT p FROM Payment p WHERE p.school = :school AND " +
           "p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<Payment> findBySchoolAndDateRange(
        @Param("school") com.bank.schoolmanagement.entity.School school,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Calculate total payments for a school
     * 
     * CRITICAL: School-level revenue tracking
     * Only counts completed, non-reversed payments
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.school = :school AND " +
           "p.isReversed = false AND p.status = 'COMPLETED'")
    BigDecimal calculateTotalPaymentsBySchool(
        @Param("school") com.bank.schoolmanagement.entity.School school
    );

    /**
     * Calculate total payments in date range for a school
     * 
     * Used for: Daily/monthly/term revenue reports
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.school = :school AND " +
           "p.paymentDate BETWEEN :startDate AND :endDate AND p.isReversed = false AND p.status = 'COMPLETED'")
    BigDecimal calculateTotalInDateRangeBySchool(
        @Param("school") com.bank.schoolmanagement.entity.School school,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Calculate total payments by payment method for a school
     * 
     * Example: How much collected via mobile money in School A?
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.school = :school AND " +
           "p.paymentMethod = :method AND p.isReversed = false AND p.status = 'COMPLETED'")
    BigDecimal calculateTotalBySchoolAndMethod(
        @Param("school") com.bank.schoolmanagement.entity.School school,
        @Param("method") String paymentMethod
    );

    /**
     * Count payments for a school
     */
    long countBySchool(com.bank.schoolmanagement.entity.School school);

    /**
     * Count payments by school and payment method
     * 
     * Example: How many mobile money transactions in School A?
     */
    long countBySchoolAndPaymentMethod(
        com.bank.schoolmanagement.entity.School school,
        String paymentMethod
    );

    /**
     * Find recent payments for a school (last N days)
     */
    @Query("SELECT p FROM Payment p WHERE p.school = :school AND " +
           "p.paymentDate >= :since ORDER BY p.paymentDate DESC")
    List<Payment> findRecentPaymentsBySchool(
        @Param("school") com.bank.schoolmanagement.entity.School school,
        @Param("since") LocalDateTime since
    );

    /**
     * Find payments received by specific bursar in a school
     * 
     * Used for: Bursar performance tracking, accountability
     */
    List<Payment> findBySchoolAndReceivedBy(
        com.bank.schoolmanagement.entity.School school,
        String receivedBy
    );

    /**
     * Find payment by school and payment reference
     * 
     * Used for: Payment lookup/verification within school context
     */
    Optional<Payment> findBySchoolAndPaymentReference(
        com.bank.schoolmanagement.entity.School school,
        String paymentReference
    );
}
