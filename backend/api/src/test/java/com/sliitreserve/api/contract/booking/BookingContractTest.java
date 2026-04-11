package com.sliitreserve.api.contract.booking;

import com.sliitreserve.api.controllers.BookingController;
import com.sliitreserve.api.controllers.advice.GlobalExceptionHandler;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.BookingStatus;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.exception.ConflictException;
import com.sliitreserve.api.repositories.UserRepository;
import com.sliitreserve.api.services.booking.BookingService;
import com.sliitreserve.api.util.mapping.BookingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingContractTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;
    @BeforeEach
    void setUp() {
        BookingController bookingController = new BookingController(bookingService, userRepository, new BookingMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createBooking_returns201WithCurrentSnakeCaseContract() throws Exception {
        UUID facilityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User currentUser = user(userId, "student@sliit.lk", Role.USER);
        when(userRepository.findByEmail("student@sliit.lk")).thenReturn(Optional.of(currentUser));

        Booking created = booking(facilityId, userId, userId, BookingStatus.PENDING);
        when(bookingService.createBooking(any(), any(), any(), any(), any(), any(), anyString(), anyInt(), nullable(String.class)))
                .thenReturn(created);

        String payload = """
                {
                  "facility_id": "%s",
                  "booking_date": "%s",
                  "start_time": "09:00:00",
                  "end_time": "10:00:00",
                  "purpose": "Lecture",
                  "attendees": 40,
                  "recurrence_rule": ""
                }
                """.formatted(facilityId, LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/v1/bookings")
                        .principal(auth("student@sliit.lk"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.facility_id").value(facilityId.toString()))
                .andExpect(jsonPath("$.requested_by_user_id").value(userId.toString()))
                .andExpect(jsonPath("$.booked_for_user_id").value(userId.toString()))
                .andExpect(jsonPath("$.booking_date").value(LocalDate.now().plusDays(1).toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.timezone").value("Asia/Colombo"));
    }

    @Test
    void createBooking_nonAdminOnBehalfRequest_returns403ForbiddenContract() throws Exception {
        UUID facilityId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID bookedForId = UUID.randomUUID();

        when(userRepository.findByEmail("student@sliit.lk"))
                .thenReturn(Optional.of(user(requesterId, "student@sliit.lk", Role.USER)));

        String payload = """
                {
                  "facility_id": "%s",
                  "booked_for_user_id": "%s",
                  "booking_date": "%s",
                  "start_time": "09:00:00",
                  "end_time": "10:00:00",
                  "purpose": "Proxy booking",
                  "attendees": 20
                }
                """.formatted(facilityId, bookedForId, LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/v1/bookings")
                        .principal(auth("student@sliit.lk"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(containsString("Only ADMIN users can book on behalf of others")));
    }

    @Test
    void createBooking_adminOnBehalfRequest_usesBookedForUserId() throws Exception {
        UUID facilityId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(userRepository.findByEmail("admin@sliit.lk"))
                .thenReturn(Optional.of(user(adminId, "admin@sliit.lk", Role.ADMIN)));

        Booking created = booking(facilityId, adminId, targetUserId, BookingStatus.PENDING);
        when(bookingService.createBooking(any(), any(), any(), any(), any(), any(), anyString(), anyInt(), nullable(String.class)))
                .thenReturn(created);

        String payload = """
                {
                  "facility_id": "%s",
                  "booked_for_user_id": "%s",
                  "booking_date": "%s",
                  "start_time": "11:00:00",
                  "end_time": "12:00:00",
                  "purpose": "Admin-assisted booking",
                  "attendees": 15
                }
                """.formatted(facilityId, targetUserId, LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/v1/bookings")
                        .principal(auth("admin@sliit.lk"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requested_by_user_id").value(adminId.toString()))
                .andExpect(jsonPath("$.booked_for_user_id").value(targetUserId.toString()));

        ArgumentCaptor<UUID> bookedForCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(bookingService).createBooking(
                any(),
                any(),
                bookedForCaptor.capture(),
                any(),
                any(),
                any(),
                anyString(),
                anyInt(),
                nullable(String.class)
        );

        org.junit.jupiter.api.Assertions.assertEquals(targetUserId, bookedForCaptor.getValue());
    }

    @Test
    void createBooking_missingRequiredFields_returns400ValidationContract() throws Exception {
        String payload = """
                {
                  "booking_date": "%s",
                  "start_time": "09:00:00",
                  "end_time": "10:00:00",
                  "purpose": "Incomplete payload",
                  "attendees": 10
                }
                """.formatted(LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/v1/bookings")
                        .principal(auth("student@sliit.lk"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    void createBooking_whenUserMissing_returns404ResourceNotFoundContract() throws Exception {
        UUID facilityId = UUID.randomUUID();
        when(userRepository.findByEmail("ghost@sliit.lk")).thenReturn(Optional.empty());

        String payload = """
                {
                  "facility_id": "%s",
                  "booking_date": "%s",
                  "start_time": "09:00:00",
                  "end_time": "10:00:00",
                  "purpose": "Lecture",
                  "attendees": 30
                }
                """.formatted(facilityId, LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/v1/bookings")
                        .principal(auth("ghost@sliit.lk"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(containsString("User not found")));
    }

    @Test
    void createBooking_whenServiceSignalsConflict_returns409ConflictContract() throws Exception {
        UUID facilityId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(userRepository.findByEmail("student@sliit.lk"))
                .thenReturn(Optional.of(user(requesterId, "student@sliit.lk", Role.USER)));
        when(bookingService.createBooking(any(), any(), any(), any(), any(), any(), anyString(), anyInt(), nullable(String.class)))
                .thenThrow(new ConflictException("The facility is already booked during the requested time slot."));

        String payload = """
                {
                  "facility_id": "%s",
                  "booking_date": "%s",
                  "start_time": "09:00:00",
                  "end_time": "10:00:00",
                  "purpose": "Conflict case",
                  "attendees": 25
                }
                """.formatted(facilityId, LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/v1/bookings")
                        .principal(auth("student@sliit.lk"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value(containsString("already booked")));
    }

    private User user(UUID id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setDisplayName("Test User");
        user.setGoogleSubject("subject-" + id);
        user.setRoles(Set.of(role));
        return user;
    }

    private Booking booking(UUID facilityId, UUID requestedById, UUID bookedForId, BookingStatus status) {
        Facility facility = new Facility();
        facility.setId(facilityId);

        User requestedBy = new User();
        requestedBy.setId(requestedById);

        User bookedFor = new User();
        bookedFor.setId(bookedForId);

        return Booking.builder()
                .id(UUID.randomUUID())
                .facility(facility)
                .requestedBy(requestedBy)
                .bookedFor(bookedFor)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .purpose("Lecture")
                .attendees(40)
                .status(status)
                .timezone("Asia/Colombo")
                .version(1L)
                .build();
    }

    private Authentication auth(String email) {
        return new UsernamePasswordAuthenticationToken(email, "N/A");
    }
}
