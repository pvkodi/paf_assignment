package com.sliitreserve.api.services.auth;

import com.sliitreserve.api.dto.auth.EmailPasswordLoginRequest;
import com.sliitreserve.api.dto.auth.RegisterRequest;
import com.sliitreserve.api.dto.auth.UserProfileResponse;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.exception.UnauthorizedException;
import com.sliitreserve.api.repositories.auth.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

/**
 * Email/Password Authentication Service.
 *
 * <p>Handles user registration and email/password-based authentication.
 * Provides password hashing, verification, and user account creation.
 *
 * <p><b>Security</b>:
 * <ul>
 *   <li>Passwords are hashed using BCrypt with strength 12</li>
 *   <li>Password verification uses constant-time comparison</li>
 *   <li>Passwords are never logged</li>
 *   <li>Email must be unique; duplicate registration is rejected</li>
 * </ul>
 *
 * <p><b>User Creation</b>:
 * <ul>
 *   <li>New email/password users are assigned default role: USER</li>
 *   <li>Account is active by default</li>
 *   <li>No suspension on creation</li>
 * </ul>
 */
@Service
@Slf4j
public class EmailPasswordAuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    /**
     * Register a new user with email and password.
     *
     * <p>Validates that:
     * <ul>
     *   <li>Email is not already registered</li>
     *   <li>Passwords match and meet minimum requirements</li>
     * </ul>
     *
     * @param request Registration request with email, name, and passwords
     * @return OAuthTokenResponse with JWT token and user profile
     * @throws IllegalArgumentException if email already exists or passwords don't match
     */
    @Transactional
    public OAuthAuthService.OAuthTokenResponse registerUser(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            log.warn("Registration failed: passwords do not match for email: {}", request.getEmail());
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed: email already registered: {}", request.getEmail());
            throw new IllegalArgumentException("Email already registered");
        }

        // Hash password
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Create new user with email/password credentials
        User user = User.builder()
                .email(request.getEmail())
                .displayName(request.getDisplayName())
                .passwordHash(passwordHash)
                // googleSubject is null for email/password users
                .googleSubject(null)
                .roles(Collections.singleton(Role.USER))
                .active(true)
                .noShowCount(0)
                .suspendedUntil(null)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        // Generate JWT tokens and return response
        String jwtToken = jwtTokenService.generateTokenForUser(user);
        LocalDateTime expiresAt = LocalDateTime.now(ZoneId.of("UTC")).plusSeconds(86400); // 24 hours
        
        // Generate refresh token (same user, for refresh endpoint)
        String refreshToken = jwtTokenService.generateTokenForUser(user);

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRoles(),
                user.isSuspended()
        );

        return new OAuthAuthService.OAuthTokenResponse(
                jwtToken,
                refreshToken,
                expiresAt,
                profile
        );
    }

    /**
     * Authenticate user with email and password.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>User exists with the provided email</li>
     *   <li>Password matches the stored hash</li>
     *   <li>Account is active</li>
     * </ul>
     *
     * @param request Login request with email and password
     * @return OAuthTokenResponse with JWT token and user profile
     * @throws UnauthorizedException if credentials are invalid or account is inactive
     */
    @Transactional(readOnly = true)
    public OAuthAuthService.OAuthTokenResponse authenticateUser(EmailPasswordLoginRequest request) {
        log.debug("Authenticating user with email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Check if account is active
        if (!user.isActive()) {
            log.warn("Login attempt on inactive account: {}", request.getEmail());
            throw new UnauthorizedException("Account is inactive");
        }

        // Verify password
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password for user: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        log.info("User authenticated successfully: {}", request.getEmail());

        // Generate JWT tokens and return response
        String jwtToken = jwtTokenService.generateTokenForUser(user);
        LocalDateTime expiresAt = LocalDateTime.now(ZoneId.of("UTC")).plusSeconds(86400); // 24 hours
        
        // Generate refresh token (same user, for refresh endpoint)
        String refreshToken = jwtTokenService.generateTokenForUser(user);

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRoles(),
                user.isSuspended()
        );

        return new OAuthAuthService.OAuthTokenResponse(
                jwtToken,
                refreshToken,
                expiresAt,
                profile
        );
    }
}
