# API Reference - School Management System

Complete REST API documentation for the School Management System.

## Base URLs
- **Development:** http://localhost:8080
- **Production:** https://api.schoolmanagement.com

## Authentication & Headers

### Required Headers

#### School-Specific Endpoints
All endpoints under `/api/school/*` require:
```
X-School-ID: 1
Content-Type: application/json
```

#### Bank Endpoints
Endpoints under `/api/bank/*` do NOT require X-School-ID (cross-school access).

---

## Table of Contents
1. [Student Management](#student-management)
2. [Fee Record Management](#fee-record-management)
3. [Payment Processing](#payment-processing)
4. [Academic Year Configuration](#academic-year-configuration)
5. [School Management](#school-management)
6. [Audit Trail](#audit-trail)

---

## Student Management

### Create Student
```http
POST /api/school/students
X-School-ID: 1
```

**Request Body:**
```json
{
  "studentId": "STU2025001",
  "firstName": "Tanaka",
  "lastName": "Moyo",
  "middleName": "John",
  "dateOfBirth": "2010-05-15",
  "gender": "MALE",
  "nationalId": "63-1234567X63",
  "grade": "Form 1",
  "className": "Form 1A",
  "enrollmentDate": "2025-01-10",
  "admissionNumber": "ADM2025001"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "studentId": "STU2025001",
  "firstName": "Tanaka",
  "lastName": "Moyo",
  "fullName": "Tanaka John Moyo",
  "grade": "Form 1",
  "className": "Form 1A",
  "isActive": true,
  "createdAt": "2025-01-10T10:30:00"
}
```

### Get All Students (Paginated)
```http
GET /api/school/students?page=0&size=20
X-School-ID: 1
```

**Query Parameters:**
- `page` - Page number (default: 0)
- `size` - Items per page (default: 20)

**Response:** `200 OK`
```json
{
  "content": [...],
  "totalElements": 500,
  "totalPages": 25,
  "size": 20,
  "number": 0
}
```

### Get Student by ID
```http
GET /api/school/students/{id}
X-School-ID: 1
```

**Response:** `200 OK` or `404 Not Found`

### Get Student by Student ID
```http
GET /api/school/students/student-id/STU2025001
X-School-ID: 1
```

### Get Student by National ID
```http
GET /api/school/students/national-id/63-1234567X63
X-School-ID: 1
```

### Search Students by Name
```http
GET /api/school/students/search?name=Tanaka
X-School-ID: 1
```

### Get Students by Grade
```http
GET /api/school/students/grade/Form%201
X-School-ID: 1
```

### Update Student
```http
PUT /api/school/students/{id}
X-School-ID: 1
```

**Request Body:** (Partial update supported)
```json
{
  "grade": "Form 2",
  "className": "Form 2A"
}
```

### Update by Student ID
```http
PUT /api/school/students/by-student-id/STU2025001
X-School-ID: 1
```

### Update by National ID
```http
PUT /api/school/students/by-national-id/63-1234567X63
X-School-ID: 1
```

### Delete Student
```http
DELETE /api/school/students/{id}
X-School-ID: 1
```

**Response:** `204 No Content`

### Deactivate Student (Soft Delete)
```http
PATCH /api/school/students/{id}/deactivate
X-School-ID: 1
```

### Reactivate Student
```http
PATCH /api/school/students/{id}/reactivate
X-School-ID: 1
```

### Promote Student
```http
POST /api/school/students/{id}/promote
X-School-ID: 1
```

**Request Body:**
```json
{
  "newGrade": "Form 2",
  "newClassName": "Form 2A",
  "newTermYear": "Term 1 2026",
  "promotionNotes": "Promoted to Form 2",
  "carryForwardBalance": true,
  "tuitionFee": 600.00,
  "boardingFee": 350.00,
  "developmentLevy": 100.00,
  "examFee": 50.00,
  "otherFees": 0.00,
  "scholarshipAmount": 0.00,
  "siblingDiscount": 0.00,
  "feeCategory": "Day Scholar"
}
```

### Demote Student
```http
POST /api/school/students/{id}/demote
X-School-ID: 1
```

**Request Body:**
```json
{
  "demotedGrade": "Form 1",
  "demotedClassName": "Form 1A",
  "reason": "Failed exams - must repeat",
  "academicYear": "2026",
  "carryForwardBalance": true,
  "tuitionFee": 600.00,
  "boardingFee": 350.00,
  "developmentLevy": 100.00,
  "examFee": 50.00,
  "otherFees": 0.00
}
```

### Year-End Promotion (Atomic)
```http
POST /api/school/students/year-end-promotion
X-School-ID: 1
```

**Request Body:**
```json
{
  "newAcademicYear": "2026",
  "carryForwardBalances": true,
  "excludedStudentIds": [123, 456],
  "promotionNotes": "Year-end promotion 2025 → 2026",
  "feeStructures": {
    "Form 2": {
      "tuitionFee": 600.00,
      "examinationFee": 50.00,
      "otherFees": 0.00
    }
  },
  "defaultFeeStructure": {
    "tuitionFee": 600.00,
    "examinationFee": 50.00,
    "otherFees": 0.00
  }
}
```

**Response:** `200 OK`
```json
{
  "totalStudents": 500,
  "promotedCount": 450,
  "completedCount": 50,
  "errorCount": 0,
  "gradeBreakdown": {
    "Form 1": {
      "fromGrade": "Form 1",
      "toGrade": "Form 2",
      "studentCount": 100,
      "successCount": 100,
      "errorCount": 0
    }
  },
  "completedStudents": [
    {
      "studentId": "STU2025050",
      "fullName": "John Doe",
      "completionStatus": "COMPLETED_PRIMARY"
    }
  ],
  "errors": []
}
```

### Upload Students via Excel
```http
POST /api/school/students/upload-excel
X-School-ID: 1
Content-Type: multipart/form-data
```

**Form Data:**
- `file` - Excel file (.xlsx or .xls)

**Response:** `200 OK`
```json
{
  "totalRows": 50,
  "successCount": 48,
  "failureCount": 2,
  "results": [
    {
      "rowNumber": 3,
      "firstName": "John",
      "lastName": "Doe",
      "success": false,
      "errorMessage": "Student ID already exists"
    }
  ]
}
```

---

## Fee Record Management

### Create Fee Record
```http
POST /api/school/fee-records
X-School-ID: 1
```

**Request Body:**
```json
{
  "studentId": 1,
  "termYear": "Term 1 2026",
  "feeCategory": "Day Scholar",
  "tuitionFee": 600.00,
  "boardingFee": 0.00,
  "developmentLevy": 100.00,
  "examFee": 50.00,
  "otherFees": 0.00,
  "scholarshipAmount": 0.00,
  "siblingDiscount": 50.00,
  "previousBalance": 100.00
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "studentId": 1,
  "studentReference": "STU2025001",
  "studentName": "Tanaka Moyo",
  "grade": "Form 1",
  "termYear": "Term 1 2026",
  "grossAmount": 750.00,
  "totalDiscounts": 50.00,
  "netAmount": 700.00,
  "previousBalance": 100.00,
  "totalAmount": 800.00,
  "amountPaid": 0.00,
  "outstandingBalance": 800.00,
  "paymentStatus": "ARREARS"
}
```

### Get All Fee Records
```http
GET /api/school/fee-records?page=0&size=20
X-School-ID: 1
```

### Get Fee Record by ID
```http
GET /api/school/fee-records/{id}
X-School-ID: 1
```

### Get Fee Records for Student
```http
GET /api/school/fee-records/student-ref/STU2025001
X-School-ID: 1
```

**Response:** List of all fee records for the student (historical)

### Get Latest Fee Record for Student
```http
GET /api/school/fee-records/student-ref/STU2025001/latest
X-School-ID: 1
```

### Update Fee Record
```http
PUT /api/school/fee-records/{id}
X-School-ID: 1
```

### Delete Fee Record
```http
DELETE /api/school/fee-records/{id}
X-School-ID: 1
```

### Get Fee Records by Grade
```http
GET /api/school/fee-records/grade/Form%201
X-School-ID: 1
```

### Get Fee Records by Payment Status
```http
GET /api/school/fee-records/payment-status/ARREARS
X-School-ID: 1
```

**Payment Statuses:**
- `PAID` - Fully paid
- `PARTIALLY_PAID` - Some payment made
- `ARREARS` - No payment or behind schedule

### Get Outstanding Fees by Grade
```http
GET /api/school/fee-records/outstanding-by-grade?grade=Form%201
X-School-ID: 1
```

**Response:** `200 OK`
```json
{
  "grade": "Form 1",
  "totalOutstanding": 45000.00,
  "studentCount": 100
}
```

### Get Total Outstanding for School
```http
GET /api/school/fee-records/total-outstanding
X-School-ID: 1
```

**Response:** `200 OK`
```json
{
  "totalOutstanding": 250000.00
}
```

---

## Payment Processing

### School Payment (Cash/Check)
```http
POST /api/school/payments/student/{studentId}
X-School-ID: 1
```

**Request Body:**
```json
{
  "amount": 500.00,
  "paymentMethod": "CASH",
  "paymentDate": "2026-01-06",
  "receiptNumber": "RCT-2026-001",
  "notes": "Term 1 payment"
}
```

**Payment Methods:**
- `CASH`
- `CHECK`
- `BANK_TRANSFER`
- `MOBILE_MONEY`

**Response:** `200 OK`
```json
{
  "id": 1,
  "studentId": "STU2025001",
  "studentName": "Tanaka Moyo",
  "amount": 500.00,
  "paymentMethod": "CASH",
  "paymentDate": "2026-01-06",
  "receiptNumber": "RCT-2026-001",
  "newOutstandingBalance": 300.00,
  "paymentStatus": "PARTIALLY_PAID"
}
```

### Bank Counter Payment
```http
POST /api/bank/payment/counter
```

**Note:** No X-School-ID required (bank has cross-school access)

**Request Body:**
```json
{
  "studentId": "STU2025001",
  "schoolId": 1,
  "amount": 500.00,
  "paymentMethod": "BANK_TRANSFER",
  "paymentDate": "2026-01-06T14:30:00",
  "bankTransactionId": "BNK-TXN-123456",
  "tellerName": "Jane Doe",
  "branchName": "Harare Main Branch",
  "parentAccountNumber": "1234567890",
  "notes": "Counter payment"
}
```

### Digital Banking Payment
```http
POST /api/bank/payment/digital
```

**Request Body:**
```json
{
  "studentId": "STU2025001",
  "schoolId": 1,
  "amount": 500.00,
  "paymentMethod": "MOBILE_MONEY",
  "paymentDate": "2026-01-06T14:30:00",
  "bankTransactionId": "ECOCASH-789456",
  "parentAccountNumber": "0771234567",
  "transactionReference": "ECOCASH-789456",
  "notes": "EcoCash payment"
}
```

### Get All Payments
```http
GET /api/school/payments?page=0&size=20
X-School-ID: 1
```

### Get Payment by ID
```http
GET /api/school/payments/{id}
X-School-ID: 1
```

### Get Payments for Student
```http
GET /api/school/payments/student/{studentId}
X-School-ID: 1
```

### Get Payments by Method
```http
GET /api/school/payments/method/MOBILE_MONEY
X-School-ID: 1
```

### Get Payments by Date Range
```http
GET /api/school/payments/date-range?startDate=2026-01-01&endDate=2026-01-31
X-School-ID: 1
```

---

## Academic Year Configuration

### Create/Update Configuration
```http
POST /api/school/academic-year-config
X-School-ID: 1
```

**Request Body:**
```json
{
  "academicYear": "2025/2026",
  "endOfYearDate": "2025-12-31",
  "nextAcademicYear": "2026",
  "carryForwardBalances": true,
  "notes": "Standard year-end promotion",
  "feeStructures": {
    "Form 2": {
      "tuitionFee": 600.00,
      "boardingFee": 350.00,
      "developmentLevy": 100.00,
      "examFee": 50.00,
      "otherFees": 0.00
    }
  },
  "defaultFeeStructure": {
    "tuitionFee": 600.00,
    "boardingFee": 350.00,
    "developmentLevy": 100.00,
    "examFee": 50.00,
    "otherFees": 0.00
  }
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "academicYear": "2025/2026",
  "endOfYearDate": "2025-12-31",
  "nextAcademicYear": "2026",
  "promotionStatus": "SCHEDULED",
  "studentsPromoted": 0,
  "studentsCompleted": 0,
  "promotionErrors": 0,
  "createdAt": "2025-01-06T10:00:00"
}
```

### Get All Configurations
```http
GET /api/school/academic-year-config
X-School-ID: 1
```

### Get Configuration by ID
```http
GET /api/school/academic-year-config/{id}
X-School-ID: 1
```

### Get Latest Active Configuration
```http
GET /api/school/academic-year-config/latest
X-School-ID: 1
```

### Cancel Scheduled Promotion
```http
POST /api/school/academic-year-config/{id}/cancel?reason=School%20year%20extended
X-School-ID: 1
```

### Manually Execute Promotion
```http
POST /api/school/academic-year-config/{id}/execute
X-School-ID: 1
```

**Response:** Year-end promotion summary (same as manual trigger)

### Deactivate Configuration
```http
DELETE /api/school/academic-year-config/{id}
X-School-ID: 1
```

---

## School Management

### Create School
```http
POST /api/schools
```

**Request Body:**
```json
{
  "schoolCode": "CHHS001",
  "schoolName": "Churchill High School",
  "schoolType": "SECONDARY",
  "address": "123 Main Street",
  "city": "Harare",
  "province": "Harare Province",
  "primaryPhone": "+263771234567",
  "email": "admin@churchill.ac.zw",
  "headTeacherName": "Dr. Jane Smith",
  "bursarName": "John Doe",
  "bursarPhone": "+263779876543",
  "subscriptionTier": "PREMIUM",
  "isActive": true
}
```

### Get All Schools
```http
GET /api/schools?page=0&size=20
```

### Get School by ID
```http
GET /api/schools/{id}
```

### Update School
```http
PUT /api/schools/{id}
```

### Delete School
```http
DELETE /api/schools/{id}
```

### Get Active Schools
```http
GET /api/schools/active
```

---

## Audit Trail

### Get All Audit Entries
```http
GET /api/school/audit-trail?page=0&size=50
X-School-ID: 1
```

### Get Audit by Action Type
```http
GET /api/school/audit-trail/action/PROMOTE_STUDENT
X-School-ID: 1
```

**Action Types:**
- `CREATE_STUDENT`
- `UPDATE_STUDENT`
- `DELETE_STUDENT`
- `PROMOTE_STUDENT`
- `DEMOTE_STUDENT`
- `YEAR_END_PROMOTION`
- `CREATE_PAYMENT`
- `CREATE_FEE_RECORD`
- `UPDATE_FEE_RECORD`

### Get Audit by Entity
```http
GET /api/school/audit-trail/entity?entityType=Student&entityId=123
X-School-ID: 1
```

### Get Audit by Date Range
```http
GET /api/school/audit-trail/date-range?startDate=2026-01-01&endDate=2026-01-31
X-School-ID: 1
```

---

## Error Responses

### Standard Error Format
```json
{
  "timestamp": "2026-01-06T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Student ID already exists",
  "path": "/api/school/students"
}
```

### Common HTTP Status Codes
- `200 OK` - Success
- `201 Created` - Resource created
- `204 No Content` - Success with no response body
- `400 Bad Request` - Invalid input
- `404 Not Found` - Resource doesn't exist
- `409 Conflict` - Duplicate or constraint violation
- `500 Internal Server Error` - Server error

---

## Rate Limiting

Currently no rate limiting implemented. Consider adding in production:
- 100 requests per minute per school
- 1000 requests per hour per school

---

## Pagination

All list endpoints support pagination:
```
?page=0&size=20&sort=createdAt,desc
```

**Parameters:**
- `page` - Page number (0-indexed)
- `size` - Items per page (default: 20, max: 100)
- `sort` - Sort field and direction

---

## Interactive Documentation

Access Swagger UI for interactive testing:
```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON specification:
```
http://localhost:8080/v3/api-docs
```
