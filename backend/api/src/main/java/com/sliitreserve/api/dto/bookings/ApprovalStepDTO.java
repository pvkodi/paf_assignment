package com.sliitreserve.api.dto.bookings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Approval Step information in booking workflow.
 * Represents a single step in the multi-level approval process.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStepDTO {

    @JsonProperty("stepOrder")
    private Integer stepOrder;

    @JsonProperty("approverRole")
    private String approverRole;

    @JsonProperty("decision")
    private String decision;

    @JsonProperty("note")
    private String note;

    @JsonProperty("decidedBy")
    private UserSummaryDTO decidedBy;

    @JsonProperty("decidedAt")
    private LocalDateTime decidedAt;
}
