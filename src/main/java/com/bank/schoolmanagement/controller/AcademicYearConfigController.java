package com.bank.schoolmanagement.controller;

import com.bank.schoolmanagement.dto.AcademicYearConfigRequest;
import com.bank.schoolmanagement.dto.AcademicYearConfigResponse;
import com.bank.schoolmanagement.dto.YearEndPromotionSummary;
import com.bank.schoolmanagement.entity.AcademicYearConfig;
import com.bank.schoolmanagement.service.AcademicYearConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing academic year configurations and automatic promotions
 */
@RestController
@RequestMapping("/api/school/academic-year-config")
@RequiredArgsConstructor
@Slf4j
public class AcademicYearConfigController {
    
    private final AcademicYearConfigService configService;
    
    /**
     * Create or update academic year configuration
     * 
     * POST /api/school/academic-year-config
     * 
     * This sets up when the automatic year-end promotion should occur.
     * The system will check daily at 2:00 AM and trigger promotion on the endOfYearDate.
     * 
     * Admin can configure:
     * - End of year date (when to trigger promotion)
     * - Next academic year label
     * - Whether to carry forward balances
     * - Fee structures for the new year
     */
    @PostMapping
    public ResponseEntity<AcademicYearConfigResponse> createOrUpdateConfig(
            @RequestBody AcademicYearConfigRequest request,
            Principal principal) {
        
        log.info("Creating/updating academic year config for: {}", request.getYear());
        
        AcademicYearConfig config = configService.createOrUpdateConfig(request, principal.getName());
        
        return ResponseEntity.ok(AcademicYearConfigResponse.fromEntity(config));
    }
    
    /**
     * Get all academic year configurations for current school
     * 
     * GET /api/school/academic-year-config
     */
    @GetMapping
    public ResponseEntity<List<AcademicYearConfigResponse>> getAllConfigs() {
        List<AcademicYearConfig> configs = configService.getConfigsForCurrentSchool();
        
        List<AcademicYearConfigResponse> responses = configs.stream()
            .map(AcademicYearConfigResponse::fromEntity)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get specific academic year configuration by ID
     * 
     * GET /api/school/academic-year-config/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AcademicYearConfigResponse> getConfigById(@PathVariable Long id) {
        return configService.getConfigById(id)
            .map(AcademicYearConfigResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get latest active configuration for current school
     * 
     * GET /api/school/academic-year-config/latest
     */
    @GetMapping("/latest")
    public ResponseEntity<AcademicYearConfigResponse> getLatestConfig() {
        return configService.getLatestActiveConfig()
            .map(AcademicYearConfigResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Cancel a scheduled promotion
     * 
     * POST /api/school/academic-year-config/{id}/cancel
     * 
     * Use this if the school decides not to proceed with the scheduled promotion.
     * Can only cancel promotions with status = SCHEDULED.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<AcademicYearConfigResponse> cancelPromotion(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        
        log.info("Cancelling promotion for config ID: {}", id);
        
        AcademicYearConfig config = configService.cancelPromotion(id, reason);
        
        return ResponseEntity.ok(AcademicYearConfigResponse.fromEntity(config));
    }
    
    /**
     * Manually trigger promotion before scheduled date
     * 
     * POST /api/school/academic-year-config/{id}/execute
     * 
     * Use this to manually trigger the promotion before the endOfYearDate.
     * Once triggered, the promotion executes immediately.
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<YearEndPromotionSummary> executePromotion(@PathVariable Long id) {
        log.info("Manually triggering promotion for config ID: {}", id);
        
        YearEndPromotionSummary summary = configService.triggerPromotion(id);
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Deactivate a configuration
     * 
     * DELETE /api/school/academic-year-config/{id}
     * 
     * Soft delete - sets isActive = false
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateConfig(@PathVariable Long id) {
        log.info("Deactivating config ID: {}", id);
        
        AcademicYearConfig config = configService.getConfigById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + id));
        
        config.setIsActive(false);
        configService.createOrUpdateConfig(
            new AcademicYearConfigRequest(), // Dummy request, we're just updating isActive
            "system"
        );
        
        return ResponseEntity.noContent().build();
    }
}
