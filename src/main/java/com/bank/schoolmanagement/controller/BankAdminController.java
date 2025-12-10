package com.bank.schoolmanagement.controller;

import com.bank.schoolmanagement.entity.Payment;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.entity.StudentFeeRecord;
import com.bank.schoolmanagement.repository.PaymentRepository;
import com.bank.schoolmanagement.repository.SchoolRepository;
import com.bank.schoolmanagement.repository.StudentFeeRecordRepository;
import com.bank.schoolmanagement.repository.StudentRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bank Admin Dashboard Controller
 * 
 * Purpose: Provides bank administrators with cross-school analytics and management capabilities.
 * 
 * This controller operates at the BANK level (not school level), allowing:
 * - View all onboarded schools
 * - Onboard new schools to the platform
 * - Track payment statistics across all schools
 * - Monitor revenue and transaction volumes
 * - School performance analytics
 * 
 * Security Note: These endpoints should be restricted to bank administrators only.
 * In production, add @PreAuthorize("hasRole('BANK_ADMIN')") annotations.
 * 
 * @author School Management System
 */
@Slf4j
@RestController
@RequestMapping("/api/bank/admin")
@RequiredArgsConstructor
public class BankAdminController {

    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final StudentFeeRecordRepository feeRecordRepository;
    private final PaymentRepository paymentRepository;

    /**
     * GET /api/bank/admin/schools
     * 
     * Get all onboarded schools with summary statistics.
     * 
     * Returns:
     * - School basic info (name, code, contact)
     * - Student count
     * - Total outstanding fees
     * - Total payments received
     * - Active status
     */
    @GetMapping("/schools")
    public ResponseEntity<List<SchoolSummary>> getAllSchools() {
        log.info("Bank admin: Getting all schools with statistics");
        
        List<School> schools = schoolRepository.findAll();
        List<SchoolSummary> summaries = schools.stream()
                .map(this::createSchoolSummary)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(summaries);
    }

    /**
     * GET /api/bank/admin/schools/{id}
     * 
     * Get detailed information for a specific school.
     * 
     * Includes:
     * - School details
     * - Student breakdown by grade
     * - Financial summary (fees, payments, outstanding)
     * - Payment method breakdown
     * - Recent payment activity
     */
    @GetMapping("/schools/{id}")
    public ResponseEntity<SchoolDetails> getSchoolDetails(@PathVariable Long id) {
        log.info("Bank admin: Getting details for school ID: {}", id);
        
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("School not found with ID: " + id));
        
        SchoolDetails details = new SchoolDetails();
        details.setSchool(school);
        
        // Student statistics
        long totalStudents = studentRepository.countBySchool(school);
        long activeStudents = studentRepository.countBySchoolAndIsActive(school, true);
        details.setTotalStudents(totalStudents);
        details.setActiveStudents(activeStudents);
        
        // Get students by grade
        List<Student> students = studentRepository.findBySchool(school);
        Map<String, Long> studentsByGrade = students.stream()
                .filter(s -> s.getGrade() != null)
                .collect(Collectors.groupingBy(Student::getGrade, Collectors.counting()));
        details.setStudentsByGrade(studentsByGrade);
        
        // Financial statistics
        List<StudentFeeRecord> feeRecords = feeRecordRepository.findBySchool(school);
        BigDecimal totalFeesAssigned = feeRecords.stream()
                .map(StudentFeeRecord::getGrossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOutstanding = feeRecords.stream()
                .map(StudentFeeRecord::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        details.setTotalFeesAssigned(totalFeesAssigned);
        details.setTotalOutstanding(totalOutstanding);
        
        // Payment statistics
        List<Payment> payments = paymentRepository.findBySchool(school);
        BigDecimal totalPaymentsReceived = payments.stream()
                .filter(p -> !p.getIsReversed())
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        details.setTotalPaymentsReceived(totalPaymentsReceived);
        details.setTotalPayments(payments.stream().filter(p -> !p.getIsReversed()).count());
        
        // Payment method breakdown
        Map<String, Long> paymentsByMethod = payments.stream()
                .filter(p -> !p.getIsReversed())
                .collect(Collectors.groupingBy(
                    p -> p.getPaymentMethod() != null ? p.getPaymentMethod().toString() : "UNKNOWN",
                    Collectors.counting()
                ));
        details.setPaymentsByMethod(paymentsByMethod);
        
        // Bank channel vs School channel breakdown
        long bankChannelPayments = payments.stream()
                .filter(p -> !p.getIsReversed() && p.isBankChannelPayment())
                .count();
        long schoolChannelPayments = payments.stream()
                .filter(p -> !p.getIsReversed() && !p.isBankChannelPayment())
                .count();
        
        details.setBankChannelPayments(bankChannelPayments);
        details.setSchoolChannelPayments(schoolChannelPayments);
        
        // Recent payments (last 10)
        List<Payment> recentPayments = payments.stream()
                .filter(p -> !p.getIsReversed())
                .sorted((p1, p2) -> p2.getPaymentDate().compareTo(p1.getPaymentDate()))
                .limit(10)
                .collect(Collectors.toList());
        details.setRecentPayments(recentPayments);
        
        return ResponseEntity.ok(details);
    }

    /**
     * POST /api/bank/admin/schools
     * 
     * Onboard a new school to the platform.
     * 
     * Request body:
     * {
     *   "schoolName": "St. Mary's High School",
     *   "schoolCode": "SCH001",
     *   "contactPerson": "John Doe",
     *   "contactPhone": "+263772123456",
     *   "contactEmail": "admin@stmarys.ac.zw",
     *   "address": "123 Education Road, Harare"
     * }
     */
    @PostMapping("/schools")
    public ResponseEntity<School> onboardSchool(@Valid @RequestBody School school) {
        log.info("Bank admin: Onboarding new school: {}", school.getSchoolName());
        
        // Validate school code uniqueness
        if (schoolRepository.findBySchoolCode(school.getSchoolCode()).isPresent()) {
            log.error("School code already exists: {}", school.getSchoolCode());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        // Set as active by default
        school.setIsActive(true);
        school.setOnboardingDate(LocalDate.now());
        
        School savedSchool = schoolRepository.save(school);
        log.info("School onboarded successfully: {} (ID: {})", savedSchool.getSchoolName(), savedSchool.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSchool);
    }

    /**
     * PUT /api/bank/admin/schools/{id}
     * 
     * Update school information.
     */
    @PutMapping("/schools/{id}")
    public ResponseEntity<School> updateSchool(@PathVariable Long id, @Valid @RequestBody School schoolUpdate) {
        log.info("Bank admin: Updating school ID: {}", id);
        
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("School not found with ID: " + id));
        
        // Update fields
        if (schoolUpdate.getSchoolName() != null) {
            school.setSchoolName(schoolUpdate.getSchoolName());
        }
        if (schoolUpdate.getHeadTeacherName() != null) {
            school.setHeadTeacherName(schoolUpdate.getHeadTeacherName());
        }
        if (schoolUpdate.getPrimaryPhone() != null) {
            school.setPrimaryPhone(schoolUpdate.getPrimaryPhone());
        }
        if (schoolUpdate.getEmail() != null) {
            school.setEmail(schoolUpdate.getEmail());
        }
        if (schoolUpdate.getAddress() != null) {
            school.setAddress(schoolUpdate.getAddress());
        }
        if (schoolUpdate.getIsActive() != null) {
            school.setIsActive(schoolUpdate.getIsActive());
        }
        
        School updatedSchool = schoolRepository.save(school);
        return ResponseEntity.ok(updatedSchool);
    }

    /**
     * GET /api/bank/admin/analytics
     * 
     * Get cross-school analytics and insights.
     * 
     * Returns:
     * - Total schools
     * - Total students across all schools
     * - Total fees collected (all time)
     * - Today's revenue
     * - This week's revenue
     * - This month's revenue
     * - Payment method distribution
     * - Top 5 schools by revenue
     * - Bank channel adoption rate
     */
    @GetMapping("/analytics")
    public ResponseEntity<BankAnalytics> getBankAnalytics() {
        log.info("Bank admin: Getting cross-school analytics");
        
        BankAnalytics analytics = new BankAnalytics();
        
        // School statistics
        long totalSchools = schoolRepository.count();
        long activeSchools = schoolRepository.countByIsActive(true);
        analytics.setTotalSchools(totalSchools);
        analytics.setActiveSchools(activeSchools);
        
        // Student statistics
        long totalStudents = studentRepository.count();
        long activeStudents = studentRepository.countByIsActive(true);
        analytics.setTotalStudents(totalStudents);
        analytics.setActiveStudents(activeStudents);
        
        // Payment statistics (all time)
        List<Payment> allPayments = paymentRepository.findAll();
        BigDecimal totalRevenue = allPayments.stream()
                .filter(p -> !p.getIsReversed())
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        analytics.setTotalRevenueAllTime(totalRevenue);
        analytics.setTotalPayments(allPayments.stream().filter(p -> !p.getIsReversed()).count());
        
        // Today's revenue
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        BigDecimal todayRevenue = allPayments.stream()
                .filter(p -> !p.getIsReversed())
                .filter(p -> p.getPaymentDate().isAfter(todayStart) && p.getPaymentDate().isBefore(todayEnd))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        analytics.setTodayRevenue(todayRevenue);
        
        // This week's revenue (last 7 days)
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        BigDecimal weekRevenue = allPayments.stream()
                .filter(p -> !p.getIsReversed())
                .filter(p -> p.getPaymentDate().isAfter(weekStart))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        analytics.setThisWeekRevenue(weekRevenue);
        
        // This month's revenue
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        BigDecimal monthRevenue = allPayments.stream()
                .filter(p -> !p.getIsReversed())
                .filter(p -> p.getPaymentDate().isAfter(monthStart))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        analytics.setThisMonthRevenue(monthRevenue);
        
        // Payment method distribution
        Map<String, Long> paymentMethodDistribution = allPayments.stream()
                .filter(p -> !p.getIsReversed())
                .collect(Collectors.groupingBy(
                    p -> p.getPaymentMethod() != null ? p.getPaymentMethod().toString() : "UNKNOWN",
                    Collectors.counting()
                ));
        analytics.setPaymentMethodDistribution(paymentMethodDistribution);
        
        // Bank channel adoption
        long bankChannelPayments = allPayments.stream()
                .filter(p -> !p.getIsReversed() && p.isBankChannelPayment())
                .count();
        long totalActivePayments = allPayments.stream().filter(p -> !p.getIsReversed()).count();
        double adoptionRate = totalActivePayments > 0 
                ? (bankChannelPayments * 100.0) / totalActivePayments 
                : 0.0;
        analytics.setBankChannelAdoptionRate(adoptionRate);
        
        // Bank channel revenue
        BigDecimal bankChannelRevenue = allPayments.stream()
                .filter(p -> !p.getIsReversed() && p.isBankChannelPayment())
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        analytics.setBankChannelRevenue(bankChannelRevenue);
        
        // Top 5 schools by revenue
        List<School> allSchools = schoolRepository.findAll();
        List<SchoolRevenueRanking> topSchools = allSchools.stream()
                .map(school -> {
                    BigDecimal schoolRevenue = paymentRepository.findBySchool(school).stream()
                            .filter(p -> !p.getIsReversed())
                            .map(Payment::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new SchoolRevenueRanking(school.getSchoolName(), school.getSchoolCode(), schoolRevenue);
                })
                .sorted((s1, s2) -> s2.getTotalRevenue().compareTo(s1.getTotalRevenue()))
                .limit(5)
                .collect(Collectors.toList());
        analytics.setTopSchoolsByRevenue(topSchools);
        
        return ResponseEntity.ok(analytics);
    }

    /**
     * GET /api/bank/admin/analytics/revenue-trend
     * 
     * Get daily revenue trend for the last 30 days across all schools.
     */
    @GetMapping("/analytics/revenue-trend")
    public ResponseEntity<List<DailyRevenue>> getRevenueTrend() {
        log.info("Bank admin: Getting revenue trend (last 30 days)");
        
        List<DailyRevenue> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);
            
            List<Payment> dayPayments = paymentRepository.findByPaymentDateBetween(dayStart, dayEnd);
            BigDecimal dayRevenue = dayPayments.stream()
                    .filter(p -> !p.getIsReversed())
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            long dayCount = dayPayments.stream().filter(p -> !p.getIsReversed()).count();
            
            trend.add(new DailyRevenue(date, dayRevenue, dayCount));
        }
        
        return ResponseEntity.ok(trend);
    }

    // ==================== Helper Methods ====================

    private SchoolSummary createSchoolSummary(School school) {
        SchoolSummary summary = new SchoolSummary();
        summary.setSchool(school);
        
        // Student count
        long studentCount = studentRepository.countBySchool(school);
        summary.setTotalStudents(studentCount);
        
        // Outstanding fees
        List<StudentFeeRecord> feeRecords = feeRecordRepository.findBySchool(school);
        BigDecimal outstanding = feeRecords.stream()
                .map(StudentFeeRecord::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalOutstanding(outstanding);
        
        // Total payments
        List<Payment> payments = paymentRepository.findBySchool(school);
        BigDecimal totalPayments = payments.stream()
                .filter(p -> !p.getIsReversed())
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalPaymentsReceived(totalPayments);
        
        // Last payment date
        payments.stream()
                .filter(p -> !p.getIsReversed())
                .max((p1, p2) -> p1.getPaymentDate().compareTo(p2.getPaymentDate()))
                .ifPresent(p -> summary.setLastPaymentDate(p.getPaymentDate()));
        
        return summary;
    }

    // ==================== DTOs ====================

    @Data
    public static class SchoolSummary {
        private School school;
        private long totalStudents;
        private BigDecimal totalOutstanding;
        private BigDecimal totalPaymentsReceived;
        private LocalDateTime lastPaymentDate;
    }

    @Data
    public static class SchoolDetails {
        private School school;
        private long totalStudents;
        private long activeStudents;
        private Map<String, Long> studentsByGrade;
        private BigDecimal totalFeesAssigned;
        private BigDecimal totalOutstanding;
        private BigDecimal totalPaymentsReceived;
        private long totalPayments;
        private Map<String, Long> paymentsByMethod;
        private long bankChannelPayments;
        private long schoolChannelPayments;
        private List<Payment> recentPayments;
    }

    @Data
    public static class BankAnalytics {
        private long totalSchools;
        private long activeSchools;
        private long totalStudents;
        private long activeStudents;
        private BigDecimal totalRevenueAllTime;
        private long totalPayments;
        private BigDecimal todayRevenue;
        private BigDecimal thisWeekRevenue;
        private BigDecimal thisMonthRevenue;
        private Map<String, Long> paymentMethodDistribution;
        private double bankChannelAdoptionRate;
        private BigDecimal bankChannelRevenue;
        private List<SchoolRevenueRanking> topSchoolsByRevenue;
    }

    @Data
    public static class SchoolRevenueRanking {
        private String schoolName;
        private String schoolCode;
        private BigDecimal totalRevenue;

        public SchoolRevenueRanking(String schoolName, String schoolCode, BigDecimal totalRevenue) {
            this.schoolName = schoolName;
            this.schoolCode = schoolCode;
            this.totalRevenue = totalRevenue;
        }
    }

    @Data
    public static class DailyRevenue {
        private LocalDate date;
        private BigDecimal revenue;
        private long paymentCount;

        public DailyRevenue(LocalDate date, BigDecimal revenue, long paymentCount) {
            this.date = date;
            this.revenue = revenue;
            this.paymentCount = paymentCount;
        }
    }
}
