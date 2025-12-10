# 💰 Bursar API Guide - Financial Management Endpoints

This guide covers all the financial management endpoints for school bursars.

## 📋 Overview

Your Student entity now includes comprehensive financial tracking:
- Fee categories (Day Scholar, Boarder, etc.)
- Individual fee components (tuition, boarding, development levy, exam fees)
- Payment tracking (amount paid, outstanding balance)
- Scholarships and discounts
- Automatic payment status calculation

## 🎯 Key Concepts

### Payment Status (Auto-calculated)
The system automatically sets payment status based on payments:
- **PAID** - Outstanding balance ≤ 0 (fully paid)
- **PARTIALLY_PAID** - Some payment made but still owes money
- **ARREARS** - No payment made (amount paid = 0)

### Automatic Calculations
The `@PrePersist` method in Student entity automatically calculates:
```java
totalBilledAmount = tuitionFee + boardingFee + developmentLevy + examFee 
                    - scholarshipAmount - siblingDiscount

outstandingBalance = totalBilledAmount + previousBalance - amountPaid
```

## 📊 New Bursar Endpoints

### 1. Query Students by Payment Status

**Get Students with Arrears:**
```http
GET http://localhost:8080/api/students/payment-status/ARREARS
```

**Get Fully Paid Students:**
```http
GET http://localhost:8080/api/students/payment-status/PAID
```

**Get Partially Paid Students:**
```http
GET http://localhost:8080/api/students/payment-status/PARTIALLY_PAID
```

### 2. Query by Fee Category

**Get All Boarders:**
```http
GET http://localhost:8080/api/students/fee-category/Boarder
```

**Get All Day Scholars:**
```http
GET http://localhost:8080/api/students/fee-category/Day%20Scholar
```

### 3. Scholarship Management

**Get All Scholarship Students:**
```http
GET http://localhost:8080/api/students/scholarships
```

### 4. Outstanding Balance Queries

**Get Students Who Still Owe Money:**
```http
GET http://localhost:8080/api/students/outstanding-balance
```

**Get Students Who Have Fully Paid:**
```http
GET http://localhost:8080/api/students/fully-paid
```

### 5. Combined Queries

**Get Grade 5 Students with Arrears:**
```http
GET http://localhost:8080/api/students/grade/Grade%205/payment-status/ARREARS
```

**Search Students with Arrears by Name:**
```http
GET http://localhost:8080/api/students/arrears/search?name=Moyo
```

### 6. Record Payment

**Record a Payment (POST):**
```http
POST http://localhost:8080/api/students/1/payment
Content-Type: application/json

{
    "amount": 500.00
}
```

This will:
- Add 500 to the student's `amountPaid`
- Recalculate `outstandingBalance`
- Update `paymentStatus` automatically

### 7. Financial Reports

**Total Outstanding Fees (All Students):**
```http
GET http://localhost:8080/api/students/reports/total-outstanding
```
Returns: Single number (e.g., 15000.00)

**Total Collected Fees (All Students):**
```http
GET http://localhost:8080/api/students/reports/total-collected
```
Returns: Single number (e.g., 45000.00)

**Count Students by Payment Status:**
```http
GET http://localhost:8080/api/students/reports/count-by-payment-status?status=ARREARS
```
Returns: Number of students (e.g., 25)

## 📤 Excel Upload with Bursar Fields

### New Excel Format (20 Columns)

Your Excel file should now have these columns:

| # | Column Name | Required | Description |
|---|-------------|----------|-------------|
| 1 | Student ID | No | Auto-generated if empty |
| 2 | First Name | Yes | Student's first name |
| 3 | Last Name | Yes | Student's last name |
| 4 | Date of Birth | No | Format: YYYY-MM-DD |
| 5 | Grade | No | e.g., Grade 5 |
| 6 | Parent Name | Yes | Parent/guardian name |
| 7 | Parent Phone | Yes | Phone number |
| 8 | Parent Email | No | Email address |
| 9 | Address | No | Physical address |
| 10 | Fee Category | No | Day Scholar, Boarder, etc. |
| 11 | Tuition Fee | No | Defaults to 0.00 |
| 12 | Boarding Fee | No | Defaults to 0.00 |
| 13 | Development Levy | No | Defaults to 0.00 |
| 14 | Exam Fee | No | Defaults to 0.00 |
| 15 | Previous Balance | No | From previous term/year |
| 16 | Amount Paid | No | Total paid so far |
| 17 | Has Scholarship | No | YES/NO or TRUE/FALSE |
| 18 | Scholarship Amount | No | Scholarship value |
| 19 | Sibling Discount | No | Discount amount |
| 20 | Bursar Notes | No | Any notes |

### Generate Sample Excel

Run this command to create a sample Excel file:
```powershell
.\create_sample_excel.ps1
```

This creates `students_sample.csv` with 5 sample students including all bursar fields.

### Upload Excel

```http
POST http://localhost:8080/api/students/upload-excel
Content-Type: multipart/form-data

file: [Select your .xlsx file]
```

## 🧪 Testing Scenarios

### Scenario 1: Find Students Who Haven't Paid

```http
GET http://localhost:8080/api/students/payment-status/ARREARS
```

Expected: List of students with `paymentStatus: "ARREARS"` and `amountPaid: 0`

### Scenario 2: Record a Payment

1. Get a student with arrears:
```http
GET http://localhost:8080/api/students/outstanding-balance
```

2. Note their ID (e.g., ID: 3) and outstanding balance (e.g., $580.00)

3. Record a payment:
```http
POST http://localhost:8080/api/students/3/payment

{
    "amount": 300.00
}
```

4. Verify the response:
- `amountPaid` should increase by 300
- `outstandingBalance` should decrease by 300
- `paymentStatus` should change to "PARTIALLY_PAID"

### Scenario 3: Financial Summary Report

Get total outstanding fees:
```http
GET http://localhost:8080/api/students/reports/total-outstanding
```

Get total collected:
```http
GET http://localhost:8080/api/students/reports/total-collected
```

Calculate collection rate:
```
Collection Rate = (Total Collected / (Total Collected + Total Outstanding)) × 100%
```

### Scenario 4: Follow Up with Parents

1. Find students with arrears in a specific grade:
```http
GET http://localhost:8080/api/students/grade/Grade%205/payment-status/ARREARS
```

2. For each student, you have:
- Student name
- Parent name
- Parent phone (`parentPhone`)
- Parent email (`parentEmail`)
- Outstanding amount (`outstandingBalance`)

3. Use this data to contact parents for payment follow-up

## 💡 PowerShell Testing Examples

### Record Multiple Payments
```powershell
# Record payment for student ID 1
$payment = @{ amount = 500.00 } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/students/1/payment" `
    -Method Post -Body $payment -ContentType "application/json"

# Record payment for student ID 2
$payment = @{ amount = 300.00 } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/students/2/payment" `
    -Method Post -Body $payment -ContentType "application/json"
```

### Get Financial Summary
```powershell
# Get all key metrics
$outstanding = Invoke-RestMethod -Uri "http://localhost:8080/api/students/reports/total-outstanding"
$collected = Invoke-RestMethod -Uri "http://localhost:8080/api/students/reports/total-collected"
$arrearsCount = Invoke-RestMethod -Uri "http://localhost:8080/api/students/reports/count-by-payment-status?status=ARREARS"
$paidCount = Invoke-RestMethod -Uri "http://localhost:8080/api/students/reports/count-by-payment-status?status=PAID"

Write-Host "Financial Summary:" -ForegroundColor Green
Write-Host "Total Outstanding: $outstanding" -ForegroundColor Yellow
Write-Host "Total Collected: $collected" -ForegroundColor Green
Write-Host "Students in Arrears: $arrearsCount" -ForegroundColor Red
Write-Host "Students Fully Paid: $paidCount" -ForegroundColor Green
```

## 🎓 What You've Learned

### 1. Financial Data Modeling
- Using `BigDecimal` for monetary amounts (avoids floating-point errors)
- Automatic calculation with `@PrePersist`
- Derived fields (paymentStatus calculated from other fields)

### 2. Complex Queries
- Combining multiple filters (grade + payment status)
- Aggregate functions (SUM, COUNT)
- Sorting by amount (largest outstanding first)

### 3. Business Logic in Services
- Payment recording logic
- Balance calculations
- Status determination rules

### 4. Bank Integration Patterns
- Payment recording endpoint (for mobile app/internet banking)
- Student lookup by parent phone (link to bank account)
- Real-time balance updates

## 🚀 Bank Channel Integration

Your endpoints are now ready for integration with:

### Mobile Banking App
```javascript
// When parent logs in, show their students
GET /api/students/parent-phone/{phone}

// Show outstanding balance
Response: [
  {
    studentId: "STU001",
    firstName: "John",
    lastName: "Doe",
    outstandingBalance: 500.00,
    paymentStatus: "PARTIALLY_PAID"
  }
]

// Parent makes payment
POST /api/students/{id}/payment
{ amount: 500.00 }
```

### Internet Banking
Same endpoints, different UI - web-based instead of mobile

### Branch/Teller System
Tellers can:
1. Look up student by name or ID
2. See outstanding balance
3. Record cash/check payments
4. Print receipt with updated balance

## 📈 Next Steps

Consider adding:
- Payment history (track each payment separately)
- PDF receipt generation
- Email notifications when payment received
- SMS alerts for low balance
- Payment plans (installments)
- Late payment fees
- Term-based fee structure

All of these can be built on top of your existing foundation! 🎉
