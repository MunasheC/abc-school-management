package com.bank.schoolmanagement.integration;

import com.bank.schoolmanagement.context.SchoolContext;
import com.bank.schoolmanagement.entity.Guardian;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.entity.StudentFeeRecord;
import com.bank.schoolmanagement.repository.SchoolRepository;
import com.bank.schoolmanagement.service.GuardianService;
import com.bank.schoolmanagement.service.StudentFeeRecordService;
import com.bank.schoolmanagement.service.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test: School Data Isolation
 * 
 * Purpose: Verify that multi-tenant data isolation works correctly.
 * 
 * This test ensures:
 * 1. School A cannot see School B's data
 * 2. School B cannot see School A's data
 * 3. All service methods respect school context
 * 4. Data leakage between schools is impossible
 * 5. SchoolContext switching works correctly
 * 
 * Test Scenario:
 * - Create School A (St. Mary's) and School B (St. John's)
 * - Add students to each school
 * - Switch context between schools
 * - Verify each school only sees their own data
 * 
 * @author School Management System
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SchoolDataIsolationTest {

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private GuardianService guardianService;

    @Autowired
    private StudentFeeRecordService feeRecordService;

    private School schoolA;
    private School schoolB;
    private Student studentA1;
    private Student studentA2;
    private Student studentB1;
    private Student studentB2;

    @BeforeEach
    public void setup() {
        // Clear any existing context
        SchoolContext.clear();

        // Create School A: St. Mary's High School
        schoolA = new School();
        schoolA.setSchoolName("St. Mary's High School");
        schoolA.setSchoolCode("MARY001");
        schoolA.setHeadTeacherName("Principal Mary");
        schoolA.setPrimaryPhone("+263772111111");
        schoolA.setEmail("admin@stmarys.ac.zw");
        schoolA.setAddress("123 Education Road, Harare");
        schoolA.setIsActive(true);
        schoolA.setOnboardingDate(LocalDate.now());
        schoolA = schoolRepository.save(schoolA);

        // Create School B: St. John's College
        schoolB = new School();
        schoolB.setSchoolName("St. John's College");
        schoolB.setSchoolCode("JOHN001");
        schoolB.setHeadTeacherName("Principal John");
        schoolB.setPrimaryPhone("+263772222222");
        schoolB.setEmail("admin@stjohns.ac.zw");
        schoolB.setAddress("456 Academy Street, Bulawayo");
        schoolB.setIsActive(true);
        schoolB.setOnboardingDate(LocalDate.now());
        schoolB = schoolRepository.save(schoolB);

        // Create students for School A
        SchoolContext.setCurrentSchool(schoolA);
        
        Guardian guardianA1 = createGuardian("Parent A1", "+263771111111", "parenta1@email.com");
        Guardian savedGuardianA1 = guardianService.createGuardianForCurrentSchool(guardianA1);
        
        studentA1 = createStudent("Alice", "Moyo", "Grade 5", savedGuardianA1);
        studentA1 = studentService.createStudentForCurrentSchool(studentA1);
        
        studentA2 = createStudent("Bob", "Ndlovu", "Grade 6", savedGuardianA1);
        studentA2 = studentService.createStudentForCurrentSchool(studentA2);

        // Create fee records for School A students
        StudentFeeRecord feeA1 = createFeeRecord(studentA1, new BigDecimal("500.00"));
        feeRecordService.createFeeRecordForCurrentSchool(feeA1);
        
        StudentFeeRecord feeA2 = createFeeRecord(studentA2, new BigDecimal("600.00"));
        feeRecordService.createFeeRecordForCurrentSchool(feeA2);

        // Create students for School B
        SchoolContext.setCurrentSchool(schoolB);
        
        Guardian guardianB1 = createGuardian("Parent B1", "+263772222222", "parentb1@email.com");
        Guardian savedGuardianB1 = guardianService.createGuardianForCurrentSchool(guardianB1);
        
        studentB1 = createStudent("Charlie", "Dube", "Grade 5", savedGuardianB1);
        studentB1 = studentService.createStudentForCurrentSchool(studentB1);
        
        studentB2 = createStudent("Diana", "Sibanda", "Grade 6", savedGuardianB1);
        studentB2 = studentService.createStudentForCurrentSchool(studentB2);

        // Create fee records for School B students
        StudentFeeRecord feeB1 = createFeeRecord(studentB1, new BigDecimal("700.00"));
        feeRecordService.createFeeRecordForCurrentSchool(feeB1);
        
        StudentFeeRecord feeB2 = createFeeRecord(studentB2, new BigDecimal("800.00"));
        feeRecordService.createFeeRecordForCurrentSchool(feeB2);

        // Clear context after setup
        SchoolContext.clear();
    }

    @AfterEach
    public void cleanup() {
        SchoolContext.clear();
    }

    @Test
    public void testSchoolACanOnlySeeTheirStudents() {
        // Set context to School A
        SchoolContext.setCurrentSchool(schoolA);

        // Get all students for School A
        List<Student> students = studentService.getAllStudentsForCurrentSchool(Pageable.unpaged()).getContent();

        // Should see exactly 2 students (Alice and Bob)
        assertEquals(2, students.size(), "School A should see exactly 2 students");
        
        // Verify students belong to School A
        assertTrue(students.stream().anyMatch(s -> s.getFirstName().equals("Alice")));
        assertTrue(students.stream().anyMatch(s -> s.getFirstName().equals("Bob")));
        
        // Should NOT see School B students
        assertFalse(students.stream().anyMatch(s -> s.getFirstName().equals("Charlie")));
        assertFalse(students.stream().anyMatch(s -> s.getFirstName().equals("Diana")));

        SchoolContext.clear();
    }

    @Test
    public void testSchoolBCanOnlySeeTheirStudents() {
        // Set context to School B
        SchoolContext.setCurrentSchool(schoolB);

        // Get all students for School B
        List<Student> students = studentService.getAllStudentsForCurrentSchool(Pageable.unpaged()).getContent();

        // Should see exactly 2 students (Charlie and Diana)
        assertEquals(2, students.size(), "School B should see exactly 2 students");
        
        // Verify students belong to School B
        assertTrue(students.stream().anyMatch(s -> s.getFirstName().equals("Charlie")));
        assertTrue(students.stream().anyMatch(s -> s.getFirstName().equals("Diana")));
        
        // Should NOT see School A students
        assertFalse(students.stream().anyMatch(s -> s.getFirstName().equals("Alice")));
        assertFalse(students.stream().anyMatch(s -> s.getFirstName().equals("Bob")));

        SchoolContext.clear();
    }

    @Test
    public void testSchoolACannotAccessSchoolBStudentById() {
        // Set context to School A
        SchoolContext.setCurrentSchool(schoolA);

        // Try to get School B's student by ID (should fail)
        var result = studentService.getStudentByIdForCurrentSchool(studentB1.getId());

        // Should not find the student (different school)
        assertTrue(result.isEmpty(), "School A should not be able to access School B's student by ID");

        SchoolContext.clear();
    }

    @Test
    public void testSchoolBCannotAccessSchoolAStudentById() {
        // Set context to School B
        SchoolContext.setCurrentSchool(schoolB);

        // Try to get School A's student by ID (should fail)
        var result = studentService.getStudentByIdForCurrentSchool(studentA1.getId());

        // Should not find the student (different school)
        assertTrue(result.isEmpty(), "School B should not be able to access School A's student by ID");

        SchoolContext.clear();
    }

    @Test
    public void testSearchIsolation() {
        // Set context to School A
        SchoolContext.setCurrentSchool(schoolA);

        // Search for "Moyo" (only in School A)
        List<Student> results = studentService.searchStudentsByNameForCurrentSchool("Moyo");

        // Should find 1 student
        assertEquals(1, results.size());
        assertEquals("Alice", results.get(0).getFirstName());
        assertEquals("Moyo", results.get(0).getLastName());

        SchoolContext.clear();

        // Set context to School B
        SchoolContext.setCurrentSchool(schoolB);

        // Search for "Moyo" in School B (should find nothing)
        List<Student> resultsB = studentService.searchStudentsByNameForCurrentSchool("Moyo");

        // Should find 0 students (Moyo is in School A)
        assertEquals(0, resultsB.size());

        SchoolContext.clear();
    }

    @Test
    public void testGradeIsolation() {
        // Set context to School A
        SchoolContext.setCurrentSchool(schoolA);

        // Get Grade 5 students in School A
        List<Student> gradeA = studentService.getStudentsByGradeForCurrentSchool("Grade 5");

        // Should find 1 student (Alice)
        assertEquals(1, gradeA.size());
        assertEquals("Alice", gradeA.get(0).getFirstName());

        SchoolContext.clear();

        // Set context to School B
        SchoolContext.setCurrentSchool(schoolB);

        // Get Grade 5 students in School B
        List<Student> gradeB = studentService.getStudentsByGradeForCurrentSchool("Grade 5");

        // Should find 1 student (Charlie)
        assertEquals(1, gradeB.size());
        assertEquals("Charlie", gradeB.get(0).getFirstName());

        SchoolContext.clear();
    }

    @Test
    public void testStudentCountIsolation() {
        // Set context to School A
        SchoolContext.setCurrentSchool(schoolA);
        long countA = studentService.countStudentsForCurrentSchool();
        assertEquals(2, countA, "School A should have 2 students");
        SchoolContext.clear();

        // Set context to School B
        SchoolContext.setCurrentSchool(schoolB);
        long countB = studentService.countStudentsForCurrentSchool();
        assertEquals(2, countB, "School B should have 2 students");
        SchoolContext.clear();
    }

    @Test
    public void testFeeRecordIsolation() {
        // Set context to School A
        SchoolContext.setCurrentSchool(schoolA);
        
        List<StudentFeeRecord> feeRecordsA = feeRecordService.getAllFeeRecordsForCurrentSchool();
        assertEquals(2, feeRecordsA.size(), "School A should have 2 fee records");
        
        BigDecimal totalOutstandingA = feeRecordService.calculateTotalOutstandingForCurrentSchool();
        assertEquals(new BigDecimal("1100.00"), totalOutstandingA, "School A total outstanding should be 1100");
        
        SchoolContext.clear();

        // Set context to School B
        SchoolContext.setCurrentSchool(schoolB);
        
        List<StudentFeeRecord> feeRecordsB = feeRecordService.getAllFeeRecordsForCurrentSchool();
        assertEquals(2, feeRecordsB.size(), "School B should have 2 fee records");
        
        BigDecimal totalOutstandingB = feeRecordService.calculateTotalOutstandingForCurrentSchool();
        assertEquals(new BigDecimal("1500.00"), totalOutstandingB, "School B total outstanding should be 1500");
        
        SchoolContext.clear();
    }

    @Test
    public void testGuardianIsolation() {
        // Set context to School A
        SchoolContext.setCurrentSchool(schoolA);
        
        List<Guardian> guardiansA = guardianService.getAllGuardiansForCurrentSchool();
        assertEquals(1, guardiansA.size(), "School A should have 1 guardian");
        assertEquals("Parent A1", guardiansA.get(0).getFullName());
        
        SchoolContext.clear();

        // Set context to School B
        SchoolContext.setCurrentSchool(schoolB);
        
        List<Guardian> guardiansB = guardianService.getAllGuardiansForCurrentSchool();
        assertEquals(1, guardiansB.size(), "School B should have 1 guardian");
        assertEquals("Parent B1", guardiansB.get(0).getFullName());
        
        SchoolContext.clear();
    }

    @Test
    public void testUpdateOperationIsolation() {
        // Set context to School A
        SchoolContext.setCurrentSchool(schoolA);

        // Update School A's student
        Student updatedStudent = new Student();
        updatedStudent.setGrade("Grade 7");
        
        Student result = studentService.updateStudentForCurrentSchool(studentA1.getId(), updatedStudent);
        assertEquals("Grade 7", result.getGrade());

        SchoolContext.clear();

        // Set context to School B
        SchoolContext.setCurrentSchool(schoolB);

        // Try to update School A's student from School B context (should fail)
        assertThrows(IllegalArgumentException.class, () -> {
            Student wrongUpdate = new Student();
            wrongUpdate.setGrade("Grade 8");
            studentService.updateStudentForCurrentSchool(studentA1.getId(), wrongUpdate);
        }, "Should not be able to update another school's student");

        SchoolContext.clear();
    }

    @Test
    public void testDeleteOperationIsolation() {
        // Set context to School B
        SchoolContext.setCurrentSchool(schoolB);

        // Try to delete School A's student from School B context (should fail)
        assertThrows(IllegalArgumentException.class, () -> {
            studentService.deleteStudentForCurrentSchool(studentA1.getId());
        }, "Should not be able to delete another school's student");

        SchoolContext.clear();
    }

    @Test
    public void testContextSwitching() {
        // Start with School A
        SchoolContext.setCurrentSchool(schoolA);
        assertEquals(2, studentService.getAllStudentsForCurrentSchool(Pageable.unpaged()).getContent().size());
        
        // Switch to School B
        SchoolContext.setCurrentSchool(schoolB);
        assertEquals(2, studentService.getAllStudentsForCurrentSchool(Pageable.unpaged()).getContent().size());
        
        // Switch back to School A
        SchoolContext.setCurrentSchool(schoolA);
        assertEquals(2, studentService.getAllStudentsForCurrentSchool(Pageable.unpaged()).getContent().size());
        
        SchoolContext.clear();
    }

    // ==================== Helper Methods ====================

    private Student createStudent(String firstName, String lastName, String grade, Guardian guardian) {
        Student student = new Student();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setGrade(grade);
        student.setGuardian(guardian);
        student.setEnrollmentDate(LocalDate.now());
        student.setIsActive(true);
        return student;
    }

    private Guardian createGuardian(String name, String phone, String email) {
        Guardian guardian = new Guardian();
        guardian.setFullName(name);
        guardian.setPrimaryPhone(phone);
        guardian.setEmail(email);
        guardian.setIsActive(true);
        return guardian;
    }

    private StudentFeeRecord createFeeRecord(Student student, BigDecimal amount) {
        StudentFeeRecord feeRecord = new StudentFeeRecord();
        feeRecord.setStudent(student);
        feeRecord.setTermYear("2025-Term1");
        feeRecord.setFeeCategory("Regular");
        feeRecord.setTuitionFee(amount);
        feeRecord.setBoardingFee(BigDecimal.ZERO);
        feeRecord.setDevelopmentLevy(BigDecimal.ZERO);
        feeRecord.setExamFee(BigDecimal.ZERO);
        feeRecord.setOtherFees(BigDecimal.ZERO);
        feeRecord.setAmountPaid(BigDecimal.ZERO);
        feeRecord.setOutstandingBalance(amount);
        return feeRecord;
    }
}
