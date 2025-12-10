package com.bank.schoolmanagement.filter;

import com.bank.schoolmanagement.context.SchoolContext;
import com.bank.schoolmanagement.entity.School;
import com.bank.schoolmanagement.repository.SchoolRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * Servlet filter that automatically sets the school context for each request.
 * 
 * This filter:
 * 1. Extracts school information from session or JWT token
 * 2. Sets SchoolContext.setCurrentSchool() for the request
 * 3. Clears the context after request completes (in finally block)
 * 
 * Applied to: /api/school/** endpoints (school-specific operations)
 * Not applied to: /api/bank/** endpoints (cross-school bank operations)
 * 
 * Usage in session (typical for web apps):
 * - School is set during login and stored in HTTP session
 * - Filter retrieves it from session.getAttribute("currentSchool")
 * 
 * Usage with JWT (for mobile/SPA apps):
 * - JWT token contains schoolId claim
 * - Filter extracts schoolId from token and looks up school
 * 
 * @Order(1) ensures this runs before Spring Security filters
 */
@Component
@Order(1)
public class SchoolContextFilter implements Filter {

    @Autowired
    private SchoolRepository schoolRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        
        try {
            // Only apply to school-specific endpoints
            // Bank endpoints (/api/bank/**) handle their own school resolution
            if (requestURI.startsWith("/api/school/")) {
                School school = extractSchoolFromRequest(httpRequest);
                
                if (school == null) {
                    // No school found - return 401 Unauthorized
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write("{\"error\": \"No school context found. Please login.\"}");
                    return;
                }
                
                // Set school context for this request
                SchoolContext.setCurrentSchool(school);
            }
            
            // Continue with the request
            chain.doFilter(request, response);
            
        } finally {
            // CRITICAL: Always clear context after request completes
            // This prevents context leakage between requests
            SchoolContext.clear();
        }
    }

    /**
     * Extracts school from HTTP request.
     * 
     * Priority order:
     * 1. Check session attribute "currentSchoolId" (set during login)
     * 2. Check JWT token "schoolId" claim (for stateless auth)
     * 3. Check request header "X-School-Id" (for testing/debugging)
     * 
     * @param request HTTP request
     * @return School entity or null if not found
     */
    private School extractSchoolFromRequest(HttpServletRequest request) {
        Long schoolId = null;
        
        // Strategy 1: Get from session (typical web app flow)
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object sessionSchoolId = session.getAttribute("currentSchoolId");
            if (sessionSchoolId != null) {
                if (sessionSchoolId instanceof Long) {
                    schoolId = (Long) sessionSchoolId;
                } else if (sessionSchoolId instanceof Integer) {
                    schoolId = ((Integer) sessionSchoolId).longValue();
                } else if (sessionSchoolId instanceof String) {
                    try {
                        schoolId = Long.parseLong((String) sessionSchoolId);
                    } catch (NumberFormatException e) {
                        // Invalid format, continue to next strategy
                    }
                }
            }
        }
        
        // Strategy 2: Get from JWT token (for future implementation)
        // if (schoolId == null) {
        //     String token = extractTokenFromHeader(request);
        //     if (token != null) {
        //         Claims claims = jwtUtil.parseToken(token);
        //         schoolId = claims.get("schoolId", Long.class);
        //     }
        // }
        
        // Strategy 3: Get from custom header (for testing/debugging)
        if (schoolId == null) {
            String headerSchoolId = request.getHeader("X-School-Id");
            if (headerSchoolId != null) {
                try {
                    schoolId = Long.parseLong(headerSchoolId);
                } catch (NumberFormatException e) {
                    // Invalid format, return null
                }
            }
        }
        
        // Lookup school in database
        if (schoolId != null) {
            Optional<School> school = schoolRepository.findById(schoolId);
            return school.orElse(null);
        }
        
        return null;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic if needed
    }

    @Override
    public void destroy() {
        // Cleanup logic if needed
    }
}
