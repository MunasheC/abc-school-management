package com.bank.schoolmanagement.context;

import com.bank.schoolmanagement.entity.School;

/**
 * SchoolContext - Thread-local storage for current school
 * 
 * PURPOSE: Multi-tenant context management
 * - Stores current school in thread-local storage
 * - Each request thread has its own school context
 * - Automatically used by services to filter data
 * 
 * LEARNING: ThreadLocal explained
 * - ThreadLocal provides thread-isolated storage
 * - Each HTTP request runs in its own thread
 * - School set in one request won't affect other requests
 * - Must be cleaned up after request completes (memory leak prevention)
 * 
 * USAGE:
 * 1. Controller/Filter sets school at request start
 * 2. Services use getCurrentSchool() to filter queries
 * 3. Filter/Interceptor clears school at request end
 */
public class SchoolContext {

    /**
     * Thread-local storage for current school
     * 
     * LEARNING: Why ThreadLocal?
     * - Web servers handle multiple requests simultaneously
     * - Each request runs in separate thread
     * - ThreadLocal ensures each thread has its own school value
     * - No interference between concurrent requests
     */
    private static final ThreadLocal<School> currentSchool = new ThreadLocal<>();

    /**
     * Set the current school for this thread/request
     * 
     * Called by:
     * - Authentication filter after user login
     * - Controller methods for testing
     * - School admin selecting active school
     * 
     * @param school The school to set as current
     */
    public static void setCurrentSchool(School school) {
        currentSchool.set(school);
        
        if (school != null) {
            System.out.println("🏫 School context set: " + school.getSchoolName() + 
                             " (Code: " + school.getSchoolCode() + ")");
        }
    }

    /**
     * Get the current school for this thread/request
     * 
     * Called by:
     * - All service methods to filter data by school
     * - Controllers to display school info
     * 
     * @return Current school, or null if not set
     * @throws IllegalStateException if no school in context (should never happen in production)
     */
    public static School getCurrentSchool() {
        School school = currentSchool.get();
        
        if (school == null) {
            throw new IllegalStateException(
                "No school in context! Ensure authentication filter sets school before accessing data."
            );
        }
        
        return school;
    }

    /**
     * Get current school without throwing exception
     * 
     * Used when school might legitimately not be set
     * (e.g., bank admin endpoints that work across all schools)
     * 
     * @return Current school, or null if not set
     */
    public static School getCurrentSchoolOrNull() {
        return currentSchool.get();
    }

    /**
     * Check if school context is set
     * 
     * @return true if school is set, false otherwise
     */
    public static boolean hasSchool() {
        return currentSchool.get() != null;
    }

    /**
     * Clear the current school from this thread
     * 
     * CRITICAL: Must be called at end of each request!
     * - Prevents memory leaks
     * - Threads are reused (thread pool)
     * - Old school value could leak to next request
     * 
     * Called by:
     * - Filter in finally block (guaranteed cleanup)
     * - @AfterEach in tests
     */
    public static void clear() {
        School school = currentSchool.get();
        
        if (school != null) {
            System.out.println("🧹 Clearing school context: " + school.getSchoolName());
        }
        
        currentSchool.remove();
    }

    /**
     * Get current school code (convenience method)
     * 
     * @return School code, or null if no school set
     */
    public static String getCurrentSchoolCode() {
        School school = getCurrentSchoolOrNull();
        return school != null ? school.getSchoolCode() : null;
    }

    /**
     * Get current school ID (convenience method)
     * 
     * @return School ID, or null if no school set
     */
    public static Long getCurrentSchoolId() {
        School school = getCurrentSchoolOrNull();
        return school != null ? school.getId() : null;
    }

    /**
     * Validate that entity belongs to current school
     * 
     * CRITICAL: Security check to prevent cross-school data access
     * 
     * @param entitySchool The school from the entity being accessed
     * @throws SecurityException if entity belongs to different school
     */
    public static void validateSchoolAccess(School entitySchool) {
        School currentSchool = getCurrentSchool();
        
        if (!currentSchool.equals(entitySchool)) {
            throw new SecurityException(
                "Access denied: Entity belongs to school " + entitySchool.getSchoolCode() + 
                ", current school is " + currentSchool.getSchoolCode()
            );
        }
    }

    /**
     * Check if current user is bank admin (no school restriction)
     * 
     * Bank admins can access data across all schools
     * 
     * @return true if no school in context (bank admin)
     */
    public static boolean isBankAdmin() {
        return currentSchool.get() == null;
    }
}
