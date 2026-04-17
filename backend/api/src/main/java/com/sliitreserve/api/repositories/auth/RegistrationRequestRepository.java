package com.sliitreserve.api.repositories.auth;

import com.sliitreserve.api.entities.auth.RegistrationRequest;
import com.sliitreserve.api.repositories.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RegistrationRequest entities.
 * Provides query methods for admin approval workflows and registration management.
 */
@Repository
public interface RegistrationRequestRepository
    extends BaseRepository<RegistrationRequest, UUID> {

  /**
   * Find a registration request by email.
   * 
   * @param email The email to search for
   * @return Optional containing the registration request if found
   */
  Optional<RegistrationRequest> findByEmail(String email);

  /**
   * Find all registration requests with a specific status, paginated.
   * Used by admin UI to show registration requests (PENDING, APPROVED, REJECTED).
   * 
   * @param status The status to filter by
   * @param pageable Pagination parameters (sorting, page size, etc.)
   * @return Page of matching registration requests
   */
  Page<RegistrationRequest> findByStatus(RegistrationRequest.RegistrationRequestStatus status, Pageable pageable);

  /**
   * Find all pending registration requests (status = PENDING), paginated.
   * Shorthand for filtering by PENDING status.
   * 
   * @param pageable Pagination parameters
   * @return Page of pending registration requests
   */
  @Query("SELECT r FROM RegistrationRequest r WHERE r.status = 'PENDING' ORDER BY r.createdAt DESC")
  Page<RegistrationRequest> findAllPending(Pageable pageable);

  /**
   * Find all pending requests for a specific email.
   * Used to check if email already has a pending registration request.
   * 
   * @param email The email to search for
   * @return List of pending requests with this email
   */
  List<RegistrationRequest> findByEmailAndStatus(String email, RegistrationRequest.RegistrationRequestStatus status);

  /**
   * Check if email already exists as a pending registration request.
   * 
   * @param email The email to check
   * @return true if a pending request exists for this email
   */
  @Query("SELECT COUNT(r) > 0 FROM RegistrationRequest r WHERE r.email = :email AND r.status = 'PENDING'")
  boolean existsByEmailAndPendingStatus(@Param("email") String email);
}
