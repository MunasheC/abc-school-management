package com.bank.schoolmanagement.controller;

import com.bank.schoolmanagement.dto.AuditTrailResponse;

import com.bank.schoolmanagement.repository.AuditTrailRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Audit Trail Controller - Endpoints for administrators to view audit logs
 */
@RestController
@RequestMapping("/api/admin/audit-trail")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit Trail (Admin)", description = "Endpoints for administrators to view and filter system audit logs")
public class AuditTrailController {

    private final AuditTrailRepository auditTrailRepository;

    /**
     * Get all audit trail entries (paginated)
     */
    @GetMapping
    @Operation(summary = "Get all audit trail entries", 
               description = "Retrieve all audit logs with pagination support")
    public ResponseEntity<Page<AuditTrailResponse>> getAllAuditTrails(Pageable pageable) {
        log.info("Fetching all audit trail entries");
        Page<AuditTrailResponse> auditTrails = auditTrailRepository.findAll(pageable)
            .map(AuditTrailResponse::fromEntity);
        return ResponseEntity.ok(auditTrails);
    }

    /**
     * Get audit trail by action type
     */
    @GetMapping("/action/{action}")
    @Operation(summary = "Get audit trail by action type",
               description = "Filter audit logs by action type (e.g., CREATE_STUDENT, UPDATE_PAYMENT)")
    public ResponseEntity<Page<AuditTrailResponse>> getAuditTrailByAction(
            @Parameter(description = "Action type to filter by") @PathVariable String action,
            Pageable pageable) {
        log.info("Fetching audit trail for action: {}", action);
        Page<AuditTrailResponse> auditTrails = auditTrailRepository.findByAction(action, pageable)
            .map(AuditTrailResponse::fromEntity);
        return ResponseEntity.ok(auditTrails);
    }

    /**
     * Get audit trail by entity type
     */
    @GetMapping("/entity/{entity}")
    @Operation(summary = "Get audit trail by entity type",
               description = "Filter audit logs by entity type (e.g., Student, Payment, StudentFeeRecord)")
    public ResponseEntity<Page<AuditTrailResponse>> getAuditTrailByEntity(
            @Parameter(description = "Entity type to filter by") @PathVariable String entity,
            Pageable pageable) {
        log.info("Fetching audit trail for entity: {}", entity);
        Page<AuditTrailResponse> auditTrails = auditTrailRepository.findByEntity(entity, pageable)
            .map(AuditTrailResponse::fromEntity);
        return ResponseEntity.ok(auditTrails);
    }

    /**
     * Get audit trail by username
     */
    @GetMapping("/user/{username}")
    @Operation(summary = "Get audit trail by username",
               description = "Filter audit logs by the user who performed the action")
    public ResponseEntity<Page<AuditTrailResponse>> getAuditTrailByUsername(
            @Parameter(description = "Username to filter by") @PathVariable String username,
            Pageable pageable) {
        log.info("Fetching audit trail for username: {}", username);
        Page<AuditTrailResponse> auditTrails = auditTrailRepository.findByUsername(username, pageable)
            .map(AuditTrailResponse::fromEntity);
        return ResponseEntity.ok(auditTrails);
    }

    /**
     * Get audit trail for a specific entity record
     */
    @GetMapping("/entity/{entity}/record/{entityId}")
    @Operation(summary = "Get audit trail for a specific entity record",
               description = "Get all audit logs for a specific entity record (e.g., all actions on Student ID 123)")
    public ResponseEntity<Page<AuditTrailResponse>> getAuditTrailByEntityAndId(
            @Parameter(description = "Entity type") @PathVariable String entity,
            @Parameter(description = "Entity record ID") @PathVariable String entityId,
            Pageable pageable) {
        log.info("Fetching audit trail for entity {} with ID: {}", entity, entityId);
        Page<AuditTrailResponse> auditTrails = auditTrailRepository.findByEntityAndEntityId(entity, entityId, pageable)
            .map(AuditTrailResponse::fromEntity);
        return ResponseEntity.ok(auditTrails);
    }

    /**
     * Get audit trail by date range
     */
    @GetMapping("/date-range")
    @Operation(summary = "Get audit trail by date range",
               description = "Filter audit logs by date range")
    public ResponseEntity<Page<AuditTrailResponse>> getAuditTrailByDateRange(
            @Parameter(description = "Start date (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        log.info("Fetching audit trail from {} to {}", startDate, endDate);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        Page<AuditTrailResponse> auditTrails = auditTrailRepository.findByDateRange(startDateTime, endDateTime, pageable)
            .map(AuditTrailResponse::fromEntity);
        return ResponseEntity.ok(auditTrails);
    }

    /**
     * Get audit trail by action and date range
     */
    @GetMapping("/action/{action}/date-range")
    @Operation(summary = "Get audit trail by action and date range",
               description = "Filter audit logs by action type within a specific date range")
    public ResponseEntity<Page<AuditTrailResponse>> getAuditTrailByActionAndDateRange(
            @Parameter(description = "Action type") @PathVariable String action,
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        log.info("Fetching audit trail for action {} from {} to {}", action, startDate, endDate);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        Page<AuditTrailResponse> auditTrails = auditTrailRepository.findByActionAndDateRange(
            action, startDateTime, endDateTime, pageable)
            .map(AuditTrailResponse::fromEntity);
        return ResponseEntity.ok(auditTrails);
    }

    /**
     * Get audit trail by entity and date range
     */
    @GetMapping("/entity/{entity}/date-range")
    @Operation(summary = "Get audit trail by entity and date range",
               description = "Filter audit logs by entity type within a specific date range")
    public ResponseEntity<Page<AuditTrailResponse>> getAuditTrailByEntityAndDateRange(
            @Parameter(description = "Entity type") @PathVariable String entity,
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        log.info("Fetching audit trail for entity {} from {} to {}", entity, startDate, endDate);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        Page<AuditTrailResponse> auditTrails = auditTrailRepository.findByEntityAndDateRange(
            entity, startDateTime, endDateTime, pageable)
            .map(AuditTrailResponse::fromEntity);
        return ResponseEntity.ok(auditTrails);
    }

    /**
     * Get audit trail entry by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get audit trail entry by ID",
               description = "Retrieve a specific audit log entry by its ID")
    public ResponseEntity<AuditTrailResponse> getAuditTrailById(
            @Parameter(description = "Audit trail entry ID") @PathVariable Long id) {
        log.info("Fetching audit trail entry with ID: {}", id);
        return auditTrailRepository.findById(id)
            .map(AuditTrailResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
