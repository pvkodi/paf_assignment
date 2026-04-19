# Integration Quick Start Guide

## What's New

Your VenueLink API now has a complete check-in to suspension workflow:

1. **Check-In Endpoints** - Users can check in via QR or staff can check them in manually
2. **Automatic No-Show Detection** - Scheduler runs every 10 minutes to find no-shows
3. **Automatic Suspension** - After 3 no-shows, users get a 1-week suspension
4. **Complete Workflow** - Everything is wired together and ready to test

---

## Fastest Way to Test Everything

### 1. Build & Start Services

```bash
cd infra
docker compose up --build -d
```

Wait 20-40 seconds for backend to start.

### 2. Verify Services Running

```bash
docker compose ps
```

You should see:
- ✅ smartcampus-postgres (healthy)
- ✅ backend (running)
- ✅ frontend (running)
- ✅ mailtrap (running)

### 3. Test Check-In Endpoint

**Option A: Using Postman** (recommended)
- Import `Smart_Campus_API.postman_collection.json`
- Go to: `Bookings → Create Booking`
- Then: `Bookings → Check-In → QR Check-In`

**Option B: Using curl**

```bash
# 1. Get auth token (replace credentials)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@campus.edu", "password": "pass"}'

# Response: {"token": "eyJhbGc..."}

# 2. Create booking
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{
    "facility_id": "facility-uuid",
    "booking_date": "2026-04-15",
    "start_time": "14:00",
    "end_time": "14:30",
    "purpose": "Team meeting"
  }'

# Response: {"id": "booking-uuid", "status": "PENDING"}

# 3. Record QR check-in
curl -X POST http://localhost:8080/api/v1/bookings/booking-uuid/check-in/qr \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json"

# Response: {"id": "checkin-uuid", "method": "QR", "checked_in_at": "2026-04-15T14:05:30"}

# 4. Verify check-in recorded
curl -X GET http://localhost:8080/api/v1/bookings/booking-uuid/check-in/status \
  -H "Authorization: Bearer eyJhbGc..."

# Response: true
```

---

## Test No-Show Detection

### Manual Test (Immediate)

As an admin, you can manually trigger no-show evaluation:

```bash
curl -X GET "http://localhost:8080/api/v1/bookings/booking-uuid/evaluate-no-show" \
  -H "Authorization: Bearer admin-token"

# Response: true (is no-show) or false (not a no-show)
```

### Automatic Test (After 10 minutes)

1. Create a booking for some time in the past (e.g., 2 hours ago)
2. Don't record a check-in
3. Wait up to 10 minutes
4. Check backend logs:

```bash
docker compose logs -f backend | grep "No-show"
```

You should see output like:
```
INFO: Found 1 bookings to evaluate for no-shows
INFO: No-show evaluation job completed. Processed: 1, No-shows: 1, Suspensions applied: 0
```

---

## Test Suspension Flow

### Step 1: Create 3 No-Shows

Create 3 bookings in the past without any check-ins. The scheduler will evaluate them and mark as no-shows.

### Step 2: Check User Suspension Status

```bash
curl -X GET "http://localhost:8080/auth/profile" \
  -H "Authorization: Bearer user-token"

# Response shows:
# "noShowCount": 3,
# "suspendedUntil": "2026-04-22T13:45:00"
```

### Step 3: Try to Create Booking (Should Fail)

```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer user-token" \
  -H "Content-Type: application/json" \
  -d '{"facility_id": "...", ...}'

# Response: HTTP 403 Forbidden
# {"error": "Your account is suspended until 2026-04-22T13:45:00. Submit an appeal."}
```

### Step 4: Submit Suspension Appeal

```bash
curl -X POST "http://localhost:8080/api/v1/appeals" \
  -H "Authorization: Bearer user-token" \
  -H "Content-Type: application/json" \
  -d '{"reason": "Medical emergency prevented check-in"}'

# Response: {"id": "appeal-uuid", "status": "PENDING"}
```

### Step 5: Admin Approves Appeal

```bash
curl -X POST "http://localhost:8080/api/v1/appeals/appeal-uuid/approve" \
  -H "Authorization: Bearer admin-token" \
  -H "Content-Type: application/json" \
  -d '{"decision": "APPROVE", "notes": "Approved based on medical documentation"}'

# Response: {"status": "APPROVED", "message": "Appeal approved. Suspension lifted."}
```

### Step 6: User Can Book Again

```bash
curl -X GET "http://localhost:8080/auth/profile" \
  -H "Authorization: Bearer user-token"

# Response shows:
# "noShowCount": 0,
# "suspendedUntil": null
# User can now book facilities again
```

---

## Monitor Scheduler Execution

Watch the scheduler job run automatically every 10 minutes:

```bash
# Terminal 1: Watch logs
docker compose logs -f backend | grep -E "No-show|scheduler|evaluateNoShows"

# Terminal 2: Keep making requests to see cache/data changes
curl -X GET "http://localhost:8080/auth/profile" \
  -H "Authorization: Bearer user-token"
```

Expected log output pattern:
```
[00:10:00] Starting scheduled no-show evaluation job
[00:10:00] Found 2 bookings to evaluate for no-shows
[00:10:01] Booking [id] evaluated as no-show
[00:10:01] User [id] no-show count incremented to 1
[00:10:02] No-show evaluation job completed. Processed: 2, No-shows: 2, Suspensions applied: 0
```

---

## API Contract Summary

| Endpoint | Method | Auth | Suspended? | Purpose |
|----------|--------|------|-----------|---------|
| `/api/v1/bookings/{id}/check-in/qr` | POST | Yes | 403 | User QR check-in |
| `/api/v1/bookings/{id}/check-in/manual` | POST | Yes (TECH/ADMIN) | 403 | Staff manual check-in |
| `/api/v1/bookings/{id}/check-in/status` | GET | Yes | 403 | Check if checked in |
| `/api/v1/bookings/{id}/evaluate-no-show` | GET | Yes (ADMIN) | Allow | Manual no-show eval |
| `/api/v1/bookings` (create) | POST | Yes | 403 | Create booking |
| `/auth/profile` | GET | Yes | Allow | View profile (whitelist) |
| `/api/v1/appeals` | POST | Yes | Allow | Submit appeal (whitelist) |

**Note**: Suspended users cannot perform 403-marked operations. Whitelisted operations marked "Allow" can be used while suspended.

---

## Troubleshooting

### Backend not starting?

```bash
docker compose logs backend | tail -50
# Check for database connection issues
# Verify postgres is healthy: docker compose ps
```

### Scheduler not running?

Check logs:
```bash
docker compose logs backend | grep -i "scheduled\|scheduler"
```

The scheduler starts 5 minutes after boot, then runs every 10 minutes.

### Endpoints returning 404?

Verify backend is running and API is accessible:
```bash
curl http://localhost:8080/health
# Should return 200 OK
```

### No-show not being evaluated?

1. Verify database has bookings in the past without check-ins
2. Wait up to 10 minutes for scheduler
3. Check logs: `docker compose logs backend | grep "No-show"`

---

## Full Documentation

See [WORKFLOW_INTEGRATION_COMPLETE.md](WORKFLOW_INTEGRATION_COMPLETE.md) for:
- Complete architecture diagram
- All requirements mapping
- Detailed integration points
- Data flow explanation

---

## What's Working

✅ QR code check-in (user-initiated)  
✅ Manual check-in (staff-recorded)  
✅ Automatic 15-minute grace period  
✅ Automatic no-show detection (every 10 min)  
✅ Automatic suspension after 3 no-shows  
✅ Suspension enforcement (403 on protected operations)  
✅ Appeal submission & approval workflow  
✅ Full transactional integrity  
✅ Timezone-aware calculations  
✅ Comprehensive logging  

---

## Questions?

Check the logs:
```bash
docker compose logs -f backend
```

Or review the implementation files:
- [CheckInController.java](backend/api/src/main/java/com/sliitreserve/api/controllers/bookings/CheckInController.java)
- [NoShowScheduler.java](backend/api/src/main/java/com/sliitreserve/api/services/booking/NoShowScheduler.java)
- [CheckInService.java](backend/api/src/main/java/com/sliitreserve/api/services/booking/CheckInService.java)
