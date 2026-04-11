package com.sliitreserve.api.repositories.ticket;

import com.sliitreserve.api.entities.ticket.TicketComment;
import com.sliitreserve.api.entities.ticket.TicketCommentVisibility;
import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for TicketComment entity persistence operations.
 *
 * <p>Provides CRUD operations and custom queries for managing ticket comments,
 * including visibility-aware queries for filtering comments based on user role.
 *
 * @see TicketComment
 * @see MaintenanceTicket
 */
@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {

  /**
   * Find all non-deleted comments for a ticket, ordered by creation time (ascending).
   *
   * @param ticket the ticket to find comments for
   * @return list of active (not soft-deleted) comments for the ticket
   */
  List<TicketComment> findByTicketAndDeletedAtIsNullOrderByCreatedAtAsc(MaintenanceTicket ticket);

  /**
   * Find all comments with a specific visibility for a ticket, excluding soft-deleted comments.
   *
   * @param ticket the ticket to find comments for
   * @param visibility the comment visibility to filter by (PUBLIC or INTERNAL)
   * @return list of active comments with the specified visibility
   */
  List<TicketComment> findByTicketAndVisibilityAndDeletedAtIsNullOrderByCreatedAtAsc(
      MaintenanceTicket ticket, TicketCommentVisibility visibility);

  /**
   * Find comments for a ticket by the given author, excluding soft-deleted comments.
   *
   * @param ticket the ticket to search for comments
   * @param authorId the comment author ID
   * @return list of comments authored by the user on this ticket
   */
  List<TicketComment> findByTicketAndAuthorIdAndDeletedAtIsNull(MaintenanceTicket ticket, UUID authorId);

  /**
   * Count active (non-deleted) comments on a ticket.
   *
   * @param ticket the ticket to count comments for
   * @return count of active comments
   */
  long countByTicketAndDeletedAtIsNull(MaintenanceTicket ticket);

  /**
   * Delete all comments for a given ticket (cascade on ticket deletion).
   *
   * @param ticket the ticket whose comments should be deleted
   */
  void deleteAllByTicket(MaintenanceTicket ticket);
}
