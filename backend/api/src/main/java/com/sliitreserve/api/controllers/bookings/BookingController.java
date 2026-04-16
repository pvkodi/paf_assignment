package com.sliitreserve.api.controllers.bookings;

import com.sliitreserve.api.dto.bookings.BookingRequestDTO;
import com.sliitreserve.api.dto.bookings.BookingResponseDTO;
import com.sliitreserve.api.dto.bookings.BookingDetailedResponseDTO;
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
    public ResponseEntity<List<com.sliitreserve.api.dto.bookings.BookingDetailedResponseDTO>> getUserBookings(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        List<com.sliitreserve.api.dto.bookings.BookingDetailedResponseDTO> response = bookingService.getUserBookingsDetailed(currentUser.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * Get pending bookings that require approval by the current user.
     * LECTURER, FACILITY_MANAGER, and ADMIN can approve.
     * Must be defined BEFORE /{bookingId} to avoid route matching issues.
     */
    @GetMapping("/pending-approvals")
    @PreAuthorize("hasAnyRole('LECTURER', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<List<com.sliitreserve.api.dto.bookings.BookingDetailedResponseDTO>> getPendingApprovals(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        List<com.sliitreserve.api.dto.bookings.BookingDetailedResponseDTO> response = bookingService.getPendingApprovalsDetailed(currentUser);

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
    public ResponseEntity<com.sliitreserve.api.dto.bookings.BookingDetailedResponseDTO> getBooking(
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

        com.sliitreserve.api.dto.bookings.BookingDetailedResponseDTO response = bookingService.getBookingDetailed(bookingId);
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
        
        Booking approvedBooking = bookingService.approveBooking(bookingId, expectedVersion, note);
        
        BookingResponseDTO response = bookingMapper.toResponseDTO(approvedBooking);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a pending booking.
     * Only LECTURER, FACILITY_MANAGER, and ADMIN can reject.
     * Rejection reason is required.
     */
    @PostMapping("/{bookingId}/reject")
    @PreAuthorize("hasAnyRole('LECTURER', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<BookingResponseDTO> rejectBooking(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) Long version) {

        Booking booking = bookingService.getBooking(bookingId);
        Long expectedVersion = version != null ? version : booking.getVersion();
        
        Booking rejectedBooking = bookingService.rejectBooking(bookingId, expectedVersion, note);
        
        BookingResponseDTO response = bookingMapper.toResponseDTO(rejectedBooking);
        return ResponseEntity.ok(response);
    }

    /**
     * Get available timeslots for a facility on a specific date.
     * Used by booking form to show available/booked times visually.
     * 
     * @param facilityId Facility ID
     * @param date Date in format YYYY-MM-DD
     * @return Available timeslots and booked ranges
     */
    @GetMapping("/availability/{facilityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getAvailableTimeslots(
            @PathVariable UUID facilityId,
            @RequestParam String date) {
        try {
            java.time.LocalDate bookingDate = java.time.LocalDate.parse(date);
            java.util.List<java.util.Map<String, Object>> slots = bookingService.getAvailableTimeslots(facilityId, bookingDate);
            return ResponseEntity.ok(slots);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Invalid date format or facility not found");
        }
    }

    /**
     * Get all bookings for admin/facility-manager dashboard.
     * Return all approved and upcoming bookings.
     * Admin sees all bookings; Facility Manager sees only their facilities.
     * 
     * @param facilityId Optional facility filter UUID
     * @param status Optional status filter (APPROVED, PENDING, CANCELLED, REJECTED)
     * @param from Start date (YYYY-MM-DD)
     * @param to End date (YYYY-MM-DD)
     * @return List of bookings
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACILITY_MANAGER')")
    public ResponseEntity<java.util.List<BookingResponseDTO>> getAdminBookings(
            @RequestParam(required = false) UUID facilityId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            Authentication authentication) {
        
        try {
            String email = (String) authentication.getPrincipal();
            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

            // Parse dates - default to current month if not provided
            java.time.LocalDate startDate = from != null ? java.time.LocalDate.parse(from) : java.time.LocalDate.now().withDayOfMonth(1);
            java.time.LocalDate endDate = to != null ? java.time.LocalDate.parse(to) : java.time.LocalDate.now().withDayOfMonth(java.time.LocalDate.now().lengthOfMonth());

            // Parse status filter
            java.util.List<com.sliitreserve.api.entities.booking.BookingStatus> statuses = new java.util.ArrayList<>();
            if (status != null && !status.isEmpty()) {
                statuses.add(com.sliitreserve.api.entities.booking.BookingStatus.valueOf(status));
            } else {
                statuses.addAll(java.util.Arrays.asList(
                    com.sliitreserve.api.entities.booking.BookingStatus.APPROVED,
                    com.sliitreserve.api.entities.booking.BookingStatus.PENDING
                ));
            }

            java.util.List<Booking> bookings = bookingService.getAdminBookings(facilityId, statuses, startDate, endDate);
            java.util.List<BookingResponseDTO> response = bookings.stream()
                    .map(bookingMapper::toResponseDTO)
                    .toList();

            return ResponseEntity.ok(response);
        } catch (java.time.format.DateTimeParseException e) {
            throw new ResourceNotFoundException("Invalid date format. Use YYYY-MM-DD");
        }
    }
}
