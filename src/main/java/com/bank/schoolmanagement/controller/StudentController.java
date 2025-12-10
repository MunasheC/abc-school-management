package com.bank.schoolmanagement.controller;

import com.bank.schoolmanagement.dto.UploadSummary;
import com.bank.schoolmanagement.entity.Payment;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.entity.StudentFeeRecord;
import com.bank.schoolmanagement.enums.PaymentMethod;
import com.bank.schoolmanagement.service.ExcelService;
import com.bank.schoolmanagement.service.PaymentService;
import com.bank.schoolmanagement.service.StudentFeeRecordService;
import com.bank.schoolmanagement.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Student Controller - REST API Endpoints
 * 
 * KEY ANNOTATIONS:
 * @RestController - Combines @Controller and @ResponseBody
 *                   All methods return data (JSON) not views (HTML pages)
 * @RequestMapping - Base URL path for all endpoints in this controller
 * @RequiredArgsConstructor - Lombok generates constructor for dependency injection
 * @Slf4j - Lombok provides logger
 * 
 * REST API BASICS:
 * REST uses HTTP methods to indicate operations:
 * - GET: Retrieve data (read)
 * - POST: Create new data
 * - PUT: Update existing data (full update)
 * - PATCH: Update existing data (partial update)
 * - DELETE: Remove data
 * 
 * HTTP Status Codes:
 * - 200 OK: Success
 * - 201 Created: New resource created
 * - 204 No Content: Success but no data to return
 * - 400 Bad Request: Invalid input
 * - 404 Not Found: Resource doesn't exist
 * - 500 Internal Server Error: Something went wrong on server
 */
@RestController
@RequestMapping("/api/school/students")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;
    private final ExcelService excelService;
    private final StudentFeeRecordService feeRecordService;
    private final PaymentService paymentService;

    /**
     * CREATE - Add a new student
     * 
     * POST /api/students
     * 
     * @Valid - Triggers validation annotations in Student entity
     * @RequestBody - Converts JSON from request body to Student object
     * 
     * Example JSON:
     * {
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "dateOfBirth": "2010-05-15",
     *   "grade": "Grade 5",
     *   "parentName": "Jane Doe",
     *   "parentPhone": "+263771234567",
     *   "parentEmail": "jane.doe@email.com",
     *   "address": "123 Main Street, Harare"
     * }
     */
    @PostMapping
    public ResponseEntity<Student> createStudent(@Valid @RequestBody Student student) {
        log.info("REST request to create student: {} {}", student.getFirstName(), student.getLastName());
        Student createdStudent = studentService.createStudentForCurrentSchool(student);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStudent);
    }

    /**
     * READ - Get all students
     * 
     * GET /api/students
     * 
     * Returns JSON array of all students
     */
    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        log.info("REST request to get all students");
        List<Student> students = studentService.getAllStudentsForCurrentSchool();
        return ResponseEntity.ok(students);
    }

    /**
     * READ - Get active students only
     * 
     * GET /api/students/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<Student>> getActiveStudents() {
        log.info("REST request to get active students");
        List<Student> students = studentService.getActiveStudents();
        return ResponseEntity.ok(students);
    }

    /**
     * READ - Get student by database ID
     * 
     * GET /api/students/{id}
     * 
     * @PathVariable - Extracts {id} from URL
     * 
     * Example: GET /api/students/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
        log.info("REST request to get student with ID: {}", id);
        return studentService.getStudentByIdForCurrentSchool(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Get student by student ID (school's ID)
     * 
     * GET /api/students/student-id/{studentId}
     * 
     * Example: GET /api/students/student-id/STU1702040288567
     */
    @GetMapping("/student-id/{studentId}")
    public ResponseEntity<Student> getStudentByStudentId(@PathVariable String studentId) {
        log.info("REST request to get student with student ID: {}", studentId);
        return studentService.getStudentByStudentIdForCurrentSchool(studentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Get students by grade
     * 
     * GET /api/students/grade/{grade}
     * 
     * Example: GET /api/students/grade/Grade%205
     * (Note: Space is encoded as %20 in URLs)
     */
    @GetMapping("/grade/{grade}")
    public ResponseEntity<List<Student>> getStudentsByGrade(@PathVariable String grade) {
        log.info("REST request to get students in grade: {}", grade);
        List<Student> students = studentService.getStudentsByGradeForCurrentSchool(grade);
        return ResponseEntity.ok(students);
    }

    /**
     * READ - Search students by name
     * 
     * GET /api/students/search?name=John
     * 
     * @RequestParam - Extracts query parameter from URL
     */
    @GetMapping("/search")
    public ResponseEntity<List<Student>> searchStudents(@RequestParam String name) {
        log.info("REST request to search students with name: {}", name);
        List<Student> students = studentService.searchStudentsByNameForCurrentSchool(name);
        return ResponseEntity.ok(students);
    }

    /**
     * READ - Get students by guardian phone (siblings)
     * 
     * GET /api/students/guardian-phone/{phone}
     * 
     * LEARNING: Uses new Guardian relationship
     * Returns all students sharing the same guardian (siblings)
     * Useful for bank integration: "Show me all students whose parent has this phone number"
     */
    @GetMapping("/guardian-phone/{phone}")
    public ResponseEntity<List<Student>> getStudentsByGuardianPhone(@PathVariable String phone) {
        log.info("REST request to get students with guardian phone: {}", phone);
        List<Student> students = studentService.getStudentsByGuardianPhone(phone);
        return ResponseEntity.ok(students);
    }

    /**
     * READ - Get students enrolled after a date
     * 
     * GET /api/students/enrolled-after?date=2024-01-01
     */
    @GetMapping("/enrolled-after")
    public ResponseEntity<List<Student>> getStudentsEnrolledAfter(@RequestParam String date) {
        log.info("REST request to get students enrolled after: {}", date);
        LocalDate enrollmentDate = LocalDate.parse(date);
        List<Student> students = studentService.getStudentsEnrolledAfter(enrollmentDate);
        return ResponseEntity.ok(students);
    }

    /**
     * UPDATE - Update student information (PARTIAL UPDATE)
     * 
     * PUT /api/students/{id}
     * 
     * IMPORTANT: This endpoint supports PARTIAL updates!
     * - You only need to send the fields you want to update
     * - Other fields will keep their existing values
     * - @Valid removed to allow partial updates
     * 
     * Example: PUT /api/students/1
     * Body: { "grade": "Grade 6", "address": "New Address" }
     * 
     * This will update only grade and address, keeping all other fields unchanged.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(
            @PathVariable Long id,
            @RequestBody Student student) {  // Removed @Valid for partial updates
        log.info("REST request to update student with ID: {}", id);
        try {
            Student updatedStudent = studentService.updateStudentForCurrentSchool(id, student);
            return ResponseEntity.ok(updatedStudent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE - Hard delete student
     * 
     * DELETE /api/students/{id}
     * 
     * Returns 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        log.info("REST request to delete student with ID: {}", id);
        try {
            studentService.deleteStudentForCurrentSchool(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PATCH - Deactivate student (soft delete)
     * 
     * PATCH /api/students/{id}/deactivate
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Student> deactivateStudent(@PathVariable Long id) {
        log.info("REST request to deactivate student with ID: {}", id);
        try {
            Student deactivatedStudent = studentService.deactivateStudent(id);
            return ResponseEntity.ok(deactivatedStudent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PATCH - Reactivate student
     * 
     * PATCH /api/students/{id}/reactivate
     */
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Student> reactivateStudent(@PathVariable Long id) {
        log.info("REST request to reactivate student with ID: {}", id);
        try {
            Student reactivatedStudent = studentService.reactivateStudent(id);
            return ResponseEntity.ok(reactivatedStudent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Statistics - Get count by grade
     * 
     * GET /api/students/stats/count-by-grade?grade=Grade%205
     */
    @GetMapping("/stats/count-by-grade")
    public ResponseEntity<Long> countByGrade(@RequestParam String grade) {
        log.info("REST request to count students in grade: {}", grade);
        long count = studentService.countStudentsByGrade(grade);
        return ResponseEntity.ok(count);
    }

    /**
     * Statistics - Get total active students
     * 
     * GET /api/students/stats/active-count
     */
    @GetMapping("/stats/active-count")
    public ResponseEntity<Long> countActiveStudents() {
        log.info("REST request to count active students");
        long count = studentService.countActiveStudents();
        return ResponseEntity.ok(count);
    }

    /* ----------------------  BURSAR-SPECIFIC ENDPOINTS  ------------------------- */

    /**
     * Get fee records by payment status
     * 
     * GET /api/students/payment-status/{status}
     * 
     * LEARNING: Now returns fee records instead of students
     * Financial queries use StudentFeeRecordService
     * 
     * Example: GET /api/students/payment-status/ARREARS
     * Valid statuses: PAID, PARTIALLY_PAID, ARREARS, OVERDUE
     */
    @GetMapping("/payment-status/{status}")
    public ResponseEntity<List<StudentFeeRecord>> getStudentsByPaymentStatus(@PathVariable String status) {
        log.info("REST request to get fee records with payment status: {}", status);
        List<StudentFeeRecord> feeRecords = feeRecordService.getFeeRecordsByPaymentStatus(status);
        return ResponseEntity.ok(feeRecords);
    }

    /**
     * Get fee records by fee category
     * 
     * GET /api/students/fee-category/{category}
     * 
     * Example: GET /api/students/fee-category/Boarder
     * Returns fee records for boarding students
     */
    @GetMapping("/fee-category/{category}")
    public ResponseEntity<List<StudentFeeRecord>> getStudentsByFeeCategory(@PathVariable String category) {
        log.info("REST request to get fee records with category: {}", category);
        List<StudentFeeRecord> feeRecords = feeRecordService.getFeeRecordsByFeeCategory(category);
        return ResponseEntity.ok(feeRecords);
    }

    /**
     * Get students with scholarships
     * 
     * GET /api/students/scholarships
     * 
     * Returns fee records with scholarship amounts
     */
    @GetMapping("/scholarships")
    public ResponseEntity<List<StudentFeeRecord>> getStudentsWithScholarships() {
        log.info("REST request to get students with scholarships");
        List<StudentFeeRecord> feeRecords = feeRecordService.getStudentsWithScholarships();
        return ResponseEntity.ok(feeRecords);
    }

    /**
     * Get students with outstanding balance
     * 
     * GET /api/students/outstanding-balance
     * 
     * Returns fee records for students who still owe money
     */
    @GetMapping("/outstanding-balance")
    public ResponseEntity<List<StudentFeeRecord>> getStudentsWithOutstandingBalance() {
        log.info("REST request to get fee records with outstanding balance");
        List<StudentFeeRecord> feeRecords = feeRecordService.getRecordsWithOutstandingBalance();
        return ResponseEntity.ok(feeRecords);
    }

    /**
     * Get students who have fully paid
     * 
     * GET /api/students/fully-paid
     * 
     * Returns fee records with zero outstanding balance
     */
    @GetMapping("/fully-paid")
    public ResponseEntity<List<StudentFeeRecord>> getFullyPaidStudents() {
        log.info("REST request to get fully paid fee records");
        List<StudentFeeRecord> feeRecords = feeRecordService.getFullyPaidRecords();
        return ResponseEntity.ok(feeRecords);
    }

    /**
     * Search students with arrears by name (DEPRECATED)
     * 
     * GET /api/students/arrears/search?name=John
     * 
     * NOTE: This endpoint is deprecated. Use:
     * 1. GET /api/students/search?name=John to find students
     * 2. GET /api/students/outstanding-balance to get arrears
     * 3. Filter client-side or create specific endpoint if needed
     */
    @GetMapping("/arrears/search")
    @Deprecated
    public ResponseEntity<String> searchStudentsWithArrears(@RequestParam String name) {
        log.warn("DEPRECATED endpoint called: /arrears/search");
        return ResponseEntity.status(HttpStatus.GONE)
            .body("This endpoint is deprecated. Use /api/students/search and /api/students/outstanding-balance separately.");
    }

    /**
     * Get students by grade (simplified)
     * 
     * GET /api/students/grade/{grade}/payment-status/{status}
     * 
     * NOTE: Complex filtering moved to client-side or use separate queries:
     * 1. GET /api/students/grade/{grade}
     * 2. GET /api/students/payment-status/{status}
     * 3. Join results client-side
     */
    @GetMapping("/grade/{grade}/payment-status/{status}")
    @Deprecated
    public ResponseEntity<String> getStudentsByGradeAndPaymentStatus(
            @PathVariable String grade,
            @PathVariable String status) {
        log.warn("DEPRECATED endpoint called: /grade/{}/payment-status/{}", grade, status);
        return ResponseEntity.status(HttpStatus.GONE)
            .body("This endpoint is deprecated. Use /api/students/grade/ and /api/students/payment-status/ separately.");
    }

    /**
     * Record a payment for a student
     * 
     * POST /api/students/{id}/payment
     * 
     * Body example:
     * {
     *   "amount": 500.00,
     *   "paymentMethod": "MOBILE_MONEY",
     *   "transactionReference": "MM123456",
     *   "receivedBy": "Bursar Name",
     *   "paymentNotes": "Tuition payment"
     * }
     * 
     * LEARNING: Now creates a Payment entity
     * - Records transaction history
     * - Updates StudentFeeRecord automatically
     * - Provides audit trail
     */
    @PostMapping("/{id}/payment")
    public ResponseEntity<Payment> recordPayment(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> payload) {
        log.info("REST request to record payment for student ID: {}", id);
        
        // Extract payment details
        Object amountObj = payload.get("amount");
        if (amountObj == null) {
            return ResponseEntity.badRequest().build();
        }
        
        BigDecimal amount = new BigDecimal(amountObj.toString());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        // Create payment object
        Payment payment = new Payment();
        payment.setAmount(amount);
        
        // Parse payment method from string
        String methodStr = (String) payload.getOrDefault("paymentMethod", "CASH");
        payment.setPaymentMethod(PaymentMethod.fromString(methodStr));
        
        payment.setTransactionReference((String) payload.get("transactionReference"));
        payment.setReceivedBy((String) payload.get("receivedBy"));
        payment.setPaymentNotes((String) payload.get("paymentNotes"));
        payment.setStatus("COMPLETED");
        payment.setPaymentDate(LocalDateTime.now());
        
        try {
            Payment savedPayment = studentService.recordPayment(id, payment);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPayment);
        } catch (IllegalArgumentException e) {
            log.error("Error recording payment: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /* ----------------------  BURSAR FINANCIAL REPORTS  ------------------------- */

    /**
     * Get total outstanding fees across all students
     * 
     * GET /api/students/reports/total-outstanding
     * 
     * LEARNING: Now uses StudentFeeRecordService
     * Returns single number: total money owed by all students
     */
    @GetMapping("/reports/total-outstanding")
    public ResponseEntity<BigDecimal> getTotalOutstandingFees() {
        log.info("REST request to get total outstanding fees");
        BigDecimal total = feeRecordService.getTotalOutstandingFees();
        return ResponseEntity.ok(total);
    }

    /**
     * Get total collected fees across all students
     * 
     * GET /api/students/reports/total-collected
     * 
     * Returns single number: total money received from all students
     */
    @GetMapping("/reports/total-collected")
    public ResponseEntity<BigDecimal> getTotalCollectedFees() {
        log.info("REST request to get total collected fees");
        BigDecimal total = feeRecordService.getTotalCollectedFees();
        return ResponseEntity.ok(total);
    }

    /**
     * Get count of fee records by payment status
     * 
     * GET /api/students/reports/count-by-payment-status?status=ARREARS
     * 
     * Example: How many students have arrears?
     */
    @GetMapping("/reports/count-by-payment-status")
    public ResponseEntity<Long> countByPaymentStatus(@RequestParam String status) {
        log.info("REST request to count fee records with payment status: {}", status);
        long count = feeRecordService.countByPaymentStatus(status);
        return ResponseEntity.ok(count);
    }

    /**
     * Health check endpoint
     * 
     * GET /api/students/health
     * 
     * Simple endpoint to verify the API is running
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Student API is running! 🎓");
    }

    /**
     * EXCEL UPLOAD - Bulk import students from Excel file
     * 
     * POST /api/students/upload-excel
     * 
     * NEW CONCEPTS:
     * 
     * 1. FILE UPLOAD IN REST API
     *    - Uses multipart/form-data content type (not JSON)
     *    - File sent as part of form data
     *    - @RequestParam("file") tells Spring to extract file from form
     * 
     * 2. MULTIPARTFILE INTERFACE
     *    - Spring's way of handling uploaded files
     *    - Provides: getOriginalFilename(), getInputStream(), getSize()
     *    - Works with any file type (Excel, PDF, images, etc.)
     * 
     * 3. BULK OPERATIONS
     *    - Process many records in one request
     *    - More efficient than calling POST /api/students 50 times
     *    - All imports happen in one transaction
     * 
     * 4. ERROR HANDLING STRATEGY
     *    - Don't fail entire upload if one student has error
     *    - Continue processing all rows
     *    - Return summary: "45 success, 5 failures"
     *    - Include details of failures so bursar can fix them
     * 
     * EXPECTED EXCEL FORMAT:
     * Row 1 (Headers): Student ID | First Name | Last Name | Date of Birth | Grade | Parent Name | Parent Phone | Parent Email | Address
     * Row 2+: Student data
     * 
     * HOW TO TEST IN POSTMAN:
     * 1. Select POST method
     * 2. URL: http://localhost:8080/api/students/upload-excel
     * 3. Go to "Body" tab
     * 4. Select "form-data" (NOT raw JSON!)
     * 5. Add key "file", change type from "Text" to "File"
     * 6. Click "Select Files" and choose your Excel file
     * 7. Send!
     * 
     * RESPONSE EXAMPLE:
     * {
     *   "totalRows": 50,
     *   "successCount": 45,
     *   "failureCount": 5,
     *   "results": [
     *     {
     *       "rowNumber": 3,
     *       "firstName": "John",
     *       "lastName": "Doe",
     *       "success": false,
     *       "errorMessage": "Student ID already exists"
     *     }
     *   ]
     * }
     * 
     * @param file - The Excel file uploaded by the bursar
     * @return UploadSummary - Report of successes and failures
     */
    @PostMapping("/upload-excel")
    public ResponseEntity<?> uploadExcelFile(@RequestParam("file") MultipartFile file) {
        log.info("REST request to upload Excel file: {}", file.getOriginalFilename());
        
        try {
            // Validate file is Excel
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return ResponseEntity
                    .badRequest()
                    .body("Please upload a valid Excel file (.xlsx or .xls)");
            }
            
            // Process the file
            UploadSummary summary = excelService.processExcelFile(file);
            
            // Return 200 OK with summary
            // Even if some rows failed, we return 200 because the request itself was successful
            // The client can check successCount vs failureCount to determine overall result
            log.info("Excel upload complete: {} succeeded, {} failed", 
                summary.getSuccessCount(), summary.getFailureCount());
            
            return ResponseEntity.ok(summary);
            
        } catch (IllegalArgumentException e) {
            // Bad request - invalid input
            log.error("Invalid Excel file: {}", e.getMessage());
            return ResponseEntity
                .badRequest()
                .body("Invalid file: " + e.getMessage());
                
        } catch (Exception e) {
            // Internal server error - something unexpected happened
            log.error("Error processing Excel file", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing file: " + e.getMessage());
        }
    }
}
