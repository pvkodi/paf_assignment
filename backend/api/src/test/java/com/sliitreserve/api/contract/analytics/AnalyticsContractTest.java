package com.sliitreserve.api.contract.analytics;

import com.sliitreserve.api.controllers.analytics.AnalyticsController;
import com.sliitreserve.api.controllers.notifications.NotificationController;
import com.sliitreserve.api.dto.analytics.UtilizationResponse;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.notification.Notification;
import com.sliitreserve.api.entities.notification.NotificationChannel;
import com.sliitreserve.api.observers.EventSeverity;
import com.sliitreserve.api.repositories.notification.NotificationRepository;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.services.analytics.UtilizationAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Analytics and Notification Contract Tests")
class AnalyticsContractTest {

    private MockMvc analyticsMockMvc;
    private MockMvc notificationMockMvc;

    private UtilizationAnalyticsService analyticsService;
    private NotificationRepository notificationRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        analyticsService = Mockito.mock(UtilizationAnalyticsService.class);
        AnalyticsController analyticsController = new AnalyticsController();
        ReflectionTestUtils.setField(analyticsController, "analyticsService", analyticsService);
        analyticsMockMvc = MockMvcBuilders.standaloneSetup(analyticsController).build();

        notificationRepository = Mockito.mock(NotificationRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        NotificationController notificationController = new NotificationController();
        ReflectionTestUtils.setField(notificationController, "notificationRepository", notificationRepository);
        ReflectionTestUtils.setField(notificationController, "userRepository", userRepository);
        notificationMockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/utilization")
    class AnalyticsEndpointContract {

        @Test
        @DisplayName("returns analytics payload with heatmap and insights")
        void getUtilizationAnalytics_returnsExpectedSchema() throws Exception {
            UtilizationResponse payload = UtilizationResponse.builder()
                .heatmap(List.of(
                    UtilizationResponse.HeatmapEntry.builder()
                        .facilityId(UUID.randomUUID())
                        .facilityName("Main Hall")
                        .dayOfWeek(1)
                        .hourOfDay(9)
                        .utilizationPercent(new BigDecimal("41.25"))
                        .build()
                ))
                .underutilizedFacilities(List.of(
                    UtilizationResponse.UnderutilizedFacility.builder()
                        .facilityId(UUID.randomUUID())
                        .facilityName("Lab C")
                        .utilizationPercent(new BigDecimal("18.40"))
                        .consecutiveUnderutilizedDays(8)
                        .recommendation("Monitor closely")
                        .build()
                ))
                .recommendations(List.of(
                    UtilizationResponse.RecommendedAlternative.builder()
                        .requestedFacilityId(UUID.randomUUID())
                        .alternativeFacilityId(UUID.randomUUID())
                        .alternativeFacilityName("Hall B")
                        .capacity(120)
                        .utilizationPercent(new BigDecimal("54.00"))
                        .reason("Capacity-fit alternative")
                        .build()
                ))
                .build();

            when(analyticsService.generateUtilizationAnalytics(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .thenReturn(payload);

            analyticsMockMvc.perform(
                    get("/api/v1/analytics/utilization")
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-30")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.heatmap[0].facilityName").value("Main Hall"))
                .andExpect(jsonPath("$.underutilizedFacilities[0].consecutiveUnderutilizedDays").value(8))
                .andExpect(jsonPath("$.recommendations[0].alternativeFacilityName").value("Hall B"));
        }

        @Test
        @DisplayName("returns 400 when from date is after to date")
        void getUtilizationAnalytics_returns400ForInvalidRange() throws Exception {
            analyticsMockMvc.perform(
                    get("/api/v1/analytics/utilization")
                        .param("from", "2026-05-01")
                        .param("to", "2026-04-01")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications")
    class NotificationEndpointContract {

        @Test
        @DisplayName("returns paginated notification feed payload")
        void getNotifications_returnsPageContract() throws Exception {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                .id(userId)
                .googleSubject("google-sub")
                .email("admin@smartcampus.local")
                .displayName("Admin")
                .roles(Set.of(Role.ADMIN))
                .build();

            Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .recipientUser(user)
                .eventType("SLA_DEADLINE_BREACHED")
                .severity(EventSeverity.HIGH)
                .channels(Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .title("SLA alert")
                .message("Ticket SLA breached")
                .eventId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .deliveredAt(LocalDateTime.now())
                .build();

            when(userRepository.findByEmail("admin@smartcampus.local")).thenReturn(Optional.of(user));
            when(notificationRepository.findByRecipientUser_IdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 20), 1));

            Authentication authentication =
                new UsernamePasswordAuthenticationToken("admin@smartcampus.local", "N/A");

            notificationMockMvc.perform(
                    get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "20")
                        .principal(authentication)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].eventType").value("SLA_DEADLINE_BREACHED"))
                .andExpect(jsonPath("$.content[0].severity").value("HIGH"))
                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("returns unread count payload")
        void getUnreadCount_returnsCountContract() throws Exception {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                .id(userId)
                .googleSubject("google-sub")
                .email("admin@smartcampus.local")
                .displayName("Admin")
                .roles(Set.of(Role.ADMIN))
                .build();

            when(userRepository.findByEmail("admin@smartcampus.local")).thenReturn(Optional.of(user));
            when(notificationRepository.countByRecipientUser_IdAndReadAtIsNull(userId)).thenReturn(3L);

                Authentication authentication =
                new UsernamePasswordAuthenticationToken("admin@smartcampus.local", "N/A");

            notificationMockMvc.perform(
                    get("/api/v1/notifications/unread/count")
                    .principal(authentication)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.unreadCount").value(3));
        }
    }
}
