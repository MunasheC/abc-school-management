package com.bank.schoolmanagement.service;

import com.bank.schoolmanagement.context.SchoolContext;
import com.bank.schoolmanagement.entity.Guardian;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.repository.GuardianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Guardian Service - Business logic for guardians (parents)
 * 
 * LEARNING: Service layer for Guardian entity
 * - Manages parent/guardian records
 * - Handles sibling relationships
 * - Prevents duplicate guardians
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuardianService {

    private final GuardianRepository guardianRepository;

    /**
     * Create or get existing guardian
     * 
     * LEARNING: Smart creation - checks if guardian exists first
     * If phone number already exists, returns existing guardian
     * This prevents duplicate parent records for siblings
     * 
     * @param guardian - Guardian to create
     * @return Guardian - New or existing guardian
     */
    @Transactional
    public Guardian createOrGetGuardian(Guardian guardian) {
        log.info("Creating or getting guardian: {}", guardian.getFullName());

        // Check if guardian already exists by primary phone
        if (guardian.getPrimaryPhone() != null) {
            Optional<Guardian> existing = guardianRepository.findByPrimaryPhone(guardian.getPrimaryPhone());
            if (existing.isPresent()) {
                log.info("Guardian already exists with phone {}, returning existing", guardian.getPrimaryPhone());
                return existing.get();
            }
        }

        // Create new guardian
        Guardian savedGuardian = guardianRepository.save(guardian);
        log.info("Created new guardian with ID: {}", savedGuardian.getId());
        return savedGuardian;
    }

    /**
     * Create guardian (force new record)
     */
    @Transactional
    public Guardian createGuardian(Guardian guardian) {
        log.info("Creating guardian: {}", guardian.getFullName());
        
        // Check for duplicate phone
        if (guardian.getPrimaryPhone() != null && 
            guardianRepository.existsByPrimaryPhone(guardian.getPrimaryPhone())) {
            throw new IllegalArgumentException("Guardian with phone " + guardian.getPrimaryPhone() + " already exists");
        }
        
        return guardianRepository.save(guardian);
    }

    /**
     * Get guardian by ID
     */
    public Optional<Guardian> getGuardianById(Long id) {
        log.debug("Fetching guardian with ID: {}", id);
        return guardianRepository.findById(id);
    }

    /**
     * Get guardian by phone
     */
    public Optional<Guardian> getGuardianByPhone(String phone) {
        log.debug("Fetching guardian with phone: {}", phone);
        return guardianRepository.findByPrimaryPhone(phone);
    }

    /**
     * Get guardian by email
     */
    public Optional<Guardian> getGuardianByEmail(String email) {
        log.debug("Fetching guardian with email: {}", email);
        return guardianRepository.findByEmail(email);
    }

    /**
     * Get all guardians
     */
    public List<Guardian> getAllGuardians() {
        log.debug("Fetching all guardians");
        return guardianRepository.findAll();
    }

    /**
     * Get active guardians only
     */
    public List<Guardian> getActiveGuardians() {
        log.debug("Fetching active guardians");
        return guardianRepository.findByIsActiveTrue();
    }

    /**
     * Search guardians by name
     */
    public List<Guardian> searchGuardiansByName(String searchTerm) {
        log.debug("Searching guardians by name: {}", searchTerm);
        return guardianRepository.searchByName(searchTerm);
    }

    /**
     * Get guardians with multiple children (siblings)
     */
    public List<Guardian> getGuardiansWithMultipleChildren() {
        log.debug("Fetching guardians with multiple children");
        return guardianRepository.findGuardiansWithMultipleChildren();
    }

    /**
     * Update guardian information
     */
    @Transactional
    public Guardian updateGuardian(Long id, Guardian updatedGuardian) {
        log.info("Updating guardian with ID: {}", id);
        
        return guardianRepository.findById(id)
                .map(existing -> {
                    // Update fields
                    if (updatedGuardian.getFullName() != null) {
                        existing.setFullName(updatedGuardian.getFullName());
                    }
                    if (updatedGuardian.getPrimaryPhone() != null) {
                        existing.setPrimaryPhone(updatedGuardian.getPrimaryPhone());
                    }
                    if (updatedGuardian.getSecondaryPhone() != null) {
                        existing.setSecondaryPhone(updatedGuardian.getSecondaryPhone());
                    }
                    if (updatedGuardian.getEmail() != null) {
                        existing.setEmail(updatedGuardian.getEmail());
                    }
                    if (updatedGuardian.getAddress() != null) {
                        existing.setAddress(updatedGuardian.getAddress());
                    }
                    if (updatedGuardian.getOccupation() != null) {
                        existing.setOccupation(updatedGuardian.getOccupation());
                    }
                    if (updatedGuardian.getEmployer() != null) {
                        existing.setEmployer(updatedGuardian.getEmployer());
                    }
                    
                    Guardian saved = guardianRepository.save(existing);
                    log.info("Guardian updated successfully");
                    return saved;
                })
                .orElseThrow(() -> {
                    log.error("Guardian not found with ID: {}", id);
                    return new IllegalArgumentException("Guardian not found with ID: " + id);
                });
    }

    /**
     * Deactivate guardian
     * 
     * NOTE: This doesn't delete the guardian or their students
     * Just marks as inactive
     */
    @Transactional
    public Guardian deactivateGuardian(Long id) {
        log.info("Deactivating guardian with ID: {}", id);
        
        return guardianRepository.findById(id)
                .map(guardian -> {
                    guardian.setIsActive(false);
                    Guardian saved = guardianRepository.save(guardian);
                    log.info("Guardian deactivated successfully");
                    return saved;
                })
                .orElseThrow(() -> {
                    log.error("Guardian not found with ID: {}", id);
                    return new IllegalArgumentException("Guardian not found with ID: " + id);
                });
    }

    /**
     * Reactivate guardian
     */
    @Transactional
    public Guardian reactivateGuardian(Long id) {
        log.info("Reactivating guardian with ID: {}", id);
        
        return guardianRepository.findById(id)
                .map(guardian -> {
                    guardian.setIsActive(true);
                    Guardian saved = guardianRepository.save(guardian);
                    log.info("Guardian reactivated successfully");
                    return saved;
                })
                .orElseThrow(() -> {
                    log.error("Guardian not found with ID: {}", id);
                    return new IllegalArgumentException("Guardian not found with ID: " + id);
                });
    }

    /**
     * Delete guardian
     * 
     * WARNING: This will NOT delete students (they remain as orphans)
     * Consider unlinking students first or using soft delete (deactivate)
     */
    @Transactional
    public void deleteGuardian(Long id) {
        log.warn("Deleting guardian with ID: {}", id);
        guardianRepository.deleteById(id);
        log.info("Guardian deleted");
    }

    /**
     * Get children (students) for a guardian
     */
    public List<Student> getChildrenForGuardian(Long guardianId) {
        log.debug("Fetching children for guardian ID: {}", guardianId);
        
        return guardianRepository.findById(guardianId)
                .map(Guardian::getStudents)
                .orElseThrow(() -> {
                    log.error("Guardian not found with ID: {}", guardianId);
                    return new IllegalArgumentException("Guardian not found with ID: " + guardianId);
                });
    }

    /**
     * Count children for a guardian
     */
    public int countChildren(Long guardianId) {
        log.debug("Counting children for guardian ID: {}", guardianId);
        
        return guardianRepository.findById(guardianId)
                .map(Guardian::getChildrenCount)
                .orElse(0);
    }

    /* ----------------------  MULTI-TENANT METHODS (School-Aware)  ------------------------- */

    /**
     * Get all guardians for current school
     */
    public List<Guardian> getAllGuardiansForCurrentSchool() {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching guardians for school: {}", currentSchool.getSchoolName());
        return guardianRepository.findBySchool(currentSchool);
    }

    /**
     * Get guardian by phone within current school
     */
    public Optional<Guardian> getGuardianByPhoneForCurrentSchool(String phone) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching guardian with phone {} in school: {}", phone, currentSchool.getSchoolName());
        return guardianRepository.findBySchoolAndPrimaryPhone(currentSchool, phone);
    }

    /**
     * Search guardians by name within current school
     */
    public List<Guardian> searchGuardiansByNameForCurrentSchool(String searchTerm) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Searching guardians with '{}' in school: {}", searchTerm, currentSchool.getSchoolName());
        return guardianRepository.searchBySchoolAndName(currentSchool, searchTerm);
    }

    /**
     * Get guardians with multiple children in current school
     */
    public List<Guardian> getGuardiansWithMultipleChildrenForCurrentSchool() {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.debug("Fetching guardians with multiple children in school: {}", currentSchool.getSchoolName());
        return guardianRepository.findGuardiansWithMultipleChildrenBySchool(currentSchool);
    }

    /**
     * Create or get guardian within current school
     * 
     * CRITICAL: Checks phone uniqueness within school, not globally
     */
    @Transactional
    public Guardian createOrGetGuardianForCurrentSchool(Guardian guardian) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Creating or getting guardian {} in school: {}", 
                guardian.getFullName(), currentSchool.getSchoolName());

        // Set school
        guardian.setSchool(currentSchool);

        // Check if guardian exists by phone in this school
        if (guardian.getPrimaryPhone() != null) {
            Optional<Guardian> existing = guardianRepository.findBySchoolAndPrimaryPhone(
                currentSchool, guardian.getPrimaryPhone()
            );
            if (existing.isPresent()) {
                log.info("Guardian already exists with phone {} in school {}", 
                        guardian.getPrimaryPhone(), currentSchool.getSchoolName());
                return existing.get();
            }
        }

        // Create new guardian
        Guardian savedGuardian = guardianRepository.save(guardian);
        log.info("Created new guardian with ID: {} in school: {}", 
                savedGuardian.getId(), currentSchool.getSchoolName());
        return savedGuardian;
    }

    /**
     * Create guardian in current school (force new record)
     */
    @Transactional
    public Guardian createGuardianForCurrentSchool(Guardian guardian) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Creating guardian {} in school: {}", 
                guardian.getFullName(), currentSchool.getSchoolName());
        
        // Set school
        guardian.setSchool(currentSchool);
        
        // Check for duplicate phone in this school
        if (guardian.getPrimaryPhone() != null && 
            guardianRepository.existsBySchoolAndPrimaryPhone(currentSchool, guardian.getPrimaryPhone())) {
            throw new IllegalArgumentException(
                "Guardian with phone " + guardian.getPrimaryPhone() + 
                " already exists in school " + currentSchool.getSchoolName()
            );
        }
        
        return guardianRepository.save(guardian);
    }

    /**
     * Update guardian in current school
     * 
     * SECURITY: Validates guardian belongs to current school
     */
    @Transactional
    public Guardian updateGuardianForCurrentSchool(Long id, Guardian updatedGuardian) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Updating guardian {} in school: {}", id, currentSchool.getSchoolName());
        
        return guardianRepository.findById(id)
                .map(existing -> {
                    // Validate school ownership
                    SchoolContext.validateSchoolAccess(existing.getSchool());
                    
                    // Update fields
                    if (updatedGuardian.getFullName() != null) {
                        existing.setFullName(updatedGuardian.getFullName());
                    }
                    if (updatedGuardian.getPrimaryPhone() != null) {
                        existing.setPrimaryPhone(updatedGuardian.getPrimaryPhone());
                    }
                    if (updatedGuardian.getSecondaryPhone() != null) {
                        existing.setSecondaryPhone(updatedGuardian.getSecondaryPhone());
                    }
                    if (updatedGuardian.getEmail() != null) {
                        existing.setEmail(updatedGuardian.getEmail());
                    }
                    if (updatedGuardian.getAddress() != null) {
                        existing.setAddress(updatedGuardian.getAddress());
                    }
                    if (updatedGuardian.getOccupation() != null) {
                        existing.setOccupation(updatedGuardian.getOccupation());
                    }
                    if (updatedGuardian.getEmployer() != null) {
                        existing.setEmployer(updatedGuardian.getEmployer());
                    }
                    
                    Guardian saved = guardianRepository.save(existing);
                    log.info("Guardian {} updated in school: {}", id, currentSchool.getSchoolName());
                    return saved;
                })
                .orElseThrow(() -> new RuntimeException("Guardian not found with ID: " + id));
    }

    /**
     * Delete guardian from current school
     * 
     * SECURITY: Validates guardian belongs to current school
     */
    @Transactional
    public void deleteGuardianForCurrentSchool(Long id) {
        School currentSchool = SchoolContext.getCurrentSchool();
        log.info("Deleting guardian {} from school: {}", id, currentSchool.getSchoolName());
        
        guardianRepository.findById(id).ifPresent(guardian -> {
            // Validate school ownership
            SchoolContext.validateSchoolAccess(guardian.getSchool());
            
            guardianRepository.deleteById(id);
            log.info("Guardian {} deleted from school: {}", id, currentSchool.getSchoolName());
        });
    }

    /**
     * Count guardians in current school
     */
    public long countGuardiansForCurrentSchool() {
        School currentSchool = SchoolContext.getCurrentSchool();
        return guardianRepository.countBySchool(currentSchool);
    }
}
