package com.sliitreserve.api.controllers.appeals;

import com.sliitreserve.api.dto.auth.AppealDecisionRequest;
import com.sliitreserve.api.dto.auth.AppealRequest;
import com.sliitreserve.api.dto.auth.AppealResponse;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.exception.ForbiddenException;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.services.auth.AppealService;
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

@RestController
@RequestMapping("/api/v1/appeals")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:8080"})
public class AppealController {

  private final AppealService appealService;
  private final UserRepository userRepository;

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<AppealResponse> submitAppeal(
      @Valid @RequestBody AppealRequest request,
      Authentication authentication) {

    log.info("Appeal submission request received");

    String userEmail = (String) authentication.getPrincipal();
    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

    AppealResponse response = appealService.submitAppeal(user.getId(), request);

    log.info("Appeal submitted successfully by user: {}", userEmail);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<AppealResponse>> listAppeals(Authentication authentication) {

    log.debug("Appeal list request received");

    String userEmail = (String) authentication.getPrincipal();
    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

    List<AppealResponse> appeals;

    if (user.hasRole(Role.ADMIN)) {
      appeals = appealService.getPendingAppeals();
      log.debug("Admin {} retrieved {} pending appeals", userEmail, appeals.size());
    } else {
      appeals = appealService.getUserAppeals(user.getId());
      log.debug("User {} retrieved their {} appeals", userEmail, appeals.size());
    }

    return ResponseEntity.ok(appeals);
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<AppealResponse> getAppealDetails(
      @PathVariable("id") UUID appealId,
      Authentication authentication) {

    log.debug("Appeal details request for id: {}", appealId);

    String userEmail = (String) authentication.getPrincipal();
    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

    AppealResponse appeal = appealService.getAppealDetails(appealId);

    if (!appeal.getUserId().equals(user.getId()) && !user.hasRole(Role.ADMIN)) {
      log.warn("User {} attempted unauthorized access to appeal {}", userEmail, appealId);
      throw new ForbiddenException("You do not have permission to view this appeal");
    }

    log.debug("Appeal details retrieved for id: {}", appealId);
    return ResponseEntity.ok(appeal);
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AppealResponse> approveAppeal(
      @PathVariable("id") UUID appealId,
      @Valid @RequestBody AppealDecisionRequest request,
      Authentication authentication) {

    log.info("Appeal approval request for id: {}", appealId);

    String adminEmail = (String) authentication.getPrincipal();
    User admin = userRepository.findByEmail(adminEmail)
        .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with email: " + adminEmail));

    AppealResponse response = appealService.approveAppeal(appealId, admin.getId(), request.getDecision());

    log.info("Appeal approved by admin {}: {}", adminEmail, appealId);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AppealResponse> rejectAppeal(
      @PathVariable("id") UUID appealId,
      @Valid @RequestBody AppealDecisionRequest request,
      Authentication authentication) {

    log.info("Appeal rejection request for id: {}", appealId);

    String adminEmail = (String) authentication.getPrincipal();
    User admin = userRepository.findByEmail(adminEmail)
        .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with email: " + adminEmail));

    AppealResponse response = appealService.rejectAppeal(appealId, admin.getId(), request.getDecision());

    log.info("Appeal rejected by admin {}: {}", adminEmail, appealId);
    return ResponseEntity.ok(response);
  }
}
