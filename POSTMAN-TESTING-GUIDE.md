# Testing School Management System with Postman

## Setup Instructions

### 1. Import the Collection
1. Open Postman
2. Click **Import** button (top left)
3. Select the file: `School-Management-System-Postman-Collection.json`
4. Collection will appear in your Collections sidebar

### 2. Verify Application is Running
- Make sure Spring Boot application is running on `http://localhost:8080`
- Check terminal shows: "✅ School Management System Started Successfully!"

### 3. Collection Variables
The collection uses these variables (auto-managed):
- `baseUrl` = http://localhost:8080
- `schoolId` = Auto-set after creating school
- `studentRef` = Auto-set after creating student
- `studentId` = Auto-set after creating student

---

## Test Execution Order

### Run Tests Sequentially (Recommended)

**Method 1: Run Entire Collection**
1. Right-click on collection name
2. Select **Run collection**
3. Click **Run School Management System**
4. View results with pass/fail status

**Method 2: Run Individual Journeys**

#### JOURNEY 1: Bank Onboards School
```
POST /api/bank/admin/schools - Create new school
GET  /api/bank/admin/schools - View all schools
```
✅ **Expected**: School created with ID, status 201

#### JOURNEY 2: School Creates Student
```
POST /api/school/students - Create student (with X-School-Id header)
GET  /api/school/students - View all students for school
```
✅ **Expected**: Student created with reference like "CHHS001-STU-001"

#### JOURNEY 3: Bank Counter Payment
```
POST /api/bank/lookup - Lookup student by reference
POST /api/bank/payment/counter - Process bank counter payment
```
✅ **Expected**: Payment processed, balance reduced by $200

#### JOURNEY 4: Mobile Payment
```
POST /api/bank/payment/digital - Process mobile banking payment
```
✅ **Expected**: Payment processed, balance reduced by $150

#### JOURNEY 5: School Views Collections
```
GET /api/school/payments/today - Get today's payments
GET /api/school/payments/statistics - Get payment statistics
```
✅ **Expected**: Returns payments made today, shows total collected

#### JOURNEY 6: Data Isolation Test
```
GET /api/school/students (X-School-Id: 1) - School 1 students
GET /api/school/students (X-School-Id: 2) - School 2 students
GET /api/school/students (no header) - Should fail with 401
```
✅ **Expected**: Different results for each school, 401 without header

#### JOURNEY 7: Bank Analytics Dashboard
```
GET /api/bank/admin/analytics - Cross-school analytics
GET /api/bank/admin/analytics/revenue-trend - 30-day revenue trend
GET /api/bank/admin/schools/{id} - Detailed school info
```
✅ **Expected**: Shows total schools, students, revenue across all schools

#### JOURNEY 8: Additional Tests
```
GET /api/school/students/search?name=Alice - Search students
GET /api/school/students/{id}/fees - Get fee record
GET /api/school/guardians - Get guardians
```
✅ **Expected**: Search results, fee details, guardian list

---

## Understanding Test Results

### Automated Tests
Each request includes test scripts that verify:
- ✅ Correct HTTP status code
- ✅ Response structure
- ✅ Required fields present
- ✅ Data types correct

### Console Output
Check **Postman Console** (bottom) for:
- Student reference numbers
- Payment amounts
- School counts
- Revenue totals

### Collection Variables
After running Journey 1-2, check collection variables:
```
schoolId = 1 (or auto-generated ID)
studentRef = CHHS001-STU-001
studentId = 1 (or auto-generated ID)
```

---

## Key Features Demonstrated

### 1. Multi-Tenant Data Isolation
- Journey 6 proves School 1 cannot see School 2 data
- `X-School-Id` header required for school endpoints
- Bank endpoints access all schools

### 2. Bank Payment Integration
- Counter payments (Journey 3): Bank teller at branch
- Digital payments (Journey 4): Mobile/internet banking
- Automatic balance updates in student fee records

### 3. Cross-School Analytics
- Journey 7: Bank sees aggregated data across all schools
- Revenue trends, top schools, payment methods
- Bank channel adoption rate

### 4. Automatic Context Management
- `X-School-Id` header sets SchoolContext
- Service layer auto-filters by current school
- Guardian reuse within same school

---

## Troubleshooting

### 401 Unauthorized on /api/school/* endpoints
**Cause**: Missing `X-School-Id` header
**Fix**: Add header with school ID (e.g., "1")

### 404 Not Found
**Cause**: Student reference or ID doesn't exist
**Fix**: Run Journey 1-2 first to create school and student

### Empty Arrays Returned
**Cause**: No data for that school yet
**Fix**: Create students first using Journey 2

### Foreign Key Constraint Error
**Cause**: Database schema issue
**Fix**: Run database migrations or restart with `spring.jpa.hibernate.ddl-auto=update`

### Connection Refused
**Cause**: Spring Boot app not running
**Fix**: Start app with `mvn spring-boot:run` in terminal

---

## Expected Test Results Summary

| Journey | Requests | Expected Pass | Key Validation |
|---------|----------|---------------|----------------|
| 1 | 2 | 2/2 | School created with ID |
| 2 | 2 | 2/2 | Student created with reference |
| 3 | 2 | 2/2 | Payment $200, balance reduced |
| 4 | 1 | 1/1 | Payment $150, balance reduced |
| 5 | 2 | 2/2 | Today's payments listed |
| 6 | 3 | 3/3 | Data isolation verified, 401 without context |
| 7 | 3 | 3/3 | Analytics show all schools |
| 8 | 3 | 3/3 | Search, fees, guardians work |
| **Total** | **18** | **18/18** | **100% Pass Rate** |

---

## Advanced Usage

### Run Collection via Newman (CLI)
```bash
npm install -g newman
newman run School-Management-System-Postman-Collection.json
```

### Generate HTML Report
```bash
newman run School-Management-System-Postman-Collection.json -r html
```

### Run with Different Environments
Create environment files for:
- **Local**: http://localhost:8080
- **Dev**: http://dev-server:8080
- **Prod**: https://api.schoolmanagement.co.zw

---

## Next Steps After Testing

1. ✅ All tests passing → System ready for deployment
2. ❌ Some tests failing → Review error messages, fix issues
3. 🔧 Add authentication → Protect endpoints with JWT/OAuth
4. 📧 Add notifications → Email receipts, SMS alerts
5. 📊 Add reporting → PDF receipts, Excel exports

---

## Support

**Issues?**
1. Check application logs in terminal
2. Verify database connection
3. Ensure all migrations ran successfully
4. Check Postman Console for detailed errors

**Success?**
All 18 requests passing = Multi-tenant SaaS platform fully operational! 🎉
