package com.bank.schoolmanagement.service;

import com.bank.schoolmanagement.dto.StudentLookupResponse;
import com.bank.schoolmanagement.dto.StudentSearchRequest;
import com.bank.schoolmanagement.entity.FlexcubeTransactionLog;
import com.bank.schoolmanagement.entity.Payment;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.entity.StudentFeeRecord;
import com.bank.schoolmanagement.enums.PaymentMethod;
import com.bank.schoolmanagement.repository.PaymentRepository;
import com.bank.schoolmanagement.repository.SchoolRepository;
import com.bank.schoolmanagement.repository.StudentFeeRecordRepository;
import com.bank.schoolmanagement.repository.StudentRepository;
import com.bank.schoolmanagement.utils.FCUBSPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bank Payment Service - For bank teller operations (Option A & C)
 * 
 * LEARNING: Bank-initiated payment processing
 * - Bank tellers can process payments for ANY school
 * - Parents pay at bank branch or via digital banking
 * - Real-time notification to schools
 * - Complete audit trail with bank transaction details
 * - Integration with Flexcube for actual fund transfers
 * 
 * KEY DIFFERENCE from PaymentService:
 * - PaymentService: School staff recording payments AT school
 * - BankPaymentService: Bank staff/system recording payments AT bank
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankPaymentService {

    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final StudentFeeRecordRepository feeRecordRepository;
    private final StudentFeeRecordService feeRecordService;
    private final SchoolRepository schoolRepository;
    private final AuditTrailService auditTrailService;
    private final FlexcubeIntegrationService flexcubeService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${flexcube.enabled:false}")
    private boolean flexcubeEnabled;
    
    @Value("${flexcube.api.url:}")
    private String flexcubeApiUrl;
    
    @Value("${flexcube.token.url:}")
    private String flexcubeTokenUrl;
    
    @Value("${flexcube.token.username:}")
    private String flexcubeTokenUsername;
    
    @Value("${flexcube.token.password:}")
    private String flexcubeTokenPassword;
    
    @Value("${flexcube.api.timeout:30000}")
    private int flexcubeTimeout;

    /**
     * Get all onboarded schools (for bank teller dropdown)
     * 
     * Used in: Bank teller interface school selection
     * Returns: All schools using the system
     */
    public List<School> getAllOnboardedSchools() {
        log.debug("Fetching all onboarded schools for bank teller");
        return schoolRepository.findByIsActiveTrue();
    }

    /**
     * Lookup student by reference number (Option A & C)
     * 
     * LEARNING: Student reference format: SCH001-STU-001
     * - SCH001: School code
     * - STU: Student indicator
     * - 001: Student sequence number
     * 
     * Process:
     * 1. Parse reference to extract school code
     * 2. Find school by code
     * 3. Set SchoolContext temporarily (for this lookup only)
     * 4. Find student in that school
     * 5. Return student with fee details
     * 
     * @param studentReference Format: "SCH001-STU-001"
     * @return StudentLookupResponse with student, school, and fee information
     */
    public StudentLookupResponse lookupStudentByReference(String studentReference) {
        log.info("Bank teller looking up student by reference: {}", studentReference);
        
        // Validate format
        if (studentReference == null || studentReference.isBlank()) {
            throw new IllegalArgumentException("Student reference cannot be empty");
        }
        
        // Parse reference: SCH001-STU-001 or just the student ID
        String schoolCode;
        String studentId;
        
        // Check if format includes school code (SCH001-STU-001)
        if (studentReference.contains("-")) {
            Pattern pattern = Pattern.compile("^([A-Z0-9]+)-(.+)$");
            Matcher matcher = pattern.matcher(studentReference);
            
            if (!matcher.matches()) {
                log.error("Invalid student reference format: {}", studentReference);
                throw new IllegalArgumentException(
                    "Invalid student reference format. Expected format: SCH001-STU001 or SCHOOLCODE-STUDENTID");
            }
            
            schoolCode = matcher.group(1);
            studentId = matcher.group(2);
            log.debug("Extracted school code: {} and student ID: {}", schoolCode, studentId);
        } else {
            // Just student ID provided, need to determine school from context
            // This requires the school to be set in the request or session
            throw new IllegalArgumentException(
                "Student reference must include school code. Expected format: SCHOOLCODE-STUDENTID");
        }
        
        // Find school by code
        School school = schoolRepository.findBySchoolCode(schoolCode)
                .orElseThrow(() -> {
                    log.error("School not found with code: {}", schoolCode);
                    return new IllegalArgumentException("School not found: " + schoolCode);
                });
        
        // Find student by student ID and school
        Student student = studentRepository.findBySchoolIdAndStudentId(school.getId(), studentId)
                .orElseThrow(() -> {
                    log.error("Student not found with ID: {} in school: {}", studentId, schoolCode);
                    return new IllegalArgumentException("Student not found: " + studentId + " in school " + schoolCode);
                });
        
        // Validate student belongs to the school
        if (!student.getSchool().getId().equals(school.getId())) {
            log.error("Student {} does not belong to school {}", studentReference, schoolCode);
            throw new IllegalArgumentException("Student does not belong to school " + schoolCode);
        }
        
        // Get ALL fee records for the student (not just latest)
        // FIXED: Must aggregate across all terms to show total outstanding balance
        List<StudentFeeRecord> allFeeRecords = feeRecordRepository.findByStudentId(student.getId());
        
        // Build DTO response (no circular references)
        StudentLookupResponse response = new StudentLookupResponse();
        response.setStudentId(student.getStudentId());
        response.setStudentName(student.getFirstName() + " " + student.getLastName());
        response.setGrade(student.getGrade());
        response.setClassName(student.getClassName());
        
        response.setSchoolCode(school.getSchoolCode());
        response.setSchoolName(school.getSchoolName());
        
        if (!allFeeRecords.isEmpty()) {
            // Calculate aggregated totals across ALL fee records
            BigDecimal totalNetAmount = BigDecimal.ZERO;
            BigDecimal totalAmountPaid = BigDecimal.ZERO;
            BigDecimal totalOutstanding = BigDecimal.ZERO;
            
            for (StudentFeeRecord record : allFeeRecords) {
                totalNetAmount = totalNetAmount.add(record.getNetAmount() != null ? record.getNetAmount() : BigDecimal.ZERO);
                totalAmountPaid = totalAmountPaid.add(record.getAmountPaid() != null ? record.getAmountPaid() : BigDecimal.ZERO);
                totalOutstanding = totalOutstanding.add(record.getOutstandingBalance() != null ? record.getOutstandingBalance() : BigDecimal.ZERO);
            }
            
            // Get latest record for fee category reference
            StudentFeeRecord latestRecord = allFeeRecords.stream()
                .max((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()))
                .orElse(allFeeRecords.get(0));
            
            response.setFeeCategory(latestRecord.getFeeCategory());
            response.setTotalFees(totalNetAmount);
            response.setAmountPaid(totalAmountPaid);
            response.setOutstandingBalance(totalOutstanding);
            
            // Determine overall payment status based on total outstanding
            if (totalOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
                response.setPaymentStatus("PAID");
            } else if (totalAmountPaid.compareTo(BigDecimal.ZERO) > 0) {
                response.setPaymentStatus("PARTIALLY_PAID");
            } else {
                response.setPaymentStatus("ARREARS");
            }
        } else {
            response.setFeeCategory("N/A");
            response.setTotalFees(BigDecimal.ZERO);
            response.setAmountPaid(BigDecimal.ZERO);
            response.setOutstandingBalance(BigDecimal.ZERO);
            response.setPaymentStatus("NO_RECORD");
        }
        
        log.info("Student lookup successful: {} from {}, Outstanding: {}", 
                 response.getStudentName(), school.getSchoolName(), response.getOutstandingBalance());
        
        return response;
    }
    
    /**
     * Internal method: Get student entities for payment processing with specific year/term
     * Used by payment methods that need actual entities to create Payment records
     * 
     * UPDATED: Now requires year and term to identify the correct fee record
     * 
     * @param studentReference Format: "SCH001-STU-001"
     * @param year Academic year (e.g., 2026)
     * @param term Term number (1, 2, or 3)
     * @return StudentLookupResult with entities
     * @throws IllegalArgumentException if year or term is null/invalid
     */
    private StudentLookupResult lookupStudentEntitiesInternal(String studentReference, Integer year, Integer term) {
        log.debug("Looking up student entities for payment: {} (Year: {}, Term: {})", 
                  studentReference, year, term);
        
        // Validate year and term
        if (year == null || year < 2020 || year > 2100) {
            throw new IllegalArgumentException("Year is required and must be between 2020-2100");
        }
        if (term == null || term < 1 || term > 3) {
            throw new IllegalArgumentException("Term is required and must be 1, 2, or 3");
        }
        log.debug("Looking up student entities for payment: {}", studentReference);
        
        // Validate format
        if (studentReference == null || studentReference.isBlank()) {
            throw new IllegalArgumentException("Student reference cannot be empty");
        }
        
        // Parse reference
        String schoolCode;
        String studentId;
        
        if (studentReference.contains("-")) {
            Pattern pattern = Pattern.compile("^([A-Z0-9]+)-(.+)$");
            Matcher matcher = pattern.matcher(studentReference);
            
            if (!matcher.matches()) {
                log.error("Invalid student reference format: {}", studentReference);
                throw new IllegalArgumentException(
                    "Invalid student reference format. Expected format: SCH001-STU001 or SCHOOLCODE-STUDENTID");
            }
            
            schoolCode = matcher.group(1);
            studentId = matcher.group(2);
        } else {
            throw new IllegalArgumentException("Reference must include school code: SCHOOLCODE-STUDENTID");
        }
        
        // Find school by code
        School school = schoolRepository.findBySchoolCode(schoolCode)
                .orElseThrow(() -> {
                    log.error("School not found with code: {}", schoolCode);
                    return new IllegalArgumentException("School not found: " + schoolCode);
                });
        
        // Find student by student ID and school
        Student student = studentRepository.findBySchoolIdAndStudentId(school.getId(), studentId)
                .orElseThrow(() -> {
                    log.error("Student not found with ID: {} in school: {}", studentId, schoolCode);
                    return new IllegalArgumentException("Student not found: " + studentId + " in school " + schoolCode);
                });
        
        // Validate student belongs to the school
        if (!student.getSchool().getId().equals(school.getId())) {
            log.error("Student {} does not belong to school {}", studentReference, schoolCode);
            throw new IllegalArgumentException("Student does not belong to school " + schoolCode);
        }
        
        // Find SPECIFIC fee record by year and term (not just latest)
        // CRITICAL: Student may have multiple fee records - must pay the correct one
        List<StudentFeeRecord> allRecords = feeRecordRepository.findByStudentId(student.getId());
        Optional<StudentFeeRecord> feeRecordOpt = allRecords.stream()
            .filter(record -> year.equals(record.getYear()) && term.equals(record.getTerm()))
            .findFirst();
        
        if (feeRecordOpt.isEmpty()) {
            log.error("No fee record found for student {} (Year: {}, Term: {})", 
                      studentReference, year, term);
            throw new IllegalArgumentException(
                String.format("No fee record found for student %s in Year %d, Term %d. " +
                             "Fee records must be created before accepting payments.", 
                             studentReference, year, term));
        }
        
        // Build entity result
        StudentLookupResult result = new StudentLookupResult();
        result.setStudent(student);
        result.setSchool(school);
        result.setFeeRecord(feeRecordOpt.orElse(null));
        
        if (feeRecordOpt.isPresent()) {
            StudentFeeRecord feeRecord = feeRecordOpt.get();
            result.setTotalFees(feeRecord.getNetAmount());
            result.setAmountPaid(feeRecord.getAmountPaid());
            result.setOutstandingBalance(feeRecord.getOutstandingBalance());
        } else {
            result.setTotalFees(BigDecimal.ZERO);
            result.setAmountPaid(BigDecimal.ZERO);
            result.setOutstandingBalance(BigDecimal.ZERO);
        }
        
        return result;
    }

    /**
     * Search students by name, school, and grade (for parents without reference codes)
     * 
     * LEARNING: Real-world scenario - many parents don't have reference codes
     * - They only know: child's name, school name, grade
     * - Bank teller needs to search by these natural identifiers
     * - Multiple matches possible (common names)
     * 
     * Process:
     * 1. Search schools by name (case-insensitive, partial match)
     * 2. For each matching school, search students by first name, last name, grade
     * 3. Get fee records for matched students
     * 4. Return list of all matches (could be 0, 1, or multiple)
     * 
     * @param request StudentSearchRequest with firstName, lastName, schoolName, grade
     * @return List of StudentLookupResponse - empty if no matches
     */
    public List<StudentLookupResponse> searchStudentsByName(StudentSearchRequest request) {
        log.info("Searching students - name: {}, school: {}, grade: {}", 
                 request.getStudentName(), request.getSchoolName(), request.getGrade());
        
        List<StudentLookupResponse> results = new ArrayList<>();
        
        // Step 1: Find schools (all if no schoolName provided)
        List<School> matchingSchools;
        if (request.getSchoolName() != null && !request.getSchoolName().isBlank()) {
            matchingSchools = schoolRepository.findBySchoolNameContainingIgnoreCase(request.getSchoolName());
            if (matchingSchools.isEmpty()) {
                log.warn("No schools found matching name: {}", request.getSchoolName());
                return results; // Empty list
            }
            log.debug("Found {} schools matching '{}'", matchingSchools.size(), request.getSchoolName());
        } else {
            // No school filter - search all schools
            matchingSchools = schoolRepository.findAll();
            log.debug("Searching across all {} schools", matchingSchools.size());
        }
        
        // Step 2: For each school, search for students matching criteria
        for (School school : matchingSchools) {
            // Get all students in this school (unpaged for search functionality)
            List<Student> schoolStudents = studentRepository.findBySchool(school, Pageable.unpaged()).getContent();
            
            // Filter by studentName if provided (searches both first and last name)
            if (request.getStudentName() != null && !request.getStudentName().isBlank()) {
                String name = request.getStudentName().toLowerCase();
                schoolStudents = schoolStudents.stream()
                    .filter(s -> s.getFirstName().toLowerCase().contains(name) 
                              || s.getLastName().toLowerCase().contains(name))
                    .toList();
            }
            
            // Filter by grade if provided
            if (request.getGrade() != null && !request.getGrade().isBlank()) {
                String grade = request.getGrade();
                schoolStudents = schoolStudents.stream()
                    .filter(s -> s.getGrade().equals(grade))
                    .toList();
            }
            
            List<Student> matchingStudents = schoolStudents;
            
            log.debug("Found {} students matching name in school {}", matchingStudents.size(), school.getSchoolName());
            
            // Step 3: Build response DTOs for each match
            for (Student student : matchingStudents) {
                StudentLookupResponse response = new StudentLookupResponse();
                response.setStudentId(student.getStudentId());
                response.setStudentName(student.getFirstName() + " " + student.getLastName());
                response.setGrade(student.getGrade());
                response.setClassName(student.getClassName());
                
                response.setSchoolCode(school.getSchoolCode());
                response.setSchoolName(school.getSchoolName());
                
                // Get latest fee record (optional)
                Optional<StudentFeeRecord> feeRecordOpt = feeRecordService.getLatestFeeRecordForStudent(student.getId());
                
                if (feeRecordOpt.isPresent()) {
                    StudentFeeRecord feeRecord = feeRecordOpt.get();
                    response.setFeeCategory(feeRecord.getFeeCategory());
                    response.setTotalFees(feeRecord.getNetAmount());
                    response.setAmountPaid(feeRecord.getAmountPaid());
                    response.setOutstandingBalance(feeRecord.getOutstandingBalance());
                    response.setPaymentStatus(feeRecord.getPaymentStatus());
                } else {
                    response.setFeeCategory("N/A");
                    response.setTotalFees(BigDecimal.ZERO);
                    response.setAmountPaid(BigDecimal.ZERO);
                    response.setOutstandingBalance(BigDecimal.ZERO);
                    response.setPaymentStatus("NO_RECORD");
                }
                
                results.add(response);
            }
        }
        
        log.info("Student search completed: {} match(es) found", results.size());
        return results;
    }

    /**
     * Record payment at bank (Option A: Teller Counter)
     * 
     * Used when: Parent walks into bank branch and pays at teller counter
     * 
     * Process:
     * 1. Teller looks up student by reference
     * 2. Teller enters payment details
     * 3. Bank system generates transaction ID
     * 4. Payment recorded with bank details (branch, teller, transaction ID)
     * 5. School's fee record updated automatically
     * 6. Notification sent to school bursar
     * 
     * @param request BankPaymentRequest with all payment details
     * @return Saved payment record
     */
    @Transactional
    public Payment recordBankCounterPayment(BankPaymentRequest request) {
        log.info("Recording bank counter payment: {} {} for student {} (Year: {}, Term: {})", 
                 request.getAmount(), request.getCurrency(), request.getStudentReference(), 
                 request.getYear(), request.getTerm());
        
        // Validate currency
        validateCurrency(request.getCurrency());
        
        // Lookup student entities with specific year/term
        StudentLookupResult lookup = lookupStudentEntitiesInternal(
            request.getStudentReference(), request.getYear(), request.getTerm());
        
        // Validate currency matches fee record currency
        if (lookup.getFeeRecord() != null && lookup.getFeeRecord().getCurrency() != null) {
            if (!request.getCurrency().equals(lookup.getFeeRecord().getCurrency())) {
                throw new IllegalArgumentException(
                    String.format("Currency mismatch: Payment currency '%s' does not match fee record currency '%s'",
                        request.getCurrency(), lookup.getFeeRecord().getCurrency()));
            }
        }
        
        // Create payment
        Payment payment = new Payment();
        payment.setSchool(lookup.getSchool());
        payment.setStudent(lookup.getStudent());
        payment.setFeeRecord(lookup.getFeeRecord());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setPaymentMethod(PaymentMethod.BANK_COUNTER);
        payment.setStatus("PENDING");  // Will be COMPLETED after Flexcube confirms
        
        // Bank-specific fields
        payment.setBankBranch(request.getBankBranch());
        payment.setTellerName(request.getTellerName());
        payment.setParentAccountNumber(request.getParentAccountNumber());
        payment.setBankTransactionId(request.getBankTransactionId());
        payment.setBankProcessedTime(LocalDateTime.now());
        payment.setReceivedBy("Bank: " + request.getTellerName());
        payment.setPaymentNotes(request.getPaymentNotes());
        
        // Save payment first to get payment reference
        Payment savedPayment = paymentRepository.save(payment);
        
        // Process Flexcube transaction if enabled
        if (flexcubeEnabled) {
            processFlexcubePayment(savedPayment, request.getParentAccountNumber(), request.getAmount());
        } else {
            // Flexcube disabled - for testing/development only
            log.warn("Flexcube integration disabled - payment marked COMPLETED without fund transfer");
            savedPayment.setStatus("COMPLETED");
            savedPayment = paymentRepository.save(savedPayment);
            
            // Update fee record immediately (since no Flexcube confirmation needed)
            if (lookup.getFeeRecord() != null) {
                StudentFeeRecord feeRecord = lookup.getFeeRecord();
                feeRecord.addPayment(request.getAmount());
                feeRecordRepository.save(feeRecord);
                log.info("Fee record updated (TEST MODE), new outstanding: {}", feeRecord.getOutstandingBalance());
            } else {
                log.warn("No fee record found for student {}, payment recorded but balance not updated",
                         request.getStudentReference());
            }
        }
        
        log.info("Bank counter payment recorded successfully: {}", savedPayment.getPaymentReference());
        
        // Audit trail
        auditTrailService.logAction(
            null, // userId (set if available)
            request.getTellerName(),
            "BANK_PAYMENT",
            "Payment",
            savedPayment.getId() != null ? savedPayment.getId().toString() : null,
            String.format("Bank payment of %s for student %s by teller %s (branch: %s, txn: %s)",
                request.getAmount(), request.getStudentReference(), request.getTellerName(), request.getBankBranch(), request.getBankTransactionId())
        );
        
        // TODO: Send notification to school bursar (task 6)
        
        return savedPayment;
    }

    /**
     * Record payment via digital banking (Option C: Mobile/Internet Banking)
     * 
     * Used when: Parent pays via mobile app, internet banking, or USSD
     * 
     * Process:
     * 1. Parent enters student reference in banking app
     * 2. System looks up student and shows outstanding balance
     * 3. Parent confirms payment
     * 4. Bank system debits parent's account
     * 5. Payment auto-recorded in school system
     * 6. Instant SMS/email confirmation to parent and school
     * 
     * @param request DigitalBankingPaymentRequest
     * @return Saved payment record
     */
    @Transactional
    public Payment recordDigitalBankingPayment(DigitalBankingPaymentRequest request) {
        log.info("Recording digital banking payment: {} {} for student {} (Year: {}, Term: {}) via {}", 
                 request.getAmount(), request.getCurrency(), request.getStudentReference(), 
                 request.getYear(), request.getTerm(), request.getPaymentMethod());
        
        // Validate currency
        validateCurrency(request.getCurrency());
        
        // Lookup student entities with specific year/term
        StudentLookupResult lookup = lookupStudentEntitiesInternal(
            request.getStudentReference(), request.getYear(), request.getTerm());
        
        // Validate currency matches fee record currency
        if (lookup.getFeeRecord() != null && lookup.getFeeRecord().getCurrency() != null) {
            if (!request.getCurrency().equals(lookup.getFeeRecord().getCurrency())) {
                throw new IllegalArgumentException(
                    String.format("Currency mismatch: Payment currency '%s' does not match fee record currency '%s'",
                        request.getCurrency(), lookup.getFeeRecord().getCurrency()));
            }
        }
        
        // Create payment
        Payment payment = new Payment();
        payment.setSchool(lookup.getSchool());
        payment.setStudent(lookup.getStudent());
        payment.setFeeRecord(lookup.getFeeRecord());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus("PENDING");  // Will be COMPLETED after Flexcube confirms
        
        // Bank-specific fields
        payment.setParentAccountNumber(request.getParentAccountNumber());
        payment.setBankTransactionId(request.getBankTransactionId());
        payment.setBankProcessedTime(LocalDateTime.now());
        payment.setReceivedBy("Digital Banking: " + request.getPaymentMethod().getDisplayName());
        payment.setPaymentNotes("Automated digital payment");
        
        // Save payment first to get payment reference
        Payment savedPayment = paymentRepository.save(payment);
        
        // Process Flexcube transaction if enabled
        if (flexcubeEnabled) {
            processFlexcubePayment(savedPayment, request.getParentAccountNumber(), request.getAmount());
        } else {
            // Flexcube disabled - for testing/development only
            log.warn("Flexcube integration disabled - payment marked COMPLETED without fund transfer");
            savedPayment.setStatus("COMPLETED");
            savedPayment = paymentRepository.save(savedPayment);
            
            // Update fee record immediately (since no Flexcube confirmation needed)
            if (lookup.getFeeRecord() != null) {
                StudentFeeRecord feeRecord = lookup.getFeeRecord();
                feeRecord.addPayment(request.getAmount());
                feeRecordRepository.save(feeRecord);
                log.info("Fee record updated (TEST MODE), new outstanding: {}", feeRecord.getOutstandingBalance());
            }
        }
        
        log.info("Digital banking payment recorded successfully: {}", savedPayment.getPaymentReference());
        
        // Audit trail
        auditTrailService.logAction(
            null, // userId (set if available)
            "DigitalBanking",
            "DIGITAL_BANK_PAYMENT",
            "Payment",
            savedPayment.getId() != null ? savedPayment.getId().toString() : null,
            String.format("Digital banking payment of %s for student %s (txn: %s)",
                request.getAmount(), request.getStudentReference(), request.getBankTransactionId())
        );
        
        // TODO: Send notification to parent and school (task 6)
        
        return savedPayment;
    }

    /**
     * Process payment through Flexcube
     * Sends fund transfer request from parent account to school collection account
     * 
     * Flow:
     * 1. Get authentication token from Flexcube token API (Basic Auth)
     * 2. Send payment request to Flexcube payment API with token
     * 3. Use payment currency (USD or ZWG) for the transaction
     * 
     * @param payment Payment entity (already saved with PENDING status)
     * @param parentAccountNumber Parent's bank account to debit
     * @param amount Transaction amount
     */
    private void processFlexcubePayment(Payment payment, String parentAccountNumber, BigDecimal amount) {
        log.info("Processing Flexcube payment for: {} (Amount: {})", payment.getPaymentReference(), amount);
        
        FlexcubeTransactionLog fcLog = null;
        
        try {
            // Step 1: Get authentication token
            String token = getFlexcubeToken();
            
            // Step 2: Build Flexcube payment request
            FCUBSPayment fcubsRequest = flexcubeService.buildSchoolFeePaymentRequest(
                payment, parentAccountNumber, amount);
            
            // Validate request
            flexcubeService.validatePaymentRequest(fcubsRequest);
            
            // Log request to database
            fcLog = flexcubeService.logRequest(fcubsRequest, payment.getPaymentReference());
            
            // Step 3: Send to Flexcube Payment API with token
            String paymentUrlWithToken = flexcubeApiUrl + "?token=" + token;
            log.info("Sending payment request to Flexcube: {}", paymentUrlWithToken);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<FCUBSPayment> requestEntity = new HttpEntity<>(fcubsRequest, headers);
            
            ResponseEntity<FlexcubePaymentResponse> responseEntity = restTemplate.postForEntity(
                paymentUrlWithToken, requestEntity, FlexcubePaymentResponse.class);
            
            FlexcubePaymentResponse fcResponse = responseEntity.getBody();
            
            // Log raw response for debugging
            log.info("Flexcube raw response: Status={}, Body={}", 
                     responseEntity.getStatusCode(), fcResponse);
            
            // Validate response structure
            if (fcResponse == null || fcResponse.getBancabc_reponse() == null) {
                throw new RuntimeException("Flexcube returned null or invalid response");
            }
            
            BancabcPaymentReponse bancabcResponse = fcResponse.getBancabc_reponse();
            
            // Log response to database
            if (fcLog != null) {
                // Convert to String for logging (since we don't have RrnResponse anymore)
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String responseJson = mapper.writeValueAsString(fcResponse);
                    fcLog.setResponsePayload(responseJson);
                    fcLog.setResponseStatus(bancabcResponse.getResponse());
                    fcLog.setResponseMessage(bancabcResponse.getMessage());
                    fcLog.setFlexcubeReference(bancabcResponse.getFlexcubeReference());
                    
                    if (bancabcResponse.isSuccess()) {
                        fcLog.markSuccess(java.time.LocalDateTime.now());
                    } else {
                        fcLog.markFailed(bancabcResponse.getMessage(), java.time.LocalDateTime.now());
                    }
                } catch (Exception e) {
                    log.warn("Failed to log Flexcube response details: {}", e.getMessage());
                }
            }
            
            // Process response - check if successful
            if (bancabcResponse.isSuccess()) {
                log.info("Flexcube payment successful: {} (XREF: {})", 
                         payment.getPaymentReference(), bancabcResponse.getFlexcubeReference());
                
                // Update payment with Flexcube details
                payment.setFlexcubeReference(bancabcResponse.getFlexcubeReference());
                payment.setStatus("COMPLETED");
                paymentRepository.save(payment);
                
                // CRITICAL: Update fee record balance ONLY after Flexcube confirms
                // This ensures school records match actual funds received
                if (payment.getFeeRecord() != null) {
                    StudentFeeRecord feeRecord = payment.getFeeRecord();
                    feeRecord.addPayment(amount);
                    feeRecordRepository.save(feeRecord);
                    log.info("Fee record updated: New balance = {}, Status = {}", 
                             feeRecord.getOutstandingBalance(), feeRecord.getPaymentStatus());
                } else {
                    log.warn("No fee record found for payment {} - balance not updated", 
                             payment.getPaymentReference());
                }
                
            } else {
                // Enhanced error message with more details
                String errorMsg = String.format("Response: %s, Message: %s, MSGSTAT: %s", 
                    bancabcResponse.getResponse(), 
                    bancabcResponse.getMessage(),
                    bancabcResponse.getValueByKey("MSGSTAT"));
                log.error("Flexcube payment failed: {}", errorMsg);
                log.error("Full response object: {}", fcResponse);
                
                payment.setStatus("FAILED");
                payment.setPaymentNotes((payment.getPaymentNotes() != null ? payment.getPaymentNotes() + "; " : "") 
                                       + "Flexcube Error: " + errorMsg);
                paymentRepository.save(payment);
                
                throw new RuntimeException("Flexcube payment failed: " + errorMsg);
            }
            
        } catch (Exception e) {
            log.error("Error processing Flexcube payment", e);
            
            // Log error
            if (fcLog != null) {
                flexcubeService.logError(fcLog, e.getMessage(), "API_ERROR");
            }
            
            // Update payment status
            payment.setStatus("FAILED");
            payment.setPaymentNotes((payment.getPaymentNotes() != null ? payment.getPaymentNotes() + "; " : "") 
                                   + "Error: " + e.getMessage());
            paymentRepository.save(payment);
            
            throw new RuntimeException("Failed to process Flexcube payment: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate currency is either USD or ZWG
     */
    private void validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (!"USD".equals(currency) && !"ZWG".equals(currency)) {
            throw new IllegalArgumentException(
                String.format("Invalid currency '%s'. Only 'USD' and 'ZWG' are supported", currency));
        }
    }
    
    /**
     * Get authentication token from Flexcube Token API
     * Uses Basic Auth with configured credentials
     * 
     * Response format:
     * {
     *   "bancabc_reponse": {
     *     "response": "00",
     *     "message": "success",
     *     "value": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
     *   }
     * }
     * 
     * @return Authentication token (JWT)
     */
    private String getFlexcubeToken() {
        log.debug("Requesting authentication token from Flexcube: {}", flexcubeTokenUrl);
        
        try {
            // Create Basic Auth header
            String auth = flexcubeTokenUsername + ":" + flexcubeTokenPassword;
            byte[] encodedAuth = java.util.Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            // Call token API using GET method (not POST)
            ResponseEntity<TokenResponse> responseEntity = restTemplate.exchange(
                flexcubeTokenUrl, 
                org.springframework.http.HttpMethod.GET, 
                requestEntity, 
                TokenResponse.class);
            
            TokenResponse tokenResponse = responseEntity.getBody();
            
            if (tokenResponse == null || tokenResponse.getBancabc_reponse() == null) {
                throw new RuntimeException("Flexcube token API returned null response");
            }
            
            BancabcReponse bancabcResponse = tokenResponse.getBancabc_reponse();
            
            // Check response code ("00" = success)
            if (!"00".equals(bancabcResponse.getResponse())) {
                throw new RuntimeException("Flexcube token API failed: " + bancabcResponse.getMessage());
            }
            
            String token = bancabcResponse.getValue();
            
            if (token == null || token.isBlank()) {
                throw new RuntimeException("Flexcube token API returned empty token");
            }
            
            log.debug("Successfully retrieved Flexcube token");
            return token.trim();
            
        } catch (Exception e) {
            log.error("Failed to get Flexcube authentication token", e);
            throw new RuntimeException("Failed to authenticate with Flexcube: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all payments processed via bank channels today
     * For bank teller end-of-day reconciliation
     */
    public List<Payment> getTodaysBankPayments() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        log.debug("Fetching today's bank payments since: {}", startOfDay);
        
        return paymentRepository.findRecentPayments(startOfDay).stream()
                .filter(Payment::isBankChannelPayment)
                .toList();
    }

    /**
     * Get all bank payments for a specific school today
     * For school-specific reconciliation
     */
    public List<Payment> getTodaysBankPaymentsForSchool(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School not found: " + schoolId));
        
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        log.debug("Fetching today's bank payments for school: {}", school.getSchoolName());
        
        return paymentRepository.findRecentPaymentsBySchool(school, startOfDay).stream()
                .filter(Payment::isBankChannelPayment)
                .toList();
    }

    /**
     * Calculate total bank payments for today
     * For bank's daily revenue tracking
     */
    public BigDecimal calculateTodaysBankPaymentsTotal() {
        List<Payment> todaysPayments = getTodaysBankPayments();
        
        BigDecimal total = todaysPayments.stream()
                .filter(p -> p.getStatus().equals("COMPLETED") && !p.getIsReversed())
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Today's total bank payments: {}", total);
        return total;
    }

    // ========== INNER CLASSES (DTOs) ==========

    /**
     * Student Lookup Result DTO
     * Contains all information bank teller needs to see
     */
    @lombok.Data
    public static class StudentLookupResult {
        private Student student;
        private School school;
        private StudentFeeRecord feeRecord;
        private BigDecimal outstandingBalance;
        private BigDecimal totalFees;
        private BigDecimal amountPaid;
        
        public String getStudentName() {
            return student != null ? student.getFullName() : "";
        }
        
        public String getSchoolName() {
            return school != null ? school.getSchoolName() : "";
        }
        
        public String getGrade() {
            return student != null ? student.getGrade() : "";
        }
        
        public String getClassName() {
            return student != null ? student.getClassName() : "";
        }
    }

    /**
     * Bank Payment Request DTO (Option A: Teller Counter)
     * 
     * IMPORTANT: year and term are REQUIRED to identify which fee record to pay
     * - Student may have multiple fee records (one per term)
     * - Payment must be applied to the correct term's record
     * - Currency must be specified (USD or ZWG)
     */
    @lombok.Data
    public static class BankPaymentRequest {
        private String studentReference;       // SCH001-STU-001 (REQUIRED)
        private Integer year;                  // 2026 (REQUIRED)
        private Integer term;                  // 1, 2, or 3 (REQUIRED)
        private String currency;               // "USD" or "ZWG" (REQUIRED)
        private BigDecimal amount;
        private String bankBranch;             // "Branch 005 - Harare Central"
        private String tellerName;             // "John Ncube"
        private String parentAccountNumber;    // "1234567890"
        private String bankTransactionId;      // "BNK-TXN-789456"
        private String paymentNotes;
    }

    /**
     * Digital Banking Payment Request DTO (Option C: Mobile/Internet Banking)
     * 
     * IMPORTANT: year and term are REQUIRED to identify which fee record to pay
     * - Currency must be specified (USD or ZWG)
     */
    @lombok.Data
    public static class DigitalBankingPaymentRequest {
        private String studentReference;       // SCH001-STU-001 (REQUIRED)
        private Integer year;                  // 2026 (REQUIRED)
        private Integer term;                  // 1, 2, or 3 (REQUIRED)
        private String currency;               // "USD" or "ZWG" (REQUIRED)
        private BigDecimal amount;
        private PaymentMethod paymentMethod;   // MOBILE_BANKING, INTERNET_BANKING, USSD
        private String parentAccountNumber;    // "1234567890"
        private String bankTransactionId;      // Auto-generated by bank system
    }
    
    /**
     * Flexcube Token Response DTOs
     * 
     * Response structure from http://10.106.60.5:2011/api/GetToken:
     * {
     *   "bancabc_reponse": {
     *     "response": "00",
     *     "message": "success",
     *     "value": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
     *   }
     * }
     */
    @lombok.Data
    public static class TokenResponse {
        private BancabcReponse bancabc_reponse;
    }
    
    @lombok.Data
    public static class BancabcReponse {
        private String response;   // "00" for success
        private String message;    // "success"
        private String value;      // The actual JWT token
    }
    
    /**
     * Flexcube Payment Response DTOs
     * 
     * Response structure from payment API:
     * {
     *   "bancabc_reponse": {
     *     "response": "00",
     *     "message": "success",
     *     "value": [
     *       ["MSGSTAT", "SUCCESS"],
     *       ["XREF", "120PBIL260080002"],
     *       ["FCCREF", "120PBIL260080002"],
     *       ["GW-SAV-01", "Transaction Saved Succesfully"]
     *     ]
     *   }
     * }
     */
    @lombok.Data
    public static class FlexcubePaymentResponse {
        private BancabcPaymentReponse bancabc_reponse;
    }
    
    @lombok.Data
    public static class BancabcPaymentReponse {
        private String response;        // "00" for success
        private String message;         // "success"
        private java.util.List<java.util.List<String>> value;  // Array of [key, value] pairs
        
        /**
         * Get value by key from the array of key-value pairs
         */
        public String getValueByKey(String key) {
            if (value == null) return null;
            for (java.util.List<String> pair : value) {
                if (pair.size() == 2 && key.equals(pair.get(0))) {
                    return pair.get(1);
                }
            }
            return null;
        }
        
        /**
         * Check if transaction was successful
         */
        public boolean isSuccess() {
            return "00".equals(response) && "SUCCESS".equalsIgnoreCase(getValueByKey("MSGSTAT"));
        }
        
        /**
         * Get Flexcube reference number
         */
        public String getFlexcubeReference() {
            String xref = getValueByKey("XREF");
            return xref != null ? xref : getValueByKey("FCCREF");
        }
    }
}
