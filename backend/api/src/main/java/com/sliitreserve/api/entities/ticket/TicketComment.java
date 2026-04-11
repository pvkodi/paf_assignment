package com.sliitreserve.api.entities.ticket;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sliitreserve.api.entities.auth.User;

/**
 * TicketComment entity representing a comment thread on a maintenance ticket.
 *
 * <p><b>Purpose</b>: Store comments and updates on ticket progress. Comments can be public
 * (visible to both users and staff) or internal (staff-only communications). Supports comment
 * lifecycle with update and soft-delete via deletedAt timestamp (FR-026, FR-029).
 *
 * <p><b>Key Fields</b> (from data-model):
 * <ul>
 *   <li><b>id</b>: UUID primary key
 *   <li><b>ticket</b>: Many-to-one reference to MaintenanceTicket (the issue being commented on)
 *   <li><b>author</b>: Many-to-one reference to User (comment author; required)
 *   <li><b>content</b>: Text field containing comment body (5-2000 chars)
 *   <li><b>visibility</b>: TicketCommentVisibility enum (PUBLIC or INTERNAL)
 *   <li><b>createdAt, updatedAt, deletedAt</b>: Audit timestamps (deletedAt for soft-delete)
 * </ul>
 *
 * <p><b>Access Control</b> (FR-026, FR-029):
 * <ul>
 *   <li>PUBLIC comments are visible to ticket creator and assigned technician; also staff.
 *   <li>INTERNAL comments visible to staff only (TECHNICIAN, FACILITY_MANAGER, ADMIN roles).
 *   <li>Authors can edit/delete own comments; admins can delete any comment.
 *   <li>Soft-delete is implemented via <code>deletedAt</code> timestamp.
 * </ul>
 *
 * <p><b>Relationships</b>:
 * <ul>
 *   <li>Many-to-one MaintenanceTicket (inverse of Ticket.comments)
 *   <li>Many-to-one User (inverse of User.ticketComments, if bidirectional)
 * </ul>
 *
 * @see MaintenanceTicket
 * @see User
 * @see TicketCommentVisibility
 */
@Entity
@Table(
    name = "ticket_comment",
    indexes = {
      @Index(name = "idx_comment_ticket", columnList = "ticket_id"),
      @Index(name = "idx_comment_author", columnList = "author_user_id"),
      @Index(name = "idx_comment_created_at", columnList = "created_at"),
      @Index(name = "idx_comment_deleted_at", columnList = "deleted_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketComment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull(message = "Ticket is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id", nullable = false)
  private MaintenanceTicket ticket;

  @NotNull(message = "Author is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_user_id", nullable = false)
  private User author;

  @NotBlank(message = "Comment content is required")
  @Size(min = 5, max = 2000, message = "Comment must be between 5 and 2000 characters")
  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @NotNull(message = "Visibility is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private TicketCommentVisibility visibility = TicketCommentVisibility.PUBLIC;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /**
   * Check if the comment has been soft-deleted.
   *
   * @return true if deletedAt is not null
   */
  public boolean isDeleted() {
    return deletedAt != null;
  }

  /**
   * Soft-delete the comment by setting deletedAt to current time.
   */
  public void delete() {
    this.deletedAt = LocalDateTime.now();
  }
}
