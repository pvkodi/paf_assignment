package com.sliitreserve.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for email/password login request.
 * 
 * <p>Captures user credentials for email/password authentication.
 * Both email and password are required and validated.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailPasswordLoginRequest {
    
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    @JsonProperty("email")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    @JsonProperty("password")
    private String password;
}
