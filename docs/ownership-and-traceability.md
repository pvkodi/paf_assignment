# Module Ownership and Feature Traceability

**Last Updated**: April 2026  
**Document Purpose**: Establish clear module/endpoint ownership, ensure verifiable individual contributions, and maintain feature-to-code traceability across the Smart Campus Operations Hub project.

---

## 1. Module Ownership Map

This section defines primary ownership for each major module. Primary owners are responsible for maintaining module contracts, reviewing cross-module PRs affecting their module, and owning test coverage for their domain.

### Member 1: Facilities & Smart Utilization Optimization Engine

**Module Scope**:
- Facility discovery and search capabilities
- Facility metadata management and hierarchies
- Utilization analytics and snapshots
- Underutilization detection and recommendations
- Alternative facility suggestions

**Owned Packages**:
- `backend/src/main/java/com/sliitreserve/api/services/facility/`
- `backend/src/main/java/com/sliitreserve/api/services/analytics/`
- `backend/src/main/java/com/sliitreserve/api/controllers/FacilityController.java`
- `backend/src/main/java/com/sliitreserve/api/controllers/AnalyticsController.java`
- `frontend/src/features/facilities/`
- `frontend/src/features/analytics/`

**Owned Tasks**: T001, T003, T006, T010, T014, T018, T026, T034, T035, T039, T040, T041, T042, T047, T052, T059, T074, T076, T080, T081, T083, T084, T089, T091, T098

---

### Member 2: Booking & Fair-Usage Policy Engine

**Module Scope**:
- Booking creation and lifecycle
- Booking validation (capacity, overlap, recurrence)
- Quota policy engine and rule evaluation
- Fair-usage enforcement per role
- Advance window and peak-hour restrictions

**Owned Packages**:
- `backend/src/main/java/com/sliitreserve/api/services/booking/BookingService.java`
- `backend/src/main/java/com/sliitreserve/api/services/booking/QuotaPolicyEngine.java`
- `backend/src/main/java/com/sliitreserve/api/controllers/BookingController.java`
- `backend/src/main/java/com/sliitreserve/api/entities/booking/`
- `backend/src/main/java/com/sliitreserve/api/strategy/quota/`
- `backend/src/main/java/com/sliitreserve/api/util/booking/BookingBuilder.java`
- `frontend/src/features/bookings/`

**Owned Tasks**: T002, T004, T011, T015, T020, T022, T023, T025, T036, T038, T043, T044, T045, T046, T048, T053, T054, T057, T062, T064, T075, T085, T094, T099

---

### Member 3: Maintenance Tickets & Intelligent Escalation System

**Module Scope**:
- Ticket creation, state machine, and lifecycle
- Ticket attachments (upload, validation, thumbnailing)
- Comment management and visibility rules
- Ticket-to-staff assignment
- Escalation chain and routing logic
- SLA deadline tracking and violations

**Owned Packages**:
- `backend/src/main/java/com/sliitreserve/api/entities/ticket/`
- `backend/src/main/java/com/sliitreserve/api/state/TicketStateMachine.java`
- `backend/src/main/java/com/sliitreserve/api/services/ticket/`
- `backend/src/main/java/com/sliitreserve/api/workflow/escalation/`
- `backend/src/main/java/com/sliitreserve/api/controllers/TicketController.java`
- `frontend/src/features/tickets/`

**Owned Tasks**: T005, T007, T012, T017, T021, T027, T028, T050, T058, T060, T061, T065, T066, T067, T068, T069, T070, T071, T072, T086, T088, T090, T092, T095, T097

---

### Member 4: Authentication & Dynamic Approval + Notifications

**Module Scope**:
- OAuth2/Google authentication and token exchange
- JWT token generation and validation
- User role and claim management
- Suspended-user policy enforcement
- Booking approval workflow chain
- Approval routing and orchestration
- In-app and email notifications
- Event observer pattern implementation

**Owned Packages**:
- `backend/src/main/java/com/sliitreserve/api/config/SecurityConfig.java`
- `backend/src/main/java/com/sliitreserve/api/config/security/`
- `backend/src/main/java/com/sliitreserve/api/entities/auth/`
- `backend/src/main/java/com/sliitreserve/api/services/auth/`
- `backend/src/main/java/com/sliitreserve/api/services/notification/`
- `backend/src/main/java/com/sliitreserve/api/workflow/approval/`
- `backend/src/main/java/com/sliitreserve/api/controllers/AuthController.java`
- `backend/src/main/java/com/sliitreserve/api/controllers/AppealController.java`
- `backend/src/main/java/com/sliitreserve/api/controllers/NotificationController.java`
- `backend/src/main/java/com/sliitreserve/api/observers/impl/`
- `frontend/src/features/auth/`

**Owned Tasks**: T008, T009, T013, T016, T019, T024, T029, T030, T031, T032, T033, T037, T049, T051, T055, T056, T063, T073, T077, T078, T079, T082, T087, T093, T096

---

## 2. Shared Infrastructure & Cross-Module Ownership

These components are foundational and shared across all modules. Primary ownership applies for maintenance and evolution.

| Component | File Path | Primary Owner | Type |
|-----------|-----------|---------------|------|
| Database Migrations | `backend/src/main/resources/db/migration/` | Member 2 | Shared Infrastructure |
| DTOs & Mappers | `backend/src/main/java/com/sliitreserve/api/dto/` | Member 3 | Shared Infrastructure |
| Entities Base | `backend/src/main/java/com/sliitreserve/api/entities/` | Member 2 | Shared Infrastructure |
| Repositories | `backend/src/main/java/com/sliitreserve/api/repositories/` | Member 2 | Shared Infrastructure |
| Global Exception Handler | `backend/src/main/java/com/sliitreserve/api/controller/advice/GlobalExceptionHandler.java` | Member 4 | Shared Infrastructure |
| Validation Config | `backend/src/main/java/com/sliitreserve/api/config/ValidationConfig.java` | Member 2 | Shared Infrastructure |
| File Storage | `backend/src/main/java/com/sliitreserve/api/services/storage/` | Member 3 | Shared Infrastructure |
| Time Policy | `backend/src/main/java/com/sliitreserve/api/config/TimePolicyConfig.java` | Member 1 | Shared Infrastructure |
| Observer Framework | `backend/src/main/java/com/sliitreserve/api/observer/` | Member 4 | Shared Infrastructure |
| Frontend App Shell | `frontend/src/app/` | Member 1 | Shared Infrastructure |
| Frontend Routes | `frontend/src/routes/` | Member 1 | Shared Infrastructure |

---

## 3. Endpoint Ownership & API Contract

Each REST endpoint is owned by a single module owner. The following table documents the endpoint ownership matrix:

### Authentication Endpoints (Member 4)

| Endpoint | Method | Purpose | Owner | Test Type |
|----------|--------|---------|-------|-----------|
| `/api/auth/oauth-login` | POST | OAuth2 callback handler | Member 4 | Contract |
| `/api/auth/profile` | GET | Retrieve logged-in user profile | Member 4 | Contract |
| `/api/auth/logout` | POST | Invalidate session | Member 4 | Contract |
| `/api/auth/suspend-appeal` | POST | Submit user suspension appeal | Member 4 | Contract |
| `/api/auth/appeals` | GET | List pending appeals (admin) | Member 4 | Contract |

### Facility Endpoints (Member 1)

| Endpoint | Method | Purpose | Owner | Test Type |
|----------|--------|---------|-------|-----------|
| `/api/facilities` | GET | Search and filter facilities | Member 1 | Contract |
| `/api/facilities/{id}` | GET | Get single facility details | Member 1 | Contract |

### Booking Endpoints (Member 2)

| Endpoint | Method | Purpose | Owner | Test Type |
|----------|--------|---------|-------|-----------|
| `/api/bookings` | POST | Create booking request | Member 2 | Contract |
| `/api/bookings` | GET | List user bookings | Member 2 | Contract |
| `/api/bookings/{id}` | GET | Get booking details | Member 2 | Contract |
| `/api/bookings/{id}/check-in` | POST | Check in to confirmed booking | Member 2 | Contract |
| `/api/bookings/{id}/cancel` | POST | Cancel booking | Member 2 | Contract |
| `/api/bookings/{id}/approve` | POST | Approve booking (admin/staff) | Member 2 | Contract |
| `/api/bookings/{id}/reject` | POST | Reject booking (admin/staff) | Member 2 | Contract |

### Ticket Endpoints (Member 3)

| Endpoint | Method | Purpose | Owner | Test Type |
|----------|--------|---------|-------|-----------|
| `/api/tickets` | POST | Create maintenance ticket | Member 3 | Contract |
| `/api/tickets` | GET | List tickets (filtered by role) | Member 3 | Contract |
| `/api/tickets/{id}` | GET | Get ticket details | Member 3 | Contract |
| `/api/tickets/{id}/comment` | POST | Add comment to ticket | Member 3 | Contract |
| `/api/tickets/{id}/assign` | POST | Assign ticket to staff | Member 3 | Contract |
| `/api/tickets/{id}/escalate` | POST | Escalate ticket | Member 3 | Contract |
| `/api/tickets/{id}/attach` | POST | Upload attachment to ticket | Member 3 | Contract |

### Analytics Endpoints (Member 1)

| Endpoint | Method | Purpose | Owner | Test Type |
|----------|--------|---------|-------|-----------|
| `/api/analytics/utilization` | GET | Get facility utilization metrics | Member 1 | Contract |
| `/api/analytics/recommendations` | GET | Get underutilization recommendations | Member 1 | Contract |

### Notification Endpoints (Member 4)

| Endpoint | Method | Purpose | Owner | Test Type |
|----------|--------|---------|-------|-----------|
| `/api/notifications` | GET | List in-app notifications | Member 4 | Contract |
| `/api/notifications/{id}/read` | POST | Mark notification as read | Member 4 | Contract |

---

## 4. Feature Traceability Rules

Feature traceability connects requirements, design decisions, code, tests, and Pull Requests. Every feature must be traceable through this chain.

### 4.1 Requirement-to-Code Traceability

Every task in `tasks.md` **must** include:

1. **Spec Reference**: Link to section in `spec.md` that motivates the task
2. **File Path**: Explicit Java/JS file path where implementation lives
3. **Entity/Service**: Primary class or service implementation
4. **Test Coverage**: Unit, contract, or integration test class path
5. **PR Evidence**: Link to merged PR or branch demonstrating completion

**Example**:
```
T045 - Booking Service with overlap/capacity validation
├─ Spec Ref: spec.md § 3.2 Booking Lifecycle
├─ Implementation: backend/src/main/java/com/sliitreserve/api/services/booking/BookingService.java
├─ Unit Test: backend/src/test/java/com/sliitreserve/api/unit/booking/BookingServiceTest.java
├─ Integration Test: backend/src/test/java/com/sliitreserve/api/integration/booking/BookingConcurrencyIntegrationTest.java
├─ PR: #42 (see docs/api/endpoint-contributions.md for full links)
└─ Status: COMPLETE
```

### 4.2 Test Coverage Expectations by Task Type

| Task Type | Unit Tests | Contract Tests | Integration Tests | Frontend Tests |
|-----------|-----------|-----------------|-------------------|-----------------|
| Entity/Model | Yes | - | - | - |
| Service Logic | Yes (mandatory) | Yes | Yes (if async/DB) | - |
| Controller/Endpoint | Yes | Yes | Yes | If applicable |
| Frontend Component | - | - | - | Yes |
| Configuration | Code review | - | - | - |

### 4.3 Git Commitand PR Traceability

Every task **must** result in a PR with:

1. **Branch name**: `feature/member-N-module-name` (e.g., `feature/member-1-facility-analytics`)
2. **PR title**: Include task ID (e.g., `T045: Implement booking service with overlap validation`)
3. **PR description**: Must include:
   - Task ID and summary
   - Link to spec.md section
   - Files modified (Java/JS paths)
   - Test coverage evidence (test class names and line counts)
   - Any breaking changes or database migrations
4. **Required Reviewers**: Minimum 1 from dependent modules (see § 5 Cross-Module Dependencies)
5. **CI Status**: All checks must pass (build, unit tests, integration tests, linting)

### 4.4 Coverage Matrix Maintenance

A coverage matrix must be maintained in `specs/001-feat-pamali-smart-campus-ops-hub/coverage-matrix.md` listing:

- Functional Requirements (FRs) → Tasks → Test Classes
- Architectural Requirements (ARs) → Tasks → Config/Infrastructure files
- Service-Level Contracts (SLs) → Tasks → Integration tests

---

## 5. Cross-Module Dependencies & Contract Checkpoints

Modules interact at well-defined boundaries. Freeze these contracts to enable safe parallel development:

### Contract 1: Authentication & Role Claims (Member 4 → All Others)

**Freeze Point**: After T033 (Endpoint Role Annotations)

**Contract**:
- JWT claims include: `userId`, `email`, `roles[]`, `suspension_status`
- Roles: `STUDENT`, `LECTURER`, `ADMIN`, `STAFF`
- Suspension status enum: `ACTIVE`, `SUSPENDED_TEMP`, `SUSPENDED_PERM`

**Dependent Modules**: All (consume role claims for RBAC)

**Validation Test**: `backend/src/test/java/com/sliitreserve/api/integration/auth/RbacIntegrationTest.java`

---

### Contract 2: Booking Events (Member 2 → Member 4, Member 1, Member 3)

**Freeze Point**: After T046 (Booking Controller) & T055 (Approval Chain)

**Contract**:
- Event types: `BOOKING_CREATED`, `BOOKING_APPROVED`, `BOOKING_REJECTED`, `BOOKING_CANCELLED`, `BOOKING_CHECKED_IN`, `BOOKING_NO_SHOW`
- Event payload includes: `bookingId`, `userId`, `facilityId`, `timestamp`, `eventType`
- Published to observer callbacks synth in `NotificationService` (Member 4) and `UtilizationSnapshotService` (Member 1)

**Dependent Modules**: Notifications (Member 4), Analytics (Member 1), Escalation (Member 3)

**Validation Test**: `backend/src/test/java/com/sliitreserve/api/integration/booking/BookingEventIntegrationTest.java`

---

### Contract 3: Ticket Escalation Events (Member 3 → Member 4)

**Freeze Point**: After T070 (SLA Scheduler) & T069 (Escalation Chain)

**Contract**:
- Event types: `TICKET_ESCALATED`, `TICKET_SLA_VIOLATION`, `TICKET_ASSIGNED`
- Event payload includes: `ticketId`, `escalationLevel`, `previousAssignee`, `newAssignee`, `timestamp`
- Consumed by `NotificationService` to send escalation alerts

**Dependent Modules**: Notifications (Member 4)

**Validation Test**: `backend/src/test/java/com/sliitreserve/api/integration/ticket/TicketEscalationIntegrationTest.java`

---

### Contract 4: Quota Policy Rules (Member 2 → Member 2 Approvals)

**Freeze Point**: After T054 (QuotaPolicyEngine)

**Contract**:
- Role quota policies defined in `backend/src/main/java/com/sliitreserve/api/strategy/quota/`
- Policy evaluation returns: `allowed`, `reason`, `quota_remaining`
- Enforced in both booking creation (T045) and approval workflow (T055)

**Dependent Modules**: Booking (self), Approval (self)

**Validation Test**: `backend/src/test/java/com/sliitreserve/api/unit/quota/QuotaPolicyEngineTest.java`

---

## 6. Ownership Rules & Conflict Resolution

### 6.1 Primary Ownership Responsibilities

1. **Module Maintenance**: Owner maintains code quality, consistency, and contracts within their module.
2. **Test Coverage**: Owner ensures service unit tests and contract tests exist and pass.
3. **Cross-Module Integration**: Owner participates in contract negotiation and validates dependent modules against contract.
4. **PR Review**: Owner must review and approve all PRs that modify their module.

### 6.2 Cross-Module Change Rules

**Rule 1**: If a task touches two owned modules, split into two subtasks with separate PRs and sequentially merge.

**Rule 2**: PRs touching a module require minimum 1 approval from the module owner.

**Rule 3**: Contract-breaking changes (e.g., changing API response format) require approval from all dependent modules.

**Rule 4**: If modules disagree on contract, escalate to tech lead; maintain backward compatibility during negotiation.

### 6.3 Shared Infrastructure Change Rules

**Rule 1**: Changes to shared packages (entities, DTOs, repositories) require approval from Member 2 (primary infrastructure owner) and any module owner whose code depends on the change.

**Rule 2**: Breaking changes to shared exceptions or validation require full team consensus via PR comment approval.

**Rule 3**: New shared utilities must follow existing patterns and be reviewed by Member 3 (DTO/mapper conventions owner).

---

## 7. Task Completion Evidence Checklist

Use this checklist to verify task completion before marking as [x] in `tasks.md`:

- [ ] ✅ **Code Written**: Primary implementation class(es) exist and follow module conventions
- [ ] ✅ **Unit Tests Failing**: If mandatory unit test task, test file created and fails initially (TDD)
- [ ] ✅ **Unit Tests Passing**: Implementation passes all unit tests
- [ ] ✅ **Contract/Integration Tests**: Contract and integration tests written and passing
- [ ] ✅ **Code Review**: Code reviewed by at least module owner (or dependent owner if cross-module)
- [ ] ✅ **PR Merged**: Feature branch merged to `develop` with CI passing
- [ ] ✅ **Spec Alignment**: Implementation matches spec.md requirements
- [ ] ✅ **Documentation**: Code comments and method-level docs completed
- [ ] ✅ **Traceability Entry**: Task logged in coverage matrix with test class names and PR link

---

## 8. Module Boundary Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Frontend Application                            │
│  M1-Analytics │ M1/M4-Auth │ M2-Bookings │ M3-Tickets │ M4-Notif    │
└────────────────────────┬────────────────────────────────────────────┘
                         │ HTTP (REST APIs)
┌────────────────────────▼────────────────────────────────────────────┐
│                  Backend Security Layer (M4)                         │
│  OAuth2 | JWT Auth | RBAC | Suspended-User Policy                   │
└────────────────────────┬────────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────────┐
│                   Controller Layer                                    │
├──────────────┬──────────────┬──────────────┬──────────────┬──────────┤
│ Auth (M4)    │ Facility(M1) │ Booking(M2)  │ Ticket(M3)   │ etc      │
└──────┬───────┴──────┬───────┴──────┬───────┴──────┬───────┴──────────┘
       │              │              │              │
┌──────▼──────┬───────▼──────┬───────▼──────┬───────▼──────────────────┐
│ Service Layer              │              │                          │
├────────────┼────────────────┼──────────────┼─────────────────────────┤
│ Auth (M4)  │ Facility (M1)  │ Booking (M2) │ Ticket (M3)            │
│ Notif (M4) │ Analytics (M1) │ Quota (M2)   │ Escalation (M3)        │
├────────────┴────────────────┴──────────────┴─────────────────────────┤
│              Shared Infrastructure Layer                              │
│  Entities │ Repositories │ DTOs │ Exception Handlers │ Time Policy   │
├────────────────────────────────┬────────────────────────────────────┤
│          Database (PostgreSQL) │  File Storage │ Email (Mailtrap)    │
└────────────────────────────────┴────────────────────────────────────┘
```

---

## 9. Contact & Escalation

| Issue Type | Contact | Escalation |
|-----------|---------|-----------|
| Task/code question in owned module | Module owner | Tech lead |
| Contract dispute | Both module owners | Tech lead mediates |
| Cross-module blocking issue | Tech lead | Project manager |
| Test failure in CI | Module owner + reviewer | Tech lead |
| Missing/unclear requirement | Feature lead | Product owner |

---

## 10. Document Maintenance

This document should be updated:

- **When**: New modules added, ownership transferred, or major restructuring occurs
- **By**: Primary owner of affected module(s)
- **Frequency**: After each major phase completion (see tasks.md phases)
- **Version**: Update timestamp in header

**Next Review Date**: After completion of Phase 5 (User Story 3)
