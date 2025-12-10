# 🏗️ Entity Refactoring Guide - Better Database Design

## Why We Refactored

### ❌ BEFORE: Monolithic Entity (Poor Design)
The original `Student` entity had **30+ fields** mixing:
- Personal information (name, date of birth)
- Parent information (name, phone, email, address)
- Financial information (fees, payments, balances)

**Problems:**
1. **Tight Coupling** - Changes to fee structure require changing Student entity
2. **Data Duplication** - Siblings each store same parent info separately
3. **Poor Performance** - Loading student always loads all financial data
4. **No History** - Can't track payments over time or fees across terms
5. **Violates SRP** - Single Responsibility Principle

### ✅ AFTER: Normalized Design (Good Design)
Now split into **4 focused entities**:

```
Student (Personal & Academic Info)
    ├── belongs to → Guardian (Parent/Guardian Info)
    ├── has one → StudentFeeRecord (Current Financial Info)
    └── has many → Payment (Transaction History)
```

## New Entity Structure

### 1. Student Entity (Core Information)
**Purpose:** Personal and academic information only

**Fields:**
- `id` - Database primary key
- `studentId` - School-generated unique ID (STU123456)
- `firstName`, `lastName`, `middleName`
- `dateOfBirth`, `gender`, `nationalId`
- `grade`, `className`, `enrollmentDate`, `admissionNumber`
- `isActive`, `withdrawalDate`, `withdrawalReason`
- `notes` - General notes about student

**Relationships:**
```java
@ManyToOne Guardian guardian           // Many students → one parent (siblings)
@OneToOne StudentFeeRecord currentFeeRecord  // One student → one current fee record
@OneToMany List<Payment> payments      // One student → many payments
```

**Helper Methods:**
- `getFullName()` - Returns "FirstName MiddleName LastName"
- `getAge()` - Calculates age from date of birth
- `hasOutstandingBalance()` - Checks if owes money
- `getOutstandingBalance()` - Gets amount owed
- `getPaymentStatus()` - Gets PAID/PARTIALLY_PAID/ARREARS
- `withdraw(reason)` - Mark student as withdrawn
- `reactivate()` - Reactivate withdrawn student

---

### 2. Guardian Entity (Parent/Guardian Information)
**Purpose:** Store parent/guardian contact and employment info

**Why Separate?**
- **Shared by Siblings** - Multiple students can have same guardian
- **Independent Updates** - Parent phone changes don't affect student record
- **Bank Integration** - One parent account pays for all their children

**Fields:**
- `id` - Database primary key
- `fullName` - Parent/guardian full name
- `primaryPhone` - **UNIQUE** contact number
- `secondaryPhone` - Alternative contact
- `email` - Email address
- `address` - Residential address
- `occupation`, `employer` - Employment details
- `isActive` - Active/inactive status

**Relationships:**
```java
@OneToMany List<Student> students  // One guardian → many students (children)
```

**Helper Methods:**
- `addStudent(student)` - Add a child and set bidirectional relationship
- `getChildrenCount()` - Count number of children

---

### 3. StudentFeeRecord Entity (Financial Information)
**Purpose:** Fee structure, discounts, balances for current term/year

**Why Separate?**
- **Financial Isolation** - Bursar changes fees without touching student data
- **Term-Based** - Can create one record per term/year
- **Automatic Calculations** - Fees and balances computed automatically

**Fields:**

**Fee Components:**
- `tuitionFee`, `boardingFee`, `developmentLevy`, `examFee`, `otherFees`

**Discounts:**
- `hasScholarship`, `scholarshipAmount`
- `siblingDiscount`, `earlyPaymentDiscount`

**Calculated Totals:**
- `grossAmount` - Total before discounts
- `netAmount` - Total after discounts (what student owes)
- `previousBalance` - Carried over from last term
- `amountPaid` - Total paid so far
- `outstandingBalance` - Remaining balance
- `paymentStatus` - PAID, PARTIALLY_PAID, ARREARS

**Other:**
- `termYear` - "Term 1 2024", "2024 Academic Year"
- `feeCategory` - "Day Scholar", "Boarder", etc.
- `bursarNotes` - Bursar's notes

**Relationships:**
```java
@OneToOne Student student  // One fee record → one student
```

**Key Methods:**
- `calculateTotals()` - **@PrePersist/@PreUpdate** - Auto-calculates all amounts
- `addPayment(amount)` - Record a payment and recalculate
- `isFullyPaid()` - Check if balance is zero
- `getPaymentPercentage()` - Calculate % of bill paid

---

### 4. Payment Entity (Transaction History)
**Purpose:** Individual payment records for audit trail

**Why Separate?**
- **Audit Trail** - Track who paid what, when
- **Bank Integration** - Link to bank transaction references
- **Reversals** - Can reverse/refund individual payments
- **Receipts** - Generate receipt for each payment

**Fields:**
- `id` - Database primary key
- `paymentReference` - Unique reference (PAY1234567890)
- `amount` - Payment amount
- `paymentMethod` - CASH, MOBILE_MONEY, BANK_TRANSFER, CHEQUE, CARD
- `transactionReference` - Bank/mobile money reference
- `receivedBy` - Name of bursar who received payment
- `paymentNotes` - Notes about this payment
- `status` - COMPLETED, PENDING, REVERSED, FAILED
- `paymentDate` - When payment was made
- `isReversed`, `reversalReason`, `reversedAt` - Reversal tracking

**Relationships:**
```java
@ManyToOne Student student              // Many payments → one student
@ManyToOne StudentFeeRecord feeRecord   // Many payments → one fee record (optional)
```

**Key Methods:**
- `generatePaymentReference()` - **@PrePersist** - Auto-generate reference
- `reverse(reason)` - Reverse this payment
- `isActive()` - Check if not reversed and completed

## Relationship Diagram

```
┌─────────────┐
│  Guardian   │ (Parent/Guardian)
│             │
│ - fullName  │
│ - phone     │◄──────┐
│ - email     │       │
│ - address   │       │ @ManyToOne
└─────────────┘       │ (Many students have one guardian)
                      │
                      │
┌─────────────────────┼────────────────────┐
│     Student         │                    │
│                     │                    │
│ - studentId         │                    │
│ - firstName         │                    │
│ - lastName          │                    │
│ - dateOfBirth       │                    │
│ - grade             │                    │
└──────┬──────────────┼────────────────────┘
       │              │
       │ @OneToOne    │ @OneToMany
       │              │ (One student has many payments)
       ▼              ▼
┌─────────────┐  ┌──────────────┐
│ FeeRecord   │  │   Payment    │
│             │  │              │
│ - tuition   │  │ - amount     │
│ - fees      │  │ - date       │
│ - balance   │  │ - method     │
│ - status    │  │ - reference  │
└─────────────┘  └──────────────┘
```

## Key Database Concepts

### 1. **@OneToMany** - One-to-Many Relationship
```java
// One Guardian has Many Students (siblings)
@OneToMany
private List<Student> students;
```
- Parent table: `guardians`
- Child table: `students` (has `guardian_id` foreign key)
- Example: One parent (Grace Moyo) has 3 children

### 2. **@ManyToOne** - Many-to-One Relationship
```java
// Many Students belong to One Guardian
@ManyToOne
private Guardian guardian;
```
- This is the opposite side of @OneToMany
- Creates foreign key in students table

### 3. **@OneToOne** - One-to-One Relationship
```java
// One Student has One FeeRecord (current)
@OneToOne
private StudentFeeRecord currentFeeRecord;
```
- Each student has exactly one current fee record
- Could be extended to @OneToMany for historical records

### 4. **Cascade Types**
```java
@OneToMany(cascade = CascadeType.ALL)
```
- `CascadeType.ALL` - Save/delete operations cascade to children
- `CascadeType.PERSIST` - Only save operations cascade
- Example: Saving student automatically saves fee record

### 5. **Fetch Types**
```java
@ManyToOne(fetch = FetchType.LAZY)
```
- `FetchType.LAZY` - Data loaded only when accessed (better performance)
- `FetchType.EAGER` - Data loaded immediately (can be slower)

### 6. **orphanRemoval**
```java
@OneToMany(orphanRemoval = true)
```
- When relationship broken, delete the orphan
- Example: Removing payment from student deletes the payment record

## Benefits of This Design

### 1. **Single Responsibility Principle (SRP)**
- Each entity has ONE reason to change
- Student entity only changes for student info updates
- Fee structure changes only affect StudentFeeRecord

### 2. **Data Integrity**
- Siblings share one Guardian record - update once, affects all
- No duplicate parent information
- Referential integrity enforced by database

### 3. **Performance**
```java
// Load only student info (fast)
Student student = studentRepository.findById(1);

// Load student with guardian (one join)
Student student = studentRepository.findByIdWithGuardian(1);

// Load student with all payments (separate query, lazy)
List<Payment> payments = student.getPayments();
```

### 4. **Historical Tracking**
- Keep all payments forever - complete audit trail
- Can add multiple fee records per student (one per term)
- Can query: "Show all payments in January 2024"

### 5. **Flexibility**
- Easy to add new fee types without changing Student
- Easy to add payment methods
- Easy to generate reports per term/year

### 6. **Bank Integration Ready**
```java
// Find all students whose parent has this phone number
List<Student> siblings = studentRepository.findByGuardianPrimaryPhone("+263771234567");

// Record payment from mobile money
Payment payment = new Payment();
payment.setAmount(500.00);
payment.setPaymentMethod("MOBILE_MONEY");
payment.setTransactionReference("MM123456");
payment.setStudent(student);
paymentRepository.save(payment);
```

## Migration Path

**Current Code Status:**
- ✅ New entities created (Student, Guardian, StudentFeeRecord, Payment)
- ⏳ **Need to update:** Repositories, Services, Controllers
- ⏳ **Need to create:** Repositories for new entities
- ⏳ **Need to update:** Excel upload to handle relationships
- ⏳ **Database migration:** Run application to auto-create new tables

**Next Steps:**
1. Create repositories for Guardian, StudentFeeRecord, Payment
2. Update StudentService to work with relationships
3. Update Controllers to expose new entities
4. Update Excel upload to create related entities
5. Test with sample data

## Learning Resources

**Key Concepts Covered:**
- ✅ Entity relationships (@OneToMany, @ManyToOne, @OneToOne)
- ✅ Fetch strategies (LAZY vs EAGER)
- ✅ Cascade operations
- ✅ Bidirectional relationships
- ✅ Database normalization
- ✅ Single Responsibility Principle
- ✅ Separation of Concerns

**Further Reading:**
- JPA/Hibernate relationship mappings
- Database normalization (1NF, 2NF, 3NF)
- SOLID principles in software design
