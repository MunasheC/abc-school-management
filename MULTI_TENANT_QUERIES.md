# Multi-Tenant Repository Query Reference

## Overview

All repositories now have **school-aware queries** to ensure complete data isolation between schools. This document provides a quick reference for all multi-tenant query methods.

## Critical Rule

**ALWAYS filter by school when querying data!**

❌ **WRONG**:
```java
List<Student> students = studentRepository.findByGrade("Grade 5");
// Returns students from ALL schools - data leak!
```

✅ **CORRECT**:
```java
School currentSchool = getCurrentSchool(); // From session/context
List<Student> students = studentRepository.findBySchoolAndGrade(currentSchool, "Grade 5");
// Returns only Grade 5 students from current school
```

---

## StudentRepository (18 Multi-Tenant Methods)

### Basic Queries

```java
// Get all students for a school
List<Student> findBySchool(School school);

// Find student by school and student ID
Optional<Student> findBySchoolAndStudentId(School school, String studentId);

// Check if student ID exists in school (prevent duplicates)
boolean existsBySchoolAndStudentId(School school, String studentId);
```

### Grade & Class Queries

```java
// By grade
List<Student> findBySchoolAndGrade(School school, String grade);
long countBySchoolAndGrade(School school, String grade);

// By class/form
List<Student> findBySchoolAndClassName(School school, String className);
long countBySchoolAndClassName(School school, String className);

// By grade AND class
List<Student> findBySchoolAndGradeAndClassName(School school, String grade, String className);
long countBySchoolAndGradeAndClassName(School school, String grade, String className);
```

### Search & Relationships

```java
// Search by name within school
List<Student> searchBySchoolAndName(School school, String searchTerm);

// Find siblings within school
List<Student> findBySchoolAndGuardian(School school, Guardian guardian);

// Count students in school
long countBySchool(School school);
```

### Usage Examples

```java
// Example 1: Get all Grade 5 students in School A
School schoolA = schoolRepository.findBySchoolCode("SCH001").get();
List<Student> grade5Students = studentRepository.findBySchoolAndGrade(schoolA, "Grade 5");

// Example 2: Get students in Form 3A at School B
School schoolB = schoolRepository.findBySchoolCode("SCH002").get();
List<Student> form3A = studentRepository.findBySchoolAndGradeAndClassName(
    schoolB, "Form 3", "Form 3A"
);

// Example 3: Check if student ID exists before enrollment
boolean exists = studentRepository.existsBySchoolAndStudentId(schoolA, "2025001");
if (exists) {
    throw new DuplicateException("Student ID already exists in this school");
}

// Example 4: Search for students named "John" in School A
List<Student> johns = studentRepository.searchBySchoolAndName(schoolA, "John");
```

---

## GuardianRepository (11 Multi-Tenant Methods)

### Basic Queries

```java
// Get all guardians for a school
List<Guardian> findBySchool(School school);

// Find guardian by school and phone (phone unique per school)
Optional<Guardian> findBySchoolAndPrimaryPhone(School school, String primaryPhone);

// Find guardian by school and email
Optional<Guardian> findBySchoolAndEmail(School school, String email);

// Check if guardian exists (prevent duplicates)
boolean existsBySchoolAndPrimaryPhone(School school, String primaryPhone);
```

### Search & Filter

```java
// Search by name within school
List<Guardian> searchBySchoolAndName(School school, String searchTerm);

// Active guardians in school
List<Guardian> findBySchoolAndIsActiveTrue(School school);

// Guardians with multiple children (sibling discount)
List<Guardian> findGuardiansWithMultipleChildrenBySchool(School school);

// By occupation
List<Guardian> findBySchoolAndOccupation(School school, String occupation);

// Count guardians
long countBySchool(School school);
```

### Usage Examples

```java
// Example 1: Find guardian by phone in School A
School schoolA = schoolRepository.findBySchoolCode("SCH001").get();
Optional<Guardian> guardian = guardianRepository.findBySchoolAndPrimaryPhone(
    schoolA, "+263771234567"
);

// Example 2: Get all guardians with multiple children (sibling discount)
List<Guardian> multiChildGuardians = 
    guardianRepository.findGuardiansWithMultipleChildrenBySchool(schoolA);

// Example 3: Check if guardian exists before creating
boolean exists = guardianRepository.existsBySchoolAndPrimaryPhone(
    schoolA, "+263771234567"
);

// Example 4: Search for guardians by name
List<Guardian> smiths = guardianRepository.searchBySchoolAndName(schoolA, "Smith");
```

---

## StudentFeeRecordRepository (20 Multi-Tenant Methods)

### Basic Queries

```java
// Get all fee records for a school
List<StudentFeeRecord> findBySchool(School school);

// By term/year
List<StudentFeeRecord> findBySchoolAndTermYear(School school, String termYear);

// By payment status
List<StudentFeeRecord> findBySchoolAndPaymentStatus(School school, String paymentStatus);

// By fee category
List<StudentFeeRecord> findBySchoolAndFeeCategory(School school, String feeCategory);
```

### Combined Filters

```java
// By term AND status
List<StudentFeeRecord> findBySchoolAndTermYearAndPaymentStatus(
    School school, String termYear, String paymentStatus
);
```

### Financial Queries

```java
// Records with outstanding balance
List<StudentFeeRecord> findRecordsWithOutstandingBalanceBySchool(School school);

// Fully paid records
List<StudentFeeRecord> findFullyPaidRecordsBySchool(School school);

// Records with scholarships
List<StudentFeeRecord> findBySchoolAndHasScholarshipTrue(School school);
```

### Financial Calculations

```java
// Total outstanding balance
BigDecimal calculateTotalOutstandingBalanceBySchool(School school);

// Total amount paid
BigDecimal calculateTotalAmountPaidBySchool(School school);

// Total gross amount (fees before payments)
BigDecimal calculateTotalGrossAmountBySchool(School school);

// Total scholarships given
BigDecimal calculateTotalScholarshipsBySchool(School school);
```

### Counts

```java
// Count by status
long countBySchoolAndPaymentStatus(School school, String paymentStatus);

// Total count
long countBySchool(School school);
```

### Usage Examples

```java
School schoolA = schoolRepository.findBySchoolCode("SCH001").get();

// Example 1: Get all arrears for Term 1 2025
List<StudentFeeRecord> arrears = feeRecordRepository.findBySchoolAndTermYearAndPaymentStatus(
    schoolA, "Term 1 2025", "ARREARS"
);

// Example 2: Calculate total outstanding balance for school
BigDecimal outstanding = feeRecordRepository.calculateTotalOutstandingBalanceBySchool(schoolA);
System.out.println("Total outstanding: $" + outstanding);

// Example 3: Get all scholarship students
List<StudentFeeRecord> scholarships = 
    feeRecordRepository.findBySchoolAndHasScholarshipTrue(schoolA);

// Example 4: Financial summary
BigDecimal totalFees = feeRecordRepository.calculateTotalGrossAmountBySchool(schoolA);
BigDecimal totalPaid = feeRecordRepository.calculateTotalAmountPaidBySchool(schoolA);
BigDecimal totalOutstanding = feeRecordRepository.calculateTotalOutstandingBalanceBySchool(schoolA);

System.out.println("Total Fees: $" + totalFees);
System.out.println("Total Paid: $" + totalPaid);
System.out.println("Outstanding: $" + totalOutstanding);
```

---

## PaymentRepository (22 Multi-Tenant Methods)

### Basic Queries

```java
// Get all payments for a school
List<Payment> findBySchool(School school);

// By student
List<Payment> findBySchoolAndStudentId(School school, Long studentId);

// Active payments by student (completed, not reversed)
List<Payment> findActivePaymentsBySchoolAndStudentId(School school, Long studentId);

// By payment reference
Optional<Payment> findBySchoolAndPaymentReference(School school, String paymentReference);
```

### Filter Queries

```java
// By payment method
List<Payment> findBySchoolAndPaymentMethod(School school, String paymentMethod);

// By status
List<Payment> findBySchoolAndStatus(School school, String status);

// Reversed payments
List<Payment> findBySchoolAndIsReversedTrue(School school);

// By bursar
List<Payment> findBySchoolAndReceivedBy(School school, String receivedBy);
```

### Date Range Queries

```java
// Payments in date range
List<Payment> findBySchoolAndDateRange(
    School school, LocalDateTime startDate, LocalDateTime endDate
);

// Recent payments (last N days)
List<Payment> findRecentPaymentsBySchool(School school, LocalDateTime since);
```

### Financial Calculations

```java
// Total payments (completed, not reversed)
BigDecimal calculateTotalPaymentsBySchool(School school);

// Total in date range
BigDecimal calculateTotalInDateRangeBySchool(
    School school, LocalDateTime startDate, LocalDateTime endDate
);

// Total by payment method
BigDecimal calculateTotalBySchoolAndMethod(School school, String paymentMethod);
```

### Counts

```java
// Total payment count
long countBySchool(School school);

// Count by payment method
long countBySchoolAndPaymentMethod(School school, String paymentMethod);
```

### Usage Examples

```java
School schoolA = schoolRepository.findBySchoolCode("SCH001").get();

// Example 1: Get today's payments
LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
List<Payment> todayPayments = paymentRepository.findBySchoolAndDateRange(
    schoolA, startOfDay, endOfDay
);

// Example 2: Calculate daily revenue
BigDecimal dailyRevenue = paymentRepository.calculateTotalInDateRangeBySchool(
    schoolA, startOfDay, endOfDay
);
System.out.println("Today's revenue: $" + dailyRevenue);

// Example 3: Mobile money vs cash analysis
BigDecimal mobileMoney = paymentRepository.calculateTotalBySchoolAndMethod(
    schoolA, "MOBILE_MONEY"
);
BigDecimal cash = paymentRepository.calculateTotalBySchoolAndMethod(
    schoolA, "CASH"
);
System.out.println("Mobile Money: $" + mobileMoney);
System.out.println("Cash: $" + cash);

// Example 4: Get payment history for a student
Long studentId = 123L;
List<Payment> paymentHistory = paymentRepository.findBySchoolAndStudentId(
    schoolA, studentId
);

// Example 5: Last 7 days payments
LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
List<Payment> weekPayments = paymentRepository.findRecentPaymentsBySchool(
    schoolA, weekAgo
);

// Example 6: Track bursar performance
List<Payment> bursarPayments = paymentRepository.findBySchoolAndReceivedBy(
    schoolA, "Jane Doe"
);
BigDecimal bursarTotal = bursarPayments.stream()
    .filter(p -> !p.getIsReversed() && "COMPLETED".equals(p.getStatus()))
    .map(Payment::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
System.out.println("Bursar collected: $" + bursarTotal);
```

---

## Common Patterns

### 1. Get Current School from Context

```java
@Service
public class StudentService {
    
    public List<Student> getStudentsByGrade(String grade) {
        School currentSchool = SchoolContext.getCurrentSchool();
        return studentRepository.findBySchoolAndGrade(currentSchool, grade);
    }
}
```

### 2. Validate School Ownership

```java
public Student getStudent(Long studentId) {
    School currentSchool = SchoolContext.getCurrentSchool();
    Student student = studentRepository.findById(studentId)
        .orElseThrow(() -> new NotFoundException("Student not found"));
    
    // CRITICAL: Verify student belongs to current school
    if (!student.getSchool().equals(currentSchool)) {
        throw new UnauthorizedException("Student belongs to different school");
    }
    
    return student;
}
```

### 3. Bulk Operations with School Context

```java
public void assignFeesToGrade(String grade, String termYear, BigDecimal tuitionFee) {
    School currentSchool = SchoolContext.getCurrentSchool();
    
    // Get only students from current school
    List<Student> students = studentRepository.findBySchoolAndGrade(currentSchool, grade);
    
    for (Student student : students) {
        StudentFeeRecord feeRecord = new StudentFeeRecord();
        feeRecord.setSchool(currentSchool);  // Always set school!
        feeRecord.setStudent(student);
        feeRecord.setTermYear(termYear);
        feeRecord.setTuitionFee(tuitionFee);
        // ... set other fields
        
        feeRecordRepository.save(feeRecord);
    }
}
```

### 4. Financial Reporting per School

```java
public Map<String, Object> getSchoolFinancialSummary() {
    School currentSchool = SchoolContext.getCurrentSchool();
    
    Map<String, Object> summary = new HashMap<>();
    
    // Fee totals
    summary.put("totalFees", 
        feeRecordRepository.calculateTotalGrossAmountBySchool(currentSchool));
    summary.put("totalPaid", 
        feeRecordRepository.calculateTotalAmountPaidBySchool(currentSchool));
    summary.put("totalOutstanding", 
        feeRecordRepository.calculateTotalOutstandingBalanceBySchool(currentSchool));
    
    // Payment totals
    summary.put("totalRevenue", 
        paymentRepository.calculateTotalPaymentsBySchool(currentSchool));
    
    // Payment method breakdown
    summary.put("mobileMoney", 
        paymentRepository.calculateTotalBySchoolAndMethod(currentSchool, "MOBILE_MONEY"));
    summary.put("cash", 
        paymentRepository.calculateTotalBySchoolAndMethod(currentSchool, "CASH"));
    
    // Counts
    summary.put("totalStudents", 
        studentRepository.countBySchool(currentSchool));
    summary.put("studentsInArrears", 
        feeRecordRepository.countBySchoolAndPaymentStatus(currentSchool, "ARREARS"));
    
    return summary;
}
```

### 5. Excel Upload with School Assignment

```java
public void processExcelFile(MultipartFile file, School school) {
    List<StudentData> excelData = parseExcel(file);
    
    for (StudentData data : excelData) {
        // Check if student ID exists in this school
        if (studentRepository.existsBySchoolAndStudentId(school, data.getStudentId())) {
            throw new DuplicateException("Student ID already exists: " + data.getStudentId());
        }
        
        Student student = new Student();
        student.setSchool(school);  // CRITICAL: Assign school
        student.setStudentId(data.getStudentId());
        // ... set other fields
        
        studentRepository.save(student);
        
        // Increment school student count
        school.incrementStudentCount();
        schoolRepository.save(school);
    }
}
```

---

## Migration Checklist

When updating existing code to use multi-tenant queries:

### ✅ Step 1: Update Repository Calls

**Before:**
```java
List<Student> students = studentRepository.findByGrade(grade);
```

**After:**
```java
School currentSchool = SchoolContext.getCurrentSchool();
List<Student> students = studentRepository.findBySchoolAndGrade(currentSchool, grade);
```

### ✅ Step 2: Update Service Methods

**Before:**
```java
public List<Student> getStudentsByGrade(String grade) {
    return studentRepository.findByGrade(grade);
}
```

**After:**
```java
public List<Student> getStudentsByGrade(School school, String grade) {
    return studentRepository.findBySchoolAndGrade(school, grade);
}
```

### ✅ Step 3: Update Controllers

**Before:**
```java
@GetMapping("/students")
public List<Student> getStudents(@RequestParam String grade) {
    return studentService.getStudentsByGrade(grade);
}
```

**After:**
```java
@GetMapping("/students")
public List<Student> getStudents(@RequestParam String grade) {
    School currentSchool = SchoolContext.getCurrentSchool();
    return studentService.getStudentsByGrade(currentSchool, grade);
}
```

### ✅ Step 4: Set School on New Entities

**Before:**
```java
Student student = new Student();
student.setStudentId("2025001");
studentRepository.save(student);
```

**After:**
```java
School currentSchool = SchoolContext.getCurrentSchool();
Student student = new Student();
student.setSchool(currentSchool);  // CRITICAL!
student.setStudentId("2025001");
studentRepository.save(student);
```

---

## Testing Multi-Tenancy

```java
@Test
public void testDataIsolation() {
    // Create two schools
    School schoolA = createSchool("SCH001", "School A");
    School schoolB = createSchool("SCH002", "School B");
    
    // Add students with same student ID to different schools
    Student studentA = new Student();
    studentA.setSchool(schoolA);
    studentA.setStudentId("2025001");
    studentRepository.save(studentA);
    
    Student studentB = new Student();
    studentB.setSchool(schoolB);
    studentB.setStudentId("2025001");  // Same ID, different school - OK!
    studentRepository.save(studentB);
    
    // Verify isolation
    List<Student> schoolAStudents = studentRepository.findBySchool(schoolA);
    assertEquals(1, schoolAStudents.size());
    assertEquals(studentA.getId(), schoolAStudents.get(0).getId());
    
    List<Student> schoolBStudents = studentRepository.findBySchool(schoolB);
    assertEquals(1, schoolBStudents.size());
    assertEquals(studentB.getId(), schoolBStudents.get(0).getId());
    
    // Verify school-aware lookup
    Optional<Student> foundA = studentRepository.findBySchoolAndStudentId(schoolA, "2025001");
    assertTrue(foundA.isPresent());
    assertEquals(studentA.getId(), foundA.get().getId());
    
    Optional<Student> foundB = studentRepository.findBySchoolAndStudentId(schoolB, "2025001");
    assertTrue(foundB.isPresent());
    assertEquals(studentB.getId(), foundB.get().getId());
}
```

---

## Summary

All 4 repositories now have **71 multi-tenant query methods**:

- **StudentRepository**: 18 school-aware methods
- **GuardianRepository**: 11 school-aware methods  
- **StudentFeeRecordRepository**: 20 school-aware methods
- **PaymentRepository**: 22 school-aware methods

**Next Steps:**

1. ✅ Update all service classes to use school-aware queries
2. ✅ Create `SchoolContext` utility to get current school from session
3. ✅ Update controllers to inject school context
4. ✅ Update `ExcelService` to assign school during upload
5. ✅ Add security annotations to restrict cross-school access
6. ✅ Test with multiple schools to verify data isolation

**Remember**: Always filter by school to prevent data leaks! 🔒
