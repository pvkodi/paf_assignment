package com.sliitreserve.api.controllers;

import com.sliitreserve.api.dto.BookingRequestDTO;
import com.sliitreserve.api.dto.BookingResponseDTO;
import com.sliitreserve.api.dto.CheckInRequest;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.exception.ForbiddenException;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.exception.ValidationException;
import com.sliitreserve.api.repositories.UserRepository;
import com.sliitreserve.api.repositories.BookingRepository;
import com.sliitreserve.api.services.booking.BookingService;
import com.sliitreserve.api.util.mapping.BookingMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for facility booking endpoints.
 * 
 * Endpoints:
 * - GET /bookings - Get pending bookings for current user's approval role
 * - GET /bookings/{id} - Get single booking details
 * - POST /bookings - Create new booking with capacity/overlap validation
 * - POST /bookings/{id}/check-in - Check in to a booking
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    /**
     * Create a new booking with capacity checks and overlap validation.
     * Supports admin bookedFor substitution.
     */
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

        // Admin on-behalf booking check
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
     * Get pending bookings for current user's approval role.
     * 
     * Returns bookings requiring approval based on user's role:
     * - LECTURER: bookings from USER role that need lecturer approval
     * - ADMIN: bookings awaiting admin approval
     * - FACILITY_MANAGER: bookings above high-capacity threshold
     * 
     * @param page Page number (0-indexed, default 0)
     * @param size Page size (default 20, max 100)
     * @param authentication Authenticated user context
     * @return Page of pending bookings for approval
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN', 'FACILITY_MANAGER')")
    public ResponseEntity<Page<BookingResponseDTO>> getPendingBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        log.info("Fetching pending bookings for user: {}", authentication.getName());

        try {
            User currentUser = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            int pageSize = Math.min(size, 100);
            Pageable pageable = PageRequest.of(page, pageSize);

            // Get pending bookings based on user's role
            Page<Booking> pendingBookings = bookingService.getPendingBookingsByApproverRole(
                    currentUser.getRoles(),
                    pageable
            );

            Page<BookingResponseDTO> response = pendingBookings
                    .map(bookingMapper::toResponseDTO);

            log.debug("Retrieved {} pending bookings for user {}", response.getSize(), currentUser.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching pending bookings for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get single booking details by ID.
     * 
     * Authorization:
     * - Booking owner can view their own booking
     * - Staff (TECHNICIAN, FACILITY_MANAGER, ADMIN) can view any booking
     * - Lecturers can view bookings in their approval chain
     * 
     * @param bookingId UUID of booking to retrieve
     * @param authentication Authenticated user context
     * @return BookingResponseDTO with full booking details and approval chain
     */
    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getBookingDetails(
            @PathVariable UUID bookingId,
            Authentication authentication) {
        
        log.info("Fetching booking details for ID: {}", bookingId);

        try {
            String email = (String) authentication.getPrincipal();
            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Retrieve booking
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

            // Authorization check
            boolean isOwner = booking.getBookedFor().getId().equals(currentUser.getId());
            boolean isStaff = currentUser.getRoles().stream()
                    .anyMatch(role -> role == Role.TECHNICIAN || role == Role.FACILITY_MANAGER || role == Role.ADMIN);
            boolean isApprover = currentUser.getRoles().stream()
                    .anyMatch(role -> role == Role.LECTURER);
            
            if (!isOwner && !isStaff && !isApprover) {
                throw new ForbiddenException("Not authorized to view this booking");
            }

            BookingResponseDTO response = bookingMapper.toResponseDTO(booking);
            log.debug("Retrieved booking {} for user {}", bookingId, currentUser.getId());
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.warn("Booking not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (ForbiddenException e) {
            log.warn("User not authorized to view booking: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error retrieving booking {}: {}", bookingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check in to a booking via QR code or manual staff entry.
     * 
     * Records attendance and triggers no-show evaluation if applicable.
     * Check-in must occur within 15 minutes of booking start time.
     * 
     * @param bookingId UUID of booking to check in
     * @param request Check-in request (QR code or manual)
     * @param authentication Authenticated user context
     * @return Updated BookingResponse with status CHECKED_IN
     */
    @PostMapping("/{bookingId}/check-in")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkInToBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CheckInRequest request,
            Authentication authentication) {
        
        log.info("Check-in attempt for booking {}", bookingId);

        try {
            String email = (String) authentication.getPrincipal();
            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Verify booking exists and user has access
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

            // Check authorization: student, staff, or admin can check in
            boolean isOwner = booking.getBookedFor().getId().equals(currentUser.getId());
            boolean isStaff = currentUser.getRoles().stream()
                    .anyMatch(role -> role == Role.TECHNICIAN || role == Role.FACILITY_MANAGER || role == Role.ADMIN);
            
            if (!isOwner && !isStaff) {
                throw new ForbiddenException("Not authorized to check in to this booking");
            }

            // Perform check-in with user tracking
            Booking updatedBooking = bookingService.checkInBooking(bookingId, request.getMethod(), currentUser);
            
            log.info("Check-in successful for booking {} by {}", bookingId, email);
            return ResponseEntity.ok(bookingMapper.toResponseDTO(updatedBooking));

        } catch (ValidationException e) {
            log.warn("Invalid check-in: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (ForbiddenException e) {
            log.warn("User not authorized for check-in: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (ResourceNotFoundException e) {
            log.warn("Booking not found for check-in: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error during check-in for booking {}: {}", bookingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
