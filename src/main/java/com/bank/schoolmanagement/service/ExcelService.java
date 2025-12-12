package com.bank.schoolmanagement.service;

import com.bank.schoolmanagement.context.SchoolContext;
import com.bank.schoolmanagement.dto.StudentUploadResult;
import com.bank.schoolmanagement.dto.UploadSummary;
import com.bank.schoolmanagement.entity.Guardian;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.entity.StudentFeeRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Excel Service - Handles Excel file upload and parsing
 * 
 * NEW CONCEPTS IN THIS CLASS:
 * 
 * 1. APACHE POI LIBRARY
 *    - Java library for reading Microsoft Office files
 *    - Workbook = entire Excel file
 *    - Sheet = one tab in the file
 *    - Row = one row of data
 *    - Cell = one box/cell in the row
 * 
 * 2. MULTIPARTFILE
 *    - Spring's interface for uploaded files
 *    - Allows us to read file content, get filename, check size
 * 
 * 3. BATCH PROCESSING
 *    - Process many records at once
 *    - Collect all results before responding
 *    - More efficient than one-by-one API calls
 * 
 * 4. ERROR HANDLING
 *    - Continue processing even if one row fails
 *    - Collect all errors to report at the end
 *    - Don't let one bad row stop the entire import
 * 
 * EXPECTED EXCEL FORMAT (20 columns):
 * Row 1 (Headers): Student ID | First Name | Last Name | Date of Birth | Grade | Parent Name | Parent Phone | 
 *                  Parent Email | Address | Fee Category | Tuition Fee | Boarding Fee | Development Levy | 
 *                  Exam Fee | Previous Balance | Amount Paid | Has Scholarship | Scholarship Amount | 
 *                  Sibling Discount | Bursar Notes
 * Row 2+: Data rows
 * 
 * REQUIRED COLUMNS: First Name, Last Name, Parent Name, Parent Phone
 * OPTIONAL COLUMNS: All others (will use default values if empty)
 * 
 * Example:
 * | Student ID | First Name | Last Name | ... | Tuition Fee | Amount Paid | Has Scholarship |
 * |------------|------------|-----------|-----|-------------|-------------|-----------------|
 * | STU001     | John       | Doe       | ... | 500.00      | 300.00      | YES             |
 * |            | Mary       | Smith     | ... | 500.00      | 500.00      | NO              |
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelService {

    private final StudentService studentService;
    private final GuardianService guardianService;
    private final StudentFeeRecordService feeRecordService;

    /**
     * Helper class to hold parsed student data before saving
     */
    private static class ParsedStudentData {
        Student student;
        StudentFeeRecord feeRecord;
        
        ParsedStudentData(Student student, StudentFeeRecord feeRecord) {
            this.student = student;
            this.feeRecord = feeRecord;
        }
    }

    /**
     * Main method to process uploaded Excel file
     * 
     * PROCESS:
     * 1. Validate file is not empty and is Excel format
     * 2. Open Excel file using Apache POI
     * 3. Read each row and convert to Student object
     * 4. Try to save each student (each in its own transaction)
     * 5. Track successes and failures
     * 6. Return summary report
     * 
     * NOTE: @Transactional removed to allow partial success.
     * Each student is saved in its own transaction via studentService.createStudent().
     * This prevents failed students from rolling back successful ones.
     * 
     * @param file - The uploaded Excel file from the HTTP request
     * @return UploadSummary - Report of successes and failures
     * @throws IOException - If file cannot be read
     */
    public UploadSummary processExcelFile(MultipartFile file) throws IOException {
        log.info("Processing Excel file: {}", file.getOriginalFilename());
        
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // Initialize counters
        int successCount = 0;
        int failureCount = 0;
        List<StudentUploadResult> results = new ArrayList<>();
        
        // Open Excel file
        // try-with-resources: Automatically closes workbook when done (prevents memory leaks)
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            
            // Get first sheet (Sheet 0)
            Sheet sheet = workbook.getSheetAt(0);
            
            // Get total number of rows (including header)
            int totalRows = sheet.getPhysicalNumberOfRows() - 1; // Subtract header row
            log.info("Found {} data rows in Excel file", totalRows);
            
            // Iterate through rows (skip row 0 which is headers)
            // Row numbering: 0 = headers, 1 = first data row, etc.
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                
                // Skip empty rows
                if (row == null || isRowEmpty(row)) {
                    log.debug("Skipping empty row {}", i + 1);
                    continue;
                }
                
                String firstName = null;
                String lastName = null;
                
                try {
                    // Parse row into Student object (does NOT save yet)
                    ParsedStudentData parsedData = parseRowToStudentData(row);
                    firstName = parsedData.student.getFirstName();
                    lastName = parsedData.student.getLastName();
                    
                    // Save student
                    Student savedStudent = studentService.createStudent(parsedData.student);
                    log.debug("Row {}: Student saved - {} {}", i + 1, firstName, lastName);
                    
                    // Save fee record if financial data provided
                    if (parsedData.feeRecord != null) {
                        parsedData.feeRecord.setStudent(savedStudent);
                        feeRecordService.createFeeRecord(parsedData.feeRecord);
                        log.debug("Row {}: Fee record saved for {} {}", i + 1, firstName, lastName);
                    }
                    
                    // Success!
                    successCount++;
                    log.debug("Row {}: Successfully imported {} {}", i + 1, firstName, lastName);
                    
                } catch (Exception e) {
                    // Failure - record the error but continue processing
                    failureCount++;
                    
                    // Get names from row if not already extracted
                    if (firstName == null) {
                        firstName = getCellValueAsString(row.getCell(1));
                    }
                    if (lastName == null) {
                        lastName = getCellValueAsString(row.getCell(2));
                    }
                    
                    log.warn("Row {}: Failed to import {} {} - {}", 
                        i + 1, firstName, lastName, e.getMessage());
                    
                    // Add failure details to results
                    results.add(new StudentUploadResult(
                        i + 1, 
                        firstName, 
                        lastName, 
                        false, 
                        e.getMessage()
                    ));
                }
            }
            
            // Create summary
            UploadSummary summary = new UploadSummary(totalRows, successCount, failureCount, results);
            log.info("Excel import complete: {} success, {} failures out of {} total", 
                successCount, failureCount, totalRows);
            
            return summary;
        }
    }

    /**
     * Parse one Excel row into Student + Guardian + StudentFeeRecord (REFACTORED v2)
     * 
     * IMPORTANT: This method ONLY PARSES data, it does NOT save to database.
     * Saving is handled separately in processExcelFile() for proper error tracking.
     * 
     * LEARNING: Creating related entities together
     * This method creates 3 entities from one Excel row:
     * 1. Guardian (parent/guardian) - checks if exists by phone first (siblings share guardian)
     * 2. Student (personal/academic info) - NOT saved yet
     * 3. StudentFeeRecord (financial info) - only created if fee data provided, NOT saved yet
     * 
     * EXCEL COLUMN MAPPING (0-indexed):
     * BASIC INFO:
     * 0: Student ID (optional - auto-generated if empty)
     * 1: First Name (required)
     * 2: Last Name (required)
     * 3: Date of Birth (optional)
     * 4: Grade (optional)
     * 5: Parent Name (required)
     * 6: Parent Phone (required)
     * 7: Parent Email (optional)
     * 8: Address (optional)
     * 
     * BURSAR INFO (NEW - now goes to StudentFeeRecord):
     * 9: Fee Category (Day Scholar/Boarder/etc.)
     * 10: Tuition Fee
     * 11: Boarding Fee
     * 12: Development Levy
     * 13: Exam Fee
     * 14: Previous Balance
     * 15: Amount Paid
     * 16: Has Scholarship (YES/NO or TRUE/FALSE)
     * 17: Scholarship Amount
     * 18: Sibling Discount
     * 19: Bursar Notes
     * 
     * @param row - Excel row to parse
     * @return ParsedStudentData containing student and optional fee record (not yet saved)
     */
    private ParsedStudentData parseRowToStudentData(Row row) {
        /* ----------------------  CREATE GUARDIAN  ------------------------- */
        
        Guardian guardian = new Guardian();
        
        // Column 5: Parent Name (required)
        String parentName = getCellValueAsString(row.getCell(5));
        guardian.setFullName(parentName);
        
        // Column 6: Parent Phone (required)
        String parentPhone = getCellValueAsString(row.getCell(6));
        guardian.setPrimaryPhone(parentPhone);
        
        // Column 7: Parent Email (optional)
        String parentEmail = getCellValueAsString(row.getCell(7));
        guardian.setEmail(parentEmail);
        
        // Column 8: Address (optional)
        String address = getCellValueAsString(row.getCell(8));
        guardian.setAddress(address);
        
        guardian.setIsActive(true);
        
        // Assign school from current context (multi-tenant)
        School currentSchool = SchoolContext.getCurrentSchool();
        if (currentSchool != null) {
            guardian.setSchool(currentSchool);
        }
        
        // Check if guardian already exists (for siblings)
        // If parent phone exists, use existing guardian (siblings share parent)
        Guardian savedGuardian = guardianService.createOrGetGuardian(guardian);
        log.debug("Guardian created/retrieved: {}, ID: {}", savedGuardian.getFullName(), savedGuardian.getId());
        
        /* ----------------------  CREATE STUDENT  ------------------------- */
        
        Student student = new Student();
        
        // Column 0: Student ID (optional)
        String studentId = getCellValueAsString(row.getCell(0));
        if (studentId != null && !studentId.trim().isEmpty()) {
            student.setStudentId(studentId.trim());
        }
        
        // Column 1: First Name (required)
        student.setFirstName(getCellValueAsString(row.getCell(1)));
        
        // Column 2: Last Name (required)
        student.setLastName(getCellValueAsString(row.getCell(2)));
        
        // Column 3: Date of Birth (optional)
        Date dob = getCellValueAsDate(row.getCell(3));
        if (dob != null) {
            student.setDateOfBirth(dob.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate());
        }
        
        // Column 4: Grade (optional)
        student.setGrade(getCellValueAsString(row.getCell(4)));
        
        // Link to guardian
        student.setGuardian(savedGuardian);
        
        // Set enrollment date to today if not provided
        student.setEnrollmentDate(LocalDate.now());
        
        // Set as active
        student.setIsActive(true);
        
        // Assign school from current context (multi-tenant)
        if (currentSchool != null) {
            student.setSchool(currentSchool);
        }
        
        /* ----------------------  CREATE STUDENT FEE RECORD (IF NEEDED)  ------------------------- */
        
        StudentFeeRecord feeRecord = null;
        
        // Check if any financial data is provided
        BigDecimal tuitionFee = getCellValueAsBigDecimal(row.getCell(10));
        BigDecimal boardingFee = getCellValueAsBigDecimal(row.getCell(11));
        BigDecimal developmentLevy = getCellValueAsBigDecimal(row.getCell(12));
        BigDecimal examFee = getCellValueAsBigDecimal(row.getCell(13));
        
        boolean hasFinancialData = (tuitionFee != null && tuitionFee.compareTo(BigDecimal.ZERO) > 0) ||
                                   (boardingFee != null && boardingFee.compareTo(BigDecimal.ZERO) > 0) ||
                                   (developmentLevy != null && developmentLevy.compareTo(BigDecimal.ZERO) > 0) ||
                                   (examFee != null && examFee.compareTo(BigDecimal.ZERO) > 0);
        
        if (hasFinancialData) {
            feeRecord = new StudentFeeRecord();
            
            // Note: Student will be linked later after it's saved
            
            // Assign school from current context (multi-tenant)
            if (currentSchool != null) {
                feeRecord.setSchool(currentSchool);
            }
            
            // Column 9: Fee Category
            String feeCategory = getCellValueAsString(row.getCell(9));
            feeRecord.setFeeCategory(feeCategory != null ? feeCategory : "Regular");
            
            // Set current term/year (customize as needed)
            feeRecord.setTermYear("2025-Term1");
            
            // Column 10-13: Fee components
            feeRecord.setTuitionFee(tuitionFee != null ? tuitionFee : BigDecimal.ZERO);
            feeRecord.setBoardingFee(boardingFee != null ? boardingFee : BigDecimal.ZERO);
            feeRecord.setDevelopmentLevy(developmentLevy != null ? developmentLevy : BigDecimal.ZERO);
            feeRecord.setExamFee(examFee != null ? examFee : BigDecimal.ZERO);
            feeRecord.setOtherFees(BigDecimal.ZERO);
            
            // Column 14: Previous Balance
            BigDecimal previousBalance = getCellValueAsBigDecimal(row.getCell(14));
            feeRecord.setPreviousBalance(previousBalance != null ? previousBalance : BigDecimal.ZERO);
            
            // Column 15: Amount Paid
            BigDecimal amountPaid = getCellValueAsBigDecimal(row.getCell(15));
            feeRecord.setAmountPaid(amountPaid != null ? amountPaid : BigDecimal.ZERO);
            
            // Column 16-17: Scholarship
            Boolean hasScholarship = getCellValueAsBoolean(row.getCell(16));
            BigDecimal scholarshipAmount = getCellValueAsBigDecimal(row.getCell(17));
            feeRecord.setHasScholarship(hasScholarship != null && hasScholarship);
            feeRecord.setScholarshipAmount(scholarshipAmount != null ? scholarshipAmount : BigDecimal.ZERO);
            
            // Column 18: Sibling Discount
            BigDecimal siblingDiscount = getCellValueAsBigDecimal(row.getCell(18));
            feeRecord.setSiblingDiscount(siblingDiscount != null ? siblingDiscount : BigDecimal.ZERO);
            
            feeRecord.setEarlyPaymentDiscount(BigDecimal.ZERO);
            
            // Column 19: Bursar Notes
            feeRecord.setBursarNotes(getCellValueAsString(row.getCell(19)));
            
            feeRecord.setIsActive(true);
        }
        
        // Return both student and optional fee record (neither saved yet)
        return new ParsedStudentData(student, feeRecord);
    }

    /**
     * Get cell value as String regardless of cell type
     * 
     * Excel cells can contain different types:
     * - STRING: Text
     * - NUMERIC: Numbers or dates
     * - BOOLEAN: true/false
     * - FORMULA: =SUM(A1:A10)
     * - BLANK: Empty
     * 
     * This method handles all types and converts to String
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
                
            case NUMERIC:
                // Could be a number or a date
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    // Format number without decimals if it's a whole number
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    }
                    return String.valueOf(numValue);
                }
                
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
                
            case FORMULA:
                // Evaluate formula and get result
                return cell.getStringCellValue();
                
            case BLANK:
                return null;
                
            default:
                return null;
        }
    }

    /**
     * Get cell value as Date
     * Only works if cell contains a date value
     */
    private Date getCellValueAsDate(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            }
        } catch (Exception e) {
            log.warn("Could not parse date from cell: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Get cell value as BigDecimal for monetary amounts
     * 
     * LEARNING: BigDecimal is the proper way to handle money in Java
     * - Avoids floating-point precision errors
     * - Example: 0.1 + 0.2 = 0.30000000000000004 with double/float
     * - But with BigDecimal: 0.1 + 0.2 = 0.3 (exact!)
     * 
     * Returns BigDecimal.ZERO if cell is empty or invalid
     */
    private java.math.BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) {
            return java.math.BigDecimal.ZERO;
        }
        
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return java.math.BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) {
                    return java.math.BigDecimal.ZERO;
                }
                // Remove currency symbols and commas
                value = value.replaceAll("[^0-9.-]", "");
                return new java.math.BigDecimal(value);
            }
        } catch (Exception e) {
            log.warn("Could not parse BigDecimal from cell: {}", e.getMessage());
        }
        
        return java.math.BigDecimal.ZERO;
    }

    /**
     * Get cell value as Boolean
     * 
     * Accepts multiple formats:
     * - Boolean cells: true/false
     * - String cells: "YES"/"NO", "TRUE"/"FALSE", "Y"/"N", "1"/"0"
     * - Numeric cells: 1=true, 0=false
     * 
     * Defaults to false if unclear
     */
    private Boolean getCellValueAsBoolean(Cell cell) {
        if (cell == null) {
            return false;
        }
        
        try {
            switch (cell.getCellType()) {
                case BOOLEAN:
                    return cell.getBooleanCellValue();
                    
                case STRING:
                    String value = cell.getStringCellValue().trim().toUpperCase();
                    return value.equals("YES") || 
                           value.equals("TRUE") || 
                           value.equals("Y") || 
                           value.equals("1");
                    
                case NUMERIC:
                    return cell.getNumericCellValue() != 0;
                    
                default:
                    return false;
            }
        } catch (Exception e) {
            log.warn("Could not parse Boolean from cell: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if an entire row is empty
     * A row is empty if all cells are null or blank
     */
    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
