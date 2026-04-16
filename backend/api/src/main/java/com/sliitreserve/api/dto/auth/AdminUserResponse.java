package com.sliitreserve.api.dto.auth;

import com.sliitreserve.api.dto.BaseResponseDTO;
import com.sliitreserve.api.entities.auth.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse extends BaseResponseDTO {

    private static final long serialVersionUID = 1L;

    private String email;
    private String displayName;
    private Set<Role> roles;
    private boolean active;
    private LocalDateTime suspendedUntil;
    private Integer noShowCount;

    public AdminUserResponse(UUID id,
                             String email,
                             String displayName,
                             Set<Role> roles,
                             boolean active,
                             LocalDateTime suspendedUntil,
                             Integer noShowCount,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
        super(id, createdAt, updatedAt);
        this.email = email;
        this.displayName = displayName;
        this.roles = roles;
        this.active = active;
        this.suspendedUntil = suspendedUntil;
        this.noShowCount = noShowCount;
    }
}
