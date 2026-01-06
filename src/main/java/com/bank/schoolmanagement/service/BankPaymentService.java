package com.bank.schoolmanagement.service;

import com.bank.schoolmanagement.dto.StudentLookupResponse;
import com.bank.schoolmanagement.dto.StudentSearchRequest;
import com.bank.schoolmanagement.entity.Payment;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.entity.StudentFeeRecord;
import com.bank.schoolmanagement.enums.PaymentMethod;
import com.bank.schoolmanagement.repository.PaymentRepository;
import com.bank.schoolmanagement.repository.SchoolRepository;
import com.bank.schoolmanagement.repository.StudentFeeRecordRepository;
import com.bank.schoolmanagement.repository.StudentRepository;
import com.bank.schoolmanagement.service.AuditTrailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        
        // Get latest fee record (optional)
        Optional<StudentFeeRecord> feeRecordOpt = feeRecordService.getLatestFeeRecordForStudent(student.getId());
        
        // Build DTO response (no circular references)
        StudentLookupResponse response = new StudentLookupResponse();
        response.setStudentId(student.getStudentId());
        response.setStudentName(student.getFirstName() + " " + student.getLastName());
        response.setGrade(student.getGrade());
        response.setClassName(student.getClassName());
        
        response.setSchoolCode(school.getSchoolCode());
        response.setSchoolName(school.getSchoolName());
        
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
        
        log.info("Student lookup successful: {} from {}, Outstanding: {}", 
                 response.getStudentName(), school.getSchoolName(), response.getOutstandingBalance());
        
        return response;
    }
    
    /**
     * Internal method: Get student entities for payment processing
     * Used by payment methods that need actual entities to create Payment records
     * @param studentReference Format: "SCH001-STU-001"
     * @return StudentLookupResult with entities
     */
    private StudentLookupResult lookupStudentEntitiesInternal(String studentReference) {
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
        
        // Get latest fee record (optional)
        Optional<StudentFeeRecord> feeRecordOpt = feeRecordService.getLatestFeeRecordForStudent(student.getId());
        
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
        log.info("Recording bank counter payment: {} for student {}", 
                 request.getAmount(), request.getStudentReference());
        
        // Lookup student entities
        StudentLookupResult lookup = lookupStudentEntitiesInternal(request.getStudentReference());
        
        // Create payment
        Payment payment = new Payment();
        payment.setSchool(lookup.getSchool());
        payment.setStudent(lookup.getStudent());
        payment.setFeeRecord(lookup.getFeeRecord());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(PaymentMethod.BANK_COUNTER);
        payment.setStatus("COMPLETED");
        
        // Bank-specific fields
        payment.setBankBranch(request.getBankBranch());
        payment.setTellerName(request.getTellerName());
        payment.setParentAccountNumber(request.getParentAccountNumber());
        payment.setBankTransactionId(request.getBankTransactionId());
        payment.setBankProcessedTime(LocalDateTime.now());
        payment.setReceivedBy("Bank: " + request.getTellerName());
        payment.setPaymentNotes(request.getPaymentNotes());
        
        // Save payment
        Payment savedPayment = paymentRepository.save(payment);
        
        // Update fee record
        if (lookup.getFeeRecord() != null) {
            StudentFeeRecord feeRecord = lookup.getFeeRecord();
            feeRecord.addPayment(request.getAmount());
            feeRecordRepository.save(feeRecord);
            log.info("Fee record updated, new outstanding: {}", feeRecord.getOutstandingBalance());
        } else {
            log.warn("No fee record found for student {}, payment recorded but balance not updated",
                     request.getStudentReference());
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
        log.info("Recording digital banking payment: {} for student {} via {}", 
                 request.getAmount(), request.getStudentReference(), request.getPaymentMethod());
        
        // Lookup student entities
        StudentLookupResult lookup = lookupStudentEntitiesInternal(request.getStudentReference());
        
        // Create payment
        Payment payment = new Payment();
        payment.setSchool(lookup.getSchool());
        payment.setStudent(lookup.getStudent());
        payment.setFeeRecord(lookup.getFeeRecord());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus("COMPLETED");
        
        // Bank-specific fields
        payment.setParentAccountNumber(request.getParentAccountNumber());
        payment.setBankTransactionId(request.getBankTransactionId());
        payment.setBankProcessedTime(LocalDateTime.now());
        payment.setReceivedBy("Digital Banking: " + request.getPaymentMethod().getDisplayName());
        payment.setPaymentNotes("Automated digital payment");
        
        // Save payment
        Payment savedPayment = paymentRepository.save(payment);
        
        // Update fee record
        if (lookup.getFeeRecord() != null) {
            StudentFeeRecord feeRecord = lookup.getFeeRecord();
            feeRecord.addPayment(request.getAmount());
            feeRecordRepository.save(feeRecord);
            log.info("Fee record updated, new outstanding: {}", feeRecord.getOutstandingBalance());
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
     */
    @lombok.Data
    public static class BankPaymentRequest {
        private String studentReference;       // SCH001-STU-001
        private BigDecimal amount;
        private String bankBranch;             // "Branch 005 - Harare Central"
        private String tellerName;             // "John Ncube"
        private String parentAccountNumber;    // "1234567890"
        private String bankTransactionId;      // "BNK-TXN-789456"
        private String paymentNotes;
    }

    /**
     * Digital Banking Payment Request DTO (Option C: Mobile/Internet Banking)
     */
    @lombok.Data
    public static class DigitalBankingPaymentRequest {
        private String studentReference;       // SCH001-STU-001
        private BigDecimal amount;
        private PaymentMethod paymentMethod;   // MOBILE_BANKING, INTERNET_BANKING, USSD
        private String parentAccountNumber;    // "1234567890"
        private String bankTransactionId;      // Auto-generated by bank system
    }
}
