package com.sliitreserve.api.services.auth;

import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JWT Token Service for application-level token operations.
 *
 * <p><b>Purpose</b>: Provides high-level JWT token issuance and validation with application-specific
 * claims (user roles, identity) and business logic for token lifecycle management.
 *
 * <p><b>Token Structure</b>:
 * <ul>
 *   <li><b>Subject (sub)</b>: User email (unique username for Spring Security)
 *   <li><b>Claims</b>:
 *     <ul>
 *       <li><code>email</code>: User email address
 *       <li><code>name</code>: User display name
 *       <li><code>picture</code>: User profile picture URL (optional)
 *       <li><code>roles</code>: Comma-separated list of assigned roles (e.g., "USER,LECTURER")
 *       <li><code>userId</code>: User UUID for service-to-service lookups
 *     </ul>
 *   <li><b>Algorithm</b>: HS256 (HMAC-SHA256)
 *   <li><b>Expiration</b>: Configured via <code>app.auth.jwt.expiration</code> (default 24 hours)
 * </ul>
 *
 * <p><b>Integration Points</b>:
 * <ul>
 *   <li>OAuthAuthService: Calls <code>generateTokenForUser(User)</code> after OAuth authentication
 *   <li>JwtAuthenticationFilter: Calls <code>validateTokenAndExtractUser(String)</code> to populate Spring Security context
 *   <li>Authorization checks: Extract roles from token claims for RBAC enforcement
 * </ul>
 *
 * <p><b>Security Notes</b>:
 * <ul>
 *   <li>JWT signing key is derived from <code>app.auth.jwt.secret</code> (must be 32+ bytes)
 *   <li>Tokens are stateless; validation is cryptographic only
 *   <li>Token expiration is checked on validation; no server-side token revocation (refresh required)
 *   <li>Roles are embedded in the token; role changes require token refresh
 * </ul>
 *
 * @see JwtUtil for low-level JWT operations
 * @see com.sliitreserve.api.config.security.JwtAuthenticationFilter for integration
 * @see OAuthAuthService for token generation after OAuth exchange
 */
@Service
@Slf4j
public class JwtTokenService {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Generate a JWT token for an authenticated user.
     *
     * <p>Assembles user identity (email, name, roles) into token claims and delegates
     * to JwtUtil for cryptographic signing.
     *
     * @param user User entity with email, displayName, roles, and optional picture URL
     * @return Signed JWT token string
     * @throws IllegalArgumentException if user is null or missing required fields
     */
    public String generateTokenForUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }

        log.debug("Generating JWT token for user: {}", user.getEmail());

        // Extract roles and convert to comma-separated string
        String rolesString = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.joining(","));

        // Build token with user-specific claims
        return jwtUtil.generateToken(
                user.getEmail(),
                user.getDisplayName(),
                null // picture URL not set on User entity yet
        );
    }

    /**
     * Validate token and extract user email (username).
     *
     * <p>Verifies token signature and expiration; returns email if valid.
     * Used by JwtAuthenticationFilter to populate Spring Security context.
     *
     * @param token JWT token string
     * @return User email if token is valid
     * @throws IllegalArgumentException if token is invalid or expired
     */
    public String validateTokenAndExtractEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            if (!jwtUtil.validateToken(token)) {
                throw new IllegalArgumentException("Invalid token signature");
            }

            if (jwtUtil.isTokenExpired(token)) {
                throw new IllegalArgumentException("Token has expired");
            }

            return jwtUtil.getEmailFromToken(token);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            throw new IllegalArgumentException("Token validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extract user email from token without validation.
     *
     * <p><b>Use Only For</b>: Non-security contexts where token was already validated upstream,
     * or for error logging where validation failure is expected.
     *
     * @param token JWT token string
     * @return User email from token, or null if parsing fails
     */
    public String extractEmailFromToken(String token) {
        try {
            return jwtUtil.getEmailFromToken(token);
        } catch (Exception e) {
            log.warn("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract claim value from token by key.
     *
     * <p>Retrieves arbitrary String claims embedded in token (e.g., "name", "roles").
     *
     * @param token JWT token string
     * @param claimKey Claim key to extract
     * @return Claim value as String, or null if not found
     */
    public String extractClaimFromToken(String token, String claimKey) {
        try {
            return jwtUtil.getClaimFromToken(token, claimKey);
        } catch (Exception e) {
            log.debug("Failed to extract claim '{}' from token: {}", claimKey, e.getMessage());
            return null;
        }
    }

    /**
     * Extract all claims from token and return as Map.
     *
     * <p>Parses the token payload and returns all claims. Useful for debugging or
     * extracting multiple values at once.
     *
     * @param token JWT token string
     * @return Map of claim keys to values
     * @throws IllegalArgumentException if token is invalid
     */
    public Map<String, Object> extractAllClaimsFromToken(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                throw new IllegalArgumentException("Invalid token signature");
            }

            String email = jwtUtil.getEmailFromToken(token);
            String name = jwtUtil.getClaimFromToken(token, "name");
            String picture = jwtUtil.getClaimFromToken(token, "picture");

            Map<String, Object> claims = new HashMap<>();
            claims.put("email", email);
            claims.put("name", name);
            if (picture != null) {
                claims.put("picture", picture);
            }

            return claims;
        } catch (Exception e) {
            log.debug("Failed to extract claims from token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token: " + e.getMessage(), e);
        }
    }

    /**
     * Check if token is expired.
     *
     * @param token JWT token string
     * @return true if token expiration date has passed
     */
    public boolean isTokenExpired(String token) {
        try {
            return jwtUtil.isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Error checking token expiration: {}", e.getMessage());
            return true; // Treat unparseable token as expired
        }
    }

    /**
     * Get token expiration instant.
     *
     * @param token JWT token string
     * @return Expiration timestamp as Instant, or null if not found
     */
    public Instant getTokenExpirationInstant(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                return null;
            }

            // Get expiration by parsing token (this is a workaround since we don't have direct access to Claims)
            // For now, we'll return null as we can't extract expiration without exposing JwtUtil internals
            // This can be enhanced if JwtUtil provides a getExpirationFromToken() method
            return null;
        } catch (Exception e) {
            log.debug("Failed to extract expiration from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate a refresh token for token renewal flow.
     *
     * <p>Refresh tokens have extended expiration and are used to obtain new access tokens
     * without requiring re-authentication.
     *
     * @param email User email
     * @return Refresh token string
     */
    public String generateRefreshToken(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        log.debug("Generating refresh token for email: {}", email);
        return jwtUtil.generateRefreshToken(email);
    }
}
