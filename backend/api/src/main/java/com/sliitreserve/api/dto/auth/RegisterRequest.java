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
 * DTO for user registration request (email/password).
 * 
 * <p>Captures user details and credentials for account creation.
 * Email must be unique, passwords must be at least 8 characters.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    @JsonProperty("email")
    private String email;
    
    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 255, message = "Display name must be between 2 and 255 characters")
    @JsonProperty("displayName")
    private String displayName;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    @JsonProperty("password")
    private String password;
    
    @NotBlank(message = "Password confirmation is required")
    @JsonProperty("confirmPassword")
    private String confirmPassword;
}
