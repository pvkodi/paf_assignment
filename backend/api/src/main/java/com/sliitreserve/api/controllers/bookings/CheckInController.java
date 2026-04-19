package com.sliitreserve.api.controllers.bookings;

import com.sliitreserve.api.dto.bookings.CheckInRequestDTO;
import com.sliitreserve.api.dto.bookings.CheckInResponseDTO;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.booking.CheckInMethod;
import com.sliitreserve.api.entities.booking.CheckInRecord;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.services.auth.SuspensionPolicyService;
import com.sliitreserve.api.services.booking.CheckInService;
import com.sliitreserve.api.util.mapping.CheckInMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * CheckInController for managing booking check-ins.
 * 
 * Handles:
 * - QR code check-ins (user-initiated)
 * - Manual staff check-ins (staff/admin initiated)
 * - No-show evaluation queries
 * 
 * Requirements:
 * - FR-020: Support check-in via QR code and manual staff check-in
 * - FR-021: Classify no-show when check-in does not occur within 15 minutes of booking start
 * - FR-022: Apply automatic 1-week suspension after 3 no-shows
 * - FR-003: Block suspended users from check-in operations
 * 
 * Protection: All endpoints require authentication and check suspension policy
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;
    private final SuspensionPolicyService suspensionPolicyService;
    private final UserRepository userRepository;
    private final CheckInMapper checkInMapper;

    /**
     * Record a QR code check-in for a booking.
     * 
     * QR check-ins are user-initiated (user scans QR code at the facility).
     * The checkedInBy field is optional for QR scans.
     * 
     * Endpoint: POST /api/v1/bookings/{bookingId}/check-in/qr
     * 
     * @param bookingId Booking ID to check in for
     * @param authentication Current authenticated user
     * @return CheckInResponseDTO with check-in details
     * @throws ResourceNotFoundException if booking not found
     * @throws ValidationException if check-in already recorded
     * @throws ForbiddenException if user is suspended
     */
    @PostMapping("/{bookingId}/check-in/qr")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CheckInResponseDTO> recordQRCheckIn(
            @PathVariable UUID bookingId,
            Authentication authentication) {
        
        log.info("QR Check-in request for booking: {} by user: {}", bookingId, authentication.getPrincipal());
        
        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        // FR-003: Check suspension policy before allowing check-in
        suspensionPolicyService.checkSuspensionPolicy(currentUser);
        
        CheckInRecord checkIn = checkInService.recordQRCheckIn(bookingId, currentUser.getId());
        CheckInResponseDTO response = checkInMapper.toResponseDTO(checkIn);
        
        log.info("QR check-in recorded successfully: booking={}, user={}", bookingId, currentUser.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Record a manual staff check-in for a booking.
     * 
     * Manual check-ins are performed by staff/admin (e.g., via attendance sheet, admin interface).
     * Required: Only TECHNICIAN or ADMIN can perform manual check-ins.
     * 
     * Endpoint: POST /api/v1/bookings/{bookingId}/check-in/manual
     * 
     * @param bookingId Booking ID to check in for
     * @param authentication Current authenticated user (must be TECHNICIAN/ADMIN)
     * @return CheckInResponseDTO with check-in details
     * @throws ResourceNotFoundException if booking not found
     * @throws ValidationException if check-in already recorded
     * @throws ForbiddenException if user is not technician/admin or is suspended
     */
    @PostMapping("/{bookingId}/check-in/manual")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<CheckInResponseDTO> recordManualCheckIn(
            @PathVariable UUID bookingId,
            Authentication authentication) {
        
        log.info("Manual check-in request for booking: {} by staff: {}", bookingId, authentication.getPrincipal());
        
        String email = (String) authentication.getPrincipal();
        User staffUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found with email: " + email));
        
        // FR-003: Even staff must not be suspended (though rare, security principle)
        suspensionPolicyService.checkSuspensionPolicy(staffUser);
        
        CheckInRecord checkIn = checkInService.recordManualCheckIn(bookingId, staffUser.getId());
        CheckInResponseDTO response = checkInMapper.toResponseDTO(checkIn);
        
        log.info("Manual check-in recorded successfully: booking={}, staff={}", bookingId, staffUser.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Record a check-in for a booking (generic endpoint).
     * 
     * This is the main check-in endpoint that supports both QR and manual methods.
     * Method is specified in the request body.
     * 
     * Endpoint: POST /api/v1/bookings/{bookingId}/check-in
     * 
     * @param bookingId Booking ID to check in for
     * @param request CheckInRequestDTO with method and optional details
     * @param authentication Current authenticated user
     * @return CheckInResponseDTO with check-in details
     */
    @PostMapping("/{bookingId}/check-in")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CheckInResponseDTO> recordCheckIn(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CheckInRequestDTO request,
            Authentication authentication) {
        
        log.info("Check-in request for booking: {} using method: {}", bookingId, request.getMethod());
        
        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        // FR-003: Check suspension policy
        suspensionPolicyService.checkSuspensionPolicy(currentUser);
        
        CheckInRecord checkIn;
        
        switch (request.getMethod()) {
            case QR -> {
                checkIn = checkInService.recordQRCheckIn(bookingId, currentUser.getId());
                log.info("QR check-in recorded via generic endpoint: booking={}", bookingId);
            }
            case MANUAL -> {
                if (!currentUser.getRoles().contains(Role.TECHNICIAN) && !currentUser.getRoles().contains(Role.ADMIN)) {
                    log.warn("Non-staff user {} attempted manual check-in", currentUser.getId());
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Only TECHNICIAN and ADMIN can perform manual check-ins"
                    );
                }
                checkIn = checkInService.recordManualCheckIn(bookingId, currentUser.getId());
                log.info("Manual check-in recorded via generic endpoint: booking={}", bookingId);
            }
            default -> throw new IllegalArgumentException("Unknown check-in method: " + request.getMethod());
        }
        
        CheckInResponseDTO response = checkInMapper.toResponseDTO(checkIn);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Check if a booking has been checked in.
     * 
     * Query endpoint to determine if user has checked in to a booking.
     * Returns boolean status.
     * 
     * Endpoint: GET /api/v1/bookings/{bookingId}/check-in/status
     * 
     * @param bookingId Booking ID to check
     * @param authentication Current authenticated user
     * @return true if booking has a check-in, false otherwise
     */
    @GetMapping("/{bookingId}/check-in/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> isCheckedIn(
            @PathVariable UUID bookingId,
            Authentication authentication) {
        
        log.debug("Check-in status query for booking: {}", bookingId);
        boolean checkedIn = checkInService.isCheckedIn(bookingId);
        
        return ResponseEntity.ok(checkedIn);
    }

    /**
     * Get all check-in records for a booking.
     * 
     * Returns list of check-in records (typically 0 or 1, may be more if data anomaly).
     * 
     * Endpoint: GET /api/v1/bookings/{bookingId}/check-ins
     * 
     * @param bookingId Booking ID to get check-ins for
     * @param authentication Current authenticated user
     * @return List of CheckInResponseDTO objects
     */
    @GetMapping("/{bookingId}/check-ins")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CheckInResponseDTO>> getCheckInsByBooking(
            @PathVariable UUID bookingId,
            Authentication authentication) {
        
        log.debug("Fetching check-in records for booking: {}", bookingId);
        List<CheckInRecord> checkIns = checkInService.getCheckInsByBooking(bookingId);
        List<CheckInResponseDTO> response = checkIns.stream()
                .map(checkInMapper::toResponseDTO)
                .toList();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Evaluate if a booking should be classified as a no-show.
     * 
     * Administrative endpoint to evaluate no-show status (typically used by scheduler jobs).
     * Only ADMIN can call this explicitly.
     * 
     * A booking is a no-show if:
     * - No check-in record exists AND
     * - Current time is more than 15 minutes after booking start (grace period expired)
     * 
     * Endpoint: GET /api/v1/bookings/{bookingId}/evaluate-no-show
     * 
     * @param bookingId Booking ID to evaluate
     * @param authentication Current authenticated user (must be ADMIN)
     * @return true if booking is a no-show, false otherwise
     */
    @GetMapping("/{bookingId}/evaluate-no-show")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> evaluateNoShow(
            @PathVariable UUID bookingId,
            Authentication authentication) {
        
        log.info("No-show evaluation requested for booking: {} by admin: {}", bookingId, authentication.getPrincipal());
        boolean isNoShow = checkInService.evaluateNoShow(bookingId);
        
        return ResponseEntity.ok(isNoShow);
    }

    /**
     * Record a check-in with geofencing verification (WiFi + GPS).
     * 
     * Enhanced check-in endpoint that verifies user location before recording attendance.
     * Uses dual-layer geofencing: WiFi (primary) + GPS (backup/secondary).
     * 
     * Verification logic:
     * 1. Verify user device is connected to facility WiFi (SSID match)
     * 2. Verify user is within GPS radius of facility (fallback)
     * 3. Record check-in if verification passes
     * 
     * Geofencing ensures:
     * - Users cannot check in remotely
     * - Users cannot check in from outside the facility premises
     * - Facilities without geofencing configured allow check-in without verification
     * 
     * Endpoint: POST /api/v1/bookings/{bookingId}/check-in/with-geofencing
     * 
     * @param bookingId Booking ID to check in for
     * @param request CheckInRequestDTO with method, WiFi, and GPS data
     * @param authentication Current authenticated user
     * @return CheckInResponseDTO with check-in details (HTTP 201)
     * @throws ResourceNotFoundException if booking not found
     * @throws ValidationException if check-in already recorded
     * @throws ForbiddenException if geofencing verification fails (WiFi or GPS)
     * @throws ForbiddenException if user is suspended
     */
    @PostMapping("/{bookingId}/check-in/with-geofencing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CheckInResponseDTO> recordCheckInWithGeofencing(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CheckInRequestDTO request,
            Authentication authentication) {
        
        log.info("========================================");
        log.info("🔵 GEOFENCING CHECK-IN ENDPOINT HIT");
        log.info("========================================");
        log.info("Booking ID: {}", bookingId);
        log.info("Method: {}", request.getMethod());

        log.info("GPS: ({}, {})", request.getLatitude(), request.getLongitude());
        log.info("User: {}", authentication.getPrincipal());
        
        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        log.info("User found: {} (ID: {})", currentUser.getEmail(), currentUser.getId());
        
        // FR-003: Check suspension policy before allowing check-in
        log.info("Checking suspension policy for user: {}", currentUser.getId());
        suspensionPolicyService.checkSuspensionPolicy(currentUser);
        log.info("✓ User is not suspended");
        
        // Determine checkedInBy user
        UUID checkedInByUserId = null;
        if (request.getMethod() == CheckInMethod.MANUAL) {
            // For manual check-in, staff performing the check-in is recorded
            checkedInByUserId = currentUser.getId();
        }
        // For QR check-in, checkedInBy is optional (can be null)
        
        // Record check-in with geofencing verification (GPS-only)
        CheckInRecord checkIn = checkInService.recordCheckInWithGeofencing(
            bookingId,
            checkedInByUserId,
            request.getMethod().name(),
            request.getLatitude(),
            request.getLongitude()
        );
        
        CheckInResponseDTO response = checkInMapper.toResponseDTO(checkIn);
        
        log.info("========================================");
        log.info("✅ CHECK-IN SUCCESSFUL");
        log.info("========================================");
        log.info("Check-in ID: {}", checkIn.getId());
        log.info("Booking ID: {}", bookingId);
        log.info("User: {}", currentUser.getEmail());
        log.info("Method: {}", request.getMethod());
        log.info("Timestamp: {}", checkIn.getCheckedInAt());
        log.info("HTTP Status: 201 CREATED");
        log.info("========================================");
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}

