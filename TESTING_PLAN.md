# Comprehensive Testing & Integration Plan for Smart Campus Operations Hub

## PHASE 0: PREREQUISITES & ENVIRONMENT SETUP

### What to Verify:

1. PostgreSQL running on localhost:5432
2. Backend compiled and running on localhost:8080
3. Frontend running on localhost:5173
4. Database migrations applied (Flyway)
5. Google OAuth credentials configured
6. CORS allowed for frontend origin

### Tools Needed:

- Postman (for API testing)
- Frontend browser
- PostgreSQL client (psql or DBeaver) for manual data inspection
- VS Code Terminal for error logs

---

## PHASE 1: HEALTH & AUTHENTICATION (FOUNDATION - NO DEPENDENCIES)

### Goal: Establish secure identity system

### Sprint Testing Order:

1. System Health Check
2. OAuth Google Login
3. User Profile Management
4. Logout
5. Suspension Status Tracking

### What Gets Tested:

- Public health endpoint works
- GoogleOAuth callback exchange (code → JWT)
- JWT token extracted and stored
- User profile contains: id, email, roles, suspended status
- Suspended users can still view profile
- Token expiration works
- Logout clears session

### Manual Data Setup Required: NONE (OAuth creates users dynamically)

### Success Criteria:

✅ Get /api/health returns 200
✅ Post /api/v1/auth/oauth/google/callback returns JWT with 24h expiration
✅ Get /api/v1/auth/profile with Bearer token returns user data
✅ Suspended user can still call /profile and /logout

---

## PHASE 2: MASTER DATA SETUP (FACILITIES & USERS)

### Goal: Create testable facilities and user roles

### Dependencies: Phase 1 (auth working)

### Manual Database Setup Required:

Database script to run ONCE:

```sql
-- Create test users with different roles
INSERT INTO "user" (id, google_subject, email, display_name, active, suspended_until, no_show_count)
VALUES
  ('00000001-0000-0000-0000-000000000001', 'sub_admin', 'admin@campus.edu', 'Admin User', true, NULL, 0),
  ('00000001-0000-0000-0000-000000000002', 'sub_lecturer', 'lecturer@campus.edu', 'Lecturer User', true, NULL, 0),
  ('00000001-0000-0000-0000-000000000003', 'sub_student', 'student@campus.edu', 'Student User', true, NULL, 0),
  ('00000001-0000-0000-0000-000000000004', 'sub_fac_mgr', 'facmgr@campus.edu', 'Facility Manager', true, NULL, 0),
  ('00000001-0000-0000-0000-000000000005', 'sub_tech', 'technician@campus.edu', 'Technician', true, NULL, 0),
  ('00000001-0000-0000-0000-000000000006', 'sub_student2', 'student2@campus.edu', 'Student 2', true, NULL, 0);

-- Assign roles
INSERT INTO user_roles (user_id, role) VALUES
  ('00000001-0000-0000-0000-000000000001', 'ADMIN'),
  ('00000001-0000-0000-0000-000000000002', 'LECTURER'),
  ('00000001-0000-0000-0000-000000000003', 'STUDENT'),
  ('00000001-0000-0000-0000-000000000004', 'FACILITY_MANAGER'),
  ('00000001-0000-0000-0000-000000000005', 'MAINTENANCE_STAFF'),
  ('00000001-0000-0000-0000-000000000006', 'STUDENT');

-- Create test facilities
INSERT INTO facility (id, facility_code, name, type, capacity, location, building, floor, status, availability_start, availability_end)
VALUES
  ('10000001-0000-0000-0000-000000000001', 'LH-101', 'Main Lecture Hall', 'LECTURE_HALL', 150, 'Building A', 'A', '1', 'ACTIVE', '08:00', '20:00'),
  ('10000001-0000-0000-0000-000000000002', 'LAB-B2', 'Computer Lab B', 'LAB', 50, 'Building B', 'B', '2', 'ACTIVE', '08:00', '18:00'),
  ('10000001-0000-0000-0000-000000000003', 'MR-101', 'Meeting Room 1', 'MEETING_ROOM', 20, 'Building C', 'C', '1', 'ACTIVE', '08:00', '18:00'),
  ('10000001-0000-0000-0000-000000000004', 'aud-main', 'Main Auditorium', 'AUDITORIUM', 500, 'Building D', 'D', '1', 'ACTIVE', '08:00', '22:00');
```

### What Gets Tested:

- Verify users have correct roles
- Verify facilities are searchable
- Each user can authenticate and have correct role set

---

## PHASE 3: ROLE-BASED ACCESS CONTROL (RBAC)

### Goal: Verify authorization layer works

### Dependencies: Phase 2 (test users created)

### Test Cases (Postman):

1. **Student tries admin endpoint** → 403 Forbidden
2. **Lecturer tries ticket assignment** → 403 Forbidden
3. **Facility manager can view tickets** → 200 OK
4. **Admin can do everything** → 200 OK
5. **Suspended user tries protected action** → 403 Suspended error

### Success Criteria:

✅ Each role can ONLY perform authorized actions
✅ Invalid tokens return 401
✅ Expired tokens return 401
✅ Suspended users blocked from all protected actions

---

## PHASE 4: TICKET LIFECYCLE (CORE OPERATIONAL FLOW)

### Goal: Validate ticket creation, assignment, status updates, comments

### Dependencies: Phase 2 (users & facilities) + Phase 3 (RBAC)

### Why Start with Tickets?

- Tickets are **independent of bookings**
- No approval workflow delays
- Tests core features: CRUD, status transitions, comments, attachments, escalation

### Testing Order:

#### 4.1: Create Ticket (Student Role)

- **Test Data**: facility_id from Phase 2, priority = LOW
- **Endpoint**: POST /api/tickets
- **Expected**: 201 Created with ticket id
- **Verify in DB**: ticket_status = OPEN, sla_deadline set correctly
- **SLA Rules to Verify**:
  - LOW: 72 hours from now
  - MEDIUM: 24 hours
  - HIGH: 8 hours
  - CRITICAL: 4 hours

#### 4.2: Attach File to Ticket

- **Test Data**: jpg/png image file
- **Endpoint**: POST /api/tickets/{ticketId}/attachments
- **Expected**: 201 Created, thumbnail generated
- **Verify in DB & File System**:
  - uploads/original/{id}.jpg exists
  - uploads/thumbnails/{id}-thumb.jpg exists (200x200px)
  - attachment metadata in ticket_attachments table
- **Test Edge Cases**:
  - Try uploading .exe → 400 Bad Request
  - Try uploading >5MB → 413 Payload Too Large
  - Try uploading same file twice → 201 (allow duplicates)

#### 4.3: Add Comments (Student & Technician)

- **Test Data**: 2 comments, mix of PUBLIC and INTERNAL
- **Endpoint**: POST /api/tickets/{ticketId}/comments
- **Expected**: 201 Created
- **Verify**:
  - Student can add PUBLIC comment only
  - Student sees only PUBLIC comments when listing
  - Technician can add INTERNAL comment
  - Technician sees both PUBLIC + INTERNAL when listing

#### 4.4: Update Ticket Status (Technician)

- **Transitions to Test**:
  - OPEN → IN_PROGRESS ✅
  - IN_PROGRESS → RESOLVED ✅
  - RESOLVED → CLOSED ✅
  - Try OPEN → CLOSED directly → 400 Bad Request (invalid transition)
- **Endpoint**: PUT /api/tickets/{ticketId}/status
- **Expected**: 200 OK for valid, error for invalid

#### 4.5: Assign Ticket (Facility Manager)

- **Test Data**: MAINTENANCE_STAFF user
- **Endpoint**: POST /api/tickets/{ticketId}/assign
- **Expected**: 200 OK, assigned_to set in DB
- **Verify**:
  - Student cannot assign → 403 Forbidden
  - Only FACILITY_MANAGER & ADMIN can assign

#### 4.6: View Escalation History

- **Manual Setup**:
  - Create CRITICAL ticket with short SLA (already passed)
  - Update ticket status manually to OPEN (stays open past SLA)
  - Trigger Flyway escalation job/manually call escalation service
- **Endpoint**: GET /api/tickets/{ticketId}/escalation-history
- **Expected**: Array of escalation events with timestamps and actions

#### 4.7: Delete Comment (Owner & Admin)

- **Test Data**: Comment by student
- **Endpoint**: DELETE /api/tickets/{ticketId}/comments/{commentId}
- **Expected**:
  - Student deletes own comment → 200 OK
  - Different student tries to delete → 403 Forbidden
  - Admin can delete any comment → 200 OK

### Success Criteria:

✅ Full ticket lifecycle: CREATE → ASSIGN → COMMENT → ATTACH → STATUS → CLOSE
✅ RBAC enforced: students can create, technicians can work, managers can manage
✅ Comment visibility: PUBLIC vs INTERNAL works
✅ File attachments: uploaded, thumbnails generated, deletable
✅ Escalation tracked when SLA breached

---

## PHASE 5: APPEALS & SUSPENSION WORKFLOW

### Goal: Validate suspension logic and appeal mechanism

### Dependencies: Phase 2 (users) + Phase 4 (tickets working)

### Why Tickets First?

Appeals rely on manual suspension triggering (which we'll simulate). Get tickets working first.

### Testing Order:

#### 5.1: Create Suspended User (Manual DB)

```sql
UPDATE "user" SET suspended_until = NOW() + INTERVAL '7 days', no_show_count = 3
WHERE id = '00000001-0000-0000-0000-000000000003' (student);
```

#### 5.2: Suspended User Can ONLY Access Auth Endpoints

- **Endpoint**: GET /api/v1/auth/profile
- **Expected**: 200 OK (allowed whitelist)
- **Endpoint**: POST /api/v1/auth/logout
- **Expected**: 200 OK (allowed whitelist)
- **Endpoint**: POST /api/tickets (try to create)
- **Expected**: 403 Forbidden "User is suspended"

#### 5.3: Submit Appeal

- **Test Data**: suspension reason = "Unfair no-show classification"
- **Endpoint**: POST /api/v1/appeals
- **Expected**: 201 Created with appeal status = SUBMITTED
- **Verify in DB**: appeal.user_id set, appeal.status = SUBMITTED
- **Error Scenarios**:
  - Non-suspended user submits appeal → 422 Unprocessable Entity
  - User with pending appeal tries again → 409 Conflict

#### 5.4: List Appeals (Admin vs User)

- **Admin Endpoint**: GET /api/v1/appeals? (as ADMIN)
- **Expected**: All SUBMITTED appeals
- **User Endpoint**: GET /api/v1/appeals? (as student)
- **Expected**: Only own appeals

#### 5.5: Approve Appeal (Admin)

- **Test Data**: Appeal id from 5.3
- **Endpoint**: POST /api/v1/appeals/{id}/approve
- **Request**: { approved: true, decision: "User has been reinstated" }
- **Expected**: 200 OK, appeal.status = APPROVED
- **Verify in DB**: user.suspended_until = NULL

#### 5.6: Reject Appeal (Admin)

- **Create another suspended user/appeal first**
- **Endpoint**: POST /api/v1/appeals/{id}/reject
- **Request**: { approved: false, decision: "Cannot reinstate at this time" }
- **Expected**: 200 OK, appeal.status = REJECTED
- **Verify**: user.suspended_until still in future

### Success Criteria:

✅ Suspended users can only access auth/profile/logout
✅ Suspended users can submit appeals
✅ Admins can list all pending appeals
✅ Admins can approve (suspension lifted) or reject appeals
✅ User status correctly updated in DB

---

## PHASE 6: NOTIFICATIONS

### Goal: Validate notification delivery and read status

### Dependencies: Phase 4 (tickets generate events)

### Testing Order:

#### 6.1: Create Notification (via Ticket Update)

- **Setup**: Create ticket, add comment
- **System should auto-create**: IN_APP notifications for all interested parties
- **Manual Check**: Query DB `SELECT * FROM notification`

#### 6.2: Get Notification Feed (Paginated)

- **Endpoint**: GET /api/v1/notifications?page=0&size=20
- **Expected**: 200 OK with paged list, sorted by newest first
- **Verify**:
  - Each notification has: id, type, severity, message, isRead, createdAt
  - Pagination works (page=1 returns different set)

#### 6.3: Mark Single Notification as Read

- **Endpoint**: POST /api/v1/notifications/{id}/read
- **Expected**: 200 OK, notification.isRead = true
- **Verify**: Doesn't affect other notifications

#### 6.4: Mark All as Read

- **Endpoint**: DELETE /api/v1/notifications
- **Expected**: 200 OK
- **Verify DB**: All notifications for this user have isRead = true

#### 6.5: Get Unread Count

- **Endpoint**: GET /api/v1/notifications/unread/count
- **Expected**: 200 OK with count: integer
- **Test**:
  - Create notifications
  - Check count = N
  - Mark one as read
  - Check count = N-1

### Success Criteria:

✅ All ticket/appeal events generate notifications
✅ Notifications paginated and sorted correctly
✅ Read status tracked and queryable
✅ Unread count accurate

---

## PHASE 7: BOOKINGS (WITH DEPENDENCIES)

### Goal: Validate booking creation with complex validations

### Dependencies: Phase 2 (facilities) + Phase 3 (RBAC)

### Why Bookings Last?

- Require facilities (Phase 2)
- Require role-based approval workflows
- Require quota/policy validations
- More complex than tickets

### Booking Approval Flow:

1. Student submits booking
2. Lecturer approves
3. Admin approves
4. Booking = APPROVED
   (If facility capacity > threshold, facility manager must also approve)

### Testing Order:

#### 7.1: Simple Booking (Non-Peak, Non-Large Capacity)

**Facility**: Meeting Room (capacity 20) from Phase 2
**User**: Student

- **Endpoint**: POST /api/v1/bookings
- **Request**:

```json
{
  "facilityId": "10000001-0000-0000-0000-000000000003",
  "bookingDate": "2026-04-15",
  "startTime": "14:00",
  "endTime": "15:00",
  "purpose": "Project meeting",
  "attendees": 10,
  "recurrenceRule": null
}
```

- **Expected**: 201 Created with booking_status = PENDING
- **Verify in DB**:
  - booking.requested_by = student id
  - booking.booked_for = student id (same)
  - booking.approval_steps contains entry for LECTURER

#### 7.2: Test Overlap Prevention

- **Setup**: Booking from 7.1 pending approval
- **Try to book**: Same facility, same date, startTime 14:30-15:30
- **Expected**: 409 Conflict "Facility already booked"

#### 7.3: Test Peak-Hour Restriction

**Peak Hours**: 08:00-10:00 (from config)

- **Booking**: Meeting Room during 09:00-10:00
- **Expected for Student**: 403 Forbidden "Peak hour restriction"
- **Booking from Lecturer**: 09:00-10:00
- **Expected for Lecturer**: 201 Created (lecturer exempt from peak-hour)

#### 7.4: Test Capacity Validation

- **Booking Lecture Hall** (capacity 150) with attendees=200
- **Expected**: 400 Bad Request "Attendees exceed facility capacity"
- **Booking with attendees=150**: 201 Created

#### 7.5: Admin Book on Behalf of Student

- **User**: ADMIN
- **Endpoint**: POST /api/v1/bookings
- **Request**:

```json
{
  "facilityId": "10000001-0000-0000-0000-000000000001",
  "bookedForUserId": "00000001-0000-0000-0000-000000000003", // student
  "bookingDate": "2026-04-16",
  "startTime": "10:00",
  "endTime": "11:00",
  "purpose": "Admin booking for student",
  "attendees": 50
}
```

- **Expected**: 201 Created with booked_for = student id, requested_by = admin id
- **Verify DB**: Both IDs recorded separately

#### 7.6: Recurring Booking (Skipping Holidays)

- **Setup**: Create holiday record in DB for 2026-04-18
- **Endpoint**: POST /api/v1/bookings
- **Request**:

```json
{
  "facilityId": "10000001-0000-0000-0000-000000000001",
  "bookingDate": "2026-04-15",
  "startTime": "10:00",
  "endTime": "11:00",
  "purpose": "Weekly lecture",
  "attendees": 80,
  "recurrenceRule": "FREQ=WEEKLY;UNTIL=2026-05-01"
}
```

- **Expected**: 201 Created, but verify in DB:
  - booking_instances: 3 records (Apr 15, 22, 29 - skipped Apr 18)
  - notification sent to user: "Holiday on 2026-04-18 skipped"

#### 7.7: Approval Workflow (Lecturer → Admin)

- **Setup**: Booking from 7.1 (PENDING)
- **Login as Lecturer**:
  - **Endpoint**: GET /api/v1/bookings/{id}/approval-steps
  - **Expected**: Array with LECTURER status = PENDING, ADMIN = NOT_YET_REQUIRED
  - **Approve**: PUT /api/v1/bookings/{id}/approval-steps/LECTURER
  - **Request**: { decision: "APPROVED" }
  - **Expected**: 200 OK, step.status = APPROVED
- **Login as Admin**:
  - **Approve**: PUT /api/v1/bookings/{id}/approval-steps/ADMIN
  - **Expected**: 200 OK
  - **Verify**: booking.status = APPROVED (all approvals complete)

#### 7.8: Large Facility Requires Manager Approval

- **Facility**: Auditorium (capacity 500) - threshold for manager sign-off likely 200+
- **Booking Auditorium with attendees 200**
- **Expected approval steps**: LECTURER → ADMIN → FACILITY_MANAGER (3-step flow)

#### 7.9: No-Show Check-In & Suspension Trigger

- **Setup**: Create 3 approved bookings from same student, past start time
- **Manual Setup**: Mark 2 as checked-out (no-show), 1 as attended
- **Verify**:
  - Student no_show_count = 2
  - Create 3rd no-show
  - Student no_show_count = 3
  - User.suspended_until set (auto-suspension triggered)

### Success Criteria:

✅ Basic booking creation with validation
✅ Overlap detection works
✅ Peak-hour restrictions enforced per role
✅ Capacity validated
✅ Admin booking on behalf works
✅ Recurring bookings created and holidays skipped
✅ Approval workflows multi-step
✅ No-show tracking and auto-suspension

---

## PHASE 8: ANALYTICS

### Goal: Validate utilization metrics and insights

### Dependencies: Phase 7 (bookings with data)

### Why Last?

Analytics depends on having booking history to analyze.

### Testing Order:

#### 8.1: Generate Utilization Data (Manual Booking Setup)

- **Create 30 days of bookings**:
  - Lecture Hall: 80% utilization
  - Meeting Room: 20% utilization (underutilized)
  - Lab: 60% utilization
  - Auditorium: 10% utilization (underutilized >7 days)

#### 8.2: Run Daily Snapshot Job

- Trigger UtilizationSnapshotService for each day
- **Verify DB**: utilization_snapshot records created

#### 8.3: Query Analytics Endpoint

- **Endpoint**: GET /api/v1/analytics/utilization?from=2026-03-12&to=2026-04-11
- **Expected**: 200 OK with:

```json
{
  "heatmap": [
    { "facilityId": "...", "facilityName": "Meeting Room", "dayOfWeek": "MONDAY", "hourOfDay": 8, "utilizationPercent": 15 },
    ...
  ],
  "underutilizedFacilities": [
    { "facilityId": "...", "utilizationPercent": 20, "consecutiveDays": 8, "recommendation": "Consider consolidating bookings" },
    { "facilityId": "...", "utilizationPercent": 10, "consecutiveDays": 30, "recommendation": "Facility may be redundant" }
  ],
  "recommendations": [
    { "requestedFacilityId": "...", "alternativeFacilityId": "...", "reason": "Better utilization fit" }
  ]
}
```

#### 8.4: Underutilization Thresholds

- **Verify Logic**:
  - Meeting Room 20% < 30% threshold → marked underutilized
  - Check consecutive_days = 30+ → persistent underutilization
  - Auditorium 10% < 30% for 7+ days → recommend alternative

#### 8.5: Admin-Only Access

- **Login as Student**: GET /api/v1/analytics/utilization
- **Expected**: 403 Forbidden ADMIN required
- **Login as Admin**: GET /api/v1/analytics/utilization
- **Expected**: 200 OK

### Success Criteria:

✅ Heatmap generated with facility × day × hour grid
✅ Underutilization detected correctly
✅ Alternative recommendations provided
✅ Only admin access allowed

---

## PHASE 9: FRONTEND INTEGRATION TESTING

### Goal: Verify frontend calls work correctly with all backend endpoints

### Testing Order:

#### 9.1: Login Flow

- Open http://localhost:5173
- Click "Sign in with Google"
- Redirected to Google
- Returns with JWT stored in browser
- Frontend displays dashboard

#### 9.2: Ticket Dashboard

- Navigate to /tickets
- List of created tickets displayed
- Add comment form works
- Upload attachment works
- Status dropdown updates ticket

#### 9.3: Ticket Detail View

- Click ticket from list
- Detail page loaded with full ticket info
- Comments section shows (filters public by role)
- Attachments display and downloadable
- Escalation history visible (if applicable)

#### 9.4: Notification Center

- Notification icon shows unread count
- Click opens dropdown/modal
- Notifications listed with newest first
- Click mark as read → count decreases
- Paginate through older notifications

---

## PHASE 10: END-TO-END INTEGRATION HARDENING

### Goal: Test realistic user workflows from start to finish

### E2E Scenario 1: Student Books Room → Gets Approved → No-show → Appeal

1. Student logs in
2. Student searches facilities
3. Student creates booking for meeting room
4. Lecturer approves booking
5. Admin approves booking
6. Booking shows as APPROVED
7. Student marks as no-show (3 times across different bookings)
8. System auto-suspends user
9. Suspended user cannot access protected endpoints
10. Suspended user submits appeal
11. Admin reviews appeal in queue
12. Admin approves → user reinstated
13. User can access system again

### E2E Scenario 2: Maintenance Issue → Escalation → Resolution

1. User creates CRITICAL ticket
2. Facility manager triages and assigns to technician
3. Technician works on (status IN_PROGRESS)
4. SLA deadline approaches → escalation triggers at 2h remaining
5. Manager notified, reassigns to senior technician
6. Senior technician resolves (status RESOLVED)
7. Manager closes (status CLOSED)
8. Escalation history shows 2 escalation events

### E2E Scenario 3: Admin Analyzes Utilization & Acts

1. Admin logs in
2. Admin queries analytics for Mar 12 - Apr 11
3. Sees heatmap: Meeting Room underutilized
4. Sees recommendations: Consolidate meeting room bookings
5. Admin cancels redundant future bookings
6. Re-runs analytics → improved utilization

---

## TROUBLESHOOTING CHECKLIST

| Issue                                     | Diagnosis                                 | Fix                                                                |
| ----------------------------------------- | ----------------------------------------- | ------------------------------------------------------------------ |
| 401 Unauthorized on protected endpoint    | Token expired or invalid format           | Ensure Bearer token in header: `Authorization: Bearer {jwt}`       |
| 403 Forbidden when user has role          | Role not in database user_roles table     | Check user_roles junction table has role entry                     |
| 409 Conflict on booking creation          | Overlapping bookings or concurrency issue | Check bookings table for overlaps in date/time/facility            |
| Notification not created on ticket update | InAppObserver not triggered               | Verify TicketService publishes events after status update          |
| Escalation not triggered                  | Scheduled job not running                 | Check Springboot @Scheduled tasks are enabled and job runs         |
| Attachment not uploading                  | File type/size validation                 | Check `allowed-extensions` and `max-file-size` in application.yaml |
| SLA deadline field NULL                   | SLA calculation not running               | Verify SLA set in TicketService.createTicket() based on priority   |

---

## TESTING TOOLS SETUP

### Postman Collection Structure:

```
├── Authorization
│   ├── POST /oauth/google/callback
│   ├── GET /auth/profile
│   └── POST /auth/logout
├── Tickets
│   ├── POST /tickets [CREATE]
│   ├── GET /tickets [LIST]
│   ├── GET /tickets/{id} [DETAIL]
│   ├── PUT /tickets/{ticketId}/status [UPDATE]
│   ├── Comments (4 endpoints)
│   ├── Attachments (3 endpoints)
│   └── Escalation History
├── Appeals
│   ├── POST /appeals
│   ├── GET /appeals
│   ├── GET /appeals/{id}
│   ├── POST /appeals/{id}/approve
│   └── POST /appeals/{id}/reject
├── Notifications
│   ├── GET /notifications
│   ├── POST /notifications/{id}/read
│   ├── DELETE /notifications
│   └── GET /notifications/unread/count
├── Bookings
│   ├── POST /bookings [CREATE]
│   ├── GET /bookings [LIST]
│   └── Approval workflows
└── Analytics
    └── GET /analytics/utilization
```

---

## SUMMARY TABLE: DEPENDENCIES & ORDER

| Phase | Component     | Depends On | Est. Time | Must-Haves                             |
| ----- | ------------- | ---------- | --------- | -------------------------------------- |
| 0     | Env Setup     | None       | 30min     | DB, Backend, Frontend, Creds           |
| 1     | Auth          | None       | 1h        | OAuth working, JWT valid               |
| 2     | Master Data   | Phase 1    | 30min     | Test users, facilities in DB           |
| 3     | RBAC          | Phase 2    | 1h        | 403 errors on forbidden access         |
| 4     | Tickets       | Phase 2+3  | 4h        | Full CRUD, comments, files, escalation |
| 5     | Appeals       | Phase 4    | 2h        | Suspension, appeal flow                |
| 6     | Notifications | Phase 4+5  | 2h        | Events trigger notifications           |
| 7     | Bookings      | Phase 2+3  | 6h        | Complex validations & approvals        |
| 8     | Analytics     | Phase 7    | 3h        | Heatmap, underutilization              |
| 9     | Frontend      | All        | 3h        | UI integration                         |
| 10    | E2E           | All        | 2h        | Real workflows                         |

**Total: ~25 hours of testing**
