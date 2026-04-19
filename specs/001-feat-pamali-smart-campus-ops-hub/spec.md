# Feature Specification: VenueLink Operations Hub

**Feature Branch**: `001-feat-pamali-smart-campus-ops-hub`  
**Created**: 2026-03-23  
**Status**: Draft  
**Input**: User description: "A university VenueLink Operations Hub with modules for authentication, facilities, bookings, approvals, quotas, maintenance tickets, SLA escalation, notifications, analytics, and local uploads."

## Clarifications

### Session 2026-03-23

- Q: For SLA deadlines (CRITICAL 4h, HIGH 8h, MEDIUM 24h, LOW 72h), how should time be counted? → A: 24x7 elapsed time (includes nights, weekends, holidays).
- Q: When a user is suspended, what access should still be allowed? → A: Only auth/session, profile view, and suspension appeal submission are allowed; all other protected actions are blocked.
- Q: For optimistic-locking/concurrency conflicts on bookings, what should the API do? → A: Fail immediately with `409 Conflict` and return current booking/version details.
- Q: Which timezone rule should govern booking times, peak-hour restrictions, no-show checks, and SLA calculations? → A: Campus local timezone with DST support.
- Q: If a person has multiple roles, which policy should be applied for quota limits, peak-hour restriction, and max advance booking window? → A: Most permissive role policy applies.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Secure Access and Role Governance (Priority: P1)

As a campus platform user, I can sign in with institutional OAuth, receive an authenticated session token, and access only the actions my role permits. Suspended accounts are blocked from protected actions.

**Why this priority**: All other modules depend on authenticated identity, role checks, and suspension enforcement.

**Independent Test**: Can be fully tested by signing in with users from each role, attempting authorized and unauthorized endpoints, and verifying suspended users are denied all protected operations.

**Acceptance Scenarios**:

1. **Given** a valid institutional account, **When** the user signs in, **Then** the system authenticates successfully and grants role-appropriate access.
2. **Given** a suspended user account, **When** the user attempts any protected operation, **Then** the system blocks access and returns a clear suspension reason.
3. **Given** a user without required role permission, **When** the user calls a restricted endpoint, **Then** the system denies access with a clear authorization error.

---

### User Story 2 - Facility Discovery and Policy-Compliant Booking (Priority: P1)

As a user, I can discover facilities and submit booking requests that respect capacity, overlap prevention, quota limits, peak-hour restrictions, and advance-booking limits. As an admin, I can book on behalf of another user.

**Why this priority**: Booking and utilization are the central business value of the platform.

**Independent Test**: Can be fully tested by creating a facility catalog, searching/filtering facilities, submitting valid and invalid bookings, and verifying booking state transitions and recurrence behavior.

**Acceptance Scenarios**:

1. **Given** available facilities, **When** a user filters by type, capacity, and location metadata, **Then** matching resources are returned with availability and status.
2. **Given** an existing booking for a resource, **When** another user attempts an overlapping booking, **Then** the request is rejected due to concurrency-safe conflict handling.
3. **Given** a recurring booking crossing a public holiday, **When** recurrence is created, **Then** holiday occurrences are skipped and the requester is notified.
4. **Given** a regular user booking during restricted peak hours, **When** the request is submitted, **Then** the system rejects the booking with a quota-policy explanation.
5. **Given** an admin creating a booking for another user, **When** the booking is submitted, **Then** the booking records both creator and booked-for user.

---

### User Story 3 - Approval, Quota Enforcement, and Suspension Lifecycle (Priority: P2)

As a platform approver or administrator, I can process booking approvals by role and facility constraints, enforce no-show policies, apply suspensions after repeated no-shows, and process suspension appeals.

**Why this priority**: Governance controls are required for fairness, compliance, and operational integrity after booking is available.

**Independent Test**: Can be fully tested by submitting booking requests from different roles and capacities, running approval paths, logging no-shows via check-in outcomes, and validating suspension + appeal flows.

**Acceptance Scenarios**:

1. **Given** a booking request from a standard user, **When** the request enters workflow, **Then** lecturer approval is required before administrator approval.
2. **Given** a booking request for a hall above capacity threshold, **When** base approvals are complete, **Then** additional facility-manager sign-off is required.
3. **Given** a user with three no-shows, **When** the no-show threshold is reached, **Then** a one-week suspension is automatically applied.
4. **Given** a suspended user appeal, **When** an administrator reviews and accepts the appeal, **Then** suspension is lifted and the user is notified.

---

### User Story 4 - Maintenance Ticketing with SLA Escalation (Priority: P2)

As a user, I can raise maintenance tickets with attachments and comments; as staff, I can triage, assign, and resolve tickets through controlled status transitions and SLA-based escalation levels.

**Why this priority**: Facilities operations require issue management and guaranteed response discipline.

**Independent Test**: Can be fully tested by creating tickets in each priority tier, attaching supported files, assigning technicians, updating statuses, and observing escalation actions at each SLA level.

**Acceptance Scenarios**:

1. **Given** a ticket with valid image attachments, **When** it is submitted, **Then** attachments are accepted within count/type/size limits and thumbnails are generated.
2. **Given** an SLA deadline breach, **When** escalation triggers, **Then** reassignment and stakeholder notifications occur according to escalation level.
3. **Given** a non-staff user, **When** viewing internal comments, **Then** internal comments are hidden while public comments remain visible.
4. **Given** a comment author, **When** editing or deleting own comment, **Then** the action succeeds; **and** admin can delete any comment.

---

### User Story 5 - Operational Notifications and Admin Analytics (Priority: P3)

As an admin, I can monitor utilization health and underutilization trends; as a user/staff member, I receive timely in-app and email notifications for high-priority and routine events.

**Why this priority**: Analytics and notification quality improve operations but depend on core transactions being in place.

**Independent Test**: Can be fully tested by generating booking and maintenance events, verifying channel/routing behavior, and running utilization calculations over a defined period.

**Acceptance Scenarios**:

1. **Given** high-priority events such as escalations or suspensions, **When** they occur, **Then** users receive both in-app and email notifications.
2. **Given** standard events such as booking approval and reminders, **When** they occur, **Then** users receive in-app notifications only.
3. **Given** 30 days of booking and availability data, **When** utilization analytics runs, **Then** utilization, underutilized assets, and alternative facility suggestions are produced per rules.

### Edge Cases

- Booking request attendee count equals facility capacity.
- Booking request attendee count exceeds capacity by 1.
- Two concurrent booking submissions target the same resource and time window.
- Recurring booking series includes multiple public holidays and partial-week gaps.
- Booking cancellation occurs after approval but before start time.
- Check-in happens exactly at 15 minutes from start time.
- User reaches no-show threshold during an already pending booking request.
- Facility status changes to out-of-service while approved bookings still exist.
- Ticket receives 4th attachment attempt.
- Attachment mimics image extension but has mismatched MIME type.
- Escalation target role has no currently available assignee.
- Facility has less than 50 available hours in analysis period and should be excluded.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST authenticate users via Google OAuth and issue a session JWT for authorized use of protected endpoints.
- **FR-002**: System MUST assign and enforce one or more roles per user from `USER`, `LECTURER`, `TECHNICIAN`, `FACILITY_MANAGER`, `ADMIN`.
- **FR-003**: System MUST block suspended users from all protected operations except auth/session, profile view, and suspension appeal submission, and MUST provide a clear suspension message.
- **FR-004**: System MUST maintain a searchable facility catalog including type, capacity, location, building, floor, availability windows, and operational status.
- **FR-005**: System MUST support facility subtypes with subtype-specific attributes for lecture halls, labs, meeting rooms, auditoriums, equipment, and sports facilities.
- **FR-006**: System MUST allow search/filter by facility type, minimum capacity, location, and building.
- **FR-007**: System MUST allow users to request bookings with date, time range, purpose, and attendee count.
- **FR-008**: System MUST reject booking requests where attendees exceed facility capacity.
- **FR-009**: System MUST prevent overlapping bookings for the same resource and time range under concurrent submissions, and MUST fail immediately on optimistic-lock conflicts with `409 Conflict` including current booking/version details.
- **FR-010**: System MUST support recurring bookings and skip occurrences on configured public holidays.
- **FR-011**: System MUST notify requesters when recurring occurrences are skipped.
- **FR-012**: System MUST support admin-created bookings on behalf of another user and record booked-for identity.
- **FR-013**: System MUST enforce maximum advance booking windows of 3 months for `USER`/`LECTURER` and 6 months for `ADMIN`.
- **FR-014**: System MUST enforce booking approval workflow based on requester role and facility rules.
- **FR-015**: System MUST require lecturer approval then admin approval for `USER` booking requests.
- **FR-016**: System MUST auto-approve bookings initiated by `LECTURER` unless additional rule-based sign-off is required.
- **FR-017**: System MUST require additional `FACILITY_MANAGER` sign-off for halls above configured high-capacity threshold.
- **FR-018**: System MUST enforce quota policy limiting `USER` bookings to 3 per week and 10 per month.
- **FR-019**: System MUST enforce peak-hour restriction preventing `USER` bookings from 08:00 to 10:00 in campus local timezone.
- **FR-020**: System MUST support check-in via QR code and manual staff check-in.
- **FR-021**: System MUST classify no-show when check-in does not occur within 15 minutes of booking start in campus local timezone.
- **FR-022**: System MUST apply an automatic 1-week suspension after 3 no-shows.
- **FR-023**: System MUST allow suspended users to submit appeals and allow `ADMIN` users to approve or reject appeals.
- **FR-024**: System MUST allow users to raise maintenance tickets linked to facilities with category, description, and priority.
- **FR-025**: System MUST support ticket status transitions `OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED` and controlled transition to `REJECTED`.
- **FR-026**: System MUST allow up to 3 image attachments per ticket and reject additional attachments.
- **FR-027**: System MUST accept ticket attachments only for JPEG, PNG, GIF, and WebP up to 5 MB each.
- **FR-028**: System MUST generate a 200x200 thumbnail for each accepted ticket image attachment.
- **FR-029**: System MUST allow admin assignment/reassignment of technicians for tickets.
- **FR-030**: System MUST support ticket comments where authors can edit/delete own comments and admins can delete any comment.
- **FR-031**: System MUST restrict internal comments visibility to staff roles only.
- **FR-032**: System MUST enforce SLA deadlines by priority using 24x7 elapsed time (including nights, weekends, and holidays) in campus local timezone with DST support: critical 4h, high 8h, medium 24h, low 72h.
- **FR-041**: System MUST use campus local timezone with DST support as the canonical timezone for booking times, recurrence evaluation, no-show checks, peak-hour restrictions, reminders, and SLA calculations.
- **FR-042**: For users with multiple assigned roles, booking-policy evaluation for max advance booking window, quota limits, and peak-hour restrictions MUST apply the most permissive eligible role policy.
- **FR-033**: System MUST execute level-based escalation actions including reassignment and stakeholder notifications.
- **FR-034**: System MUST send high-priority notifications via both in-app and email for defined event types.
- **FR-035**: System MUST send standard notifications in-app only for defined event types.
- **FR-036**: System MUST provide admin-only utilization analytics using booked-hours to available-hours percentage.
- **FR-037**: System MUST exclude facilities under maintenance and those with less than 50 available hours in the analysis window.
- **FR-038**: System MUST mark facilities underutilized when average utilization is below 30% over 30 days and flag if persistent for more than 7 consecutive days.
- **FR-039**: System MUST suggest alternative facilities for requested time slots based on availability and capacity fit.
- **FR-040**: System MUST store uploaded files on local filesystem under `/uploads` only.

### Architecture and API Contract Requirements *(mandatory)*

- **AR-001**: Backend request handling MUST follow strict `Controller -> Service -> Repository` layering with no layer skipping.
- **AR-002**: API boundaries MUST use request/response DTOs only; entities MUST NOT be returned directly.
- **AR-003**: Facility creation behavior MUST use a Factory pattern.
- **AR-004**: Quota evaluation behavior MUST use a Strategy pattern.
- **AR-005**: Approval and escalation routing MUST use a Chain of Responsibility pattern.
- **AR-006**: Ticket status transition behavior MUST use a State pattern.
- **AR-007**: Notification fan-out behavior MUST use an Observer pattern.
- **AR-008**: Booking object construction MUST use a Builder pattern.
- **AR-009**: Data persistence access MUST use the Repository pattern.

### Security, Validation, and Error Requirements *(mandatory)*

- **SV-001**: Every endpoint MUST define permitted roles in an access matrix and enforce authorization checks.
- **SV-002**: Every request DTO MUST define validation constraints and trigger validation before business processing.
- **SV-003**: API responses MUST return meaningful structured errors with consistent naming and status semantics.
- **SV-004**: Image upload handling MUST validate MIME type, enforce size limits, and sanitize filenames before persistence.

### Key Entities *(include if feature involves data)*

- **User**: Platform identity with role memberships, suspension state, no-show counters, and appeal history.
- **Facility**: Bookable resource with common metadata (type, capacity, location, availability windows, status) and subtype-specific attributes.
- **Booking**: Reservation request and lifecycle record with requester, booked-for user, time range, purpose, attendees, status, and recurrence metadata.
- **ApprovalStep**: Workflow approval node with approver role, decision, sequence order, and decision timestamp.
- **QuotaPolicyRecord**: Period-based booking counters, peak-hour compliance flags, and enforcement outcomes.
- **CheckInRecord**: Attendance evidence for each booking occurrence including method, timestamp, and no-show derivation.
- **Suspension**: Enforcement record including trigger reason, start/end time, and appeal status.
- **Ticket**: Maintenance issue record linked to facility, category, priority, status, assigned technician, and SLA deadlines.
- **TicketComment**: Comment thread item with author, visibility type (public/internal), edit history, and deletion marker.
- **Attachment**: Ticket image metadata including original filename, sanitized filename, MIME type, size, and thumbnail path.
- **EscalationEvent**: SLA breach escalation record with level, reassignment target, notifications sent, and incident-report indicator.
- **Notification**: Event-driven message record with severity, channels, recipients, and delivery status.
- **UtilizationSnapshot**: Time-window aggregate per facility for available hours, booked hours, utilization percentage, and underutilization flags.

### Role Access Matrix *(mandatory for API features)*

| Endpoint Group | USER | LECTURER | TECHNICIAN | FACILITY_MANAGER | ADMIN |
| --- | --- | --- | --- | --- | --- |
| Auth session and profile | Allow | Allow | Allow | Allow | Allow |
| Facility browse/search | Allow | Allow | Allow | Allow | Allow |
| Submit own booking | Allow | Allow | Deny | Deny | Allow |
| Book on behalf of user | Deny | Deny | Deny | Deny | Allow |
| Approve user bookings | Deny | Allow | Deny | Conditional (high-capacity halls) | Allow |
| Quota and suspension appeals | Submit appeal | Submit appeal | Deny | Deny | Review/decide |
| Create maintenance ticket | Allow | Allow | Allow | Allow | Allow |
| Assign/reassign technician | Deny | Deny | Deny | Deny | Allow |
| Internal ticket comments | Deny | Allow | Allow | Allow | Allow |
| View admin analytics | Deny | Deny | Deny | Deny | Allow |

## Assumptions

- Public holidays are managed in a configurable institutional calendar available to booking logic and evaluated in campus local timezone.
- Peak-hour restriction applies only to `USER` role bookings, not to lecturer or admin bookings.
- "Next available technician" and "senior technician" are determined by operational availability attributes maintained in staff profiles.
- Emergency maintenance is treated as a high-priority notification class.
- Alternative facility suggestions prioritize same facility type first, then nearest capacity and location fit.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 95% of successful sign-ins complete within 60 seconds from login initiation to authenticated landing state.
- **SC-002**: 100% of protected endpoints reject unauthorized roles in access-control test suites.
- **SC-003**: 100% of booking submissions that violate capacity, overlap, quota, peak-hour, or advance-window rules are rejected with explicit reasons.
- **SC-004**: 99% of recurring booking series correctly skip public-holiday occurrences and generate skip notifications.
- **SC-005**: 100% of tickets with unsupported file type, oversized file, or over-limit attachment count are rejected before storage.
- **SC-006**: 95% of SLA breaches trigger the correct escalation level actions within 5 minutes of breach detection.
- **SC-007**: 100% of high-priority events deliver notifications to both required channels, and 100% of standard events remain in-app only.
- **SC-008**: Admin analytics output for utilization and underutilization matches expected calculation results with at least 99% accuracy against validation datasets.
