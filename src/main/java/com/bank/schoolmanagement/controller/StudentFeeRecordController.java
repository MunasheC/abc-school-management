package com.bank.schoolmanagement.controller;

import com.bank.schoolmanagement.entity.StudentFeeRecord;
import com.bank.schoolmanagement.service.StudentFeeRecordService;
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
    public ResponseEntity<StudentFeeRecord> createFeeRecord(@Valid @RequestBody StudentFeeRecord feeRecord) {
        log.info("REST request to create fee record for student ID: {}", 
                 feeRecord.getStudent() != null ? feeRecord.getStudent().getId() : "null");
        
        StudentFeeRecord created = feeRecordService.createFeeRecord(feeRecord);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * READ - Get fee record by ID
     * 
     * GET /api/fee-records/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<StudentFeeRecord> getFeeRecordById(@PathVariable Long id) {
        log.info("REST request to get fee record with ID: {}", id);
        return feeRecordService.getFeeRecordById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Get fee record for a student by database ID
     * 
     * GET /api/fee-records/student/{studentId}
     * 
     * Returns the current fee record for the student (by database ID)
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<StudentFeeRecord> getFeeRecordByStudentId(@PathVariable Long studentId) {
        log.info("REST request to get fee record for student database ID: {}", studentId);
        return feeRecordService.getFeeRecordByStudentId(studentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Get fee record for a student by studentId (e.g., STU1733838975353)
     * 
     * GET /api/fee-records/student-ref/{studentId}
     * 
     * Returns the current fee record for the student using their studentId field
     * Example: GET /api/fee-records/student-ref/STU1733838975353
     */
    @GetMapping("/student-ref/{studentId}")
    public ResponseEntity<StudentFeeRecord> getFeeRecordByStudentReference(@PathVariable String studentId) {
        log.info("REST request to get fee record for student reference: {}", studentId);
        return feeRecordService.getFeeRecordByStudentReference(studentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Get fee records by term/year
     * 
     * GET /api/fee-records/term/{termYear}
     * 
     * Example: GET /api/fee-records/term/2025-Term1
     */
    @GetMapping("/term/{termYear}")
    public ResponseEntity<List<StudentFeeRecord>> getFeeRecordsByTermYear(@PathVariable String termYear) {
        log.info("REST request to get fee records for term: {}", termYear);
        List<StudentFeeRecord> records = feeRecordService.getFeeRecordsByTermYear(termYear);
        return ResponseEntity.ok(records);
    }

    /**
     * READ - Get fee records by payment status
     * 
     * GET /api/fee-records/payment-status/{status}
     * 
     * Valid statuses: PAID, PARTIALLY_PAID, ARREARS, OVERDUE
     */
    @GetMapping("/payment-status/{status}")
    public ResponseEntity<List<StudentFeeRecord>> getFeeRecordsByPaymentStatus(@PathVariable String status) {
        log.info("REST request to get fee records with status: {}", status);
        List<StudentFeeRecord> records = feeRecordService.getFeeRecordsByPaymentStatus(status);
        return ResponseEntity.ok(records);
    }

    /**
     * READ - Get fee records by category
     * 
     * GET /api/fee-records/category/{category}
     * 
     * Example: GET /api/fee-records/category/Boarder
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<StudentFeeRecord>> getFeeRecordsByCategory(@PathVariable String category) {
        log.info("REST request to get fee records for category: {}", category);
        List<StudentFeeRecord> records = feeRecordService.getFeeRecordsByFeeCategory(category);
        return ResponseEntity.ok(records);
    }

    /**
     * READ - Get records with scholarships
     * 
     * GET /api/fee-records/scholarships
     */
    @GetMapping("/scholarships")
    public ResponseEntity<List<StudentFeeRecord>> getRecordsWithScholarships() {
        log.info("REST request to get fee records with scholarships");
        List<StudentFeeRecord> records = feeRecordService.getStudentsWithScholarships();
        return ResponseEntity.ok(records);
    }

    /**
     * READ - Get records with outstanding balance
     * 
     * GET /api/fee-records/outstanding
     * 
     * Returns all records where student still owes money
     */
    @GetMapping("/outstanding")
    public ResponseEntity<List<StudentFeeRecord>> getRecordsWithOutstandingBalance() {
        log.info("REST request to get fee records with outstanding balance");
        List<StudentFeeRecord> records = feeRecordService.getRecordsWithOutstandingBalance();
        return ResponseEntity.ok(records);
    }

    /**
     * READ - Get fully paid records
     * 
     * GET /api/fee-records/fully-paid
     */
    @GetMapping("/fully-paid")
    public ResponseEntity<List<StudentFeeRecord>> getFullyPaidRecords() {
        log.info("REST request to get fully paid fee records");
        List<StudentFeeRecord> records = feeRecordService.getFullyPaidRecords();
        return ResponseEntity.ok(records);
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
    public ResponseEntity<StudentFeeRecord> updateFeeRecord(
            @PathVariable Long id,
            @RequestBody StudentFeeRecord feeRecord) {
        log.info("REST request to update fee record with ID: {}", id);
        
        try {
            StudentFeeRecord updated = feeRecordService.updateFeeRecord(id, feeRecord);
            return ResponseEntity.ok(updated);
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
    public ResponseEntity<StudentFeeRecord> addPayment(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> payload) {
        log.info("REST request to add payment to fee record ID: {}", id);
        
        BigDecimal amount = payload.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            StudentFeeRecord updated = feeRecordService.addPayment(id, amount);
            return ResponseEntity.ok(updated);
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
    public ResponseEntity<StudentFeeRecord> applyScholarship(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> payload) {
        log.info("REST request to apply scholarship to fee record ID: {}", id);
        
        BigDecimal amount = payload.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            StudentFeeRecord updated = feeRecordService.applyScholarship(id, amount);
            return ResponseEntity.ok(updated);
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
    public ResponseEntity<StudentFeeRecord> applySiblingDiscount(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> payload) {
        log.info("REST request to apply sibling discount to fee record ID: {}", id);
        
        BigDecimal amount = payload.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            StudentFeeRecord updated = feeRecordService.applySiblingDiscount(id, amount);
            return ResponseEntity.ok(updated);
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
    public ResponseEntity<StudentFeeRecord> deactivateFeeRecord(@PathVariable Long id) {
        log.info("REST request to deactivate fee record with ID: {}", id);
        
        try {
            StudentFeeRecord deactivated = feeRecordService.deactivateFeeRecord(id);
            return ResponseEntity.ok(deactivated);
        } catch (IllegalArgumentException e) {
            log.error("Error deactivating fee record: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
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
        
        List<StudentFeeRecord> records = feeRecordService.assignFeesToGrade(
            grade,
            request.termYear,
            request.feeCategory,
            request.tuitionFee,
            request.boardingFee,
            request.developmentLevy,
            request.examFee,
            request.otherFees
        );
        
        BulkFeeAssignmentResponse response = new BulkFeeAssignmentResponse(
            "Fees assigned successfully",
            grade,
            records.size(),
            records
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
        
        List<StudentFeeRecord> records = feeRecordService.assignFeesToMultipleGrades(
            request.grades,
            request.termYear,
            request.feeCategory,
            request.tuitionFee,
            request.boardingFee,
            request.developmentLevy,
            request.examFee,
            request.otherFees
        );
        
        BulkFeeAssignmentResponse response = new BulkFeeAssignmentResponse(
            "Fees assigned successfully to multiple grades",
            String.join(", ", request.grades),
            records.size(),
            records
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
        
        log.warn("REST request to assign fees to ALL students school-wide");
        
        List<StudentFeeRecord> records = feeRecordService.assignFeesToAllStudents(
            request.termYear,
            request.feeCategory,
            request.tuitionFee,
            request.boardingFee,
            request.developmentLevy,
            request.examFee,
            request.otherFees
        );
        
        BulkFeeAssignmentResponse response = new BulkFeeAssignmentResponse(
            "Fees assigned successfully to all students",
            "All Grades",
            records.size(),
            records
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
        
        List<StudentFeeRecord> records = feeRecordService.assignFeesToClass(
            className,
            request.termYear,
            request.feeCategory,
            request.tuitionFee,
            request.boardingFee,
            request.developmentLevy,
            request.examFee,
            request.otherFees
        );
        
        BulkFeeAssignmentResponse response = new BulkFeeAssignmentResponse(
            "Fees assigned successfully",
            "Class: " + className,
            records.size(),
            records
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
        
        List<StudentFeeRecord> records = feeRecordService.assignFeesToGradeAndClass(
            grade,
            className,
            request.termYear,
            request.feeCategory,
            request.tuitionFee,
            request.boardingFee,
            request.developmentLevy,
            request.examFee,
            request.otherFees
        );
        
        BulkFeeAssignmentResponse response = new BulkFeeAssignmentResponse(
            "Fees assigned successfully",
            grade + " - " + className,
            records.size(),
            records
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
        
        List<StudentFeeRecord> records = feeRecordService.assignFeesToForms(
            request.grades,
            request.termYear,
            request.feeCategory,
            request.tuitionFee,
            request.boardingFee,
            request.developmentLevy,
            request.examFee,
            request.otherFees
        );
        
        BulkFeeAssignmentResponse response = new BulkFeeAssignmentResponse(
            "Fees assigned successfully to secondary school forms",
            String.join(", ", request.grades),
            records.size(),
            records
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        public String termYear;
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
        public String termYear;
        public String feeCategory;
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
        public List<StudentFeeRecord> feeRecords;

        public BulkFeeAssignmentResponse(String message, String affectedGrades, 
                                         int studentsAffected, List<StudentFeeRecord> feeRecords) {
            this.message = message;
            this.affectedGrades = affectedGrades;
            this.studentsAffected = studentsAffected;
            this.feeRecords = feeRecords;
        }
    }
}
