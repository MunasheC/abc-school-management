package com.bank.schoolmanagement.scheduled;

import com.bank.schoolmanagement.entity.AcademicYearConfig;
import com.bank.schoolmanagement.service.AcademicYearConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled task to check for and trigger automatic year-end promotions
 * 
 * RUNS: Daily at 2:00 AM
 * 
 * WORKFLOW:
 * 1. Finds all academic year configs with end-of-year date = today
 * 2. Status must be SCHEDULED
 * 3. Triggers automatic promotion for each
 * 4. Updates status to COMPLETED or FAILED
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YearEndPromotionScheduler {
    
    private final AcademicYearConfigService configService;
    
    /**
     * Check for and execute year-end promotions
     * 
     * Runs daily at 2:00 AM
     * Cron: second minute hour day month day-of-week
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void checkAndExecutePromotions() {
        log.info("Running scheduled year-end promotion check");
        
        try {
            List<AcademicYearConfig> duePromotions = configService.findPromotionsDueToday();
            
            if (duePromotions.isEmpty()) {
                log.info("No year-end promotions due today");
                return;
            }
            
            log.info("Found {} promotion(s) due today", duePromotions.size());
            
            for (AcademicYearConfig config : duePromotions) {
                try {
                    log.info("Executing scheduled promotion for school: {} ({}, term {})",
                        config.getSchool().getSchoolName(), config.getYear(), config.getTerm());
                    
                    configService.executePromotion(config);
                    
                    log.info("Successfully completed promotion for school: {} ({}, term {})",
                        config.getSchool().getSchoolName(), config.getYear(), config.getTerm());
                    
                } catch (Exception e) {
                    log.error("Failed to execute promotion for school: {} (config ID: {})",
                        config.getSchool().getSchoolName(), config.getId(), e);
                    // Continue with next promotion
                }
            }
            
        } catch (Exception e) {
            log.error("Error in year-end promotion scheduler", e);
        }
    }
}
