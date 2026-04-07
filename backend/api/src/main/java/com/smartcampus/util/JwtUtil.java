package com.smartcampus.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token Utility for OAuth2 + JWT Authentication.
 * Handles token generation, validation, and claim extraction.
 *
 * Token Structure:
 * - Header: HS256 algorithm
 * - Payload: email (subject), name, picture (Google profile), roles
 * - Signature: HMAC-SHA256 with app.auth.jwt.secret
 *
 * Token Types:
 * - Access Token (24h): Used for API requests
 * - Refresh Token (7d): Used to obtain new access tokens
 *
 * Configuration:
 * - app.auth.jwt.secret: Signing key (min 32 characters)
 * - app.auth.jwt.expiration: Access token TTL in milliseconds
 * - app.auth.jwt.refresh-expiration: Refresh token TTL in milliseconds
 *
 * Usage in JwtAuthenticationFilter:
 * <pre>
 * String token = extractTokenFromHeader(authHeader);
 * if (jwtUtil.validateToken(token)) {
 *     String email = jwtUtil.getEmailFromToken(token);
 *     // Create authentication with user details
 * }
 * </pre>
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.auth.jwt.secret}")
    private String jwtSecret;

    @Value("${app.auth.jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.auth.jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * Get HMAC-SHA256 signing key from secret.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generate access token with user information from Google OAuth.
     *
     * @param email User email (from Google OAuth)
     * @param name User display name (from Google OAuth)
     * @param picture User profile picture URL (from Google OAuth)
     * @return Signed JWT access token
     */
    public String generateToken(String email, String name, String picture) {
        log.debug("Generating access token for email: {}", email);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("name", name);
        if (picture != null) {
            claims.put("picture", picture);
        }
        return createToken(claims, email, jwtExpiration);
    }

    /**
     * Generate refresh token for token renewal without re-authentication.
     *
     * @param email User email
     * @return Signed JWT refresh token
     */
    public String generateRefreshToken(String email) {
        log.debug("Generating refresh token for email: {}", email);
        return createToken(new HashMap<>(), email, refreshExpiration);
    }

    /**
     * Create JWT token with claims, subject, and expiration.
     *
     * @param claims Token payload claims
     * @param subject Token subject (typically email)
     * @param expiration Token TTL in milliseconds
     * @return Signed JWT token
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract email (subject) from token.
     *
     * @param token JWT token
     * @return Email address from token subject
     * @throws io.jsonwebtoken.JwtException if token is invalid or expired
     */
    public String getEmailFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    /**
     * Extract specific claim from token.
     *
     * @param token JWT token
     * @param claimKey Claim key to extract
     * @return Claim value as String
     * @throws io.jsonwebtoken.JwtException if token is invalid or expired
     */
    public String getClaimFromToken(String token, String claimKey) {
        return (String) getAllClaimsFromToken(token).get(claimKey);
    }

    /**
     * Get all claims from token.
     *
     * @param token JWT token
     * @return Claims object containing all payload claims
     * @throws io.jsonwebtoken.JwtException if token is invalid or expired
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate token signature and expiration.
     *
     * @param token JWT token to validate
     * @return true if token is valid and not expired, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            log.debug("Token validation successful");
            return true;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is expired.
     *
     * @param token JWT token
     * @return true if token expiration is before current time, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getAllClaimsFromToken(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            log.debug("Could not determine expiration: {}", e.getMessage());
            return true; // Treat parsing errors as expired
        }
    }
}
