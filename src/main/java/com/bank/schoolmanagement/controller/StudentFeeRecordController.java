 
package com.bank.schoolmanagement.controller;

import com.bank.schoolmanagement.dto.StudentFeeRecordResponse;
import com.bank.schoolmanagement.entity.StudentFeeRecord;
import com.bank.schoolmanagement.service.StudentFeeRecordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;



/**
 * StudentFeeRecord Controller - REST API for Financial Records
 * 
 * BASE PATH: /api/fee-records
 * 
 * LEARNING: Separate controller for financial operations
 * - Manages fee records per student per term
 * - Tracks payments and balances
 * - Provides financial reports for bursar
 */
@RestController
@RequestMapping("/api/school/fee-records")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Student Fee Record Management", description = "Endpoints for school administrators and bursars to manage student fee records and bulk operations. Requires X-School-ID header.")
@SecurityRequirement(name = "X-School-ID")

public class StudentFeeRecordController {

    private final StudentFeeRecordService feeRecordService;

    /**
     * CREATE - Add a new fee record
     * 
     * POST /api/fee-records
     * 
     * Body example:
     * {
     *   "student": { "id": 1 },
     *   "termYear": "2025-Term1",
     *   "feeCategory": "Boarder",
     *   "tuitionFee": 500.00,
     *   "boardingFee": 300.00,
     *   "developmentLevy": 50.00,
     *   "examFee": 25.00,
     *   "previousBalance": 0.00,
     *   "amountPaid": 0.00,
     *   "scholarshipAmount": 100.00,
     *   "siblingDiscount": 50.00
     * }
     * 
     * NOTE: grossAmount, netAmount, outstandingBalance calculated automatically
     */
    @PostMapping
    public ResponseEntity<StudentFeeRecordResponse> createFeeRecord(@Valid @RequestBody StudentFeeRecord feeRecord) {
        log.info("REST request to create fee record for student ID: {}", 
                 feeRecord.getStudent() != null ? feeRecord.getStudent().getId() : "null");
        StudentFeeRecord created = feeRecordService.createFeeRecord(feeRecord);
        StudentFeeRecordResponse dto = StudentFeeRecordResponse.fromEntity(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * READ - Get fee record by ID
     * 
     * GET /api/fee-records/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<StudentFeeRecordResponse> getFeeRecordById(@PathVariable Long id) {
        log.info("REST request to get fee record with ID: {}", id);
        return feeRecordService.getFeeRecordById(id)
                .map(StudentFeeRecordResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Get latest fee record for a student by database ID
     * 
     * GET /api/fee-records/student/{studentId}
     * 
     * Returns the most recent fee record for the student (by database ID)
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<StudentFeeRecordResponse> getFeeRecordByStudentId(@PathVariable Long studentId) {
        log.info("REST request to get latest fee record for student database ID: {}", studentId);
        return feeRecordService.getLatestFeeRecordForStudent(studentId)
                .map(StudentFeeRecordResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Get ALL fee records for a student by studentId (complete history)
     * 
     * GET /api/school/fee-records/student-ref/{studentId}
     * 
     * Returns ALL fee records for the student (all terms/years) FOR CURRENT SCHOOL
     * Example: GET /api/school/fee-records/student-ref/2025-5001
     * 
     * MULTI-TENANT SAFE: Only returns records from current school context
     * NOTE: Changed from single record to list (OneToMany relationship)
     */
    @GetMapping("/student-id/{studentId}")
    public ResponseEntity<List<StudentFeeRecordResponse>> getFeeRecordsByStudentReference(@PathVariable String studentId) {
        log.info("REST request to get all fee records for student reference: {}", studentId);
        
        List<StudentFeeRecord> records = feeRecordService.getFeeRecordsByStudentReferenceForCurrentSchool(studentId);
        
        if (records.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<StudentFeeRecordResponse> response = records.stream()
            .map(StudentFeeRecordResponse::fromEntity)
            .toList();
            
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Get fee records by term/year
     * 
     * GET /api/fee-records/term/{termYear}
     * 
     * Example: GET /api/fee-records/term/2025-Term1
     */
    @GetMapping("/year/{year}/term/{term}")
    public ResponseEntity<List<StudentFeeRecordResponse>> getFeeRecordsByTermYear(@PathVariable Integer year, @PathVariable Integer term) {
        log.info("REST request to get fee records for year: {}, term: {}", year, term);
        List<StudentFeeRecord> records = feeRecordService.getFeeRecordsByYearAndTerm(year, term);
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get all fee records of current school
     * @param status
     * @return
     */
    @Operation(summary = "Get all fee records for the current school", description = "Returns all student fee records associated with the current school context.")
    @ApiResponse(
        responseCode = "200", 
        description = "Successful retrieval of fee records",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudentFeeRecordResponse.class)))
    )
    @GetMapping
    public ResponseEntity<List<StudentFeeRecordResponse>> getAllFeeRecordsForCurrentSchool() {
        List<StudentFeeRecord> records = feeRecordService.getAllFeeRecordsForCurrentSchool();
        List<StudentFeeRecordResponse> dtos = records.stream()
            .map(StudentFeeRecordResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * READ - Get fee records by payment status
     * 
     * GET /api/fee-records/payment-status/{status}
     * 
     * Valid statuses: PAID, PARTIALLY_PAID, ARREARS, OVERDUE
     */
    @GetMapping("/payment-status/{status}")
    public ResponseEntity<List<StudentFeeRecordResponse>> getFeeRecordsByPaymentStatus(@PathVariable String status) {
        log.info("REST request to get fee records with status: {}", status);
        List<StudentFeeRecord> records = feeRecordService.getFeeRecordsByPaymentStatus(status);
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * READ - Get fee records by category
     * 
     * GET /api/fee-records/category/{category}
     * 
     * Example: GET /api/fee-records/category/Boarder
     * 
     * MULTI-TENANT SAFE: Only returns records from current school context
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<StudentFeeRecordResponse>> getFeeRecordsByCategory(@PathVariable String category) {
        log.info("REST request to get fee records for category: {}", category);
        List<StudentFeeRecord> records = feeRecordService.getFeeRecordsByFeeCategoryForCurrentSchool(category);
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * READ - Get records with scholarships
     * 
     * GET /api/fee-records/scholarships
     */
    @GetMapping("/scholarships")
    public ResponseEntity<List<StudentFeeRecordResponse>> getRecordsWithScholarships() {
        log.info("REST request to get fee records with scholarships");
        List<StudentFeeRecord> records = feeRecordService.getStudentsWithScholarships();
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * READ - Get records with outstanding balance
     * 
     * GET /api/fee-records/outstanding
     * 
     * Returns all records where student still owes money
     */
    @GetMapping("/outstanding")
    public ResponseEntity<List<StudentFeeRecordResponse>> getRecordsWithOutstandingBalance() {
        log.info("REST request to get fee records with outstanding balance");
        List<StudentFeeRecord> records = feeRecordService.getRecordsWithOutstandingBalance();
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * READ - Get fully paid records
     * 
     * GET /api/fee-records/fully-paid
     */
    @GetMapping("/fully-paid")
    public ResponseEntity<List<StudentFeeRecordResponse>> getFullyPaidRecords() {
        log.info("REST request to get fully paid fee records");
        List<StudentFeeRecord> records = feeRecordService.getFullyPaidRecords();
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * UPDATE - Update fee record
     * 
     * PUT /api/fee-records/{id}
     * 
     * Supports partial updates
     * Totals recalculated automatically
     */
    @PutMapping("/{id}")
    public ResponseEntity<StudentFeeRecordResponse> updateFeeRecord(
            @PathVariable Long id,
            @RequestBody StudentFeeRecord feeRecord) {
        log.info("REST request to update fee record with ID: {}", id);

        try {
            StudentFeeRecord updated = feeRecordService.updateFeeRecord(id, feeRecord);
            return ResponseEntity.ok(StudentFeeRecordResponse.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            log.error("Error updating fee record: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * ADD PAYMENT - Add payment to fee record
     * 
     * POST /api/fee-records/{id}/payment
     * 
     * Body: { "amount": 500.00 }
     * 
     * LEARNING: Manual payment addition
     * Updates amountPaid and recalculates outstandingBalance
     */
    @PostMapping("/{id}/payment")
    public ResponseEntity<StudentFeeRecordResponse> addPayment(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> payload) {
        log.info("REST request to add payment to fee record ID: {}", id);

        BigDecimal amount = payload.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }

        try {
            StudentFeeRecord updated = feeRecordService.addPayment(id, amount);
            StudentFeeRecordResponse dto =  StudentFeeRecordResponse.fromEntity(updated);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            log.error("Error adding payment: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * APPLY SCHOLARSHIP - Add scholarship discount
     * 
     * POST /api/fee-records/{id}/scholarship
     * 
     * Body: { "amount": 100.00 }
     */
        @PostMapping("/{id}/scholarship")
        public ResponseEntity<StudentFeeRecordResponse> applyScholarship(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> payload) {
        log.info("REST request to apply scholarship to fee record ID: {}", id);
        
        BigDecimal amount = payload.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            StudentFeeRecord updated = feeRecordService.applyScholarship(id, amount);
            return ResponseEntity.ok(StudentFeeRecordResponse.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            log.error("Error applying scholarship: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * APPLY SIBLING DISCOUNT
     * 
     * POST /api/fee-records/{id}/sibling-discount
     * 
     * Body: { "amount": 50.00 }
     */
        @PostMapping("/{id}/sibling-discount")
        public ResponseEntity<StudentFeeRecordResponse> applySiblingDiscount(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> payload) {
        log.info("REST request to apply sibling discount to fee record ID: {}", id);
        
        BigDecimal amount = payload.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            StudentFeeRecord updated = feeRecordService.applySiblingDiscount(id, amount);
            return ResponseEntity.ok(StudentFeeRecordResponse.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            log.error("Error applying sibling discount: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /* ----------------------  FINANCIAL REPORTS  ------------------------- */

    /**
     * REPORT - Total outstanding fees
     * 
     * GET /api/fee-records/reports/total-outstanding
     * 
     * Returns total money owed across all students
     */
    @GetMapping("/reports/total-outstanding")
    public ResponseEntity<BigDecimal> getTotalOutstandingFees() {
        log.info("REST request to get total outstanding fees");
        BigDecimal total = feeRecordService.getTotalOutstandingFees();
        return ResponseEntity.ok(total);
    }

    /**
     * REPORT - Total collected fees
     * 
     * GET /api/fee-records/reports/total-collected
     * 
     * Returns total money received from all students
     */
    @GetMapping("/reports/total-collected")
    public ResponseEntity<BigDecimal> getTotalCollectedFees() {
        log.info("REST request to get total collected fees");
        BigDecimal total = feeRecordService.getTotalCollectedFees();
        return ResponseEntity.ok(total);
    }

    /**
     * REPORT - Total gross amount (before discounts)
     * 
     * GET /api/fee-records/reports/total-gross
     */
    @GetMapping("/reports/total-gross")
    public ResponseEntity<BigDecimal> getTotalGrossAmount() {
        log.info("REST request to get total gross amount");
        BigDecimal total = feeRecordService.getTotalGrossAmount();
        return ResponseEntity.ok(total);
    }

    /**
     * REPORT - Total scholarships awarded
     * 
     * GET /api/fee-records/reports/total-scholarships
     */
    @GetMapping("/reports/total-scholarships")
    public ResponseEntity<BigDecimal> getTotalScholarships() {
        log.info("REST request to get total scholarships");
        BigDecimal total = feeRecordService.getTotalScholarships();
        return ResponseEntity.ok(total);
    }

    /**
     * REPORT - Collection rate percentage
     * 
     * GET /api/fee-records/reports/collection-rate
     * 
     * Returns (Total Collected / Total Gross) * 100
     */
    @GetMapping("/reports/collection-rate")
    public ResponseEntity<BigDecimal> getCollectionRate() {
        log.info("REST request to get collection rate");
        BigDecimal rate = feeRecordService.getCollectionRate();
        return ResponseEntity.ok(rate);
    }

    /**
     * REPORT - Count by payment status
     * 
     * GET /api/fee-records/reports/count?status=ARREARS
     */
    @GetMapping("/reports/count")
    public ResponseEntity<Long> countByPaymentStatus(@RequestParam String status) {
        log.info("REST request to count fee records with status: {}", status);
        long count = feeRecordService.countByPaymentStatus(status);
        return ResponseEntity.ok(count);
    }

    /**
     * DEACTIVATE - Deactivate fee record
     * 
     * POST /api/fee-records/{id}/deactivate
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<StudentFeeRecordResponse> deactivateFeeRecord(@PathVariable Long id) {
        log.info("REST request to deactivate fee record with ID: {}", id);

        try {
            StudentFeeRecord deactivated = feeRecordService.deactivateFeeRecord(id);
            return ResponseEntity.ok(StudentFeeRecordResponse.fromEntity(deactivated));
        } catch (IllegalArgumentException e) {
            log.error("Error deactivating fee record: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * UPDATE - Update a student's fee record by studentId, year, term, and currency
     * 
     * PUT /api/fee-records/student/{studentId}/year/{year}/term/{term}/currency/{currency}
     * 
     * Body example:
     * {
     *   "feeCategory": "Boarder",
     *   "tuitionFee": 550.00,
     *   "boardingFee": 320.00,
     *   "developmentLevy": 55.00,
     *   "examFee": 30.00,
     *   "otherFees": 25.00
     * }
     * 
     * LEARNING: Multi-currency support
     * - Students can have multiple fee records for same term with different currencies
     * - Currency is part of the identifier (studentId + year + term + currency)
     * - Use this endpoint to adjust fees after initial assignment
     * 
     * Example: Update ZWG fees for student S001 in 2025 Term 1
     * PUT /api/fee-records/student/S001/year/2025/term/1/currency/ZWG
     */
    @PutMapping("/student/{studentId}/year/{year}/term/{term}/currency/{currency}")
    public ResponseEntity<StudentFeeRecordResponse> updateFeeRecordByStudent(
            @PathVariable String studentId,
            @PathVariable Integer year,
            @PathVariable Integer term,
            @PathVariable String currency,
            @RequestBody FeeRecordUpdateRequest request) {
        
        log.info("REST request to update fee record for student {} (year={}, term={}, currency={})", 
                 studentId, year, term, currency);
        
        StudentFeeRecord updated = feeRecordService.updateFeeRecordForStudent(
            studentId,
            year,
            term,
            currency,
            request.feeCategory,
            request.tuitionFee,
            request.boardingFee,
            request.developmentLevy,
            request.examFee,
            request.otherFees
        );
        
        StudentFeeRecordResponse response = StudentFeeRecordResponse.fromEntity(updated);
        return ResponseEntity.ok(response);
    }

    /* ----------------------  BULK FEE ASSIGNMENT  ------------------------- */

    /**
     * BULK - Assign fees to all students in a grade
     * 
     * POST /api/fee-records/bulk/grade/{grade}
     * 
     * Body example:
     * {
     *   "termYear": "2025-Term1",
     *   "feeCategory": "Boarder",
     *   "currency": "ZWG",
     *   "tuitionFee": 500.00,
     *   "boardingFee": 300.00,
     *   "developmentLevy": 50.00,
     *   "examFee": 25.00,
     *   "otherFees": 20.00
     * }
     * 
     * LEARNING: Bulk operations
     * - Sets fees for ALL students in specified grade at once
     * - Much faster than creating individual fee records
     * - Useful for bursar setting term fees by grade level
     * 
     * Example: Set fees for all "Grade 5" students
     * POST /api/fee-records/bulk/grade/Grade 5
     */
    @PostMapping("/bulk/grade/{grade}")
    public ResponseEntity<BulkFeeAssignmentResponse> assignFeesToGrade(
            @PathVariable String grade,
            @RequestBody BulkFeeAssignmentRequest request) {
        
        log.info("REST request to assign fees to grade: {}", grade);
        
        // Use school-aware service method so school is assigned from context
        // Check for existing records with same currency (allows multiple currencies per term)
        List<StudentFeeRecord> existing = feeRecordService.getFeeRecordsByYearAndTermForCurrentSchool(request.year, request.term)
            .stream().filter(r -> grade.equals(r.getStudent().getGrade()) && 
                                  request.currency.equals(r.getCurrency())).toList();
        if (!existing.isEmpty()) {
            return ResponseEntity.badRequest().body(new BulkFeeAssignmentResponse(
                "Fees already assigned!", grade, existing.size(), existing.stream().map(StudentFeeRecordResponse::fromEntity).toList()
            ));
        }
        List<StudentFeeRecord> records = feeRecordService.assignFeesToGradeForCurrentSchool(
            grade,
            request.year,
            request.term,
            request.feeCategory,
            request.currency,
            request.tuitionFee,
            request.boardingFee,
            request.developmentLevy,
            request.examFee,
            request.otherFees
        );
        
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        BulkFeeAssignmentResponse response = new BulkFeeAssignmentResponse(
            "Fees assigned successfully",
            grade,
            records.size(),
            dtos
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * BULK - Assign fees to multiple grades
     * 
     * POST /api/fee-records/bulk/grades
     * 
     * Body example:
     * {
     *   "grades": ["Grade 1", "Grade 2", "Grade 3"],
     *   "termYear": "2025-Term1",
     *   "feeCategory": "Day Scholar",
     *   "currency": "USD",
     *   "tuitionFee": 400.00,
     *   "boardingFee": 0.00,
     *   "developmentLevy": 50.00,
     *   "examFee": 25.00,
     *   "otherFees": 20.00
     * }
     * 
     * Example use case: Set fees for all primary school grades (1-7)
     */
    @PostMapping("/bulk/grades")
    public ResponseEntity<BulkFeeAssignmentResponse> assignFeesToMultipleGrades(
            @RequestBody BulkFeeAssignmentRequestMultiGrade request) {
        
        log.info("REST request to assign fees to {} grades", request.grades.size());
        
        // Call the school-aware grade method for each requested grade
        List<StudentFeeRecord> records = new java.util.ArrayList<>();
        for (String g : request.grades) {
            // Check for existing records with same currency (allows multiple currencies per term)
            List<StudentFeeRecord> existing = feeRecordService.getFeeRecordsByYearAndTermForCurrentSchool(request.year, request.term)
                .stream().filter(r -> g.equals(r.getStudent().getGrade()) && 
                                      request.currency.equals(r.getCurrency())).toList();
            if (!existing.isEmpty()) {
                return ResponseEntity.badRequest().body(new BulkFeeAssignmentResponse(
                    "Fees already assigned!", g, existing.size(), existing.stream().map(StudentFeeRecordResponse::fromEntity).toList()
                ));
            }
            records.addAll(feeRecordService.assignFeesToGradeForCurrentSchool(
                g,
                request.year,
                request.term,
                request.feeCategory,
                request.currency,
                request.tuitionFee,
                request.boardingFee,
                request.developmentLevy,
                request.examFee,
                request.otherFees
            ));
        }
        
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        BulkFeeAssignmentResponse response = new BulkFeeAssignmentResponse(
            "Fees assigned successfully to multiple grades",
            String.join(", ", request.grades),
            records.size(),
            dtos
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * BULK - Assign fees to ALL students in school
     * 
     * POST /api/fee-records/bulk/all-students
     * 
     * Body example:
     * {
     *   "termYear": "2025-Term1",
     *   "feeCategory": "Boarder",
     *   "currency": "ZWG",
     *   "tuitionFee": 500.00,
     *   "boardingFee": 300.00,
     *   "developmentLevy": 50.00,
     *   "examFee": 25.00,
     *   "otherFees": 20.00
     * }
     * 
     * WARNING: Use with caution - affects ALL active students
     * Useful when entire school has uniform fee structure
     */
    @PostMapping("/bulk/all-students")
    public ResponseEntity<BulkFeeAssignmentResponse> assignFeesToAllStudents(
            @RequestBody BulkFeeAssignmentRequest request) {
        
        log.warn("REST request to assign fees to ALL students in current school");
        
        // use the school-aware method (already school-aware in service)
        // Check for existing records with same currency (allows multiple currencies per term)
        List<StudentFeeRecord> existing = feeRecordService.getFeeRecordsByYearAndTermForCurrentSchool(request.year, request.term)
            .stream().filter(r -> request.currency.equals(r.getCurrency())).toList();
        if (!existing.isEmpty()) {
            return ResponseEntity.badRequest().body(new BulkFeeAssignmentResponse(
                "Fees already assigned!", "All Grades", existing.size(), existing.stream().map(StudentFeeRecordResponse::fromEntity).toList()
            ));
        }
        List<StudentFeeRecord> records = feeRecordService.assignFeesToAllStudentsForCurrentSchool(
            request.year,
            request.term,
            request.feeCategory,
            request.currency,
            request.tuitionFee,
            request.boardingFee,
            request.developmentLevy,
            request.examFee,
            request.otherFees
        );
        
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        BulkFeeAssignmentResponse response = new BulkFeeAssignmentResponse(
            "Fees assigned successfully to all students",
            "All Grades",
            records.size(),
            dtos
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * BULK - Assign fees to a specific class/form
     * 
     * POST /api/fee-records/bulk/class/{className}
     * 
     * Body example:
     * {
     *   "termYear": "2025-Term1",
     *   "feeCategory": "Boarder",
     *   "currency": "USD",
     *   "tuitionFee": 500.00,
     *   "boardingFee": 300.00,
     *   "developmentLevy": 50.00,
     *   "examFee": 25.00,
     *   "otherFees": 20.00
     * }
     * 
     * LEARNING: Zimbabwe education system support
     * - Primary: Classes like "5A", "5B", "6C"
     * - Secondary: Classes like "Form 3A", "Form 4B", "Form 5C"
     * 
     * Examples:
     * POST /api/fee-records/bulk/class/5A         (Primary school class)
     * POST /api/fee-records/bulk/class/Form 3B    (Secondary school form)
     */
    @PostMapping("/bulk/class/{className}")
    public ResponseEntity<BulkFeeAssignmentResponse> assignFeesToClass(
            @PathVariable String className,
            @RequestBody BulkFeeAssignmentRequest request) {
        
        log.info("REST request to assign fees to class: {}", className);
        
        // Use school-aware method so school context is applied
        // Check for existing records with same currency (allows multiple currencies per term)
        List<StudentFeeRecord> existing = feeRecordService.getFeeRecordsByYearAndTermForCurrentSchool(request.year, request.term)
            .stream().filter(r -> className.equals(r.getStudent().getClassName()) && 
                                  request.currency.equals(r.getCurrency())).toList();
        if (!existing.isEmpty()) {
            return ResponseEntity.badRequest().body(new BulkFeeAssignmentResponse(
                "Fees already assigned!", className, existing.size(), existing.stream().map(StudentFeeRecordResponse::fromEntity).toList()
            ));
        }
        List<StudentFeeRecord> records = feeRecordService.assignFeesToClassForCurrentSchool(
            className,
            request.year,
            request.term,
            request.feeCategory,
            request.currency,
            request.tuitionFee,
            request.boardingFee,
            request.developmentLevy,
            request.examFee,
            request.otherFees
        );
        
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        BulkFeeAssignmentResponse response = new BulkFeeAssignmentResponse(
            "Fees assigned successfully",
            "Class: " + className,
            records.size(),
            dtos
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * BULK - Assign fees to specific grade and class
     * 
     * POST /api/fee-records/bulk/grade/{grade}/class/{className}
     * 
     * Examples:
     * POST /api/fee-records/bulk/grade/Grade 5/class/5A
     * POST /api/fee-records/bulk/grade/Form 3/class/Form 3B
     */
    @PostMapping("/bulk/grade/{grade}/class/{className}")
    public ResponseEntity<BulkFeeAssignmentResponse> assignFeesToGradeAndClass(
            @PathVariable String grade,
            @PathVariable String className,
            @RequestBody BulkFeeAssignmentRequest request) {
        
        log.info("REST request to assign fees to {} - {}", grade, className);
        
        // Use the school-aware variant which assigns school from context
        // Check for existing records with same currency (allows multiple currencies per term)
        List<StudentFeeRecord> existing = feeRecordService.getFeeRecordsByYearAndTermForCurrentSchool(request.year, request.term)
            .stream().filter(r -> grade.equals(r.getStudent().getGrade()) && 
                                  className.equals(r.getStudent().getClassName()) && 
                                  request.currency.equals(r.getCurrency())).toList();
        if (!existing.isEmpty()) {
            return ResponseEntity.badRequest().body(new BulkFeeAssignmentResponse(
                "Fees already assigned!", grade + " - " + className, existing.size(), existing.stream().map(StudentFeeRecordResponse::fromEntity).toList()
            ));
        }
        List<StudentFeeRecord> records = feeRecordService.assignFeesToGradeAndClassForCurrentSchool(
            grade,
            className,
            request.year,
            request.term,
            request.feeCategory,
            request.currency,
            request.tuitionFee,
            request.boardingFee,
            request.developmentLevy,
            request.examFee,
            request.otherFees
        );
        
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        BulkFeeAssignmentResponse response = new BulkFeeAssignmentResponse(
            "Fees assigned successfully",
            grade + " - " + className,
            records.size(),
            dtos
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * BULK - Assign fees to all secondary school forms
     * 
     * POST /api/fee-records/bulk/forms
     * 
     * Body example:
     * {
     *   "grades": ["Form 1", "Form 2", "Form 3", "Form 4", "Form 5", "Form 6"],
     *   "termYear": "2025-Term1",
     *   "feeCategory": "Boarder",
     *   "currency": "ZWG",
     *   "tuitionFee": 600.00,
     *   "boardingFee": 350.00,
     *   "developmentLevy": 75.00,
     *   "examFee": 50.00,
     *   "otherFees": 25.00
     * }
     * 
     * LEARNING: Secondary school bulk assignment
     * - Sets fees for all Form 1-6 students
     * - Typically higher fees than primary school
     */
    @PostMapping("/bulk/forms")
    public ResponseEntity<BulkFeeAssignmentResponse> assignFeesToForms(
            @RequestBody BulkFeeAssignmentRequestMultiGrade request) {
        
        log.info("REST request to assign fees to {} forms", request.grades.size());
        
        // Use school-aware grade assignment for each form
        List<StudentFeeRecord> records = new java.util.ArrayList<>();
        for (String form : request.grades) {
            // Check for existing records with same currency (allows multiple currencies per term)
            List<StudentFeeRecord> existing = feeRecordService.getFeeRecordsByYearAndTermForCurrentSchool(request.year, request.term)
                .stream().filter(r -> form.equals(r.getStudent().getGrade()) && 
                                      request.currency.equals(r.getCurrency())).toList();
            if (!existing.isEmpty()) {
                return ResponseEntity.badRequest().body(new BulkFeeAssignmentResponse(
                    "Fees already assigned!", form, existing.size(), existing.stream().map(StudentFeeRecordResponse::fromEntity).toList()
                ));
            }
            records.addAll(feeRecordService.assignFeesToGradeForCurrentSchool(
                form,
                request.year,
                request.term,
                request.feeCategory,
                request.currency,
                request.tuitionFee,
                request.boardingFee,
                request.developmentLevy,
                request.examFee,
                request.otherFees
            ));
        }
        
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        BulkFeeAssignmentResponse response = new BulkFeeAssignmentResponse(
            "Fees assigned successfully to secondary school forms",
            String.join(", ", request.grades),
            records.size(),
            dtos
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

       /**
     * BULK UPDATE - Update fee records for a single grade
     *
     * PUT /api/fee-records/bulk/grade/{grade}
     * Body: { "termYear": "2025-Term1", ...fields to update... }
     */
    @PutMapping("/bulk/grade/{grade}")
    public ResponseEntity<BulkFeeAssignmentResponse> bulkUpdateGrade(
            @PathVariable String grade,
            @RequestBody BulkFeeAssignmentRequest update) {
        log.info("Bulk update fee records for grade: {} term: {}", grade, update.year, update.term);
        List<StudentFeeRecord> records = feeRecordService.getFeeRecordsByYearAndTermForCurrentSchool(update.year, update.term)
            .stream().filter(r -> grade.equals(r.getStudent().getGrade())).toList();
        if (records.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        int updated = 0;
        for (StudentFeeRecord r : records) {
            if (update.feeCategory != null) r.setFeeCategory(update.feeCategory);
            if (update.tuitionFee != null) r.setTuitionFee(update.tuitionFee);
            if (update.boardingFee != null) r.setBoardingFee(update.boardingFee);
            if (update.developmentLevy != null) r.setDevelopmentLevy(update.developmentLevy);
            if (update.examFee != null) r.setExamFee(update.examFee);
            if (update.otherFees != null) r.setOtherFees(update.otherFees);
            feeRecordService.updateFeeRecord(r.getId(), r);
            updated++;
        }
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        return ResponseEntity.ok(new BulkFeeAssignmentResponse(
            "Bulk update successful", grade, updated, dtos
        ));
    }

    /**
     * BULK UPDATE - Update fee records for multiple grades
     *
     * PUT /api/fee-records/bulk/grades
     * Body: { "grades": ["Form 1", "Form 2"], "termYear": "2025-Term1", ...fields to update... }
     */
    @PutMapping("/bulk/grades")
    public ResponseEntity<BulkFeeAssignmentResponse> bulkUpdateGrades(
            @RequestBody BulkFeeAssignmentRequestMultiGrade update) {
        log.info("Bulk update fee records for grades: {} year{} term: {}", update.grades, update.year, update.term);
        int updated = 0;
        List<StudentFeeRecord> allRecords = new java.util.ArrayList<>();
        for (String grade : update.grades) {
            List<StudentFeeRecord> records = feeRecordService.getFeeRecordsByYearAndTermForCurrentSchool(update.year, update.term)
                .stream().filter(r -> grade.equals(r.getStudent().getGrade())).toList();
            for (StudentFeeRecord r : records) {
                if (update.feeCategory != null) r.setFeeCategory(update.feeCategory);
                if (update.tuitionFee != null) r.setTuitionFee(update.tuitionFee);
                if (update.boardingFee != null) r.setBoardingFee(update.boardingFee);
                if (update.developmentLevy != null) r.setDevelopmentLevy(update.developmentLevy);
                if (update.examFee != null) r.setExamFee(update.examFee);
                if (update.otherFees != null) r.setOtherFees(update.otherFees);
                feeRecordService.updateFeeRecord(r.getId(), r);
                updated++;
            }
            allRecords.addAll(records);
        }
        List<StudentFeeRecordResponse> dtos = allRecords.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        return ResponseEntity.ok(new BulkFeeAssignmentResponse(
            "Bulk update successful", String.join(", ", update.grades), updated, dtos
        ));
    }

    /**
     * BULK UPDATE - Update fee records for a class/stream
     *
     * PUT /api/fee-records/bulk/class/{className}
     * Body: { "termYear": "2025-Term1", ...fields to update... }
     */
    @PutMapping("/bulk/class/{className}")
    public ResponseEntity<BulkFeeAssignmentResponse> bulkUpdateClass(
            @PathVariable String className,
            @RequestBody BulkFeeAssignmentRequest update) {
        log.info("Bulk update fee records for class: {} year {} term: {}", className, update.year, update.term);
        List<StudentFeeRecord> records = feeRecordService.getFeeRecordsByYearAndTermForCurrentSchool(update.year, update.term)
            .stream().filter(r -> className.equals(r.getStudent().getClassName())).toList();
        if (records.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        int updated = 0;
        for (StudentFeeRecord r : records) {
            if (update.feeCategory != null) r.setFeeCategory(update.feeCategory);
            if (update.tuitionFee != null) r.setTuitionFee(update.tuitionFee);
            if (update.boardingFee != null) r.setBoardingFee(update.boardingFee);
            if (update.developmentLevy != null) r.setDevelopmentLevy(update.developmentLevy);
            if (update.examFee != null) r.setExamFee(update.examFee);
            if (update.otherFees != null) r.setOtherFees(update.otherFees);
            feeRecordService.updateFeeRecord(r.getId(), r);
            updated++;
        }
        List<StudentFeeRecordResponse> dtos = records.stream().map(StudentFeeRecordResponse::fromEntity).toList();
        return ResponseEntity.ok(new BulkFeeAssignmentResponse(
            "Bulk update successful", className, updated, dtos
        ));
    }

    /**
     * Health check
     * 
     * GET /api/fee-records/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Fee Records API is running! 💰");
    }

    /* ----------------------  REQUEST/RESPONSE DTOs  ------------------------- */

    /**
     * Request DTO for bulk fee assignment (single grade or all students)
     */
    public static class BulkFeeAssignmentRequest {
        public Integer year;
        public Integer term;
        public String feeCategory;
        public String currency;
        public BigDecimal tuitionFee;
        public BigDecimal boardingFee;
        public BigDecimal developmentLevy;
        public BigDecimal examFee;
        public BigDecimal otherFees;
    }

    /**
     * Request DTO for updating a fee record
     */
    public static class FeeRecordUpdateRequest {
        public String feeCategory;
        public BigDecimal tuitionFee;
        public BigDecimal boardingFee;
        public BigDecimal developmentLevy;
        public BigDecimal examFee;
        public BigDecimal otherFees;
    }

    /**
     * Request DTO for bulk fee assignment to multiple grades
     */
    public static class BulkFeeAssignmentRequestMultiGrade {
        public List<String> grades;
        public Integer year;
        public Integer term;
        public String feeCategory;
        public String currency;
        public BigDecimal tuitionFee;
        public BigDecimal boardingFee;
        public BigDecimal developmentLevy;
        public BigDecimal examFee;
        public BigDecimal otherFees;
    }

    /**
     * Response DTO for bulk operations
     */
    public static class BulkFeeAssignmentResponse {
        public String message;
        public String affectedGrades;
        public int studentsAffected;
        public List<StudentFeeRecordResponse> feeRecords;

        public BulkFeeAssignmentResponse(String message, String affectedGrades, 
                                         int studentsAffected, List<StudentFeeRecordResponse> feeRecords) {
            this.message = message;
            this.affectedGrades = affectedGrades;
            this.studentsAffected = studentsAffected;
            this.feeRecords = feeRecords;
        }
    }
}
