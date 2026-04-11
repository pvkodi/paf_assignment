package com.sliitreserve.api.services.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sliitreserve.api.dto.auth.OAuthCodeExchangeRequest;
import com.sliitreserve.api.dto.auth.UserProfileResponse;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.repositories.UserRepository;
import com.sliitreserve.api.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

/**
 * OAuth 2.0 Authorization Code Flow Service.
 *
 * <p>Handles Google OAuth 2.0 authorization code exchange as per RFC 6749 and OpenID Connect.
 * Implements the standard authorization code flow:
 *
 * <pre>
 * 1. Frontend obtains authorization code from Google OAuth consent screen
 * 2. Frontend sends code + redirectUri to /auth/oauth/google/callback
 * 3. Backend exchanges code for id_token + access_token with Google Token Endpoint
 * 4. Backend verifies id_token signature and extracts user claims
 * 5. Backend looks up or creates User in database
 * 6. Backend generates JWT session token and returns UserProfileResponse
 * </pre>
 *
 * <p><b>Security</b>:
 * <ul>
 *   <li>Code validation: Only redeemable once; client_secret kept secure on backend</li>
 *   <li>HTTPS only in production: Redirect URI must match registered origin</li>
 *   <li>Token verification: ID token signature verified with Google's public keys</li>
 *   <li>User mapping: Google Subject (sub) is immutable and unique per user</li>
 * </ul>
 *
 * <p><b>RFC 6749 Compliance</b>:
 * <ul>
 *   <li>Authorization Code Grant Type (4.1.1 - 4.1.4)</li>
 *   <li>Authorization Endpoint: Google's OAuth 2.0 consent screen (handled by frontend)</li>
 *   <li>Token Endpoint: Google Token Service (handled by this service)</li>
 *   <li>Redirection Endpoint: /auth/oauth/google/callback</li>
 * </ul>
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749">OAuth 2.0 Authorization Framework (RFC 6749)</a>
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html">OpenID Connect Core 1.0</a>
 * @see <a href="https://developers.google.com/identity/protocols/oauth2">Google OAuth 2.0 Flow</a>
 */
@Service
@Slf4j
public class OAuthAuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.auth.google.client-id}")
    private String googleClientId;

    @Value("${app.auth.google.client-secret}")
    private String googleClientSecret;

    /**
     * Exchange authorization code for ID token and create/retrieve user session.
     *
     * <p>This method implements the authorization code exchange step (RFC 6749 Section 4.1.3):
     * - Exchanges authorization code for tokens with Google Token Endpoint
     * - Verifies the returned ID token signature
     * - Extracts user identity claims
     * - Creates or updates user in database
     * - Generates JWT session token
     *
     * @param request OAuth code exchange request (code + redirectUri)
     * @return UserProfileResponse with JWT token and user profile
     * @throws IllegalArgumentException if code is invalid or expired
     * @throws IOException if token endpoint communication fails
     */
    @Transactional
    public OAuthTokenResponse exchangeCodeForToken(OAuthCodeExchangeRequest request)
            throws IOException {
        log.debug("Exchanging authorization code for tokens: {}", request.getRedirectUri());
        log.debug("Using Google Client ID: {}", googleClientId);
        log.debug("Authorization code: {}", request.getCode().substring(0, Math.min(10, request.getCode().length())));

        try {
            // Step 1: Exchange authorization code for ID token with Google Token Endpoint
            // This implements RFC 6749 Section 4.1.3 - Authorization Code Exchange
            log.info("Calling Google Token Endpoint with redirect URI: {}", request.getRedirectUri());
            
            log.debug("Token Request Details: code={}, redirectUri={}, clientId={}", 
                request.getCode().substring(0, Math.min(20, request.getCode().length())) + "...",
                request.getRedirectUri(),
                googleClientId);
            
            GoogleAuthorizationCodeTokenRequest tokenRequest = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    "https://oauth2.googleapis.com/token",  // Google Token Endpoint
                    googleClientId,
                    googleClientSecret,
                    request.getCode(),                      // Authorization code from frontend
                    request.getRedirectUri()                // Must match registered redirect URI
            );

            GoogleTokenResponse tokenResponse = tokenRequest.execute();

            log.debug("Successfully exchanged authorization code for tokens");

            // Step 2: Verify ID token signature with Google's public keys
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // Get ID token from the token response
            String idTokenString = tokenResponse.getIdToken();
            if (idTokenString == null) {
                log.warn("No ID token in response from Google Token Endpoint");
                throw new IllegalArgumentException("No ID token received from Google");
            }

            var idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                log.warn("ID token verification failed for authorization code");
                throw new IllegalArgumentException("Invalid or expired authorization code");
            }

            // Step 3: Extract user claims from verified ID token
            String googleSubject = idToken.getPayload().getSubject();
            String email = idToken.getPayload().getEmail();
            String displayName = (String) idToken.getPayload().get("name");
            String picture = (String) idToken.getPayload().get("picture");

            log.debug("Successfully verified Google ID token for user: {}", email);

            // Step 4: Look up or create user in database
            User user = userRepository.findByGoogleSubject(googleSubject)
                    .orElseGet(() -> createNewUser(googleSubject, email, displayName));

            // Step 5: Update user profile if needed (name/picture may change)
            user.setEmail(email);
            user.setDisplayName(displayName);
            userRepository.save(user);

            log.info("User authenticated: {} ({})", email, googleSubject);

            // Step 6: Generate JWT session token
            String accessToken = jwtUtil.generateToken(email, displayName, picture);
            long expirationMs = 86400000; // 24 hours (matches JWT config)
            LocalDateTime expiresAt = LocalDateTime.now(ZoneId.systemDefault())
                    .plusSeconds(expirationMs / 1000);

            // Step 7: Build response with user profile and JWT
            UserProfileResponse userProfile = new UserProfileResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRoles(),
                    user.getSuspendedUntil() != null && user.getSuspendedUntil().isAfter(LocalDateTime.now())
            );

            return new OAuthTokenResponse(accessToken, expiresAt, userProfile);

        } catch (GeneralSecurityException e) {
            log.warn("ID token verification failed: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Invalid authorization code: token verification failed", e);
        } catch (IOException e) {
            log.error("Error exchanging authorization code with Google: {} | Cause: {}", e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "unknown", e);
            
            // Check for common OAuth errors
            if (e.getMessage() != null && e.getMessage().contains("invalid_grant")) {
                log.error("OAuth invalid_grant error - Check: 1) Redirect URI matches Google Console, 2) Code not expired (10 min), 3) Code not reused, 4) Client secret is correct");
            }
            throw e;
        }
    }

    /**
     * Create a new user for first-time OAuth login.
     *
     * <p>New users are created with:
     * - Role: USER (default role for new users)
     * - Active: true
     * - No-show count: 0
     * - Suspension: none
     *
     * @param googleSubject Google's unique user identifier (immutable)
     * @param email Institutional email address
     * @param displayName User's display name from OAuth provider
     * @return New User entity (persisted)
     */
    private User createNewUser(String googleSubject, String email, String displayName) {
        log.info("Creating new user from OAuth: {} ({})", email, googleSubject);

        User user = User.builder()
                .googleSubject(googleSubject)
                .email(email)
                .displayName(displayName)
                .roles(Collections.singleton(Role.USER)) // Default role for new users
                .active(true)
                .noShowCount(0)
                .suspendedUntil(null) // Not suspended
                .build();

        return userRepository.save(user);
    }

    /**
     * Response DTO for OAuth authorization code exchange.
     *
     * <p>Wraps the JWT token and user profile for the API response.
     */
    public static class OAuthTokenResponse {
        private final String accessToken;
        private final LocalDateTime expiresAt;
        private final UserProfileResponse user;

        public OAuthTokenResponse(String accessToken, LocalDateTime expiresAt, UserProfileResponse user) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
            this.user = user;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public UserProfileResponse getUser() {
            return user;
        }
    }
}
