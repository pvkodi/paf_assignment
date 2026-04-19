# OAuth Configuration Verification Guide

## Problem Analysis

### Issue: `Error 401: invalid_client`
This error occurs when:
1. Frontend sends a request with an invalid/missing Client ID
2. Google OAuth validates the Client ID against registered applications
3. No matching application found → 401 invalid_client

---

## Root Cause Found ✅

### What Was Missing
- **Frontend .env file** not created
- Frontend defaulted to `"YOUR_GOOGLE_CLIENT_ID"` (invalid placeholder)
- Backend had the correct credentials but frontend never sent them

### Configuration Mismatch
```
Backend OAuth Client ID:     954142944464-qroe8pd2cpv00lt158p4o7dj2a1sc6vd.apps.googleusercontent.com
Frontend OAuth Client ID:    "YOUR_GOOGLE_CLIENT_ID" ❌ (INVALID - missing .env)
```

---

## What Was Fixed

### Created: `frontend/.env`
```
VITE_API_URL=http://localhost:8080/api
VITE_GOOGLE_CLIENT_ID=954142944464-qroe8pd2cpv00lt158p4o7dj2a1sc6vd.apps.googleusercontent.com
VITE_OAUTH_REDIRECT_URI=http://localhost:5173/auth/callback
```

### Client ID Sync Check
✅ Frontend VITE_GOOGLE_CLIENT_ID = Backend app.auth.google.client-id
✅ Both reference the same Google OAuth application

---

## Next Steps to Verify

### Step 1: Verify Frontend Starts With New Config
```bash
# Stop the frontend dev server (Ctrl+C if running)
# Restart it to load the new .env file
cd frontend
npm run dev
```

**Expected output:** Vite dev server starts on http://localhost:5173

### Step 2: Check Browser Console
1. Open browser DevTools (F12)
2. Go to Login page (http://localhost:5173)
3. In Console, check for any errors about VITE_GOOGLE_CLIENT_ID

**Should see:** `VITE_GOOGLE_CLIENT_ID = 954142944464-qroe8pd2cpv00lt158p4o7dj2a1sc6vd.apps.googleusercontent.com`

### Step 3: Verify Google Cloud Console Setup
1. Go to https://console.cloud.google.com
2. Select project: **VenueLink Hub** (or your GCP project)
3. Navigate to: APIs & Services → Credentials
4. Find OAuth 2.0 Client ID: **954142944464-qroe8pd2cpv00lt158p4o7dj2a1sc6vd.apps.googleusercontent.com**

**Check these fields:**
- ✅ Authorized redirect URIs includes: `http://localhost:5173/auth/callback`
- ✅ Client ID is active (not deleted/disabled)
- ✅ Client Secret matches: `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`

### Step 4: Test OAuth Callback Endpoint (Backend)
```bash
# Open Terminal
# Test the backend OAuth endpoint exists
curl -X POST http://localhost:8080/api/v1/auth/oauth/google/callback \
  -H "Content-Type: application/json" \
  -d '{"code":"test","redirectUri":"http://localhost:5173/auth/callback"}'
```

**Expected response:** 
- ❌ Bad Request (because code="test" is invalid) OR
- ✅ 400 with helpful error message (proves endpoint is reachable)

**NOT expected:**
- ❌ 404 Not Found (endpoint would be missing)
- ❌ 401 Unauthorized (would mean CORS or auth config is broken)

### Step 5: Test Full OAuth Flow
1. Ensure backend is running: `cd backend/api && ./mvnw.cmd spring-boot:run`
2. Ensure frontend is running: `cd frontend && npm run dev`
3. Navigate to http://localhost:5173
4. Click "Sign in with Google"
5. You should be redirected to **Google Login** (not OAuth error page)

**Success indicators:**
- ✅ Redirected to accounts.google.com
- ✅ See login form with your Google account
- ✅ Can proceed with authentication

**Failure indicators:**
- ❌ "Authorization Error" before Google login
- ❌ Invalid_client error on Google's side
- → Means Client ID mismatch still exists

---

## Validation Checklist

- [ ] Frontend .env file created with valid VITE_GOOGLE_CLIENT_ID
- [ ] Frontend dev server restarted (to reload .env)
- [ ] Browser console shows correct Client ID (not "YOUR_GOOGLE_CLIENT_ID")
- [ ] Google Cloud Console has authorized redirect URI: http://localhost:5173/auth/callback
- [ ] Backend is running on http://localhost:8080
- [ ] /api/v1/auth/oauth/google/callback endpoint responds (not 404)
- [ ] Can proceed to Google login page without OAuth error

---

## If Still Having Issues

### Check 1: Google Cloud Console Authorized Redirect URIs
**Path:** APIs & Services → Credentials → OAuth 2.0 Client ID (Web application)

**Add these redirect URIs if missing:**
- `http://localhost:5173/auth/callback` (development)
- `http://localhost:3000/auth/callback` (if using port 3000)

### Check 2: Backend CORS Configuration
**File:** `application.yaml`
```yaml
cors:
  allowed-origins: http://localhost:5173,http://localhost:3000,http://localhost:8080
```

✅ Frontend origin `http://localhost:5173` is in allowed-origins

### Check 3: Browser Cache
Clear browser localStorage and sessionStorage:
1. Press F12 (DevTools)
2. Application → Storage → Clear All
3. Refresh page

### Check 4: Environment Variables Not Loading
If frontend still shows "YOUR_GOOGLE_CLIENT_ID":
- Stop frontend dev server (Ctrl+C)
- Delete `node_modules/.vite` cache (if it exists)
- Restart: `npm run dev`

---

## Root Cause Summary

| Component | Issue | Fix |
|-----------|-------|-----|
| Frontend `.env` | Missing file | ✅ Created with correct Client ID |
| VITE_GOOGLE_CLIENT_ID | Set to fallback "YOUR_GOOGLE_CLIENT_ID" | ✅ Now matches backend value |
| Google OAuth Client ID | Never reached backend because invalid | ✅ Now will be validated by Google |
| GCP Redirect URI | Should include http://localhost:5173/auth/callback | ⚠️ **MUST VERIFY** |

---

## Command to Restart Everything Fresh

```bash
# Stop all services
# Open 4 terminal windows

# Terminal 1: Backend API
cd backend/api
./mvnw.cmd spring-boot:run

# Terminal 2: Frontend
cd frontend
npm run dev

# Terminal 3: Docker (PostgreSQL)
docker compose -f infra/docker-compose.yml up -d

# Terminal 4: Test OAuth endpoint
curl -X POST http://localhost:8080/api/v1/auth/oauth/google/callback \
  -H "Content-Type: application/json" \
  -d '{"code":"test","redirectUri":"http://localhost:5173/auth/callback"}'
```

Then try logging in at http://localhost:5173
