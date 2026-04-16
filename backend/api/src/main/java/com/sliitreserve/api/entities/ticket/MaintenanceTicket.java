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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.facility.Facility;

/**
 * MaintenanceTicket entity representing a facility maintenance request.
 */
@Entity
@Table(name = "maintenance_ticket",
    indexes = {
      @Index(name = "idx_ticket_facility", columnList = "facility_id"),
      @Index(name = "idx_ticket_status", columnList = "status"),
      @Index(name = "idx_ticket_priority", columnList = "priority"),
      @Index(name = "idx_ticket_created_by", columnList = "created_by_user_id"),
      @Index(name = "idx_ticket_assigned_to", columnList = "assigned_technician_user_id"),
      @Index(name = "idx_ticket_created_at", columnList = "created_at"),
      @Index(name = "idx_ticket_sla_due_at", columnList = "sla_due_at"),
      @Index(name = "idx_ticket_escalation_level", columnList = "escalation_level")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceTicket {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull(message = "Facility is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "facility_id", nullable = false)
  private Facility facility;

  @NotNull(message = "Category is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private TicketCategory category;

  @NotNull(message = "Priority is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private TicketPriority priority;

  @NotNull(message = "Status is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private TicketStatus status = TicketStatus.OPEN;

  @Column(name = "rejection_reason", columnDefinition = "TEXT")
  private String rejectionReason;

  @NotBlank(message = "Title is required")
  @Size(min = 20, max = 200, message = "Title must be between 20 and 200 characters")
  @Column(nullable = false, length = 200)
  private String title;

  @NotBlank(message = "Description is required")
  @Size(min = 50, message = "Description must be at least 50 characters")
  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @NotNull(message = "Created by user is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_technician_user_id")
  private User assignedTechnician;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @NotNull(message = "SLA due date is required")
  @Column(name = "sla_due_at", nullable = false, updatable = false)
  private LocalDateTime slaDueAt;

  @Column(name = "escalation_level", nullable = false)
  @Builder.Default
  private Integer escalationLevel = 0;

  @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<TicketComment> comments = new ArrayList<>();

  @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<TicketAttachment> attachments = new ArrayList<>();

  @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<TicketEscalation> escalationHistory = new ArrayList<>();

  public boolean isTerminal() {
    return status == TicketStatus.CLOSED || status == TicketStatus.REJECTED;
  }

  public boolean isResolved() {
    return status == TicketStatus.RESOLVED;
  }

  public boolean isAssigned() {
    return assignedTechnician != null;
  }

  public boolean canTransitionTo(TicketStatus targetStatus) {
    if (isTerminal()) {
      return this.status == targetStatus;
    }
    return targetStatus != null && !targetStatus.equals(this.status);
  }

  public static int getMaxEscalationLevel() {
    return 3;
  }
}