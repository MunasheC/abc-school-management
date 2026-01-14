package com.bank.schoolmanagement.controller;

import com.bank.schoolmanagement.dto.StudentLookupResponse;
import com.bank.schoolmanagement.dto.StudentSearchRequest;
import com.bank.schoolmanagement.entity.Payment;
import com.bank.schoolmanagement.dto.PaymentResponse;
import com.bank.schoolmanagement.dto.SchoolResponse;
import com.bank.schoolmanagement.service.BankPaymentService;
import com.bank.schoolmanagement.service.BankPaymentService.BankPaymentRequest;
import com.bank.schoolmanagement.service.BankPaymentService.DigitalBankingPaymentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Bank Payment Controller - REST API for bank teller operations
 * 
 * LEARNING: Bank-side payment interface (Option A & C)
 * - Used by bank tellers at branch counters
 * - Used by mobile/internet banking systems
 * - Separate from school controller (different users)
 * 
 * ENDPOINTS:
 * - GET /api/bank/schools - List all onboarded schools
 * - POST /api/bank/lookup - Lookup student by reference
 * - POST /api/bank/payment/counter - Record teller payment
 * - POST /api/bank/payment/digital - Record digital payment
 * - GET /api/bank/reconciliation/today - Today's bank payments
 * - GET /api/bank/reconciliation/school/{id} - School-specific payments
 */
@RestController
@RequestMapping("/api/bank")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // TODO: Configure proper CORS for production
@Tag(name = "Bank Payment Operations", description = "Endpoints for bank tellers and digital banking systems to process school fee payments")
public class BankPaymentController {

    private final BankPaymentService bankPaymentService;

    /**
     * GET /api/bank/schools
     * 
     * Get all onboarded schools
     * Used by: Bank teller interface school dropdown
     * 
     * Response: List of schools with codes and names
     */
    @Operation(
        summary = "Get all onboarded schools",
        description = "Returns a list of all schools registered in the system. Used by bank teller interfaces for school selection dropdowns."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of schools",
                     content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = SchoolResponse.class)))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/schools")
    public ResponseEntity<List<SchoolResponse>> getAllSchools() {
        log.info("GET /api/bank/schools - Fetching all onboarded schools");

        List<com.bank.schoolmanagement.entity.School> schools = bankPaymentService.getAllOnboardedSchools();

        List<SchoolResponse> dtos = schools.stream().map(SchoolResponse::fromEntity).toList();

        log.info("Found {} onboarded schools", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    /**
     * POST /api/bank/lookup
     * 
     * Lookup student by reference number
     * Used by: Bank teller before processing payment
     * 
     * Request body:
     * {
     *   "studentReference": "SCH001-STU-001"
     * }
     * 
     * Response: Student details with outstanding balance
     */
    @Operation(
        summary = "Lookup student by reference code",
        description = "Retrieves student details and fee information using their unique reference code (format: SCHOOLCODE-STUDENTID). " +
                     "Returns outstanding balance, payment status, and school information."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Student found successfully", 
                    content = @Content(schema = @Schema(implementation = StudentLookupResponse.class))),
        @ApiResponse(responseCode = "404", description = "Student not found with given reference"),
        @ApiResponse(responseCode = "400", description = "Invalid reference format")
    })
    @PostMapping("/lookup")
    public ResponseEntity<?> lookupStudent(
            @Parameter(description = "Student reference request containing the student reference code", required = true)
            @RequestBody StudentLookupRequest request) {
        log.info("POST /api/bank/lookup - Looking up student: {}", request.getStudentReference());
        
        try {
            StudentLookupResponse result = bankPaymentService.lookupStudentByReference(
                request.getStudentReference());
            
            log.info("Student lookup successful: {}", result.getStudentName());
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.error("Student lookup failed: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * POST /api/bank/search
     * 
     * Search students by name, school, and grade
     * Used by: Bank teller when parent doesn't have reference code
     * 
     * Real-world scenario:
     * - Parent: "I want to pay for my child John Doe, Grade 9 at Churchill High School"
     * - Teller searches by these natural identifiers
     * - Multiple matches possible (common names)
     * 
     * Request body:
     * {
     *   "studentFirstName": "Alice",
     *   "studentLastName": "Moyo",
     *   "schoolName": "Churchill High School",
     *   "grade": "GRADE_9"
     * }
     * 
     * Response: Array of matching students (0, 1, or multiple)
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchStudent(@RequestBody StudentSearchRequest request) {
        log.info("POST /api/bank/search - Searching student: {} at {} in {}", 
                 request.getStudentName(), request.getSchoolName(), request.getGrade());
        
        try {
            List<StudentLookupResponse> results = bankPaymentService.searchStudentsByName(request);
            
            if (results.isEmpty()) {
                log.warn("No students found matching search criteria");
                String searchCriteria = "";
                if (request.getStudentName() != null) searchCriteria += "name: " + request.getStudentName() + " ";
                if (request.getSchoolName() != null) searchCriteria += "school: " + request.getSchoolName() + " ";
                if (request.getGrade() != null) searchCriteria += "grade: " + request.getGrade();
                
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No students found matching: " + searchCriteria.trim()));
            }
            
            log.info("Student search successful: {} match(es) found", results.size());
            return ResponseEntity.ok(results);
            
        } catch (IllegalArgumentException e) {
            log.error("Student search failed: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/bank/payment/counter
     * 
     * Record payment at bank teller counter (Option A)
     * Used by: Bank teller after parent pays at counter
     * 
     * IMPORTANT: year and term are REQUIRED to specify which fee record to pay
     * - Students may have multiple fee records (one per term)
     * - Payment must be applied to the correct term
     * - Currency must be specified (USD or ZWG) and must match fee record currency
     * 
     * Request body:
     * {
     *   "studentReference": "SCH001-STU-001",
     *   "year": 2026,
     *   "term": 1,
     *   "currency": "USD",
     *   "amount": 200.00,
     *   "bankBranch": "Branch 005 - Harare Central",
     *   "tellerName": "John Ncube",
     *   "parentAccountNumber": "1234567890",
     *   "bankTransactionId": "BNK-TXN-789456",
     *   "paymentNotes": "Parent payment at branch - Term 1 fees"
     * }
     * 
     * Response: Payment confirmation with receipt details
     */
    @PostMapping("/payment/counter")
    public ResponseEntity<?> recordBankCounterPayment(
            @RequestBody BankPaymentRequest request) {
        
        log.info("POST /api/bank/payment/counter - Recording payment: {} for student {}", 
                 request.getAmount(), request.getStudentReference());
        
        try {
            // Validate request
            validateBankPaymentRequest(request);
            
            // Record payment
            Payment payment = bankPaymentService.recordBankCounterPayment(request);
            
            // Create response
            PaymentResponse response = PaymentResponse.fromEntity(payment);
            
            log.info("Bank counter payment recorded successfully: {}", 
                     payment.getPaymentReference());
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Payment recording failed: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error recording payment", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Payment processing failed: " + e.getMessage()));
        }
    }

    /**
     * POST /api/bank/payment/digital
     * 
     * Record payment via digital banking (Option C)
     * Used by: Mobile banking app, internet banking, USSD system
     * 
     * Request body:
     * {
     *   "studentReference": "SCH001-STU-001",
     *   "amount": 200.00,
     *   "paymentMethod": "MOBILE_BANKING",
     *   "parentAccountNumber": "1234567890",
     *   "bankTransactionId": "MBK-202512-98765"
     * }
     * 
     * Response: Payment confirmation
     */
    @PostMapping("/payment/digital")
    public ResponseEntity<?> recordDigitalBankingPayment(
            @RequestBody DigitalBankingPaymentRequest request) {
        
        log.info("POST /api/bank/payment/digital - Recording {} payment: {} for student {}", 
                 request.getPaymentMethod(), request.getAmount(), request.getStudentReference());
        
        try {
            // Validate request
            validateDigitalPaymentRequest(request);
            
            // Record payment
            Payment payment = bankPaymentService.recordDigitalBankingPayment(request);
            
            // Create response
            PaymentResponse response = PaymentResponse.fromEntity(payment);
            
            log.info("Digital banking payment recorded successfully: {}", 
                     payment.getPaymentReference());
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Digital payment recording failed: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error recording digital payment", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Payment processing failed: " + e.getMessage()));
        }
    }

    /**
     * GET /api/bank/reconciliation/today
     * 
     * Get all bank payments processed today
     * Used by: Bank operations for end-of-day reconciliation
     * 
     * Response: List of all bank payments with totals
     */
    @GetMapping("/reconciliation/today")
    public ResponseEntity<ReconciliationResponse> getTodaysReconciliation() {
        log.info("GET /api/bank/reconciliation/today - Fetching today's bank payments");
        
        List<Payment> payments = bankPaymentService.getTodaysBankPayments();
        BigDecimal total = bankPaymentService.calculateTodaysBankPaymentsTotal();
        
        ReconciliationResponse response = new ReconciliationResponse();
        response.setPayments(payments.stream().map(PaymentResponse::fromEntity).toList());
        response.setTotalAmount(total);
        response.setPaymentCount(payments.size());
        response.setReconciliationDate(java.time.LocalDate.now());
        
        log.info("Today's bank payments: {} transactions, total: {}", payments.size(), total);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/bank/reconciliation/school/{schoolId}
     * 
     * Get today's bank payments for specific school
     * Used by: School reconciliation, bank reporting
     * 
     * Response: School-specific payments
     */
    @GetMapping("/reconciliation/school/{schoolId}")
    public ResponseEntity<ReconciliationResponse> getSchoolReconciliation(
            @PathVariable Long schoolId) {
        
        log.info("GET /api/bank/reconciliation/school/{} - Fetching school's bank payments", 
                 schoolId);
        
        try {
            List<Payment> payments = bankPaymentService.getTodaysBankPaymentsForSchool(schoolId);
            
            BigDecimal total = payments.stream()
                    .filter(p -> p.getStatus().equals("COMPLETED") && !p.getIsReversed())
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            ReconciliationResponse response = new ReconciliationResponse();
            response.setPayments(payments.stream().map(PaymentResponse::fromEntity).toList());
            response.setTotalAmount(total);
            response.setPaymentCount(payments.size());
            response.setReconciliationDate(java.time.LocalDate.now());
            
            log.info("School {} bank payments: {} transactions, total: {}", 
                     schoolId, payments.size(), total);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("School reconciliation failed: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .build();
        }
    }

    // ========== VALIDATION HELPERS ==========

    private void validateBankPaymentRequest(BankPaymentRequest request) {
        if (request.getStudentReference() == null || request.getStudentReference().isBlank()) {
            throw new IllegalArgumentException("Student reference is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (request.getBankTransactionId() == null || request.getBankTransactionId().isBlank()) {
            throw new IllegalArgumentException("Bank transaction ID is required");
        }
        if (request.getTellerName() == null || request.getTellerName().isBlank()) {
            throw new IllegalArgumentException("Teller name is required");
        }
    }

    private void validateDigitalPaymentRequest(DigitalBankingPaymentRequest request) {
        if (request.getStudentReference() == null || request.getStudentReference().isBlank()) {
            throw new IllegalArgumentException("Student reference is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (request.getBankTransactionId() == null || request.getBankTransactionId().isBlank()) {
            throw new IllegalArgumentException("Bank transaction ID is required");
        }
        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (!request.getPaymentMethod().isBankChannel()) {
            throw new IllegalArgumentException("Invalid payment method for digital banking");
        }
    }

    // ========== DTOs ==========

    @lombok.Data
    public static class StudentLookupRequest {
        private String studentReference;
    }

    // PaymentResponse DTO is provided in `com.bank.schoolmanagement.dto.PaymentResponse`

    @lombok.Data
    public static class ReconciliationResponse {
        private List<PaymentResponse> payments;
        private BigDecimal totalAmount;
        private int paymentCount;
        private java.time.LocalDate reconciliationDate;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String error;
    }
}
