package com.sliitreserve.api.dto.auth;

import com.sliitreserve.api.dto.BaseRequestDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for OAuth2 authorization code exchange request.
 * 
 * <p>Represents the payload for exchanging a Google authorization code
 * for JWT tokens. This is the contract for the OAuth callback endpoint.
 * 
 * @see <a href="https://developers.google.com/identity/protocols/oauth2">Google OAuth2 Protocol</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthCodeExchangeRequest extends BaseRequestDTO {

    private static final long serialVersionUID = 1L;

    /**
     * Authorization code received from Google authorization server.
     * Required for code-for-token exchange.
     */
    @NotBlank(message = "Authorization code is required")
    private String code;

    /**
     * Redirect URI used during authorization request.
     * Must match the registered redirect URI in Google OAuth app configuration.
     */
    @NotNull(message = "Redirect URI is required")
    private String redirectUri;
}
