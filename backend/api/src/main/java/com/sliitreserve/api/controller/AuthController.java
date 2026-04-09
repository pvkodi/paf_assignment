package com.sliitreserve.api.controller;

import com.sliitreserve.api.dto.auth.AuthResponse;
import com.sliitreserve.api.dto.GoogleTokenRequest;
import com.sliitreserve.api.service.GoogleAuthService;
import com.sliitreserve.api.util.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:8080"})
public class AuthController {

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/google")
    public ResponseEntity<?> authenticateGoogle(@RequestBody GoogleTokenRequest request) {
        try {
            // Verify the Google token
            GoogleIdToken.Payload payload = googleAuthService.verifyToken(request.getToken());

            // Extract user information from payload
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            // Generate JWT tokens
            String accessToken = jwtUtil.generateToken(email, name, picture);
            String refreshToken = jwtUtil.generateRefreshToken(email);

            // Return authentication response
            AuthResponse authResponse = new AuthResponse(
                    accessToken,
                    refreshToken,
                    email,
                    name,
                    picture
            );

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new ErrorResponse("Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid authorization header"));
            }

            String refreshToken = authHeader.substring(7);

            // Validate refresh token
            if (!jwtUtil.validateToken(refreshToken)) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid or expired refresh token"));
            }

            String email = jwtUtil.getEmailFromToken(refreshToken);
            String name = jwtUtil.getClaimFromToken(refreshToken, "name");
            String picture = jwtUtil.getClaimFromToken(refreshToken, "picture");

            // Generate new access token
            String newAccessToken = jwtUtil.generateToken(email, name, picture);

            return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, refreshToken));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Token refresh failed: " + e.getMessage()));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid authorization header"));
            }

            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);
                return ResponseEntity.ok(new TokenValidationResponse(true, email));
            } else {
                return ResponseEntity.ok(new TokenValidationResponse(false, null));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Token validation failed: " + e.getMessage()));
        }
    }

    // Inner classes for responses
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }

    public static class TokenRefreshResponse {
        public String accessToken;
        public String refreshToken;

        public TokenRefreshResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }

    public static class TokenValidationResponse {
        public boolean valid;
        public String email;

        public TokenValidationResponse(boolean valid, String email) {
            this.valid = valid;
            this.email = email;
        }

        public boolean isValid() {
            return valid;
        }

        public String getEmail() {
            return email;
        }
    }
}
