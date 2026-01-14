package com.bank.schoolmanagement.repository;

import com.bank.schoolmanagement.entity.FlexcubeTransactionLog;
import com.bank.schoolmanagement.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Flexcube Transaction Logs
 */
@Repository
public interface FlexcubeTransactionLogRepository extends JpaRepository<FlexcubeTransactionLog, Long> {

    /**
     * Find log by payment
     */
    Optional<FlexcubeTransactionLog> findByPayment(Payment payment);

    /**
     * Find logs by status
     */
    List<FlexcubeTransactionLog> findByStatus(String status);

    /**
     * Find failed transactions
     */
    List<FlexcubeTransactionLog> findByStatusIn(List<String> statuses);

    /**
     * Find by Flexcube reference
     */
    Optional<FlexcubeTransactionLog> findByFlexcubeReference(String flexcubeReference);

    /**
     * Find by transaction reference
     */
    Optional<FlexcubeTransactionLog> findByTransactionReference(String transactionReference);

    /**
     * Find logs within date range
     */
    List<FlexcubeTransactionLog> findByRequestTimestampBetween(
        LocalDateTime startDate, 
        LocalDateTime endDate
    );

    /**
     * Find failed transactions that can be retried
     */
    @Query("SELECT f FROM FlexcubeTransactionLog f WHERE f.status IN ('ERROR', 'TIMEOUT') " +
           "AND (f.retryCount IS NULL OR f.retryCount < :maxRetries)")
    List<FlexcubeTransactionLog> findRetryableTransactions(@Param("maxRetries") int maxRetries);

    /**
     * Find slow transactions (duration > threshold)
     */
    @Query("SELECT f FROM FlexcubeTransactionLog f WHERE f.durationMs > :thresholdMs ORDER BY f.durationMs DESC")
    List<FlexcubeTransactionLog> findSlowTransactions(@Param("thresholdMs") long thresholdMs);

    /**
     * Count by status
     */
    long countByStatus(String status);

    /**
     * Count successful transactions today
     */
    @Query("SELECT COUNT(f) FROM FlexcubeTransactionLog f WHERE f.status = 'SUCCESS' " +
           "AND f.requestTimestamp >= :startOfDay")
    long countSuccessfulTransactionsToday(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Get average response time for successful transactions
     */
    @Query("SELECT AVG(f.durationMs) FROM FlexcubeTransactionLog f WHERE f.status = 'SUCCESS' " +
           "AND f.requestTimestamp >= :startDate")
    Double getAverageResponseTime(@Param("startDate") LocalDateTime startDate);
}
