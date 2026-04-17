package com.sliitreserve.api.controllers.auth;

import com.sliitreserve.api.dto.auth.AuthResponse;
import com.sliitreserve.api.dto.auth.OAuthCodeExchangeRequest;
import com.sliitreserve.api.dto.auth.UserProfileResponse;
import com.sliitreserve.api.dto.auth.EmailPasswordLoginRequest;
import com.sliitreserve.api.dto.auth.RegisterRequest;
import com.sliitreserve.api.dto.auth.CreateRegistrationRequestDTO;
import com.sliitreserve.api.dto.auth.RegistrationRequestDTO;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.exception.UnauthorizedException;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.services.auth.OAuthAuthService;
import com.sliitreserve.api.services.auth.EmailPasswordAuthService;
import com.sliitreserve.api.services.auth.RegistrationRequestService;
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
import com.sliitreserve.api.dto.ErrorResponseDTO;
import java.util.Map;

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
    private RegistrationRequestService registrationRequestService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SuspensionPolicyService suspensionPolicyService;

    @PostMapping("/oauth/google/callback")
    public ResponseEntity<?> exchangeOAuthCode(@Valid @RequestBody OAuthCodeExchangeRequest request) {
        log.info("OAuth callback initiated for redirectUri: {}", request.getRedirectUri());

        try {
            OAuthAuthService.OAuthTokenResponse tokenResponse = oauthAuthService.exchangeCodeForToken(request);

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

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody CreateRegistrationRequestDTO request) {
        log.info("Registration request for email: {}", request.getEmail());

        try {
            RegistrationRequestDTO registrationRequest = registrationRequestService.createRegistrationRequest(request);

            Map<String, Object> response = Map.of(
                    "status", "PENDING",
                    "message", "Registration submitted successfully. Awaiting admin approval.",
                    "registrationId", registrationRequest.getId(),
                    "email", registrationRequest.getEmail()
            );

            log.info("Registration request created successfully: {}", registrationRequest.getId());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

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

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody EmailPasswordLoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());

        try {
            OAuthAuthService.OAuthTokenResponse tokenResponse = emailPasswordAuthService.authenticateUser(request);

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

    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUserProfile() {
        try {
            String userEmail = extractEmailFromSecurityContext();

            if (userEmail == null) {
                log.warn("Profile request without valid authentication");
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("UNAUTHORIZED", "Authentication required"));
            }

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            log.debug("Profile retrieved for user: {}", userEmail);

            UserProfileResponse profile = new UserProfileResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRoles(),
                    suspensionPolicyService.isSuspended(user)
            );
            profile.setPicture(null);

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

    private String extractEmailFromSecurityContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            Object principal = authentication.getPrincipal();
            return principal != null ? principal.toString() : null;

        } catch (Exception e) {
            log.debug("Error extracting email from security context: {}", e.getMessage());
            return null;
        }
    }

    private ErrorResponseDTO createErrorResponse(String errorCode, String message) {
        return new ErrorResponseDTO(errorCode, message);
    }

    private Map<String, Object> createSuccessResponse(String message) {
        return Map.of("message", message, "timestamp", LocalDateTime.now());
    }
}
