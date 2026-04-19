package com.sliitreserve.api.repositories.ticket;

import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.entities.ticket.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for TicketAttachment entity persistence operations.
 *
 * <p>Provides CRUD operations and custom queries for managing ticket attachments,
 * including querying by ticket, ordering by upload time, and counting attachments
 * to enforce the 3-attachment-per-ticket limit.
 *
 * @see TicketAttachment
 * @see MaintenanceTicket
 */
@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, UUID> {

  /**
   * Find all attachments for a given ticket, ordered by upload time (most recent first).
   *
   * @param ticket the ticket to find attachments for
   * @return list of TicketAttachments for the ticket, ordered by uploadedAt descending
   */
  List<TicketAttachment> findByTicketOrderByUploadedAtDesc(MaintenanceTicket ticket);

  /**
   * Count attachments for a given ticket to enforce the limit.
   *
   * @param ticket the ticket to count attachments for
   * @return the number of attachments for the ticket
   */
  long countByTicket(MaintenanceTicket ticket);

  /**
   * Delete all attachments for a given ticket (cascade on ticket deletion).
   *
   * @param ticket the ticket whose attachments should be deleted
   */
  void deleteAllByTicket(MaintenanceTicket ticket);
}
