package com.sliitreserve.api.controllers;

import com.sliitreserve.api.dto.auth.AuthResponse;
import com.sliitreserve.api.dto.auth.OAuthCodeExchangeRequest;
import com.sliitreserve.api.dto.auth.UserProfileResponse;
import com.sliitreserve.api.dto.auth.EmailPasswordLoginRequest;
import com.sliitreserve.api.dto.auth.RegisterRequest;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.exception.UnauthorizedException;
import com.sliitreserve.api.repositories.UserRepository;
import com.sliitreserve.api.services.auth.OAuthAuthService;
import com.sliitreserve.api.services.auth.EmailPasswordAuthService;
import com.sliitreserve.api.services.auth.JwtTokenService;
import com.sliitreserve.api.services.auth.SuspensionPolicyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Authentication Controller.
 *
 * <p><b>Purpose</b>: Handles user authentication flows (OAuth and email/password) and manages
 * user session/profile endpoints. Enforces suspension policy on protected endpoints per FR-003.
 *
 * <p><b>Endpoints</b>:
 * <ul>
 *   <li><b>POST /auth/oauth/google/callback</b> [PUBLIC]
 *     - Exchange Google authorization code for JWT
 *     - Returns: AuthResponse (token, expiresAt, user profile)
 *   <li><b>POST /auth/register</b> [PUBLIC]
 *     - Register new user with email and password
 *     - Returns: AuthResponse (token, expiresAt, user profile)
 *   <li><b>POST /auth/login</b> [PUBLIC]
 *     - Authenticate user with email and password
 *     - Returns: AuthResponse (token, expiresAt, user profile)
 *   <li><b>GET /auth/profile</b> [PROTECTED - Allows suspended users]
 *     - Get current authenticated user's profile
 *     - Returns: UserProfileResponse (id, email, roles, suspended status)
 *   <li><b>POST /auth/logout</b> [PROTECTED - Allows suspended users]
 *     - Clear user session (stateless JWT requires client-side token deletion)
 *     - Returns: Confirmation response
 * </ul>
 *
 * <p><b>Security</b>:
 * <ul>
 *   <li>OAuth callback, registration, and login are public (no Bearer auth required)</li>
 *   <li>Profile and logout endpoints are protected but allow suspended users (whitelist)</li>
 *   <li>Passwords are hashed with BCrypt (strength 12) and never logged</li>
 *   <li>JWT Bearer token extracted from Authorization header for protected endpoints</li>
 * </ul>
 *
 * <p><b>Integration</b>:
 * <ul>
 *   <li>OAuthAuthService: Exchanges OAuth code for JWT and user profile
 *   <li>EmailPasswordAuthService: Handles registration and email/password authentication
 *   <li>JwtTokenService: Validates tokens and extracts user email
 *   <li>UserRepository: Looks up user profile for protected endpoints
 *   <li>SuspensionPolicyService: Enforces suspension on non-whitelisted operations
 * </ul>
 *
 * @see OAuthAuthService for OAuth flow implementation
 * @see EmailPasswordAuthService for email/password authentication
 * @see JwtTokenService for JWT operations
 * @see SuspensionPolicyService for suspension enforcement
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:8080"})
public class AuthController {

    @Autowired
    private OAuthAuthService oauthAuthService;

    @Autowired
    private EmailPasswordAuthService emailPasswordAuthService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SuspensionPolicyService suspensionPolicyService;

    /**
     * OAuth 2.0 Authorization Code Exchange Endpoint.
     *
     * <p>Implements RFC 6749 Section 4.1.3 - Authorization Code Exchange.
     * Frontend sends Google authorization code received from OAuth consent screen.
     * Backend exchanges code for ID token, verifies signature, creates/retrieves user,
     * generates JWT session token, and returns authenticated user profile.
     *
     * <p><b>Request</b>: OAuthCodeExchangeRequest
     * - code: Authorization code from Google OAuth
     * - redirectUri: Origin of request (must match registered URI)
     *
     * <p><b>Response</b>: AuthResponse (HTTP 200)
     * - token: JWT access token for subsequent API calls
     * - expiresAt: Token expiration timestamp (ISO 8601)
     * - user: UserProfileResponse with id, email, roles, suspension status
     *
     * <p><b>Error Responses</b>:
     * - HTTP 400: Invalid authorization code or redirect URI mismatch
     * - HTTP 401: Token verification failed (invalid signature or expired)
     * - HTTP 500: Unexpected error during code exchange
     *
     * <p><b>Security</b>: Public endpoint (no authentication required).
     *
     * @param request OAuth code exchange request (code + redirectUri)
     * @return ResponseEntity with AuthResponse (200 OK) or error (40x/50x)
     */
    @PostMapping("/oauth/google/callback")
    public ResponseEntity<?> exchangeOAuthCode(@Valid @RequestBody OAuthCodeExchangeRequest request) {
        log.info("OAuth callback initiated for redirectUri: {}", request.getRedirectUri());

        try {
            // Exchange authorization code for JWT and user profile
            OAuthAuthService.OAuthTokenResponse tokenResponse = oauthAuthService.exchangeCodeForToken(request);

            // Build response with contract-compliant fields
            AuthResponse authResponse = new AuthResponse();
            authResponse.setToken(tokenResponse.getAccessToken());
            authResponse.setRefreshToken(tokenResponse.getRefreshToken());
            authResponse.setExpiresAt(tokenResponse.getExpiresAt());
            authResponse.setUser(tokenResponse.getUser());

            log.info("OAuth authentication successful for user: {}", tokenResponse.getUser().getEmail());

            return ResponseEntity.ok(authResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid authorization code: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("INVALID_CODE", "Authorization code is invalid, expired, or already used. Please try logging in again."));

        } catch (IOException e) {
            log.error("Error during OAuth code exchange: {}", e.getMessage(), e);
            String message = "Error communicating with OAuth provider. ";
            if (e.getMessage() != null && e.getMessage().contains("invalid_grant")) {
                message += "Possible causes: (1) Redirect URI mismatch with Google Console, (2) Code expired (10 min timeout), (3) Code already used. Check application logs for details.";
            }
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("OAUTH_ERROR", message));

        } catch (Exception e) {
            log.error("Unexpected error during OAuth authentication: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("AUTH_ERROR", "Authentication failed"));
        }
    }

    /**
     * User Registration Endpoint (Email/Password).
     *
     * <p>Creates a new user account with email and password authentication.
     * Email must be unique and not already registered.
     * Passwords must be at least 8 characters and must match.
     *
     * <p><b>Request</b>: RegisterRequest
     * - email: Institutional email (must be unique)
     * - displayName: User's display name
     * - password: Password (min 8 chars)
     * - confirmPassword: Password confirmation (must match password)
     *
     * <p><b>Response</b>: AuthResponse (HTTP 201)
     * - token: JWT access token for subsequent API calls
     * - expiresAt: Token expiration timestamp (ISO 8601)
     * - user: UserProfileResponse with id, email, roles, suspension status
     *
     * <p><b>Error Responses</b>:
     * - HTTP 400: Validation failed (invalid email, passwords don't match, email already registered)
     * - HTTP 500: Unexpected error during registration
     *
     * <p><b>Security</b>: Public endpoint (no authentication required).
     * Passwords are hashed with BCrypt before storage.
     *
     * @param request Registration request (email, name, passwords)
     * @return ResponseEntity with AuthResponse (201 Created) or error
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for email: {}", request.getEmail());

        try {
            // Register user and generate JWT
            OAuthAuthService.OAuthTokenResponse tokenResponse = emailPasswordAuthService.registerUser(request);

            // Build response with contract-compliant fields
            AuthResponse authResponse = new AuthResponse();
            authResponse.setToken(tokenResponse.getAccessToken());
            authResponse.setRefreshToken(tokenResponse.getRefreshToken());
            authResponse.setExpiresAt(tokenResponse.getExpiresAt());
            authResponse.setUser(tokenResponse.getUser());

            log.info("User registered successfully: {}", tokenResponse.getUser().getEmail());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(authResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Registration validation failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("REGISTRATION_ERROR", "Registration failed"));
        }
    }

    /**
     * Email/Password Login Endpoint.
     *
     * <p>Authenticates user with email and password credentials.
     * Verifies password against stored hash and returns JWT on success.
     *
     * <p><b>Request</b>: EmailPasswordLoginRequest
     * - email: User's institutional email
     * - password: User's password
     *
     * <p><b>Response</b>: AuthResponse (HTTP 200)
     * - token: JWT access token for subsequent API calls
     * - expiresAt: Token expiration timestamp (ISO 8601)
     * - user: UserProfileResponse with id, email, roles, suspension status
     *
     * <p><b>Error Responses</b>:
     * - HTTP 400: Validation failed (missing email or password)
     * - HTTP 401: Invalid credentials or inactive account
     * - HTTP 500: Unexpected error during authentication
     *
     * <p><b>Security</b>: Public endpoint (no authentication required).
     * Passwords are verified using BCrypt constant-time comparison.
     * Failed attempts are logged; rate limiting recommended at load balancer level.
     *
     * @param request Login request (email and password)
     * @return ResponseEntity with AuthResponse (200 OK) or error
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody EmailPasswordLoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());

        try {
            // Authenticate user and generate JWT
            OAuthAuthService.OAuthTokenResponse tokenResponse = emailPasswordAuthService.authenticateUser(request);

            // Build response with contract-compliant fields
            AuthResponse authResponse = new AuthResponse();
            authResponse.setToken(tokenResponse.getAccessToken());
            authResponse.setRefreshToken(tokenResponse.getRefreshToken());
            authResponse.setExpiresAt(tokenResponse.getExpiresAt());
            authResponse.setUser(tokenResponse.getUser());

            log.info("User authenticated successfully: {}", tokenResponse.getUser().getEmail());

            return ResponseEntity.ok(authResponse);

        } catch (UnauthorizedException e) {
            log.warn("Authentication failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("INVALID_CREDENTIALS", e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error during authentication: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("AUTH_ERROR", "Authentication failed"));
        }
    }

    /**
     * Get Current User Profile Endpoint.
     *
     * <p>Returns the authenticated user's profile information including roles and suspension status.
     * This endpoint is specifically whitelisted for suspended users (FR-003) so they can
     * view their profile and suspension reason.
     *
     * <p><b>Request</b>: No body. Uses Bearer token from Authorization header.
     *
     * <p><b>Response</b>: UserProfileResponse (HTTP 200)
     * - id: User UUID
     * - email: User's institutional email
     * - displayName: User's display name
     * - roles: Set of assigned roles
     * - suspended: Suspension status (true/false)
     *
     * <p><b>Error Responses</b>:
     * - HTTP 401: Missing or invalid Bearer token
     * - HTTP 404: User not found in database
     * - HTTP 500: Unexpected error
     *
     * <p><b>Security</b>: Protected endpoint with Bearer token authentication.
     * Suspended users explicitly allowed (whitelist exception).
     *
     * @return ResponseEntity with UserProfileResponse (200 OK) or error
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUserProfile() {
        try {
            // Extract user email from JWT token in SecurityContext
            String userEmail = extractEmailFromSecurityContext();

            if (userEmail == null) {
                log.warn("Profile request without valid authentication");
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("UNAUTHORIZED", "Authentication required"));
            }

            // Look up user in database
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            log.debug("Profile retrieved for user: {}", userEmail);

            // Build response with current user data
            UserProfileResponse profile = new UserProfileResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRoles(),
                    suspensionPolicyService.isSuspended(user)
            );
            profile.setPicture(null); // Not stored on User entity yet

            return ResponseEntity.ok(profile);

        } catch (UnauthorizedException e) {
            log.warn("Unauthorized profile access: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("UNAUTHORIZED", e.getMessage()));

        } catch (Exception e) {
            log.error("Error retrieving user profile: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("PROFILE_ERROR", "Error retrieving profile"));
        }
    }

    /**
     * Logout Endpoint.
     *
     * <p>Provides a signal for client-side logout. Since JWT is stateless, logout is
     * primarily a client-side operation (delete token from storage). This endpoint can be
     * used for audit logging or to trigger any server-side cleanup.
     *
     * <p>This endpoint is whitelisted for suspended users so they can end their session.
     *
     * <p><b>Request</b>: No body. Uses Bearer token from Authorization header.
     *
     * <p><b>Response</b>: Success response (HTTP 200)
     *
     * <p><b>Security</b>: Protected endpoint with Bearer token authentication.
     * Suspended users explicitly allowed (whitelist exception).
     *
     * @return ResponseEntity with success message
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        try {
            String userEmail = extractEmailFromSecurityContext();

            if (userEmail == null) {
                log.warn("Logout request without valid authentication");
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("UNAUTHORIZED", "Authentication required"));
            }

            log.info("User logout: {}", userEmail);

            return ResponseEntity.ok(createSuccessResponse("Logged out successfully"));

        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("LOGOUT_ERROR", "Error during logout"));
        }
    }

    /**
     * Extract user email from Spring Security context.
     *
     * <p>Retrieves the authenticated user's email (username) from SecurityContextHolder.
     * This value is set by JwtAuthenticationFilter during request processing.
     *
     * @return User email if authenticated, null otherwise
     */
    private String extractEmailFromSecurityContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            // Principal is set as email by JwtAuthenticationFilter
            Object principal = authentication.getPrincipal();
            return principal != null ? principal.toString() : null;

        } catch (Exception e) {
            log.debug("Error extracting email from security context: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Build a standardized error response DTO.
     *
     * @param errorCode Application error code
     * @param message Human-readable error message
     * @return Error response object
     */
    private ErrorResponse createErrorResponse(String errorCode, String message) {
        return new ErrorResponse(errorCode, message, LocalDateTime.now());
    }

    /**
     * Build a standardized success response DTO.
     *
     * @param message Success message
     * @return Success response object
     */
    private SuccessResponse createSuccessResponse(String message) {
        return new SuccessResponse(message, LocalDateTime.now());
    }

    /**
     * Simple error response DTO for API responses.
     */
    record ErrorResponse(String code, String message, LocalDateTime timestamp) { }

    /**
     * Simple success response DTO for API responses.
     */
    record SuccessResponse(String message, LocalDateTime timestamp) { }
}
