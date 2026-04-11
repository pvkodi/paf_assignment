package com.sliitreserve.api.contract.workflow;

import com.sliitreserve.api.controllers.AppealController;
import com.sliitreserve.api.controllers.advice.GlobalExceptionHandler;
import com.sliitreserve.api.dto.auth.AppealResponse;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.SuspensionAppealStatus;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.repositories.UserRepository;
import com.sliitreserve.api.services.auth.AppealService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApprovalAndAppealContractTest {

    @Mock
    private AppealService appealService;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AppealController appealController = new AppealController(appealService, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(appealController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void submitAppeal_returns201WithExpectedContractFields() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "student@sliit.lk", Role.USER);
        when(userRepository.findByEmail("student@sliit.lk")).thenReturn(Optional.of(user));

        UUID appealId = UUID.randomUUID();
        AppealResponse response = appealResponse(
                appealId,
                userId,
                "student@sliit.lk",
                "Need access restored",
                SuspensionAppealStatus.SUBMITTED,
                null,
                null,
                null
        );
        when(appealService.submitAppeal(eq(userId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/appeals")
                        .principal(auth("student@sliit.lk"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Need access restored"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(appealId.toString()))
                .andExpect(jsonPath("$.user_id").value(userId.toString()))
                .andExpect(jsonPath("$.user_email").value("student@sliit.lk"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void submitAppeal_missingReason_returns400ValidationContract() throws Exception {
        mockMvc.perform(post("/api/v1/appeals")
                        .principal(auth("student@sliit.lk"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    void listAppeals_adminUser_receivesPendingQueue() throws Exception {
        UUID adminId = UUID.randomUUID();
        User admin = user(adminId, "admin@sliit.lk", Role.ADMIN);
        when(userRepository.findByEmail("admin@sliit.lk")).thenReturn(Optional.of(admin));

        UUID appealId = UUID.randomUUID();
        AppealResponse pending = appealResponse(
                appealId,
                UUID.randomUUID(),
                "student@sliit.lk",
                "Please review",
                SuspensionAppealStatus.SUBMITTED,
                null,
                null,
                null
        );

        when(appealService.getPendingAppeals()).thenReturn(List.of(pending));

        mockMvc.perform(get("/api/v1/appeals")
                        .principal(auth("admin@sliit.lk")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(appealId.toString()))
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"));

        verify(appealService).getPendingAppeals();
    }

    @Test
    void listAppeals_regularUser_receivesOwnAppeals() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "student@sliit.lk", Role.USER);
        when(userRepository.findByEmail("student@sliit.lk")).thenReturn(Optional.of(user));

        when(appealService.getUserAppeals(userId)).thenReturn(List.of(
                appealResponse(
                        UUID.randomUUID(),
                        userId,
                        "student@sliit.lk",
                        "History item",
                        SuspensionAppealStatus.REJECTED,
                        "admin@sliit.lk",
                        LocalDateTime.now().minusDays(1),
                        "Need better attendance"
                )
        ));

        mockMvc.perform(get("/api/v1/appeals")
                        .principal(auth("student@sliit.lk")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].user_id").value(userId.toString()))
                .andExpect(jsonPath("$[0].status").value("REJECTED"));

        verify(appealService).getUserAppeals(userId);
    }

    @Test
    void getAppealDetails_ownerAccess_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID appealId = UUID.randomUUID();
        User user = user(userId, "student@sliit.lk", Role.USER);
        when(userRepository.findByEmail("student@sliit.lk")).thenReturn(Optional.of(user));

        when(appealService.getAppealDetails(appealId)).thenReturn(
                appealResponse(
                        appealId,
                        userId,
                        "student@sliit.lk",
                        "Review my case",
                        SuspensionAppealStatus.SUBMITTED,
                        null,
                        null,
                        null
                )
        );

        mockMvc.perform(get("/api/v1/appeals/{id}", appealId)
                        .principal(auth("student@sliit.lk")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appealId.toString()))
                .andExpect(jsonPath("$.user_id").value(userId.toString()));
    }

    @Test
    void getAppealDetails_nonOwnerNonAdmin_returns403Forbidden() throws Exception {
        UUID callerId = UUID.randomUUID();
        UUID appealId = UUID.randomUUID();
        User caller = user(callerId, "student@sliit.lk", Role.USER);
        when(userRepository.findByEmail("student@sliit.lk")).thenReturn(Optional.of(caller));

        when(appealService.getAppealDetails(appealId)).thenReturn(
                appealResponse(
                        appealId,
                        UUID.randomUUID(),
                        "other@sliit.lk",
                        "Someone else appeal",
                        SuspensionAppealStatus.SUBMITTED,
                        null,
                        null,
                        null
                )
        );

        mockMvc.perform(get("/api/v1/appeals/{id}", appealId)
                        .principal(auth("student@sliit.lk")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(containsString("do not have permission")));
    }

    @Test
    void approveAppeal_returns200WithReviewedContract() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID appealId = UUID.randomUUID();

        when(userRepository.findByEmail("admin@sliit.lk"))
                .thenReturn(Optional.of(user(adminId, "admin@sliit.lk", Role.ADMIN)));

        AppealResponse approved = appealResponse(
                appealId,
                UUID.randomUUID(),
                "student@sliit.lk",
                "Appeal accepted",
                SuspensionAppealStatus.APPROVED,
                "admin@sliit.lk",
                LocalDateTime.now(),
                "Approved after review"
        );
        when(appealService.approveAppeal(eq(appealId), eq(adminId), eq("Approved after review")))
                .thenReturn(approved);

        mockMvc.perform(post("/api/v1/appeals/{id}/approve", appealId)
                        .principal(auth("admin@sliit.lk"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approved": true,
                                  "decision": "Approved after review"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appealId.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewed_by_user_email").value("admin@sliit.lk"));
    }

    @Test
    void rejectAppeal_returns200WithReviewedContract() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID appealId = UUID.randomUUID();

        when(userRepository.findByEmail("admin@sliit.lk"))
                .thenReturn(Optional.of(user(adminId, "admin@sliit.lk", Role.ADMIN)));

        AppealResponse rejected = appealResponse(
                appealId,
                UUID.randomUUID(),
                "student@sliit.lk",
                "Appeal rejected",
                SuspensionAppealStatus.REJECTED,
                "admin@sliit.lk",
                LocalDateTime.now(),
                "Insufficient grounds"
        );
        when(appealService.rejectAppeal(eq(appealId), eq(adminId), eq("Insufficient grounds")))
                .thenReturn(rejected);

        mockMvc.perform(post("/api/v1/appeals/{id}/reject", appealId)
                        .principal(auth("admin@sliit.lk"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approved": false,
                                  "decision": "Insufficient grounds"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appealId.toString()))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewed_by_user_email").value("admin@sliit.lk"));
    }

    @Test
    void approveAppeal_missingApprovedFlag_returns400ValidationContract() throws Exception {
        UUID appealId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/appeals/{id}/approve", appealId)
                        .principal(auth("admin@sliit.lk"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "No approval flag"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"));
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

    private AppealResponse appealResponse(
            UUID appealId,
            UUID userId,
            String userEmail,
            String reason,
            SuspensionAppealStatus status,
            String reviewedByUserEmail,
            LocalDateTime reviewedAt,
            String decision
    ) {
        AppealResponse response = new AppealResponse();
        response.setId(appealId);
        response.setUserId(userId);
        response.setUserEmail(userEmail);
        response.setReason(reason);
        response.setStatus(status);
        response.setReviewedByUserEmail(reviewedByUserEmail);
        response.setReviewedAt(reviewedAt);
        response.setDecision(decision);
        response.setCreatedAt(LocalDateTime.now().minusDays(2));
        response.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return response;
    }

    private Authentication auth(String email) {
        return new UsernamePasswordAuthenticationToken(email, "N/A");
    }
}
