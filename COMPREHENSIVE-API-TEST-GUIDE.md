# 🧪 Comprehensive API Test Guide

## Overview
This guide provides complete testing procedures for the School Management System API, covering all major features and user journeys.

---

## 📋 Prerequisites

### 1. Start the Application
```powershell
mvn spring-boot:run
```

Wait for: `Started SchoolManagementApplication`

### 2. Base URL
```
http://localhost:8080
```

### 3. Test Tools
- **Swagger UI**: http://localhost:8080/swagger-ui.html (Interactive testing)
- **Postman**: Import collection or use manual requests
- **PowerShell/cURL**: Command-line testing

### 4. Test Schools
After running the application, you should have schools in the database. If not, create them first (see Section 1).

---

## 🎯 Test Scenarios

## 1️⃣ School Onboarding (Bank Admin)

### Test 1.1: Create School A
**Endpoint**: `POST /api/bank/admin/schools`

**Request Body**:
```json
{
  "schoolName": "Chitungwiza High School",
  "schoolCode": "CHHS001",
  "contactEmail": "admin@chitungwizahigh.ac.zw",
  "contactPhone": "+263772123456",
  "address": "123 Liberation Road, Chitungwiza"
}
```

**Expected Response**: `201 Created`
```json
{
  "id": 1,
  "schoolName": "Chitungwiza High School",
  "schoolCode": "CHHS001",
  "contactEmail": "admin@chitungwizahigh.ac.zw",
  "contactPhone": "+263772123456",
  "address": "123 Liberation Road, Chitungwiza",
  "isActive": true,
  "createdAt": "2025-12-12T10:00:00"
}
```

**Validation**:
- ✅ School created with unique code
- ✅ Returns school ID
- ✅ `isActive` is true

---

### Test 1.2: Get All Schools (Bank View)
**Endpoint**: `GET /api/bank/schools`

**Expected Response**: `200 OK`
```json
[
  {
    "id": 1,
    "schoolName": "Chitungwiza High School",
    "schoolCode": "CHHS001",
    ...
  }
]
```

**Validation**:
- ✅ Returns array of schools
- ✅ Includes newly created school

---

## 2️⃣ Student Enrollment (Bursar Perspective)

**IMPORTANT**: All bursar/school operations require `X-School-ID` header!

### Test 2.1: Single Student Enrollment
**Endpoint**: `POST /api/school/students`

**Headers**:
```
X-School-ID: 1
Content-Type: application/json
```

**Request Body**:
```json
{
  "studentId": "STU2025001",
  "firstName": "Tanaka",
  "lastName": "Moyo",
  "dateOfBirth": "2010-05-15",
  "gender": "MALE",
  "grade": "FORM_3",
  "className": "3A",
  "guardian": {
    "fullName": "Grace Moyo",
    "primaryPhone": "+263771234567",
    "email": "grace.moyo@email.com",
    "address": "45 Main Street, Chitungwiza"
  }
}
```

**Expected Response**: `201 Created`
```json
{
  "id": 1,
  "studentId": "STU2025001",
  "firstName": "Tanaka",
  "lastName": "Moyo",
  "studentReference": "CHHS001-STU2025001",
  "guardian": {
    "id": 1,
    "fullName": "Grace Moyo",
    "primaryPhone": "+263771234567"
  },
  "isActive": true
}
```

**Validation**:
- ✅ Student created with generated reference (schoolCode-studentId)
- ✅ Guardian created automatically
- ✅ Sibling detection works (if same guardian phone exists)

---

### Test 2.2: Enroll Sibling (Same Guardian)
**Endpoint**: `POST /api/school/students`

**Headers**: `X-School-ID: 1`

**Request Body**:
```json
{
  "studentId": "STU2025002",
  "firstName": "Rudo",
  "lastName": "Moyo",
  "dateOfBirth": "2012-08-20",
  "gender": "FEMALE",
  "grade": "FORM_1",
  "className": "1B",
  "guardian": {
    "fullName": "Grace Moyo",
    "primaryPhone": "+263771234567",
    "email": "grace.moyo@email.com"
  }
}
```

**Expected Response**: `201 Created`

**Validation**:
- ✅ Student created
- ✅ **Uses existing guardian** (same guardian ID as Tanaka)
- ✅ Both students now linked to same guardian

**Verify Sibling Relationship**:
```
GET /api/school/guardians/{guardianId}/children
```
Should return both Tanaka and Rudo.

---

### Test 2.3: Bulk Student Upload via Excel
**Endpoint**: `POST /api/school/students/upload-excel`

**Headers**: `X-School-ID: 1`

**Request**: `multipart/form-data`
- **file**: Upload `student-bulk-upload-template.csv`

**Expected Response**: `200 OK`
```json
{
  "message": "File processed successfully",
  "successCount": 5,
  "failureCount": 0,
  "errors": []
}
```

**Template Location**: `student-bulk-upload-template.csv` in project root

**Validation**:
- ✅ All 5 students from template created
- ✅ Siblings detected (Sarah Chikwanha's children)
- ✅ Fee records created automatically

**Check Results**:
```
GET /api/school/students
Headers: X-School-ID: 1
```

---

## 3️⃣ Fee Assignment (Bursar Operations)

### Test 3.1: Assign Fees to Single Grade
**Endpoint**: `POST /api/school/fee-records/bulk/grade/{grade}`

**Headers**: `X-School-ID: 1`

**Path Parameter**: `grade = FORM_3`

**Request Body**:
```json
{
  "termYear": "2025-Term1",
  "feeCategory": "DAY_SCHOLAR",
  "tuitionFee": 450.00,
  "boardingFee": 0.00,
  "developmentLevy": 50.00,
  "examFee": 30.00,
  "otherFees": 20.00
}
```

**Expected Response**: `201 Created`
```json
{
  "message": "Fees assigned successfully to grade",
  "grade": "FORM_3",
  "studentsAffected": 1,
  "feeRecords": [...]
}
```

**Validation**:
- ✅ Fee records created for all Form 3 students
- ✅ Gross amount = 450 + 0 + 50 + 30 + 20 = 550.00
- ✅ Net amount = Gross - discounts
- ✅ Outstanding balance = Net - amountPaid

---

### Test 3.2: Assign Fees to ALL Students
**Endpoint**: `POST /api/school/fee-records/bulk/all-students`

**Headers**: `X-School-ID: 1`

**Request Body**:
```json
{
  "termYear": "2025-Term1",
  "feeCategory": "BOARDER",
  "tuitionFee": 450.00,
  "boardingFee": 350.00,
  "developmentLevy": 50.00,
  "examFee": 30.00,
  "otherFees": 20.00
}
```

**Expected Response**: `201 Created`

**Validation**:
- ✅ All students in school get fee records
- ✅ Gross amount = 900.00 per student
- ✅ Multi-tenant: Only affects School 1 students

**Verify Multi-Tenancy**:
1. Assign fees to School 1 (X-School-ID: 1)
2. Check School 2 students (X-School-ID: 2) - should have NO fee records

---

### Test 3.3: Apply Scholarship
**Endpoint**: `POST /api/school/fee-records/{feeRecordId}/scholarship`

**Headers**: `X-School-ID: 1`

**Request Body**:
```json
{
  "scholarshipAmount": 100.00
}
```

**Expected Response**: `200 OK`

**Validation**:
- ✅ `hasScholarship` = true
- ✅ `scholarshipAmount` = 100.00
- ✅ `netAmount` reduced by scholarship
- ✅ `outstandingBalance` recalculated

---

### Test 3.4: Apply Sibling Discount
**Endpoint**: `POST /api/school/fee-records/{feeRecordId}/sibling-discount`

**Headers**: `X-School-ID: 1`

**Request Body**:
```json
{
  "siblingDiscount": 50.00
}
```

**Expected Response**: `200 OK`

**Validation**:
- ✅ `siblingDiscount` = 50.00
- ✅ `netAmount` reduced
- ✅ Automatically applied to 2nd+ children

---

## 4️⃣ Bank Payment Processing

**NO X-School-ID HEADER NEEDED** - Bank operates across all schools

### Test 4.1: Student Lookup by Reference
**Endpoint**: `POST /api/bank/lookup`

**Request Body**:
```json
{
  "studentReference": "CHHS001-STU2025001"
}
```

**Expected Response**: `200 OK`
```json
{
  "student": {
    "studentId": "STU2025001",
    "firstName": "Tanaka",
    "lastName": "Moyo",
    "studentReference": "CHHS001-STU2025001",
    "grade": "FORM_3"
  },
  "school": {
    "schoolName": "Chitungwiza High School",
    "schoolCode": "CHHS001"
  },
  "feeRecord": {
    "termYear": "2025-Term1",
    "grossAmount": 550.00,
    "netAmount": 550.00,
    "amountPaid": 0.00,
    "outstandingBalance": 550.00,
    "paymentStatus": "UNPAID"
  },
  "guardian": {
    "fullName": "Grace Moyo",
    "primaryPhone": "+263771234567"
  }
}
```

**Validation**:
- ✅ Returns complete student + school + fee + guardian info
- ✅ Bank can see outstanding balance
- ✅ Works across all schools

---

### Test 4.2: Search Students by Name
**Endpoint**: `POST /api/bank/search`

**Request Body**:
```json
{
  "studentName": "Moyo",
  "schoolName": "Chitungwiza",
  "grade": "FORM_3"
}
```

**Expected Response**: `200 OK` (Array of matches)
```json
[
  {
    "student": {
      "firstName": "Tanaka",
      "lastName": "Moyo",
      ...
    },
    "school": {...},
    "feeRecord": {...}
  }
]
```

**Validation**:
- ✅ Returns all students matching "Moyo"
- ✅ Filtered by school name (partial match)
- ✅ Filtered by grade
- ✅ Empty array if no matches

**Test Cases**:
- Search by first name only: `{"studentName": "Tanaka"}`
- Search by last name only: `{"studentName": "Moyo"}`
- Search by school only: `{"schoolName": "Chitungwiza"}`
- Search with no filters: `{}` (returns all students)

---

### Test 4.3: Process Counter Payment (Bank Teller)
**Endpoint**: `POST /api/bank/counter-payment`

**Request Body**:
```json
{
  "studentReference": "CHHS001-STU2025001",
  "amount": 250.00,
  "paymentMethod": "CASH",
  "receiptNumber": "RCP2025001",
  "tellerName": "John Banda",
  "branchCode": "HRE001",
  "notes": "Partial payment - parent paid cash at counter"
}
```

**Expected Response**: `200 OK`
```json
{
  "message": "Payment processed successfully",
  "payment": {
    "id": 1,
    "amount": 250.00,
    "paymentMethod": "CASH",
    "receiptNumber": "RCP2025001",
    "paymentDate": "2025-12-12T14:30:00",
    "tellerName": "John Banda",
    "branchCode": "HRE001"
  },
  "updatedFeeRecord": {
    "amountPaid": 250.00,
    "outstandingBalance": 300.00,
    "paymentStatus": "PARTIAL"
  }
}
```

**Validation**:
- ✅ Payment record created
- ✅ Fee record `amountPaid` increased by 250.00
- ✅ `outstandingBalance` decreased to 300.00
- ✅ `paymentStatus` changed from UNPAID to PARTIAL

**Payment Methods**: CASH, CARD, MOBILE_MONEY, BANK_TRANSFER, CHEQUE

---

### Test 4.4: Process Digital Payment
**Endpoint**: `POST /api/bank/digital-payment`

**Request Body**:
```json
{
  "studentReference": "CHHS001-STU2025001",
  "amount": 300.00,
  "paymentMethod": "MOBILE_MONEY",
  "transactionId": "TXN987654321",
  "digitalChannel": "ECOCASH",
  "customerPhone": "+263771234567",
  "notes": "Full payment via Ecocash"
}
```

**Expected Response**: `200 OK`

**Validation**:
- ✅ Payment recorded with transaction ID
- ✅ Fee record now shows `amountPaid: 550.00` (250 + 300)
- ✅ `outstandingBalance: 0.00`
- ✅ `paymentStatus: PAID`

**Digital Channels**: ECOCASH, ONEMONEY, INNBUCKS, INTERNET_BANKING, MOBILE_APP

---

### Test 4.5: Full Payment in One Transaction
**Endpoint**: `POST /api/bank/counter-payment`

**Request Body**:
```json
{
  "studentReference": "CHHS001-STU2025002",
  "amount": 550.00,
  "paymentMethod": "CARD",
  "receiptNumber": "RCP2025002",
  "tellerName": "Mary Phiri",
  "branchCode": "HRE001"
}
```

**Expected Response**: `200 OK`

**Validation**:
- ✅ `amountPaid: 550.00`
- ✅ `outstandingBalance: 0.00`
- ✅ `paymentStatus: PAID` (not PARTIAL)
- ✅ Payment status automatically calculated

---

## 5️⃣ Financial Reports (Bursar)

### Test 5.1: Get All Fee Records
**Endpoint**: `GET /api/school/fee-records`

**Headers**: `X-School-ID: 1`

**Expected Response**: `200 OK` (Array of fee records)

**Validation**:
- ✅ Returns only School 1 fee records
- ✅ Multi-tenant isolation working

---

### Test 5.2: Get Fee Records by Payment Status
**Endpoint**: `GET /api/school/fee-records/status/{status}`

**Headers**: `X-School-ID: 1`

**Path Parameter**: `status = UNPAID`

**Expected Response**: `200 OK`

**Validation**:
- ✅ Returns only UNPAID records
- ✅ Filtered by current school

**Status Values**: UNPAID, PARTIAL, PAID, ARREARS, OVERPAID

---

### Test 5.3: Get Arrears (Outstanding Balances)
**Endpoint**: `GET /api/school/fee-records/arrears`

**Headers**: `X-School-ID: 1`

**Expected Response**: `200 OK`

**Validation**:
- ✅ Returns students with outstanding balance > 0
- ✅ Useful for follow-up on unpaid fees

---

### Test 5.4: Calculate Total Outstanding
**Endpoint**: `GET /api/school/fee-records/total-outstanding`

**Headers**: `X-School-ID: 1`

**Expected Response**: `200 OK`
```json
{
  "totalOutstanding": 1250.00
}
```

**Validation**:
- ✅ Sum of all outstanding balances
- ✅ School-specific (multi-tenant)

---

### Test 5.5: Calculate Collection Rate
**Endpoint**: `GET /api/school/fee-records/collection-rate`

**Headers**: `X-School-ID: 1`

**Expected Response**: `200 OK`
```json
{
  "collectionRate": 65.50,
  "totalGross": 5500.00,
  "totalCollected": 3602.50
}
```

**Formula**: `(Total Collected / Total Gross) × 100`

**Validation**:
- ✅ Shows percentage of fees collected
- ✅ Helps track financial performance

---

## 6️⃣ Guardian Management

### Test 6.1: Get All Guardians
**Endpoint**: `GET /api/school/guardians`

**Headers**: `X-School-ID: 1`

**Expected Response**: `200 OK` (Array of guardians)

**Validation**:
- ✅ Returns only School 1 guardians
- ✅ Multi-tenant isolation

---

### Test 6.2: Get Guardian's Children
**Endpoint**: `GET /api/school/guardians/{guardianId}/children`

**Headers**: `X-School-ID: 1`

**Expected Response**: `200 OK`
```json
[
  {
    "firstName": "Tanaka",
    "lastName": "Moyo",
    ...
  },
  {
    "firstName": "Rudo",
    "lastName": "Moyo",
    ...
  }
]
```

**Validation**:
- ✅ Returns all students linked to this guardian
- ✅ Demonstrates sibling relationship

---

### Test 6.3: Get Guardians with Multiple Children
**Endpoint**: `GET /api/school/guardians/with-multiple-children`

**Headers**: `X-School-ID: 1`

**Expected Response**: `200 OK`

**Validation**:
- ✅ Returns only guardians with 2+ children
- ✅ Useful for applying sibling discounts

---

## 7️⃣ Multi-Tenancy Validation

### Test 7.1: Create Second School
**Endpoint**: `POST /api/bank/admin/schools`

**Request Body**:
```json
{
  "schoolName": "Harare High School",
  "schoolCode": "HHS001",
  "contactEmail": "admin@hararehigh.ac.zw",
  "contactPhone": "+263773456789",
  "address": "456 Second Street, Harare"
}
```

**Expected**: School 2 created with `id: 2`

---

### Test 7.2: Add Student to School 2
**Endpoint**: `POST /api/school/students`

**Headers**: `X-School-ID: 2`

**Request Body**:
```json
{
  "studentId": "STU2025100",
  "firstName": "David",
  "lastName": "Mutasa",
  "dateOfBirth": "2011-03-10",
  "gender": "MALE",
  "grade": "FORM_2",
  "guardian": {
    "fullName": "Peter Mutasa",
    "primaryPhone": "+263774567890",
    "email": "peter.mutasa@email.com"
  }
}
```

**Expected**: Student created with `studentReference: "HHS001-STU2025100"`

---

### Test 7.3: Verify Data Isolation
**Test A**: Get School 1 students
```
GET /api/school/students
Headers: X-School-ID: 1
```
**Expected**: Returns Tanaka, Rudo, etc. (School 1 students only)

**Test B**: Get School 2 students
```
GET /api/school/students
Headers: X-School-ID: 2
```
**Expected**: Returns David Mutasa only (School 2 students only)

**Test C**: Try to access School 2 student with School 1 header
```
GET /api/school/students/{davidId}
Headers: X-School-ID: 1
```
**Expected**: `403 Forbidden` or `404 Not Found`

**Validation**:
- ✅ School 1 cannot see School 2 data
- ✅ School 2 cannot see School 1 data
- ✅ Multi-tenant isolation working perfectly

---

## 8️⃣ Bank Admin Analytics

### Test 8.1: Get All Schools Summary
**Endpoint**: `GET /api/bank/admin/schools`

**Expected Response**: `200 OK`
```json
[
  {
    "id": 1,
    "schoolName": "Chitungwiza High School",
    "totalStudents": 6,
    "activeStudents": 6,
    "totalOutstanding": 1250.00
  },
  {
    "id": 2,
    "schoolName": "Harare High School",
    "totalStudents": 1,
    "activeStudents": 1,
    "totalOutstanding": 0.00
  }
]
```

**Validation**:
- ✅ Shows all onboarded schools
- ✅ Cross-school analytics
- ✅ Bank-level view (no X-School-ID needed)

---

### Test 8.2: Get School Details
**Endpoint**: `GET /api/bank/admin/schools/{schoolId}`

**Path Parameter**: `schoolId = 1`

**Expected Response**: `200 OK`
```json
{
  "school": {...},
  "totalStudents": 6,
  "activeStudents": 6,
  "studentsByGrade": {
    "FORM_1": 1,
    "FORM_3": 1,
    "GRADE_1": 2,
    "GRADE_2": 2
  },
  "totalFeesAssigned": 5500.00,
  "totalPaymentsReceived": 3250.00,
  "totalOutstanding": 2250.00,
  "paymentsByMethod": {
    "CASH": 1,
    "CARD": 1,
    "MOBILE_MONEY": 1
  }
}
```

**Validation**:
- ✅ Comprehensive school analytics
- ✅ Student distribution by grade
- ✅ Financial summary
- ✅ Payment method breakdown

---

## 9️⃣ Error Handling Tests

### Test 9.1: Invalid Student Reference
**Endpoint**: `POST /api/bank/lookup`

**Request Body**:
```json
{
  "studentReference": "INVALID-REF"
}
```

**Expected Response**: `404 Not Found`
```json
{
  "error": "Student not found with reference: INVALID-REF"
}
```

---

### Test 9.2: Missing X-School-ID Header
**Endpoint**: `GET /api/school/students`

**Headers**: None (missing X-School-ID)

**Expected Response**: `400 Bad Request`
```json
{
  "error": "X-School-ID header is required"
}
```

---

### Test 9.3: Duplicate Student ID
**Endpoint**: `POST /api/school/students`

**Headers**: `X-School-ID: 1`

**Request Body**: Use existing `studentId: "STU2025001"`

**Expected Response**: `409 Conflict`
```json
{
  "error": "Student with ID STU2025001 already exists in this school"
}
```

---

### Test 9.4: Payment Amount Exceeds Outstanding
**Endpoint**: `POST /api/bank/counter-payment`

**Request Body**:
```json
{
  "studentReference": "CHHS001-STU2025001",
  "amount": 10000.00,
  "paymentMethod": "CASH",
  "receiptNumber": "RCP2025099"
}
```

**Expected Response**: `200 OK` (Overpayment allowed)

**Validation**:
- ✅ Payment accepted
- ✅ `paymentStatus: OVERPAID`
- ✅ `outstandingBalance` becomes negative

---

### Test 9.5: Invalid Payment Method
**Endpoint**: `POST /api/bank/counter-payment`

**Request Body**:
```json
{
  "studentReference": "CHHS001-STU2025001",
  "amount": 100.00,
  "paymentMethod": "BITCOIN",
  "receiptNumber": "RCP2025100"
}
```

**Expected Response**: `400 Bad Request`
```json
{
  "error": "Invalid payment method: BITCOIN"
}
```

---

## 🔟 Edge Cases & Advanced Tests

### Test 10.1: Search with No Criteria
**Endpoint**: `POST /api/bank/search`

**Request Body**: `{}`

**Expected Response**: `200 OK` (All students from all schools)

**Validation**:
- ✅ Returns students from all schools
- ✅ Demonstrates bank's cross-school access

---

### Test 10.2: Guardian Phone Uniqueness Within School
**School 1**: Create student with guardian phone `+263771111111`
**School 2**: Create student with SAME guardian phone `+263771111111`

**Expected**:
- ✅ Both succeed
- ✅ Two separate guardian records created
- ✅ Phone is unique **per school**, not globally

---

### Test 10.3: Bulk Upload with Siblings
Upload Excel with these rows:
```
StudentID,First Name,Last Name,...,Parent Phone
STU001,John,Smith,...,+263771111111
STU002,Jane,Smith,...,+263771111111  ← Same phone
```

**Expected**:
- ✅ Both students created
- ✅ **Single guardian created** (shared by both)
- ✅ Automatic sibling detection

---

### Test 10.4: Apply Sibling Discount to Family
**Steps**:
1. Find guardian with multiple children
2. Get fee records for both children
3. Apply sibling discount to 2nd child

**Validation**:
- ✅ 1st child: Full fees
- ✅ 2nd child: Fees minus sibling discount
- ✅ Family pays less overall

---

## 📊 Complete Test Flow

### End-to-End User Journey: Parent Paying Fees

**Scenario**: Grace Moyo walks into bank to pay for both her children.

**Step 1**: Bank teller searches for first child
```
POST /api/bank/lookup
Body: {"studentReference": "CHHS001-STU2025001"}
```

**Step 2**: Check outstanding balance → $550.00

**Step 3**: Process payment for child 1
```
POST /api/bank/counter-payment
Body: {
  "studentReference": "CHHS001-STU2025001",
  "amount": 550.00,
  "paymentMethod": "CASH",
  "receiptNumber": "RCP001"
}
```

**Step 4**: Search for second child (sibling)
```
GET /api/school/guardians/{guardianId}/children
```

**Step 5**: Process payment for child 2
```
POST /api/bank/counter-payment
Body: {
  "studentReference": "CHHS001-STU2025002",
  "amount": 500.00,
  "paymentMethod": "CASH",
  "receiptNumber": "RCP002"
}
```

**Step 6**: Bursar checks collection rate
```
GET /api/school/fee-records/collection-rate
Headers: X-School-ID: 1
```

**Validation**:
- ✅ Both children fully paid
- ✅ Bank transaction records created
- ✅ School fee records updated
- ✅ Collection rate increased

---

## ✅ Test Checklist

### Bank Operations
- [ ] List all schools
- [ ] Onboard new school
- [ ] Lookup student by reference
- [ ] Search students by name/school/grade
- [ ] Process counter payment (cash/card)
- [ ] Process digital payment (mobile money)
- [ ] View school analytics

### School/Bursar Operations
- [ ] Create single student
- [ ] Enroll siblings (same guardian)
- [ ] Bulk upload via Excel
- [ ] Assign fees to grade
- [ ] Assign fees to class
- [ ] Assign fees to all students
- [ ] Apply scholarship
- [ ] Apply sibling discount
- [ ] View all fee records
- [ ] View arrears
- [ ] Calculate collection rate

### Guardian Management
- [ ] View all guardians
- [ ] View guardian's children
- [ ] Find guardians with multiple children
- [ ] Update guardian info

### Multi-Tenancy
- [ ] Create multiple schools
- [ ] Add students to different schools
- [ ] Verify data isolation (School A can't see School B)
- [ ] Test guardian phone uniqueness per school
- [ ] Test with/without X-School-ID header

### Error Handling
- [ ] Invalid student reference
- [ ] Missing required header
- [ ] Duplicate student ID
- [ ] Invalid payment method
- [ ] Invalid school code

---

## 🎯 Success Criteria

### Functional Requirements
- ✅ All endpoints return correct status codes
- ✅ Data validation working (required fields, formats)
- ✅ Business logic correct (fee calculations, payment status)
- ✅ Multi-tenancy enforced (data isolation)

### Performance
- ✅ API responds within 500ms for simple queries
- ✅ Bulk operations (1000 students) complete within 5 seconds
- ✅ No memory leaks or crashes

### Data Integrity
- ✅ Transactions atomic (payment updates fee record)
- ✅ No orphaned records (guardian deletion doesn't break students)
- ✅ Calculations accurate (totals, balances)

---

## 📝 Test Report Template

After testing, document results:

```markdown
## Test Execution Report

**Date**: 2025-12-12
**Tester**: [Your Name]
**Environment**: Local (localhost:8080)

### Summary
- Total Tests: 50
- Passed: 48
- Failed: 2
- Skipped: 0

### Failed Tests
1. **Test 4.3** - Counter payment with invalid receipt number
   - Error: Duplicate receipt number not caught
   - Fix Required: Add unique constraint

2. **Test 7.3** - Multi-tenancy isolation
   - Error: School 1 can see School 2 guardian data
   - Fix Required: Add security check in GuardianController

### Recommendations
- Add receipt number uniqueness validation
- Enhance multi-tenant security checks
- Add rate limiting for payment endpoints

### Sign-off
- [ ] All critical tests passed
- [ ] Ready for deployment
```

---

## 🚀 Quick Start Commands

### PowerShell Test Script
```powershell
# Set base URL
$baseUrl = "http://localhost:8080"

# Test 1: Get all schools
$schools = Invoke-RestMethod -Uri "$baseUrl/api/bank/schools" -Method Get
Write-Host "Schools: $($schools.Count)"

# Test 2: Create student
$student = @{
    studentId = "STU2025999"
    firstName = "Test"
    lastName = "Student"
    dateOfBirth = "2010-01-01"
    gender = "MALE"
    grade = "FORM_1"
    guardian = @{
        fullName = "Test Parent"
        primaryPhone = "+263779999999"
    }
} | ConvertTo-Json

$headers = @{"X-School-ID" = "1"}
Invoke-RestMethod -Uri "$baseUrl/api/school/students" -Method Post -Body $student -ContentType "application/json" -Headers $headers
```

---

## 📚 Additional Resources

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Documentation**: `API-DOCUMENTATION.md`
- **Bulk Upload Guide**: `BULK-UPLOAD-GUIDE.md`
- **Multi-Tenant Guide**: `MULTI_TENANT_ARCHITECTURE.md`

---

**Good luck with testing! 🎉**
