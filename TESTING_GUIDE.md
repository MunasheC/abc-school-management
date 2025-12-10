# 🎓 Testing Your Spring Boot REST API

Congratulations! You've built a complete REST API. Here's how to test it:

## 📋 Quick Testing Guide

### Method 1: Using Browser (Simple GET requests)
Open your browser and visit these URLs:

1. **Health Check:**
   ```
   http://localhost:8080/api/students/health
   ```
   Should return: `Student API is running! 🎓`

2. **Get All Students:**
   ```
   http://localhost:8080/api/students
   ```
   Returns JSON array of students (empty at first)

3. **Get Active Students:**
   ```
   http://localhost:8080/api/students/active
   ```

### Method 2: Using PowerShell

```powershell
# Health Check
Invoke-WebRequest -Uri "http://localhost:8080/api/students/health" | Select-Object -ExpandProperty Content

# Get all students
Invoke-RestMethod -Uri "http://localhost:8080/api/students" -Method Get

# Create a new student (POST request with JSON)
$student = @{
    firstName = "John"
    lastName = "Doe"
    dateOfBirth = "2010-05-15"
    grade = "Grade 5"
    parentName = "Jane Doe"
    parentPhone = "+263771234567"
    parentEmail = "jane.doe@email.com"
    address = "123 Main Street, Harare"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/students" -Method Post -Body $student -ContentType "application/json"
```

### Method 3: Using Postman (Recommended for Full Testing)

1. Download Postman from https://www.postman.com/downloads/
2. Create a new request
3. Try these endpoints:

#### CREATE a Student (POST)
- **URL:** `http://localhost:8080/api/students`
- **Method:** POST
- **Body** (select "raw" and "JSON"):
```json
{
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "2010-05-15",
    "grade": "Grade 5",
    "parentName": "Jane Doe",
    "parentPhone": "+263771234567",
    "parentEmail": "jane.doe@email.com",
    "address": "123 Main Street, Harare"
}
```

#### READ All Students (GET)
- **URL:** `http://localhost:8080/api/students`
- **Method:** GET

#### READ One Student (GET)
- **URL:** `http://localhost:8080/api/students/1`
- **Method:** GET

#### UPDATE a Student (PUT) - Partial Update Supported!
- **URL:** `http://localhost:8080/api/students/1`
- **Method:** PUT
- **Body:** (Only include fields you want to change)
```json
{
    "grade": "Grade 6",
    "address": "456 New Street, Harare"
}
```

#### DELETE a Student (DELETE)
- **URL:** `http://localhost:8080/api/students/1`
- **Method:** DELETE

#### SEARCH Students (GET)
- **URL:** `http://localhost:8080/api/students/search?name=John`
- **Method:** GET

## 📊 Complete Endpoint Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/students` | Create new student |
| POST | `/api/students/upload-excel` | **🆕 Bulk import from Excel file** |
| GET | `/api/students` | Get all students |
| GET | `/api/students/{id}` | Get student by ID |
| GET | `/api/students/student-id/{studentId}` | Get by student ID |
| GET | `/api/students/active` | Get active students |
| GET | `/api/students/grade/{grade}` | Get students by grade |
| GET | `/api/students/search?name={name}` | Search by name |
| GET | `/api/students/parent-phone/{phone}` | Get by parent phone |
| PUT | `/api/students/{id}` | Update student |
| DELETE | `/api/students/{id}` | Delete student |
| PATCH | `/api/students/{id}/deactivate` | Deactivate student |
| PATCH | `/api/students/{id}/reactivate` | Reactivate student |
| GET | `/api/students/stats/count-by-grade?grade={grade}` | Count by grade |
| GET | `/api/students/stats/active-count` | Count active students |

> **📤 NEW FEATURE:** Excel Upload - See [EXCEL_UPLOAD_GUIDE.md](EXCEL_UPLOAD_GUIDE.md) for detailed instructions on bulk importing students!

## 🎯 What You've Learned

1. **Full Stack Development:**
   - Database Layer (JPA Entity)
   - Data Access Layer (Repository)
   - Business Logic Layer (Service)
   - API Layer (Controller)

2. **REST Principles:**
   - HTTP Methods (GET, POST, PUT, DELETE, PATCH)
   - URL Design
   - Status Codes
   - JSON Request/Response
   - Partial Updates (sending only changed fields)

3. **Spring Boot Magic:**
   - Dependency Injection
   - Auto-configuration
   - Component Scanning
   - Transaction Management
   - Validation (`@Valid` for create, skipped for partial updates)

## 🚀 Next Steps

To start your application:
```bash
mvn spring-boot:run
```

Then test your endpoints using any of the methods above!

## 💡 Pro Tips

- Check the console logs to see the SQL queries Hibernate generates
- Look for validation errors in the response when you send invalid data
- Try creating students without a studentId - it will auto-generate one!
- Use the search endpoint to find students by name
