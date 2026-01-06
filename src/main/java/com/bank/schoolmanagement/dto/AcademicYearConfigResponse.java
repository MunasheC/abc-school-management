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
    private String academicYear;
    private LocalDate endOfYearDate;
    private String nextAcademicYear;
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
        dto.setAcademicYear(config.getAcademicYear());
        dto.setEndOfYearDate(config.getEndOfYearDate());
        dto.setNextAcademicYear(config.getNextAcademicYear());
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
