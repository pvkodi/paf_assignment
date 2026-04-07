package com.smartcampus.config.security;

import com.smartcampus.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT Authentication Filter for Spring Security.
 * Intercepts HTTP requests and validates JWT tokens in Authorization header.
 *
 * Filter Chain:
 * 1. Extract JWT from Authorization: Bearer {token} header
 * 2. Validate token signature and expiration
 * 3. Extract email (username) from token
 * 4. Create Authentication object
 * 5. Set in SecurityContextHolder for downstream processing
 *
 * Applies to: All requests (configured in SecurityConfig)
 * Skip: Public endpoints (/api/auth/**, /health, /ping, /error)
 *
 * Error Handling:
 * - Missing/invalid Authorization header: Request proceeds without authentication
 * - Invalid token signature: Logged as warning, request continues unauthenticated
 * - Expired token: Logged as debug, request continues unauthenticated
 * - Framework returns 401 if protected endpoint accessed without valid authentication
 *
 * Integration with SecurityConfig:
 * SecurityConfig filters out protected endpoints after this filter runs.
 * GlobalExceptionHandler catches authentication failures and returns proper error responses.
 *
 * Example Token Flow:
 * <pre>
 * Frontend -> Authorization: Bearer eyJhbGc... (access token)
 *   ↓
 * JwtAuthenticationFilter.doFilterInternal()
 *   ↓
 * Extract token: "eyJhbGc..."
 *   ↓
 * JwtUtil.validateToken(token) → true
 *   ↓
 * JwtUtil.getEmailFromToken(token) → "user@example.com"
 *   ↓
 * Create UsernamePasswordAuthenticationToken("user@example.com", null, [])
 *   ↓
 * SecurityContextHolder.setContext(authentication)
 *   ↓
 * Next filter in chain (authorization checks run here)
 * </pre>
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Filter method called once per request.
     * Attempts to extract and validate JWT token from Authorization header.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Filter chain for request delegation
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Extract JWT token from Authorization header
            String token = extractTokenFromRequest(request);
            
            // If token exists and is valid, set authentication in context
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                setAuthentication(token);
            } else if (StringUtils.hasText(token)) {
                log.warn("JWT token validation failed for request: {}", request.getRequestURI());
            }
            
        } catch (JwtException e) {
            log.warn("JWT exception in filter: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in JWT filter: {}", e.getMessage(), e);
        }
        
        // Continue filter chain regardless of authentication result
        // Authorization errors will be caught by Spring Security or GlobalExceptionHandler
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header.
     * Expected format: "Authorization: Bearer {token}"
     *
     * @param request HTTP request
     * @return JWT token if present, null otherwise
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * Set authentication in SecurityContextHolder based on JWT token.
     * Creates UsernamePasswordAuthenticationToken with email as principal.
     *
     * @param token JWT token
     */
    private void setAuthentication(String token) {
        try {
            String email = jwtUtil.getEmailFromToken(token);
            
            if (StringUtils.hasText(email)) {
                // Create authentication with email as principal, no credentials, empty authorities
                // Roles/authorities will be loaded from User entity (T027) for detailed RBAC
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    email,
                    null,  // No credentials stored
                    new ArrayList<>()  // Authorities loaded later from User entity
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT authentication set for user: {}", email);
                
            } else {
                log.warn("Could not extract email from valid JWT token");
            }
            
        } catch (Exception e) {
            log.error("Error setting authentication from JWT: {}", e.getMessage(), e);
        }
    }
}
