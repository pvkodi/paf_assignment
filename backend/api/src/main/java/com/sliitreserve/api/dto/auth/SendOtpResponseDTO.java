package com.sliitreserve.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for OTP send response
 * Contains OTP ID and expiration time (for client-side UI feedback)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendOtpResponseDTO {

  @JsonProperty("otpId")
  private String otpId; // UUID of the OTP record

  @JsonProperty("email")
  private String email;

  @JsonProperty("expiresAt")
  private LocalDateTime expiresAt;

  @JsonProperty("expirationMinutes")
  private int expirationMinutes;

  @JsonProperty("message")
  private String message; // "OTP sent successfully to your email"
}
