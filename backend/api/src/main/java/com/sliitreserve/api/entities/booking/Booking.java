package com.sliitreserve.api.entities.booking;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.auth.User;

/**
 * Booking entity representing a facility reservation request and lifecycle record.
 *
 * <p><b>Purpose</b>: Store booking details including requester, facility, time range, purpose,
 * attendee count, approval/status, and recurrence metadata. Uses optimistic locking via
 * @Version to detect concurrent modification conflicts (409 response per FR-009).
 *
 * <p><b>Key Fields</b> (from data-model):
 * <ul>
 *   <li><b>id</b>: UUID primary key
 *   <li><b>facility</b>: Many-to-one reference to Facility
 *   <li><b>requestedBy</b>: User who submitted the booking
 *   <li><b>bookedFor</b>: User the booking is for (may differ from requester for admin
 *       bookings; FR-012)
 *   <li><b>bookingDate</b>: Date of the booking in campus local timezone
 *   <li><b>startTime</b>: Start time (LocalTime)
 *   <li><b>endTime</b>: End time (LocalTime)
 *   <li><b>purpose</b>: Booking purpose/description
 *   <li><b>attendees</b>: Expected attendee count (must be ≤ facility.capacity)
 *   <li><b>status</b>: PENDING, APPROVED, REJECTED, CANCELLED
 *   <li><b>recurrenceRule</b>: iCal RRULE for recurring bookings (nullable)
 *   <li><b>isRecurringMaster</b>: Flag indicating if this is the master of a recurring series
 *   <li><b>timezone</b>: Booking timezone (campus default by policy)
 *   <li><b>version</b>: Optimistic lock version for concurrency control (FR-009)
 *   <li><b>createdAt, updatedAt</b>: Audit timestamps
 * </ul>
 *
 * <p><b>Approval Policy</b> (FR-014 to FR-017):
 * <ul>
 *   <li>USER bookings: 2-step (LECTURER → ADMIN)
 *   <li>LECTURER bookings: auto-approve unless high-capacity facility
 *   <li>High-capacity facilities: additional FACILITY_MANAGER sign-off
 *   <li>ADMIN bookings: auto-approve (bypass workflow)
 * </ul>
 *
 * <p><b>Concurrency Safety</b> (FR-009): Uses optimistic locking via @Version. On concurrent
 * update to the same booking, JPA throws OptimisticLockException, which is translated to HTTP
 * 409 Conflict with current version details.
 *
 * <p><b>Recurrence</b> (FR-010): Recurring bookings store an iCal RRULE and are expanded as
 * needed. Public holiday occurrences are skipped (FR-010).
 *
 * @see BookingStatus for status enumeration
 * @see Facility for facility reference
 * @see User for requester/bookedFor references
 */
@Entity
@Table(
    name = "booking",
    indexes = {
      @Index(name = "idx_booking_facility", columnList = "facility_id"),
      @Index(name = "idx_booking_requested_by", columnList = "requested_by_user_id"),
      @Index(name = "idx_booking_booked_for", columnList = "booked_for_user_id"),
      @Index(name = "idx_booking_status", columnList = "status"),
      @Index(name = "idx_booking_date", columnList = "booking_date")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull(message = "Facility is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "facility_id", nullable = false)
  private Facility facility;

  @NotNull(message = "Requested by user is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requested_by_user_id", nullable = false)
  private User requestedBy;

  @NotNull(message = "Booked for user is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booked_for_user_id", nullable = false)
  private User bookedFor;

  @NotNull(message = "Booking date is required")
  @Column(name = "booking_date", nullable = false)
  private LocalDate bookingDate;

  @NotNull(message = "Start time is required")
  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @NotNull(message = "End time is required")
  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @NotBlank(message = "Purpose is required")
  @Column(nullable = false, length = 500)
  private String purpose;

  @Min(value = 1, message = "Attendees must be at least 1")
  @Column(nullable = false)
  private Integer attendees;

  @NotNull(message = "Status is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Builder.Default
  private BookingStatus status = BookingStatus.PENDING;

  @Column(name = "recurrence_rule", length = 500)
  private String recurrenceRule;

  @Column(name = "is_recurring_master", nullable = false)
  @Builder.Default
  private boolean isRecurringMaster = false;

  @Column(nullable = false, length = 50)
  @Builder.Default
  private String timezone = "Asia/Colombo";

  @Version
  @Column(nullable = false)
  @Builder.Default
  private Long version = 0L;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Validate that startTime is before endTime.
   *
   * @return true if valid
   */
  public boolean isValidTimeRange() {
    return startTime != null && endTime != null && startTime.isBefore(endTime);
  }

  /**
   * Validate that attendees do not exceed facility capacity.
   *
   * @return true if valid
   */
  public boolean isValidCapacity() {
    return facility != null && attendees != null && attendees <= facility.getCapacity();
  }

  /**
   * Check if this booking is in a terminal state (APPROVED, REJECTED, or CANCELLED).
   *
   * @return true if terminal
   */
  public boolean isTerminal() {
    return status == BookingStatus.APPROVED
        || status == BookingStatus.REJECTED
        || status == BookingStatus.CANCELLED;
  }

  /**
   * Check if this booking is recurring.
   *
   * @return true if recurrence rule is set
   */
  public boolean isRecurring() {
    return recurrenceRule != null && !recurrenceRule.isBlank();
  }
}
