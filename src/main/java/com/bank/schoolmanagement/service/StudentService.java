package com.bank.schoolmanagement.service;

import com.bank.schoolmanagement.context.SchoolContext;
import com.bank.schoolmanagement.entity.Guardian;
import com.bank.schoolmanagement.entity.Payment;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.entity.Student;
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
                    return savedStudent;
                })
                .orElseThrow(() -> {
                    log.error("Student not found with ID: {}", id);
                    return new IllegalArgumentException("Student not found with ID: " + id);
                });
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
        
        studentRepository.deleteById(id);
        log.info("Student deleted successfully");
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
                .map(student -> {
                    SchoolContext.validateSchoolAccess(student.getSchool());
                    return student;
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
        if (student.getGuardian() != null && student.getGuardian().getId() == null) {
            Guardian guardian = student.getGuardian();
            guardian.setSchool(currentSchool);  // Set school on guardian too
            Guardian savedGuardian = guardianService.createOrGetGuardian(guardian);
            student.setGuardian(savedGuardian);
            log.info("Guardian created/retrieved with ID: {}", savedGuardian.getId());
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
                    // Validate school ownership
                    SchoolContext.validateSchoolAccess(existingStudent.getSchool());
                    
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
        
        studentRepository.findById(id).ifPresent(student -> {
            // Validate school ownership
            SchoolContext.validateSchoolAccess(student.getSchool());
            
            studentRepository.deleteById(id);
            
            // Update school student count
            currentSchool.decrementStudentCount();
            
            log.info("Student {} deleted from school: {}", id, currentSchool.getSchoolName());
        });
    }
}
