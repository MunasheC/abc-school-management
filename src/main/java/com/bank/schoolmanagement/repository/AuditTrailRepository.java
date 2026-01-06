package com.bank.schoolmanagement.repository;

import com.bank.schoolmanagement.entity.AuditTrail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;


@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {
    
    // Find by action type
    Page<AuditTrail> findByAction(String action, Pageable pageable);
    
    // Find by entity type
    Page<AuditTrail> findByEntity(String entity, Pageable pageable);
    
    // Find by username
    Page<AuditTrail> findByUsername(String username, Pageable pageable);
    
    // Find by entity and entity ID
    Page<AuditTrail> findByEntityAndEntityId(String entity, String entityId, Pageable pageable);
    
    // Find by date range
    @Query("SELECT a FROM AuditTrail a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditTrail> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate, 
                                       Pageable pageable);
    
    // Find by action and date range
    @Query("SELECT a FROM AuditTrail a WHERE a.action = :action AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditTrail> findByActionAndDateRange(@Param("action") String action,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                Pageable pageable);
    
    // Find by entity and date range
    @Query("SELECT a FROM AuditTrail a WHERE a.entity = :entity AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditTrail> findByEntityAndDateRange(@Param("entity") String entity,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                Pageable pageable);
}
