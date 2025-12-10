package com.bank.schoolmanagement.controller;

import com.bank.schoolmanagement.entity.Guardian;
import com.bank.schoolmanagement.entity.Student;
import com.bank.schoolmanagement.service.GuardianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Guardian Controller - REST API for Parents/Guardians
 * 
 * BASE PATH: /api/guardians
 * 
 * LEARNING: Separate controller for guardian operations
 * - Manages parent/guardian information
 * - Supports sibling queries (students sharing same guardian)
 * - Prevents duplicate parent records
 */
@RestController
@RequestMapping("/api/school/guardians")
@RequiredArgsConstructor
@Slf4j
public class GuardianController {

    private final GuardianService guardianService;

    /**
     * CREATE - Add a new guardian
     * 
     * POST /api/guardians
     * 
     * Body example:
     * {
     *   "fullName": "Jane Doe",
     *   "primaryPhone": "+263771234567",
     *   "secondaryPhone": "+263779876543",
     *   "email": "jane.doe@email.com",
     *   "address": "123 Main Street, Harare",
     *   "occupation": "Teacher",
     *   "employer": "ABC School"
     * }
     */
    @PostMapping
    public ResponseEntity<Guardian> createGuardian(@Valid @RequestBody Guardian guardian) {
        log.info("REST request to create guardian: {}", guardian.getFullName());
        
        try {
            Guardian createdGuardian = guardianService.createGuardianForCurrentSchool(guardian);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdGuardian);
        } catch (IllegalArgumentException e) {
            log.error("Error creating guardian: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * READ - Get all guardians
     * 
     * GET /api/guardians
     */
    @GetMapping
    public ResponseEntity<List<Guardian>> getAllGuardians() {
        log.info("REST request to get all guardians");
        List<Guardian> guardians = guardianService.getAllGuardiansForCurrentSchool();
        return ResponseEntity.ok(guardians);
    }

    /**
     * READ - Get active guardians only
     * 
     * GET /api/guardians/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<Guardian>> getActiveGuardians() {
        log.info("REST request to get active guardians");
        List<Guardian> guardians = guardianService.getActiveGuardians();
        return ResponseEntity.ok(guardians);
    }

    /**
     * READ - Get guardian by ID
     * 
     * GET /api/guardians/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Guardian> getGuardianById(@PathVariable Long id) {
        log.info("REST request to get guardian with ID: {}", id);
        return guardianService.getGuardianById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Get guardian by phone number
     * 
     * GET /api/guardians/phone/{phone}
     * 
     * Useful for checking if guardian already exists before creating
     */
    @GetMapping("/phone/{phone}")
    public ResponseEntity<Guardian> getGuardianByPhone(@PathVariable String phone) {
        log.info("REST request to get guardian with phone: {}", phone);
        return guardianService.getGuardianByPhoneForCurrentSchool(phone)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Get guardian by email
     * 
     * GET /api/guardians/email/{email}
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<Guardian> getGuardianByEmail(@PathVariable String email) {
        log.info("REST request to get guardian with email: {}", email);
        return guardianService.getGuardianByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Search guardians by name
     * 
     * GET /api/guardians/search?name=Doe
     * 
     * Searches in fullName field using LIKE query
     */
    @GetMapping("/search")
    public ResponseEntity<List<Guardian>> searchGuardians(@RequestParam String name) {
        log.info("REST request to search guardians by name: {}", name);
        List<Guardian> guardians = guardianService.searchGuardiansByName(name);
        return ResponseEntity.ok(guardians);
    }

    /**
     * READ - Get guardians with multiple children (siblings)
     * 
     * GET /api/guardians/with-multiple-children
     * 
     * LEARNING: Returns guardians who have 2+ students
     * Useful for sibling discount calculations
     */
    @GetMapping("/with-multiple-children")
    public ResponseEntity<List<Guardian>> getGuardiansWithMultipleChildren() {
        log.info("REST request to get guardians with multiple children");
        List<Guardian> guardians = guardianService.getGuardiansWithMultipleChildrenForCurrentSchool();
        return ResponseEntity.ok(guardians);
    }

    /**
     * READ - Get children (students) for a guardian
     * 
     * GET /api/guardians/{id}/children
     * 
     * Returns all students belonging to this guardian
     */
    @GetMapping("/{id}/children")
    public ResponseEntity<List<Student>> getChildrenForGuardian(@PathVariable Long id) {
        log.info("REST request to get children for guardian ID: {}", id);
        
        try {
            List<Student> children = guardianService.getChildrenForGuardian(id);
            return ResponseEntity.ok(children);
        } catch (IllegalArgumentException e) {
            log.error("Guardian not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * READ - Count children for a guardian
     * 
     * GET /api/guardians/{id}/children/count
     */
    @GetMapping("/{id}/children/count")
    public ResponseEntity<Integer> countChildren(@PathVariable Long id) {
        log.info("REST request to count children for guardian ID: {}", id);
        int count = guardianService.countChildren(id);
        return ResponseEntity.ok(count);
    }

    /**
     * UPDATE - Update guardian information
     * 
     * PUT /api/guardians/{id}
     * 
     * Supports partial updates - send only fields to update
     */
    @PutMapping("/{id}")
    public ResponseEntity<Guardian> updateGuardian(
            @PathVariable Long id,
            @RequestBody Guardian guardian) {
        log.info("REST request to update guardian with ID: {}", id);
        
        try {
            Guardian updatedGuardian = guardianService.updateGuardianForCurrentSchool(id, guardian);
            return ResponseEntity.ok(updatedGuardian);
        } catch (IllegalArgumentException e) {
            log.error("Error updating guardian: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DEACTIVATE - Soft delete guardian
     * 
     * POST /api/guardians/{id}/deactivate
     * 
     * LEARNING: Doesn't delete guardian or students
     * Just marks as inactive for record keeping
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Guardian> deactivateGuardian(@PathVariable Long id) {
        log.info("REST request to deactivate guardian with ID: {}", id);
        
        try {
            Guardian deactivated = guardianService.deactivateGuardian(id);
            return ResponseEntity.ok(deactivated);
        } catch (IllegalArgumentException e) {
            log.error("Error deactivating guardian: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * REACTIVATE - Restore deactivated guardian
     * 
     * POST /api/guardians/{id}/reactivate
     */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Guardian> reactivateGuardian(@PathVariable Long id) {
        log.info("REST request to reactivate guardian with ID: {}", id);
        
        try {
            Guardian reactivated = guardianService.reactivateGuardian(id);
            return ResponseEntity.ok(reactivated);
        } catch (IllegalArgumentException e) {
            log.error("Error reactivating guardian: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE - Permanently remove guardian
     * 
     * DELETE /api/guardians/{id}
     * 
     * WARNING: This doesn't delete students
     * Students remain as "orphans" in database
     * Consider using deactivate instead
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuardian(@PathVariable Long id) {
        log.warn("REST request to DELETE guardian with ID: {}", id);
        guardianService.deleteGuardianForCurrentSchool(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Health check
     * 
     * GET /api/guardians/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Guardian API is running! 👨‍👩‍👧‍👦");
    }
}
