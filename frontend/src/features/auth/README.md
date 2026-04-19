# Frontend Auth Implementation (T034)

## Overview

This directory contains the frontend authentication implementation for the VenueLink Operations Hub, fulfilling User Story 1 (Secure Access and Role Governance).

**Status**: ✅ Complete  
**Requirements Met**: FR-001 (OAuth), FR-002 (Role Assignment), FR-003 (Suspension Enforcement)

## Architecture

### Authentication Flow

```
1. User visits app
   ↓
2. AuthProvider checks stored token
   ↓
3. If valid: Load user profile, redirect to dashboard
   If expired: Attempt refresh
   If missing: Redirect to login
   ↓
4. User clicks "Sign in with Google"
   ↓
5. OAuth callback handler receives code
   ↓
6. Exchange code for JWT via backend
   ↓
7. Store tokens and user in localStorage
   ↓
8. Redirect to dashboard
```

### State Management

- **AuthContext** (`../contexts/AuthContext.jsx`): Central state management for auth
- **useAuth Hook**: Easy access to auth state and methods from any component
- **LocalStorage**: Persistence of JWT tokens and user profile

### Route Protection

- **ProtectedRoute** (`./ProtectedRoute.jsx`): Higher-order component for guarding routes
- Role-based access control (RBAC)
- Suspension enforcement - blocked from all protected operations
- Whitelisted endpoints for suspended users (profile, logout, appeals)

## File Structure

```
frontend/src/
├── contexts/
│   └── AuthContext.jsx          # Auth state & provider
├── features/
│   └── auth/
│       ├── LoginPage.jsx        # Google OAuth login UI
│       ├── OAuthCallback.jsx    # OAuth callback handler
│       └── index.js             # Feature exports
├── routes/
│   ├── ProtectedRoute.jsx       # Route guard component
│   └── index.js                 # Route exports
├── services/
│   ├── authService.js           # Enhanced auth logic
│   └── apiClient.js             # API client with interceptors
└── App.jsx                      # Main app with routing
```

## Components

### AuthContext & AuthProvider

Provides global auth state and methods:

```javascript
import { useAuth } from "../contexts/AuthContext";

function MyComponent() {
  const {
    isAuthenticated,
    user,
    loading,
    login,
    logout,
    hasRole,
    isSuspended,
  } = useAuth();

  return <>{isAuthenticated && <span>Hello {user.name}</span>}</>;
}
```

### ProtectedRoute

Guards routes with auth checks:

```javascript
<Route
  path="/dashboard"
  element={
    <ProtectedRoute requiredRoles={["ADMIN"]}>
      <AdminDashboard />
    </ProtectedRoute>
  }
/>
```

### LoginPage

OAuth login interface - automatically initiates Google OAuth flow.

### OAuthCallback

Handles OAuth redirect callback - exchanges authorization code for JWT tokens.

## authService API

### Token Management

```javascript
authService.getAccessToken(); // Get stored JWT
authService.setAuthTokens(token, refresh, user); // Store tokens
authService.refreshAccessToken(); // Refresh expired token
authService.isTokenExpired(); // Check expiry
authService.validateToken(token); // Validate on backend
```

### User Management

```javascript
authService.getCurrentUser(); // Get stored user profile
authService.fetchUserProfile(); // Fetch fresh profile from backend
authService.hasRole(roles); // Check if user has role(s)
authService.isSuspended(); // Check suspension status
```

### OAuth

```javascript
authService.exchangeGoogleToken(code); // Exchange OAuth code for JWT
authService.handleOAuthCallback(); // Parse callback URL and exchange
```

### Logout

```javascript
authService.logout(); // Clear tokens and user data
```

## Environment Variables

Create `.env` file in `frontend/` directory:

```env
# API Configuration
VITE_API_URL=http://localhost:8080/api

# Google OAuth
VITE_GOOGLE_CLIENT_ID=your-google-client-id

# Optional
VITE_APP_NAME=VenueLink Hub
VITE_APP_DEBUG=false
```

## Backend Integration

### Required Endpoints

All endpoints follow `/api/v1/auth/` path prefix:

1. **POST /oauth/google/callback**
   - Input: `{ code: string, redirectUri: string }`
   - Output: `{ token: string, refreshToken: string, user: object }`
   - Method: OAuth code exchange via backend (FR-001)

2. **POST /refresh**
   - Input: Refresh token in Authorization header
   - Output: `{ accessToken: string, refreshToken: string }`
   - Method: Token refresh on expiry

3. **GET /profile**
   - Input: JWT in Authorization header
   - Output: User profile with roles and suspension status
   - Method: Fetch current user (FR-002, FR-003)

4. **POST /logout**
   - Input: JWT in Authorization header
   - Output: Success response
   - Method: Notify backend of logout

### Response Format

```javascript
{
  token: "eyJhbGciOiJIUzI1NiIs...",
  refreshToken: "eyJhbGciOiJIUzI1NiIs...",
  user: {
    id: "uuid",
    email: "user@example.com",
    displayName: "User Name",
    picture: "https://...",
    roles: ["USER"],
    suspended: false
  }
}
```

## Error Handling

### Token Refresh on 401

When a request returns 401 (Unauthorized):

1. Automatically attempts token refresh
2. Retries original request with new token
3. If refresh fails, redirects to login

### Suspension Handling

Suspended users see access denied page for protected routes, except:

- Profile view
- Logout
- Appeal submission

### OAuth Errors

Captured from Google OAuth redirect:

- `error` parameter logged
- User redirected to login after 3 seconds
- Clear error message displayed

## Testing

### Manual Testing

1. **OAuth Flow**

   ```
   1. Navigate to /login
   2. Click "Sign in with Google"
   3. Complete Google authentication
   4. Should redirect to /dashboard
   5. User profile should display
   ```

2. **Token Refresh**

   ```
   1. Log in successfully
   2. Wait for token to expire (or mock expiration)
   3. Make API request
   4. Should auto-refresh and succeed
   ```

3. **Protected Routes**

   ```
   1. Log out
   2. Try to access /dashboard
   3. Should redirect to /login
   ```

4. **Suspension Enforcement**

   ```
   1. Mock suspended user in backend
   2. Log in with suspended account
   3. Try to access protected route
   4. Should show suspension message
   5. Should allow profile/appeal endpoints
   ```

5. **Role-Based Access**
   ```
   1. Admin user: access to admin routes
   2. Regular user: denied from admin routes
   3. Different roles: appropriate access levels
   ```

## Security Considerations

### JWT Handling

- Tokens stored in localStorage (XSS vulnerable - consider upgrading to httpOnly cookies)
- No token verification on client (performed by backend)
- Tokens not logged or exposed in console

### CORS

- Frontend configured for CORS with backend API
- Credentials included in requests when needed

### OAuth Security

- Redirect URI validated against origin
- Code exchange happens on backend (code never exposed client-side)
- PKCE not implemented (can be added in future)

### Token Refresh

- Refresh tokens used for token renewal
- Automatic retry on 401 prevents excessive redirects
- Refresh token expiration logs user out

## Future Enhancements

1. **httpOnly Cookies**: Move JWT from localStorage to httpOnly cookies for XSS protection
2. **PKCE**: Add Proof Key for Code Exchange for additional OAuth security
3. **Token Rotation**: Implement automatic token rotation on refresh
4. **Session Management**: Add session timeout warnings
5. **Multi-tab Sync**: Synchronize auth state across browser tabs
6. **Biometric Auth**: Support fingerprint/face recognition fallback

## Troubleshooting

### Issue: "No authorization code received"

- Ensure Google OAuth redirect URI matches configured value
- Check browser console for redirect URL
- Verify VITE_GOOGLE_CLIENT_ID is set correctly

### Issue: Token refresh fails with 401

- Check backend refresh token endpoint is working
- Verify refresh token is not expired
- Check server logs for token validation errors

### Issue: Protected route shows blank page

- Ensure AuthProvider wraps application
- Check browser console for context errors
- Verify loading state is handled

### Issue: User stays logged in after logout in another tab

- Implement cross-tab communication using BroadcastChannel API
- Clear shared storage on logout

## Contributing

When modifying auth code:

1. Keep context logic separate from component logic
2. Use hooks (useAuth) instead of direct context consumption
3. Add error boundaries for auth failures
4. Test token refresh on expired tokens
5. Ensure suspension state is respected
6. Document new environment variables in .env.example

---

**Last Updated**: 2026-04-09  
**Implementation**: Complete (T034)  
**Status**: Ready for Integration Testing with Backend (T023-T026)
