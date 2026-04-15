package com.sliitreserve.api.controllers.bookings;

import com.sliitreserve.api.dto.bookings.BookingRequestDTO;
import com.sliitreserve.api.dto.bookings.BookingResponseDTO;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.exception.ForbiddenException;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.services.booking.BookingService;
import com.sliitreserve.api.util.mapping.BookingMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponseDTO> createBooking(
            @Valid @RequestBody BookingRequestDTO request,
            Authentication authentication) {

        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        UUID requestedBy = currentUser.getId();
        UUID bookedFor = requestedBy;

        if (request.getBookedForUserId() != null && !request.getBookedForUserId().equals(requestedBy)) {
            boolean isAdmin = currentUser.getRoles().contains(Role.ADMIN);
            if (!isAdmin) {
                throw new ForbiddenException("Only ADMIN users can book on behalf of others");
            }
            bookedFor = request.getBookedForUserId();
        }

        Booking booking = bookingService.createBooking(
                request.getFacilityId(),
                requestedBy,
                bookedFor,
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getPurpose(),
                request.getAttendees(),
                request.getRecurrenceRule()
        );

        BookingResponseDTO response = bookingMapper.toResponseDTO(booking);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all bookings for the current authenticated user.
     * Returns bookings where user is either the requester or the bookedFor user.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BookingResponseDTO>> getUserBookings(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        List<Booking> bookings = bookingService.getUserBookings(currentUser.getId());
        List<BookingResponseDTO> response = bookings.stream()
                .map(bookingMapper::toResponseDTO)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Get pending bookings that require approval by the current user.
     * LECTURER, FACILITY_MANAGER, and ADMIN can approve.
     * Must be defined BEFORE /{bookingId} to avoid route matching issues.
     */
    @GetMapping("/pending-approvals")
    @PreAuthorize("hasAnyRole('LECTURER', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<List<BookingResponseDTO>> getPendingApprovals(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        List<Booking> pendingBookings = bookingService.getPendingApprovalsForUser(currentUser);
        List<BookingResponseDTO> response = pendingBookings.stream()
                .map(bookingMapper::toResponseDTO)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Get quota status for the current user.
     * Must be defined BEFORE /{bookingId} to avoid route matching issues.
     */
    @GetMapping("/quota-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, Object>> getQuotaStatus(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        java.util.Map<String, Object> quotaStatus = bookingService.getQuotaStatus(currentUser);
        return ResponseEntity.ok(quotaStatus);
    }

    /**
     * Get a specific booking by ID.
     */
    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponseDTO> getBooking(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Booking booking = bookingService.getBooking(bookingId);

        // Authorization: User can only view their own bookings
        if (!booking.getRequestedBy().getId().equals(currentUser.getId()) &&
            !booking.getBookedFor().getId().equals(currentUser.getId()) &&
            !currentUser.getRoles().contains(Role.ADMIN)) {
            throw new ForbiddenException("You do not have permission to view this booking");
        }

        BookingResponseDTO response = bookingMapper.toResponseDTO(booking);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve a pending booking.
     * Only LECTURER, FACILITY_MANAGER, and ADMIN can approve.
     */
    @PostMapping("/{bookingId}/approve")
    @PreAuthorize("hasAnyRole('LECTURER', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<BookingResponseDTO> approveBooking(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) Long version) {

        Booking booking = bookingService.getBooking(bookingId);
        Long expectedVersion = version != null ? version : booking.getVersion();
        
        Booking approvedBooking = bookingService.approveBooking(bookingId, expectedVersion);
        // TODO: Handle approval note if needed
        
        BookingResponseDTO response = bookingMapper.toResponseDTO(approvedBooking);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a pending booking.
     * Only LECTURER, FACILITY_MANAGER, and ADMIN can reject.
     */
    @PostMapping("/{bookingId}/reject")
    @PreAuthorize("hasAnyRole('LECTURER', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<BookingResponseDTO> rejectBooking(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) Long version) {

        Booking booking = bookingService.getBooking(bookingId);
        Long expectedVersion = version != null ? version : booking.getVersion();
        
        Booking rejectedBooking = bookingService.rejectBooking(bookingId, expectedVersion);
        // TODO: Handle rejection note if needed
        
        BookingResponseDTO response = bookingMapper.toResponseDTO(rejectedBooking);
        return ResponseEntity.ok(response);
    }
}
