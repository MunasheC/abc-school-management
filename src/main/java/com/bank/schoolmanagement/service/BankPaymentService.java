package com.bank.schoolmanagement.service;

import com.bank.schoolmanagement.entity.Payment;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.entity.StudentFeeRecord;
import com.bank.schoolmanagement.enums.PaymentMethod;
import com.bank.schoolmanagement.repository.PaymentRepository;
import com.bank.schoolmanagement.repository.SchoolRepository;
import com.bank.schoolmanagement.repository.StudentFeeRecordRepository;
import com.bank.schoolmanagement.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final SchoolRepository schoolRepository;

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
     * @return StudentLookupResult with student, school, and fee information
     */
    public StudentLookupResult lookupStudentByReference(String studentReference) {
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
        
        // Get fee record (optional)
        Optional<StudentFeeRecord> feeRecordOpt = feeRecordRepository.findByStudentId(student.getId());
        
        StudentLookupResult result = new StudentLookupResult();
        result.setStudent(student);
        result.setSchool(school);
        result.setFeeRecord(feeRecordOpt.orElse(null));
        
        if (feeRecordOpt.isPresent()) {
            StudentFeeRecord feeRecord = feeRecordOpt.get();
            result.setOutstandingBalance(feeRecord.getOutstandingBalance());
            result.setTotalFees(feeRecord.getNetAmount());
            result.setAmountPaid(feeRecord.getAmountPaid());
        } else {
            result.setOutstandingBalance(BigDecimal.ZERO);
            result.setTotalFees(BigDecimal.ZERO);
            result.setAmountPaid(BigDecimal.ZERO);
        }
        
        log.info("Student lookup successful: {} from {}, Outstanding: {}", 
                 student.getFullName(), school.getSchoolName(), result.getOutstandingBalance());
        
        return result;
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
        
        // Lookup student
        StudentLookupResult lookup = lookupStudentByReference(request.getStudentReference());
        
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
        
        // Lookup student
        StudentLookupResult lookup = lookupStudentByReference(request.getStudentReference());
        
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
