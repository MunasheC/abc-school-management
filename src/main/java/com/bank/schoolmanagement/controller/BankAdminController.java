package com.bank.schoolmanagement.controller;

import com.bank.schoolmanagement.dto.SchoolUpdateRequest;
import com.bank.schoolmanagement.dto.SchoolSummaryResponse;
import com.bank.schoolmanagement.dto.SchoolDetailsResponse;
import com.bank.schoolmanagement.dto.SchoolResponse;
import com.bank.schoolmanagement.dto.PaymentResponse;
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

import org.springframework.data.domain.Pageable;
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
    public ResponseEntity<List<SchoolSummaryResponse>> getAllSchools() {
        log.info("Bank admin: Getting all schools with statistics");

        List<School> schools = schoolRepository.findAll();
        List<SchoolSummaryResponse> summaries = schools.stream()
                .map(this::createSchoolSummary)
                .collect(Collectors.toList());

        return ResponseEntity.ok(summaries);
    }

    /**
     * GET /api/bank/admin/schools/code/{schoolCode}
     * 
     * Get detailed information for a specific school by school code.
     * 
     * Includes:
     * - School details
     * - Student breakdown by grade
     * - Financial summary (fees, payments, outstanding)
     * - Payment method breakdown
     * - Recent payment activity
     */
    @GetMapping("/schools/code/{schoolCode}")
    public ResponseEntity<SchoolDetailsResponse> getSchoolDetailsByCode(@PathVariable String schoolCode) {
        log.info("Bank admin: Getting details for school code: {}", schoolCode);
        
        School school = schoolRepository.findBySchoolCode(schoolCode)
                .orElseThrow(() -> new IllegalArgumentException("School not found with code: " + schoolCode));
        
        SchoolDetailsResponse details = SchoolDetailsResponse.fromSchool(school);
        
        // Student statistics
        long totalStudents = studentRepository.countBySchool(school);
        long activeStudents = studentRepository.countBySchoolAndIsActive(school, true);
        details.setTotalStudents(totalStudents);
        details.setActiveStudents(activeStudents);
        
        // Get students by grade
        List<Student> students = studentRepository.findBySchool(school, Pageable.unpaged()).getContent();
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

        // Recent payments (last 10) mapped to DTOs
        List<PaymentResponse> recentPayments = payments.stream()
            .filter(p -> !p.getIsReversed())
            .sorted((p1, p2) -> p2.getPaymentDate().compareTo(p1.getPaymentDate()))
            .limit(10)
            .map(PaymentResponse::fromEntity)
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
    public ResponseEntity<SchoolResponse> onboardSchool(@Valid @RequestBody School school) {
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

        return ResponseEntity.status(HttpStatus.CREATED).body(SchoolResponse.fromEntity(savedSchool));
    }

    /**
     * PUT /api/bank/admin/schools/code/{schoolCode}
     * 
     * Update school information by school code.
     * Only include the fields you want to update in the request body.
     */
    @PutMapping("/schools/code/{schoolCode}")
    public ResponseEntity<SchoolResponse> updateSchoolByCode(
            @PathVariable String schoolCode, 
            @RequestBody SchoolUpdateRequest updateRequest) {
        log.info("Bank admin: Updating school with code: {}", schoolCode);
        
        School school = schoolRepository.findBySchoolCode(schoolCode)
                .orElseThrow(() -> new IllegalArgumentException("School not found with code: " + schoolCode));
        
        // Update fields if provided
        if (updateRequest.getSchoolName() != null) {
            school.setSchoolName(updateRequest.getSchoolName());
        }
        if (updateRequest.getSchoolType() != null) {
            school.setSchoolType(updateRequest.getSchoolType());
        }
        if (updateRequest.getAddress() != null) {
            school.setAddress(updateRequest.getAddress());
        }
        if (updateRequest.getCity() != null) {
            school.setCity(updateRequest.getCity());
        }
        if (updateRequest.getProvince() != null) {
            school.setProvince(updateRequest.getProvince());
        }
        if (updateRequest.getPostalCode() != null) {
            school.setPostalCode(updateRequest.getPostalCode());
        }
        if (updateRequest.getCountry() != null) {
            school.setCountry(updateRequest.getCountry());
        }
        if (updateRequest.getPrimaryPhone() != null) {
            school.setPrimaryPhone(updateRequest.getPrimaryPhone());
        }
        if (updateRequest.getSecondaryPhone() != null) {
            school.setSecondaryPhone(updateRequest.getSecondaryPhone());
        }
        if (updateRequest.getEmail() != null) {
            school.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getWebsite() != null) {
            school.setWebsite(updateRequest.getWebsite());
        }
        if (updateRequest.getHeadTeacherName() != null) {
            school.setHeadTeacherName(updateRequest.getHeadTeacherName());
        }
        if (updateRequest.getBursarName() != null) {
            school.setBursarName(updateRequest.getBursarName());
        }
        if (updateRequest.getBursarPhone() != null) {
            school.setBursarPhone(updateRequest.getBursarPhone());
        }
        if (updateRequest.getBursarEmail() != null) {
            school.setBursarEmail(updateRequest.getBursarEmail());
        }
        if (updateRequest.getMinistryRegistrationNumber() != null) {
            school.setMinistryRegistrationNumber(updateRequest.getMinistryRegistrationNumber());
        }
        if (updateRequest.getZimsecCenterNumber() != null) {
            school.setZimsecCenterNumber(updateRequest.getZimsecCenterNumber());
        }
        if (updateRequest.getBankAccountNumber() != null) {
            school.setBankAccountNumber(updateRequest.getBankAccountNumber());
        }
        if (updateRequest.getBankBranch() != null) {
            school.setBankBranch(updateRequest.getBankBranch());
        }
        if (updateRequest.getBankAccountName() != null) {
            school.setBankAccountName(updateRequest.getBankAccountName());
        }
        if (updateRequest.getRelationshipManager() != null) {
            school.setRelationshipManager(updateRequest.getRelationshipManager());
        }
        if (updateRequest.getRelationshipManagerPhone() != null) {
            school.setRelationshipManagerPhone(updateRequest.getRelationshipManagerPhone());
        }
        if (updateRequest.getSubscriptionTier() != null) {
            school.setSubscriptionTier(updateRequest.getSubscriptionTier());
        }
        if (updateRequest.getIsActive() != null) {
            school.setIsActive(updateRequest.getIsActive());
        }
        if (updateRequest.getMaxStudents() != null) {
            school.setMaxStudents(updateRequest.getMaxStudents());
        }
        if (updateRequest.getLogoUrl() != null) {
            school.setLogoUrl(updateRequest.getLogoUrl());
        }
        if (updateRequest.getPrimaryColor() != null) {
            school.setPrimaryColor(updateRequest.getPrimaryColor());
        }
        if (updateRequest.getNotes() != null) {
            school.setNotes(updateRequest.getNotes());
        }
        
        School updatedSchool = schoolRepository.save(school);
        log.info("School updated successfully: {}", schoolCode);
        return ResponseEntity.ok(SchoolResponse.fromEntity(updatedSchool));
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

        private SchoolSummaryResponse createSchoolSummary(School school) {
        SchoolSummaryResponse summary = SchoolSummaryResponse.fromEntity(school);

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

    // SchoolSummaryResponse and SchoolDetailsResponse DTOs are defined in `com.bank.schoolmanagement.dto` package

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
