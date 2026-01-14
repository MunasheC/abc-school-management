
package com.bank.schoolmanagement.controller;

import com.bank.schoolmanagement.dto.BulkPromotionSummary;
import com.bank.schoolmanagement.dto.StudentPromotionRequest;
import com.bank.schoolmanagement.dto.UploadSummary;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.entity.StudentFeeRecord;
import com.bank.schoolmanagement.dto.StudentResponse;
import com.bank.schoolmanagement.service.ExcelService;
import com.bank.schoolmanagement.service.StudentFeeRecordService;
import com.bank.schoolmanagement.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



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
@Tag(name = "Student Management (School/Bursar)", description = "Endpoints for school administrators and bursars to manage student enrollment, records, and bulk operations. Requires X-School-ID header.")
@SecurityRequirement(name = "X-School-ID")
public class StudentController {

    private final StudentService studentService;
    private final ExcelService excelService;
    private final StudentFeeRecordService feeRecordService;
    

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
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody Student student) {
        log.info("REST request to create student: {} {}", student.getFirstName(), student.getLastName());
        Student createdStudent = studentService.createStudentForCurrentSchool(student);
        return ResponseEntity.status(HttpStatus.CREATED).body(StudentResponse.fromEntity(createdStudent));
    }

    /**
     * READ - Get all students
     * 
     * GET /api/students
     * 
     * Returns JSON array of all students
     */
    @GetMapping
    public ResponseEntity<Page<StudentResponse>> getAllStudents(Pageable pageable) {
        log.info("REST request to get all students");
        Page<StudentResponse> students = studentService.getAllStudentsForCurrentSchool(pageable)
            .map(StudentResponse::fromEntity);
        return ResponseEntity.ok(students);
    }

    /**
     * READ - Get active students only
     * 
     * GET /api/students/active
     */
    // @GetMapping("/active")
    // public ResponseEntity<List<StudentResponse>> getActiveStudents() {
    //     log.info("REST request to get active students");
    //     List<StudentResponse> students = studentService.getActiveStudents().stream()
    //         .map(StudentResponse::fromEntity)
    //         .toList();
    //     return ResponseEntity.ok(students);
    // }

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
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Long id) {
        log.info("REST request to get student with ID: {}", id);
        return studentService.getStudentByIdForCurrentSchool(id)
                .map(StudentResponse::fromEntity)
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
    public ResponseEntity<StudentResponse> getStudentByStudentId(@PathVariable String studentId) {
        log.info("REST request to get student with student ID: {}", studentId);
        return studentService.getStudentByStudentIdForCurrentSchool(studentId)
                .map(StudentResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @RequestBody Student student) {  // Removed @Valid for partial updates
        log.info("REST request to update student with ID: {}", id);
        try {
            Student updatedStudent = studentService.updateStudentForCurrentSchool(id, student);
            return ResponseEntity.ok(StudentResponse.fromEntity(updatedStudent));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

        /**
     * UPDATE - Update student by studentId (PARTIAL UPDATE)
     * PUT /api/school/students/by-student-id/{studentId}
     */
    @PutMapping("/by-student-id/{studentId}")
    public ResponseEntity<StudentResponse> updateStudentByStudentId(
            @PathVariable String studentId,
            @RequestBody Student student) {
        log.info("REST request to update student with studentId: {}", studentId);
        Student updatedStudent = studentService.updateStudentByStudentIdForCurrentSchool(studentId, student);
        return ResponseEntity.ok(StudentResponse.fromEntity(updatedStudent));
    }

    /**
     * UPDATE - Update student by nationalId (PARTIAL UPDATE)
     * PUT /api/school/students/by-national-id/{nationalId}
     */
    @PutMapping("/by-national-id/{nationalId}")
    public ResponseEntity<StudentResponse> updateStudentByNationalId(
            @PathVariable String nationalId,
            @RequestBody Student student) {
        log.info("REST request to update student with nationalId: {}", nationalId);
        Student updatedStudent = studentService.updateStudentByNationalIdForCurrentSchool(nationalId, student);
        return ResponseEntity.ok(StudentResponse.fromEntity(updatedStudent));
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
    public ResponseEntity<StudentResponse> deactivateStudent(@PathVariable Long id) {
        log.info("REST request to deactivate student with ID: {}", id);
        try {
            Student deactivatedStudent = studentService.deactivateStudent(id);
            return ResponseEntity.ok(StudentResponse.fromEntity(deactivatedStudent));
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
    public ResponseEntity<StudentResponse> reactivateStudent(@PathVariable Long id) {
        log.info("REST request to reactivate student with ID: {}", id);
        try {
            Student reactivatedStudent = studentService.reactivateStudent(id);
            return ResponseEntity.ok(StudentResponse.fromEntity(reactivatedStudent));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
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
    @Operation(
        summary = "Bulk enroll students via Excel upload",
        description = "Upload an Excel file (.xlsx or .xls) to enroll multiple students at once. " +
                     "The file must have specific columns (Student ID, First Name, Last Name, Parent Name, Parent Phone, etc.). " +
                     "Returns a summary showing how many students were successfully enrolled and which ones failed with error details. " +
                     "Supports partial success - valid students are saved even if some rows have errors. " +
                     "Automatically detects siblings via shared parent phone numbers and creates fee records if fee data is included."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File processed successfully (check successCount vs failureCount in response)", 
                    content = @Content(schema = @Schema(implementation = UploadSummary.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file format or empty file"),
        @ApiResponse(responseCode = "500", description = "Error processing file")
    })
    @PostMapping("/upload-excel")
    public ResponseEntity<?> uploadExcelFile(
            @Parameter(description = "Excel file (.xlsx or .xls) containing student data. " +
                                    "Required columns: First Name, Last Name, Parent Name, Parent Phone. " +
                                    "See documentation for complete column list.", 
                      required = true)
            @RequestParam("file") MultipartFile file) {
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
    
    /**
     * PROMOTE - Promote individual student to next grade
     * 
     * POST /api/school/students/{id}/promote
     * 
     * PURPOSE: Handle grade progression for individual student
     * - Updates student's grade and className
     * - Creates new fee record for new academic year
     * - Optionally carries forward outstanding balance
     * 
     * LEARNING: Promotion workflow
     * 1. Update student academic info (grade, class)
     * 2. Get current fee record to check outstanding balance
     * 3. Create new fee record for new term/year
     * 4. Audit trail captures before/after states
     * 
     * Example Usage: Promote Tanaka from Form 1 to Form 2 for 2026
     * 
     * @param id Student database ID
     * @param promotionRequest Promotion details and new fee structure
     * @return Promoted student with new fee record details
     */
    @PostMapping("/{id}/promote")
    @Operation(
        summary = "Promote student to next grade",
        description = "Updates student grade/class and creates new fee record for new academic year. Optionally carries forward outstanding balance."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student promoted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid promotion request"),
        @ApiResponse(responseCode = "404", description = "Student not found")
    })
    public ResponseEntity<?> promoteStudent(
            @PathVariable Long id,
            @RequestBody StudentPromotionRequest promotionRequest) {
        
        try {
            log.info("Promoting student ID {} to grade: {}, year: {}, term: {}", 
                id, promotionRequest.getNewGrade(), promotionRequest.getNewYear(), promotionRequest.getNewTerm());
            
            // 1. Get current fee record to check outstanding balance
            final java.math.BigDecimal previousBalance;
            if (Boolean.TRUE.equals(promotionRequest.getCarryForwardBalance())) {
                java.util.Optional<StudentFeeRecord> currentFeeRecord = 
                    feeRecordService.getLatestFeeRecordForStudent(id);
                
                if (currentFeeRecord.isPresent() && 
                    currentFeeRecord.get().getOutstandingBalance() != null) {
                    previousBalance = currentFeeRecord.get().getOutstandingBalance();
                    log.info("Carrying forward balance: {} for student ID: {}", previousBalance, id);
                } else {
                    previousBalance = java.math.BigDecimal.ZERO;
                }
            } else {
                previousBalance = java.math.BigDecimal.ZERO;
            }
            
            // 2. Promote student (update grade and className)
            Student promotedStudent = studentService.promoteStudent(
                id,
                promotionRequest.getNewGrade(),
                promotionRequest.getNewClassName(),
                promotionRequest.getPromotionNotes()
            );
            
            // 3. Create new fee record for promoted grade/term
            StudentFeeRecord newFeeRecord = feeRecordService.createPromotionFeeRecord(
                promotedStudent,
                promotionRequest.getNewYear(),
                promotionRequest.getNewTerm(),
                previousBalance,
                promotionRequest.getTuitionFee(),
                promotionRequest.getBoardingFee(),
                promotionRequest.getDevelopmentLevy(),
                promotionRequest.getExamFee(),
                promotionRequest.getOtherFees(),
                promotionRequest.getScholarshipAmount(),
                promotionRequest.getSiblingDiscount(),
                promotionRequest.getFeeCategory()
            );
            
            // 4. Build response with student and fee details
            String message = String.format(
                "Student %s %s promoted from %s to %s. New fee record created for %s. Outstanding balance: %s",
                promotedStudent.getFirstName(),
                promotedStudent.getLastName(),
                promotionRequest.getNewGrade(), // We don't have old grade here, using new for now
                promotedStudent.getGrade(),
                promotionRequest.getNewYear(),
                promotionRequest.getNewTerm(),
                newFeeRecord.getOutstandingBalance()
            );
            
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("message", message);
                put("student", StudentResponse.fromEntity(promotedStudent));
                put("newFeeRecord", new java.util.HashMap<String, Object>() {{
                    put("id", newFeeRecord.getId());
                    put("year", newFeeRecord.getYear());
                    put("term", newFeeRecord.getTerm());
                    put("netAmount", newFeeRecord.getNetAmount());
                    put("previousBalance", previousBalance);
                    put("outstandingBalance", newFeeRecord.getOutstandingBalance());
                    put("paymentStatus", newFeeRecord.getPaymentStatus());
                }});
            }});
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid promotion request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error promoting student ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error promoting student: " + e.getMessage());
        }
    }
    
    /**
     * PROMOTE STUDENT BY STUDENT ID - Promote individual student using school's student reference
     * 
     * POST /api/school/students/by-reference/{studentId}/promote
     * 
     * DIFFERENCE FROM DATABASE ID ENDPOINT:
     * - Uses studentId (school's reference like "STU-F1-0001") instead of database ID
     * - Automatically scopes to current school context
     * - More intuitive for school users who know student references
     * 
     * Example: POST /api/school/students/by-studentID/STU-F1-0001/promote
     */
    @PostMapping("/by-studentID/{studentId}/promote")
    @Operation(
        summary = "Promote student by Student ID",
        description = "Promotes a student to next grade using their student reference ID (not database ID). " +
                      "Updates grade/class and creates new fee record. Automatically scoped to current school."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student promoted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid promotion request"),
        @ApiResponse(responseCode = "404", description = "Student not found in current school")
    })
    public ResponseEntity<?> promoteStudentByStudentId(
            @Parameter(description = "Student's reference ID (e.g., 'STU2025001', '2025001')", required = true)
            @PathVariable String studentId,
            @Valid @RequestBody StudentPromotionRequest promotionRequest) {
        
        try {
            log.info("Promoting student {} to grade: {}, year: {}, term: {}", 
                studentId, promotionRequest.getNewGrade(), promotionRequest.getNewYear(), promotionRequest.getNewTerm());
            
            // 1. Find student by studentId in current school context
            java.util.Optional<Student> studentOpt = studentService.getStudentByStudentId(studentId);
            if (!studentOpt.isPresent()) {
                log.error("Student with ID {} not found in current school", studentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Student with ID " + studentId + " not found in current school");
            }
            
            Student student = studentOpt.get();
            String oldGrade = student.getGrade();
            
            // 2. Get current fee record to check outstanding balance
            final java.math.BigDecimal previousBalance;
            if (Boolean.TRUE.equals(promotionRequest.getCarryForwardBalance())) {
                java.util.Optional<StudentFeeRecord> currentFeeRecord = 
                    feeRecordService.getLatestFeeRecordForStudent(student.getId());
                
                if (currentFeeRecord.isPresent() && 
                    currentFeeRecord.get().getOutstandingBalance() != null) {
                    previousBalance = currentFeeRecord.get().getOutstandingBalance();
                    log.info("Carrying forward balance: {} for student {}", previousBalance, studentId);
                } else {
                    previousBalance = java.math.BigDecimal.ZERO;
                }
            } else {
                previousBalance = java.math.BigDecimal.ZERO;
            }
            
            // 3. Promote student (update grade and className)
            Student promotedStudent = studentService.promoteStudent(
                student.getId(),
                promotionRequest.getNewGrade(),
                promotionRequest.getNewClassName(),
                promotionRequest.getPromotionNotes()
            );
            
            // 4. Create new fee record for promoted grade/term
            StudentFeeRecord newFeeRecord = feeRecordService.createPromotionFeeRecord(
                promotedStudent,
                promotionRequest.getNewYear(),
                promotionRequest.getNewTerm(),
                previousBalance,
                promotionRequest.getTuitionFee(),
                promotionRequest.getBoardingFee(),
                promotionRequest.getDevelopmentLevy(),
                promotionRequest.getExamFee(),
                promotionRequest.getOtherFees(),
                promotionRequest.getScholarshipAmount(),
                promotionRequest.getSiblingDiscount(),
                promotionRequest.getFeeCategory()
            );
            
            // 5. Build response with student and fee details
            String message = String.format(
                "Student %s %s (ID: %s) promoted from %s to %s. New fee record created for %s. Outstanding balance: $%.2f",
                promotedStudent.getFirstName(),
                promotedStudent.getLastName(),
                studentId,
                oldGrade,
                promotedStudent.getGrade(),
                promotionRequest.getNewYear(),
                promotionRequest.getNewTerm(),
                newFeeRecord.getOutstandingBalance()
            );
            
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("message", message);
                put("student", StudentResponse.fromEntity(promotedStudent));
                put("oldGrade", oldGrade);
                put("newGrade", promotedStudent.getGrade());
                put("newFeeRecord", new java.util.HashMap<String, Object>() {{
                    put("id", newFeeRecord.getId());
                    put("year", newFeeRecord.getYear());
                    put("term", newFeeRecord.getTerm());
                    put("netAmount", newFeeRecord.getNetAmount());
                    put("previousBalance", previousBalance);
                    put("outstandingBalance", newFeeRecord.getOutstandingBalance());
                    put("paymentStatus", newFeeRecord.getPaymentStatus());
                }});
            }});
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid promotion request for student {}: {}", studentId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error promoting student {}: {}", studentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error promoting student: " + e.getMessage());
        }
    }
    
    /**
     * DEMOTE - Demote student back to previous grade (for repeating students)
     * 
     * POST /api/school/students/{id}/demote
     * 
     * PURPOSE: Revert incorrectly promoted student or mark as repeating
     * 
     * USE CASE: After automatic year-end promotion, admin realizes student
     * should be repeating the grade due to failed exams or other reasons
     * 
     * FEATURES:
     * - Demotes student back to specified grade
     * - Clears completion status if student was marked as completed
     * - Reactivates student if they were deactivated
     * - Creates new fee record for repeated grade
     * - Optionally carries forward outstanding balance
     * 
     * Example: Student auto-promoted Form 1→Form 2, but should repeat Form 1
     * 
     * @param id Student database ID
     * @param demotionRequest Demotion details and fee structure
     * @return Demoted student with new fee record details
     */
    @PostMapping("/{id}/demote")
    @Operation(
        summary = "Demote student back to previous grade",
        description = "Reverts student to a previous grade (e.g., for repeating students). Creates new fee record for repeated grade. Clears completion status and reactivates if needed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student demoted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid demotion request"),
        @ApiResponse(responseCode = "404", description = "Student not found")
    })
    public ResponseEntity<?> demoteStudent(
            @PathVariable Long id,
            @RequestBody com.bank.schoolmanagement.dto.StudentDemotionRequest demotionRequest) {
        
        try {
            log.info("Demoting student ID {} back to grade: {}, year: {}, term: {}", 
                id, demotionRequest.getDemotedGrade(), demotionRequest.getYear(), demotionRequest.getTerm());
            
            // 1. Get current student state
            Student student = studentService.getStudentByIdForCurrentSchool(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + id));
            String oldGrade = student.getGrade();
            
            // 2. Get current fee record to check outstanding balance
            final java.math.BigDecimal previousBalance;
            if (demotionRequest.getCarryForwardBalance()) {
                java.util.Optional<StudentFeeRecord> currentFeeRecord = 
                    feeRecordService.getLatestFeeRecordForStudent(id);
                
                if (currentFeeRecord.isPresent() && 
                    currentFeeRecord.get().getOutstandingBalance() != null) {
                    previousBalance = currentFeeRecord.get().getOutstandingBalance();
                    log.info("Carrying forward balance: {} for student {}", previousBalance, id);
                } else {
                    previousBalance = java.math.BigDecimal.ZERO;
                }
            } else {
                previousBalance = java.math.BigDecimal.ZERO;
            }
            
            // 3. Demote student (update grade, clear completion status, reactivate)
            Student demotedStudent = studentService.demoteStudent(
                id,
                demotionRequest.getDemotedGrade(),
                demotionRequest.getDemotedClassName(),
                demotionRequest.getReason()
            );
            
            // 4. Create new fee record for repeated grade
            StudentFeeRecord newFeeRecord = feeRecordService.createPromotionFeeRecord(
                demotedStudent,
                demotionRequest.getYear(),
                demotionRequest.getTerm(),
                previousBalance,
                demotionRequest.getTuitionFee(),
                demotionRequest.getBoardingFee(),
                demotionRequest.getDevelopmentLevy(),
                demotionRequest.getExamFee(),
                demotionRequest.getOtherFees(),
                java.math.BigDecimal.ZERO, // scholarshipAmount
                java.math.BigDecimal.ZERO, // siblingDiscount
                null // feeCategory
            );
            
            // 5. Build response with student and fee details
            String message = String.format(
                "Student %s %s (ID: %s) demoted from %s back to %s. Reason: %s. New fee record created for %s. Outstanding balance: $%.2f",
                demotedStudent.getFirstName(),
                demotedStudent.getLastName(),
                id,
                oldGrade,
                demotedStudent.getGrade(),
                demotionRequest.getReason(),
                demotionRequest.getYear(),
                demotionRequest.getTerm(),
                newFeeRecord.getOutstandingBalance()
            );
            
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("message", message);
                put("student", StudentResponse.fromEntity(demotedStudent));
                put("oldGrade", oldGrade);
                put("demotedGrade", demotedStudent.getGrade());
                put("reason", demotionRequest.getReason());
                put("newFeeRecord", new java.util.HashMap<String, Object>() {{
                    put("id", newFeeRecord.getId());
                    put("year", newFeeRecord.getYear());
                    put("term", newFeeRecord.getTerm());
                    put("netAmount", newFeeRecord.getNetAmount());
                    put("previousBalance", previousBalance);
                    put("outstandingBalance", newFeeRecord.getOutstandingBalance());
                    put("paymentStatus", newFeeRecord.getPaymentStatus());
                }});
            }});
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid demotion request for student {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error demoting student {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error demoting student: " + e.getMessage());
        }
    }
    
    /**
     * BULK PROMOTE - Promote all students in a grade
     * 
     * POST /api/school/students/bulk-promote
     * 
     * PURPOSE: Year-end mass promotion (e.g., all Form 1 → Form 2)
     * 
     * LEARNING: Bulk promotion workflow
     * - Promotes entire grade at once
     * - Each student gets individual promotion logic applied
     * - Continues on errors (partial success possible)
     * - Returns detailed summary of successes and failures
     * 
     * Example: Promote all Form 1 students to Form 2 for Term 1 2026
     * 
     * Request Body:
     * {
     *   "currentGrade": "Form 1",
     *   "newGrade": "Form 2",
     *   "newClassName": null,  // Will keep their current class names
     *   "newTermYear": "Term 1 2026",
     *   "carryForwardBalance": true,
     *   "tuitionFee": 600.00,
     *   "boardingFee": 350.00,
     *   ...
     * }
     */
    @PostMapping("/bulk-promote")
    @Operation(
        summary = "Bulk promote students by grade",
        description = "Promotes all active students in a specified grade to the next grade. Creates new fee records for new academic year."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bulk promotion completed (check summary for individual results)"),
        @ApiResponse(responseCode = "400", description = "Invalid bulk promotion request")
    })
    public ResponseEntity<?> bulkPromoteStudents(
            @RequestParam String currentGrade,
            @RequestBody StudentPromotionRequest promotionRequest) {
        
        try {
            log.info("Starting bulk promotion: {} → {}, Year: {}, Term: {}", 
                currentGrade, promotionRequest.getNewGrade(), promotionRequest.getNewYear(), promotionRequest.getNewTerm());
            
            // Get all students in the current grade
            java.util.List<Student> studentsToPromote = 
                studentService.getStudentsByGradeForPromotion(currentGrade);
            
            if (studentsToPromote.isEmpty()) {
                log.warn("No active students found in grade: {}", currentGrade);
                return ResponseEntity.badRequest()
                    .body("No active students found in grade: " + currentGrade);
            }
            
            // Initialize summary
            BulkPromotionSummary summary = new BulkPromotionSummary();
            summary.setTotalStudents(studentsToPromote.size());
            summary.setSuccessCount(0);
            summary.setFailureCount(0);
            
            // Process each student
            for (Student student : studentsToPromote) {
                try {
                    // Get previous balance if carrying forward
                    java.math.BigDecimal previousBalance = java.math.BigDecimal.ZERO;
                    if (Boolean.TRUE.equals(promotionRequest.getCarryForwardBalance())) {
                        java.util.Optional<StudentFeeRecord> currentFeeRecord = 
                            feeRecordService.getLatestFeeRecordForStudent(student.getId());
                        
                        if (currentFeeRecord.isPresent() && 
                            currentFeeRecord.get().getOutstandingBalance() != null) {
                            previousBalance = currentFeeRecord.get().getOutstandingBalance();
                        }
                    }
                    
                    // Promote student
                    String newClassName = promotionRequest.getNewClassName() != null 
                        ? promotionRequest.getNewClassName() 
                        : student.getClassName(); // Keep current class if not specified
                    
                    Student promoted = studentService.promoteStudent(
                        student.getId(),
                        promotionRequest.getNewGrade(),
                        newClassName,
                        "Bulk promotion from " + currentGrade + " to " + promotionRequest.getNewGrade()
                    );
                    
                    // Create new fee record
                    feeRecordService.createPromotionFeeRecord(
                        promoted,
                        promotionRequest.getNewYear(),
                        promotionRequest.getNewTerm(),
                        previousBalance,
                        promotionRequest.getTuitionFee(),
                        promotionRequest.getBoardingFee(),
                        promotionRequest.getDevelopmentLevy(),
                        promotionRequest.getExamFee(),
                        promotionRequest.getOtherFees(),
                        promotionRequest.getScholarshipAmount(),
                        promotionRequest.getSiblingDiscount(),
                        promotionRequest.getFeeCategory()
                    );
                    
                    // Success
                    summary.setSuccessCount(summary.getSuccessCount() + 1);
                    summary.getPromotedStudentIds().add(promoted.getStudentId());
                    
                    log.debug("Successfully promoted: {} {} ({})", 
                        student.getFirstName(), student.getLastName(), student.getStudentId());
                    
                } catch (Exception e) {
                    // Error with individual student - log and continue
                    summary.setFailureCount(summary.getFailureCount() + 1);
                    summary.getErrors().add(new BulkPromotionSummary.PromotionError(
                        student.getId(),
                        student.getFirstName() + " " + student.getLastName(),
                        e.getMessage()
                    ));
                    
                    log.error("Failed to promote student {} {}: {}", 
                        student.getFirstName(), student.getLastName(), e.getMessage());
                }
            }
            
            // Set summary message
            summary.setMessage(String.format(
                "Bulk promotion completed: %d of %d students promoted from %s to %s",
                summary.getSuccessCount(),
                summary.getTotalStudents(),
                currentGrade,
                promotionRequest.getNewGrade()
            ));
            
            log.info("Bulk promotion complete: {} succeeded, {} failed", 
                summary.getSuccessCount(), summary.getFailureCount());
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            log.error("Error in bulk promotion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error in bulk promotion: " + e.getMessage());
        }
    }
    
    /**
     * YEAR-END PROMOTION - Atomic promotion of all grades simultaneously
     * 
     * POST /api/school/students/year-end-promotion
     * 
     * CRITICAL: This endpoint prevents the double-promotion bug by processing all grades
     * in a single atomic operation. All students are promoted based on their INITIAL grade,
     * not their updated grade.
     * 
     * EXAMPLE: If you promote:
     * - Form 1 students → Form 2
     * - Form 2 students → Form 3
     * - Form 3 students → Form 4
     * 
     * The newly promoted Form 2 students will NOT be promoted again to Form 3
     * because the snapshot is taken before any changes.
     * 
     * FEATURES:
     * - School-type-aware progression (Primary uses Grades, Secondary uses Forms)
     * - Completion tracking (COMPLETED_PRIMARY, COMPLETED_O_LEVEL, COMPLETED_A_LEVEL)
     * - Excluded students (can skip specific students who are repeating)
     * - Grade-specific fee structures
     * - Carry forward outstanding balances
     * 
     * Request Body Example:
     * {
     *   "newAcademicYear": "2026",
     *   "carryForwardBalances": true,
     *   "excludedStudentIds": [123, 456],
     *   "promotionNotes": "Year-end promotion 2025 → 2026",
     *   "feeStructures": {
     *     "Form 2": {
     *       "tuitionFee": 500.00,
     *       "examinationFee": 50.00,
     *       "otherFees": 25.00
     *     },
     *     "Form 3": {
     *       "tuitionFee": 550.00,
     *       "examinationFee": 60.00,
     *       "otherFees": 30.00
     *     }
     *   },
     *   "defaultFeeStructure": {
     *     "tuitionFee": 450.00,
     *     "examinationFee": 40.00,
     *     "otherFees": 20.00
     *   }
     * }
     */
    @PostMapping("/year-end-promotion")
    @Operation(
        summary = "Year-End Promotion (Atomic)",
        description = "Promotes all grades/forms simultaneously to prevent double-promotion. " +
                      "School-type-aware (Primary: Grades 1-7, Secondary: Forms 1-6). " +
                      "Tracks completion statuses for students finishing education."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Year-end promotion completed successfully",
            content = @Content(schema = @Schema(implementation = com.bank.schoolmanagement.dto.YearEndPromotionSummary.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request (missing required fields, invalid academic year, etc.)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Server error during promotion"
        )
    })
    public ResponseEntity<?> performYearEndPromotion(
            @Valid @RequestBody com.bank.schoolmanagement.dto.YearEndPromotionRequest request) {
        
        log.info("REST request for year-end promotion: academic year {}", request.getNewYear());
        
        try {
            // Validate request
            if (request.getNewYear() == null && request.getNewTerm() == null || request.getNewYear() == 0 && request.getNewTerm() == 0) {
                return ResponseEntity.badRequest()
                    .body("Academic year and term are required");
            }
            
            // Perform atomic year-end promotion
            com.bank.schoolmanagement.dto.YearEndPromotionSummary summary = 
                studentService.performYearEndPromotion(request);
            
            log.info("Year-end promotion completed: {} promoted, {} completed, {} errors",
                summary.getPromotedCount(), summary.getCompletedCount(), summary.getErrorCount());
            
            return ResponseEntity.ok(summary);
            
        } catch (IllegalStateException e) {
            log.error("Invalid state for year-end promotion: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
            
        } catch (Exception e) {
            log.error("Error in year-end promotion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error in year-end promotion: " + e.getMessage());
        }
    }
}
