# Google OAuth Implementation - Configuration Checklist

## Pre-Deployment Checklist

Before running the application, verify all configuration items:

### Frontend Configuration

- [ ] `frontend/src/pages/Login.jsx`
  - [ ] Google Client ID is correct: `954142944464-qroe8pd2cpv00lt158p4o7dj2a1sc6vd.apps.googleusercontent.com`
  - [ ] API_URL points to correct backend: `http://localhost:8080/api/auth`

- [ ] `frontend/package.json`
  - [ ] Contains `@react-oauth/google` dependency
  - [ ] All dependencies installed (`npm install` completed)

- [ ] `frontend/src/services/authService.js`
  - [ ] Axios interceptor configured for token refresh
  - [ ] localStorage keys used: `accessToken`, `refreshToken`, `user`

### Backend Configuration

- [ ] `backend/api/pom.xml`
  - [ ] Contains `com.google.api-client` dependency (v1.35.2)
  - [ ] Contains JWT dependencies (jjwt)
  - [ ] spring-boot-starter-security included
  - [ ] spring-boot-starter-oauth2-client included

- [ ] `backend/api/src/main/resources/application.yaml`
  - [ ] Database credentials correct
  - [ ] Google Client ID configured
  - [ ] **Google Client Secret configured** (REQUIRED!)
  - [ ] JWT secret set (change for production)
  - [ ] CORS allowed origins listed

- [ ] Backend Java files created
  - [ ] `util/JwtUtil.java` - JWT generation/validation
  - [ ] `service/GoogleAuthService.java` - Google token verification
  - [ ] `controller/AuthController.java` - Auth endpoints
  - [ ] `config/SecurityConfig.java` - CORS and security
  - [ ] `dto/GoogleTokenRequest.java` - Request DTO
  - [ ] `dto/AuthResponse.java` - Response DTO

### Environment & System

- [ ] Java 21+ installed
  - [ ] Verify: `java -version`

- [ ] Node.js 18+ installed
  - [ ] Verify: `node --version`

- [ ] PostgreSQL running
  - [ ] Database `smartcampus` exists
  - [ ] Username/password in application.yaml match

- [ ] Ports available
  - [ ] Port 8080 (backend) not in use
  - [ ] Port 5173 (frontend dev) not in use

- [ ] Google Cloud Project configured
  - [ ] OAuth 2.0 credential created
  - [ ] Authorized origins include:
    - [ ] `http://localhost:5173`
    - [ ] `http://localhost:8080`
    - [ ] `http://localhost:3000` (if used)
  - [ ] Authorized redirect URIs include Google endpoints

### Verification Steps

Once configured, verify:

1. **Backend builds successfully**

   ```bash
   cd backend/api
   ./mvnw.cmd clean install
   ```

   - [ ] No compilation errors
   - [ ] No missing dependencies

2. **Frontend builds successfully**

   ```bash
   cd frontend
   npm install
   ```

   - [ ] All packages installed
   - [ ] No security vulnerabilities that block startup

3. **Ports are accessible**

   ```bash
   # Terminal 1
   cd backend/api
   ./mvnw.cmd spring-boot:run

   # Terminal 2
   cd frontend
   npm run dev
   ```

   - [ ] Backend starts without errors
   - [ ] Frontend dev server starts
   - [ ] Can access http://localhost:5173 in browser

4. **Login page loads**
   - [ ] Open http://localhost:5173
   - [ ] See Google login button
   - [ ] No console errors (F12)

5. **OAuth workflow**
   - [ ] Click "Sign in with Google"
   - [ ] Google consent screen appears
   - [ ] After consent, redirected to dashboard
   - [ ] Dashboard shows user profile
   - [ ] Logout button works

### Common Issues & Fixes

**Issue: "Module not found" for @react-oauth/google**

- [ ] Run `npm install` in frontend directory
- [ ] Check package.json has the dependency
- [ ] Clear node_modules: `rm -r node_modules && npm install`

**Issue: "Cannot find symbol: GoogleAuthService"**

- [ ] Rebuild backend: `./mvnw.cmd clean install`
- [ ] Check Java files are in correct package structure
- [ ] Verify no import statement errors

**Issue: CORS errors in browser console**

- [ ] Verify application.yaml has correct CORS origins
- [ ] Restart backend after YAML changes
- [ ] Check SecurityConfig has @EnableCors

**Issue: "Invalid ID token" from backend**

- [ ] Verify Client ID in frontend = Client ID in backend
- [ ] Ensure fresh token (not expired)
- [ ] Check Google Cloud project OAuth setup

**Issue: Port 8080 or 5173 already in use**

- [ ] Find process using port: `netstat -ano | findstr :8080` (Windows)
- [ ] Kill process or use different port
- [ ] Update API_URL in frontend if changing port

### Files Ready for Testing

After checks pass, these files should be in place:

```
✓ backend/api/src/main/resources/application.yaml
✓ backend/api/pom.xml
✓ backend/api/src/main/java/com/sliitreserve/api/config/SecurityConfig.java
✓ backend/api/src/main/java/com/sliitreserve/api/controller/AuthController.java
✓ backend/api/src/main/java/com/sliitreserve/api/service/GoogleAuthService.java
✓ backend/api/src/main/java/com/sliitreserve/api/util/JwtUtil.java
✓ backend/api/src/main/java/com/sliitreserve/api/dto/*
✓ frontend/package.json (with @react-oauth/google)
✓ frontend/src/App.jsx
✓ frontend/src/main.jsx
✓ frontend/src/pages/Login.jsx
✓ frontend/src/pages/Dashboard.jsx
✓ frontend/src/services/authService.js
```

### Security Checklist

Before deploying to production:

- [ ] Change JWT secret to strong random string (64+ characters)
- [ ] Move secrets to environment variables (not hardcoded)
- [ ] Enable HTTPS for all endpoints
- [ ] Set `httpOnly` flag on cookies if using cookies
- [ ] Add rate limiting to auth endpoints
- [ ] Setup CSRF protection
- [ ] Configure refresh token in httpOnly cookie
- [ ] Add logging for auth attempts
- [ ] Setup token revocation mechanism
- [ ] Test token expiration scenarios

---

**Once all items are checked, you're ready to test the authentication flow!**

Next: Open terminal and run `./start-services.bat` (Windows) or `./start-services.sh` (Linux/Mac)
