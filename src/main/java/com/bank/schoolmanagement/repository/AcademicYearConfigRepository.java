package com.bank.schoolmanagement.repository;

import com.bank.schoolmanagement.entity.AcademicYearConfig;
import com.bank.schoolmanagement.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearConfigRepository extends JpaRepository<AcademicYearConfig, Long> {
    
    /**
     * Find configuration by school and academic year
     */
    Optional<AcademicYearConfig> findBySchoolAndYear(School school, Integer year);
    
    /**
     * Find all configurations for a school
     */
    List<AcademicYearConfig> findBySchoolOrderByEndOfYearDateDesc(School school);
    
    /**
     * Find active configurations for a school
     */
    List<AcademicYearConfig> findBySchoolAndIsActiveTrueOrderByEndOfYearDateDesc(School school);
    
    /**
     * Find latest active configuration for a school
     */
    Optional<AcademicYearConfig> findFirstBySchoolAndIsActiveTrueOrderByEndOfYearDateDesc(School school);
    
    /**
     * Find all scheduled promotions that should be triggered today
     * Used by scheduled task
     */
    @Query("SELECT a FROM AcademicYearConfig a WHERE " +
           "a.isActive = true AND " +
           "a.promotionStatus = 'SCHEDULED' AND " +
           "a.endOfYearDate <= :currentDate")
    List<AcademicYearConfig> findPromotionsDueByDate(LocalDate currentDate);
    
    /**
     * Find configurations by status
     */
    List<AcademicYearConfig> findByPromotionStatusOrderByEndOfYearDateDesc(String status);
}
