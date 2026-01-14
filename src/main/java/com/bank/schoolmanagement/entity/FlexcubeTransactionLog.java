package com.bank.schoolmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Flexcube Transaction Log Entity
 * 
 * Logs all requests sent to Flexcube and responses received
 * 
 * PURPOSE:
 * - Complete audit trail of Flexcube API interactions
 * - Troubleshooting failed transactions
 * - Reconciliation between school system and bank
 * - Performance monitoring (response times)
 * - Retry tracking for failed requests
 * 
 * RELATIONSHIP:
 * - @ManyToOne to Payment (nullable - request might fail before payment created)
 */
@Entity
@Table(name = "flexcube_transaction_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlexcubeTransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Link to payment (nullable)
     * May be null if request failed before payment creation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    // ========== REQUEST DATA (FCUBSPayment) ==========

    /**
     * Complete FCUBSPayment request as JSON
     * Stores entire request object for audit
     */
    @Column(name = "request_payload", columnDefinition = "TEXT", nullable = false)
    private String requestPayload;

    /** SOURCE from FCUBSPayment (e.g., "MOBILEBANKING", "COUNTER") */
    @Column(name = "source", length = 50)
    private String source;

    /** USERID from FCUBSPayment (e.g., "FLEXSWITCH") */
    @Column(name = "user_id", length = 50)
    private String userId;

    /** BRANCH from FCUBSPayment */
    @Column(name = "branch", length = 50)
    private String branch;

    /** TXNACC - Source account (parent's account to debit) */
    @Column(name = "txn_account", length = 100)
    private String txnAccount;

    /** OFFSETACC - Destination account (school account to credit) */
    @Column(name = "offset_account", length = 100)
    private String offsetAccount;

    /** TXNAMT - Transaction amount */
    @Column(name = "transaction_amount", precision = 12, scale = 2)
    private BigDecimal transactionAmount;

    /** TXNCCY - Transaction currency */
    @Column(name = "transaction_currency", length = 10)
    private String transactionCurrency;

    /** NARRATION from FCUBSPayment */
    @Column(name = "narration", length = 500)
    private String narration;

    // ========== RESPONSE DATA (RrnResponse / FCUBSPaymentResponse) ==========

    /**
     * Complete Flexcube response as JSON
     * Stores RrnResponse or FCUBSPaymentResponse
     */
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    /** response field from RrnResponse ("SUCCESS" or "ERROR") */
    @Column(name = "response_status", length = 50)
    private String responseStatus;

    /** message field from RrnResponse */
    @Column(name = "response_message", length = 500)
    private String responseMessage;

    /** TRN_REF_NO from FcubsRecordItem (Flexcube's transaction reference) */
    @Column(name = "flexcube_reference", length = 100)
    private String flexcubeReference;

    /** TRN_DT from FcubsRecordItem (Transaction date) */
    @Column(name = "transaction_date", length = 50)
    private String transactionDate;

    /** VALUE_DT from FcubsRecordItem (Value date) */
    @Column(name = "value_date", length = 50)
    private String valueDate;

    /** MAKER_ID from FcubsRecordItem */
    @Column(name = "maker_id", length = 50)
    private String makerId;

    /** CHECKER_ID from FcubsRecordItem */
    @Column(name = "checker_id", length = 50)
    private String checkerId;

    // ========== TIMING & PERFORMANCE ==========

    @CreationTimestamp
    @Column(name = "request_timestamp", nullable = false, updatable = false)
    private LocalDateTime requestTimestamp;

    @Column(name = "response_timestamp")
    private LocalDateTime responseTimestamp;

    /** Duration in milliseconds (response_timestamp - request_timestamp) */
    @Column(name = "duration_ms")
    private Long durationMs;

    // ========== STATUS TRACKING ==========

    /**
     * Overall status: PENDING, SUCCESS, ERROR, TIMEOUT, RETRY
     * Different from responseStatus which is Flexcube's response
     */
    @Column(name = "status", length = 50)
    private String status = "PENDING";

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    // ========== TRANSACTION IDENTIFIERS ==========

    /** Your system's transaction reference (from Payment.bankTransactionId) */
    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    /** Correlation ID for tracing across systems */
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    // ========== ADDITIONAL METADATA ==========

    /** Flexcube API endpoint URL */
    @Column(name = "endpoint_url", length = 200)
    private String endpointUrl;

    /** HTTP status code from API call */
    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    /** Number of retry attempts for this request */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /** IP address or host that made the request */
    @Column(name = "source_ip", length = 50)
    private String sourceIp;

    // ========== HELPER METHODS ==========

    /**
     * Mark request as successful
     */
    public void markSuccess(LocalDateTime responseTime) {
        this.status = "SUCCESS";
        this.responseTimestamp = responseTime;
        if (this.requestTimestamp != null) {
            this.durationMs = java.time.Duration.between(requestTimestamp, responseTime).toMillis();
        }
    }

    /**
     * Mark request as failed
     */
    public void markFailed(String errorMsg, LocalDateTime responseTime) {
        this.status = "ERROR";
        this.errorMessage = errorMsg;
        this.responseTimestamp = responseTime;
        if (this.requestTimestamp != null) {
            this.durationMs = java.time.Duration.between(requestTimestamp, responseTime).toMillis();
        }
    }

    /**
     * Mark request as timeout
     */
    public void markTimeout() {
        this.status = "TIMEOUT";
        this.responseTimestamp = LocalDateTime.now();
        if (this.requestTimestamp != null) {
            this.durationMs = java.time.Duration.between(requestTimestamp, responseTimestamp).toMillis();
        }
    }

    /**
     * Increment retry counter
     */
    public void incrementRetry() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
        this.status = "RETRY";
    }

    /**
     * Check if request was successful
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(status) && "SUCCESS".equalsIgnoreCase(responseStatus);
    }

    /**
     * Check if request can be retried
     */
    public boolean canRetry(int maxRetries) {
        return this.retryCount != null && this.retryCount < maxRetries && 
               ("ERROR".equals(status) || "TIMEOUT".equals(status));
    }
}
