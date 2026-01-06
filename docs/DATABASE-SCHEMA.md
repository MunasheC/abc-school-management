# Database Schema Documentation

## Overview

The School Management System uses MySQL as its database. The schema supports:
- Multi-tenancy (school isolation)
- Student enrollment and academic progression
- Fee management and payment tracking
- Audit trail for all changes
- Automated year-end promotions

---

## Entity Relationship Diagram

```
┌─────────────┐
│   School    │
└──────┬──────┘
       │ 1
       │
       │ *
┌──────┴───────────┬───────────────┬──────────────┬──────────────────┐
│                  │               │              │                  │
┌──────▼──────┐  ┌─▼────────────┐ ┌▼─────────┐  ┌▼─────────────────┐
│   Student   │  │ AcademicYear │ │ Guardian │  │   AuditTrail     │
└──────┬──────┘  │    Config    │ └──────────┘  └──────────────────┘
       │ 1       └──────────────┘
       │
       │ *
┌──────┴──────────────┐
│ StudentFeeRecord    │
└──────┬──────────────┘
       │ 1
       │
       │ *
┌──────▼──────┐
│   Payment   │
└─────────────┘
```

---

## Table Definitions

### schools

**Purpose:** Multi-tenant school configuration

```sql
CREATE TABLE schools (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_code VARCHAR(50) UNIQUE NOT NULL,
    school_name VARCHAR(200) NOT NULL,
    school_type VARCHAR(50),                    -- PRIMARY, SECONDARY, COMBINED
    
    -- Contact Information
    address VARCHAR(500),
    city VARCHAR(100),
    province VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'Zimbabwe',
    primary_phone VARCHAR(20) NOT NULL,
    secondary_phone VARCHAR(20),
    email VARCHAR(100),
    website VARCHAR(200),
    
    -- Administration
    head_teacher_name VARCHAR(100),
    bursar_name VARCHAR(100),
    bursar_phone VARCHAR(20),
    bursar_email VARCHAR(100),
    
    -- Registration
    ministry_registration_number VARCHAR(100),
    license_expiry_date DATE,
    zimsec_center_number VARCHAR(50),
    
    -- Banking
    bank_account_number VARCHAR(50),
    bank_branch VARCHAR(100),
    bank_account_name VARCHAR(100),
    relationship_manager VARCHAR(100),
    relationship_manager_phone VARCHAR(20),
    
    -- Subscription
    subscription_tier VARCHAR(50),              -- FREE, BASIC, PREMIUM
    onboarding_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    deactivation_date DATETIME,
    deactivation_reason VARCHAR(500),
    
    -- Configuration
    logo_url VARCHAR(500),
    primary_color VARCHAR(20),
    max_students INT,
    current_student_count INT DEFAULT 0,
    
    -- Metadata
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_school_code (school_code),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### students

**Purpose:** Core student personal and academic information

```sql
CREATE TABLE students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    student_id VARCHAR(50) NOT NULL,            -- School's student reference
    
    -- Personal Information
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    middle_name VARCHAR(50),
    date_of_birth DATE,
    gender VARCHAR(20),                         -- MALE, FEMALE, OTHER
    national_id VARCHAR(50),
    
    -- Academic Information
    grade VARCHAR(50),
    enrollment_date DATE,
    admission_number VARCHAR(50),
    class_name VARCHAR(50),
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    completion_status VARCHAR(50),              -- COMPLETED_PRIMARY, COMPLETED_O_LEVEL, COMPLETED_A_LEVEL
    withdrawal_date DATE,
    withdrawal_reason VARCHAR(500),
    
    -- Metadata
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE,
    UNIQUE KEY uk_school_student (school_id, student_id),
    INDEX idx_school_grade (school_id, grade),
    INDEX idx_national_id (national_id),
    INDEX idx_is_active (is_active),
    INDEX idx_completion_status (completion_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### student_fee_records

**Purpose:** Financial records per student per term/year (historical tracking)

```sql
CREATE TABLE student_fee_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    school_id BIGINT NOT NULL,
    
    -- Term/Year
    term_year VARCHAR(100),                     -- "Term 1 2026", "2026 Academic Year"
    fee_category VARCHAR(50),                   -- Day Scholar, Boarder
    
    -- Fee Components
    tuition_fee DECIMAL(10, 2) DEFAULT 0.00,
    boarding_fee DECIMAL(10, 2) DEFAULT 0.00,
    development_levy DECIMAL(10, 2) DEFAULT 0.00,
    exam_fee DECIMAL(10, 2) DEFAULT 0.00,
    other_fees DECIMAL(10, 2) DEFAULT 0.00,
    
    -- Discounts
    scholarship_amount DECIMAL(10, 2) DEFAULT 0.00,
    sibling_discount DECIMAL(10, 2) DEFAULT 0.00,
    
    -- Calculated Amounts
    gross_amount DECIMAL(10, 2) DEFAULT 0.00,  -- Sum of all fees
    total_discounts DECIMAL(10, 2) DEFAULT 0.00,
    net_amount DECIMAL(10, 2) DEFAULT 0.00,     -- Gross - Discounts
    previous_balance DECIMAL(10, 2) DEFAULT 0.00,
    total_amount DECIMAL(10, 2) DEFAULT 0.00,   -- Net + Previous Balance
    amount_paid DECIMAL(10, 2) DEFAULT 0.00,
    outstanding_balance DECIMAL(10, 2) DEFAULT 0.00,
    
    -- Status
    payment_status VARCHAR(50),                 -- PAID, PARTIALLY_PAID, ARREARS
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE,
    INDEX idx_student_term (student_id, term_year),
    INDEX idx_school_term (school_id, term_year),
    INDEX idx_payment_status (payment_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### payments

**Purpose:** Individual payment transaction records

```sql
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    fee_record_id BIGINT NOT NULL,
    school_id BIGINT NOT NULL,
    
    -- Payment Details
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,        -- CASH, CHECK, BANK_TRANSFER, MOBILE_MONEY
    payment_date DATETIME NOT NULL,
    receipt_number VARCHAR(100),
    
    -- Bank Integration
    bank_transaction_id VARCHAR(100),
    transaction_reference VARCHAR(100),
    parent_account_number VARCHAR(50),
    teller_name VARCHAR(100),
    branch_name VARCHAR(100),
    bank_processed_time DATETIME,
    
    -- Metadata
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (fee_record_id) REFERENCES student_fee_records(id) ON DELETE CASCADE,
    FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE,
    INDEX idx_student (student_id),
    INDEX idx_fee_record (fee_record_id),
    INDEX idx_payment_date (payment_date),
    INDEX idx_payment_method (payment_method),
    INDEX idx_bank_transaction (bank_transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### academic_year_configs

**Purpose:** Automated year-end promotion configuration

```sql
CREATE TABLE academic_year_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    
    -- Configuration
    academic_year VARCHAR(50) NOT NULL,
    end_of_year_date DATE NOT NULL,
    next_academic_year VARCHAR(50),
    carry_forward_balances BOOLEAN DEFAULT TRUE,
    
    -- Promotion Status
    promotion_status VARCHAR(20) DEFAULT 'SCHEDULED',  -- SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, FAILED
    promotion_executed_at DATETIME,
    students_promoted INT DEFAULT 0,
    students_completed INT DEFAULT 0,
    promotion_errors INT DEFAULT 0,
    
    -- Fee Structures (JSON)
    fee_structures TEXT,                        -- JSON: {"Form 2": {...}, "Form 3": {...}}
    default_fee_structure TEXT,                 -- JSON: {...}
    
    -- Metadata
    notes TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    
    FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE,
    UNIQUE KEY uk_school_academic_year (school_id, academic_year),
    INDEX idx_end_of_year_date (end_of_year_date),
    INDEX idx_promotion_status (promotion_status),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### guardians

**Purpose:** Parent/guardian information (shared across siblings)

```sql
CREATE TABLE guardians (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    
    -- Guardian Information
    guardian_name VARCHAR(100) NOT NULL,
    relationship VARCHAR(50),                   -- Father, Mother, Guardian
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    national_id VARCHAR(50),
    occupation VARCHAR(100),
    employer VARCHAR(200),
    
    -- Address
    address VARCHAR(500),
    city VARCHAR(100),
    province VARCHAR(100),
    
    -- Emergency Contact
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    emergency_contact_relationship VARCHAR(50),
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE,
    INDEX idx_phone_number (phone_number),
    INDEX idx_school (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### student_guardians

**Purpose:** Many-to-many relationship between students and guardians

```sql
CREATE TABLE student_guardians (
    student_id BIGINT NOT NULL,
    guardian_id BIGINT NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    
    PRIMARY KEY (student_id, guardian_id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (guardian_id) REFERENCES guardians(id) ON DELETE CASCADE,
    INDEX idx_student (student_id),
    INDEX idx_guardian (guardian_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### audit_trail

**Purpose:** Track all changes to the system

```sql
CREATE TABLE audit_trail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT,
    
    -- User Information
    user_id VARCHAR(100),
    user_name VARCHAR(100),
    
    -- Action Details
    action VARCHAR(100) NOT NULL,               -- CREATE_STUDENT, UPDATE_STUDENT, PROMOTE_STUDENT, etc.
    entity_type VARCHAR(100),                   -- Student, Payment, FeeRecord
    entity_id VARCHAR(100),
    description TEXT,
    
    -- Change Tracking
    before_value TEXT,
    after_value TEXT,
    
    -- Metadata
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE,
    INDEX idx_school (school_id),
    INDEX idx_action (action),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## Common Queries

### Get all students in a grade
```sql
SELECT s.* 
FROM students s
WHERE s.school_id = ? 
  AND s.grade = ?
  AND s.is_active = TRUE
ORDER BY s.last_name, s.first_name;
```

### Get students with outstanding fees
```sql
SELECT s.student_id, s.first_name, s.last_name, f.outstanding_balance
FROM students s
JOIN student_fee_records f ON s.id = f.student_id
WHERE s.school_id = ?
  AND f.outstanding_balance > 0
  AND s.is_active = TRUE
ORDER BY f.outstanding_balance DESC;
```

### Get total outstanding by grade
```sql
SELECT s.grade, SUM(f.outstanding_balance) as total_outstanding, COUNT(DISTINCT s.id) as student_count
FROM students s
JOIN student_fee_records f ON s.id = f.student_id
WHERE s.school_id = ?
  AND s.is_active = TRUE
GROUP BY s.grade
ORDER BY s.grade;
```

### Get payment history for a student
```sql
SELECT p.payment_date, p.amount, p.payment_method, p.receipt_number
FROM payments p
WHERE p.student_id = ?
ORDER BY p.payment_date DESC;
```

### Get all active promotions due today
```sql
SELECT * 
FROM academic_year_configs
WHERE is_active = TRUE
  AND promotion_status = 'SCHEDULED'
  AND end_of_year_date <= CURDATE();
```

---

## Indexes

**Performance Optimization:**

Key indexes for common queries:
- `students.uk_school_student` - Unique constraint + fast lookup
- `students.idx_school_grade` - Grade queries
- `student_fee_records.idx_student_term` - Fee record history
- `payments.idx_payment_date` - Date range queries
- `audit_trail.idx_created_at` - Audit log queries

---

## Data Migration Scripts

### Add new school
```sql
INSERT INTO schools (school_code, school_name, school_type, primary_phone, is_active)
VALUES ('TEST001', 'Test High School', 'SECONDARY', '+263771234567', TRUE);
```

### Create student with fee record
```sql
-- Insert student
INSERT INTO students (school_id, student_id, first_name, last_name, grade, is_active)
VALUES (1, 'STU2025001', 'John', 'Doe', 'Form 1', TRUE);

-- Insert fee record
INSERT INTO student_fee_records (
    student_id, school_id, term_year, tuition_fee, 
    gross_amount, net_amount, total_amount, outstanding_balance, payment_status
)
VALUES (
    LAST_INSERT_ID(), 1, 'Term 1 2026', 600.00,
    600.00, 600.00, 600.00, 600.00, 'ARREARS'
);
```

---

## Backup & Maintenance

### Daily Backup
```bash
mysqldump -u root -p school_management_db > backup_$(date +%Y%m%d).sql
```

### Restore from Backup
```bash
mysql -u root -p school_management_db < backup_20260106.sql
```

### Check Table Sizes
```sql
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb
FROM information_schema.tables
WHERE table_schema = 'school_management_db'
ORDER BY (data_length + index_length) DESC;
```

### Optimize Tables
```sql
OPTIMIZE TABLE students;
OPTIMIZE TABLE student_fee_records;
OPTIMIZE TABLE payments;
```
