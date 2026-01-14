package com.bank.schoolmanagement.service;

import com.bank.schoolmanagement.entity.FlexcubeTransactionLog;
import com.bank.schoolmanagement.entity.Payment;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.repository.FlexcubeTransactionLogRepository;
import com.bank.schoolmanagement.utils.FCUBSPayment;
import com.bank.schoolmanagement.utils.RrnResponse;
import com.bank.schoolmanagement.utils.FcubsRecordItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Flexcube Integration Service
 * 
 * Handles integration with Oracle FLEXCUBE banking system for processing payments.
 * 
 * ACCOUNT STRUCTURE:
 * - Source Account (TXNACC): Parent's account - debited
 * - Destination Account (OFFSETACC): School's collection account - credited
 * 
 * EXAMPLE FLOW:
 * 1. Parent initiates payment via mobile banking
 * 2. System builds Flexcube request with parent account as source (TXNACC)
 * 3. School collection account as destination (OFFSETACC)
 * 4. Flexcube debits parent account, credits school account
 * 5. Transaction confirmation sent back to system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlexcubeIntegrationService {
    
    private final FlexcubeTransactionLogRepository logRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Configuration values (should be in application.properties)
    @Value("${flexcube.default.source:MOBILEBANKING}")
    private String defaultSource;
    
    @Value("${flexcube.default.userid:FLEXSWITCH}")
    private String defaultUserId;
    
    @Value("${flexcube.default.branch:120}")
    private String defaultBranch;
    
    @Value("${flexcube.default.product:ABDS}")
    private String defaultProduct;
    
    @Value("${flexcube.default.currency:USD}")
    private String defaultCurrency;
    
    @Value("${flexcube.school.collection.account:58520134002019}")
    private String schoolCollectionAccount;
    
    /**
     * Build Flexcube payment request for school fee payment
     * 
     * @param payment Payment entity containing payment details (including currency)
     * @param parentAccountNumber Parent's bank account (source account to debit)
     * @param amount Transaction amount
     * @return FCUBSPayment request ready to send to Flexcube
     */
    public FCUBSPayment buildSchoolFeePaymentRequest(
            Payment payment, 
            String parentAccountNumber,
            BigDecimal amount) {
        
        log.info("Building Flexcube payment request for payment ID: {}, amount: {}, currency: {}", 
                payment.getId(), amount, payment.getCurrency());
        
        Student student = payment.getStudent();
        School school = payment.getSchool();
        String currency = payment.getCurrency() != null ? payment.getCurrency() : defaultCurrency;
        
        // Build narration in format: CURRENCY|BRANCH|ACCOUNT_REFERENCE_DETAILS
        String narration = buildNarration(
            currency,
            defaultBranch,
            parentAccountNumber,
            student.getStudentId(),
            payment.getBankTransactionId()
        );
        
        // Build the Flexcube request
        FCUBSPayment fcubsPayment = FCUBSPayment.builder()
            // System identifiers
            .SOURCE(defaultSource)                  // e.g., "MOBILEBANKING", "COUNTER"
            .USERID(defaultUserId)                  // e.g., "FLEXSWITCH"
            .BRANCH(defaultBranch)                  // e.g., "120"
            .PROD(defaultProduct)                   // e.g., "ABDS"
            .BRN(defaultBranch)                     // e.g., "120"
            
            // SOURCE ACCOUNT (Debit) - Parent's account
            .TXNCCY(currency)                       // Transaction currency: "USD" or "ZWG"
            .TXNBRN(defaultBranch)                  // Transaction branch: "120"
            .TXNACC(parentAccountNumber)            // Source account to DEBIT (parent's account)
            
            // DESTINATION ACCOUNT (Credit) - School's collection account
            .OFFSETCCY(currency)                    // Offset currency: "USD" or "ZWG"
            .OFFSETBRN(defaultBranch)               // Offset branch: "120"
            .OFFSETACC(schoolCollectionAccount)     // Destination account to CREDIT (school account)
            
            // Transaction details
            .TXNAMT(amount)                         // Amount to transfer
            .NARRATION(narration)                   // Transaction description
            .TXNDATE(getCurrentDate())              // Transaction date
            
            // Additional metadata
            .ACCOUNT_NUMBER(parentAccountNumber)
            .BENEF_NAME(school.getSchoolName())
            .BRANCH_CODE(defaultBranch)
            .CUSTOMERS_BANK_NAME("BancABC")
            .MSGID(payment.getBankTransactionId())
            .CORRELID(payment.getPaymentReference())
            .build();
        
        log.debug("Flexcube payment request built: {}", fcubsPayment);
        
        return fcubsPayment;
    }
    
    /**
     * Build Flexcube payment request with custom accounts
     * 
     * Use this method when you need to specify custom source and destination accounts
     * 
     * @param sourceAccount Account to debit (parent's account)
     * @param destinationAccount Account to credit (school's account)
     * @param amount Transaction amount
     * @param narration Transaction description
     * @param sourceBranch Source account branch code
     * @param destinationBranch Destination account branch code
     * @param currency Transaction currency
     * @return FCUBSPayment request
     */
    public FCUBSPayment buildCustomPaymentRequest(
            String sourceAccount,
            Object destinationAccount,
            BigDecimal amount,
            String narration,
            String sourceBranch,
            String destinationBranch,
            String currency) {
        
        log.info("Building custom Flexcube payment: {} from {} to {}", 
                amount, sourceAccount, destinationAccount);
        
        return FCUBSPayment.builder()
            // System identifiers
            .SOURCE(defaultSource)
            .USERID(defaultUserId)
            .BRANCH(defaultBranch)
            .PROD(defaultProduct)
            .BRN(defaultBranch)
            
            // SOURCE ACCOUNT (Debit)
            .TXNCCY(currency)
            .TXNBRN(sourceBranch)
            .TXNACC(sourceAccount)              // Account to DEBIT
            
            // DESTINATION ACCOUNT (Credit)
            .OFFSETCCY(currency)
            .OFFSETBRN(destinationBranch)
            .OFFSETACC(destinationAccount)       // Account to CREDIT
            
            // Transaction details
            .TXNAMT(amount)
            .NARRATION(narration)
            .TXNDATE(getCurrentDate())
            .build();
    }
    
    /**
     * Build narration string for Flexcube transaction
     * 
     * Format: CURRENCY|BRANCH|ACCOUNT_STUDENTID_TRANSACTIONID
     * Example: USD|120|52289804360572_10_840632018707E63
     * 
     * @param currency Transaction currency
     * @param branch Branch code
     * @param accountNumber Parent account number
     * @param studentId Student ID
     * @param transactionId Bank transaction ID
     * @return Formatted narration string
     */
    private String buildNarration(
            String currency,
            String branch,
            String accountNumber,
            String studentId,
            String transactionId) {
        
        // Format: USD|120|ACCOUNT_STUDENTID_TRANSACTIONID
        return String.format("%s|%s|%s_%s_%s",
            currency,
            branch,
            accountNumber,
            studentId,
            transactionId != null ? transactionId : "PENDING"
        );
    }
    
    /**
     * Get current date in Flexcube format
     * 
     * @return Formatted date string
     */
    private String getCurrentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDateTime.now().format(formatter);
    }
    
    /**
     * Validate Flexcube payment request before sending
     * 
     * @param payment FCUBSPayment request to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validatePaymentRequest(FCUBSPayment payment) {
        log.debug("Validating Flexcube payment request");
        
        if (payment.getTXNACC() == null || payment.getTXNACC().isBlank()) {
            throw new IllegalArgumentException("Source account (TXNACC) is required");
        }
        
        if (payment.getOFFSETACC() == null) {
            throw new IllegalArgumentException("Destination account (OFFSETACC) is required");
        }
        
        if (payment.getTXNAMT() == null || payment.getTXNAMT().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero");
        }
        
        if (payment.getTXNCCY() == null || payment.getTXNCCY().isBlank()) {
            throw new IllegalArgumentException("Transaction currency (TXNCCY) is required");
        }
        
        if (payment.getOFFSETCCY() == null || payment.getOFFSETCCY().isBlank()) {
            throw new IllegalArgumentException("Offset currency (OFFSETCCY) is required");
        }
        
        if (!payment.getTXNCCY().equals(payment.getOFFSETCCY())) {
            log.warn("Currency mismatch: TXNCCY={} vs OFFSETCCY={}", 
                    payment.getTXNCCY(), payment.getOFFSETCCY());
        }
        
        log.debug("Flexcube payment request validation passed");
    }
    
    /**
     * Example: Build payment request from scratch
     * 
     * This demonstrates the exact structure you provided:
     * {
     *   "SOURCE": "MOBILEBANKING",
     *   "USERID": "FLEXSWITCH",
     *   "BRANCH": "120",
     *   "PROD": "ABDS",
     *   "BRN": "120",
     *   "TXNCCY": "USD",
     *   "TXNBRN": "120",
     *   "TXNACC": "L13111000",              // Source (debit)
     *   "OFFSETCCY": "USD",
     *   "OFFSETBRN": "120",
     *   "OFFSETACC": 58520134002019,        // Destination (credit)
     *   "TXNAMT": 100000,
     *   "NARRATION": "USD|120|52289804360572_10_840632018707E63"
     * }
     */
    public FCUBSPayment buildExampleRequest() {
        return FCUBSPayment.builder()
            .SOURCE("MOBILEBANKING")
            .USERID("FLEXSWITCH")
            .BRANCH("120")
            .PROD("ABDS")
            .BRN("120")
            .TXNCCY("USD")
            .TXNBRN("120")
            .TXNACC("L13111000")                    // Source account (parent's account to debit)
            .OFFSETCCY("USD")
            .OFFSETBRN("120")
            .OFFSETACC(58520134002019L)             // Destination account (school account to credit)
            .TXNAMT(new BigDecimal("100000"))
            .NARRATION("USD|120|52289804360572_10_840632018707E63")
            .build();
    }
    
    /**
     * Process Flexcube response and update Payment entity
     * 
     * Extracts relevant data from Flexcube response and stores it in Payment
     * 
     * @param payment Payment entity to update
     * @param flexcubeResponse RrnResponse from Flexcube API
     */
    public void processFlexcubeResponse(Payment payment, RrnResponse flexcubeResponse) {
        log.info("Processing Flexcube response for payment: {}", payment.getPaymentReference());
        
        if (flexcubeResponse == null || !"SUCCESS".equalsIgnoreCase(flexcubeResponse.getResponse())) {
            log.warn("Flexcube response is null or not successful");
            return;
        }
        
        try {
            // Extract transaction reference from first record
            if (flexcubeResponse.getValue() != null && 
                flexcubeResponse.getValue().getFcubs_record() != null &&
                !flexcubeResponse.getValue().getFcubs_record().isEmpty()) {
                
                FcubsRecordItem firstRecord = flexcubeResponse.getValue().getFcubs_record().get(0);
                
                // Set Flexcube reference
                payment.setFlexcubeReference(firstRecord.getTRN_REF_NO());
                
                // Set value date
                if (firstRecord.getVALUE_DT() != null) {
                    try {
                        LocalDate valueDate = LocalDate.parse(firstRecord.getVALUE_DT());
                        payment.setFlexcubeValueDate(valueDate);
                    } catch (Exception e) {
                        log.warn("Failed to parse Flexcube value date: {}", firstRecord.getVALUE_DT());
                    }
                }
                
                // Set currency
                if (firstRecord.getAC_CCY() != null) {
                    payment.setCurrency(firstRecord.getAC_CCY());
                }
                
                log.debug("Flexcube details set: reference={}, valueDate={}, currency={}", 
                         payment.getFlexcubeReference(), 
                         payment.getFlexcubeValueDate(),
                         payment.getCurrency());
            }
            
            // Store complete response as JSON
            String responseJson = objectMapper.writeValueAsString(flexcubeResponse);
            payment.setFlexcubeResponseJson(responseJson);
            
            log.info("Flexcube response processed successfully for payment: {}", 
                    payment.getPaymentReference());
            
        } catch (Exception e) {
            log.error("Error processing Flexcube response", e);
            // Don't throw - partial data is acceptable
        }
    }
    
    /**
     * Log Flexcube request
     * Creates a transaction log entry before sending request to Flexcube
     * 
     * @param fcubsPayment The payment request to log
     * @param transactionRef Your system's transaction reference
     * @return FlexcubeTransactionLog entity (unsaved)
     */
    public FlexcubeTransactionLog logRequest(FCUBSPayment fcubsPayment, String transactionRef) {
        log.debug("Logging Flexcube request: {}", transactionRef);
        
        try {
            FlexcubeTransactionLog logEntry = new FlexcubeTransactionLog();
            
            // Store complete request as JSON
            String requestJson = objectMapper.writeValueAsString(fcubsPayment);
            logEntry.setRequestPayload(requestJson);
            
            // Extract key fields from FCUBSPayment for easy querying
            logEntry.setSource(fcubsPayment.getSOURCE());
            logEntry.setUserId(fcubsPayment.getUSERID());
            logEntry.setBranch(fcubsPayment.getBRANCH());
            logEntry.setTxnAccount(fcubsPayment.getTXNACC());
            logEntry.setOffsetAccount(fcubsPayment.getOFFSETACC() != null ? 
                                     fcubsPayment.getOFFSETACC().toString() : null);
            logEntry.setTransactionAmount(fcubsPayment.getTXNAMT());
            logEntry.setTransactionCurrency(fcubsPayment.getTXNCCY());
            logEntry.setNarration(fcubsPayment.getNARRATION());
            
            // Set transaction reference
            logEntry.setTransactionReference(transactionRef);
            logEntry.setCorrelationId(fcubsPayment.getCORRELID());
            
            // Set status as PENDING
            logEntry.setStatus("PENDING");
            
            // Save to database
            return logRepository.save(logEntry);
            
        } catch (Exception e) {
            log.error("Error logging Flexcube request", e);
            // Return null if logging fails - don't block transaction
            return null;
        }
    }
    
    /**
     * Log Flexcube response
     * Updates existing log entry with response data
     * 
     * @param logEntry The log entry created during request
     * @param response Flexcube response (RrnResponse)
     * @param httpStatusCode HTTP status code from API call
     */
    public void logResponse(FlexcubeTransactionLog logEntry, RrnResponse response, Integer httpStatusCode) {
        if (logEntry == null) {
            log.warn("Cannot log response - log entry is null");
            return;
        }
        
        try {
            LocalDateTime responseTime = LocalDateTime.now();
            
            // Store complete response as JSON
            String responseJson = objectMapper.writeValueAsString(response);
            logEntry.setResponsePayload(responseJson);
            
            // Extract response fields
            logEntry.setResponseStatus(response.getResponse());
            logEntry.setResponseMessage(response.getMessage());
            logEntry.setHttpStatusCode(httpStatusCode);
            
            // Extract fields from fcubs_record if available
            if (response.getValue() != null && 
                response.getValue().getFcubs_record() != null &&
                !response.getValue().getFcubs_record().isEmpty()) {
                
                FcubsRecordItem firstRecord = response.getValue().getFcubs_record().get(0);
                logEntry.setFlexcubeReference(firstRecord.getTRN_REF_NO());
                logEntry.setTransactionDate(firstRecord.getTRN_DT());
                logEntry.setValueDate(firstRecord.getVALUE_DT());
                logEntry.setMakerId(firstRecord.getMAKER_ID());
                logEntry.setCheckerId(firstRecord.getCHECKER_ID());
            }
            
            // Mark as success or error based on response
            if ("SUCCESS".equalsIgnoreCase(response.getResponse())) {
                logEntry.markSuccess(responseTime);
            } else {
                logEntry.markFailed(response.getMessage(), responseTime);
                logEntry.setErrorCode("FLEXCUBE_ERROR");
            }
            
            // Save updated log
            logRepository.save(logEntry);
            
            log.info("Flexcube response logged: status={}, duration={}ms", 
                    logEntry.getStatus(), logEntry.getDurationMs());
            
        } catch (Exception e) {
            log.error("Error logging Flexcube response", e);
        }
    }
    
    /**
     * Log error (when Flexcube call fails)
     * 
     * @param logEntry The log entry created during request
     * @param errorMessage Error message
     * @param errorCode Error code
     */
    public void logError(FlexcubeTransactionLog logEntry, String errorMessage, String errorCode) {
        if (logEntry == null) {
            log.warn("Cannot log error - log entry is null");
            return;
        }
        
        try {
            logEntry.markFailed(errorMessage, LocalDateTime.now());
            logEntry.setErrorCode(errorCode);
            logRepository.save(logEntry);
            
            log.warn("Flexcube error logged: code={}, message={}", errorCode, errorMessage);
            
        } catch (Exception e) {
            log.error("Error logging Flexcube error", e);
        }
    }
    
    /**
     * Log timeout
     * 
     * @param logEntry The log entry created during request
     */
    public void logTimeout(FlexcubeTransactionLog logEntry) {
        if (logEntry == null) {
            return;
        }
        
        try {
            logEntry.markTimeout();
            logEntry.setErrorMessage("Request timeout");
            logEntry.setErrorCode("TIMEOUT");
            logRepository.save(logEntry);
            
            log.warn("Flexcube timeout logged: duration={}ms", logEntry.getDurationMs());
            
        } catch (Exception e) {
            log.error("Error logging Flexcube timeout", e);
        }
    }
}
