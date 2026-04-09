package com.sliitreserve.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for authentication response.
 * 
 * <p>Supports both legacy and contract-compliant response formats:
 * 
 * <p><b>Legacy format</b> (for backward compatibility):
 * - accessToken, refreshToken, email, name, picture, tokenType
 * 
 * <p><b>Contract-compliant format</b> (RFC 6749 OAuth2):
 * - token, expiresAt, user (with id, email, roles, suspended)
 * 
 * Follows OpenAPI specification for auth endpoint responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    // === Contract-Compliant Fields (OAuth2/OpenID Connect) ===

    /**
     * JWT access token for authenticated requests.
     * Should be included in Authorization: Bearer {token} header.
     */
    @JsonProperty("token")
    private String token;

    /**
     * Token expiration timestamp in ISO 8601 format.
     * Clients should refresh the token before this time.
     */
    @JsonProperty("expiresAt")
    private LocalDateTime expiresAt;

    /**
     * Authenticated user profile (id, email, roles, suspension status).
     */
    @JsonProperty("user")
    private UserProfileResponse user;

    // === Legacy Fields (for backward compatibility) ===

    /**
     * Legacy field: JWT access token (same as 'token').
     * Deprecated: Use 'token' field instead.
     */
    @JsonProperty("accessToken")
    private String accessToken;

    /**
     * Legacy field: Refresh token.
     * Deprecated: Will be managed separately in T030.
     */
    @JsonProperty("refreshToken")
    private String refreshToken;

    /**
     * Legacy field: User email.
     * Deprecated: Use 'user.email' in user profile instead.
     */
    @JsonProperty("email")
    private String email;

    /**
     * Legacy field: User display name.
     * Deprecated: Use 'user.displayName' in user profile instead.
     */
    @JsonProperty("name")
    private String name;

    /**
     * Legacy field: User picture URL.
     * Deprecated: Use 'user.picture' in user profile instead.
     */
    @JsonProperty("picture")
    private String picture;

    /**
     * Legacy field: Token type (usually "Bearer").
     * Deprecated: Implicit in Authorization header format.
     */
    @JsonProperty("tokenType")
    private String tokenType;

    /**
     * Legacy constructor for backward compatibility.
     * Constructs response in old format.
     */
    public AuthResponse(String accessToken, String refreshToken, String email, String name, String picture) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.tokenType = "Bearer";
    }

    /**
     * Contract-compliant constructor for OAuth2/OpenID Connect.
     * Constructs response with token, expiration, and user profile.
     */
    public AuthResponse(String token, LocalDateTime expiresAt, UserProfileResponse user) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.user = user;
    }
}

