package com.sliitreserve.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliitreserve.api.dto.BaseResponseDTO;
import com.sliitreserve.api.entities.auth.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

/**
 * DTO for authenticated user profile response.
 * 
 * <p>Contains user identity, roles, and suspension status.
 * Used in authentication and profile endpoints to communicate
 * user state to the frontend.
 * 
 * @see com.sliitreserve.api.entities.auth.User
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse extends BaseResponseDTO {

    private static final long serialVersionUID = 1L;

    /**
     * Unique user identifier (UUID).
     * Inherited from BaseResponseDTO.
     */
    // id is inherited from BaseResponseDTO

    /**
     * User's institutional email address.
     */
    @JsonProperty("email")
    private String email;

    /**
     * User's display name (from OAuth provider).
     */
    @JsonProperty("displayName")
    private String displayName;

    /**
     * Picture URL (from OAuth provider).
     */
    @JsonProperty("picture")
    private String picture;

    /**
     * User's assigned roles (can have multiple).
     * Policy resolution treats multi-role users with the most permissive policy (FR-042).
     */
    @JsonProperty("roles")
    private Set<Role> roles;

    /**
     * Suspension status.
     * When suspended, user can only access auth/session and profile endpoints,
     * and appeal submission (FR-031).
     */
    @JsonProperty("suspended")
    private boolean suspended;

    /**
     * Convenience constructor for creating ProfileResponse with essential fields.
     * 
     * @param id user UUID
     * @param email institutional email
     * @param displayName user's display name
     * @param roles assigned roles
     * @param suspended suspension status
     */
    public UserProfileResponse(UUID id, String email, String displayName, Set<Role> roles, boolean suspended) {
        super(id, null, null); // timestamps not applicable for session profile
        this.email = email;
        this.displayName = displayName;
        this.roles = roles;
        this.suspended = suspended;
    }
}
