package com.bank.schoolmanagement.dto;

import com.bank.schoolmanagement.entity.AuditTrail;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for AuditTrail API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditTrailResponse {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String entity;
    private String entityId;
    private String details;
    private String beforeValue;
    private String afterValue;
    private LocalDateTime timestamp;

    public static AuditTrailResponse fromEntity(AuditTrail auditTrail) {
        AuditTrailResponse dto = new AuditTrailResponse();
        dto.setId(auditTrail.getId());
        dto.setUserId(auditTrail.getUserId());
        dto.setUsername(auditTrail.getUsername());
        dto.setAction(auditTrail.getAction());
        dto.setEntity(auditTrail.getEntity());
        dto.setEntityId(auditTrail.getEntityId());
        dto.setDetails(auditTrail.getDetails());
        dto.setBeforeValue(auditTrail.getBeforeValue());
        dto.setAfterValue(auditTrail.getAfterValue());
        dto.setTimestamp(auditTrail.getTimestamp());
        return dto;
    }
}
