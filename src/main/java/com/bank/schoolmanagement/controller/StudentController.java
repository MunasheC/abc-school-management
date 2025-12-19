
package com.bank.schoolmanagement.controller;

import com.bank.schoolmanagement.dto.UploadSummary;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.dto.StudentResponse;
import com.bank.schoolmanagement.service.ExcelService;
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
}
