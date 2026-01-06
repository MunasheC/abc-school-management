package com.bank.schoolmanagement.service;

import com.bank.schoolmanagement.entity.AuditTrail;
import com.bank.schoolmanagement.repository.AuditTrailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditTrailService {
    private final AuditTrailRepository auditTrailRepository;

    public void logAction(Long userId, String username, String action, String entity, String entityId, String details) {
        logAction(userId, username, action, entity, entityId, details, null, null);
    }

    public void logAction(Long userId, String username, String action, String entity, String entityId, String details, String beforeValue, String afterValue) {
        AuditTrail audit = new AuditTrail();
        audit.setUserId(userId);
        audit.setUsername(username);
        audit.setAction(action);
        audit.setEntity(entity);
        audit.setEntityId(entityId);
        audit.setDetails(details);
        audit.setBeforeValue(beforeValue);
        audit.setAfterValue(afterValue);
        auditTrailRepository.save(audit);
    }
}
