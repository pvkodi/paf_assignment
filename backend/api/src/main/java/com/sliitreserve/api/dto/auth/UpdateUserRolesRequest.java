package com.sliitreserve.api.dto.auth;

import com.sliitreserve.api.entities.auth.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRolesRequest {
    private Set<Role> roles;
}
