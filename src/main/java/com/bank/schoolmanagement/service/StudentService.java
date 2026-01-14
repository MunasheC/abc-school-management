    
package com.bank.schoolmanagement.service;

import com.bank.schoolmanagement.context.SchoolContext;
import com.bank.schoolmanagement.entity.Guardian;
import com.bank.schoolmanagement.entity.Payment;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.entity.StudentFeeRecord;
import com.bank.schoolmanagement.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Student Service - Business Logic Layer (REFACTORED)
 * 
 * KEY CONCEPTS:
 * @Service - Marks this as a service component for Spring's dependency injection
 * @RequiredArgsConstructor - Lombok generates constructor for final fields (dependency injection)
 * @Slf4j - Lombok provides a logger instance (log) for logging
 * @Transactional - Ensures database operations are atomic (all-or-nothing)
 * 
 * WHY SERVICE LAYER?
 * - Separates business logic from HTTP handling (Controller)
 * - Separates business logic from data access (Repository)
 * - Makes code testable and maintainable
 * - Can be reused by multiple controllers or other services
 * 
 * REFACTORED DESIGN:
 * - Now works with Guardian, StudentFeeRecord, Payment entities
 * - Uses relationship-aware queries (JOIN FETCH) for performance
 * - Delegates financial operations to StudentFeeRecordService and PaymentService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    /**
     * Dependency Injection (UPDATED)
     * 
     * Now injects multiple services to work with related entities
     * - StudentRepository: Student data access
     * - GuardianService: Guardian/parent operations
     * - PaymentService: Payment transactions
     * 
     * Note: For fee record operations, use StudentFeeRecordService directly in controllers
     */
    private final StudentRepository studentRepository;
    private final GuardianService guardianService;
    private final PaymentService paymentService;
    private final AuditTrailService auditTrailService;
    private final AcademicProgressionService academicProgressionService;
    private final StudentFeeRecordService studentFeeRecordService;

    /**
     * Create a new student
     * 
     * LEARNING: Creating student with relationships
     * If student has a guardian, it must be saved separately first
     * The guardian relationship is set using student.setGuardian()
     * 
     * @Transactional ensures if any error occurs, all changes are rolled back
     */
    @Transactional
    public Student createStudent(Student student) {
        log.info("Creating new student: {} {}", student.getFirstName(), student.getLastName());
        
        // Check if studentId already exists (if provided)
        if (student.getStudentId() != null && 
            !student.getStudentId().isEmpty() && 
            studentRepository.existsByStudentId(student.getStudentId())) {
            
            log.warn("Student ID {} already exists", student.getStudentId());
            throw new IllegalArgumentException("Student ID already exists: " + student.getStudentId());
        }
        
        // If guardian is provided but not saved, save it first
        if (student.getGuardian() != null && student.getGuardian().getId() == null) {
            Guardian guardian = guardianService.createOrGetGuardian(student.getGuardian());
            student.setGuardian(guardian);
            log.info("Guardian created/retrieved with ID: {}", guardian.getId());
        }
        
        // The @PrePersist method in Student entity will generate ID if needed
        Student savedStudent = studentRepository.save(student);
        log.info("Student created successfully with ID: {}", savedStudent.getId());
        
        // Audit trail
        auditTrailService.logAction(
            null,
            "SYSTEM",
            "CREATE_STUDENT",
            "Student",
            savedStudent.getId() != null ? savedStudent.getId().toString() : null,
            String.format("Created student: %s %s (ID: %s)",
                savedStudent.getFirstName(), savedStudent.getLastName(), savedStudent.getStudentId())
        );
        
        return savedStudent;
    }

    /**
     * Get student by database ID
     */
    public Optional<Student> getStudentById(Long id) {
        log.debug("Fetching student with ID: {}", id);
        return studentRepository.findById(id);
    }

    /**
     * Get student by student ID (school's ID)
     */
    public Optional<Student> getStudentByStudentId(String studentId) {
        log.debug("Fetching student with student ID: {}", studentId);
        return studentRepository.findByStudentId(studentId);
    }

    /**
     * Get all students
     */
    public List<Student> getAllStudents() {
        log.debug("Fetching all students");
        return studentRepository.findAll();
    }

    /**
     * Get all active students
     */
    public List<Student> getActiveStudents() {
        log.debug("Fetching active students");
        return studentRepository.findByIsActiveTrue();
    }

    /**
     * Get students by grade
     */
    public List<Student> getStudentsByGrade(String grade) {
        log.debug("Fetching students in grade: {}", grade);
        return studentRepository.findByGrade(grade);
    }

    /**
     * Search students by name
     */
    public List<Student> searchStudentsByName(String searchTerm) {
        log.debug("Searching students with term: {}", searchTerm);
        return studentRepository.searchByName(searchTerm);
    }

    /**
     * Get students by guardian phone (useful for bank integration)
     * 
     * LEARNING: This uses the new Guardian relationship
     * Returns all siblings (students sharing the same guardian)
     */
    public List<Student> getStudentsByGuardianPhone(String guardianPhone) {
        log.debug("Fetching students with guardian phone: {}", guardianPhone);
        return studentRepository.findByGuardianPhone(guardianPhone);
    }

    /**
     * Get students enrolled after a specific date
     */
    public List<Student> getStudentsEnrolledAfter(LocalDate date) {
        log.debug("Fetching students enrolled after: {}", date);
        return studentRepository.findStudentsEnrolledAfter(date);
    }

    /**
     * Update student information
     * 
     * LEARNING: Guardian updates
     * Parent/guardian info is now in separate Guardian entity
     * To update guardian, use GuardianService.updateGuardian()
     */
    @Transactional
    public Student updateStudent(Long id, Student updatedStudent) {
        log.info("Updating student with ID: {}", id);
        
        return studentRepository.findById(id)
                .map(existingStudent -> {
                    // Capture before state
                    String beforeValue = String.format("Name: %s %s, Grade: %s, Class: %s, Active: %s",
                        existingStudent.getFirstName(), existingStudent.getLastName(),
                        existingStudent.getGrade(), existingStudent.getClassName(),
                        existingStudent.getIsActive());
                    
                    // Update student fields (only non-null values)
                    if (updatedStudent.getFirstName() != null) {
                        existingStudent.setFirstName(updatedStudent.getFirstName());
                    }
                    if (updatedStudent.getMiddleName() != null) {
                        existingStudent.setMiddleName(updatedStudent.getMiddleName());
                    }
                    if (updatedStudent.getLastName() != null) {
                        existingStudent.setLastName(updatedStudent.getLastName());
                    }
                    if (updatedStudent.getDateOfBirth() != null) {
                        existingStudent.setDateOfBirth(updatedStudent.getDateOfBirth());
                    }
                    if (updatedStudent.getGender() != null) {
                        existingStudent.setGender(updatedStudent.getGender());
                    }
                    if (updatedStudent.getNationalId() != null) {
                        existingStudent.setNationalId(updatedStudent.getNationalId());
                    }
                    if (updatedStudent.getGrade() != null) {
                        existingStudent.setGrade(updatedStudent.getGrade());
                    }
                    if (updatedStudent.getClassName() != null) {
                        existingStudent.setClassName(updatedStudent.getClassName());
                    }
                    if (updatedStudent.getAdmissionNumber() != null) {
                        existingStudent.setAdmissionNumber(updatedStudent.getAdmissionNumber());
                    }
                    if (updatedStudent.getNotes() != null) {
                        existingStudent.setNotes(updatedStudent.getNotes());
                    }
                    if (updatedStudent.getIsActive() != null) {
                        existingStudent.setIsActive(updatedStudent.getIsActive());
                    }
                    
                    // Update guardian relationship if provided
                    if (updatedStudent.getGuardian() != null) {
                        existingStudent.setGuardian(updatedStudent.getGuardian());
                    }
                    
                    Student savedStudent = studentRepository.save(existingStudent);
                    log.info("Student updated successfully");
                    
                    // Capture after state
                    String afterValue = String.format("Name: %s %s, Grade: %s, Class: %s, Active: %s",
                        savedStudent.getFirstName(), savedStudent.getLastName(),
                        savedStudent.getGrade(), savedStudent.getClassName(),
                        savedStudent.getIsActive());
                    
                    // Audit trail
                    auditTrailService.logAction(
                        null,
                        "SYSTEM",
                        "UPDATE_STUDENT",
                        "Student",
                        savedStudent.getId().toString(),
                        String.format("Updated student: %s %s (ID: %s)",
                            savedStudent.getFirstName(), savedStudent.getLastName(), savedStudent.getStudentId()),
                        beforeValue,
                        afterValue
                    );
                    
                    return savedStudent;
                })
                .orElseThrow(() -> {
                    log.error("Student not found with ID: {}", id);
                    return new IllegalArgumentException("Student not found with ID: " + id);
                });
    }

    @Transactional
    public Student updateStudentByStudentIdForCurrentSchool(String studentId, Student updatedStudent) {
        Long schoolId = SchoolContext.getCurrentSchoolId();
        Student existingStudent = studentRepository.findByStudentId(studentId)
                .filter(s -> s.getSchool().getId().equals(schoolId))
                .orElseThrow(() -> new IllegalArgumentException("Student not found for studentId: " + studentId));
        return updateStudent(existingStudent.getId(), updatedStudent);
    }

    @Transactional
    public Student updateStudentByNationalIdForCurrentSchool(String nationalId, Student updatedStudent) {
        Long schoolId = SchoolContext.getCurrentSchoolId();
        Student existingStudent = studentRepository.findByNationalId(nationalId)
                .filter(s -> s.getSchool().getId().equals(schoolId))
                .orElseThrow(() -> new IllegalArgumentException("Student not found for nationalId: " + nationalId));
        return updateStudent(existingStudent.getId(), updatedStudent);
    }

    /**
     * Delete student (hard delete)
     */
    @Transactional
    public void deleteStudent(Long id) {
        log.info("Deleting student with ID: {}", id);
        
        if (!studentRepository.existsById(id)) {
            log.error("Student not found with ID: {}", id);
            throw new IllegalArgumentException("Student not found with ID: " + id);
        }
        
        Student student = studentRepository.findById(id).orElse(null);
        
        studentRepository.deleteById(id);
        log.info("Student deleted successfully");
        
        // Audit trail
        if (student != null) {
            auditTrailService.logAction(
                null,
                "SYSTEM",
                "DELETE_STUDENT",
                "Student",
                id.toString(),
                String.format("Deleted student: %s %s (ID: %s)",
                    student.getFirstName(), student.getLastName(), student.getStudentId())
            );
        }
    }

    /**
     * Deactivate student (soft delete - just mark as inactive)
     * Better for data integrity - you keep historical records
     */
    @Transactional
    public Student deactivateStudent(Long id) {
        log.info("Deactivating student with ID: {}", id);
        
        return studentRepository.findById(id)
                .map(student -> {
                    student.setIsActive(false);
                    Student savedStudent = studentRepository.save(student);
                    log.info("Student deactivated successfully");
                    
                    // Audit trail
                    auditTrailService.logAction(
                        null,
                        "SYSTEM",
                        "DEACTIVATE_STUDENT",
                        "Student",
                        savedStudent.getId().toString(),
                        String.format("Deactivated student: %s %s (ID: %s)",
                            savedStudent.getFirstName(), savedStudent.getLastName(), savedStudent.getStudentId())
                    );
                    
                    return savedStudent;
                })
                .orElseThrow(() -> {
                    log.error("Student not found with ID: {}", id);
                    return new IllegalArgumentException("Student not found with ID: " + id);
                });
    }

    /**
     * Reactivate student
     */
    @Transactional
    public Student reactivateStudent(Long id) {
        log.info("Reactivating student with ID: {}", id);
        
        return studentRepository.findById(id)
                .map(student -> {
                    student.setIsActive(true);
                    Student savedStudent = studentRepository.save(student);
                    log.info("Student reactivated successfully");
                    
                    // Audit trail
                    auditTrailService.logAction(
                        null,
                        "SYSTEM",
                        "REACTIVATE_STUDENT",
                        "Student",
                        savedStudent.getId().toString(),
                        String.format("Reactivated student: %s %s (ID: %s)",
                            savedStudent.getFirstName(), savedStudent.getLastName(), savedStudent.getStudentId())
                    );
                    
                    return savedStudent;
                })
                .orElseThrow(() -> {
                    log.error("Student not found with ID: {}", id);
                    return new IllegalArgumentException("Student not found with ID: " + id);
                });
    }

    /**
     * Get count of students by grade
     */
    public long countStudentsByGrade(String grade) {
        log.debug("Counting students in grade: {}", grade);
        return studentRepository.countByGrade(grade);
    }

    /**
     * Get total number of active students
     */
    public long countActiveStudents() {
        log.debug("Counting active students");
        return studentRepository.findByIsActiveTrue().size();
    }

    /* ----------------------  RELATIONSHIP-AWARE METHODS (NEW)  ------------------------- */

    /**
     * Get student with guardian loaded
     * 
     * LEARNING: JOIN FETCH optimization
     * Loads student + guardian in ONE database query
     * Without this, you'd need 2 queries (N+1 problem)
     */
    public Optional<Student> getStudentWithGuardian(Long id) {
        log.debug("Fetching student {} with guardian", id);
        return studentRepository.findByIdWithGuardian(id);
    }

    /**
     * Get student with fee record loaded
     */
    public Optional<Student> getStudentWithFeeRecord(Long id) {
        log.debug("Fetching student {} with fee record", id);
        return studentRepository.findByIdWithFeeRecord(id);
    }

    /**
     * Get student with all relationships loaded
     * 
     * LEARNING: Multiple JOIN FETCH
     * Loads student + guardian + fee record + payments in ONE query
     * Very efficient when you need all related data
     */
    public Optional<Student> getStudentWithAllRelationships(Long id) {
        log.debug("Fetching student {} with all relationships", id);
        return studentRepository.findByIdWithAllRelationships(id);
    }

    /**
     * Get siblings (students sharing the same guardian)
     */
    public List<Student> getSiblings(Long guardianId) {
        log.debug("Fetching siblings for guardian ID: {}", guardianId);
        return studentRepository.findByGuardianId(guardianId);
    }

    /**
     * Withdraw student
     * 
     * LEARNING: Uses the withdraw() method from Student entity
     * Sets isActive=false, records withdrawal date and reason
     */
    @Transactional
    public Student withdrawStudent(Long id, String reason) {
        log.info("Withdrawing student with ID: {}, Reason: {}", id, reason);
        
        return studentRepository.findById(id)
                .map(student -> {
                    student.withdraw(reason);
                    Student saved = studentRepository.save(student);
                    log.info("Student withdrawn successfully");
                    return saved;
                })
                .orElseThrow(() -> {
                    log.error("Student not found with ID: {}", id);
                    return new IllegalArgumentException("Student not found with ID: " + id);
                });
    }

    /* ----------------------  FINANCIAL METHODS (DELEGATED)  ------------------------- */

    /**
     * Record a payment for a student
     * 
     * LEARNING: Payment delegation
     * Financial operations are now handled by PaymentService and StudentFeeRecordService
     * This keeps single responsibility - StudentService focuses on student operations
     * 
     * @param studentId - The student's ID (database ID)
     * @param payment - Payment object with amount, method, etc.
     * @return Created payment record
     */
    @Transactional
    public Payment recordPayment(Long studentId, Payment payment) {
        log.info("Recording payment of {} for student ID: {}", payment.getAmount(), studentId);
        return paymentService.recordPayment(studentId, payment);
    }

    /**
     * Get student's outstanding balance
     * 
     * LEARNING: Uses the helper method from Student entity
     * Returns ZERO if no fee record exists
     */
    public BigDecimal getStudentOutstandingBalance(Long studentId) {
        log.debug("Getting outstanding balance for student ID: {}", studentId);
        
        return studentRepository.findById(studentId)
                .map(Student::getOutstandingBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Check if student has outstanding balance
     */
    public boolean hasOutstandingBalance(Long studentId) {
        log.debug("Checking if student {} has outstanding balance", studentId);
        
        return studentRepository.findById(studentId)
                .map(Student::hasOutstandingBalance)
                .orElse(false);
    }

    /**
     * Get student's payment status
     */
    public String getStudentPaymentStatus(Long studentId) {
        log.debug("Getting payment status for student ID: {}", studentId);
        
        return studentRepository.findById(studentId)
                .map(Student::getPaymentStatus)
                .orElse("UNKNOWN");
    }

    /* ----------------------  MULTI-TENANT METHODS (School-Aware)  ------------------------- */

    /**
     * Get all students for current school
     * 
     * CRITICAL: Multi-tenant data isolation
     * - Uses SchoolContext to get current school
     * - Returns only students from current school
     * - School users automatically see only their students
     * @param pageable 
     */
    public Page<Student> getAllStudentsForCurrentSchool(Pageable pageable) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching all students for school: {}", currentSchool.getSchoolName());
        Page<Student> students = studentRepository.findBySchool(currentSchool, pageable);
        return students;
    }

    /**
     * Get student by ID with school validation
     * 
     * SECURITY: Validates student belongs to current school
     * @throws SecurityException if student belongs to different school
     */
    public Optional<Student> getStudentByIdForCurrentSchool(Long id) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching student {} for school: {}", id, currentSchool.getSchoolName());
        
        return studentRepository.findById(id)
                .flatMap(student -> {
                    try {
                        SchoolContext.validateSchoolAccess(student.getSchool());
                        return Optional.of(student);
                    } catch (SecurityException se) {
                        // Treat as not found within current school
                        log.debug("Access denied when fetching student {}: {}", id, se.getMessage());
                        return Optional.empty();
                    }
                });
    }

    /**
     * Get student by student ID within current school
     */
    public Optional<Student> getStudentByStudentIdForCurrentSchool(String studentId) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching student {} in school: {}", studentId, currentSchool.getSchoolName());
        return studentRepository.findBySchoolAndStudentId(currentSchool, studentId);
    }

    /**
     * Get students by grade within current school
     */
    public List<Student> getStudentsByGradeForCurrentSchool(String grade) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching grade {} students for school: {}", grade, currentSchool.getSchoolName());
        return studentRepository.findBySchoolAndGrade(currentSchool, grade);
    }

    /**
     * Get students by class name within current school
     */
    public List<Student> getStudentsByClassNameForCurrentSchool(String className) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching {} students for school: {}", className, currentSchool.getSchoolName());
        return studentRepository.findBySchoolAndClassName(currentSchool, className);
    }

    /**
     * Get students by grade and class within current school
     */
    public List<Student> getStudentsByGradeAndClassForCurrentSchool(String grade, String className) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching grade {} class {} for school: {}", 
                 grade, className, currentSchool.getSchoolName());
        return studentRepository.findBySchoolAndGradeAndClassName(currentSchool, grade, className);
    }

    /**
     * Search students by name within current school
     */
    public List<Student> searchStudentsByNameForCurrentSchool(String searchTerm) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Searching students with '{}' in school: {}", 
                 searchTerm, currentSchool.getSchoolName());
        return studentRepository.searchBySchoolAndName(currentSchool, searchTerm);
    }

    /**
     * Count students in current school
     */
    public long countStudentsForCurrentSchool() {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Counting students for school: {}", currentSchool.getSchoolName());
        return studentRepository.countBySchool(currentSchool);
    }

    /**
     * Count students by grade in current school
     */
    public long countStudentsByGradeForCurrentSchool(String grade) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Counting grade {} students in school: {}", 
                 grade, currentSchool.getSchoolName());
        return studentRepository.countBySchoolAndGrade(currentSchool, grade);
    }

    /**
     * Create student in current school
     * 
     * CRITICAL: Automatically assigns current school to student
     * - No need to manually set school in controller
     * - Ensures student always belongs to current school
     */
    @Transactional
    public Student createStudentForCurrentSchool(Student student) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Creating student {} {} in school: {}", 
                student.getFirstName(), student.getLastName(), currentSchool.getSchoolName());
        
        // Set school automatically
        student.setSchool(currentSchool);
        
        // Check if studentId exists in this school
        if (student.getStudentId() != null && 
            !student.getStudentId().isEmpty() && 
            studentRepository.existsBySchoolAndStudentId(currentSchool, student.getStudentId())) {
            
            log.warn("Student ID {} already exists in school {}", 
                    student.getStudentId(), currentSchool.getSchoolName());
            throw new IllegalArgumentException("Student ID already exists in this school: " + 
                                             student.getStudentId());
        }
        
        // Handle guardian
        if (student.getGuardian() != null) {
            // If no ID provided, create or get by phone within current school
            if (student.getGuardian().getId() == null) {
                Guardian guardian = student.getGuardian();
                guardian.setSchool(currentSchool);  // Set school on guardian too
                Guardian savedGuardian = guardianService.createOrGetGuardian(guardian);
                student.setGuardian(savedGuardian);
                log.info("Guardian created/retrieved with ID: {}", savedGuardian.getId());
            } else {
                // If client provided an ID, load a managed Guardian entity and validate school
                Long gid = student.getGuardian().getId();
                Guardian existingGuardian = guardianService.getGuardianById(gid)
                        .orElseThrow(() -> new IllegalArgumentException("Guardian not found with ID: " + gid));

                // Ensure the guardian belongs to the current school (prevent cross-tenant linking)
                SchoolContext.validateSchoolAccess(existingGuardian.getSchool());

                // Attach the managed guardian to student to avoid detached-entity errors
                student.setGuardian(existingGuardian);
                log.info("Linked student to existing guardian ID: {}", gid);
            }
        }
        
        Student savedStudent = studentRepository.save(student);
        log.info("Student created successfully with ID: {} in school: {}", 
                savedStudent.getId(), currentSchool.getSchoolName());
        
        // Update school student count
        currentSchool.incrementStudentCount();
        
        return savedStudent;
    }

    /**
     * Update student in current school
     * 
     * SECURITY: Validates student belongs to current school before updating
     */
    @Transactional
    public Student updateStudentForCurrentSchool(Long id, Student updatedStudent) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Updating student {} in school: {}", id, currentSchool.getSchoolName());
        
        return studentRepository.findById(id)
                .map(existingStudent -> {
                    // Validate school ownership (convert security exception to IllegalArgumentException)
                    try {
                        SchoolContext.validateSchoolAccess(existingStudent.getSchool());
                    } catch (SecurityException se) {
                        log.warn("Unauthorized update attempt for student {}: {}", id, se.getMessage());
                        throw new IllegalArgumentException("Student not found with ID: " + id);
                    }

                    // Update fields (same as before)
                    if (updatedStudent.getFirstName() != null) {
                        existingStudent.setFirstName(updatedStudent.getFirstName());
                    }
                    if (updatedStudent.getMiddleName() != null) {
                        existingStudent.setMiddleName(updatedStudent.getMiddleName());
                    }
                    if (updatedStudent.getLastName() != null) {
                        existingStudent.setLastName(updatedStudent.getLastName());
                    }
                    if (updatedStudent.getDateOfBirth() != null) {
                        existingStudent.setDateOfBirth(updatedStudent.getDateOfBirth());
                    }
                    if (updatedStudent.getGender() != null) {
                        existingStudent.setGender(updatedStudent.getGender());
                    }
                    if (updatedStudent.getNationalId() != null) {
                        existingStudent.setNationalId(updatedStudent.getNationalId());
                    }
                    if (updatedStudent.getGrade() != null) {
                        existingStudent.setGrade(updatedStudent.getGrade());
                    }
                    if (updatedStudent.getClassName() != null) {
                        existingStudent.setClassName(updatedStudent.getClassName());
                    }
                    if (updatedStudent.getAdmissionNumber() != null) {
                        existingStudent.setAdmissionNumber(updatedStudent.getAdmissionNumber());
                    }
                    
                    Student saved = studentRepository.save(existingStudent);
                    log.info("Student {} updated in school: {}", 
                            id, currentSchool.getSchoolName());
                    return saved;
                })
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + id));
    }

    /**
     * Delete student from current school
     * 
     * SECURITY: Validates student belongs to current school before deleting
     */
    @Transactional
    public void deleteStudentForCurrentSchool(Long id) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Deleting student {} from school: {}", id, currentSchool.getSchoolName());
        
        studentRepository.findById(id).ifPresentOrElse(student -> {
            try {
                // Validate school ownership
                SchoolContext.validateSchoolAccess(student.getSchool());

                studentRepository.deleteById(id);

                // Update school student count
                currentSchool.decrementStudentCount();

                log.info("Student {} deleted from school: {}", id, currentSchool.getSchoolName());
            } catch (SecurityException se) {
                log.warn("Unauthorized delete attempt for student {}: {}", id, se.getMessage());
                throw new IllegalArgumentException("Student not found with ID: " + id);
            }
        }, () -> {
            throw new IllegalArgumentException("Student not found with ID: " + id);
        });
    }

    /**
     * Promote student to next grade/form
     * 
     * PURPOSE: Handle grade progression at year-end or mid-year
     * 
     * LEARNING: Student promotion workflow
     * 1. Update student's grade and className
     * 2. Previous fee records remain unchanged (historical data)
     * 3. Controller/service creates new fee record for new academic year
     * 4. Can optionally carry forward outstanding balance
     * 
     * USAGE EXAMPLES:
     * - Year-end: Form 1 → Form 2
     * - Mid-year transfer: Grade 5A → Grade 5B
     * - Repeating: Form 3 → Form 3 (new academic year)
     * 
     * @param studentId Student to promote
     * @param newGrade New grade/form level
     * @param newClassName New class within grade
     * @param promotionNotes Optional notes about promotion
     * @return Updated student
     */
    @Transactional
    public Student promoteStudent(Long studentId, String newGrade, String newClassName, String promotionNotes) {
        log.info("Promoting student ID {} to grade: {}, class: {}", studentId, newGrade, newClassName);
        
        return studentRepository.findById(studentId)
                .map(student -> {
                    String oldGrade = student.getGrade();
                    String oldClassName = student.getClassName();
                    
                    // Capture before state
                    String beforeValue = String.format("Grade: %s, Class: %s", oldGrade, oldClassName);
                    
                    // Update student grade information
                    student.setGrade(newGrade);
                    student.setClassName(newClassName);
                    
                    // Add promotion notes if provided
                    if (promotionNotes != null && !promotionNotes.isBlank()) {
                        String currentNotes = student.getNotes() != null ? student.getNotes() : "";
                        String updatedNotes = currentNotes.isBlank() 
                            ? promotionNotes 
                            : currentNotes + "\n[" + LocalDate.now() + "] " + promotionNotes;
                        student.setNotes(updatedNotes);
                    }
                    
                    Student promoted = studentRepository.save(student);
                    
                    // Capture after state
                    String afterValue = String.format("Grade: %s, Class: %s", newGrade, newClassName);
                    
                    // Audit trail
                    auditTrailService.logAction(
                        null,
                        "SYSTEM",
                        "PROMOTE_STUDENT",
                        "Student",
                        promoted.getId().toString(),
                        String.format("Promoted student: %s %s from %s to %s",
                            promoted.getFirstName(), promoted.getLastName(), oldGrade, newGrade),
                        beforeValue,
                        afterValue
                    );
                    
                    log.info("Student promoted successfully: {} {} ({} → {})", 
                        promoted.getFirstName(), promoted.getLastName(), oldGrade, newGrade);
                    
                    return promoted;
                })
                .orElseThrow(() -> {
                    log.error("Student not found with ID: {}", studentId);
                    return new IllegalArgumentException("Student not found with ID: " + studentId);
                });
    }
    
    /**
     * Demote student back to previous grade (for repeating students)
     * 
     * PURPOSE: Revert incorrectly promoted student or mark as repeating
     * 
     * USE CASE: After automatic year-end promotion, admin realizes student
     * should be repeating the grade
     * 
     * WORKFLOW:
     * 1. Update student grade back to previous level
     * 2. Clear completion status if student was marked as completed
     * 3. Reactivate student if they were deactivated
     * 4. Add audit trail entry
     * 
     * @param studentId Student to demote
     * @param demotedGrade Grade to demote back to
     * @param demotedClassName Class within the demoted grade
     * @param reason Reason for demotion
     * @return Updated student
     */
    @Transactional
    public Student demoteStudent(Long studentId, String demotedGrade, String demotedClassName, String reason) {
        log.info("Demoting student ID {} back to grade: {}, class: {}", studentId, demotedGrade, demotedClassName);
        
        return studentRepository.findById(studentId)
                .map(student -> {
                    String oldGrade = student.getGrade();
                    String oldClassName = student.getClassName();
                    String oldCompletionStatus = student.getCompletionStatus();
                    boolean wasInactive = !student.getIsActive();
                    
                    // Capture before state
                    String beforeValue = String.format("Grade: %s, Class: %s, Status: %s, Active: %s", 
                        oldGrade, oldClassName, 
                        oldCompletionStatus != null ? oldCompletionStatus : "ACTIVE",
                        student.getIsActive());
                    
                    // Update student to demoted grade
                    student.setGrade(demotedGrade);
                    student.setClassName(demotedClassName);
                    
                    // Clear completion status (student is no longer completed)
                    if (oldCompletionStatus != null && !oldCompletionStatus.isBlank()) {
                        student.setCompletionStatus(null);
                        log.info("Cleared completion status: {}", oldCompletionStatus);
                    }
                    
                    // Reactivate student if they were deactivated due to completion
                    if (wasInactive) {
                        student.setIsActive(true);
                        log.info("Reactivated student (was inactive)");
                    }
                    
                    // Add demotion notes
                    String demotionNote = String.format("[%s] DEMOTION: %s → %s. Reason: %s", 
                        LocalDate.now(), oldGrade, demotedGrade, reason);
                    String currentNotes = student.getNotes() != null ? student.getNotes() : "";
                    String updatedNotes = currentNotes.isBlank() 
                        ? demotionNote 
                        : currentNotes + "\n" + demotionNote;
                    student.setNotes(updatedNotes);
                    
                    Student demoted = studentRepository.save(student);
                    
                    // Capture after state
                    String afterValue = String.format("Grade: %s, Class: %s, Status: ACTIVE, Active: true", 
                        demotedGrade, demotedClassName);
                    
                    // Audit trail
                    auditTrailService.logAction(
                        null,
                        "SYSTEM",
                        "DEMOTE_STUDENT",
                        "Student",
                        demoted.getId().toString(),
                        String.format("Demoted student: %s %s from %s to %s. Reason: %s",
                            demoted.getFirstName(), demoted.getLastName(), oldGrade, demotedGrade, reason),
                        beforeValue,
                        afterValue
                    );
                    
                    log.info("Student demoted successfully: {} {} ({} → {})", 
                        demoted.getFirstName(), demoted.getLastName(), oldGrade, demotedGrade);
                    
                    return demoted;
                })
                .orElseThrow(() -> {
                    log.error("Student not found with ID: {}", studentId);
                    return new IllegalArgumentException("Student not found with ID: " + studentId);
                });
    }
    
    /**
     * Get students by current grade (for bulk operations)
     * 
     * USAGE: Retrieve all students in a specific grade for bulk promotion
     * Example: Get all Form 1 students to promote to Form 2 at year-end
     * 
     * @param grade Grade to filter by
     * @return List of students in that grade
     */
    public List<Student> getStudentsByGradeForPromotion(String grade) {
        log.debug("Fetching active students in grade: {} for promotion", grade);
        School currentSchool = SchoolContext.getCurrentSchool();
        
        if (currentSchool == null) {
            log.error("No school context available for promotion query");
            throw new IllegalStateException("School context is required");
        }
        
        // Get only active students in the specified grade
        return studentRepository.findBySchoolAndGradeAndIsActiveTrue(currentSchool, grade);
    }
    
    /**
     * Year-end promotion for ALL grades/forms 
     * 
     * PURPOSE: Prevent double-promotion bug by processing all grades simultaneously
     * 
     * CRITICAL LOGIC:
     * 1. Take SNAPSHOT of all students with their current grades (before any changes)
     * 2. Calculate next level for each student based on snapshot
     * 3. Apply all promotions/completions together
     * 
     * This prevents:
     * - Promoting Form 1 → Form 2
     * - Then promoting Form 2 → Form 3 (which would include newly promoted Form 2s!)
     * 
     * PROGRESSION RULES:
     * - Primary: Grade 1→2, 2→3, ..., 6→7, 7→Completed Primary
     * - Secondary: Form 1→2, 2→3, 3→4→Completed O Level
     * - A Level: Form 4→5, 5→6, 6→Completed A Level
     * 
     * @param excludedStudentIds Students to skip (repeating grade, transferred, etc.)
     * @return Map of students grouped by their current grade for processing
     */
    @Transactional
    public java.util.Map<String, java.util.List<Student>> prepareYearEndPromotion(List<Long> excludedStudentIds) {
        log.info("Preparing year-end promotion snapshot");
        School currentSchool = SchoolContext.getCurrentSchool();
        
        if (currentSchool == null) {
            log.error("No school context available for year-end promotion");
            throw new IllegalStateException("School context is required");
        }
        
        // Get ALL active students
        java.util.List<Student> allStudents = studentRepository.findBySchoolAndIsActive(currentSchool, true);
        
        // Group by current grade (snapshot before promotion)
        java.util.Map<String, java.util.List<Student>> studentsByGrade = new java.util.HashMap<>();
        
        for (Student student : allStudents) {
            // Skip excluded students
            if (excludedStudentIds != null && excludedStudentIds.contains(student.getId())) {
                log.debug("Skipping excluded student: {} {}", student.getFirstName(), student.getLastName());
                continue;
            }
            
            // Skip already completed students
            if (student.getCompletionStatus() != null && !student.getCompletionStatus().isBlank()) {
                log.debug("Skipping completed student: {} {} ({})", 
                    student.getFirstName(), student.getLastName(), student.getCompletionStatus());
                continue;
            }
            
            String currentGrade = student.getGrade();
            if (currentGrade == null || currentGrade.isBlank()) {
                log.warn("Student {} {} has no grade assigned", student.getFirstName(), student.getLastName());
                continue;
            }
            
            studentsByGrade.computeIfAbsent(currentGrade, k -> new java.util.ArrayList<>()).add(student);
        }
        
        log.info("Promotion snapshot ready: {} grades with total {} students", 
            studentsByGrade.size(), studentsByGrade.values().stream().mapToInt(List::size).sum());
        
        return studentsByGrade;
    }
    
    /**
     * Apply promotion to a single student based on school type
     * 
     * @param student Student to promote
     * @param schoolType School type (PRIMARY, SECONDARY, COMBINED)
     * @param promotionNotes Notes about promotion
     * @return Updated student
     */
    @Transactional
    public Student applyYearEndPromotion(Student student, String schoolType, String promotionNotes) {
        log.debug("Applying year-end promotion for student: {} {} ({})", 
            student.getFirstName(), student.getLastName(), student.getGrade());
        
        String currentGrade = student.getGrade();
        String beforeValue = String.format("Grade: %s, Status: %s", 
            currentGrade, student.getCompletionStatus() != null ? student.getCompletionStatus() : "ACTIVE");
        
        // Calculate next level
        AcademicProgressionService.ProgressionResult progression = 
            academicProgressionService.getNextLevel(currentGrade, schoolType);
        
        if (progression.isCompleted()) {
            // Student completes their education
            student.setCompletionStatus(progression.getCompletionStatus());
            student.setIsActive(false); // Mark as inactive (completed)
            
            String afterValue = String.format("Grade: %s, Status: %s", 
                currentGrade, progression.getCompletionStatus());
            
            Student completed = studentRepository.save(student);
            
            // Audit trail
            auditTrailService.logAction(
                null,
                "SYSTEM",
                "COMPLETE_EDUCATION",
                "Student",
                completed.getId().toString(),
                String.format("Student %s %s completed: %s",
                    completed.getFirstName(), completed.getLastName(), progression.getCompletionStatus()),
                beforeValue,
                afterValue
            );
            
            log.info("Student {} {} completed: {}", 
                completed.getFirstName(), completed.getLastName(), progression.getCompletionStatus());
            
            return completed;
            
        } else {
            // Promote to next grade/form
            student.setGrade(progression.getNextGrade());
            
            // Add promotion notes
            if (promotionNotes != null && !promotionNotes.isBlank()) {
                String currentNotes = student.getNotes() != null ? student.getNotes() : "";
                String updatedNotes = currentNotes.isBlank() 
                    ? promotionNotes 
                    : currentNotes + "\n[" + LocalDate.now() + "] " + promotionNotes;
                student.setNotes(updatedNotes);
            }
            
            String afterValue = String.format("Grade: %s, Status: ACTIVE", progression.getNextGrade());
            
            Student promoted = studentRepository.save(student);
            
            // Audit trail
            auditTrailService.logAction(
                null,
                "SYSTEM",
                "YEAR_END_PROMOTION",
                "Student",
                promoted.getId().toString(),
                String.format("Year-end promotion: %s %s (%s → %s)",
                    promoted.getFirstName(), promoted.getLastName(), currentGrade, progression.getNextGrade()),
                beforeValue,
                afterValue
            );
            
            log.info("Student {} {} promoted: {} → {}", 
                promoted.getFirstName(), promoted.getLastName(), currentGrade, progression.getNextGrade());
            
            return promoted;
        }
    }
    
    /**
     * Complete year-end promotion orchestration
     * 
     * ATOMIC OPERATION: All grades promote simultaneously to prevent double-promotion
     * 
     * WORKFLOW:
     * 1. Take snapshot of ALL active students grouped by current grade
     * 2. Calculate next level for each grade based on school type
     * 3. Apply all promotions/completions together
     * 4. Create fee records for promoted students
     * 5. Generate detailed summary
     * 
     * @param request Year-end promotion configuration
     * @return Detailed summary with statistics and errors
     */
    @Transactional
    public com.bank.schoolmanagement.dto.YearEndPromotionSummary performYearEndPromotion(
            com.bank.schoolmanagement.dto.YearEndPromotionRequest request) {
        
        log.info("Starting year-end promotion for academic year: {}, term: {}", request.getNewYear(), request.getNewTerm());
        
        School currentSchool = SchoolContext.getCurrentSchool();
        if (currentSchool == null) {
            throw new IllegalStateException("School context is required for year-end promotion");
        }
        
        // Initialize summary
        com.bank.schoolmanagement.dto.YearEndPromotionSummary summary = new com.bank.schoolmanagement.dto.YearEndPromotionSummary();
        summary.setGradeBreakdown(new java.util.HashMap<>());
        summary.setPromotedStudentIds(new java.util.ArrayList<>());
        summary.setCompletedStudents(new java.util.ArrayList<>());
        summary.setErrors(new java.util.ArrayList<>());
        
        // Initialize counters to 0
        summary.setPromotedCount(0);
        summary.setCompletedCount(0);
        summary.setErrorCount(0);
        
        try {
            // STEP 1: Take snapshot of all students by grade (BEFORE any changes)
            java.util.Map<String, java.util.List<Student>> studentsByGrade = 
                prepareYearEndPromotion(request.getExcludedStudentIds());
            
            int totalStudents = studentsByGrade.values().stream()
                .mapToInt(java.util.List::size).sum();
            summary.setTotalStudentsProcessed(totalStudents);
            summary.setExcludedCount(request.getExcludedStudentIds() != null 
                ? request.getExcludedStudentIds().size() : 0);
            
            log.info("Snapshot captured: {} grades with {} total students", 
                studentsByGrade.size(), totalStudents);
            
            // STEP 2: Process each grade group atomically
            for (java.util.Map.Entry<String, java.util.List<Student>> entry : studentsByGrade.entrySet()) {
                String currentGrade = entry.getKey();
                java.util.List<Student> students = entry.getValue();
                
                log.info("Processing grade: {} ({} students)", currentGrade, students.size());
                
                // Initialize grade stats
                com.bank.schoolmanagement.dto.YearEndPromotionSummary.GradePromotionStats gradeStats = 
                    new com.bank.schoolmanagement.dto.YearEndPromotionSummary.GradePromotionStats();
                gradeStats.setFromGrade(currentGrade);
                gradeStats.setStudentCount(students.size());
                gradeStats.setSuccessCount(0);
                gradeStats.setErrorCount(0);
                
                // Process each student in this grade
                for (Student student : students) {
                    try {
                        // Apply promotion based on school type
                        Student updated = applyYearEndPromotion(
                            student, 
                            currentSchool.getSchoolType(), 
                            request.getPromotionNotes()
                        );
                        
                        // Check if student completed or promoted
                        if (updated.getCompletionStatus() != null && !updated.getCompletionStatus().isBlank()) {
                            // Student completed their education
                            gradeStats.setSuccessCount(gradeStats.getSuccessCount() + 1);
                            gradeStats.setToGrade("COMPLETED");
                            summary.setCompletedCount(summary.getCompletedCount() + 1);
                            
                            summary.getCompletedStudents().add(
                                new com.bank.schoolmanagement.dto.YearEndPromotionSummary.CompletedStudent(
                                    updated.getId(),
                                    updated.getStudentId(),
                                    updated.getFirstName() + " " + updated.getLastName(),
                                    updated.getCompletionStatus()
                                )
                            );
                            
                        } else {
                            // Student was promoted to next grade
                            gradeStats.setSuccessCount(gradeStats.getSuccessCount() + 1);
                            gradeStats.setToGrade(updated.getGrade());
                            summary.setPromotedCount(summary.getPromotedCount() + 1);
                            summary.getPromotedStudentIds().add(updated.getStudentId());
                            
                            // Create fee record for promoted student
                            try {
                                createFeeRecordForPromotedStudent(
                                    updated, 
                                    request.getNewYear(),
                                    request.getNewTerm(),
                                    request.getFeeStructures(),
                                    request.getDefaultFeeStructure(),
                                    request.getCarryForwardBalances()
                                );
                            } catch (Exception feeError) {
                                log.error("Failed to create fee record for {}: {}", 
                                    updated.getStudentId(), feeError.getMessage());
                                // Don't fail promotion, just log error
                            }
                        }
                        
                    } catch (Exception e) {
                        gradeStats.setErrorCount(gradeStats.getErrorCount() + 1);
                        summary.setErrorCount(summary.getErrorCount() + 1);
                        
                        summary.getErrors().add(
                            new com.bank.schoolmanagement.dto.YearEndPromotionSummary.PromotionError(
                                student.getId(),
                                student.getFirstName() + " " + student.getLastName(),
                                currentGrade,
                                e.getMessage()
                            )
                        );
                        
                        log.error("Error promoting student {} {}: {}", 
                            student.getFirstName(), student.getLastName(), e.getMessage());
                    }
                }
                
                // Save grade breakdown
                summary.getGradeBreakdown().put(currentGrade, gradeStats);
                
                log.info("Grade {} complete: {} successful ({} promoted + completed), {} errors",
                    currentGrade, gradeStats.getSuccessCount(), 
                    gradeStats.getToGrade(), gradeStats.getErrorCount());
            }
            
            // Set final summary message
            summary.setMessage(String.format(
                "Year-end promotion complete for %s Term %s: %d students promoted, %d completed, %d errors",
                request.getNewYear(),
                request.getNewTerm(),
                summary.getPromotedCount(),
                summary.getCompletedCount(),
                summary.getErrorCount()
            ));
            
            log.info("Year-end promotion complete: {} promoted, {} completed, {} errors",
                summary.getPromotedCount(), summary.getCompletedCount(), summary.getErrorCount());
            
            return summary;
            
        } catch (Exception e) {
            log.error("Critical error in year-end promotion: {}", e.getMessage(), e);
            summary.setMessage("Year-end promotion failed: " + e.getMessage());
            summary.setErrorCount(summary.getTotalStudentsProcessed());
            throw new RuntimeException("Year-end promotion failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create fee record for promoted student with grade-specific fee structure
     */
    private void createFeeRecordForPromotedStudent(
            Student student,
            Integer year,
            Integer term,
            java.util.Map<String, com.bank.schoolmanagement.dto.YearEndPromotionRequest.FeeStructure> feeStructures,
            com.bank.schoolmanagement.dto.YearEndPromotionRequest.FeeStructure defaultFeeStructure,
            Boolean carryForwardBalances) {
        
        // Get fee structure for student's NEW grade
        String newGrade = student.getGrade();
        com.bank.schoolmanagement.dto.YearEndPromotionRequest.FeeStructure feeStructure = 
            feeStructures != null ? feeStructures.get(newGrade) : null;
        
        if (feeStructure == null) {
            feeStructure = defaultFeeStructure;
        }
        
        if (feeStructure == null) {
            log.warn("No fee structure defined for grade {}, skipping fee record creation", newGrade);
            return;
        }
        
        // Calculate previous balance if needed
        java.math.BigDecimal previousBalance = java.math.BigDecimal.ZERO;
        if (Boolean.TRUE.equals(carryForwardBalances)) {
            java.util.Optional<StudentFeeRecord> latestRecordOpt = 
                studentFeeRecordService.getLatestFeeRecordForStudent(student.getId());
            if (latestRecordOpt.isPresent()) {
                previousBalance = latestRecordOpt.get().getOutstandingBalance();
            }
        }
        
        // Create new fee record with all required parameters
        studentFeeRecordService.createPromotionFeeRecord(
            student,
            year,
            term,
            previousBalance,
            feeStructure.getTuitionFee(),
            feeStructure.getBoardingFee(),
            feeStructure.getDevelopmentLevy(),
            feeStructure.getExamFee(),
            feeStructure.getOtherFees(),
            feeStructure.getDefaultScholarship(),
            feeStructure.getDefaultSiblingDiscount(),
            feeStructure.getFeeCategory()
        );
        
        log.debug("Created fee record for {} {} in {}: tuition={}, boarding={}, exam={}",
            student.getFirstName(), student.getLastName(), newGrade,
            feeStructure.getTuitionFee(), feeStructure.getBoardingFee(), feeStructure.getExamFee());
    }

}
