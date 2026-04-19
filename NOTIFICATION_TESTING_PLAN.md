# Notification Feature Testing Plan

**Date Created**: April 15, 2026  
**Feature**: User Story 5 - Operational Notifications and Admin Analytics (Priority: P3)  
**Status**: All implementation tasks marked complete - Ready for validation testing

---

## Overview

This testing plan validates that the notification system correctly routes domain events to users via multiple channels (in-app + email) based on event severity, with proper observer pattern implementation and analytics integration.

### Key Requirements Being Tested

- **FR-034**: High-priority notifications via both in-app and email
- **FR-035**: Standard notifications in-app only
- **AR-007**: Observer pattern for notification fan-out
- **SC-007**: 100% of high-priority events deliver to both channels; 100% of standard events remain in-app only

---

## Test Execution Levels

### Level 1: Unit Tests ✓ (Already in place)

**File**: `backend/src/test/java/com/sliitreserve/api/unit/notification/NotificationServiceTest.java`

**Coverage**:

- [x] HIGH severity events route to both observers
- [x] STANDARD severity events route to InApp-only observer
- [x] Observer filtering via canHandle()
- [x] Event dispatch to matching observers
- [x] Multiple observer subscription and management
- [x] Error handling and observer isolation

**Command to run**:

```bash
cd backend/api
mvn test -Dtest=NotificationServiceTest
```

**Expected Result**: All tests PASS with 100% observer routing accuracy

---

### Level 2: Component Unit Tests ✓ (Already in place)

**File**: `backend/src/test/java/com/sliitreserve/api/unit/analytics/UtilizationServiceTest.java`

**Coverage**:

- [x] Utilization calculation accuracy
- [x] Underutilization detection (< 30% for 30 days, 7+ consecutive days)
- [x] Facility exclusion rules (under maintenance, < 50 hours)
- [x] Alternative facility recommendation logic

**Command to run**:

```bash
cd backend/api
mvn test -Dtest=UtilizationServiceTest
mvn test -Dtest=UtilizationCalculatorTest
```

**Expected Result**: All calculations match specification requirements

---

### Level 3: Contract Tests ✓ (Already in place)

**File**: `backend/src/test/java/com/sliitreserve/api/contract/analytics/AnalyticsContractTest.java`

**Coverage**:

- [x] GET /api/v1/notifications - Correct schema and filtering
- [x] POST /api/v1/notifications/{id}/read - Mark as read
- [x] GET /api/v1/notifications/unread/count - Unread counter
- [x] GET /api/v1/analytics/utilization - Analytics schema
- [x] Auth/RBAC validation on all endpoints

**Command to run**:

```bash
cd backend/api
mvn test -Dtest=AnalyticsContractTest
```

**Expected Result**: All endpoints return correct JSON schema, status codes, and respect role-based access

---

### Level 4: Integration Tests ✓ (Already in place)

**File**: `backend/src/test/java/com/sliitreserve/api/integration/analytics/NotificationAnalyticsIntegrationTest.java`

**Coverage**:

- [x] Event publishing through actual EventPublisher
- [x] InAppObserver persists to database
- [x] EmailObserver routes to SMTP adapter
- [x] UtilizationSnapshotService daily scheduler
- [x] Async dispatch without blocking publisher
- [x] Database transaction consistency

**Command to run**:

```bash
cd backend/api
mvn test -Dtest=NotificationAnalyticsIntegrationTest
```

**Expected Result**: Events flow through observers → database/email without errors

---

## Level 5: Manual End-to-End Testing (NEW - Required for Validation)

### 5.1 Scenario: Booking Approval → Standard Notification

**Setup**:

1. Ensure services running: `docker-compose -f infra/docker-compose.yml up -d`
2. Backend on port 8080, Frontend on port 5173

**Test Steps**:

```
1. Login as USER role (student account)
2. Search and submit booking request for a facility
3. Logout
4. Login as LECTURER role (approver)
5. Navigate to Approval Queue
6. Approve the booking
7. Logout
8. Login back as USER role
```

**Expected Results**:

- ✓ USER receives in-app notification "Booking Approved"
- ✓ No email is sent (STANDARD severity)
- ✓ Notification appears in notification feed with fresh timestamp
- ✓ Notification can be marked as read
- ✓ Check unread count decreases

**Postman Test**:

```bash
# As authenticated USER
GET /api/v1/notifications?page=0&size=20
# Expected: Contains BOOKING_APPROVED event with no email_sent flag
```

---

### 5.2 Scenario: No-Show → Suspension → High-Priority Notification

**Setup**:

- Ensure an existing booking in check-in phase

**Test Steps**:

```
1. Login as USER, navigate to facility booking check-in
2. Wait past booking start time without checking in (creates no-show)
3. Admin runs no-show processing job or manually triggers check
4. User receives 3rd no-show (triggers auto-suspension)
5. Login as ADMIN to verify suspension
6. Check user email (Mailtrap/test inbox)
```

**Expected Results**:

- ✓ In-app notification: "Account Suspended - Multiple No-Shows"
- ✓ Email received: "Account Suspension Notice" with appeal instructions
- ✓ User cannot submit new bookings
- ✓ Notification marked as HIGH severity (channels: IN_APP, EMAIL)

**Postman Test**:

```bash
# As authenticated USER
GET /api/v1/notifications?page=0&size=20
# Expected: Contains USER_SUSPENDED event with both IN_APP and EMAIL channels

# Check Mailtrap (Playground)
# Expected: Email subject line contains "Suspension"
```

---

### 5.3 Scenario: SLA Deadline Breach → Escalation → High-Priority Notification

**Setup**:

- Create a CRITICAL priority maintenance ticket
- Trigger scheduler or manually test SLA breach workflow

**Test Steps**:

```
1. Login as USER, create maintenance ticket with CRITICAL priority
2. Ticket enters standard assignment workflow
3. Simulate 4+ hours without response (CRITICAL SLA window)
4. Trigger escalation scheduler (or admin action)
5. Check notifications for staff and admin
```

**Expected Results**:

- ✓ TECHNICIAN receives: in-app + email notification "SLA Approaching - 1 hour remaining"
- ✓ FACILITY_MANAGER receives: in-app + email notification "SLA Breached - Ticket Escalated"
- ✓ ADMIN receives: in-app + email notification "Critical SLA Breach"
- ✓ Ticket auto-reassigned to senior technician
- ✓ Escalation level incremented (visible in ticket detail)

**Postman Test**:

```bash
# After SLA breach triggered
GET /api/v1/notifications?page=0&size=20
# Expected: Multiple HIGH severity events for different users
```

---

### 5.4 Scenario: Analytics & Underutilization Detection

**Setup**:

- Generate 30 days of booking data
- Run daily utilization snapshot jobs

**Test Steps**:

```
1. Login as ADMIN
2. Navigate to Analytics Dashboard
3. Select date range: Last 30 days
4. Observe heatmap (utilization by day/hour)
5. Identify underutilized facilities (< 30%)
6. Review alternative facility recommendations
```

**Expected Results**:

- ✓ Heatmap displays correctly with color coding (0-100%)
- ✓ Underutilized facilities flagged with reason
- ✓ Alternative recommendations include capacity-fit alternatives
- ✓ Data excludes facilities under maintenance
- ✓ Data excludes facilities with < 50 available hours

**Postman Test**:

```bash
# As ADMIN user
GET /api/v1/analytics/utilization?from=2026-03-15&to=2026-04-15
# Expected: JSON response with heatmap, underutilized list, recommendations
```

---

### 5.5 Scenario: Notification Center UI (Frontend)

**Setup**:

- Frontend running on http://localhost:5173

**Test Steps**:

```
1. Login as any role
2. Click notification bell icon (top-right)
3. Observe notification drawer/modal
4. Verify newest notifications appear first
5. Click "Mark as Read" on a notification
6. Verify it moves to "read" section
7. Check unread badge count updates
```

**Expected Results**:

- ✓ Notification list loads within 2 seconds
- ✓ Notifications display: title, description, timestamp, severity badge
- ✓ Clicking "Mark as Read" updates badge count immediately
- ✓ Sorting order: newest first
- ✓ Pagination works (if > 20 notifications)
- ✓ Differentiates IN_APP and EMAIL channels visually

---

### 5.6 Scenario: Email Delivery Validation

**Setup**:

- Mailtrap SMTP configured in `application.yaml` (T079)
- Mailtrap credentials set in environment variables

**Test Steps**:

```
1. Trigger a HIGH severity event (suspension or SLA breach)
2. Check Mailtrap Playground inbox within 30 seconds
3. Verify email contains:
   - Correct recipient (user email)
   - Event title in subject line
   - Event description in body
   - Action link (if applicable)
   - Clear call-to-action
4. Reply/archive in Mailtrap to verify integration
```

**Expected Results**:

- ✓ Email arrives within 5 seconds of event trigger
- ✓ Subject line matches event title
- ✓ Body includes formatted description
- ✓ Email From: configured domain (not spam)
- ✓ No HTML rendering errors
- ✓ Links are clickable and point to correct URLs

**Mailtrap Inbox Path**:

```
Mailtrap Dashboard → [Your Project] → Inbox → Emails
```

---

## Level 6: Performance & Load Testing (NEW - Recommended)

### 6.1 Performance Benchmark

**Objective**: Verify notifications don't block system operations

**Test**:

```bash
# Run load test during notification publishing
# Monitor API latency

# High-priority event triggering test
# 1000 simultaneous users receiving notifications
# Measure:
# - Event publish latency (should be < 100ms)
# - Database insert latency (< 50ms)
# - Email queue latency (< 200ms async)
```

**Expected Results**:

- ✓ Event publish completes in < 100ms (blocking call to service.publish())
- ✓ Observer dispatch is async (non-blocking)
- ✓ Database transactions don't deadlock under 1000 concurrent ops
- ✓ No memory leaks from observer queuing

**Command**:

```bash
cd backend/api
mvn test -Dtest=NotificationPerformanceTest -P performance
```

---

### 6.2 Analytics Query Performance

**Objective**: Utilization analytics complete within acceptable time

**Test**:

```
- Query 1: 30-day period with 1000+ facilities
- Expected: < 2 seconds
- Query 2: Full year with 500 bookings/day
- Expected: < 5 seconds
```

**Command**:

```bash
# Monitor query execution time
SELECT COUNT(*) as snapshot_count FROM utilization_snapshot
WHERE snapshot_date BETWEEN '2026-03-15' AND '2026-04-15';
```

---

## Level 7: Cross-Cutting Validation

### 7.1 Database Consistency Check

**Verify**:

```sql
-- All published HIGH severity events have both IN_APP and EMAIL channels
SELECT COUNT(*) as high_severity_count, severity FROM notification GROUP BY severity;
-- Expected: HIGH severity notifications have 2 rows per event (split by channel)

-- All STANDARD severity events have only IN_APP channel
SELECT DISTINCT channels FROM notification WHERE severity = 'STANDARD';
-- Expected: Only 'IN_APP' channel

-- No missing recipients for published events
SELECT n.* FROM notification n
LEFT JOIN user u ON n.recipient_user_id = u.id
WHERE u.id IS NULL;
-- Expected: 0 rows
```

---

### 7.2 Observer Pattern Validation

**Verify InAppObserver behavior**:

- [x] Subscribes to EventPublisher on startup
- [x] Receives all HIGH + STANDARD severity events
- [x] Persists to notification table with correct schema
- [x] Never throws exceptions on invalid events
- [x] Handles null affectedUserId gracefully

**Verify EmailObserver behavior**:

- [x] Subscribes to EventPublisher on startup
- [x] Receives HIGH severity events only (canHandle returns false for STANDARD)
- [x] Routes to SMTP adapter with Mailtrap config
- [x] Uses EmailTemplateFactory for formatting
- [x] Includes retry logic for transient failures

---

### 7.3 Authorization Checks

**Verify RBAC on notification endpoints**:

```bash
# As USER role
GET /api/v1/notifications
# Expected: 200 OK (own notifications only)

# As TECHNICIAN attempting to access USER's notifications
# Try: GET /api/v1/notifications?userId=<other-user-id>
# Expected: 403 FORBIDDEN or empty result

# As non-ADMIN attempting analytics
GET /api/v1/analytics/utilization
# Expected: 403 FORBIDDEN
```

---

## Level 8: Edge Cases & Error Handling

### 8.1 Test Invalid Events

**Case 1**: Missing required fields

```java
EventEnvelope event = EventEnvelope.builder()
    .eventType("TEST_EVENT")
    // Missing: severity, title, affectedUserId
    .build();
notificationService.publish(event);
// Expected: IllegalArgumentException thrown
```

**Case 2**: Null severity

```java
EventEnvelope event = EventEnvelope.builder()
    .eventId(UUID.randomUUID().toString())
    .eventType("TEST_EVENT")
    .severity(null)  // Invalid
    .title("Test")
    .build();
// Expected: Rejected with validation error
```

**Case 3**: Unknown severity

```java
EventEnvelope event = createValidEvent();
event.setSeverity("UNKNOWN_SEVERITY");  // Invalid enum
// Expected: Rejected or mapped to STANDARD
```

---

### 8.2 Test Database Failures

**Case 1**: Database connection lost during notification save

- Expected: Logged error, event continues processing, email still attempts
- Verify: Observer exception doesn't crash publisher

**Case 2**: Duplicate event ID

- Expected: Idempotent handling (second publish ignored or deduplicated)
- Verify: No duplicate rows created

---

### 8.3 Test Async Dispatch

**Case 1**: Multiple rapid notifications (1000 in 1 second)

- Expected: All queued and processed
- Verify: No loss of events

**Case 2**: Observer takes > 10 seconds to process

- Expected: Publisher returns immediately (async)
- Verify: Other observers not blocked

---

## Test Data Generation Script

Create test events for manual validation:

```bash
#!/bin/bash

# High Priority Event (CRITICAL SLA BREACH)
curl -X POST http://localhost:8080/api/v1/admin/test/event \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "SLA_DEADLINE_BREACHED",
    "severity": "HIGH",
    "title": "Critical SLA Breached",
    "description": "Ticket #T001 has exceeded critical SLA deadline",
    "affectedUserId": 1,
    "actionUrl": "/tickets/1",
    "actionLabel": "View Ticket"
  }'

# Standard Event (BOOKING APPROVED)
curl -X POST http://localhost:8080/api/v1/admin/test/event \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "BOOKING_APPROVED",
    "severity": "STANDARD",
    "title": "Your Booking is Approved",
    "description": "Meeting Room A booking for tomorrow 2 PM",
    "affectedUserId": 1,
    "actionUrl": "/bookings/1"
  }'

# Suspension Event (HIGH)
curl -X POST http://localhost:8080/api/v1/admin/test/event \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "USER_SUSPENDED",
    "severity": "HIGH",
    "title": "Account Suspended",
    "description": "Your account has been suspended due to 3 no-shows",
    "affectedUserId": 1,
    "actionUrl": "/appeals/submit",
    "actionLabel": "File Appeal"
  }'
```

---

## Defect Checklist

When testing, look for these common issues:

### Backend Issues

- [ ] Events not reaching observers
- [ ] Observer exceptions not caught
- [ ] Email not sent (SMTP config issue)
- [ ] Database constraint violations
- [ ] Async dispatch blocking
- [ ] Duplicate notifications created
- [ ] Incorrect timezone in notifications
- [ ] Missing or null required fields

### Frontend Issues

- [ ] Notification bell not updating
- [ ] Notification list not loading
- [ ] Mark as read not working
- [ ] Notifications overlapping with other UI
- [ ] Email channel indicator missing
- [ ] Timestamp formatting incorrect
- [ ] Pagination broken

---

## Success Criteria

✓ **Test Coverage**: All tests in Levels 1-4 PASS  
✓ **E2E Validation**: All Level 5 scenarios complete successfully  
✓ **Performance**: Level 6 benchmarks within acceptable limits  
✓ **Zero Blockers**: No CRITICAL defects found  
✓ **Specification Compliance**: SC-007 (100% multi-channel routing) verified

---

## Recommended Test Execution Order

1. **Quick Validation (~10 minutes)**
   - Run all unit tests: `mvn test -Dtest=Notification*`
   - Run contract tests: `mvn test -Dtest=AnalyticsContract*`
   - Result: Confirm no regressions

2. **Basic E2E (~30 minutes)**
   - Scenario 5.1: Booking approval
   - Scenario 5.2: No-show suspension
   - Scenario 5.5: Notification UI

3. **Full Validation (~60 minutes)**
   - All Level 5 scenarios
   - Email delivery check (Mailtrap)
   - Utilization analytics accuracy

4. **Performance & Edge Cases (~30 minutes)**
   - Level 6 tests
   - Level 8 edge cases
   - Database consistency checks

---

## Notes

- **Mailtrap Setup**: Verify credentials in `application.yaml` (MailConfig.java) before testing email scenarios
- **Scheduler**: Daily utilization snapshot runs at configurable time (check UtilizationSnapshotService.java)
- **Async Processing**: Observable delays (~500ms) may occur in async observer dispatch; this is expected
- **Timezone**: All timestamps use Asia/Colombo; verify local timezone conversion before interpreting results

---

## Sign-Off

| Phase             | Tester | Date | Status        | Notes                               |
| ----------------- | ------ | ---- | ------------- | ----------------------------------- |
| Unit Tests        | -      | -    | □ Not Started | Complete T073-T074                  |
| Contract Tests    | -      | -    | □ Not Started | Complete T075                       |
| Integration Tests | -      | -    | □ Not Started | Complete T076                       |
| E2E Scenarios     | -      | -    | □ Not Started | Pair with developer                 |
| Performance       | -      | -    | □ Not Started | If load testing resources available |

---

**Last Updated**: April 15, 2026  
**Next Review**: After first E2E test cycle completion
