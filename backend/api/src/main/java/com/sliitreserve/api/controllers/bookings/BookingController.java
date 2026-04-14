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
}
