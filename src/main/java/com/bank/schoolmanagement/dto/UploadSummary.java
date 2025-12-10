package com.bank.schoolmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO - Upload Summary Response
 * 
 * This DTO represents the OVERALL result of uploading an Excel file.
 * After processing all rows in the Excel file, we send this back to tell:
 * - How many students were successfully imported?
 * - How many failed?
 * - What were the specific errors?
 * 
 * EXAMPLE RESPONSE:
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
 *     },
 *     ...
 *   ]
 * }
 * 
 * This allows the school bursar to:
 * 1. See how many students were imported successfully
 * 2. Identify which specific students had problems
 * 3. Fix the Excel file and re-upload only the failed rows
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadSummary {
    
    /**
     * Total number of data rows in the Excel file
     * (excludes header row)
     */
    private int totalRows;
    
    /**
     * Number of students successfully saved to database
     */
    private int successCount;
    
    /**
     * Number of students that failed validation or had errors
     */
    private int failureCount;
    
    /**
     * Detailed results for each row
     * Only failures are typically included to keep response small
     * (Can include all if needed for auditing)
     */
    private List<StudentUploadResult> results;
    
    /**
     * Constructor that initializes empty results list
     * Prevents NullPointerException when adding results
     */
    public UploadSummary(int totalRows, int successCount, int failureCount) {
        this.totalRows = totalRows;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.results = new ArrayList<>();
    }
    
    /**
     * Helper method to add a result to the list
     * Makes code cleaner: summary.addResult(...) vs summary.getResults().add(...)
     */
    public void addResult(StudentUploadResult result) {
        if (this.results == null) {
            this.results = new ArrayList<>();
        }
        this.results.add(result);
    }
}
