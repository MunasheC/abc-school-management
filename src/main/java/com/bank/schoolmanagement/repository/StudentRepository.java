    
package com.bank.schoolmanagement.repository;

import com.bank.schoolmanagement.entity.Student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Student Repository - Interface for database operations
 * 
 * KEY CONCEPT: This is an INTERFACE, not a class!
 * You don't write any implementation - Spring Data JPA creates it automatically!
 * 
 * @Repository - Marks this as a repository component (optional with JpaRepository)
 * 
 * JpaRepository<Student, Long> means:
 * - Student: The entity type we're working with
 * - Long: The type of the primary key (id field)
 * 
 * JpaRepository provides these methods OUT OF THE BOX:
 * - save(Student) - Insert or update
 * - findById(Long) - Find by primary key
 * - findAll() - Get all records
 * - deleteById(Long) - Delete by primary key
 * - count() - Count all records
 * - existsById(Long) - Check if exists
 * ...and many more!
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * Custom Query Methods
     * 
     * Spring Data JPA can generate queries from method names!
     * You just name the method following a pattern and Spring creates the query.
     * 
     * Pattern: findBy + FieldName + Condition
     */

    /**
     * Find student by student ID (school's ID, not database ID)
     * Spring generates: SELECT * FROM students WHERE student_id = ?
     */
    Optional<Student> findByStudentId(String studentId);

    /**
     * Find student by school ID and student ID
     * Used for bank lookup to ensure student belongs to the correct school
     * Spring generates: SELECT * FROM students WHERE school_id = ? AND student_id = ?
     */
    Optional<Student> findBySchoolIdAndStudentId(Long schoolId, String studentId);

    /**
     * Find all students by grade
     * Spring generates: SELECT * FROM students WHERE grade = ?
     */
    List<Student> findByGrade(String grade);

    /**
     * Find students by class name (form/stream)
     * Example: "5A", "Form 3B"
     * Spring generates: SELECT * FROM students WHERE class_name = ?
     */
    List<Student> findByClassName(String className);

    /**
     * Find students by grade AND class name
     * Example: grade="Grade 5", className="5A"
     * Spring generates: SELECT * FROM students WHERE grade = ? AND class_name = ?
     */
    List<Student> findByGradeAndClassName(String grade, String className);

    /**
     * Find students by first name (case-insensitive)
     * Spring generates: SELECT * FROM students WHERE LOWER(first_name) = LOWER(?)
     */
    List<Student> findByFirstNameIgnoreCase(String firstName);

    /**
     * Find students by last name (case-insensitive)
     * Spring generates: SELECT * FROM students WHERE LOWER(last_name) = LOWER(?)
     */
    List<Student> findByLastNameIgnoreCase(String lastName);

    /**
     * Find students by first AND last name
     * Spring generates: SELECT * FROM students WHERE first_name = ? AND last_name = ?
     */
    List<Student> findByFirstNameAndLastName(String firstName, String lastName);
    
    /**
     * Find students by first name, last name, school ID, and grade
     * Used for bank search when parent doesn't know student reference
     * Spring generates: SELECT * FROM students WHERE first_name = ? AND last_name = ? AND school_id = ? AND grade = ?
     */
    List<Student> findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndSchoolIdAndGrade(
        String firstName, String lastName, Long schoolId, String grade);

    
    Optional<Student> findByNationalId(String nationalId);

    /**
     * Find active students only
     * Spring generates: SELECT * FROM students WHERE is_active = true
     */
    List<Student> findByIsActiveTrue();

    /**
     * Check if a student ID already exists
     * Spring generates: SELECT COUNT(*) > 0 FROM students WHERE student_id = ?
     */
    boolean existsByStudentId(String studentId);

    /**
     * Custom JPQL Query
     * 
     * Sometimes you need more complex queries than method names allow.
     * Use @Query annotation to write custom JPQL (Java Persistence Query Language)
     * 
     * JPQL is similar to SQL but uses entity names and field names instead of table/column names
     */
    
    /**
     * Search students by name (first or last) - case insensitive
     * The % signs are wildcards for LIKE queries
     */
    @Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Student> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Find students enrolled after a specific date
     */
    @Query("SELECT s FROM Student s WHERE s.enrollmentDate >= :date ORDER BY s.enrollmentDate DESC")
    List<Student> findStudentsEnrolledAfter(@Param("date") java.time.LocalDate date);

    /**
     * Count students by grade
     */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.grade = :grade AND s.isActive = true")
    long countByGrade(@Param("grade") String grade);

    /**
     * Count students by class name
     */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.className = :className AND s.isActive = true")
    long countByClassName(@Param("className") String className);

    /**
     * Count students by grade and class name
     */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.grade = :grade AND s.className = :className AND s.isActive = true")
    long countByGradeAndClassName(@Param("grade") String grade, @Param("className") String className);

    /* ----------------------  BURSAR-SPECIFIC QUERIES  ------------------------- */
    
    /**
     * NOTE: Financial queries moved to StudentFeeRecordRepository
     * - Payment status, fee categories, outstanding balances are now in StudentFeeRecord
     * - Use StudentFeeRecordRepository for financial operations
     * - This repository focuses on student personal/academic data only
     */

    /* ----------------------  RELATIONSHIP QUERIES (NEW)  ------------------------- */

    /**
     * Find student with guardian loaded
     * 
     * LEARNING: JOIN FETCH optimization
     * - Loads student AND guardian in ONE database query
     * - Without this, would make 2 queries (N+1 problem)
     * - Use when you know you'll need guardian data
     */
    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.guardian WHERE s.id = :id")
    Optional<Student> findByIdWithGuardian(@Param("id") Long id);

    /**
     * Find student with fee record loaded
     */
    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.currentFeeRecord WHERE s.id = :id")
    Optional<Student> findByIdWithFeeRecord(@Param("id") Long id);

    /**
     * Find student with all relationships loaded (guardian + fee record + payments)
     * 
     * LEARNING: Multiple JOIN FETCH
     * - Loads everything in one query
     * - Use carefully - can be slow if student has many payments
     */
    @Query("SELECT DISTINCT s FROM Student s " +
           "LEFT JOIN FETCH s.guardian " +
           "LEFT JOIN FETCH s.currentFeeRecord " +
           "LEFT JOIN FETCH s.payments " +
           "WHERE s.id = :id")
    Optional<Student> findByIdWithAllRelationships(@Param("id") Long id);

    /**
     * Find students by guardian phone
     * Useful for: "Find all siblings who have this parent's phone"
     */
    @Query("SELECT s FROM Student s WHERE s.guardian.primaryPhone = :phone")
    List<Student> findByGuardianPhone(@Param("phone") String phone);

    /**
     * Find students by guardian ID
     * Returns all siblings sharing this guardian
     */
    List<Student> findByGuardianId(Long guardianId);

    /* ----------------------  MULTI-TENANT QUERIES (School-Aware)  ------------------------- */

    /**
     * Find all students for a specific school
     * 
     * CRITICAL: Multi-tenant data isolation
     * - Returns only students from specified school
     * - Used by school users to view their students
     * 
     * Spring generates: SELECT * FROM students WHERE school_id = ?
     * @param pageable 
     */
    Page<Student> findBySchool(com.bank.schoolmanagement.entity.School school, Pageable pageable);

    /**
     * Find student by school and student ID
     * 
     * LEARNING: Student IDs are unique per school, not globally
     * - School A's student "2025001" is different from School B's "2025001"
     * - Must always query with school context
     */
    Optional<Student> findBySchoolAndStudentId(com.bank.schoolmanagement.entity.School school, String studentId);

    /**
     * Find students by school and grade
     * 
     * Example: Get all Grade 5 students from School A
     * Spring generates: SELECT * FROM students WHERE school_id = ? AND grade = ?
     */
    List<Student> findBySchoolAndGrade(com.bank.schoolmanagement.entity.School school, String grade);

    /**
     * Find students by school and class name
     * 
     * Example: Get all students in "5A" from School A
     * Spring generates: SELECT * FROM students WHERE school_id = ? AND class_name = ?
     */
    List<Student> findBySchoolAndClassName(com.bank.schoolmanagement.entity.School school, String className);

    /**
     * Find students by school, grade, and class name
     * 
     * Most specific query: School + Grade + Class
     * Example: School A, Grade 5, Class 5A
     */
    List<Student> findBySchoolAndGradeAndClassName(
        com.bank.schoolmanagement.entity.School school, 
        String grade, 
        String className
    );

    /**
     * Count students for a school
     * 
     * Used for: Capacity tracking, statistics
     */
    long countBySchool(com.bank.schoolmanagement.entity.School school);

    /**
     * Count students by school and active status
     */
    long countBySchoolAndIsActive(com.bank.schoolmanagement.entity.School school, Boolean isActive);

    /**
     * Count students by active status (all schools)
     */
    long countByIsActive(Boolean isActive);

    /**
     * Count students by school and grade
     */
    long countBySchoolAndGrade(com.bank.schoolmanagement.entity.School school, String grade);

    /**
     * Count students by school and class name
     */
    long countBySchoolAndClassName(com.bank.schoolmanagement.entity.School school, String className);

    /**
     * Count students by school, grade, and class name
     */
    long countBySchoolAndGradeAndClassName(
        com.bank.schoolmanagement.entity.School school,
        String grade,
        String className
    );

    /**
     * Find students by school and guardian
     * 
     * Get siblings within same school
     */
    List<Student> findBySchoolAndGuardian(
        com.bank.schoolmanagement.entity.School school,
        com.bank.schoolmanagement.entity.Guardian guardian
    );

    /**
     * Search students by name within a school
     * 
     * CRITICAL: Always scope search to current school
     */
    @Query("SELECT s FROM Student s WHERE s.school = :school AND " +
           "(LOWER(s.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Student> searchBySchoolAndName(
        @Param("school") com.bank.schoolmanagement.entity.School school,
        @Param("searchTerm") String searchTerm
    );

    /**
     * Check if student ID exists in school
     * 
     * Used during enrollment to prevent duplicate student IDs within a school
     */
    boolean existsBySchoolAndStudentId(
        com.bank.schoolmanagement.entity.School school,
        String studentId
    );
    
}
