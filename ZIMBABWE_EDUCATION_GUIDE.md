# Zimbabwe Education System - Fee Assignment Quick Reference 🇿🇼

## Education Structure

### Primary School (ECD A → Grade 7)
- **ECD A** (Early Childhood Development A)
- **ECD B** (Early Childhood Development B)
- **Grade 1** → **Grade 7**

**Classes/Streams:** 1A, 1B, 2A, 2B, 3A, 3B, 4A, 4B, 5A, 5B, 6A, 6B, 7A, 7B

### Secondary School (Form 1 → Form 6)
- **Form 1** → **Form 4** (O-Level, ZIMSEC)
- **Form 5** → **Form 6** (A-Level, ZIMSEC)

**Classes/Streams:** Form 1A, Form 1B, Form 2A, Form 2B, etc.

---

## Fee Assignment Endpoints

### 1. By Grade Level (All Classes)

```bash
# Primary: All Grade 5 students (5A, 5B, 5C, etc.)
POST /api/fee-records/bulk/grade/Grade 5

# Secondary: All Form 3 students (Form 3A, Form 3B, etc.)
POST /api/fee-records/bulk/grade/Form 3
```

### 2. By Specific Class/Stream

```bash
# Primary: Only class 5A
POST /api/fee-records/bulk/class/5A

# Secondary: Only Form 3B
POST /api/fee-records/bulk/class/Form 3B
```

### 3. By Grade AND Class (Most Specific)

```bash
# Specific: Grade 5, Class 5A
POST /api/fee-records/bulk/grade/Grade 5/class/5A

# Specific: Form 3, Class Form 3B
POST /api/fee-records/bulk/grade/Form 3/class/Form 3B
```

### 4. Multiple Grades/Forms

```bash
# All primary school
POST /api/fee-records/bulk/grades
{ "grades": ["Grade 1", "Grade 2", ..., "Grade 7"] }

# All secondary school
POST /api/fee-records/bulk/forms
{ "grades": ["Form 1", "Form 2", ..., "Form 6"] }
```

---

## Typical Fee Structure

### Primary School (Grade 1-7)
```json
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

### Secondary School (Form 1-3)
```json
{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 600.00,
  "boardingFee": 350.00,
  "developmentLevy": 75.00,
  "examFee": 40.00,
  "otherFees": 25.00
}
```

### Form 4 (O-Level Year)
```json
{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 650.00,
  "boardingFee": 350.00,
  "developmentLevy": 75.00,
  "examFee": 150.00,  // ZIMSEC O-Level registration
  "otherFees": 30.00
}
```

### Form 6 (A-Level Year)
```json
{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 700.00,
  "boardingFee": 400.00,
  "developmentLevy": 100.00,
  "examFee": 200.00,  // ZIMSEC A-Level registration
  "otherFees": 50.00
}
```

---

## Common Workflows

### Start of Term 1

**Step 1:** Set fees for primary school
```bash
POST /api/fee-records/bulk/grades
{
  "grades": ["Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Grade 6", "Grade 7"],
  "termYear": "2025-Term1",
  "feeCategory": "Day Scholar",
  "tuitionFee": 400.00,
  ...
}
```

**Step 2:** Set fees for lower secondary (Form 1-3)
```bash
POST /api/fee-records/bulk/forms
{
  "grades": ["Form 1", "Form 2", "Form 3"],
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 600.00,
  ...
}
```

**Step 3:** Set special fees for Form 4 (O-Level)
```bash
POST /api/fee-records/bulk/grade/Form 4
{
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 650.00,
  "examFee": 150.00,
  ...
}
```

**Step 4:** Set special fees for Form 5-6 (A-Level)
```bash
POST /api/fee-records/bulk/forms
{
  "grades": ["Form 5", "Form 6"],
  "termYear": "2025-Term1",
  "feeCategory": "Boarder",
  "tuitionFee": 700.00,
  "examFee": 200.00,
  ...
}
```

### Adjusting Fees for Specific Streams

If you have different streams with different fee structures:

```bash
# Top stream (A classes) - might have lower fees due to scholarships
POST /api/fee-records/bulk/classes
Manually call /bulk/class/5A, /bulk/class/6A, etc.

# Or use the combined endpoint for each:
POST /api/fee-records/bulk/grade/Grade 5/class/5A
POST /api/fee-records/bulk/grade/Grade 6/class/6A
```

---

## Student Data Structure

When uploading students via Excel or API, ensure:

```json
{
  "studentId": "2025001",
  "firstName": "Tatenda",
  "lastName": "Moyo",
  "grade": "Form 3",        // "Grade 1" to "Grade 7", "Form 1" to "Form 6"
  "className": "Form 3B",   // Specific class/stream
  "dateOfBirth": "2010-05-15",
  "gender": "MALE",
  ...
}
```

**Important:** 
- `grade` = Level (Grade 1-7, Form 1-6)
- `className` = Specific class/stream (5A, Form 3B)
- Both should be consistent for proper fee assignment

---

## Query Students

### Get all students in a grade
```bash
GET /api/students/grade/Grade 5
GET /api/students/grade/Form 3
```

### Get all students in a specific class
```bash
GET /api/students  # Filter manually by className
# Or use: GET /api/students?className=5A (if endpoint exists)
```

### Count students by grade
```bash
# Use the repository method countByGrade
# Returns count of active students
```

---

## Fee Categories

### Boarder
- Full tuition + boarding fees
- Accommodation and meals included
- Higher total fees

### Day Scholar
- Tuition fees only
- No boarding fees (boardingFee = 0.00)
- Students live at home

### Special Categories (Can Add More)
- Scholarship
- Staff Child
- Sibling Discount
- Bursary

---

## Term Structure

Zimbabwe typically has 3 terms:

- **Term 1:** January - April
- **Term 2:** May - August  
- **Term 3:** September - December

**Term naming convention:**
- "2025-Term1"
- "2025-Term2"
- "2025-Term3"

Each term requires separate fee records:
```bash
# Set Term 1 fees
POST /api/fee-records/bulk/grade/Form 3
{ "termYear": "2025-Term1", ... }

# Set Term 2 fees (later in year)
POST /api/fee-records/bulk/grade/Form 3
{ "termYear": "2025-Term2", ... }
```

---

## API Response Example

```json
{
  "message": "Fees assigned successfully",
  "affectedGrades": "Form 3",
  "studentsAffected": 45,
  "feeRecords": [
    {
      "id": 1,
      "termYear": "2025-Term1",
      "feeCategory": "Boarder",
      "tuitionFee": 600.00,
      "boardingFee": 350.00,
      "developmentLevy": 75.00,
      "examFee": 40.00,
      "otherFees": 25.00,
      "grossAmount": 1090.00,
      "netAmount": 1090.00,
      "outstandingBalance": 1090.00,
      "paymentStatus": "ARREARS",
      "student": {
        "id": 1,
        "studentId": "2025001",
        "firstName": "Tatenda",
        "lastName": "Moyo",
        "grade": "Form 3",
        "className": "Form 3B"
      }
    }
    // ... 44 more students
  ]
}
```

---

## Tips

1. **Start with grades, then refine by class** if needed
2. **Set examination fees higher** for Form 4 (O-Level) and Form 6 (A-Level)
3. **Use consistent naming**: "Grade 1" not "Gr1", "Form 3" not "F3"
4. **Test with one class first** before applying school-wide
5. **Check student counts** before bulk assignment to avoid surprises

## Need Help?

Check the main guide: `BULK_FEE_ASSIGNMENT_GUIDE.md`
