package com.bank.schoolmanagement.service;

import com.bank.schoolmanagement.context.SchoolContext;
import com.bank.schoolmanagement.dto.AcademicYearConfigRequest;
import com.bank.schoolmanagement.dto.YearEndPromotionRequest;
import com.bank.schoolmanagement.dto.YearEndPromotionSummary;
import com.bank.schoolmanagement.entity.AcademicYearConfig;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.repository.AcademicYearConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
            .findBySchoolAndAcademicYear(currentSchool, request.getAcademicYear());
        
        AcademicYearConfig config;
        if (existingOpt.isPresent()) {
            config = existingOpt.get();
            log.info("Updating existing academic year config for {}", request.getAcademicYear());
        } else {
            config = new AcademicYearConfig();
            config.setSchool(currentSchool);
            config.setCreatedBy(createdBy);
            log.info("Creating new academic year config for {}", request.getAcademicYear());
        }
        
        config.setAcademicYear(request.getAcademicYear());
        config.setEndOfYearDate(request.getEndOfYearDate());
        config.setNextAcademicYear(request.getNextAcademicYear());
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
            config.getId(), config.getAcademicYear());
        
        config.setPromotionStatus("IN_PROGRESS");
        configRepository.save(config);
        
        try {
            // Build promotion request from config
            YearEndPromotionRequest promotionRequest = new YearEndPromotionRequest();
            promotionRequest.setNewAcademicYear(config.getNextAcademicYear());
            promotionRequest.setCarryForwardBalances(config.getCarryForwardBalances());
            promotionRequest.setExcludedStudentIds(null); // None excluded by default
            promotionRequest.setPromotionNotes("Automated year-end promotion for " + config.getAcademicYear());
            
            // Deserialize fee structures
            try {
                if (config.getFeeStructuresJson() != null) {
                    Map<String, YearEndPromotionRequest.FeeStructure> feeStructures = 
                        objectMapper.readValue(config.getFeeStructuresJson(), 
                            new TypeReference<Map<String, YearEndPromotionRequest.FeeStructure>>() {});
                    promotionRequest.setFeeStructures(feeStructures);
                }
                if (config.getDefaultFeeStructureJson() != null) {
                    YearEndPromotionRequest.FeeStructure defaultStructure = 
                        objectMapper.readValue(config.getDefaultFeeStructureJson(), 
                            YearEndPromotionRequest.FeeStructure.class);
                    promotionRequest.setDefaultFeeStructure(defaultStructure);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize fee structures: " + e.getMessage(), e);
            }
            
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
            
            return summary;
            
        } catch (Exception e) {
            log.error("Year-end promotion failed for config ID: {}", config.getId(), e);
            config.markPromotionFailed(e.getMessage());
            configRepository.save(config);
            throw new RuntimeException("Promotion failed: " + e.getMessage(), e);
        }
    }
}
