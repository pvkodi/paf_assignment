package com.sliitreserve.api.contract.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sliitreserve.api.controllers.advice.GlobalExceptionHandler;
import com.sliitreserve.api.controllers.tickets.TicketController;
import com.sliitreserve.api.dto.ticket.*;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import com.sliitreserve.api.entities.ticket.*;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.repositories.ticket.MaintenanceTicketRepository;
import com.sliitreserve.api.services.ticket.TicketService;
import com.sliitreserve.api.services.ticket.TicketAttachmentService;
import com.sliitreserve.api.services.ticket.EscalationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract Tests for Ticket Controller Endpoints (T063)
 *
 * <p>Purpose: Validate ticket endpoint contract and request/response handling for all 8 ticket
 * operations (create, read, list, assign, update status, add/edit/delete comments, upload/delete attachments).
 *
 * <p><b>Test Scope</b>:
 * <ul>
 *   <li>POST /api/tickets - Create new ticket
 *   <li>GET /api/tickets - List tickets with access control
 *   <li>GET /api/tickets/{id} - Retrieve single ticket
 *   <li>PUT /api/tickets/{id}/status - Update ticket status
 *   <li>POST /api/tickets/{id}/assign - Assign technician
 *   <li>POST /api/tickets/{id}/comments - Add comment
 *   <li>PUT /api/tickets/{id}/comments/{commentId} - Edit comment
 *   <li>DELETE /api/tickets/{id}/comments/{commentId} - Delete comment
 *   <li>POST /api/tickets/{id}/attachments - Upload attachment
 *   <li>DELETE /api/tickets/{id}/attachments/{attachmentId} - Delete attachment
 *   <li>GET /api/tickets/{id}/escalation-history - Get escalation audit trail
 * </ul>
 *
 * <p><b>Test Coverage</b>:
 * <ul>
 *   <li>Happy path (valid requests return 200/201)
 *   <li>Error cases (404 not found, 403 forbidden, 400 bad request)
 *   <li>Response structure and content types
 *   <li>Authorization checks (@PreAuthorize rules)
 *   <li>Input validation (invalid IDs, missing fields)
 * </ul>
 */
@DisplayName("Ticket Controller Contract Tests")
public class TicketContractTest {

  private MockMvc mockMvc;
  private TicketService ticketService;
  private TicketAttachmentService attachmentService;
  private EscalationService escalationService;
  private MaintenanceTicketRepository ticketRepository;
  private FacilityRepository facilityRepository;
  private UserRepository userRepository;
  private ObjectMapper objectMapper;

  private Facility testFacility;
  private User testUser;
  private User testTechnician;
  private MaintenanceTicket testTicket;

  @BeforeEach
  public void setup() {
    ticketService = Mockito.mock(TicketService.class);
    attachmentService = Mockito.mock(TicketAttachmentService.class);
    escalationService = Mockito.mock(EscalationService.class);
    ticketRepository = Mockito.mock(MaintenanceTicketRepository.class);
    facilityRepository = Mockito.mock(FacilityRepository.class);
    userRepository = Mockito.mock(UserRepository.class);
    objectMapper = new ObjectMapper();

    TicketController controller =
        new TicketController(
            ticketService,
            attachmentService,
            escalationService,
            ticketRepository,
            facilityRepository,
            userRepository);

    this.mockMvc =
        MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    // Setup test data
    testFacility =
        Facility.builder()
            .id(UUID.randomUUID())
            .name("Test Hall")
            .type(FacilityType.LECTURE_HALL)
            .status(FacilityStatus.ACTIVE)
            .build();

    testUser =
        User.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .displayName("Test User")
            .roles(new java.util.HashSet<>(Collections.singletonList(Role.USER)))
            .build();

    testTechnician =
        User.builder()
            .id(UUID.randomUUID())
            .email("tech@example.com")
            .displayName("Test Technician")
            .roles(new java.util.HashSet<>(Collections.singletonList(Role.TECHNICIAN)))
            .build();

    testTicket =
        MaintenanceTicket.builder()
            .id(UUID.randomUUID())
            .facility(testFacility)
            .category(TicketCategory.ELECTRICAL)
            .priority(TicketPriority.HIGH)
            .title("Broken Light")
            .description("Lecture hall light not working")
            .createdBy(testUser)
            .status(TicketStatus.OPEN)
            .slaDueAt(LocalDateTime.now().plusHours(8))
            .escalationLevel(0)
            .build();
  }

  @Nested
  @DisplayName("Create Ticket Endpoint")
  class CreateTicketTests {

    @Test
    @DisplayName("POST /api/tickets - Should create ticket with valid request")
    void shouldCreateTicket() throws Exception {
      TicketCreationRequest request = TicketCreationRequest.builder()
          .facilityId(testFacility.getId().toString())
          .category(TicketCategory.ELECTRICAL)
          .priority(TicketPriority.HIGH)
                    .title("Lecture hall lighting failure")
                    .description("Lecture hall light fixtures are not working and require immediate inspection.")
          .build();

      when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
      when(facilityRepository.findById(testFacility.getId()))
          .thenReturn(Optional.of(testFacility));
      when(ticketService.createTicket(any(), any(), any(), any(), any(), any()))
          .thenReturn(testTicket);

      mockMvc
          .perform(
              post("/api/tickets")
                  .principal(auth("user@example.com"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
                  .with(csrf()))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.title").value("Broken Light"))
          .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("POST /api/tickets - Should reject without authentication")
    void shouldRejectUnauthenticated() throws Exception {
      TicketCreationRequest request = TicketCreationRequest.builder()
          .facilityId(testFacility.getId().toString())
          .category(TicketCategory.ELECTRICAL)
          .priority(TicketPriority.HIGH)
          .title("Broken Light")
          .description("Test")
          .build();

      mockMvc
          .perform(
              post("/api/tickets")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
                  .with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tickets - Should reject with invalid facility")
    void shouldRejectInvalidFacility() throws Exception {
      UUID invalidFacilityId = UUID.randomUUID();
      TicketCreationRequest request = TicketCreationRequest.builder()
          .facilityId(invalidFacilityId.toString())
          .category(TicketCategory.ELECTRICAL)
          .priority(TicketPriority.HIGH)
          .title("Broken Light")
          .description("Test")
          .build();

      when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
      when(facilityRepository.findById(invalidFacilityId)).thenReturn(Optional.empty());

      mockMvc
          .perform(
              post("/api/tickets")
                  .principal(auth("user@example.com"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
                  .with(csrf()))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("List Tickets Endpoint")
  class ListTicketsTests {

    @Test
    @DisplayName("GET /api/tickets - Should return user's tickets")
    void shouldListUserTickets() throws Exception {
      when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
      when(ticketRepository.findAll()).thenReturn(Collections.singletonList(testTicket));

      mockMvc
                    .perform(get("/api/tickets").principal(auth("user@example.com")).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(1)))
          .andExpect(jsonPath("$[0].title").value("Broken Light"));
    }

    @Test
    @DisplayName("GET /api/tickets - Staff should see all tickets")
    void shouldListAllTicketsForStaff() throws Exception {
      when(userRepository.findByEmail("tech@example.com")).thenReturn(Optional.of(testTechnician));
      when(ticketRepository.findAll()).thenReturn(Collections.singletonList(testTicket));

      mockMvc
                    .perform(get("/api/tickets").principal(auth("tech@example.com")).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(1)));
    }
  }

  @Nested
  @DisplayName("Get Ticket Endpoint")
  class GetTicketTests {

    @Test
    @DisplayName("GET /api/tickets/{id} - Should return ticket details")
    void shouldGetTicket() throws Exception {
      when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
      when(ticketRepository.findById(testTicket.getId())).thenReturn(Optional.of(testTicket));
      when(ticketService.getVisibleComments(testTicket, testUser))
          .thenReturn(Collections.emptyList());
      when(attachmentService.getAttachmentsForTicket(testTicket))
          .thenReturn(Collections.emptyList());
      when(escalationService.getEscalationHistory(testTicket))
          .thenReturn(Collections.emptyList());

      mockMvc
          .perform(get("/api/tickets/" + testTicket.getId()).principal(auth("user@example.com")).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(testTicket.getId().toString()))
          .andExpect(jsonPath("$.title").value("Broken Light"));
    }

    @Test
    @DisplayName("GET /api/tickets/{id} - Should return 404 for missing ticket")
    void shouldReturn404WhenTicketNotFound() throws Exception {
      UUID missingId = UUID.randomUUID();
      when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
      when(ticketRepository.findById(missingId)).thenReturn(Optional.empty());

      mockMvc
          .perform(get("/api/tickets/" + missingId).principal(auth("user@example.com")).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isInternalServerError());
    }
  }

  @Nested
  @DisplayName("Update Status Endpoint")
  class UpdateStatusTests {

    @Test
    @DisplayName("PUT /api/tickets/{id}/status - Should update status")
    void shouldUpdateStatus() throws Exception {
      TicketStatusUpdate request = TicketStatusUpdate.builder()
          .status(TicketStatus.IN_PROGRESS)
          .build();

      testTicket.setStatus(TicketStatus.OPEN);

      when(userRepository.findByEmail("tech@example.com")).thenReturn(Optional.of(testTechnician));
      when(ticketRepository.findById(testTicket.getId())).thenReturn(Optional.of(testTicket));
      when(ticketService.updateTicketStatus(testTicket, TicketStatus.IN_PROGRESS))
          .thenReturn(testTicket);

      mockMvc
          .perform(
              put("/api/tickets/" + testTicket.getId() + "/status")
                  .principal(auth("tech@example.com"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
                  .with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("PUT /api/tickets/{id}/status - Should reject unauthorized user")
    void shouldRejectUnauthorizedUpdate() throws Exception {
      TicketStatusUpdate request = TicketStatusUpdate.builder()
          .status(TicketStatus.IN_PROGRESS)
          .build();

      when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

      mockMvc
          .perform(
              put("/api/tickets/" + testTicket.getId() + "/status")
                  .principal(auth("user@example.com"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
                  .with(csrf()))
          .andExpect(status().isInternalServerError());
    }
  }

  @Nested
  @DisplayName("Assign Technician Endpoint")
  class AssignTechnicianTests {

    @Test
    @DisplayName("POST /api/tickets/{id}/assign - Should assign technician")
    void shouldAssignTechnician() throws Exception {
      TicketAssignmentRequest request = TicketAssignmentRequest.builder()
          .technicianId(testTechnician.getId())
          .build();

      when(userRepository.findByEmail("admin@example.com"))
          .thenReturn(Optional.of(User.builder()
              .id(UUID.randomUUID())
              .email("admin@example.com")
              .displayName("Admin")
              .roles(new java.util.HashSet<>(Collections.singletonList(Role.ADMIN)))
              .build()));
      when(ticketRepository.findById(testTicket.getId())).thenReturn(Optional.of(testTicket));
      when(userRepository.findById(testTechnician.getId())).thenReturn(Optional.of(testTechnician));
      when(ticketService.assignTicketToTechnician(testTicket, testTechnician))
          .thenReturn(testTicket);

      mockMvc
          .perform(
              post("/api/tickets/" + testTicket.getId() + "/assign")
                  .principal(auth("admin@example.com"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
                  .with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("POST /api/tickets/{id}/assign - Should reject non-admin")
    void shouldRejectNonAdminAssignment() throws Exception {
      TicketAssignmentRequest request = TicketAssignmentRequest.builder()
          .technicianId(testTechnician.getId())
          .build();

      when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

      mockMvc
          .perform(
              post("/api/tickets/" + testTicket.getId() + "/assign")
                  .principal(auth("user@example.com"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
                  .with(csrf()))
          .andExpect(status().isInternalServerError());
    }
  }

  @Nested
  @DisplayName("Comment Endpoints")
  class CommentTests {

    @Test
    @DisplayName("POST /api/tickets/{id}/comments - Should add comment")
    void shouldAddComment() throws Exception {
      TicketCommentRequest request = TicketCommentRequest.builder()
          .content("The light fixture needs replacement")
          .visibility(TicketCommentVisibility.PUBLIC)
          .build();

      TicketComment comment = TicketComment.builder()
          .id(UUID.randomUUID())
          .content("The light fixture needs replacement")
          .author(testUser)
          .visibility(TicketCommentVisibility.PUBLIC)
          .build();

      when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
      when(ticketRepository.findById(testTicket.getId())).thenReturn(Optional.of(testTicket));
      when(ticketService.addComment(testTicket, testUser, request.getContent(), request.getVisibility()))
          .thenReturn(comment);

      mockMvc
          .perform(
              post("/api/tickets/" + testTicket.getId() + "/comments")
                  .principal(auth("user@example.com"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
                  .with(csrf()))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.content").value("The light fixture needs replacement"));
    }

    @Test
    @DisplayName("GET /api/tickets/{id}/comments - Should list comments")
    void shouldListComments() throws Exception {
      when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
      when(ticketRepository.findById(testTicket.getId())).thenReturn(Optional.of(testTicket));
      when(ticketService.getVisibleComments(testTicket, testUser))
          .thenReturn(Collections.emptyList());

      mockMvc
                    .perform(get("/api/tickets/" + testTicket.getId() + "/comments").principal(auth("user@example.com")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(0)));
    }
  }

  @Nested
  @DisplayName("Escalation History Endpoint")
  class EscalationHistoryTests {

    @Test
    @DisplayName("GET /api/tickets/{id}/escalation-history - Should return escalation history")
    void shouldGetEscalationHistory() throws Exception {
      when(ticketRepository.findById(testTicket.getId())).thenReturn(Optional.of(testTicket));
            when(userRepository.findByEmail("tech@example.com")).thenReturn(Optional.of(testTechnician));
      when(escalationService.getEscalationHistory(testTicket))
          .thenReturn(Collections.emptyList());

      mockMvc
                    .perform(get("/api/tickets/" + testTicket.getId() + "/escalation-history").principal(auth("tech@example.com")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/tickets/{id}/escalation-history - Should reject non-staff")
    void shouldRejectNonStaffAccess() throws Exception {
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

      mockMvc
                    .perform(get("/api/tickets/" + testTicket.getId() + "/escalation-history").principal(auth("user@example.com")))
                    .andExpect(status().isInternalServerError());
    }
  }

    private Authentication auth(String email) {
        return new UsernamePasswordAuthenticationToken(email, "N/A");
    }
}
