# School Management System - User Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [User Roles](#user-roles)
4. [Common Workflows](#common-workflows)
5. [Troubleshooting](#troubleshooting)

---

## Introduction

The School Management System is a comprehensive platform designed for schools to manage:
- Student enrollment and records
- Fee management and payments
- Academic year-end promotions
- Bank payment integration

### Key Features
- **Multi-tenant**: Multiple schools on one platform
- **Automated Promotions**: Configure once, promote automatically
- **Payment Integration**: Connects with Oracle FLEXCUBE
- **Audit Trail**: Every change is tracked

---

## Getting Started

### For School Administrators

#### 1. Access Your School Portal
- Your school has a unique **School ID**
- All requests require the `X-School-ID` header
- Contact your bank relationship manager for your School ID

#### 2. Initial Setup
```
Step 1: Verify school information
Step 2: Upload existing student data (Excel)
Step 3: Configure fee structures
Step 4: Set up academic year-end dates
```

---

## User Roles

### School Administrator
**Permissions:**
- Manage students (create, update, delete)
- Configure academic year settings
- Trigger year-end promotions
- View all reports

**Common Tasks:**
- Enroll new students
- Update student information
- Configure end-of-year promotion dates
- Demote repeating students

### Bursar (Financial Officer)
**Permissions:**
- Manage fee records
- Record payments
- View financial reports
- Update fee structures

**Common Tasks:**
- Create fee records for new students
- Record school-based payments
- Generate financial reports
- Update outstanding balances

### Bank Teller
**Permissions:**
- Record counter payments
- View student fee information
- Search students by parent phone

**Common Tasks:**
- Process over-the-counter payments
- Look up student fee balances
- Record payment transactions

---

## Common Workflows

### Workflow 1: Enrolling a New Student

**Manual Entry:**
```http
POST /api/school/students
X-School-ID: 1
Content-Type: application/json

{
  "firstName": "Tanaka",
  "lastName": "Moyo",
  "dateOfBirth": "2010-05-15",
  "grade": "Form 1",
  "className": "Form 1A",
  "gender": "MALE",
  "nationalId": "63-1234567X63"
}
```

**Bulk Upload via Excel:**
1. Download the Excel template
2. Fill in student information
3. Upload via POST /api/school/students/upload-excel
4. Review success/failure summary
5. Fix any errors and re-upload failed records

**Required Fields:**
- First Name
- Last Name
- Grade

**Optional Fields:**
- Student ID (auto-generated if not provided)
- Date of Birth
- Gender
- National ID
- Parent information
- Fee information

### Workflow 2: Recording a Payment

**School-Based Payment (Cash/Check):**
```http
POST /api/school/payments/student/{studentId}
X-School-ID: 1

{
  "amount": 500.00,
  "paymentMethod": "CASH",
  "paymentDate": "2026-01-06",
  "receiptNumber": "RCT-2026-001",
  "notes": "Term 1 payment"
}
```

**Bank Counter Payment:**
```http
POST /api/bank/payment/counter

{
  "studentId": "STU2025001",
  "schoolId": 1,
  "amount": 500.00,
  "paymentMethod": "BANK_TRANSFER",
  "bankTransactionId": "BNK-TXN-123456",
  "tellerName": "Jane Doe",
  "branchName": "Harare Main Branch"
}
```

**Digital Banking Payment:**
```http
POST /api/bank/payment/digital

{
  "studentId": "STU2025001",
  "schoolId": 1,
  "amount": 500.00,
  "paymentMethod": "MOBILE_MONEY",
  "bankTransactionId": "ECOCASH-789456",
  "parentAccountNumber": "1234567890"
}
```

### Workflow 3: Year-End Student Promotion

**Option A: Automated Promotion (Recommended)**

**Step 1: Configure End-of-Year Date**
```http
POST /api/school/academic-year-config
X-School-ID: 1

{
  "academicYear": "2025/2026",
  "endOfYearDate": "2025-12-31",
  "nextAcademicYear": "2026",
  "carryForwardBalances": true,
  "feeStructures": {
    "Form 2": {
      "tuitionFee": 600.00,
      "boardingFee": 350.00,
      "developmentLevy": 100.00,
      "examFee": 50.00,
      "otherFees": 0.00
    },
    "Form 3": {
      "tuitionFee": 620.00,
      "boardingFee": 350.00,
      "developmentLevy": 100.00,
      "examFee": 60.00,
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

**Step 2: System Automatically Promotes on December 31**
- Scheduler runs daily at 2:00 AM
- Checks for configurations with endOfYearDate = today
- Executes promotion automatically
- Updates all students to next grade/form
- Creates new fee records
- Tracks completion (Grade 7, Form 4, Form 6)

**Step 3: Review Results**
```http
GET /api/school/academic-year-config/1
X-School-ID: 1
```

Response shows:
- Students promoted count
- Students completed count
- Any errors encountered

**Option B: Manual Trigger**

Execute promotion immediately without waiting for scheduled date:
```http
POST /api/school/academic-year-config/1/execute
X-School-ID: 1
```

### Workflow 4: Handling Repeating Students

If a student was automatically promoted but should repeat the grade:

```http
POST /api/school/students/123/demote
X-School-ID: 1

{
  "demotedGrade": "Form 1",
  "demotedClassName": "Form 1A",
  "reason": "Failed end-of-year exams - must repeat Form 1",
  "academicYear": "2026 Academic Year",
  "carryForwardBalance": true,
  "tuitionFee": 600.00,
  "boardingFee": 350.00,
  "developmentLevy": 100.00,
  "examFee": 50.00,
  "otherFees": 0.00
}
```

**What happens:**
- Student grade reverted (Form 2 → Form 1)
- Completion status cleared
- Student reactivated if was deactivated
- New fee record created
- Audit trail updated with reason

### Workflow 5: Generating Financial Reports

**Outstanding Fees by Grade:**
```http
GET /api/school/fee-records/outstanding-by-grade?grade=Form%201
X-School-ID: 1
```

**Students in Arrears:**
```http
GET /api/school/fee-records/payment-status/ARREARS
X-School-ID: 1
```

**Total Outstanding for School:**
```http
GET /api/school/fee-records/total-outstanding
X-School-ID: 1
```

### Workflow 6: Searching for Students

**By Name:**
```http
GET /api/school/students/search?name=Tanaka
X-School-ID: 1
```

**By Grade:**
```http
GET /api/school/students/grade/Form%201
X-School-ID: 1
```

**By Student ID:**
```http
GET /api/school/students/student-id/STU2025001
X-School-ID: 1
```

**By National ID:**
```http
GET /api/school/students/national-id/63-1234567X63
X-School-ID: 1
```

---

## Troubleshooting

### Common Issues

#### Issue: "School context is required"
**Cause:** Missing X-School-ID header  
**Solution:** Add X-School-ID header to all school-specific requests

#### Issue: "Student not found"
**Cause:** Using wrong identifier or student belongs to different school  
**Solution:** 
- Verify student ID is correct
- Ensure X-School-ID matches student's school
- Use GET /api/school/students to list all students

#### Issue: Year-end promotion didn't trigger
**Cause:** Several possibilities  
**Solution:**
1. Check promotion status: GET /api/school/academic-year-config/latest
2. Verify endOfYearDate has passed
3. Confirm promotionStatus = "SCHEDULED"
4. Check application logs for errors
5. Manually trigger: POST /api/school/academic-year-config/{id}/execute

#### Issue: Excel upload fails
**Cause:** Invalid file format or data  
**Solution:**
1. Use .xlsx or .xls format only
2. Ensure required columns present (First Name, Last Name)
3. Check upload summary for specific row errors
4. Fix data and re-upload

#### Issue: Payment not reflecting on fee record
**Cause:** Payment amount not updating outstanding balance  
**Solution:**
1. Verify payment was successful (check response)
2. Get latest fee record: GET /api/school/fee-records/student-ref/{studentId}
3. Check audit trail for payment entry
4. Contact support if payment missing

#### Issue: Grade 7 students still active after promotion
**Cause:** Completion logic may have encountered error  
**Solution:**
1. Check promotion errors array in response
2. Review application logs
3. Manually complete student if needed
4. Contact support with student IDs

---

## Best Practices

### Student Management
1. **Always provide National ID** - Helps with student identification
2. **Use consistent grade naming** - "Form 1" not "form 1" or "F1"
3. **Bulk upload for new intakes** - Faster than manual entry
4. **Regular backups** - Export student data periodically

### Fee Management
1. **Create fee records for all students** - Before term starts
2. **Update outstanding balances promptly** - After payment processing
3. **Use payment methods correctly** - For accurate reporting
4. **Record receipt numbers** - For audit purposes

### Year-End Promotion
1. **Configure early** - Set up end-of-year config well in advance
2. **Test with manual trigger** - Before relying on scheduler
3. **Review results immediately** - Check for errors after promotion
4. **Have repeating student list ready** - For quick demotions

### Security
1. **Never share School ID publicly** - Treat as sensitive
2. **Limit bursar access** - Only authorized personnel
3. **Review audit trail regularly** - Monitor for unauthorized changes
4. **Use strong passwords** - For all user accounts

---

## Support

### Getting Help
- **Technical Issues:** Contact your bank relationship manager
- **System Errors:** Check application logs first
- **Feature Requests:** Submit via your bank contact
- **Training:** Request user training sessions

### Documentation
- **API Reference:** http://localhost:8080/swagger-ui.html
- **Technical Docs:** See /docs folder
- **Release Notes:** Check CHANGELOG.md

### Emergency Contacts
- **System Down:** Contact bank IT support immediately
- **Data Loss:** Restore from latest backup
- **Payment Discrepancies:** Contact bank reconciliation team
