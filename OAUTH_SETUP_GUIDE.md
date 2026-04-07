# Google OAuth Implementation Guide

## Overview

This project implements Google OAuth 2.0 authentication with JWT token management across both frontend (React) and backend (Spring Boot).

## Prerequisites

- Google Cloud Project with OAuth 2.0 credentials
- Client ID: `954142944464-qroe8pd2cpv00lt158p4o7dj2a1sc6vd.apps.googleusercontent.com`
- Java 21+
- Node.js 18+
- PostgreSQL 12+

## Backend Setup

### 1. Add Google Client Secret to application.yaml

Edit `backend/api/src/main/resources/application.yaml`:

```yaml
app:
  auth:
    google:
      client-id: 954142944464-qroe8pd2cpv00lt158p4o7dj2a1sc6vd.apps.googleusercontent.com
      client-secret: YOUR_CLIENT_SECRET_HERE # Replace with actual secret
```

### 2. Update JWT Secret

For production, change the JWT secret in `application.yaml`:

```yaml
app:
  auth:
    jwt:
      secret: your-production-grade-secret-key-at-least-32-characters-long
```

### 3. Build and Run Backend

```bash
cd backend/api
./mvnw clean install
./mvnw spring-boot:run
```

Backend will run on `http://localhost:8080`

## Frontend Setup

### 1. Install Dependencies

```bash
cd frontend
npm install
```

### 2. Update Google Client ID (if different)

Edit `frontend/src/pages/Login.jsx`:

```javascript
const GOOGLE_CLIENT_ID =
  "954142944464-qroe8pd2cpv00lt158p4o7dj2a1sc6vd.apps.googleusercontent.com";
```

### 3. Run Development Server

```bash
npm run dev
```

Frontend will run on `http://localhost:5173`

## API Endpoints

### 1. Google OAuth Login

**POST** `/api/auth/google`

Request:

```json
{
  "token": "google-id-token-from-client"
}
```

Response:

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "email": "user@example.com",
  "name": "User Name",
  "picture": "https://...",
  "tokenType": "Bearer"
}
```

### 2. Refresh Token

**POST** `/api/auth/refresh`

Headers:

```
Authorization: Bearer <refresh-token>
```

Response:

```json
{
  "accessToken": "new-jwt-access-token",
  "refreshToken": "refresh-token"
}
```

### 3. Validate Token

**GET** `/api/auth/validate`

Headers:

```
Authorization: Bearer <access-token>
```

Response:

```json
{
  "valid": true,
  "email": "user@example.com"
}
```

## Authentication Flow

1. **User clicks "Sign in with Google"** on the login page
2. **Google OAuth consent screen** appears
3. **Browser receives ID token** from Google
4. **Frontend sends token to backend** via `/api/auth/google` endpoint
5. **Backend verifies token** with Google's public keys
6. **Backend generates JWT tokens** (access + refresh)
7. **Frontend stores tokens** in localStorage
8. **Frontend redirects to dashboard** with authenticated session
9. **Automatic token refresh** happens transparently when access token expires

## Token Management

### Access Token

- **Lifetime**: 24 hours (configurable)
- **Storage**: localStorage
- **Usage**: Sent in Authorization header for protected requests
- **Format**: `Bearer <token>`

### Refresh Token

- **Lifetime**: 7 days (configurable)
- **Storage**: localStorage
- **Usage**: Only used to get new access tokens
- **Format**: `Bearer <token>`

### Token Refresh Mechanism

- Axios interceptor automatically detects 401 responses
- Attempts to refresh token using the refresh endpoint
- Retries original request with new access token
- If refresh fails, logs out user and redirects to login

## Protecting Endpoints

To protect an endpoint from unauthorized access, create a filter or interceptor that:

1. Extracts JWT token from Authorization header
2. Validates token using `JwtUtil.validateToken()`
3. Extracts user info using `JwtUtil.getEmailFromToken()` etc.
4. Proceeds or denies based on validation result

Example in a controller:

```java
@GetMapping("/protected")
public ResponseEntity<?> protectedEndpoint(
        @RequestHeader("Authorization") String authHeader) {

    String token = authHeader.substring(7); // Remove "Bearer "

    if (!jwtUtil.validateToken(token)) {
        return ResponseEntity.status(401).build();
    }

    String email = jwtUtil.getEmailFromToken(token);
    // Use email to fetch user data or perform authorization checks

    return ResponseEntity.ok("Protected content");
}
```

## Security Considerations

### Required for Production:

1. ✅ Enable HTTPS for all endpoints
2. ✅ Use environment variables for secrets (not hardcoded)
3. ✅ Increase JWT secret length to 64+ characters
4. ✅ Set `httpOnly` flag on tokens if using cookies instead of localStorage
5. ✅ Implement CSRF protection for state-changing operations
6. ✅ Add rate limiting to auth endpoints
7. ✅ Store refresh tokens in secure, httpOnly cookies

### Environment Variables (add to .env or deployment config):

```
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000
REFRESH_TOKEN_EXPIRATION=604800000
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://app.yourdomain.com
```

## Troubleshooting

### "Invalid ID token" error

- Verify Client ID matches in frontend and backend
- Check that token is being sent immediately after generation
- Ensure Google Cloud OAuth consent screen is configured

### CORS errors

- Check `cors.allowed-origins` in application.yaml
- Ensure frontend URL matches allowed origins exactly
- Add `/api/auth/**` to CORS allowed paths

### Token validation fails

- Verify JWT secret matches between token generation and validation
- Check token hasn't expired
- Ensure Authorization header format is `Bearer <token>`

### User info not loading on dashboard

- Check localStorage has `user` key after login
- Verify accessToken is being stored
- Check browser console for errors

## Database User Management (Optional)

When ready to persist user data, create a User entity:

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    private String email;
    private String name;
    private String picture;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
```

Then modify the auth controller to save/update users in the database.

## Next Steps

1. ✅ Test the login flow in development
2. ✅ Verify tokens are being generated correctly
3. Test protected endpoints with token validation
4. Implement role-based access control (RBAC) if needed
5. Add user data persistence to database
6. Deploy with proper environment variables
7. Configure production OAuth credentials

## Support & References

- [Google OAuth 2.0 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [Spring Security OAuth2](https://spring.io/projects/spring-security-oauth)
- [React OAuth Google](https://www.npmjs.com/package/@react-oauth/google)
