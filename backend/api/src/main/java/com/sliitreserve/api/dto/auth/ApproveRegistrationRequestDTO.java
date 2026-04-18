package com.sliitreserve.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for approving a registration request.
 * Admin can optionally add a note when approving.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRegistrationRequestDTO {

  @JsonProperty("note")
  private String note;
}
