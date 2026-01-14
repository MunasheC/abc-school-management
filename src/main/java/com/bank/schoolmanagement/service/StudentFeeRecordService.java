package com.bank.schoolmanagement.service;

import com.bank.schoolmanagement.context.SchoolContext;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.entity.StudentFeeRecord;
import com.bank.schoolmanagement.repository.StudentFeeRecordRepository;
import com.bank.schoolmanagement.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * StudentFeeRecord Service - Business logic for fee records
 * 
 * LEARNING: Service layer for financial operations
 * - Manages fee records per student per term
 * - Calculates totals automatically
 * - Tracks payment status
 * - Provides financial reports for bursar
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentFeeRecordService {

    private final StudentFeeRecordRepository feeRecordRepository;
    private final StudentRepository studentRepository;
    private final AuditTrailService auditTrailService;
    

    /**
     * Create fee record for student
     * 
     * LEARNING: Fee record creation
     * - calculateTotals() is called automatically by @PrePersist
     * - This calculates grossAmount, netAmount, outstandingBalance
     * - paymentStatus is set based on outstandingBalance
     */
    @Transactional
    public StudentFeeRecord createFeeRecord(StudentFeeRecord feeRecord) {
        log.info("Creating fee record for student ID: {}, Year: {}, Term: {}", 
                 feeRecord.getStudent().getId(), feeRecord.getYear(), feeRecord.getTerm());
        
        StudentFeeRecord saved = feeRecordRepository.save(feeRecord);
        log.info("Fee record created with ID: {}, Outstanding: {}", 
                 saved.getId(), saved.getOutstandingBalance());
        
        // Audit trail
        auditTrailService.logAction(
            null,
            "SYSTEM",
            "CREATE_FEE_RECORD",
            "StudentFeeRecord",
            saved.getId() != null ? saved.getId().toString() : null,
            String.format("Created fee record for student ID %s, Year: %d, Term: %d, Total: %s",
                saved.getStudent() != null ? saved.getStudent().getId() : "UNKNOWN",
                saved.getYear(), saved.getTerm(), saved.getNetAmount())
        );
        
        return saved;
    }

    /**
     * Get fee record by ID
     */
    public Optional<StudentFeeRecord> getFeeRecordById(Long id) {
        log.debug("Fetching fee record with ID: {}", id);
        return feeRecordRepository.findById(id);
    }

    /**
     * Get ALL fee records for a student by database ID
     * 
     * CHANGE: Returns List instead of Optional (OneToMany relationship)
     * Returns complete financial history (all terms/years)
     */
    public List<StudentFeeRecord> getFeeRecordsByStudentId(Long studentId) {
        log.debug("Fetching all fee records for student database ID: {}", studentId);
        return feeRecordRepository.findByStudentId(studentId);
    }

    /**
     * Get ALL fee records for a student by their studentId field (e.g., STU1733838975353)
     * 
     * WARNING: NOT school-aware - use getFeeRecordsByStudentReferenceForCurrentSchool instead
     * CHANGE: Returns List instead of Optional (OneToMany relationship)
     * Returns complete financial history (all terms/years)
     */
    public List<StudentFeeRecord> getFeeRecordsByStudentReference(String studentId) {
        log.debug("Fetching all fee records for student reference: {}", studentId);
        return feeRecordRepository.findByStudent_StudentId(studentId);
    }
    
    /**
     * Get ALL fee records for a student by studentId FOR CURRENT SCHOOL (SCHOOL-AWARE)
     * 
     * MULTI-TENANT SAFE: Filters by current school context
     * Returns complete financial history (all terms/years) for the student in current school
     */
    public List<StudentFeeRecord> getFeeRecordsByStudentReferenceForCurrentSchool(String studentId) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching all fee records for student reference: {} in school: {}", 
                  studentId, currentSchool.getSchoolName());
        return feeRecordRepository.findBySchoolAndStudent_StudentId(currentSchool, studentId);
    }
    
    /**
     * Get the LATEST fee record for a student (most recent term)
     * Useful for current term balance checks and promotions
     */
    public Optional<StudentFeeRecord> getLatestFeeRecordForStudent(Long studentId) {
        log.debug("Fetching latest fee record for student ID: {}", studentId);
        List<StudentFeeRecord> records = feeRecordRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
    }

    /**
     * Get fee records for a specific year and term
     */
    public List<StudentFeeRecord> getFeeRecordsByYearAndTerm(Integer year, Integer term) {
        log.debug("Fetching fee records for year: {}, term: {}", year, term);
        return feeRecordRepository.findByYearAndTerm(year, term);
    }

    /**
     * Get fee records by payment status
     */
    public List<StudentFeeRecord> getFeeRecordsByPaymentStatus(String status) {
        log.debug("Fetching fee records with status: {}", status);
        return feeRecordRepository.findByPaymentStatusAndIsActiveTrue(status);
    }

    /**
     * Get fee records by fee category
     * WARNING: NOT school-aware - use getFeeRecordsByFeeCategoryForCurrentSchool instead
     */
    public List<StudentFeeRecord> getFeeRecordsByFeeCategory(String feeCategory) {
        log.debug("Fetching fee records for category: {}", feeCategory);
        return feeRecordRepository.findByFeeCategory(feeCategory);
    }

    /**
     * Get fee records by fee category FOR CURRENT SCHOOL (SCHOOL-AWARE)
     * 
     * MULTI-TENANT SAFE: Filters by current school context
     */
    public List<StudentFeeRecord> getFeeRecordsByFeeCategoryForCurrentSchool(String feeCategory) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching fee records for category: {} in school: {}", 
                  feeCategory, currentSchool.getSchoolName());
        return feeRecordRepository.findBySchoolAndFeeCategory(currentSchool, feeCategory);
    }

    /**
     * Get students with scholarships
     */
    public List<StudentFeeRecord> getStudentsWithScholarships() {
        log.debug("Fetching students with scholarships");
        return feeRecordRepository.findByHasScholarshipTrue();
    }

    /**
     * Get records with outstanding balances
     */
    public List<StudentFeeRecord> getRecordsWithOutstandingBalance() {
        log.debug("Fetching records with outstanding balance");
        return feeRecordRepository.findRecordsWithOutstandingBalance();
    }

    /**
     * Get fully paid records
     */
    public List<StudentFeeRecord> getFullyPaidRecords() {
        log.debug("Fetching fully paid records");
        return feeRecordRepository.findFullyPaidRecords();
    }

    /**
     * Update fee record
     * 
     * LEARNING: Automatic recalculation
     * When you update fees or discounts, calculateTotals() runs automatically
     * because of @PreUpdate hook
     */
    @Transactional
    public StudentFeeRecord updateFeeRecord(Long id, StudentFeeRecord updatedRecord) {
        log.info("Updating fee record with ID: {}", id);
        
        return feeRecordRepository.findById(id)
                .map(existing -> {
                    // Capture before state
                    String beforeValue = String.format("Net Amount: %s, Paid: %s, Outstanding: %s, Category: %s, Year: %d, Term: %d",
                        existing.getNetAmount(), existing.getAmountPaid(), 
                        existing.getOutstandingBalance(), existing.getFeeCategory(), existing.getYear(), existing.getTerm());
                    
                    // Update fee components
                    if (updatedRecord.getTuitionFee() != null) {
                        existing.setTuitionFee(updatedRecord.getTuitionFee());
                    }
                    if (updatedRecord.getBoardingFee() != null) {
                        existing.setBoardingFee(updatedRecord.getBoardingFee());
                    }
                    if (updatedRecord.getDevelopmentLevy() != null) {
                        existing.setDevelopmentLevy(updatedRecord.getDevelopmentLevy());
                    }
                    if (updatedRecord.getExamFee() != null) {
                        existing.setExamFee(updatedRecord.getExamFee());
                    }
                    if (updatedRecord.getOtherFees() != null) {
                        existing.setOtherFees(updatedRecord.getOtherFees());
                    }
                    
                    // Update discounts
                    if (updatedRecord.getScholarshipAmount() != null) {
                        existing.setScholarshipAmount(updatedRecord.getScholarshipAmount());
                        existing.setHasScholarship(updatedRecord.getScholarshipAmount().compareTo(BigDecimal.ZERO) > 0);
                    }
                    if (updatedRecord.getSiblingDiscount() != null) {
                        existing.setSiblingDiscount(updatedRecord.getSiblingDiscount());
                    }
                    if (updatedRecord.getEarlyPaymentDiscount() != null) {
                        existing.setEarlyPaymentDiscount(updatedRecord.getEarlyPaymentDiscount());
                    }
                    
                    // Update other fields
                    if (updatedRecord.getFeeCategory() != null) {
                        existing.setFeeCategory(updatedRecord.getFeeCategory());
                    }
                    if (updatedRecord.getYear() != null) {
                        existing.setYear(updatedRecord.getYear());
                    }
                    if (updatedRecord.getTerm() != null) {
                        existing.setTerm(updatedRecord.getTerm());
                    }
                    if (updatedRecord.getBursarNotes() != null) {
                        existing.setBursarNotes(updatedRecord.getBursarNotes());
                    }
                    
                    // calculateTotals() will run automatically due to @PreUpdate
                    StudentFeeRecord saved = feeRecordRepository.save(existing);
                    log.info("Fee record updated, new outstanding: {}", saved.getOutstandingBalance());
                    
                    // Capture after state
                    String afterValue = String.format("Net Amount: %s, Paid: %s, Outstanding: %s, Category: %s, Year: %d, Term: %d",
                        saved.getNetAmount(), saved.getAmountPaid(), 
                        saved.getOutstandingBalance(), saved.getFeeCategory(), saved.getYear(), saved.getTerm());
                    
                    // Audit trail
                    auditTrailService.logAction(
                        null,
                        "SYSTEM",
                        "UPDATE_FEE_RECORD",
                        "StudentFeeRecord",
                        saved.getId().toString(),
                        String.format("Updated fee record for student ID %s, Year: %d, Term: %d, New Total: %s",
                            saved.getStudent() != null ? saved.getStudent().getId() : "UNKNOWN",
                            saved.getYear(), saved.getTerm(), saved.getNetAmount()),
                        beforeValue,
                        afterValue
                    );
                    
                    return saved;
                })
                .orElseThrow(() -> {
                    log.error("Fee record not found with ID: {}", id);
                    return new IllegalArgumentException("Fee record not found with ID: " + id);
                });
    }

    /**
     * Add payment to fee record
     * 
     * LEARNING: Manual payment addition
     * Use this when recording a payment directly to the fee record
     * The addPayment() method in StudentFeeRecord automatically:
     * - Adds to amountPaid
     * - Recalculates outstandingBalance
     * - Updates paymentStatus
     */
    @Transactional
    public StudentFeeRecord addPayment(Long feeRecordId, BigDecimal amount) {
        log.info("Adding payment of {} to fee record ID: {}", amount, feeRecordId);
        
        return feeRecordRepository.findById(feeRecordId)
                .map(feeRecord -> {
                    feeRecord.addPayment(amount);
                    StudentFeeRecord saved = feeRecordRepository.save(feeRecord);
                    log.info("Payment added, new outstanding: {}, status: {}", 
                             saved.getOutstandingBalance(), saved.getPaymentStatus());
                    return saved;
                })
                .orElseThrow(() -> {
                    log.error("Fee record not found with ID: {}", feeRecordId);
                    return new IllegalArgumentException("Fee record not found with ID: " + feeRecordId);
                });
    }

    /**
     * Apply scholarship to student
     */
    @Transactional
    public StudentFeeRecord applyScholarship(Long feeRecordId, BigDecimal scholarshipAmount) {
        log.info("Applying scholarship of {} to fee record ID: {}", scholarshipAmount, feeRecordId);
        
        return feeRecordRepository.findById(feeRecordId)
                .map(feeRecord -> {
                    feeRecord.setScholarshipAmount(scholarshipAmount);
                    feeRecord.setHasScholarship(scholarshipAmount.compareTo(BigDecimal.ZERO) > 0);
                    // calculateTotals() will run automatically
                    StudentFeeRecord saved = feeRecordRepository.save(feeRecord);
                    log.info("Scholarship applied, new net amount: {}", saved.getNetAmount());
                    return saved;
                })
                .orElseThrow(() -> {
                    log.error("Fee record not found with ID: {}", feeRecordId);
                    return new IllegalArgumentException("Fee record not found with ID: " + feeRecordId);
                });
    }

    /**
     * Apply sibling discount
     */
    @Transactional
    public StudentFeeRecord applySiblingDiscount(Long feeRecordId, BigDecimal discountAmount) {
        log.info("Applying sibling discount of {} to fee record ID: {}", discountAmount, feeRecordId);
        
        return feeRecordRepository.findById(feeRecordId)
                .map(feeRecord -> {
                    feeRecord.setSiblingDiscount(discountAmount);
                    StudentFeeRecord saved = feeRecordRepository.save(feeRecord);
                    log.info("Sibling discount applied, new net amount: {}", saved.getNetAmount());
                    return saved;
                })
                .orElseThrow(() -> {
                    log.error("Fee record not found with ID: {}", feeRecordId);
                    return new IllegalArgumentException("Fee record not found with ID: " + feeRecordId);
                });
    }

    // ========== FINANCIAL REPORTS ==========

    /**
     * Get total outstanding fees across all students
     * 
     * LEARNING: Aggregate function
     * This uses SQL SUM to calculate total in the database
     * Much faster than fetching all records and summing in Java
     */
    public BigDecimal getTotalOutstandingFees() {
        log.debug("Calculating total outstanding fees");
        BigDecimal total = feeRecordRepository.calculateTotalOutstandingFees();
        log.info("Total outstanding fees: {}", total);
        return total;
    }

    /**
     * Get total fees collected
     */
    public BigDecimal getTotalCollectedFees() {
        log.debug("Calculating total collected fees");
        BigDecimal total = feeRecordRepository.calculateTotalCollectedFees();
        log.info("Total collected fees: {}", total);
        return total;
    }

    /**
     * Get total gross amount (before discounts)
     */
    public BigDecimal getTotalGrossAmount() {
        log.debug("Calculating total gross amount");
        BigDecimal total = feeRecordRepository.calculateTotalGrossAmount();
        log.info("Total gross amount: {}", total);
        return total;
    }

    /**
     * Get total scholarships awarded
     */
    public BigDecimal getTotalScholarships() {
        log.debug("Calculating total scholarships");
        BigDecimal total = feeRecordRepository.calculateTotalScholarships();
        log.info("Total scholarships: {}", total);
        return total;
    }

    /**
     * Get collection rate (percentage of fees collected)
     * 
     * LEARNING: Business calculation
     * Collection Rate = (Total Collected / Total Gross) * 100
     * Shows how well the school is collecting fees
     */
    public BigDecimal getCollectionRate() {
        log.debug("Calculating collection rate");
        
        BigDecimal totalGross = getTotalGrossAmount();
        BigDecimal totalCollected = getTotalCollectedFees();
        
        if (totalGross.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Total gross amount is zero, collection rate is 0%");
            return BigDecimal.ZERO;
        }
        
        BigDecimal rate = totalCollected
                .multiply(BigDecimal.valueOf(100))
                .divide(totalGross, 2, RoundingMode.HALF_UP);
        
        log.info("Collection rate: {}%", rate);
        return rate;
    }

    /**
     * Count records by payment status
     */
    public long countByPaymentStatus(String status) {
        log.debug("Counting records with status: {}", status);
        return feeRecordRepository.countByPaymentStatus(status);
    }

    /**
     * Get records by fee category and payment status
     */
    public List<StudentFeeRecord> getRecordsByFeeCategoryAndStatus(String feeCategory, String status) {
        log.debug("Fetching records for category {} with status {}", feeCategory, status);
        return feeRecordRepository.findByFeeCategoryAndPaymentStatus(feeCategory, status);
    }

    /**
     * Get records by year, term and payment status
     */
    public List<StudentFeeRecord> getRecordsByYearAndTermAndStatus(Integer year, Integer term, String status) {
        log.debug("Fetching records for year {} term {} with status {}", year, term, status);
        return feeRecordRepository.findByYearAndTermAndPaymentStatus(year, term, status);
    }

    /**
     * Check if student has fully paid
     */
    public boolean isFullyPaid(Long feeRecordId) {
        log.debug("Checking if fee record {} is fully paid", feeRecordId);
        
        return feeRecordRepository.findById(feeRecordId)
                .map(StudentFeeRecord::isFullyPaid)
                .orElse(false);
    }

    /**
     * Get payment percentage for a student
     */
    public BigDecimal getPaymentPercentage(Long feeRecordId) {
        log.debug("Getting payment percentage for fee record {}", feeRecordId);
        
        return feeRecordRepository.findById(feeRecordId)
                .map(record -> BigDecimal.valueOf(record.getPaymentPercentage()))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Deactivate fee record
     */
    @Transactional
    public StudentFeeRecord deactivateFeeRecord(Long id) {
        log.info("Deactivating fee record with ID: {}", id);
        
        return feeRecordRepository.findById(id)
                .map(feeRecord -> {
                    feeRecord.setIsActive(false);
                    StudentFeeRecord saved = feeRecordRepository.save(feeRecord);
                    log.info("Fee record deactivated");
                    return saved;
                })
                .orElseThrow(() -> {
                    log.error("Fee record not found with ID: {}", id);
                    return new IllegalArgumentException("Fee record not found with ID: " + id);
                });
    }

    /* ----------------------  BULK FEE ASSIGNMENT  ------------------------- */

    /**
     * Assign fees to all students in a specific grade
     * 
     * LEARNING: Bulk operations for efficiency
     * - Creates/updates fee records for all students in a grade at once
     * - Useful for setting term fees by grade level
     * - Each student gets individual fee record with same amounts
     * 
     * @param grade The grade to assign fees to (e.g., "Grade 5")
     * @param year The academic year (e.g., 2025)
     * @param term The academic term (e.g., 1, 2, 3)
     * @param feeCategory The fee category (e.g., "Boarder", "Day Scholar")
     * @param tuitionFee Tuition fee amount
     * @param boardingFee Boarding fee amount (0 for day scholars)
     * @param developmentLevy Development levy amount
     * @param examFee Exam fee amount
     * @param otherFees Other fees amount
     * @return List of created/updated fee records
     */
    @Transactional
    public List<StudentFeeRecord> assignFeesToGrade(
            String grade,
            Integer year,
            Integer term,
            String feeCategory,
            BigDecimal tuitionFee,
            BigDecimal boardingFee,
            BigDecimal developmentLevy,
            BigDecimal examFee,
            BigDecimal otherFees) {
        
        log.info("Assigning fees to all students in grade: {}, year: {}, term: {}", grade, year, term);
        
        // Get all active students in the grade
        List<Student> students = studentRepository.findByGrade(grade);
        log.info("Found {} students in grade: {}", students.size(), grade);
        
        List<StudentFeeRecord> createdRecords = new ArrayList<>();
        
        for (Student student : students) {
            if (!student.getIsActive()) {
                log.debug("Skipping inactive student: {}", student.getStudentId());
                continue;
            }
            
            // Check if student already has fee record for this year and term
            Optional<StudentFeeRecord> existingRecord = getLatestFeeRecordForStudent(student.getId());
            
            StudentFeeRecord feeRecord;
            if (existingRecord.isPresent() && 
                existingRecord.get().getYear().equals(year) && 
                existingRecord.get().getTerm().equals(term)) {
                // Update existing record
                feeRecord = existingRecord.get();
                log.debug("Updating existing fee record for student: {}", student.getStudentId());
            } else {
                // Create new record
                feeRecord = new StudentFeeRecord();
                feeRecord.setStudent(student);
                feeRecord.setYear(year);
                feeRecord.setTerm(term);
                log.debug("Creating new fee record for student: {}", student.getStudentId());
            }
            
            // Set fee amounts
            feeRecord.setFeeCategory(feeCategory);
            feeRecord.setTuitionFee(tuitionFee);
            feeRecord.setBoardingFee(boardingFee);
            feeRecord.setDevelopmentLevy(developmentLevy);
            feeRecord.setExamFee(examFee);
            feeRecord.setOtherFees(otherFees);
            
            // Preserve existing payment info if updating
            if (existingRecord.isEmpty()) {
                feeRecord.setPreviousBalance(BigDecimal.ZERO);
                feeRecord.setAmountPaid(BigDecimal.ZERO);
            }
            
            // Save (calculateTotals runs automatically via @PrePersist/@PreUpdate)
            StudentFeeRecord saved = feeRecordRepository.save(feeRecord);
            createdRecords.add(saved);
        }
        
        log.info("Successfully assigned fees to {} students in grade: {}", createdRecords.size(), grade);
        return createdRecords;
    }

    /**
     * Assign fees to multiple grades at once
     * 
     * LEARNING: Batch operations across grades
     * - Useful when all grades have same fee structure
     * - Or when setting fees for primary vs secondary school
     * 
     * @param grades List of grades (e.g., ["Grade 1", "Grade 2", "Grade 3"])
     * @param year Academic year
     * @param term Academic term
     * @param feeCategory Fee category
     * @param tuitionFee Tuition amount
     * @param boardingFee Boarding amount
     * @param developmentLevy Development levy
     * @param examFee Exam fee
     * @param otherFees Other fees
     * @return List of all created/updated records
     */
    @Transactional
    public List<StudentFeeRecord> assignFeesToMultipleGrades(
            List<String> grades,
            Integer year,
            Integer term,
            String feeCategory,
            BigDecimal tuitionFee,
            BigDecimal boardingFee,
            BigDecimal developmentLevy,
            BigDecimal examFee,
            BigDecimal otherFees) {
        
        log.info("Assigning fees to {} grades for year: {}, term: {}", grades.size(), year, term);
        
        List<StudentFeeRecord> allRecords = new ArrayList<>();
        
        for (String grade : grades) {
            List<StudentFeeRecord> gradeRecords = assignFeesToGrade(
                grade, year, term, feeCategory, 
                tuitionFee, boardingFee, developmentLevy, 
                examFee, otherFees
            );
            allRecords.addAll(gradeRecords);
        }
        
        log.info("Successfully assigned fees to {} students across {} grades", 
                 allRecords.size(), grades.size());
        return allRecords;
    }

    /**
     * Assign fees to all students in school
     * 
     * LEARNING: School-wide fee assignment
     * - Sets fees for ALL active students regardless of grade
     * - Useful when entire school has uniform fee structure
     * 
     * @return List of created/updated records
     */
    @Transactional
    public List<StudentFeeRecord> assignFeesToAllStudents(
            Integer year,
            Integer term,
            String feeCategory,
            BigDecimal tuitionFee,
            BigDecimal boardingFee,
            BigDecimal developmentLevy,
            BigDecimal examFee,
            BigDecimal otherFees) {
        
        log.info("Assigning fees to ALL students for year: {}, term: {}", year, term);
        
        List<Student> allStudents = studentRepository.findByIsActiveTrue();
        log.info("Found {} active students in school", allStudents.size());
        
        List<StudentFeeRecord> createdRecords = new ArrayList<>();
        
        for (Student student : allStudents) {
            Optional<StudentFeeRecord> existingRecord = getLatestFeeRecordForStudent(student.getId());
            
            StudentFeeRecord feeRecord;
            if (existingRecord.isPresent() && existingRecord.get().getYear().equals(year) && existingRecord.get().getTerm().equals(term)) {
                feeRecord = existingRecord.get();
            } else {
                feeRecord = new StudentFeeRecord();
                feeRecord.setStudent(student);
                feeRecord.setYear(year);
                feeRecord.setTerm(term);
            }
            
            feeRecord.setFeeCategory(feeCategory);
            feeRecord.setTuitionFee(tuitionFee);
            feeRecord.setBoardingFee(boardingFee);
            feeRecord.setDevelopmentLevy(developmentLevy);
            feeRecord.setExamFee(examFee);
            feeRecord.setOtherFees(otherFees);
            
            if (existingRecord.isEmpty()) {
                feeRecord.setPreviousBalance(BigDecimal.ZERO);
                feeRecord.setAmountPaid(BigDecimal.ZERO);
            }
            
            StudentFeeRecord saved = feeRecordRepository.save(feeRecord);
            createdRecords.add(saved);
        }
        
        log.info("Successfully assigned fees to {} students school-wide", createdRecords.size());
        return createdRecords;
    }

    /**
     * Assign fees to students in a specific class/form
     * 
     * LEARNING: Zimbabwe education structure support
     * - Primary: Grade 1 - Grade 7 (classes like "5A", "5B")
     * - Secondary: Form 1 - Form 6 (classes like "Form 3A", "Form 4B")
     * 
     * @param className The class name (e.g., "5A", "Form 3B")
     * @param termYear Term/year
     * @param feeCategory Fee category
     * @param tuitionFee Tuition amount
     * @param boardingFee Boarding amount
     * @param developmentLevy Development levy
     * @param examFee Exam fee
     * @param otherFees Other fees
     * @return List of created/updated fee records
     */
    @Transactional
    public List<StudentFeeRecord> assignFeesToClass(
            String className,
            Integer year,
            Integer term,
            String feeCategory,
            BigDecimal tuitionFee,
            BigDecimal boardingFee,
            BigDecimal developmentLevy,
            BigDecimal examFee,
            BigDecimal otherFees) {
        
        log.info("Assigning fees to class: {}, year: {}, term: {}", className, year, term);
        
        List<Student> students = studentRepository.findByClassName(className);
        log.info("Found {} students in class: {}", students.size(), className);
        
        List<StudentFeeRecord> createdRecords = new ArrayList<>();
        
        for (Student student : students) {
            if (!student.getIsActive()) {
                log.debug("Skipping inactive student: {}", student.getStudentId());
                continue;
            }
            
            Optional<StudentFeeRecord> existingRecord = getLatestFeeRecordForStudent(student.getId());
            
            StudentFeeRecord feeRecord;
            if (existingRecord.isPresent() && existingRecord.get().getYear().equals(year) && existingRecord.get().getTerm().equals(term)) {
                feeRecord = existingRecord.get();
                log.debug("Updating existing fee record for student: {}", student.getStudentId());
            } else {
                feeRecord = new StudentFeeRecord();
                feeRecord.setStudent(student);
                feeRecord.setYear(year);
                feeRecord.setTerm(term);
                log.debug("Creating new fee record for student: {}", student.getStudentId());
            }
            
            feeRecord.setFeeCategory(feeCategory);
            feeRecord.setTuitionFee(tuitionFee);
            feeRecord.setBoardingFee(boardingFee);
            feeRecord.setDevelopmentLevy(developmentLevy);
            feeRecord.setExamFee(examFee);
            feeRecord.setOtherFees(otherFees);
            
            if (existingRecord.isEmpty()) {
                feeRecord.setPreviousBalance(BigDecimal.ZERO);
                feeRecord.setAmountPaid(BigDecimal.ZERO);
            }
            
            StudentFeeRecord saved = feeRecordRepository.save(feeRecord);
            createdRecords.add(saved);
        }
        
        log.info("Successfully assigned fees to {} students in class: {}", createdRecords.size(), className);
        return createdRecords;
    }

    /**
     * Assign fees to specific grade and class combination
     * 
     * Example: Grade="Grade 5", className="5A"
     * Example: Grade="Form 3", className="Form 3B"
     * 
     * @param grade The grade level
     * @param className The class/stream
     * @return List of created/updated fee records
     */
    @Transactional
    public List<StudentFeeRecord> assignFeesToGradeAndClass(
            String grade,
            String className,
            Integer year,
            Integer term,
            String feeCategory,
            BigDecimal tuitionFee,
            BigDecimal boardingFee,
            BigDecimal developmentLevy,
            BigDecimal examFee,
            BigDecimal otherFees) {
        
        log.info("Assigning fees to grade: {}, class: {}, year: {}, term: {}", grade, className, year, term);
        
        List<Student> students = studentRepository.findByGradeAndClassName(grade, className);
        log.info("Found {} students in {} - {}", students.size(), grade, className);
        
        List<StudentFeeRecord> createdRecords = new ArrayList<>();
        
        for (Student student : students) {
            if (!student.getIsActive()) {
                continue;
            }
            
            Optional<StudentFeeRecord> existingRecord = getLatestFeeRecordForStudent(student.getId());
            
            StudentFeeRecord feeRecord;
            if (existingRecord.isPresent() && existingRecord.get().getYear().equals(year) && existingRecord.get().getTerm().equals(term)) {
                feeRecord = existingRecord.get();
            } else {
                feeRecord = new StudentFeeRecord();
                feeRecord.setStudent(student);
                feeRecord.setYear(year);
                feeRecord.setTerm(term);
            }
            
            feeRecord.setFeeCategory(feeCategory);
            feeRecord.setTuitionFee(tuitionFee);
            feeRecord.setBoardingFee(boardingFee);
            feeRecord.setDevelopmentLevy(developmentLevy);
            feeRecord.setExamFee(examFee);
            feeRecord.setOtherFees(otherFees);
            
            if (existingRecord.isEmpty()) {
                feeRecord.setPreviousBalance(BigDecimal.ZERO);
                feeRecord.setAmountPaid(BigDecimal.ZERO);
            }
            
            StudentFeeRecord saved = feeRecordRepository.save(feeRecord);
            createdRecords.add(saved);
        }
        
        log.info("Successfully assigned fees to {} students in {} - {}", 
                 createdRecords.size(), grade, className);
        return createdRecords;
    }

    /**
     * Assign fees to all forms (secondary school)
     * 
     * Zimbabwe secondary: Form 1, Form 2, Form 3, Form 4, Form 5, Form 6
     * 
     * @param forms List of forms (e.g., ["Form 1", "Form 2", "Form 3", "Form 4"])
     * @return List of all created/updated records
     */
    @Transactional
    public List<StudentFeeRecord> assignFeesToForms(
            List<String> forms,
            Integer year,
            Integer term,
            String feeCategory,
            BigDecimal tuitionFee,
            BigDecimal boardingFee,
            BigDecimal developmentLevy,
            BigDecimal examFee,
            BigDecimal otherFees) {
        
        log.info("Assigning fees to {} forms for year: {}, term: {}", forms.size(), year, term);
        
        List<StudentFeeRecord> allRecords = new ArrayList<>();
        
        for (String form : forms) {
            List<StudentFeeRecord> formRecords = assignFeesToGrade(
                form, year, term, feeCategory,
                tuitionFee, boardingFee, developmentLevy,
                examFee, otherFees
            );
            allRecords.addAll(formRecords);
        }
        
        log.info("Successfully assigned fees to {} students across {} forms",
                 allRecords.size(), forms.size());
        return allRecords;
    }

    // ========== MULTI-TENANT SCHOOL-AWARE METHODS ==========

    /**
     * Get all fee records for current school
     */
    public List<StudentFeeRecord> getAllFeeRecordsForCurrentSchool() {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching all fee records for school: {}", currentSchool.getSchoolName());
        return feeRecordRepository.findBySchool(currentSchool);
    }

    /**
     * Get fee record by ID for current school with security validation
     */
    public Optional<StudentFeeRecord> getFeeRecordByIdForCurrentSchool(Long id) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching fee record {} for school: {}", id, currentSchool.getSchoolName());
        
        return feeRecordRepository.findById(id)
                .filter(record -> {
                    SchoolContext.validateSchoolAccess(record.getSchool());
                    return true;
                });
    }

    /**
     * Get fee records by year and term for current school
     */
    public List<StudentFeeRecord> getFeeRecordsByYearAndTermForCurrentSchool(Integer year, Integer term) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching fee records for year {} term {} in school: {}", year, term, currentSchool.getSchoolName());
        return feeRecordRepository.findBySchoolAndYearAndTerm(currentSchool, year, term);
    }

    /**
     * Get fee records by payment status for current school
     */
    public List<StudentFeeRecord> getFeeRecordsByStatusForCurrentSchool(String status) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching fee records with status {} for school: {}", status, currentSchool.getSchoolName());
        return feeRecordRepository.findBySchoolAndPaymentStatus(currentSchool, status);
    }

    /**
     * Get fee records with outstanding balance for current school
     */
    public List<StudentFeeRecord> getArrearsForCurrentSchool() {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching arrears for school: {}", currentSchool.getSchoolName());
        return feeRecordRepository.findRecordsWithOutstandingBalanceBySchool(currentSchool);
    }

    /**
     * Get arrears by year and term for current school
     */
    public List<StudentFeeRecord> getArrearsByYearAndTermForCurrentSchool(Integer year, Integer term) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching arrears for year {} term {} in school: {}", year, term, currentSchool.getSchoolName());
        return feeRecordRepository.findBySchoolAndYearAndTermAndPaymentStatus(currentSchool, year, term, "ARREARS");
    }

    /**
     * Get students with scholarships for current school
     */
    public List<StudentFeeRecord> getScholarshipsForCurrentSchool() {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching scholarship students for school: {}", currentSchool.getSchoolName());
        return feeRecordRepository.findBySchoolAndHasScholarshipTrue(currentSchool);
    }

    /**
     * Calculate total outstanding fees for current school
     */
    public BigDecimal calculateTotalOutstandingForCurrentSchool() {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Calculating total outstanding for school: {}", currentSchool.getSchoolName());
        BigDecimal total = feeRecordRepository.calculateTotalOutstandingBalanceBySchool(currentSchool);
        log.info("Total outstanding for {}: {}", currentSchool.getSchoolName(), total);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Calculate total collected fees for current school
     */
    public BigDecimal calculateTotalCollectedForCurrentSchool() {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Calculating total collected for school: {}", currentSchool.getSchoolName());
        BigDecimal total = feeRecordRepository.calculateTotalAmountPaidBySchool(currentSchool);
        log.info("Total collected for {}: {}", currentSchool.getSchoolName(), total);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Calculate total scholarships for current school
     */
    public BigDecimal calculateTotalScholarshipsForCurrentSchool() {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Calculating total scholarships for school: {}", currentSchool.getSchoolName());
        BigDecimal total = feeRecordRepository.calculateTotalScholarshipsBySchool(currentSchool);
        log.info("Total scholarships for {}: {}", currentSchool.getSchoolName(), total);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Get collection rate for current school
     */
    public BigDecimal getCollectionRateForCurrentSchool() {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Calculating collection rate for school: {}", currentSchool.getSchoolName());
        
        BigDecimal totalGross = feeRecordRepository.calculateTotalGrossAmountBySchool(currentSchool);
        BigDecimal totalCollected = feeRecordRepository.calculateTotalAmountPaidBySchool(currentSchool);
        
        if (totalGross == null || totalGross.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Total gross amount is zero for school: {}", currentSchool.getSchoolName());
            return BigDecimal.ZERO;
        }
        
        BigDecimal rate = totalCollected
                .multiply(BigDecimal.valueOf(100))
                .divide(totalGross, 2, RoundingMode.HALF_UP);
        
        log.info("Collection rate for {}: {}%", currentSchool.getSchoolName(), rate);
        return rate;
    }

    /**
     * Count fee records by status for current school
     */
    public long countByStatusForCurrentSchool(String status) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Counting {} records for school: {}", status, currentSchool.getSchoolName());
        return feeRecordRepository.countBySchoolAndPaymentStatus(currentSchool, status);
    }

    /**
     * Create fee record for current school
     * Automatically assigns school from context
     */
    @Transactional
    public StudentFeeRecord createFeeRecordForCurrentSchool(StudentFeeRecord feeRecord) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Creating fee record for student {} in school: {}", 
                 feeRecord.getStudent().getId(), currentSchool.getSchoolName());
        
        // Validate student belongs to current school
        SchoolContext.validateSchoolAccess(feeRecord.getStudent().getSchool());
        
        // Auto-assign school
        feeRecord.setSchool(currentSchool);
        
        StudentFeeRecord saved = feeRecordRepository.save(feeRecord);
        log.info("Fee record created with ID: {}, Outstanding: {}", 
                 saved.getId(), saved.getOutstandingBalance());
        return saved;
    }

    /**
     * Update fee record for current school with security validation
     */
    @Transactional
    public StudentFeeRecord updateFeeRecordForCurrentSchool(Long id, StudentFeeRecord updatedRecord) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Updating fee record {} for school: {}", id, currentSchool.getSchoolName());
        
        return feeRecordRepository.findById(id)
                .map(existing -> {
                    // Security validation
                    SchoolContext.validateSchoolAccess(existing.getSchool());
                    
                    // Update fields (same as original updateFeeRecord)
                    if (updatedRecord.getTuitionFee() != null) {
                        existing.setTuitionFee(updatedRecord.getTuitionFee());
                    }
                    if (updatedRecord.getBoardingFee() != null) {
                        existing.setBoardingFee(updatedRecord.getBoardingFee());
                    }
                    if (updatedRecord.getDevelopmentLevy() != null) {
                        existing.setDevelopmentLevy(updatedRecord.getDevelopmentLevy());
                    }
                    if (updatedRecord.getExamFee() != null) {
                        existing.setExamFee(updatedRecord.getExamFee());
                    }
                    if (updatedRecord.getOtherFees() != null) {
                        existing.setOtherFees(updatedRecord.getOtherFees());
                    }
                    if (updatedRecord.getScholarshipAmount() != null) {
                        existing.setScholarshipAmount(updatedRecord.getScholarshipAmount());
                        existing.setHasScholarship(updatedRecord.getScholarshipAmount().compareTo(BigDecimal.ZERO) > 0);
                    }
                    if (updatedRecord.getSiblingDiscount() != null) {
                        existing.setSiblingDiscount(updatedRecord.getSiblingDiscount());
                    }
                    if (updatedRecord.getEarlyPaymentDiscount() != null) {
                        existing.setEarlyPaymentDiscount(updatedRecord.getEarlyPaymentDiscount());
                    }
                    if (updatedRecord.getFeeCategory() != null) {
                        existing.setFeeCategory(updatedRecord.getFeeCategory());
                    }
                    if (updatedRecord.getYear() != null) {
                        existing.setYear(updatedRecord.getYear());
                    }
                    if (updatedRecord.getTerm() != null) {
                        existing.setTerm(updatedRecord.getTerm());
                    }
                    if (updatedRecord.getBursarNotes() != null) {
                        existing.setBursarNotes(updatedRecord.getBursarNotes());
                    }
                    
                    StudentFeeRecord saved = feeRecordRepository.save(existing);
                    log.info("Fee record updated, new outstanding: {}", saved.getOutstandingBalance());
                    return saved;
                })
                .orElseThrow(() -> {
                    log.error("Fee record not found with ID: {}", id);
                    return new IllegalArgumentException("Fee record not found with ID: " + id);
                });
    }

    /**
     * Delete fee record for current school with security validation
     */
    @Transactional
    public void deleteFeeRecordForCurrentSchool(Long id) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Deleting fee record {} for school: {}", id, currentSchool.getSchoolName());
        
        StudentFeeRecord record = feeRecordRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Fee record not found with ID: {}", id);
                    return new IllegalArgumentException("Fee record not found with ID: " + id);
                });
        
        // Security validation
        SchoolContext.validateSchoolAccess(record.getSchool());
        
        feeRecordRepository.delete(record);
        log.info("Fee record {} deleted successfully", id);
    }

    // ========== BULK FEE ASSIGNMENT FOR CURRENT SCHOOL ==========

    /**
     * Assign fees to all students in a grade for current school
     */
    @Transactional
    public List<StudentFeeRecord> assignFeesToGradeForCurrentSchool(
            String grade,
            Integer year,
            Integer term,
            String feeCategory,
            String currency,
            BigDecimal tuitionFee,
            BigDecimal boardingFee,
            BigDecimal developmentLevy,
            BigDecimal examFee,
            BigDecimal otherFees) {
        
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Assigning fees to grade {} in school: {}", grade, currentSchool.getSchoolName());
        
        List<Student> students = studentRepository.findBySchoolAndGrade(currentSchool, grade);
        log.info("Found {} students in grade {} for school {}", 
                 students.size(), grade, currentSchool.getSchoolName());
        
        return assignFeesToStudentList(students, currentSchool, year, term, feeCategory, currency,
                                      tuitionFee, boardingFee, developmentLevy, examFee, otherFees);
    }

    /**
     * Assign fees to students in a class for current school
     */
    @Transactional
    public List<StudentFeeRecord> assignFeesToClassForCurrentSchool(
            String className,
            Integer year,
            Integer term,
            String feeCategory,
            String currency,
            BigDecimal tuitionFee,
            BigDecimal boardingFee,
            BigDecimal developmentLevy,
            BigDecimal examFee,
            BigDecimal otherFees) {
        
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Assigning fees to class {} in school: {}", className, currentSchool.getSchoolName());
        
        List<Student> students = studentRepository.findBySchoolAndClassName(currentSchool, className);
        log.info("Found {} students in class {} for school {}",
                 students.size(), className, currentSchool.getSchoolName());
        
        return assignFeesToStudentList(students, currentSchool, year, term, feeCategory, currency,
                                      tuitionFee, boardingFee, developmentLevy, examFee, otherFees);
    }

    /**
     * Assign fees to grade and class combination for current school
     */
    @Transactional
    public List<StudentFeeRecord> assignFeesToGradeAndClassForCurrentSchool(
            String grade,
            String className,
            Integer year,
            Integer term,
            String feeCategory,
            String currency,
            BigDecimal tuitionFee,
            BigDecimal boardingFee,
            BigDecimal developmentLevy,
            BigDecimal examFee,
            BigDecimal otherFees) {
        
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Assigning fees to {} - {} in school: {}", 
                 grade, className, currentSchool.getSchoolName());
        
        List<Student> students = studentRepository.findBySchoolAndGradeAndClassName(
                currentSchool, grade, className);
        log.info("Found {} students in {} - {} for school {}",
                 students.size(), grade, className, currentSchool.getSchoolName());
        
        return assignFeesToStudentList(students, currentSchool, year, term, feeCategory, currency,
                                      tuitionFee, boardingFee, developmentLevy, examFee, otherFees);
    }

    /**
     * Assign fees to all students in current school
     */
    @Transactional
    public List<StudentFeeRecord> assignFeesToAllStudentsForCurrentSchool(
            Integer year,
            Integer term,
            String feeCategory,
            String currency,
            BigDecimal tuitionFee,
            BigDecimal boardingFee,
            BigDecimal developmentLevy,
            BigDecimal examFee,
            BigDecimal otherFees) {
        
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Assigning fees to ALL students in school: {}", currentSchool.getSchoolName());
        
        List<Student> students = studentRepository.findBySchool(currentSchool, Pageable.unpaged()).getContent();
        log.info("Found {} students in school {}", students.size(), currentSchool.getSchoolName());
        
        return assignFeesToStudentList(students, currentSchool, year, term, feeCategory, currency,
                                      tuitionFee, boardingFee, developmentLevy, examFee, otherFees);
    }

    /**
     * Helper method to assign fees to a list of students
     * Reduces code duplication in bulk assignment methods
     */
    private List<StudentFeeRecord> assignFeesToStudentList(
            List<Student> students,
            School school,
            Integer year,
            Integer term,
            String feeCategory,
            String currency,
            BigDecimal tuitionFee,
            BigDecimal boardingFee,
            BigDecimal developmentLevy,
            BigDecimal examFee,
            BigDecimal otherFees) {
        
        List<StudentFeeRecord> createdRecords = new ArrayList<>();
        
        for (Student student : students) {
            if (!student.getIsActive()) {
                log.debug("Skipping inactive student: {}", student.getStudentId());
                continue;
            }
            
            // Check if student already has fee record for this term and school
            Optional<StudentFeeRecord> existingRecord = feeRecordRepository
                    .findByStudentId(student.getId())
                    .stream()
                    .filter(r -> r.getYear().equals(year) && r.getTerm().equals(term) && r.getSchool().equals(school))
                    .findFirst();
            
            StudentFeeRecord feeRecord;
            if (existingRecord.isPresent()) {
                // Update existing record
                feeRecord = existingRecord.get();
                log.debug("Updating existing fee record for student: {}", student.getStudentId());
            } else {
                // Create new record
                feeRecord = new StudentFeeRecord();
                feeRecord.setSchool(school);
                feeRecord.setStudent(student);
                feeRecord.setYear(year);
                feeRecord.setTerm(term);
                feeRecord.setPreviousBalance(BigDecimal.ZERO);
                feeRecord.setAmountPaid(BigDecimal.ZERO);
                log.debug("Creating new fee record for student: {}", student.getStudentId());
            }
            
            // Set fee amounts
            feeRecord.setFeeCategory(feeCategory);
            feeRecord.setCurrency(currency);
            feeRecord.setTuitionFee(tuitionFee);
            feeRecord.setBoardingFee(boardingFee);
            feeRecord.setDevelopmentLevy(developmentLevy);
            feeRecord.setExamFee(examFee);
            feeRecord.setOtherFees(otherFees);
            
            // Save (calculateTotals runs automatically)
            StudentFeeRecord saved = feeRecordRepository.save(feeRecord);
            createdRecords.add(saved);
        }
        
        log.info("Successfully assigned fees to {} students", createdRecords.size());
        return createdRecords;
    }
    
    /**
     * Create fee record for promoted student
     * 
     * PURPOSE: Generate new fee record when student is promoted to next grade
     * 
     * LEARNING: Promotion fee record workflow
     * 1. Previous term's fee record remains unchanged (historical data)
     * 2. Create new fee record for new academic year/term
     * 3. Optionally carry forward outstanding balance from previous term
     * 4. Apply new fee structure for the promoted grade
     * 
     * @param student Student being promoted
     * @param termYear New academic term/year (e.g., "Term 1 2026")
     * @param previousBalance Outstanding balance from previous term (can be null/zero)
     * @param tuitionFee New tuition fee amount
     * @param boardingFee New boarding fee amount
     * @param developmentLevy New development levy
     * @param examFee New exam fee
     * @param otherFees Other fees
     * @param scholarshipAmount Scholarship to apply
     * @param siblingDiscount Sibling discount to apply
     * @param feeCategory Fee category (BOARDING, DAY_SCHOLAR, etc.)
     * @return Created fee record
     */
    @Transactional
    public StudentFeeRecord createPromotionFeeRecord(
            Student student,
            Integer year,
            Integer term,
            BigDecimal previousBalance,
            BigDecimal tuitionFee,
            BigDecimal boardingFee,
            BigDecimal developmentLevy,
            BigDecimal examFee,
            BigDecimal otherFees,
            BigDecimal scholarshipAmount,
            BigDecimal siblingDiscount,
            String feeCategory) {
        
        log.info("Creating promotion fee record for student: {}, Term: {}, Previous balance: {}", 
            student.getStudentId(), term, previousBalance);
        
        // Create new fee record (OneToMany relationship allows multiple records per student)
        StudentFeeRecord feeRecord = new StudentFeeRecord();
        feeRecord.setStudent(student);
        feeRecord.setSchool(student.getSchool());
        feeRecord.setYear(year);
        feeRecord.setTerm(term);
        feeRecord.setFeeCategory(feeCategory != null ? feeCategory : "STANDARD");
        
        // Set fee components
        feeRecord.setTuitionFee(tuitionFee != null ? tuitionFee : BigDecimal.ZERO);
        feeRecord.setBoardingFee(boardingFee != null ? boardingFee : BigDecimal.ZERO);
        feeRecord.setDevelopmentLevy(developmentLevy != null ? developmentLevy : BigDecimal.ZERO);
        feeRecord.setExamFee(examFee != null ? examFee : BigDecimal.ZERO);
        feeRecord.setOtherFees(otherFees != null ? otherFees : BigDecimal.ZERO);
        
        // Set discounts
        feeRecord.setScholarshipAmount(scholarshipAmount != null ? scholarshipAmount : BigDecimal.ZERO);
        feeRecord.setSiblingDiscount(siblingDiscount != null ? siblingDiscount : BigDecimal.ZERO);
        feeRecord.setHasScholarship(scholarshipAmount != null && scholarshipAmount.compareTo(BigDecimal.ZERO) > 0);
        
        // Carry forward previous balance if provided
        if (previousBalance != null && previousBalance.compareTo(BigDecimal.ZERO) > 0) {
            feeRecord.setPreviousBalance(previousBalance);
            log.info("Carrying forward previous balance: {} for student: {}", previousBalance, student.getStudentId());
        } else {
            feeRecord.setPreviousBalance(BigDecimal.ZERO);
        }
        
        // Initialize payment fields for new term
        feeRecord.setAmountPaid(BigDecimal.ZERO);
        
        // Save (calculateTotals runs automatically via @PrePersist)
        StudentFeeRecord saved = feeRecordRepository.save(feeRecord);
        
        log.info("Created promotion fee record - Student: {}, Year: {}, Term: {}, Net Amount: {}, Outstanding: {}", 
            student.getStudentId(), year, term, saved.getNetAmount(), saved.getOutstandingBalance());
        
        // Audit trail
        auditTrailService.logAction(
            null,
            "SYSTEM",
            "CREATE_PROMOTION_FEE_RECORD",
            "StudentFeeRecord",
            saved.getId().toString(),
            String.format("Created promotion fee record for student %s (ID: %s), Year: %s, Term: %s, Amount: %s, Previous Balance: %s",
                student.getFirstName() + " " + student.getLastName(),
                student.getStudentId(),
                year,
                term,
                saved.getNetAmount(),
                previousBalance != null ? previousBalance : BigDecimal.ZERO)
        );
        
        return saved;
    }
}
