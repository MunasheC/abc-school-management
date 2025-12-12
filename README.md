# 🏫 School Management System

A Spring Boot application for managing school student data, designed as a partnership tool for banking services.

## 📋 Features

### Student Management
- ✅ Create, Read, Update, Delete students
- ✅ Search students by name, grade, parent phone
- ✅ Activate/deactivate student records
- ✅ Auto-generate student IDs
- ✅ Audit trail (created/updated timestamps)

### Financial Management (Bursar Features) 💰
- ✅ Track tuition, boarding, development levy, exam fees
- ✅ Automatic payment status calculation (PAID/PARTIALLY_PAID/ARREARS)
- ✅ Scholarship and sibling discount management
- ✅ Outstanding balance tracking with previous term balances
- ✅ Payment recording and history
- ✅ Financial reports (total outstanding, total collected)
- ✅ Query students by payment status and fee category

### Excel Upload 📤
- ✅ Bulk import students from Excel files (.xlsx, .xls)
- ✅ Support for 20 columns including student info, guardian info, and fees
- ✅ Multi-tenant support via X-School-ID header
- ✅ Automatic sibling detection (shared guardians via phone number)
- ✅ Auto-generates Student IDs if not provided
- ✅ Creates fee records automatically if fee data included
- ✅ Automatic validation and detailed error reporting
- ✅ Handles partial uploads (continues on errors)
- ✅ Detailed upload summary with success/failure counts

### Bank Integration Ready 🏦
- ✅ RESTful API for mobile apps and internet banking
- ✅ Payment recording endpoint for real-time updates
- ✅ Student lookup by parent phone (link to bank accounts)
- ✅ JSON responses for easy consumption
- ✅ MySQL database for reliability
- ✅ Transaction management for data consistency

## 🛠️ Technologies Used
- **Spring Boot 3.2.0** - Application framework
- **Spring Data JPA** - Database operations
- **MySQL** - Database
- **Apache POI** - Excel file processing
- **Maven** - Build tool
- **Lombok** - Reduce boilerplate code

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.9+
- MySQL 8.0+

### Running the Application
1. Update `src/main/resources/application.properties` with your MySQL password
2. Run: `mvn spring-boot:run`
3. Access at: `http://localhost:8080`

### Testing the API
- See [API-DOCUMENTATION.md](API-DOCUMENTATION.md) for complete API reference (all endpoints)
- See [STUDENT-ENROLLMENT-GUIDE.md](STUDENT-ENROLLMENT-GUIDE.md) for student enrollment overview
- See [BULK-UPLOAD-QUICK-START.md](BULK-UPLOAD-QUICK-START.md) for Excel bulk upload quick reference
- See [BULK-UPLOAD-GUIDE.md](BULK-UPLOAD-GUIDE.md) for detailed Excel upload documentation

### Quick Start: Create Sample Data
```powershell
# Generate sample Excel file
.\create_sample_excel.ps1

# Open the CSV in Excel and save as .xlsx
# Then upload via Postman to /api/students/upload-excel
```

## 📚 API Endpoints (Summary)

### Student CRUD
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/students` | POST | Create single student |
| `/api/students` | GET | Get all students |
| `/api/students/{id}` | GET | Get by ID |
| `/api/students/{id}` | PUT | Update student |
| `/api/students/{id}` | DELETE | Delete student |

### Bursar/Financial
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/students/payment-status/{status}` | GET | Get by payment status |
| `/api/students/outstanding-balance` | GET | Students with debt |
| `/api/students/scholarships` | GET | Scholarship students |
| `/api/students/{id}/payment` | POST | Record a payment |
| `/api/students/reports/total-outstanding` | GET | Total fees owed |
| `/api/students/reports/total-collected` | GET | Total fees collected |

### Bulk Operations
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/school/students/upload-excel` | POST | Bulk import from Excel (requires X-School-ID header) |

**See detailed documentation:**
- [API-DOCUMENTATION.md](API-DOCUMENTATION.md) - Complete API reference with all endpoints
- [STUDENT-ENROLLMENT-GUIDE.md](STUDENT-ENROLLMENT-GUIDE.md) - Overview of enrollment methods
- [BULK-UPLOAD-GUIDE.md](BULK-UPLOAD-GUIDE.md) - Comprehensive bulk upload guide

## 📖 Learning Resources
This project is built as a learning exercise to understand:
- Spring Boot fundamentals
- REST API development
- Database integration with JPA/Hibernate
- File processing with Apache POI
- Layered architecture (Entity → Repository → Service → Controller)
- DTO pattern for API responses
- Transaction management
- Batch processing
- Error handling strategies
