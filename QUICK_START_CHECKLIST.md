# QUICK START EXECUTION CHECKLIST

## PRE-FLIGHT (Do Once)

- [ ] **PostgreSQL Running**
  ```bash
  # On Windows:
  # Check Services > PostgreSQL (should be running)
  # Or: psql -U smartcampus -d smartcampus -h localhost
  # Should connect without errors
  ```

- [ ] **Backend Started**
  ```bash
  cd backend/api
  mvn clean spring-boot:run
  # Wait for: "Started Application in X seconds"
  ```

- [ ] **Frontend Started**
  ```bash
  cd frontend
  npm install  # First time only
  npm run dev
  # Should show: "VITE v... ready in X ms"
  # localhost:5173 accessible
  ```

- [ ] **Create Test Database**
  ```bash
  psql -U smartcampus -d smartcampus -h localhost
  # Copy/paste SECTION 1-3 from sql_setup_scripts.md
  # Run VERIFICATION CHECKLIST queries
  ```

- [ ] **Postman Imported** (Optional but recommended)
  - Create collection "VenueLink API"
  - Create folders: Auth, Tickets, Bookings, Appeals, Notifications, Analytics
  - Add environment variables: `base_url`, `admin_token`, `student_token`, `technician_token`

---

## PHASE 1: AUTH (START HERE)

**Duration**: 30 minutes  
**Tools**: Postman or curl

### Step 1.1: Health Check
```
GET http://localhost:8080/api/health
Expected: 200 OK { "status": "UP" }
```

### Step 1.2: Test OAuth Flow
```
POST http://localhost:8080/api/v1/auth/oauth/google/callback
Body (manual test):
{
  "code": "{actual_google_code_from_browser}",
  "redirectUri": "http://localhost:5173/auth/callback"
}
Expected: 200 OK + JWT token in response
Store JWT: copy to Postman environment as {{admin_token}}
```

### Step 1.3: Profile Test
```
GET http://localhost:8080/api/v1/auth/profile
Auth: Bearer {{admin_token}}
Expected: 200 OK + user data with roles
```

### Step 1.4: Create Student Token
Repeat Step 1.2 with different Google account (or use pre-created student DB user)
Store JWT: {{student_token}}

### Success Criteria
- ✅ Health check returns 200
- ✅ OAuth exchange returns valid JWT
- ✅ Profile endpoint returns user with roles
- ✅ Two different tokens created (admin + student)

---

## PHASE 2-3: RBAC SETUP

**Duration**: 15 minutes  
**Tools**: PostgreSQL

### Run these SQL sections in order:
1. SECTION 1: CREATE TEST USERS & ROLES
2. SECTION 2: CREATE TEST FACILITIES
3. SECTION 3: CREATE SUSPENDED USER
4. VERIFICATION CHECKLIST: Confirm all data

### Double-check
```sql
SELECT u.email, array_agg(r.role) as roles FROM "user" u 
LEFT JOIN user_roles r ON u.id = r.user_id
WHERE u.email LIKE '%@campus.edu'
GROUP BY u.email;

-- Should show:
-- admin@campus.edu | {ADMIN}
-- lecturer@campus.edu | {LECTURER}
-- student@campus.edu | {STUDENT}
-- facmgr@campus.edu | {FACILITY_MANAGER}
-- technician@campus.edu | {MAINTENANCE_STAFF}
-- student2@campus.edu | {STUDENT}
-- suspended@campus.edu | {STUDENT}
```

---

## PHASE 4: TICKETS (CORE TESTING)

**Duration**: 3-4 hours  
**Tools**: Postman + PostgreSQL queries

### 4.1: Create Ticket
```
POST http://localhost:8080/api/tickets
Auth: {{student_token}}
Body: {
  "facilityId": "10000001-0000-0000-0000-000000000001",
  "title": "Broken projector",
  "description": "Projector disconnects during class",
  "category": "ELECTRICAL",
  "priority": "MEDIUM"
}
Expected: 201 Created
Save ticket_id to {{ticket_id}}
```

### 4.2: Verify in DB
```sql
SELECT id, title, status, sla_deadline FROM maintenance_ticket
ORDER BY created_at DESC LIMIT 1;

-- Verify:
-- status = OPEN
-- sla_deadline = NOW() + 24 hours (for MEDIUM priority)
```

### 4.3: Add Comment (Public)
```
POST http://localhost:8080/api/tickets/{{ticket_id}}/comments
Auth: {{student_token}}
Body: {
  "content": "This happens every Tuesday class",
  "visibility": "PUBLIC"
}
Expected: 201 Created
```

### 4.4: Add Comment (Internal - Technician)
```
POST http://localhost:8080/api/tickets/{{ticket_id}}/comments
Auth: {{technician_token}}
Body: {
  "content": "Need to replace HDMI cable, cost ~$45",
  "visibility": "INTERNAL"
}
Expected: 201 Created
Store comment_id to {{comment_id}}
```

### 4.5: Verify Comment Visibility
```
GET http://localhost:8080/api/tickets/{{ticket_id}}/comments
Auth: {{student_token}}
Expected: Only 1 comment (PUBLIC only, INTERNAL hidden)

GET http://localhost:8080/api/tickets/{{ticket_id}}/comments
Auth: {{technician_token}}
Expected: 2 comments (PUBLIC + INTERNAL visible)
```

### 4.6: Upload Attachment
```
POST http://localhost:8080/api/tickets/{{ticket_id}}/attachments
Auth: {{student_token}}
Content-Type: multipart/form-data
File: [upload any jpg/png image]
Expected: 201 Created with attachment_id
Save attachment_id to {{attachment_id}}
```

### 4.7: Verify File Uploaded
```bash
# Check file system:
ls -la uploads/original/  # Should see file
ls -la uploads/thumbnails/  # Should see thumbnail

# Verify DB:
SELECT id, file_name, file_size FROM ticket_attachment 
WHERE id = '{{attachment_id}}';
```

### 4.8: Assign Ticket (Facility Manager)
```
POST http://localhost:8080/api/tickets/{{ticket_id}}/assign
Auth: {{facmgr_token}}
Body: {
  "assignToUserId": "00000001-0000-0000-0000-000000000005"
}
Expected: 200 OK
```

### 4.9: Update Status to IN_PROGRESS (Technician)
```
PUT http://localhost:8080/api/tickets/{{ticket_id}}/status
Auth: {{technician_token}}
Body: {
  "status": "IN_PROGRESS"
}
Expected: 200 OK

-- Verify in DB:
SELECT status FROM maintenance_ticket WHERE id = '{{ticket_id}}';
-- Should show: IN_PROGRESS
```

### 4.10: Update Status to RESOLVED
```
PUT http://localhost:8080/api/tickets/{{ticket_id}}/status
Auth: {{technician_token}}
Body: {
  "status": "RESOLVED"
}
Expected: 200 OK
```

### 4.11: Update Status to CLOSED
```
PUT http://localhost:8080/api/tickets/{{ticket_id}}/status
Auth: {{facmgr_token}}
Body: {
  "status": "CLOSED"
}
Expected: 200 OK
```

### 4.12: Delete Attachment (By Owner)
```
DELETE http://localhost:8080/api/tickets/{{ticket_id}}/attachments/{{attachment_id}}
Auth: {{student_token}}
Expected: 204 No Content

-- Try with different user:
DELETE http://localhost:8080/api/tickets/{{ticket_id}}/attachments/{{attachment_id}}
Auth: {{technician_token}}
Expected: 403 Forbidden (not owner/admin)
```

### 4.13: Update Own Comment
```
PUT http://localhost:8080/api/tickets/{{ticket_id}}/comments/{{comment_id}}
Auth: {{student_token}}
Body: {
  "content": "Updated: This happens every Tuesday and Thursday class",
  "visibility": "PUBLIC"
}
Expected: 200 OK
```

### 4.14: Delete Comment (Admin Override)
```
DELETE http://localhost:8080/api/tickets/{{ticket_id}}/comments/{{comment_id}}
Auth: {{admin_token}}
Expected: 204 No Content

-- Try as non-owner, non-admin:
DELETE http://localhost:8080/api/tickets/{{ticket_id}}/comments/{{comment_id}}
Auth: {{technician_token}}
Expected: 403 Forbidden
```

### 4.15: Test Error Cases

**Invalid transition**:
```
PUT http://localhost:8080/api/tickets/{{ticket_id}}/status
Body: { "status": "OPEN" }  # Try to transition CLOSED → OPEN
Expected: 400 Bad Request "Invalid status transition"
```

**Student tries to assign**:
```
POST http://localhost:8080/api/tickets/{{ticket_id}}/assign
Auth: {{student_token}}
Expected: 403 Forbidden "Insufficient permissions"
```

**Non-owner tries to update comment**:
```
PUT http://localhost:8080/api/tickets/{{ticket_id}}/comments/{{comment_id}}
Auth: {{student2_token}}
Expected: 403 Forbidden "Not authorized to edit this comment"
```

### Phase 4 Success Criteria
- ✅ Create ticket (OPEN status, SLA set)
- ✅ Add comments (public & internal, role-filtered)
- ✅ Upload attachment (file + thumbnail created)
- ✅ Assign ticket (facility manager only)
- ✅ Update status (valid transitions only)
- ✅ Delete attachment (owner/admin only)
- ✅ Edit comment (owner/admin only)
- ✅ Delete comment (owner/admin only)
- ✅ Error handling for invalid operations

---

## PHASE 5: APPEALS

**Duration**: 1 hour  
**Tools**: Postman + PostgreSQL

### 5.1: Submit Appeal (Suspended User)
```
POST http://localhost:8080/api/v1/appeals
Auth: {{suspended_token}}  # From DB step: suspended@campus.edu
Body: {
  "reason": "I had medical emergency on those dates. I have doctor documentation."
}
Expected: 201 Created with appeal_id
Save to {{appeal_id}}
```

### 5.2: Verify Status
```sql
SELECT id, status, reason FROM suspension_appeal WHERE id = '{{appeal_id}}';
-- Should show: status = SUBMITTED
```

### 5.3: List Appeals (Admin View)
```
GET http://localhost:8080/api/v1/appeals
Auth: {{admin_token}}
Expected: 200 OK with array containing {{appeal_id}}
```

### 5.4: List Appeals (User View)
```
GET http://localhost:8080/api/v1/appeals
Auth: {{suspended_token}}
Expected: 200 OK with only own appeal
```

### 5.5: Get Appeal Detail
```
GET http://localhost:8080/api/v1/appeals/{{appeal_id}}
Auth: {{admin_token}} OR {{suspended_token}}
Expected: 200 OK with full appeal data
```

### 5.6: Approve Appeal (Admin)
```
POST http://localhost:8080/api/v1/appeals/{{appeal_id}}/approve
Auth: {{admin_token}}
Body: {
  "decision": "Medical documentation verified. Suspension lifted immediately."
}
Expected: 200 OK
```

### 5.7: Verify User Unsuspended
```sql
SELECT id, email, suspended_until, no_show_count FROM "user"
WHERE email = 'suspended@campus.edu';
-- Should show: suspended_until = NULL (or past date)
```

### 5.8: Test Reject Appeal
Create another suspended user:
```sql
INSERT INTO "user" (id, google_subject, email, display_name, suspended_until, no_show_count)
VALUES ('00000001-0000-0000-0000-000000000008', 'sub_sus2', 'suspended2@campus.edu', 'Frank', NOW() + INTERVAL '7 days', 3);
INSERT INTO user_roles (user_id, role) VALUES ('00000001-0000-0000-0000-000000000008', 'STUDENT');
```

Then:
```
POST http://localhost:8080/api/v1/appeals
Auth: {{suspended2_token}}
Body: { "reason": "I don't think I should be suspended" }
Expected: 201 Created, save as {{appeal_id_2}}

POST http://localhost:8080/api/v1/appeals/{{appeal_id_2}}/reject
Auth: {{admin_token}}
Body: { "decision": "Insufficient evidence. Suspension continues." }
Expected: 200 OK

-- Verify in DB:
SELECT status, user_id FROM suspension_appeal WHERE id = '{{appeal_id_2}}';
-- status = REJECTED

SELECT suspended_until FROM "user" WHERE id = (SELECT user_id FROM suspension_appeal WHERE id = '{{appeal_id_2}}');
-- Still shows future date (suspension remains)
```

### Phase 5 Success Criteria
- ✅ Suspended user can submit appeal
- ✅ Admin can list all pending appeals
- ✅ Users see only own appeals
- ✅ Admin can approve (suspension lifted)
- ✅ Admin can reject (suspension remains)
- ✅ User status updated correctly in DB

---

## PHASE 6: NOTIFICATIONS

**Duration**: 1 hour  
**Tools**: Postman

### 6.1: Get Initial Unread Count
```
GET http://localhost:8080/api/v1/notifications/unread/count
Auth: {{student_token}}
Expected: Some count (N)
```

### 6.2: Get Notification Feed
```
GET http://localhost:8080/api/v1/notifications?page=0&size=20
Auth: {{student_token}}
Expected: 200 OK, array of notifications sorted by date descending
```

### 6.3: Create Event That Generates Notification
```
POST http://localhost:8080/api/tickets
Auth: {{student_token}}
Body: { ... }
Expected: 201 Created ticket

-- Notifications should be auto-created for relevant users
```

### 6.4: Check Unread Count Increased
```
GET http://localhost:8080/api/v1/notifications/unread/count
Auth: {{student_token}}  # Or admin/others who should be notified
Expected: Count = N+1 (or more)
```

### 6.5: Mark Single Notification as Read
```
GET http://localhost:8080/api/v1/notifications?page=0&size=5
Auth: {{student_token}}
Expected: Get first notification id from response, save as {{notif_id}}

POST http://localhost:8080/api/v1/notifications/{{notif_id}}/read
Auth: {{student_token}}
Expected: 200 OK
```

### 6.6: Verify Read Status
```
GET http://localhost:8080/api/v1/notifications/{{notif_id}}
Auth: {{student_token}}
Expected: isRead = true
```

### 6.7: Mark All as Read
```
DELETE http://localhost:8080/api/v1/notifications
Auth: {{student_token}}
Expected: 200 OK

GET http://localhost:8080/api/v1/notifications/unread/count
Auth: {{student_token}}
Expected: 0 (all marked as read)
```

### Phase 6 Success Criteria
- ✅ Unread count queryable
- ✅ Notifications listed with pagination
- ✅ Single notification marked as read
- ✅ All notifications marked as read
- ✅ Unread count decreases correctly

---

## PHASE 7: BOOKINGS (COMPLEX - TAKE YOUR TIME)

**Duration**: 4-6 hours  
**Tools**: Postman + PostgreSQL  

### 7.1: Simple Booking (Non-Peak)
```
POST http://localhost:8080/api/v1/bookings
Auth: {{student_token}}
Body: {
  "facilityId": "10000001-0000-0000-0000-000000000003",
  "bookingDate": "2026-04-20",
  "startTime": "14:00",
  "endTime": "15:00",
  "purpose": "Project meeting",
  "attendees": 8,
  "recurrenceRule": null
}
Expected: 201 Created
Save to {{booking_id_1}}
```

### 7.2: Verify Booking Created
```sql
SELECT id, facility_id, booking_date, start_time, end_time, status FROM booking
WHERE id = '{{booking_id_1}}';
-- status should be PENDING
```

### 7.3: Test Overlap Detection
```
POST http://localhost:8080/api/v1/bookings
Auth: {{student_token}}
Body: {
  "facilityId": "10000001-0000-0000-0000-000000000003",
  "bookingDate": "2026-04-20",
  "startTime": "14:30",
  "endTime": "15:30",
  "purpose": "Overlapping",
  "attendees": 5
}
Expected: 409 Conflict "Facility already booked"
```

### 7.4: Test Peak-Hour Restriction (Student)
```
POST http://localhost:8080/api/v1/bookings
Auth: {{student_token}}
Body: {
  "facilityId": "10000001-0000-0000-0000-000000000003",
  "bookingDate": "2026-04-21",
  "startTime": "09:00",
  "endTime": "10:00",
  "purpose": "Peak hour try",
  "attendees": 5
}
Expected: 403 Forbidden "Peak-hour restriction (08:00-10:00)"
```

### 7.5: Test Peak-Hour OK (Lecturer)
```
POST http://localhost:8080/api/v1/bookings
Auth: {{lecturer_token}}
Body: {
  "facilityId": "10000001-0000-0000-0000-000000000003",
  "bookingDate": "2026-04-21",
  "startTime": "09:00",
  "endTime": "10:00",
  "purpose": "Lecturer booking during peak",
  "attendees": 15
}
Expected: 201 Created (lecturer exempt)
Save to {{booking_id_2}}
```

### 7.6: Test Capacity Validation
```
POST http://localhost:8080/api/v1/bookings
Auth: {{student_token}}
Body: {
  "facilityId": "10000001-0000-0000-0000-000000000003",
  "bookingDate": "2026-04-22",
  "startTime": "10:00",
  "endTime": "11:00",
  "purpose": "Too many people",
  "attendees": 500
}
Expected: 400 Bad Request "Attendees (500) exceed capacity (20)"
```

### 7.7: Test Admin Book on Behalf
```
POST http://localhost:8080/api/v1/bookings
Auth: {{admin_token}}
Body: {
  "facilityId": "10000001-0000-0000-0000-000000000001",
  "bookingDate": "2026-04-23",
  "startTime": "11:00",
  "endTime": "12:00",
  "purpose": "Admin booking for student",
  "attendees": 80,
  "bookedForUserId": "00000001-0000-0000-0000-000000000003"
}
Expected: 201 Created
Save to {{booking_id_3}}
```

### 7.8: Verify bookedFor Recorded
```sql
SELECT id, requested_by, booked_for FROM booking WHERE id = '{{booking_id_3}}';
-- requested_by = admin UUID
-- booked_for = student UUID (different!)
```

### 7.9: Test Recurring Booking
```
POST http://localhost:8080/api/v1/bookings
Auth: {{lecturer_token}}
Body: {
  "facilityId": "10000001-0000-0000-0000-000000000001",
  "bookingDate": "2026-04-14",
  "startTime": "10:00",
  "endTime": "11:00",
  "purpose": "Weekly Lecture",
  "attendees": 100,
  "recurrenceRule": "FREQ=WEEKLY;UNTIL=2026-05-12;BYDAY=MO"
}
Expected: 201 Created
Save to {{booking_id_4}}
```

### 7.10: Verify Recurring Instances
```sql
-- If booking_instance table exists:
SELECT booking_id, booking_date, start_time, end_time FROM booking_instance
WHERE booking_id = '{{booking_id_4}}'
ORDER BY booking_date;

-- Should show 5 instances (Apr 14, 21, 28, May 5, 12)
-- All on Mondays
```

### Phase 7 Success Criteria
- ✅ Create simple booking (status PENDING)
- ✅ Overlap detection works (409)
- ✅ Peak-hour restriction enforced for student (403)
- ✅ Peak-hour allowed for lecturer (201)
- ✅ Capacity validation works (400)
- ✅ Admin booked-for user works
- ✅ Recurring bookings create instances
- ✅ Holidays skipped (if configured)

---

## PHASE 8: ANALYTICS

**Duration**: 1-2 hours  
**Tools**: Postman + PostgreSQL

### 8.1: Run Section 4 of sql_setup_scripts (Booking Data)
Create 30 days of historical booking data for analytics

### 8.2: Verify Booking Data
```sql
SELECT COUNT(*) FROM booking;
-- Should be >= 48

SELECT f.facility_code, COUNT(*) as bookings FROM booking b
JOIN facility f ON b.facility_id = f.id
GROUP BY f.facility_code;
```

### 8.3: Query Analytics (Admin)
```
GET http://localhost:8080/api/v1/analytics/utilization?from=2026-03-12&to=2026-04-11
Auth: {{admin_token}}
Expected: 200 OK with heatmap, underutilized, recommendations
```

### 8.4: Verify Response Structure
```json
{
  "heatmap": [ ... ],
  "underutilizedFacilities": [ ... ],
  "recommendations": [ ... ]
}
```

### 8.5: Analyze Heatmap
- Meeting Room should show low utilization (20% from test data)
- Lecture Hall should show high utilization (80%)

### 8.6: Check Underutilized Facilities
```
Response should include Meeting Room (if <30% average)
```

### 8.7: Test Non-Admin Access
```
GET http://localhost:8080/api/v1/analytics/utilization?from=2026-03-12&to=2026-04-11
Auth: {{student_token}}
Expected: 403 Forbidden "ADMIN role required"
```

### Phase 8 Success Criteria
- ✅ Analytics queryable with date range
- ✅ Heatmap generated correctly
- ✅ Underutilization detected
- ✅ Recommendations provided
- ✅ Admin-only access enforced

---

## PHASE 9: FRONTEND INTEGRATION

**Duration**: 1-2 hours  
**Tools**: Frontend + Browser

### 9.1: Open Frontend
```
http://localhost:5173
Should see: "Sign in with Google" button
```

### 9.2: Click Login
Select your Google account → authorize → redirect to callback

### 9.3: Check Dashboard
First page shows skeleton dashboard (features placeholder)

### 9.4: Navigate to Tickets
Click "Tickets" in navigation → should see: TicketDashboard component

### 9.5: Create Ticket via UI
- Fill form with ticket details
- Submit
- Verify ticket appears in list

### 9.6: Verify Notifications Work
- Notification icon shows count
- Click to open feed
- Notifications display

### Phase 9 Success Criteria
- ✅ OAuth login works
- ✅ Dashboard loads
- ✅ Ticket dashboard loads
- ✅ Create ticket via UI works
- ✅ Notifications display

---

## PHASE 10: END-TO-END SCENARIOS

### Scenario 1: Complete Ticket Lifecycle
```
1. Student creates CRITICAL ticket
2. Facility Manager assigns to Technician
3. Technician marks IN_PROGRESS
4. (Wait for SLA breach if testing escalation)
5. Technician marks RESOLVED
6. Manager marks CLOSED
7. Verify escalation history if applicable
8. Verify notifications sent to all parties
```

### Scenario 2: Booking + Approval + No-show
```
1. Student books Meeting Room
2. Lecturer approves (if required)
3. Admin approves → booking=APPROVED
4. (Past booking date) Mark as no-show
5. Repeat for 2 more no-shows
6. System auto-suspends user
7. Try to access protected endpoint → 403
8. Submit appeal
9. Admin approves → suspension lifted
10. User can access again
```

### Scenario 3: Admin Analytics Review
```
1. Admin opens analytics dashboard
2. Queries utilization for past 30 days
3. Sees underutilized Meeting Room (20% utilization)
4. Sees recommendation to consolidate
5. Admin manually cancels redundant future bookings
6. Re-queries analytics → improved metrics
```

---

## TROUBLESHOOTING QUICK REF

| Problem | Diagnosis | Solution |
|---------|-----------|----------|
| 401 on every endpoint | Invalid/expired token | Get new JWT from OAuth callback |
| 403 when user has role | Role not in user_roles table | Check user_roles junction table |
| 409 on booking creation | Overlapping bookings | Check bookings table for conflicts |
| File upload fails | Wrong MIME type | Check allowed-extensions in config |
| No notifications created | Observer not triggered | Check InAppObserver logs |
| SLA deadline NULL | Calculation failed | Verify TicketService sets SLA based on priority |
| Escalation not firing | Scheduled job disabled | Check @Scheduled is working |
| Comment visibility wrong | Role-filtering bug | Check ticket_comment.visibility in SELECT |

---

## FINAL VERIFICATION SCRIPT

Run this SQL after all 10 phases complete:

```sql
-- Summary of all test data
SELECT 
  'Users' as entity, COUNT(*) as count FROM "user"
UNION ALL
SELECT 'Facilities', COUNT(*) FROM facility
UNION ALL
SELECT 'Tickets', COUNT(*) FROM maintenance_ticket
UNION ALL
SELECT 'Bookings', COUNT(*) FROM booking
UNION ALL
SELECT 'Appeals', COUNT(*) FROM suspension_appeal
UNION ALL
SELECT 'Comments', COUNT(*) FROM ticket_comment
UNION ALL
SELECT 'Attachments', COUNT(*) FROM ticket_attachment
UNION ALL
SELECT 'Notifications', COUNT(*) FROM notification;
```

Expected output (approximate):
```
Users: 8
Facilities: 5
Tickets: 4+
Bookings: 48+
Appeals: 2+
Comments: 4+
Attachments: 2+
Notifications: 10+
```

**If all counts > 0: You've successfully tested the entire system!**
