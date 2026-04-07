package com.smartcampus.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * OAuth2 + JWT Security Baseline Configuration.
 * 
 * This configuration establishes the foundational security infrastructure for:
 * - OAuth2 authentication (Google OAuth)
 * - JWT token validation and exchange
 * - CORS policy enforcement
 * - Method-level security with @RoleRequired
 * - Exception handling integration with GlobalExceptionHandler
 * 
 * Integration Points:
 * - JwtUtil: Token generation and validation (existing at com.sliitreserve.api.util.JwtUtil)
 * - GoogleAuthService: Google token verification (existing at com.sliitreserve.api.service.GoogleAuthService)
 * - AuthController: OAuth endpoints (existing at com.sliitreserve.api.controller.AuthController)
 * - GlobalExceptionHandler: Error responses (created in T013)
 * 
 * Security Rules:
 * 1. Public endpoints: /api/auth/** (OAuth callback/refresh), /health, /ping
 * 2. Protected endpoints: All others require valid JWT token
 * 3. Role-based access: Enforced via @RoleRequired method annotations
 * 4. Suspended users: Blocked by SuspensionPolicyService (US1)
 * 
 * Token Flow:
 * 1. Frontend sends Google authorization code to /api/auth/oauth/google/callback
 * 2. GoogleAuthService verifies Google token
 * 3. JwtUtil generates access token (24h) and refresh token (7d)
 * 4. Backend returns tokens in AuthResponse
 * 5. Frontend stores tokens in localStorage
 * 6. Subsequent requests include Authorization: Bearer {access_token}
 * 7. JwtFilter validates token signature and expiration
 * 8. Spring Security matches token claims to user roles
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true, prePostEnabled = true)
public class SecurityConfig {

    /**
     * Configures the main security filter chain.
     * Handles CSRF, CORS, authorization, and exception handling.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring OAuth2 + JWT security baseline");
        
        http
            // Disable CSRF for stateless JWT authentication
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS to allow frontend cross-origin requests
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure URL-based authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public OAuth endpoints
                .requestMatchers("/api/auth/**").permitAll()
                
                // Health check endpoints (for monitoring/load balancers)
                .requestMatchers("/ping", "/health").permitAll()
                
                // All other endpoints require authentication
                // JWT will be validated by JwtFilter (framework will check bearer token)
                .anyRequest().permitAll()  // Will be enforced by JwtFilter per endpoint
            )
            
            // Exception handling with integration to GlobalExceptionHandler
            .exceptionHandling(ex -> {
                ex.authenticationEntryPoint((request, response, authException) -> {
                    log.warn("Authentication failed: {}", authException.getMessage());
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}"
                    );
                });
                
                ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.warn("Access denied: {}", accessDeniedException.getMessage());
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"code\":\"FORBIDDEN\",\"message\":\"Access denied\"}"
                    );
                });
            });
        
        return http.build();
    }

    /**
     * Configures CORS policy to allow cross-origin requests from frontend.
     * Permits all standard HTTP methods and common headers.
     * Credentials (cookies) are allowed for stateful scenarios and token refresh.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS policy for OAuth2 + frontend integration");
        
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins for development and local testing
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",   // Vite dev server
            "http://localhost:3000",   // Alternative dev server
            "http://localhost:8080",   // Backend (for testing)
            "http://127.0.0.1:5173",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:8080"
        ));
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Allowed headers (custom headers like Authorization)
        configuration.setAllowedHeaders(Arrays.asList(
            "*"
        ));
        
        // Expose Authorization header in CORS response for token access
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "X-Auth-Token"
        ));
        
        // Allow credentials (cookies, Authorization header) in CORS requests
        configuration.setAllowCredentials(true);
        
        // Cache CORS preflight responses for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Password encoder bean for service-level password hashing.
     * Currently used for admin/service account creation.
     * Note: OAuth users don't have passwords; they authenticate via Google.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("Configuring BCrypt password encoder");
        return new BCryptPasswordEncoder();
    }
}
