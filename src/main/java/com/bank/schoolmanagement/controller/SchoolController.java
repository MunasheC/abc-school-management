package com.bank.schoolmanagement.controller;

import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.dto.SchoolResponse;
import com.bank.schoolmanagement.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * SchoolController - Multi-Tenant School Management API
 * 
 * PURPOSE: REST API for bank admins to manage schools on the platform
 * - Onboard new schools
 * - Manage subscriptions and licenses
 * - Track capacity and compliance
 * - Analytics and reporting
 * 
 * BASE PATH: /api/schools
 * 
 * SECURITY NOTE: These endpoints should be restricted to bank administrators
 * - School users (bursar) have separate endpoints scoped to their school
 * - Relationship managers can only see their assigned schools
 */
@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    /* ----------------------  ONBOARDING & REGISTRATION  ------------------------- */

    /**
     * Onboard a new school onto the platform
     * 
     * POST /api/schools
     * 
     * Request body example:
     * {
     *   "schoolCode": "SCH001",
     *   "schoolName": "Highlands Primary School",
     *   "schoolType": "PRIMARY",
     *   "primaryPhone": "+263771234567",
     *   "email": "admin@highlands.ac.zw",
     *   "address": "123 Main Street",
     *   "city": "Harare",
     *   "province": "Harare",
     *   "ministryRegistrationNumber": "MIN-2024-001",
     *   "subscriptionTier": "FREE",
     *   "relationshipManager": "John Doe",
     *   "relationshipManagerPhone": "+263772345678"
     * }
     */
    @PostMapping
    public ResponseEntity<SchoolResponse> onboardSchool(@Valid @RequestBody School school) {
        try {
            School onboarded = schoolService.onboardSchool(school);
            return ResponseEntity.status(HttpStatus.CREATED).body(SchoolResponse.fromEntity(onboarded));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /* ----------------------  SCHOOL RETRIEVAL  ------------------------- */

    /**
     * Get all active schools
     * 
     * GET /api/schools
     * 
     * Use case: Bank admin dashboard - see all schools
     */
    @GetMapping
    public ResponseEntity<List<SchoolResponse>> getAllActiveSchools() {
        List<School> schools = schoolService.getAllActiveSchools();
        List<SchoolResponse> dtos = schools.stream().map(SchoolResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get school by ID
     * 
     * GET /api/schools/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SchoolResponse> getSchoolById(@PathVariable Long id) {
        return schoolService.getSchoolById(id)
            .map(SchoolResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get school by unique code
     * 
     * GET /api/schools/code/{schoolCode}
     * 
     * Example: GET /api/schools/code/SCH001
     */
    @GetMapping("/code/{schoolCode}")
    public ResponseEntity<SchoolResponse> getSchoolByCode(@PathVariable String schoolCode) {
        return schoolService.getSchoolByCode(schoolCode)
            .map(SchoolResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /* ----------------------  SCHOOL MANAGEMENT  ------------------------- */

    /**
     * Update school details
     * 
     * PUT /api/schools/{id}
     * 
     * Request body: Partial updates allowed (only include fields to change)
     * {
     *   "schoolName": "Updated School Name",
     *   "primaryPhone": "+263771234567",
     *   "maxStudents": 500
     * }
     */
    @PutMapping("/{id}")
    public ResponseEntity<SchoolResponse> updateSchool(
        @PathVariable Long id,
        @RequestBody School updates
    ) {
        try {
            School updated = schoolService.updateSchool(id, updates);
            return ResponseEntity.ok(SchoolResponse.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate school (soft delete)
     * 
     * POST /api/schools/{id}/deactivate
     * 
     * Request body:
     * {
     *   "reason": "School closed permanently"
     * }
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<SchoolResponse> deactivateSchool(
        @PathVariable Long id,
        @RequestBody Map<String, String> request
    ) {
        try {
            String reason = request.getOrDefault("reason", "No reason provided");
            School deactivated = schoolService.deactivateSchool(id, reason);
            return ResponseEntity.ok(SchoolResponse.fromEntity(deactivated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reactivate school
     * 
     * POST /api/schools/{id}/reactivate
     */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<SchoolResponse> reactivateSchool(@PathVariable Long id) {
        try {
            School reactivated = schoolService.reactivateSchool(id);
            return ResponseEntity.ok(SchoolResponse.fromEntity(reactivated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /* ----------------------  SUBSCRIPTION MANAGEMENT  ------------------------- */

    /**
     * Update subscription tier
     * 
     * POST /api/schools/{id}/subscription
     * 
     * Request body:
     * {
     *   "tier": "PREMIUM"
     * }
     * 
     * Valid tiers: FREE, BASIC, PREMIUM
     */
    @PostMapping("/{id}/subscription")
    public ResponseEntity<SchoolResponse> updateSubscriptionTier(
        @PathVariable Long id,
        @RequestBody Map<String, String> request
    ) {
        try {
            String tier = request.get("tier");
            School updated = schoolService.updateSubscriptionTier(id, tier);
            return ResponseEntity.ok(SchoolResponse.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get schools by subscription tier
     * 
     * GET /api/schools/subscription/{tier}
     * 
     * Example: GET /api/schools/subscription/PREMIUM
     */
    @GetMapping("/subscription/{tier}")
    public ResponseEntity<List<SchoolResponse>> getSchoolsByTier(@PathVariable String tier) {
        List<School> schools = schoolService.getSchoolsByTier(tier);
        List<SchoolResponse> dtos = schools.stream().map(SchoolResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get capacity alerts (schools at or near capacity)
     * 
     * GET /api/schools/capacity-alerts
     * 
     * Response:
     * {
     *   "atCapacity": [...],
     *   "nearCapacity": [...]
     * }
     */
    @GetMapping("/capacity-alerts")
    public ResponseEntity<Map<String, List<SchoolResponse>>> getCapacityAlerts() {
        Map<String, List<School>> raw = schoolService.getCapacityAlerts();
        Map<String, List<SchoolResponse>> mapped = raw.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream().map(SchoolResponse::fromEntity).toList()
            ));
        return ResponseEntity.ok(mapped);
    }

    /* ----------------------  LICENSE MANAGEMENT  ------------------------- */

    /**
     * Renew school license
     * 
     * POST /api/schools/{id}/renew-license
     * 
     * Request body:
     * {
     *   "expiryDate": "2026-12-31"
     * }
     */
    @PostMapping("/{id}/renew-license")
    public ResponseEntity<SchoolResponse> renewLicense(
        @PathVariable Long id,
        @RequestBody Map<String, String> request
    ) {
        try {
            LocalDate expiryDate = LocalDate.parse(request.get("expiryDate"));
            School renewed = schoolService.renewLicense(id, expiryDate);
            return ResponseEntity.ok(SchoolResponse.fromEntity(renewed));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get schools with expired licenses
     * 
     * GET /api/schools/expired-licenses
     */
    @GetMapping("/expired-licenses")
    public ResponseEntity<List<SchoolResponse>> getSchoolsWithExpiredLicenses() {
        List<School> schools = schoolService.getSchoolsWithExpiredLicenses();
        List<SchoolResponse> dtos = schools.stream().map(SchoolResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get schools with licenses expiring soon
     * 
     * GET /api/schools/expiring-licenses?days=30
     * 
     * Query param: days (default 30)
     */
    @GetMapping("/expiring-licenses")
    public ResponseEntity<List<SchoolResponse>> getSchoolsWithExpiringLicenses(
        @RequestParam(defaultValue = "30") int days
    ) {
        List<School> schools = schoolService.getSchoolsWithExpiringLicenses(days);
        List<SchoolResponse> dtos = schools.stream().map(SchoolResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Auto-deactivate schools with expired licenses
     * 
     * POST /api/schools/deactivate-expired
     * 
     * Use case: Scheduled job (runs daily)
     * Returns list of schools that were deactivated
     */
    @PostMapping("/deactivate-expired")
    public ResponseEntity<List<SchoolResponse>> deactivateExpiredLicenses() {
        List<School> deactivated = schoolService.deactivateExpiredLicenses();
        List<SchoolResponse> dtos = deactivated.stream().map(SchoolResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /* ----------------------  RELATIONSHIP MANAGER  ------------------------- */

    /**
     * Get schools for a relationship manager
     * 
     * GET /api/schools/manager/{managerName}
     * 
     * Example: GET /api/schools/manager/John%20Doe
     */
    @GetMapping("/manager/{managerName}")
    public ResponseEntity<List<SchoolResponse>> getSchoolsByRelationshipManager(
        @PathVariable String managerName
    ) {
        List<School> schools = schoolService.getSchoolsByRelationshipManager(managerName);
        List<SchoolResponse> dtos = schools.stream().map(SchoolResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Assign relationship manager to school
     * 
     * POST /api/schools/{id}/assign-manager
     * 
     * Request body:
     * {
     *   "managerName": "John Doe",
     *   "managerPhone": "+263771234567"
     * }
     */
    @PostMapping("/{id}/assign-manager")
    public ResponseEntity<SchoolResponse> assignRelationshipManager(
        @PathVariable Long id,
        @RequestBody Map<String, String> request
    ) {
        try {
            String managerName = request.get("managerName");
            String managerPhone = request.get("managerPhone");
            School updated = schoolService.assignRelationshipManager(id, managerName, managerPhone);
            return ResponseEntity.ok(SchoolResponse.fromEntity(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get relationship manager workload distribution
     * 
     * GET /api/schools/manager-workload
     * 
     * Response: Map of manager name → school count
     * {
     *   "John Doe": 5,
     *   "Jane Smith": 3,
     *   "Bob Johnson": 7
     * }
     */
    @GetMapping("/manager-workload")
    public ResponseEntity<Map<String, Long>> getRelationshipManagerWorkload() {
        return ResponseEntity.ok(schoolService.getRelationshipManagerWorkload());
    }

    /* ----------------------  SEARCH & FILTER  ------------------------- */

    /**
     * Search schools by name
     * 
     * GET /api/schools/search?name=High
     * 
     * Case-insensitive, partial match
     */
    @GetMapping("/search")
    public ResponseEntity<List<SchoolResponse>> searchSchools(@RequestParam String name) {
        List<School> schools = schoolService.searchSchoolsByName(name);
        List<SchoolResponse> dtos = schools.stream().map(SchoolResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get schools by type
     * 
     * GET /api/schools/type/{type}
     * 
     * Valid types: PRIMARY, SECONDARY, COMBINED
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<SchoolResponse>> getSchoolsByType(@PathVariable String type) {
        List<School> schools = schoolService.getSchoolsByType(type);
        List<SchoolResponse> dtos = schools.stream().map(SchoolResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get schools by city
     * 
     * GET /api/schools/city/{city}
     * 
     * Example: GET /api/schools/city/Harare
     */
    @GetMapping("/city/{city}")
    public ResponseEntity<List<SchoolResponse>> getSchoolsByCity(@PathVariable String city) {
        List<School> schools = schoolService.getSchoolsByCity(city);
        List<SchoolResponse> dtos = schools.stream().map(SchoolResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get schools by province
     * 
     * GET /api/schools/province/{province}
     * 
     * Example: GET /api/schools/province/Harare
     */
    @GetMapping("/province/{province}")
    public ResponseEntity<List<SchoolResponse>> getSchoolsByProvince(@PathVariable String province) {
        List<School> schools = schoolService.getSchoolsByProvince(province);
        List<SchoolResponse> dtos = schools.stream().map(SchoolResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /* ----------------------  STATISTICS & ANALYTICS  ------------------------- */

    /**
     * Get comprehensive school statistics
     * 
     * GET /api/schools/{id}/statistics
     * 
     * Response:
     * {
     *   "schoolName": "Highlands Primary",
     *   "currentStudents": 85,
     *   "maxStudents": 100,
     *   "capacityPercentage": 85.0,
     *   "subscriptionTier": "FREE",
     *   "daysUntilExpiry": 120,
     *   ...
     * }
     */
    @GetMapping("/{id}/statistics")
    public ResponseEntity<Map<String, Object>> getSchoolStatistics(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(schoolService.getSchoolStatistics(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get platform-wide statistics
     * 
     * GET /api/schools/statistics/platform
     * 
     * Response:
     * {
     *   "totalSchools": 50,
     *   "totalStudents": 12500,
     *   "schoolsAtCapacity": 3,
     *   "freeSchools": 30,
     *   "basicSchools": 15,
     *   "premiumSchools": 5,
     *   ...
     * }
     */
    @GetMapping("/statistics/platform")
    public ResponseEntity<Map<String, Object>> getPlatformStatistics() {
        return ResponseEntity.ok(schoolService.getPlatformStatistics());
    }

    /**
     * Get recently onboarded schools (last 10)
     * 
     * GET /api/schools/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<List<SchoolResponse>> getRecentlyOnboardedSchools() {
        List<School> schools = schoolService.getRecentlyOnboardedSchools();
        List<SchoolResponse> dtos = schools.stream().map(SchoolResponse::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }
}
