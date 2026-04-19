package com.sliitreserve.api.entities.booking;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sliitreserve.api.entities.auth.User;

/**
 * CheckInRecord entity representing a check-in event for a booking.
 *
 * <p><b>Purpose</b>: Store evidence of attendance (or absence) for a booking. Used to track
 * no-shows and evaluate suspension eligibility per FR-021, FR-022.
 *
 * <p><b>Key Fields</b> (from data-model.md):
 * <ul>
 *   <li><b>id</b>: UUID primary key
 *   <li><b>booking</b>: Many-to-one reference to the Booking being checked in
 *   <li><b>method</b>: Check-in method (QR or MANUAL per FR-020)
 *   <li><b>checkedInBy</b>: User who performed the check-in (staff for manual, system/user for QR)
 *   <li><b>checkedInAt</b>: Timestamp of check-in (in campus local timezone)
 *   <li><b>createdAt</b>: Record creation timestamp (audit trail)
 * </ul>
 *
 * <p><b>No-Show Classification</b> (FR-021): A booking is classified as no-show if:
 * <ul>
 *   <li>No CheckInRecord exists for the booking, AND
 *   <li>Current time is more than 15 minutes after booking start time (in campus local timezone)
 * </ul>
 *
 * <p><b>Suspension Logic</b> (FR-022): When a user reaches 3 no-shows, automatic 1-week
 * suspension is applied. Evaluated by CheckInService.evaluateNoShow() after check-in/
 * no-show determination.
 *
 * <p><b>Check-In Methods</b> (FR-020):
 * <ul>
 *   <li><b>QR</b>: User scans QR code (e.g., booking credential, facility entrance)
 *   <li><b>MANUAL</b>: Staff records check-in manually (requires staff/admin role)
 * </ul>
 *
 * @see CheckInMethod for method enumeration
 * @see Booking for booking reference
 * @see User for check-in performer reference
 */
@Entity
@Table(
    name = "check_in",
    indexes = {
      @Index(name = "idx_check_in_booking", columnList = "booking_id"),
      @Index(name = "idx_check_in_timestamp", columnList = "checked_in_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull(message = "Booking is required")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false, foreignKey = @ForeignKey(name = "fk_check_in_booking"))
  private Booking booking;

  @NotNull(message = "Check-in method is required")
  @Enumerated(EnumType.STRING)
  @Column(name = "method", nullable = false, length = 20)
  private CheckInMethod method;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "checked_in_by_user_id", foreignKey = @ForeignKey(name = "fk_check_in_user"))
  private User checkedInBy;

  @NotNull(message = "Check-in timestamp is required")
  @Column(name = "checked_in_at", nullable = false)
  private LocalDateTime checkedInAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
