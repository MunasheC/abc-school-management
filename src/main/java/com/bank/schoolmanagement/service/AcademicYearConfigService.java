package com.bank.schoolmanagement.service;

import com.bank.schoolmanagement.context.SchoolContext;
import com.bank.schoolmanagement.dto.AcademicYearConfigRequest;
import com.bank.schoolmanagement.dto.YearEndPromotionRequest;
import com.bank.schoolmanagement.dto.YearEndPromotionSummary;
import com.bank.schoolmanagement.entity.AcademicYearConfig;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.repository.AcademicYearConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing academic year configurations and automatic promotions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicYearConfigService {
    
    private final AcademicYearConfigRepository configRepository;
    private final StudentService studentService;
    private final ObjectMapper objectMapper;
    
    /**
     * Create or update academic year configuration
     */
    @Transactional
    public AcademicYearConfig createOrUpdateConfig(AcademicYearConfigRequest request, String createdBy) {
        School currentSchool = SchoolContext.getCurrentSchool();
        if (currentSchool == null) {
            throw new IllegalStateException("School context is required");
        }
        
        // Check if configuration already exists
        Optional<AcademicYearConfig> existingOpt = configRepository
            .findBySchoolAndYear(currentSchool, request.getYear());
        
        AcademicYearConfig config;
        if (existingOpt.isPresent()) {
            config = existingOpt.get();
            log.info("Updating existing academic year config for {}", request.getYear());
        } else {
            config = new AcademicYearConfig();
            config.setSchool(currentSchool);
            config.setCreatedBy(createdBy);
            log.info("Creating new academic year config for {}", request.getYear());
        }
        
        config.setYear(request.getYear());
        config.setTerm(request.getTerm());
        config.setEndOfYearDate(request.getEndOfYearDate());
        config.setNextYear(request.getNextYear());
        config.setCarryForwardBalances(request.getCarryForwardBalances());
        config.setNotes(request.getNotes());
        
        // Serialize fee structures to JSON
        try {
            if (request.getFeeStructures() != null) {
                config.setFeeStructuresJson(objectMapper.writeValueAsString(request.getFeeStructures()));
            }
            if (request.getDefaultFeeStructure() != null) {
                config.setDefaultFeeStructureJson(objectMapper.writeValueAsString(request.getDefaultFeeStructure()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize fee structures: " + e.getMessage(), e);
        }
        
        return configRepository.save(config);
    }
    
    /**
     * Get all configurations for current school
     */
    public List<AcademicYearConfig> getConfigsForCurrentSchool() {
        School currentSchool = SchoolContext.getCurrentSchool();
        if (currentSchool == null) {
            throw new IllegalStateException("School context is required");
        }
        return configRepository.findBySchoolOrderByEndOfYearDateDesc(currentSchool);
    }
    
    /**
     * Get configuration by ID
     */
    public Optional<AcademicYearConfig> getConfigById(Long id) {
        return configRepository.findById(id);
    }
    
    /**
     * Get latest active configuration for current school
     */
    public Optional<AcademicYearConfig> getLatestActiveConfig() {
        School currentSchool = SchoolContext.getCurrentSchool();
        if (currentSchool == null) {
            throw new IllegalStateException("School context is required");
        }
        return configRepository.findFirstBySchoolAndIsActiveTrueOrderByEndOfYearDateDesc(currentSchool);
    }
    
    /**
     * Cancel a scheduled promotion
     */
    @Transactional
    public AcademicYearConfig cancelPromotion(Long configId, String reason) {
        AcademicYearConfig config = configRepository.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + configId));
        
        if (!"SCHEDULED".equals(config.getPromotionStatus())) {
            throw new IllegalStateException("Can only cancel SCHEDULED promotions");
        }
        
        config.cancel(reason);
        return configRepository.save(config);
    }
    
    /**
     * Manually trigger promotion for a configuration
     */
    @Transactional
    public YearEndPromotionSummary triggerPromotion(Long configId) {
        AcademicYearConfig config = configRepository.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + configId));
        
        if (!"SCHEDULED".equals(config.getPromotionStatus())) {
            throw new IllegalStateException("Promotion already executed or cancelled");
        }
        
        return executePromotion(config);
    }
    
    /**
     * Find all promotions that should be triggered today
     * Called by scheduled task
     */
    public List<AcademicYearConfig> findPromotionsDueToday() {
        return configRepository.findPromotionsDueByDate(LocalDate.now());
    }
    
    /**
     * Execute promotion for a configuration
     */
    @Transactional
    public YearEndPromotionSummary executePromotion(AcademicYearConfig config) {
        log.info("Executing year-end promotion for config ID: {} ({})", 
            config.getId(), config.getYear());
        
        config.setPromotionStatus("IN_PROGRESS");
        configRepository.save(config);
        
        try {
            // Build promotion request from config
            // NOTE: Fee structures are NOT used during automatic promotion
            // Fee assignment must be done manually via StudentFeeRecordController
            YearEndPromotionRequest promotionRequest = new YearEndPromotionRequest();
            promotionRequest.setNewYear(config.getNextYear());
            promotionRequest.setCarryForwardBalances(config.getCarryForwardBalances());
            promotionRequest.setExcludedStudentIds(null); // None excluded by default
            promotionRequest.setPromotionNotes("Automated year-end promotion for " + config.getYear());
            
            // Fee structures intentionally not set - manual fee assignment required
            promotionRequest.setFeeStructures(null);
            promotionRequest.setDefaultFeeStructure(null);
            
            // Execute promotion using StudentService
            YearEndPromotionSummary summary = studentService.performYearEndPromotion(promotionRequest);
            
            // Update config with results
            config.markPromotionCompleted(
                summary.getPromotedCount(),
                summary.getCompletedCount(),
                summary.getErrorCount()
            );
            configRepository.save(config);
            
            log.info("Year-end promotion completed for config ID: {}. Promoted: {}, Completed: {}, Errors: {}",
                config.getId(), summary.getPromotedCount(), summary.getCompletedCount(), summary.getErrorCount());
            
            // Automatic rollover: Create next year's configuration
            try {
                createNextYearConfig(config);
            } catch (Exception e) {
                log.error("Failed to create automatic rollover config for next year", e);
                // Don't fail the entire promotion if rollover fails
            }
            
            return summary;
            
        } catch (Exception e) {
            log.error("Year-end promotion failed for config ID: {}", config.getId(), e);
            config.markPromotionFailed(e.getMessage());
            configRepository.save(config);
            throw new RuntimeException("Promotion failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Automatically create next year's academic configuration after successful promotion
     * 
     * ROLLOVER LOGIC:
     * - Creates config for the year after nextYear (e.g., if 2025->2026, creates 2027)
     * - Copies fee structures from current config
     * - Sets end-of-year date to same month/day but next year
     * - Admin can modify later if needed
     */
    private void createNextYearConfig(AcademicYearConfig completedConfig) throws JsonProcessingException {
        School school = completedConfig.getSchool();
        Integer rolloverYear = completedConfig.getNextYear();
        
        // Check if config for next year already exists
        Optional<AcademicYearConfig> existingConfig = configRepository.findBySchoolAndYear(school, rolloverYear);
        if (existingConfig.isPresent()) {
            log.info("Academic year config for {} already exists, skipping automatic rollover", rolloverYear);
            return;
        }
        
        log.info("Creating automatic rollover config for year {}", rolloverYear);
        
        AcademicYearConfig nextYearConfig = new AcademicYearConfig();
        nextYearConfig.setSchool(school);
        nextYearConfig.setYear(rolloverYear);
        nextYearConfig.setTerm(1); // Reset to term 1 for new year
        
        // Set end-of-year date: same month/day as current config, but next year
        LocalDate newEndDate = completedConfig.getEndOfYearDate().plusYears(1);
        nextYearConfig.setEndOfYearDate(newEndDate);
        
        // Set next year to current nextYear + 1
        nextYearConfig.setNextYear(rolloverYear + 1);
        
        // Copy other settings
        nextYearConfig.setCarryForwardBalances(completedConfig.getCarryForwardBalances());
        nextYearConfig.setFeeStructuresJson(completedConfig.getFeeStructuresJson());
        nextYearConfig.setDefaultFeeStructureJson(completedConfig.getDefaultFeeStructureJson());
        
        // Set as scheduled for automatic promotion
        nextYearConfig.setPromotionStatus("SCHEDULED");
        nextYearConfig.setIsActive(true);
        nextYearConfig.setCreatedBy("SYSTEM_ROLLOVER");
        nextYearConfig.setNotes("Automatically created after " + completedConfig.getYear() + " promotion. Admin can modify as needed.");
        
        configRepository.save(nextYearConfig);
        
        log.info("Successfully created automatic rollover config for year {} with end date: {}", 
            rolloverYear, newEndDate);
    }
}
