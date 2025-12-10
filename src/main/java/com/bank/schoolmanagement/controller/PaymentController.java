package com.bank.schoolmanagement.controller;

import com.bank.schoolmanagement.entity.Payment;
import com.bank.schoolmanagement.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Payment Controller - REST API for Payment Transactions
 * 
 * BASE PATH: /api/payments
 * 
 * LEARNING: Separate controller for payment operations
 * - Records payment transactions with audit trail
 * - Supports multiple payment methods (cash, mobile money, bank transfer)
 * - Integrates with bank systems via transaction references
 * - Provides payment reversal for refunds/errors
 */
@RestController
@RequestMapping("/api/school/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * CREATE - Record a payment
     * 
     * POST /api/payments/student/{studentId}
     * 
     * Body example:
     * {
     *   "amount": 500.00,
     *   "paymentMethod": "MOBILE_MONEY",
     *   "transactionReference": "MM123456789",
     *   "receivedBy": "Bursar Name",
     *   "paymentNotes": "Tuition payment for Term 1",
     *   "status": "COMPLETED"
     * }
     * 
     * Payment Methods: CASH, MOBILE_MONEY, BANK_TRANSFER, CHEQUE, CARD
     * Status: COMPLETED, PENDING, REVERSED, FAILED
     */
    @PostMapping("/student/{studentId}")
    public ResponseEntity<Payment> recordPayment(
            @PathVariable Long studentId,
            @Valid @RequestBody Payment payment) {
        log.info("REST request to record payment of {} for student ID: {}", 
                 payment.getAmount(), studentId);
        
        try {
            Payment savedPayment = paymentService.recordPaymentForCurrentSchool(studentId, payment);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPayment);
        } catch (IllegalArgumentException e) {
            log.error("Error recording payment: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * READ - Get payment by ID
     * 
     * GET /api/payments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        log.info("REST request to get payment with ID: {}", id);
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Get payment by reference
     * 
     * GET /api/payments/reference/{reference}
     * 
     * Payment reference is auto-generated unique ID
     */
    @GetMapping("/reference/{reference}")
    public ResponseEntity<Payment> getPaymentByReference(@PathVariable String reference) {
        log.info("REST request to get payment with reference: {}", reference);
        return paymentService.getPaymentByReference(reference)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Get payment by transaction reference
     * 
     * GET /api/payments/transaction/{transactionReference}
     * 
     * LEARNING: Bank integration
     * Used to find payment by the bank's transaction ID
     */
    @GetMapping("/transaction/{transactionReference}")
    public ResponseEntity<Payment> getPaymentByTransactionReference(@PathVariable String transactionReference) {
        log.info("REST request to get payment with transaction reference: {}", transactionReference);
        return paymentService.getPaymentByTransactionReference(transactionReference)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Get all payments for a student
     * 
     * GET /api/payments/student/{studentId}
     * 
     * Returns payment history ordered by date (newest first)
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Payment>> getPaymentsByStudent(@PathVariable Long studentId) {
        log.info("REST request to get payments for student ID: {}", studentId);
        List<Payment> payments = paymentService.getPaymentsByStudent(studentId);
        return ResponseEntity.ok(payments);
    }

    /**
     * READ - Get active payments for a student
     * 
     * GET /api/payments/student/{studentId}/active
     * 
     * Returns only non-reversed, completed payments
     */
    @GetMapping("/student/{studentId}/active")
    public ResponseEntity<List<Payment>> getActivePaymentsByStudent(@PathVariable Long studentId) {
        log.info("REST request to get active payments for student ID: {}", studentId);
        List<Payment> payments = paymentService.getActivePaymentsByStudent(studentId);
        return ResponseEntity.ok(payments);
    }

    /**
     * READ - Get payments by method
     * 
     * GET /api/payments/method/{method}
     * 
     * Example: GET /api/payments/method/MOBILE_MONEY
     */
    @GetMapping("/method/{method}")
    public ResponseEntity<List<Payment>> getPaymentsByMethod(@PathVariable String method) {
        log.info("REST request to get payments by method: {}", method);
        List<Payment> payments = paymentService.getPaymentsByMethod(method);
        return ResponseEntity.ok(payments);
    }

    /**
     * READ - Get payments by status
     * 
     * GET /api/payments/status/{status}
     * 
     * Valid statuses: COMPLETED, PENDING, REVERSED, FAILED
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(@PathVariable String status) {
        log.info("REST request to get payments with status: {}", status);
        List<Payment> payments = paymentService.getPaymentsByStatus(status);
        return ResponseEntity.ok(payments);
    }

    /**
     * READ - Get reversed payments (audit trail)
     * 
     * GET /api/payments/reversed
     * 
     * Shows all payments that have been reversed/refunded
     */
    @GetMapping("/reversed")
    public ResponseEntity<List<Payment>> getReversedPayments() {
        log.info("REST request to get reversed payments");
        List<Payment> payments = paymentService.getReversedPayments();
        return ResponseEntity.ok(payments);
    }

    /**
     * READ - Get payments in date range
     * 
     * GET /api/payments/date-range?start=2025-01-01T00:00:00&end=2025-12-31T23:59:59
     * 
     * Date format: yyyy-MM-dd'T'HH:mm:ss
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<Payment>> getPaymentsBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.info("REST request to get payments between {} and {}", start, end);
        List<Payment> payments = paymentService.getPaymentsBetweenDates(start, end);
        return ResponseEntity.ok(payments);
    }

    /**
     * READ - Get recent payments
     * 
     * GET /api/payments/recent?since=2025-12-01T00:00:00
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Payment>> getRecentPayments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        log.info("REST request to get payments since: {}", since);
        List<Payment> payments = paymentService.getRecentPayments(since);
        return ResponseEntity.ok(payments);
    }

    /**
     * READ - Get today's payments
     * 
     * GET /api/payments/today
     */
    @GetMapping("/today")
    public ResponseEntity<List<Payment>> getTodaysPayments() {
        log.info("REST request to get today's payments");
        List<Payment> payments = paymentService.getTodaysPaymentsForCurrentSchool();
        return ResponseEntity.ok(payments);
    }

    /**
     * READ - Get this week's payments
     * 
     * GET /api/payments/this-week
     */
    @GetMapping("/this-week")
    public ResponseEntity<List<Payment>> getThisWeeksPayments() {
        log.info("REST request to get this week's payments");
        List<Payment> payments = paymentService.getThisWeeksPayments();
        return ResponseEntity.ok(payments);
    }

    /**
     * READ - Get payments received by specific bursar
     * 
     * GET /api/payments/received-by/{name}
     * 
     * Useful for tracking which bursar processed which payments
     */
    @GetMapping("/received-by/{name}")
    public ResponseEntity<List<Payment>> getPaymentsByReceivedBy(@PathVariable String name) {
        log.info("REST request to get payments received by: {}", name);
        List<Payment> payments = paymentService.getPaymentsByReceivedBy(name);
        return ResponseEntity.ok(payments);
    }

    /**
     * REVERSE - Reverse a payment
     * 
     * POST /api/payments/{id}/reverse
     * 
     * Body: { "reason": "Incorrect amount entered" }
     * 
     * LEARNING: Payment reversal
     * Used for refunds, incorrect amounts, payment errors
     * Doesn't delete payment (audit trail) - marks as reversed
     */
    @PostMapping("/{id}/reverse")
    public ResponseEntity<Payment> reversePayment(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        log.warn("REST request to reverse payment ID: {}", id);
        
        String reason = payload.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Payment reversed = paymentService.reversePayment(id, reason);
            return ResponseEntity.ok(reversed);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error reversing payment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * REVERSE - Reverse payment by reference
     * 
     * POST /api/payments/reference/{reference}/reverse
     * 
     * Body: { "reason": "Duplicate payment" }
     */
    @PostMapping("/reference/{reference}/reverse")
    public ResponseEntity<Payment> reversePaymentByReference(
            @PathVariable String reference,
            @RequestBody Map<String, String> payload) {
        log.warn("REST request to reverse payment with reference: {}", reference);
        
        String reason = payload.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Payment reversed = paymentService.reversePaymentByReference(reference, reason);
            return ResponseEntity.ok(reversed);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error reversing payment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /* ----------------------  FINANCIAL REPORTS  ------------------------- */

    /**
     * REPORT - Total payments by student
     * 
     * GET /api/payments/reports/student/{studentId}/total
     */
    @GetMapping("/reports/student/{studentId}/total")
    public ResponseEntity<BigDecimal> calculateTotalByStudent(@PathVariable Long studentId) {
        log.info("REST request to calculate total payments for student ID: {}", studentId);
        BigDecimal total = paymentService.calculateTotalByStudent(studentId);
        return ResponseEntity.ok(total);
    }

    /**
     * REPORT - Total by payment method
     * 
     * GET /api/payments/reports/method/{method}/total
     * 
     * Shows how much money collected via each payment channel
     */
    @GetMapping("/reports/method/{method}/total")
    public ResponseEntity<BigDecimal> calculateTotalByMethod(@PathVariable String method) {
        log.info("REST request to calculate total for payment method: {}", method);
        BigDecimal total = paymentService.calculateTotalByMethod(method);
        return ResponseEntity.ok(total);
    }

    /**
     * REPORT - Total in date range
     * 
     * GET /api/payments/reports/date-range/total?start=2025-01-01T00:00:00&end=2025-12-31T23:59:59
     */
    @GetMapping("/reports/date-range/total")
    public ResponseEntity<BigDecimal> calculateTotalInDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.info("REST request to calculate total between {} and {}", start, end);
        BigDecimal total = paymentService.calculateTotalInDateRange(start, end);
        return ResponseEntity.ok(total);
    }

    /**
     * REPORT - Count by method
     * 
     * GET /api/payments/reports/method/{method}/count
     */
    @GetMapping("/reports/method/{method}/count")
    public ResponseEntity<Long> countByMethod(@PathVariable String method) {
        log.info("REST request to count payments by method: {}", method);
        long count = paymentService.countByMethod(method);
        return ResponseEntity.ok(count);
    }

    /**
     * REPORT - Payment statistics by method
     * 
     * GET /api/payments/reports/statistics
     * 
     * LEARNING: Business intelligence
     * Returns comprehensive payment statistics:
     * - Count and total amount per payment method
     * - Grand totals across all methods
     * - Helps bank understand channel usage
     */
    @GetMapping("/reports/statistics")
    public ResponseEntity<PaymentService.PaymentStatistics> getPaymentStatistics() {
        log.info("REST request to get payment statistics");
        PaymentService.PaymentStatistics stats = paymentService.getPaymentStatisticsForCurrentSchool();
        return ResponseEntity.ok(stats);
    }

    /**
     * Health check
     * 
     * GET /api/payments/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Payment API is running! 💳");
    }
}
