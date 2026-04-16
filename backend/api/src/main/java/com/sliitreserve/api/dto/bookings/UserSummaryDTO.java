package com.sliitreserve.api.dto.bookings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Summary DTO for User information in booking contexts.
 * Contains essential user details without exposing sensitive information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("displayName")
    private String displayName;
}
