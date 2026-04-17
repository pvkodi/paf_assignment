package com.sliitreserve.api.controllers.users;

import com.sliitreserve.api.dto.auth.UserProfileResponse;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Search for users by email or display name.
     * Allows ADMIN and FACILITY_MANAGER users to find users when booking facilities for others.
     * 
     * @param query Search term (searches in email and display name)
     * @return List of matching users
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACILITY_MANAGER')")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam String query,
            Authentication authentication) {

        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Validate query length
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        if (query.length() > 100) {
            throw new IllegalArgumentException("Search query too long. Maximum 100 characters.");
        }

        // Search for users
        List<User> users = userRepository.searchByEmailOrDisplayName(query.trim(), query.trim());

        // Map to response DTO, excluding sensitive information
        List<UserSearchResponse> response = users.stream()
                .map(u -> new UserSearchResponse(
                        u.getId(),
                        u.getEmail(),
                        (u.getDisplayName() != null && !u.getDisplayName().isBlank()) ? u.getDisplayName() : u.getEmail(),
                        u.getRoles() != null ? new java.util.ArrayList<>(u.getRoles()) : List.of()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Get all users (for backward compatibility).
     * Allows ADMIN and FACILITY_MANAGER users to get all users.
     * 
     * @return List of all users
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACILITY_MANAGER')")
    public ResponseEntity<List<UserSearchResponse>> getAllUsers(Authentication authentication) {

        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        List<User> users = userRepository.findAll();

        // Map to response DTO
        List<UserSearchResponse> response = users.stream()
                .map(u -> new UserSearchResponse(
                        u.getId(),
                        u.getEmail(),
                        (u.getDisplayName() != null && !u.getDisplayName().isBlank()) ? u.getDisplayName() : u.getEmail(),
                        u.getRoles() != null ? new java.util.ArrayList<>(u.getRoles()) : List.of()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Simple DTO for user search responses.
     * Includes only necessary information for user selection in booking forms.
     */
    public static class UserSearchResponse {
        @JsonProperty("id")
        public java.util.UUID id;
        
        @JsonProperty("email")
        public String email;
        
        @JsonProperty("displayName")
        public String displayName;
        
        @JsonProperty("roles")
        public java.util.List<Role> roles;

        public UserSearchResponse(java.util.UUID id, String email, String displayName, java.util.List<Role> roles) {
            this.id = id;
            this.email = email;
            this.displayName = displayName;
            this.roles = roles;
        }

        // Getters for JSON serialization
        public java.util.UUID getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public String getDisplayName() {
            return displayName;
        }

        public java.util.List<Role> getRoles() {
            return roles;
        }
    }
}
