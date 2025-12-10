package com.bank.schoolmanagement.service;

import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SchoolService - Multi-Tenant School Management Service
 * 
 * PURPOSE: Business logic for school onboarding, management, and analytics
 * - This is the bank's platform for onboarding schools
 * - Each school is a tenant with isolated data
 * - Manages subscriptions, capacity, licenses, and bank relationships
 * 
 * LEARNING: Service layer responsibilities
 * - Input validation (unique codes, capacity limits, dates)
 * - Business rules (subscription tiers, license enforcement)
 * - Calculations (statistics, capacity percentages)
 * - Coordination (multiple entity updates)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SchoolService {

    private final SchoolRepository schoolRepository;

    /* ----------------------  ONBOARDING & REGISTRATION  ------------------------- */

    /**
     * Onboard a new school onto the platform
     * 
     * BUSINESS RULES:
     * - School code must be unique
     * - Ministry registration required
     * - Bank account can be added later
     * - Starts with FREE tier by default
     * - License valid for 1 year initially
     * 
     * @param school School to onboard
     * @return Onboarded school with ID
     * @throws IllegalArgumentException if school code exists
     */
    public School onboardSchool(School school) {
        // Validate unique school code
        if (schoolRepository.existsBySchoolCode(school.getSchoolCode())) {
            throw new IllegalArgumentException(
                "School code already exists: " + school.getSchoolCode()
            );
        }

        // Set onboarding defaults
        if (school.getOnboardingDate() == null) {
            school.setOnboardingDate(LocalDate.now());
        }
        
        if (school.getIsActive() == null) {
            school.setIsActive(true);
        }

        if (school.getSubscriptionTier() == null || school.getSubscriptionTier().isEmpty()) {
            school.setSubscriptionTier("FREE");
        }

        if (school.getCurrentStudentCount() == null) {
            school.setCurrentStudentCount(0);
        }

        // Set default capacity based on tier
        if (school.getMaxStudents() == null) {
            school.setMaxStudents(getDefaultCapacity(school.getSubscriptionTier()));
        }

        // Set default license (1 year)
        if (school.getLicenseExpiryDate() == null) {
            school.setLicenseExpiryDate(LocalDate.now().plusYears(1));
        }

        School savedSchool = schoolRepository.save(school);
        
        System.out.println("✅ School onboarded successfully: " + school.getSchoolName() + 
                          " (Code: " + school.getSchoolCode() + ")");
        
        return savedSchool;
    }

    /**
     * Get default student capacity based on subscription tier
     */
    private Integer getDefaultCapacity(String tier) {
        return switch (tier.toUpperCase()) {
            case "FREE" -> 100;
            case "BASIC" -> 500;
            case "PREMIUM" -> 2000;
            default -> 100;
        };
    }

    /* ----------------------  SCHOOL MANAGEMENT  ------------------------- */

    /**
     * Get school by ID
     */
    public Optional<School> getSchoolById(Long id) {
        return schoolRepository.findById(id);
    }

    /**
     * Get school by unique code (tenant identifier)
     */
    public Optional<School> getSchoolByCode(String schoolCode) {
        return schoolRepository.findBySchoolCode(schoolCode);
    }

    /**
     * Get all active schools
     */
    public List<School> getAllActiveSchools() {
        return schoolRepository.findByIsActiveTrue();
    }

    /**
     * Update school details
     * 
     * RULES:
     * - Cannot change school code (tenant identifier is permanent)
     * - Cannot reduce maxStudents below currentStudentCount
     * 
     * @param id School ID
     * @param updates School with updated fields
     * @return Updated school
     */
    public School updateSchool(Long id, School updates) {
        School existing = schoolRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("School not found with ID: " + id));

        // Update allowed fields
        if (updates.getSchoolName() != null) {
            existing.setSchoolName(updates.getSchoolName());
        }
        if (updates.getSchoolType() != null) {
            existing.setSchoolType(updates.getSchoolType());
        }
        if (updates.getPrimaryPhone() != null) {
            existing.setPrimaryPhone(updates.getPrimaryPhone());
        }
        if (updates.getEmail() != null) {
            existing.setEmail(updates.getEmail());
        }
        if (updates.getAddress() != null) {
            existing.setAddress(updates.getAddress());
        }
        if (updates.getCity() != null) {
            existing.setCity(updates.getCity());
        }
        if (updates.getProvince() != null) {
            existing.setProvince(updates.getProvince());
        }
        
        // Administration updates
        if (updates.getHeadTeacherName() != null) {
            existing.setHeadTeacherName(updates.getHeadTeacherName());
        }
        if (updates.getBursarName() != null) {
            existing.setBursarName(updates.getBursarName());
        }
        if (updates.getBursarPhone() != null) {
            existing.setBursarPhone(updates.getBursarPhone());
        }
        if (updates.getBursarEmail() != null) {
            existing.setBursarEmail(updates.getBursarEmail());
        }

        // Banking updates
        if (updates.getBankAccountNumber() != null) {
            existing.setBankAccountNumber(updates.getBankAccountNumber());
        }
        if (updates.getBankBranch() != null) {
            existing.setBankBranch(updates.getBankBranch());
        }
        if (updates.getRelationshipManager() != null) {
            existing.setRelationshipManager(updates.getRelationshipManager());
        }
        if (updates.getRelationshipManagerPhone() != null) {
            existing.setRelationshipManagerPhone(updates.getRelationshipManagerPhone());
        }

        // Capacity update (with validation)
        if (updates.getMaxStudents() != null) {
            if (updates.getMaxStudents() < existing.getCurrentStudentCount()) {
                throw new IllegalArgumentException(
                    "Cannot reduce capacity below current student count (" + 
                    existing.getCurrentStudentCount() + ")"
                );
            }
            existing.setMaxStudents(updates.getMaxStudents());
        }

        // Configuration updates
        if (updates.getLogoUrl() != null) {
            existing.setLogoUrl(updates.getLogoUrl());
        }
        if (updates.getPrimaryColor() != null) {
            existing.setPrimaryColor(updates.getPrimaryColor());
        }

        return schoolRepository.save(existing);
    }

    /**
     * Deactivate school (soft delete)
     * 
     * Use case: School closes, fails compliance, non-payment
     * - Preserves data for historical reporting
     * - Blocks all access
     * - Can be reactivated later
     * 
     * @param id School ID
     * @param reason Deactivation reason (for audit trail)
     */
    public School deactivateSchool(Long id, String reason) {
        School school = schoolRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("School not found with ID: " + id));

        school.deactivate(reason);
        
        System.out.println("⚠️ School deactivated: " + school.getSchoolName() + 
                          " (Reason: " + reason + ")");
        
        return schoolRepository.save(school);
    }

    /**
     * Reactivate school
     * 
     * Use case: School resolves issues, renews license
     */
    public School reactivateSchool(Long id) {
        School school = schoolRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("School not found with ID: " + id));

        school.reactivate();
        
        System.out.println("✅ School reactivated: " + school.getSchoolName());
        
        return schoolRepository.save(school);
    }

    /* ----------------------  SUBSCRIPTION MANAGEMENT  ------------------------- */

    /**
     * Upgrade/downgrade subscription tier
     * 
     * RULES:
     * - Can upgrade anytime (FREE → BASIC → PREMIUM)
     * - Can downgrade if within new tier's capacity
     * - Capacity adjusted automatically per tier
     * 
     * @param id School ID
     * @param newTier New subscription tier (FREE/BASIC/PREMIUM)
     * @return Updated school
     */
    public School updateSubscriptionTier(Long id, String newTier) {
        School school = schoolRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("School not found with ID: " + id));

        String oldTier = school.getSubscriptionTier();
        Integer newCapacity = getDefaultCapacity(newTier);

        // Validate downgrade
        if (school.getCurrentStudentCount() > newCapacity) {
            throw new IllegalArgumentException(
                "Cannot downgrade to " + newTier + ". Current student count (" + 
                school.getCurrentStudentCount() + ") exceeds tier capacity (" + 
                newCapacity + ")"
            );
        }

        school.setSubscriptionTier(newTier);
        school.setMaxStudents(newCapacity);

        School updated = schoolRepository.save(school);
        
        System.out.println("📊 Subscription updated: " + school.getSchoolName() + 
                          " (" + oldTier + " → " + newTier + ")");
        
        return updated;
    }

    /**
     * Get schools by subscription tier
     */
    public List<School> getSchoolsByTier(String tier) {
        return schoolRepository.findBySubscriptionTier(tier);
    }

    /**
     * Get schools at or near capacity
     */
    public Map<String, List<School>> getCapacityAlerts() {
        Map<String, List<School>> alerts = new HashMap<>();
        alerts.put("atCapacity", schoolRepository.findSchoolsAtCapacity());
        alerts.put("nearCapacity", schoolRepository.findSchoolsNearCapacity());
        return alerts;
    }

    /* ----------------------  LICENSE MANAGEMENT  ------------------------- */

    /**
     * Renew school license
     * 
     * @param id School ID
     * @param newExpiryDate New expiry date (typically 1 year from now)
     */
    public School renewLicense(Long id, LocalDate newExpiryDate) {
        School school = schoolRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("School not found with ID: " + id));

        school.setLicenseExpiryDate(newExpiryDate);
        
        // Reactivate if was deactivated due to expiry
        if (!school.getIsActive()) {
            school.reactivate();
        }

        School updated = schoolRepository.save(school);
        
        System.out.println("📝 License renewed: " + school.getSchoolName() + 
                          " (Valid until: " + newExpiryDate + ")");
        
        return updated;
    }

    /**
     * Get schools with expired licenses
     */
    public List<School> getSchoolsWithExpiredLicenses() {
        return schoolRepository.findSchoolsWithExpiredLicenses(LocalDate.now());
    }

    /**
     * Get schools with licenses expiring soon
     * 
     * @param daysAhead How many days to look ahead (e.g., 30 for next month)
     */
    public List<School> getSchoolsWithExpiringLicenses(int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(daysAhead);
        return schoolRepository.findSchoolsWithLicensesExpiringSoon(today, futureDate);
    }

    /**
     * Automatically deactivate schools with expired licenses
     * 
     * Use case: Scheduled job (runs daily)
     * - Check all schools
     * - Deactivate if license expired
     * - Send notification to school and relationship manager
     */
    public List<School> deactivateExpiredLicenses() {
        List<School> expired = getSchoolsWithExpiredLicenses();
        
        expired.forEach(school -> {
            if (school.getIsActive()) {
                school.deactivate("License expired on " + school.getLicenseExpiryDate());
                schoolRepository.save(school);
                
                System.out.println("⛔ Auto-deactivated (expired license): " + school.getSchoolName());
            }
        });
        
        return expired;
    }

    /* ----------------------  RELATIONSHIP MANAGER  ------------------------- */

    /**
     * Get schools for a specific relationship manager
     */
    public List<School> getSchoolsByRelationshipManager(String managerName) {
        return schoolRepository.findByRelationshipManager(managerName);
    }

    /**
     * Assign relationship manager to school
     */
    public School assignRelationshipManager(Long schoolId, String managerName, String managerPhone) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new RuntimeException("School not found with ID: " + schoolId));

        school.setRelationshipManager(managerName);
        school.setRelationshipManagerPhone(managerPhone);

        return schoolRepository.save(school);
    }

    /**
     * Get relationship manager workload distribution
     * 
     * Returns: Map of manager name → school count
     */
    public Map<String, Long> getRelationshipManagerWorkload() {
        List<Object[]> results = schoolRepository.countSchoolsPerRelationshipManager();
        
        return results.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1]
            ));
    }

    /* ----------------------  SEARCH & FILTER  ------------------------- */

    /**
     * Search schools by name
     */
    public List<School> searchSchoolsByName(String name) {
        return schoolRepository.findBySchoolNameContainingIgnoreCase(name);
    }

    /**
     * Get schools by type
     */
    public List<School> getSchoolsByType(String type) {
        return schoolRepository.findBySchoolType(type);
    }

    /**
     * Get schools by location
     */
    public List<School> getSchoolsByCity(String city) {
        return schoolRepository.findByCity(city);
    }

    public List<School> getSchoolsByProvince(String province) {
        return schoolRepository.findByProvince(province);
    }

    /* ----------------------  STATISTICS & ANALYTICS  ------------------------- */

    /**
     * Get comprehensive school statistics
     */
    public Map<String, Object> getSchoolStatistics(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new RuntimeException("School not found with ID: " + schoolId));

        Map<String, Object> stats = new HashMap<>();
        stats.put("schoolName", school.getSchoolName());
        stats.put("schoolCode", school.getSchoolCode());
        stats.put("currentStudents", school.getCurrentStudentCount());
        stats.put("maxStudents", school.getMaxStudents());
        stats.put("capacityPercentage", 
            school.getCurrentStudentCount() * 100.0 / school.getMaxStudents());
        stats.put("subscriptionTier", school.getSubscriptionTier());
        stats.put("isActive", school.getIsActive());
        stats.put("licenseExpiryDate", school.getLicenseExpiryDate());
        stats.put("daysUntilExpiry", 
            java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), school.getLicenseExpiryDate()));

        return stats;
    }

    /**
     * Get platform-wide statistics
     */
    public Map<String, Object> getPlatformStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalSchools", schoolRepository.countActiveSchools());
        stats.put("totalStudents", schoolRepository.getTotalStudentCountAcrossPlatform());
        stats.put("schoolsAtCapacity", schoolRepository.findSchoolsAtCapacity().size());
        stats.put("schoolsNearCapacity", schoolRepository.findSchoolsNearCapacity().size());
        stats.put("expiredLicenses", getSchoolsWithExpiredLicenses().size());
        stats.put("expiringIn30Days", getSchoolsWithExpiringLicenses(30).size());
        
        // Count by tier
        stats.put("freeSchools", schoolRepository.findBySubscriptionTier("FREE").size());
        stats.put("basicSchools", schoolRepository.findBySubscriptionTier("BASIC").size());
        stats.put("premiumSchools", schoolRepository.findBySubscriptionTier("PREMIUM").size());
        
        return stats;
    }

    /**
     * Get recently onboarded schools (last 10)
     */
    public List<School> getRecentlyOnboardedSchools() {
        return schoolRepository.findTop10ByIsActiveTrueOrderByOnboardingDateDesc();
    }
}
