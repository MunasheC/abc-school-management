# Bulk Fee Assignment Guide 💰

## Overview

Instead of setting fees for one student at a time, you can now assign fees to:
- **Entire grades** (Grade 1-7, Form 1-6)
- **Specific classes/forms** (5A, Form 3B)
- **Multiple grades** at once
- **All students** in school

**Zimbabwe Education System Support:**
- **Primary School:** Grade 1 → Grade 7 (with classes like 5A, 5B, 6C)
- **Secondary School:** Form 1 → Form 6 (with classes like Form 3A, Form 4B)

## Available Endpoints

### 1. Assign Fees by Grade Level 📚

**Endpoint:** `POST /api/fee-records/bulk/grade/{grade}`

**Primary School Example:** Set fees for all Grade 5 students

```http
POST http://localhost:8080/api/fee-records/bulk/grade/Grade 5
Content-Type: application/json

{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 500.00,
  "boardingFee": 300.00,
  "developmentLevy": 50.00,
  "examFee": 25.00,
  "otherFees": 20.00
}
```

**Response:**
```json
{
  "message": "Fees assigned successfully",
  "affectedGrades": "Grade 5",
  "studentsAffected": 25,
  "feeRecords": [
    {
      "id": 1,
      "termYear": "2025-Term1",
      "grossAmount": 870.00,
      "netAmount": 870.00,
      "outstandingBalance": 870.00,
      "paymentStatus": "ARREARS"
    }
    // ... all 25 students
  ]
}
```

**Secondary School Example:** Set fees for Form 3 students

```http
POST http://localhost:8080/api/fee-records/bulk/grade/Form 3
Content-Type: application/json

{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 600.00,
  "boardingFee": 350.00,
  "developmentLevy": 75.00,
  "examFee": 50.00,
  "otherFees": 25.00
}
```

### 2. Assign Fees to Specific Class/Form 🎓

**Endpoint:** `POST /api/fee-records/bulk/class/{className}`

**Primary School Example:** Set fees for class 5A only

```http
POST http://localhost:8080/api/fee-records/bulk/class/5A
Content-Type: application/json

{
  "termYear": "2025-Term1",
  "feeCategory": "Day Scholar",
  "tuitionFee": 400.00,
  "boardingFee": 0.00,
  "developmentLevy": 50.00,
  "examFee": 25.00,
  "otherFees": 20.00
}
```

**Secondary School Example:** Set fees for Form 3B only

```http
POST http://localhost:8080/api/fee-records/bulk/class/Form 3B
Content-Type: application/json

{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 600.00,
  "boardingFee": 350.00,
  "developmentLevy": 75.00,
  "examFee": 50.00,
  "otherFees": 25.00
}
```

### 3. Assign Fees to Grade AND Class 📖

**Endpoint:** `POST /api/fee-records/bulk/grade/{grade}/class/{className}`

**Example:** Set fees specifically for Grade 5, Class 5A

```http
POST http://localhost:8080/api/fee-records/bulk/grade/Grade 5/class/5A
Content-Type: application/json

{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 500.00,
  "boardingFee": 300.00,
  "developmentLevy": 50.00,
  "examFee": 25.00,
  "otherFees": 20.00
}
```

### 4. Assign Fees to Multiple Grades 📚

**Endpoint:** `POST /api/fee-records/bulk/grades`

**Primary School Example:** Set fees for all primary grades (1-7)

```http
POST http://localhost:8080/api/fee-records/bulk/grades
Content-Type: application/json

{
  "grades": ["Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Grade 6", "Grade 7"],
  "termYear": "2025-Term1",
  "feeCategory": "Day Scholar",
  "tuitionFee": 400.00,
  "boardingFee": 0.00,
  "developmentLevy": 50.00,
  "examFee": 25.00,
  "otherFees": 20.00
}
```

### 5. Assign Fees to All Forms (Secondary School) 🎓

**Endpoint:** `POST /api/fee-records/bulk/forms`

**Example:** Set fees for all secondary school forms (1-6)

```http
POST http://localhost:8080/api/fee-records/bulk/forms
Content-Type: application/json

{
  "grades": ["Form 1", "Form 2", "Form 3", "Form 4", "Form 5", "Form 6"],
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 600.00,
  "boardingFee": 350.00,
  "developmentLevy": 75.00,
  "examFee": 50.00,
  "otherFees": 25.00
}
```

**Use Cases:**
- Set fees for all primary grades (1-7)
- Set fees for all secondary forms (1-6)
- Apply fee changes to specific grade groups

### 6. Assign Fees to ALL Students 🏫

**Endpoint:** `POST /api/fee-records/bulk/all-students`

**Example:** Set uniform fees for entire school

```http
POST http://localhost:8080/api/fee-records/bulk/all-students
Content-Type: application/json

{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 500.00,
  "boardingFee": 300.00,
  "developmentLevy": 50.00,
  "examFee": 25.00,
  "otherFees": 20.00
}
```

**⚠️ Warning:** Use with caution - affects ALL active students!

## Request Fields Explained

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `termYear` | String | Term/year identifier | "2025-Term1", "2025-Term2" |
| `feeCategory` | String | Student category | "Boarder", "Day Scholar" |
| `tuitionFee` | Decimal | Base tuition amount | 500.00 |
| `boardingFee` | Decimal | Boarding charges (0 for day scholars) | 300.00 |
| `developmentLevy` | Decimal | Development/infrastructure levy | 50.00 |
| `examFee` | Decimal | Examination fees | 25.00 |
| `otherFees` | Decimal | Miscellaneous fees | 20.00 |

## How It Works

### Behind the Scenes:

1. **Find Students:** System finds all active students in specified grade(s)

2. **Check Existing Records:** For each student:
   - If fee record exists for this term → **Update** with new amounts
   - If no fee record exists → **Create** new record

3. **Preserve Payment Data:** When updating:
   - Previous payments are preserved
   - Outstanding balance is recalculated
   - Payment status is updated automatically

4. **Automatic Calculations:** For each record:
   - `grossAmount` = sum of all fees
   - `netAmount` = grossAmount - discounts
   - `outstandingBalance` = netAmount + previousBalance - amountPaid
   - `paymentStatus` = calculated based on outstanding balance

### Example Workflow:

```
1. Start of Term 1, 2025
   ↓
2. Bursar calls: POST /api/fee-records/bulk/grade/Grade 5
   {
     "termYear": "2025-Term1",
     "tuitionFee": 500.00,
     ...
   }
   ↓
3. System finds 25 students in Grade 5
   ↓
4. Creates 25 fee records with same amounts
   ↓
5. Each student can now make individual payments
   ↓
6. If fees change mid-term, call same endpoint again
   → Updates all 25 records
   → Preserves existing payments
```

## Comparison: Before vs After

### Before (Manual) ❌

```
For each of 25 students:
  POST /api/fee-records
  {
    "student": { "id": 1 },
    "tuitionFee": 500.00,
    ...
  }
```

**Result:** 25 separate API calls, tedious and error-prone

### After (Bulk) ✅

```
One API call:
  POST /api/fee-records/bulk/grade/Grade 5
  {
    "tuitionFee": 500.00,
    ...
  }
```

**Result:** 25 records created instantly!

## Common Scenarios (Zimbabwe Context)

### Scenario 1: Different Fees by School Level

```
# Primary School (Grades 1-7) - Lower fees
POST /api/fee-records/bulk/grades
{
  "grades": ["Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Grade 6", "Grade 7"],
  "termYear": "2025-Term1",
  "tuitionFee": 400.00,
  "boardingFee": 250.00,
  ...
}

# Secondary School (Forms 1-6) - Higher fees
POST /api/fee-records/bulk/forms
{
  "grades": ["Form 1", "Form 2", "Form 3", "Form 4", "Form 5", "Form 6"],
  "termYear": "2025-Term1",
  "tuitionFee": 600.00,
  "boardingFee": 350.00,
  ...
}
```

### Scenario 2: Different Fees by Class (Streaming)

```
# Set fees for top stream (5A) - might have scholarships
POST /api/fee-records/bulk/class/5A
{
  "termYear": "2025-Term1",
  "feeCategory": "Day Scholar",
  "tuitionFee": 400.00,
  ...
}

# Set fees for parallel stream (5B)
POST /api/fee-records/bulk/class/5B
{
  "termYear": "2025-Term1",
  "feeCategory": "Day Scholar",
  "tuitionFee": 400.00,
  ...
}

# Set fees for Form 4A (O-Level stream)
POST /api/fee-records/bulk/class/Form 4A
{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 650.00,
  "examFee": 100.00,  // Higher for ZIMSEC O-Level exams
  ...
}
```

### Scenario 3: Different Fees for Boarders vs Day Scholars

**Note:** You need to specify feeCategory in the request. To handle both:

```
# Set boarder fees for all Form 3 students
POST /api/fee-records/bulk/grade/Form 3
{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 600.00,
  "boardingFee": 350.00,
  ...
}

# Set day scholar fees for all Form 3 students
POST /api/fee-records/bulk/grade/Form 3
{
  "termYear": "2025-Term1", 
  "feeCategory": "Day Scholar",
  "tuitionFee": 600.00,
  "boardingFee": 0.00,
  ...
}
```

### Scenario 4: O-Level and A-Level Examination Years

```
# Form 4 (O-Level year) - Higher exam fees
POST /api/fee-records/bulk/grade/Form 4
{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 650.00,
  "examFee": 150.00,  // ZIMSEC O-Level registration
  ...
}

# Form 6 (A-Level year) - Highest exam fees
POST /api/fee-records/bulk/grade/Form 6
{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 700.00,
  "examFee": 200.00,  // ZIMSEC A-Level registration
  ...
}
```

### Scenario 5: Mid-Term Fee Increase

```
# Original fees set in January
POST /api/fee-records/bulk/all-students
{
  "termYear": "2025-Term1",
  "tuitionFee": 500.00,
  ...
}

# Fee increase in March (updates existing records)
POST /api/fee-records/bulk/all-students
{
  "termYear": "2025-Term1",
  "tuitionFee": 550.00,  // Increased by $50
  ...
}

Result: 
- All records updated with new amounts
- Existing payments preserved
- Outstanding balances recalculated
```

## Testing in Postman

### Step 1: Get Grades in System
```
GET http://localhost:8080/api/students
```
Look at the `grade` field to see what grades exist.

### Step 2: Assign Fees to a Grade
```
POST http://localhost:8080/api/fee-records/bulk/grade/Grade 5
Body: (raw JSON)
{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 500.00,
  "boardingFee": 300.00,
  "developmentLevy": 50.00,
  "examFee": 25.00,
  "otherFees": 20.00
}
```

### Step 3: Verify Fee Records Created
```
GET http://localhost:8080/api/fee-records/term/2025-Term1
```

### Step 4: Check Individual Student
```
GET http://localhost:8080/api/students/1
```
Should see the student has a fee record now.

## Benefits

✅ **Efficiency:** Set fees for 100+ students with one API call  
✅ **Consistency:** All students in grade get same fee structure  
✅ **Easy Updates:** Change fees mid-term, preserves payment history  
✅ **Audit Trail:** All changes logged via service layer  
✅ **Automatic Calculations:** Totals and balances computed automatically  
✅ **Safe:** Only affects active students, skips inactive ones  

## Next Steps

After assigning fees, students can make payments via:
```
POST http://localhost:8080/api/students/{id}/payment
{
  "amount": 200.00,
  "paymentMethod": "MOBILE_MONEY",
  "transactionReference": "TXN123456"
}
```

Payment automatically updates the student's fee record! 💳
