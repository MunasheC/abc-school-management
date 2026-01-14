package com.bank.schoolmanagement.dto;

import com.bank.schoolmanagement.entity.AcademicYearConfig;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for academic year configuration
 */
@Data
public class AcademicYearConfigResponse {
    
    private Long id;
    private Integer year;
    private Integer term;
    private LocalDate endOfYearDate;
    private Integer nextYear;
    private Boolean carryForwardBalances;
    private String promotionStatus;
    private LocalDateTime promotionExecutedAt;
    private Integer studentsPromoted;
    private Integer studentsCompleted;
    private Integer promotionErrors;
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String createdBy;
    
    /**
     * Convert entity to DTO
     */
    public static AcademicYearConfigResponse fromEntity(AcademicYearConfig config) {
        AcademicYearConfigResponse dto = new AcademicYearConfigResponse();
        dto.setId(config.getId());
        dto.setYear(config.getYear());
        dto.setTerm(config.getTerm());
        dto.setEndOfYearDate(config.getEndOfYearDate());
        dto.setNextYear(config.getNextYear());
        dto.setCarryForwardBalances(config.getCarryForwardBalances());
        dto.setPromotionStatus(config.getPromotionStatus());
        dto.setPromotionExecutedAt(config.getPromotionExecutedAt());
        dto.setStudentsPromoted(config.getStudentsPromoted());
        dto.setStudentsCompleted(config.getStudentsCompleted());
        dto.setPromotionErrors(config.getPromotionErrors());
        dto.setNotes(config.getNotes());
        dto.setIsActive(config.getIsActive());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setCreatedBy(config.getCreatedBy());
        return dto;
    }
}
