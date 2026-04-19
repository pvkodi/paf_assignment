package com.sliitreserve.api.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliitreserve.api.entities.ticket.TicketCategory;
import com.sliitreserve.api.entities.ticket.TicketPriority;

import java.util.UUID;

/**
 * Request DTO for creating a new maintenance ticket.
 *
 * <p><b>Constraints</b>:
 * <ul>
 *   <li>Title: 20-200 characters (required)
 *   <li>Description: 50+ characters (required)
 *   <li>Category: Must be valid TicketCategory enum
 *   <li>Priority: Must be valid TicketPriority enum
 *   <li>Facility ID: Must reference existing facility
 * </ul>
 *
 * <p><b>Validation Scope</b>:
 * <ul>
 *   <li>Field-level: Bean Validation annotations enforce format/length
 *   <li>Business-logic: TicketService validates facility existence and accessibility
 * </ul>
 *
 * @see com.sliitreserve.api.entities.ticket.MaintenanceTicket for entity structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCreationRequest {

  /**
   * Title of the maintenance issue.
   * Must be concise but descriptive (20-200 characters).
   * Examples: "Water leak in Room 101", "HVAC not cooling Laboratory A"
   */
  @NotBlank(message = "Title is required")
  @Size(min = 20, max = 200, message = "Title must be between 20 and 200 characters")
  private String title;

  /**
   * Detailed description of the maintenance issue.
   * Must provide sufficient context for technicians to understand the problem (50+ characters).
   * Can include photos, previous attempts, relevant dates/times.
   */
  @NotBlank(message = "Description is required")
  @Size(min = 50, message = "Description must be at least 50 characters")
  private String description;

  /**
   * Category of the maintenance issue.
   * Examples: PLUMBING, ELECTRICAL, HVAC, CARPENTRY, GENERAL
   */
  @NotNull(message = "Category is required")
  private TicketCategory category;

  /**
   * Priority level of the maintenance request.
   * Examples: CRITICAL (immediate response needed), HIGH, MEDIUM, LOW
   */
  @NotNull(message = "Priority is required")
  private TicketPriority priority;

  /**
   * Facility ID where the issue is located.
   * Must reference an existing facility that the user has access to.
   * Accepted as string UUID and validated for format.
   */
  @NotBlank(message = "Facility ID is required")
  @JsonProperty("facilityId")
  private String facilityId;

  /**
   * Convert and validate facilityId string to UUID.
   */
  public UUID getFacilityIdAsUUID() {
    if (this.facilityId == null || this.facilityId.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(this.facilityId);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid facility ID format: " + this.facilityId, e);
    }
  }
}
