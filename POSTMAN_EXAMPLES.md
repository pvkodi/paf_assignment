# Postman Request Examples - Copy/Paste Ready

## SETUP NOTES
- **Base URL**: http://localhost:8080/api/v1 (or /api for health)
- **Auth Header**: `Authorization: Bearer {jwt_token}` (copy from OAuth response)
- **Content-Type**: application/json
- Replace UUIDs in examples with actual IDs from your database

---

## PHASE 1: AUTHENTICATION

### 1.1 Health Check (NO AUTH)
```http
GET http://localhost:8080/api/health
```

### 1.2 OAuth Google Callback (SIMULATED)
```http
POST http://localhost:8080/api/v1/auth/oauth/google/callback
Content-Type: application/json

{
  "code": "4/0AWtgzdrYourActualGoogleCodeHere...",
  "redirectUri": "http://localhost:5173/auth/callback"
}
```
**Response** (keep this token for subsequent requests):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2026-04-12T10:30:45Z",
  "user": {
    "id": "uuid-here",
    "email": "student@campus.edu",
    "roles": ["STUDENT"],
    "suspended": false
  }
}
```

### 1.3 Get User Profile (WITH AUTH)
```http
GET http://localhost:8080/api/v1/auth/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 1.4 Logout
```http
POST http://localhost:8080/api/v1/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

---

## PHASE 4: TICKETS

### 4.1 Create Ticket (Student)
```http
POST http://localhost:8080/api/tickets
Authorization: Bearer {student_token}
Content-Type: application/json

{
  "facilityId": "10000001-0000-0000-0000-000000000001",
  "title": "Broken projector in Lecture Hall",
  "description": "The projector connection keeps disconnecting during lectures",
  "category": "ELECTRICAL",
  "priority": "MEDIUM",
  "attachmentCount": 0
}
```

### 4.2 List All Tickets (With Role Filtering)
```http
GET http://localhost:8080/api/tickets
Authorization: Bearer {user_token}
```
**Query Params**: ?status=OPEN&priority=HIGH

### 4.3 Get Ticket Detail
```http
GET http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {user_token}
```

### 4.4 Add Comment (Public)
```http
POST http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000/comments
Authorization: Bearer {student_token}
Content-Type: application/json

{
  "content": "I can confirm this happens during every class",
  "visibility": "PUBLIC"
}
```

### 4.5 Add Internal Comment (Technician)
```http
POST http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000/comments
Authorization: Bearer {technician_token}
Content-Type: application/json

{
  "content": "Need to check HDMI cable and replace if damaged. Cost estimate: $45",
  "visibility": "INTERNAL"
}
```

### 4.6 Upload Attachment
```http
POST http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000/attachments
Authorization: Bearer {student_token}
Content-Type: multipart/form-data

[Binary image file]
```

### 4.7 Get All Attachments for Ticket
```http
GET http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000/attachments
Authorization: Bearer {user_token}
```

### 4.8 Delete Attachment
```http
DELETE http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000/attachments/660f9500-f40c-52e5-b827-557766551111
Authorization: Bearer {user_token}
Content-Type: application/json
```

### 4.9 Assign Ticket (Facility Manager)
```http
POST http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000/assign
Authorization: Bearer {facility_manager_token}
Content-Type: application/json

{
  "assignToUserId": "00000001-0000-0000-0000-000000000005"
}
```

### 4.10 Update Ticket Status (Technician)
```http
PUT http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000/status
Authorization: Bearer {technician_token}
Content-Type: application/json

{
  "status": "IN_PROGRESS"
}
```

**Valid Transitions**:
- OPEN → IN_PROGRESS
- IN_PROGRESS → RESOLVED
- RESOLVED → CLOSED
- OPEN → REJECTED

### 4.11 Get Escalation History
```http
GET http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000/escalation-history
Authorization: Bearer {user_token}
```

### 4.12 Get Comments for Ticket
```http
GET http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000/comments
Authorization: Bearer {technician_token}
```
Note: Student sees only PUBLIC; Technician sees PUBLIC + INTERNAL

### 4.13 Update Own Comment
```http
PUT http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000/comments/770g0611-g51d-63f6-c938-668877662222
Authorization: Bearer {student_token}
Content-Type: application/json

{
  "content": "Updated comment text",
  "visibility": "PUBLIC"
}
```

### 4.14 Delete Own Comment
```http
DELETE http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000/comments/770g0611-g51d-63f6-c938-668877662222
Authorization: Bearer {student_token}
```

---

## PHASE 5: APPEALS

### 5.1 Submit Appeal (Suspended User)
```http
POST http://localhost:8080/api/v1/appeals
Authorization: Bearer {suspended_student_token}
Content-Type: application/json

{
  "reason": "I believe my three no-shows were due to medical emergency. I have doctor certificates to prove it."
}
```
**Response** (201 Created):
```json
{
  "id": "880h1722-h62e-74g7-d049-779988773333",
  "userId": "00000001-0000-0000-0000-000000000003",
  "reason": "I believe my three no-shows...",
  "status": "SUBMITTED",
  "createdAt": "2026-04-11T15:30:00Z"
}
```

### 5.2 List Appeals (Admin)
```http
GET http://localhost:8080/api/v1/appeals
Authorization: Bearer {admin_token}
```
Returns all SUBMITTED appeals

### 5.3 List Appeals (User)
```http
GET http://localhost:8080/api/v1/appeals
Authorization: Bearer {student_token}
```
Returns only own appeals

### 5.4 Get Appeal Details
```http
GET http://localhost:8080/api/v1/appeals/880h1722-h62e-74g7-d049-779988773333
Authorization: Bearer {admin_or_user_token}
```

### 5.5 Approve Appeal (Admin)
```http
POST http://localhost:8080/api/v1/appeals/880h1722-h62e-74g7-d049-779988773333/approve
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "decision": "Appeal approved. Medical documentation verified. User reinstated effective immediately."
}
```

### 5.6 Reject Appeal (Admin)
```http
POST http://localhost:8080/api/v1/appeals/880h1722-h62e-74g7-d049-779988773333/reject
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "decision": "Insufficient evidence provided. Appeal rejected. Suspension continues for 7 days."
}
```

---

## PHASE 6: NOTIFICATIONS

### 6.1 Get Notifications (Paginated)
```http
GET http://localhost:8080/api/v1/notifications?page=0&size=20
Authorization: Bearer {user_token}
```

### 6.2 Get Single Notification
```http
GET http://localhost:8080/api/v1/notifications/990i2833-i73f-85h8-e150-880099884444
Authorization: Bearer {user_token}
```

### 6.3 Mark Notification as Read
```http
POST http://localhost:8080/api/v1/notifications/990i2833-i73f-85h8-e150-880099884444/read
Authorization: Bearer {user_token}
Content-Type: application/json
```

### 6.4 Mark All as Read
```http
DELETE http://localhost:8080/api/v1/notifications
Authorization: Bearer {user_token}
Content-Type: application/json
```

### 6.5 Get Unread Count
```http
GET http://localhost:8080/api/v1/notifications/unread/count
Authorization: Bearer {user_token}
```
**Response**:
```json
{
  "unreadCount": 5
}
```

---

## PHASE 7: BOOKINGS

### 7.1 Create Simple Booking
```http
POST http://localhost:8080/api/v1/bookings
Authorization: Bearer {student_token}
Content-Type: application/json

{
  "facilityId": "10000001-0000-0000-0000-000000000003",
  "bookingDate": "2026-04-15",
  "startTime": "14:00",
  "endTime": "15:00",
  "purpose": "Project team meeting",
  "attendees": 8,
  "recurrenceRule": null,
  "bookedForUserId": null
}
```

### 7.2 Create Admin Booking on Behalf of Student
```http
POST http://localhost:8080/api/v1/bookings
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "facilityId": "10000001-0000-0000-0000-000000000001",
  "bookingDate": "2026-04-16",
  "startTime": "10:00",
  "endTime": "11:00",
  "purpose": "Admin-booked lecture make-up session",
  "attendees": 120,
  "recurrenceRule": null,
  "bookedForUserId": "00000001-0000-0000-0000-000000000003"
}
```

### 7.3 Create Recurring Weekly Booking
```http
POST http://localhost:8080/api/v1/bookings
Authorization: Bearer {lecturer_token}
Content-Type: application/json

{
  "facilityId": "10000001-0000-0000-0000-000000000001",
  "bookingDate": "2026-04-14",
  "startTime": "09:00",
  "endTime": "10:00",
  "purpose": "Weekly Algorithms lecture",
  "attendees": 100,
  "recurrenceRule": "FREQ=WEEKLY;UNTIL=2026-05-16;BYDAY=MO",
  "bookedForUserId": null
}
```

### 7.4 Test Peak-Hour Rejection (Student)
```http
POST http://localhost:8080/api/v1/bookings
Authorization: Bearer {student_token}
Content-Type: application/json

{
  "facilityId": "10000001-0000-0000-0000-000000000003",
  "bookingDate": "2026-04-15",
  "startTime": "09:00",
  "endTime": "10:00",
  "purpose": "Trying to book during restricted peak hours",
  "attendees": 5,
  "recurrenceRule": null
}
```
**Expected 403**: "Peak-hour restriction applies to your role during 08:00-10:00"

### 7.5 Test Capacity Validation
```http
POST http://localhost:8080/api/v1/bookings
Authorization: Bearer {student_token}
Content-Type: application/json

{
  "facilityId": "10000001-0000-0000-0000-000000000003",
  "bookingDate": "2026-04-15",
  "startTime": "14:00",
  "endTime": "15:00",
  "purpose": "Way too many people",
  "attendees": 500,
  "recurrenceRule": null
}
```
**Expected 400**: "Attendees (500) exceed facility capacity (20)"

### 7.6 Test Overlap Detection
```http
POST http://localhost:8080/api/v1/bookings
Authorization: Bearer {student_token}
Content-Type: application/json

{
  "facilityId": "10000001-0000-0000-0000-000000000003",
  "bookingDate": "2026-04-15",
  "startTime": "14:30",
  "endTime": "15:30",
  "purpose": "Overlapping with existing booking",
  "attendees": 5,
  "recurrenceRule": null
}
```
**Expected 409** (if booking from 7.1 is still pending): "Facility already booked for this time slot"

---

## PHASE 8: ANALYTICS

### 8.1 Get Utilization Analytics
```http
GET http://localhost:8080/api/v1/analytics/utilization?from=2026-03-12&to=2026-04-11
Authorization: Bearer {admin_token}
```

**Response**:
```json
{
  "heatmap": [
    {
      "facilityId": "10000001-0000-0000-0000-000000000001",
      "facilityName": "Main Lecture Hall",
      "dayOfWeek": "MONDAY",
      "hourOfDay": 8,
      "utilizationPercent": 85
    },
    {
      "facilityId": "10000001-0000-0000-0000-000000000003",
      "facilityName": "Meeting Room 1",
      "dayOfWeek": "MONDAY",
      "hourOfDay": 8,
      "utilizationPercent": 15
    }
  ],
  "underutilizedFacilities": [
    {
      "facilityId": "10000001-0000-0000-0000-000000000003",
      "facilityName": "Meeting Room 1",
      "utilizationPercent": 20,
      "consecutiveDays": 30,
      "recommendation": "Consider consolidating meeting bookings across fewer facilities"
    },
    {
      "facilityId": "10000001-0000-0000-0000-000000000004",
      "facilityName": "Main Auditorium",
      "utilizationPercent": 10,
      "consecutiveDays": 30,
      "recommendation": "Facility may be over-provisioned for current demand"
    }
  ],
  "recommendations": [
    {
      "requestedFacilityId": "10000001-0000-0000-0000-000000000003",
      "alternativeFacilityId": "10000001-0000-0000-0000-000000000001",
      "reason": "Main Lecture Hall has better historical utilization"
    }
  ]
}
```

### 8.2 Test Non-Admin Access (Should Fail)
```http
GET http://localhost:8080/api/v1/analytics/utilization?from=2026-03-12&to=2026-04-11
Authorization: Bearer {student_token}
```
**Expected 403**: "Insufficient permissions. ADMIN role required."

---

## ERROR RESPONSE EXAMPLES

### 401 Unauthorized
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired JWT token"
}
```

### 403 Forbidden (Role)
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "User is suspended. Access denied."
}
```

### 403 Forbidden (RBAC)
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient permissions for this operation"
}
```

### 404 Not Found
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Ticket not found with id: 550e8400-e29b-41d4-a716-446655440000"
}
```

### 409 Conflict (Concurrency)
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Facility already booked for 2026-04-15 14:00-15:00",
  "currentState": {
    "facilityId": "10000001-0000-0000-0000-000000000003",
    "bookedTime": "2026-04-15 14:00-14:30"
  }
}
```

### 422 Unprocessable Entity
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "User is not currently suspended. Cannot submit appeal."
}
```

### Validation Error (400)
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": {
    "attendees": "Must be between 1 and facility capacity"
  }
}
```
