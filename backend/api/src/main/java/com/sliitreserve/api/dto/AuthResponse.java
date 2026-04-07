package com.sliitreserve.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String email;
    private String name;
    private String picture;
    private String tokenType;

    public AuthResponse(String accessToken, String refreshToken, String email, String name, String picture) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.tokenType = "Bearer";
    }
}
