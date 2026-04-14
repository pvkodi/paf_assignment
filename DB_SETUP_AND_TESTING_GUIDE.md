# Database Setup & Testing Guide

## Quick Start: Seed Database with Test Data

### Prerequisites

Ensure PostgreSQL 14+ is running and accessible. Update `backend/api/src/main/resources/application.yaml` with your database credentials if needed.

### Option 1: Run Backend with Auto-Migration (Recommended)

```bash
cd /Users/pamigee/Desktop/paf_assignment/backend/api

# Start backend with Flyway auto-migration
./mvnw spring-boot:run

# Backend will:
# 1. Connect to PostgreSQL
# 2. Run all migrations (V1, V2, V3)
# 3. Seed test data automatically
# 4. Start Spring app on http://localhost:8080
```

Logs will show:
```
[INFO] web Starting DispatcherServlet 'dispatcherServlet'
[INFO] Flyway database migration completed successfully
[INFO] Started ApiApplication in X.XXX seconds
```

### Option 2: Database Only (Without Backend)

```bash
# Using Docker Compose
cd /Users/pamigee/Desktop/paf_assignment/infra
docker-compose up -d

# Manually run Flyway migrations
cd /Users/pamigee/Desktop/paf_assignment/backend/api
./mvnw flyway:migrate
```

---

## Test Data Overview

### 👥 Test User Accounts

All users have password: **`YourPassword123!`**

| Email | Role | Purpose |
|-------|------|---------|
| `admin@smartcampus.edu` | ADMIN | Test admin-only features, approvals |
| `lecturer@smartcampus.edu` | LECTURER | Test lecturer role, approvals |
| `lecturer2@smartcampus.edu` | LECTURER | Secondary lecturer for scenario testing |
| `facility.manager@smartcampus.edu` | FACILITY_MANAGER | Test facility manager permissions |
| `student@smartcampus.edu` | USER | Test basic user booking flow |
| `student2@smartcampus.edu` | USER | Test conflict/quota scenarios |
| `student3@smartcampus.edu` | USER | Additional test user |
| `tech@smartcampus.edu` | TECHNICIAN | Test technician workflows |

---

### 🏛️ Test Facilities

| Name | Code | Capacity | Hours | Purpose |
|------|------|----------|-------|---------|
| Main Lecture Hall A101 | LH-A101 | 120 | 08:00-18:00 | Large class sessions |
| Computing Lab C203 | LAB-C203 | 40 | 08:00-20:00 | Practical sessions |
| Seminar Room B205 | SR-B205 | 25 | 08:00-18:00 | Group discussions |
| Main Auditorium | AUD-MAIN | 500 | 07:00-22:00 | Large events, stage |
| Advanced Lab D302 | LAB-D302 | 50 | 08:00-20:00 | Advanced workshops |
| Committee Room E101 | MR-E101 | 15 | 08:00-17:30 | Meetings |

---

### 📅 Public Holidays (Apr-May 2026)

These dates will cause recurring booking occurrences to be skipped:

- **April 14-15**: Sinhala & Tamil New Year
- **May 1**: International Workers Day  
- **May 27**: Poson Full Moon Poya Day

---

### 📋 Test Booking Scenarios

#### Scenario 1: View Approved Booking
- **Booking**: Seminar on **April 16** (10:00-12:00)
- **Facility**: Seminar Room B205 (25 capacity)
- **Status**: APPROVED
- **User**: student2@smartcampus.edu
- **Purpose**: Database Design Theory Discussion

**Test**: Click notification or find booking in list

---

#### Scenario 2: Pending Approval
- **Booking**: DB Workshop on **April 17** (14:00-16:30)
- **Facility**: Advanced Computing Lab D302 (50 capacity)
- **Status**: PENDING
- **User**: student3@smartcampus.edu
- **Purpose**: Advanced SQL Optimization Workshop

**Test**: Admin/Lecturer can approve this booking

---

#### Scenario 3: Large Event
- **Booking**: Cultural Event on **April 19** (18:00-20:00)
- **Facility**: Main Auditorium (500 capacity)
- **Status**: PENDING
- **User**: student@smartcampus.edu
- **Attendees**: 200

**Test**: Facility manager must approve large events

---

#### Scenario 4: Recurring Weekly Course (Important!)
- **Master Booking**: Weekly DB Course
- **Facility**: Advanced Computing Lab D302  
- **Pattern**: `FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=12`
- **Time**: 09:00-11:00
- **Status**: APPROVED
- **Lecturer**: lecturer2@smartcampus.edu

**Test**: Try to delete one occurrence, check conflict handling on public holidays

**Expected Skipped Dates**:
- April 14, 15 (New Year holiday)
- Any dates with conflicts

**To test recurrence skipping in UI**:
```
1. Create new recurring booking
2. Set pattern: FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=5
3. Submit - UI should poll and show skipped dates
4. Check Notifications center for BOOKING_RECURRING_SKIPPED event
```

---

#### Scenario 5: Daily Standups
- **Master Booking**: Daily Team Standups
- **Facility**: Committee Room E101
- **Pattern**: `FREQ=DAILY;COUNT=10`
- **Time**: 08:30-09:00
- **Status**: PENDING

**Test**: Admin approves recurring daily meetings

---

### 📊 Analytics Test Data

Pre-seeded utilization snapshots show:

| Facility | Utilization | Notes |
|----------|--------------|-------|
| Seminar Room B205 | 65% | Good utilization |
| Committee Room E101 | 25% | **Underutilized** (recommendation trigger) |
| Advanced Lab D302 | 80% | High utilization |
| Sports Court | 30% | **Underutilized** |

**Test**:
```
1. Login as ADMIN
2. Navigate to Analytics Dashboard
3. Run analysis for date range (30 days)
4. Should see recommendations for underutilized facilities
5. Check heatmap for usage patterns
```

---

### 🔔 Test Notifications

Pre-seeded in-app notifications:
- ✅ **Booking Approved**: Seminar approval notification
- ⚠️ **Conflict Warning**: Time slot conflict detected
- 💡 **Facility Recommendation**: Alternative facility suggestion

**Test**:
```
1. Login as student2@smartcampus.edu
2. Go to Notifications Center
3. Should see 3 pre-seeded notifications
4. Mark as read and verify UI updates
5. Check pagination (page size: 10)
```

---

## Testing Checklist

### Backend API Testing (via curl or Postman)

```bash
# Get all facilities
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/facilities

# Search facilities
curl -H "Authorization: Bearer YOUR_TOKEN" \
  'http://localhost:8080/api/v1/facilities?search=Lab'

# Create booking
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "facilityId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1",
    "bookingDate": "2026-04-20",
    "startTime": "10:00:00",
    "endTime": "12:00:00",
    "purpose": "Test booking",
    "attendees": 15,
    "recurrenceRule": null
  }'

# Get notifications
curl -H "Authorization: Bearer YOUR_TOKEN" \
  'http://localhost:8080/api/v1/notifications?page=0&size=10'

# Get analytics
curl -H "Authorization: Bearer YOUR_TOKEN" \
  'http://localhost:8080/api/v1/analytics/utilization?from=2026-04-01&to=2026-05-01'
```

### Frontend UI Testing Scenarios

#### ✅ Facility Search & Selection
```
1. Go to Facilities & Bookings page
2. Search "Lab" - should show 2 results
3. Filter by capacity (40+)
4. Select "Advanced Computing Lab D302"
5. Verify info displays (capacity, location, hours)
```

#### ✅ Create Simple Booking
```
1. Select facility (Seminar Room)
2. Pick date: April 20
3. Time: 14:00-15:30
4. Purpose: "Team meeting"
5. Attendees: 12
6. Submit
7. Should see success message
8. Check notifications center for confirmation
```

#### ✅ Create Recurring Booking (The Key Test!)
```
1. Select facility (Advanced Lab D302)
2. Pick date: April 16 (Monday)
3. Time: 09:00-11:00
4. Purpose: "Weekly Workshop"
5. Attendees: 30
6. Check: "This is a recurring booking"
7. Recurrence: FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=4
8. Submit
9. **UI should show polling indicator**
10. **Yellow notification appears with skipped dates**
    (e.g., "Skipped: 2026-04-14" for holiday)
11. Click link to view booking details
```

#### ✅ Conflict Detection
```
1. Try to create booking on April 16 10:00-11:00  
   (conflicts with existing APPROVED booking)
2. Should get 409 Conflict error
3. Error message: "...already booked during this time..."
4. Try another time or facility
```

#### ✅ Admin "Book For" Feature
```
1. Login as facility.manager@smartcampus.edu
2. Create booking
3. Should see "Book for Another User" field
4. Enter: student@smartcampus.edu
5. Submit as admin on behalf of student
```

#### ✅ Notifications Center
```
1. Go to Notifications page
2. Verify unread count shown
3. Click on a notification
4. Mark as read - should update count
5. Click "Mark all as read"
6. Should show 0 unread
```

#### ✅ Analytics Dashboard (ADMIN only)
```
1. Login as admin@smartcampus.edu
2. Go to Analytics Dashboard
3. Set date range: April 1 - April 30
4. Click "Run Analysis"
5. Should see:
   - Heatmap table (facility × day × hour)
   - Underutilized facilities list
   - Recommendations section
6. Scroll through heatmap (30+ rows)
```

---

## Database Commands

### Check Database Connection

```bash
psql -U admin -h localhost -d smartcampus_db

-- Once connected, test some queries:
SELECT COUNT(*) FROM public."user";
SELECT COUNT(*) FROM public.facility;
SELECT COUNT(*) FROM public.booking;
SELECT COUNT(*) FROM public.notification;
SELECT * FROM public.public_holiday ORDER BY holiday_date;
```

### Reset Database (Fresh Start)

```bash
./mvnw flyway:clean
./mvnw flyway:migrate
```

### View Flyway Version History

```bash
psql -U admin -h localhost -d smartcampus_db
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

---

## Troubleshooting

### Issue: "Connection refused" to PostgreSQL

**Solution**: Start PostgreSQL or Docker Compose
```bash
cd infra
docker-compose up -d
```

### Issue: Migrations failed with "already exists"

**Solution**: Clean and run again
```bash
./mvnw flyway:clean
./mvnw flyway:migrate
```

### Issue: UI doesn't show test data

**Solution**: Refresh page and check:
1. Backend running on port 8080
2. JWT token valid
3. Check browser console for API errors

### Issue: Recurring booking notification doesn't appear

**Solution**:
1. Ensure BookingService publishes `BOOKING_RECURRING_SKIPPED` event
2. InAppObserver creates notification
3. Check query: Notifications API should return it within 3 seconds
4. Check database:
   ```sql
   SELECT * FROM public.notification 
   WHERE event_type='BOOKING_RECURRING_SKIPPED' 
   ORDER BY created_at DESC;
   ```

---

## Next Steps

1. **Start Backend**: `./mvnw spring-boot:run` (Spring runs migrations)
2. **Start Frontend**: `npm run dev` in `frontend/` folder
3. **Login**: Use credentials above
4. **Test**: Follow scenarios above
5. **Monitor Logs**:
   ```bash
   # Terminal 1: Backend
   cd backend/api && ./mvnw spring-boot:run
   
   # Terminal 2: Frontend  
   cd frontend && npm run dev
   
   # Terminal 3: Watch migrations
   tail -f backend/api/target/api-*.log
   ```

---

## Database Schema Quick Reference

Main tables:
- `public."user"` - User accounts
- `public.facility` - Facilities and resources
- `public.booking` - Booking requests (master + occurrences)
- `public.public_holiday` - Non-working dates
- `public.notification` - In-app notification inbox
- `public.utilization_snapshot` - Daily analytics data
- `public.approval_step` - Booking approval workflow
- `public.check_in` - Facility usage tracking

UUIDs are stable and documented above for manual testing.
