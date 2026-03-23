# Tasks: Smart Campus Operations Hub

**Input**: Design documents from `/specs/001-feat-pamali-smart-campus-ops-hub/`  
**Prerequisites**: `plan.md` (required), `spec.md` (required), `research.md`, `data-model.md`, `contracts/openapi.yaml`, `quickstart.md`

**Tests**: Service-layer unit tests are mandatory for all affected service methods. Contract and integration tests are included for each user story.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no direct dependency)
- **[Story]**: User story label (`[US1]`, `[US2]`, ...)
- Every task includes an exact file path

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize backend/frontend workspaces, local runtime, and CI baseline.

- [X] T001 Create backend module structure in `backend/src/main/java/com/smartcampus/` and `backend/src/test/java/com/smartcampus/`
- [X] T002 Create frontend app shell with route skeleton in `frontend/src/app/` and `frontend/src/routes/`
- [X] T003 [P] Configure Docker Compose services in `infra/docker-compose.yml`
- [X] T004 [P] Add backend dependencies (security, jjwt, mail, thumbnailator, test libs) in `backend/pom.xml`
- [X] T005 [P] Add frontend dependencies (router, axios, tailwind) in `frontend/package.json`
- [X] T006 [P] Configure Tailwind and base styles in `frontend/tailwind.config.js` and `frontend/src/index.css`
- [X] T007 [P] Add environment templates in `backend/.env.example` and `frontend/.env.example`
- [X] T008 Add CI pipeline for backend/frontend build and tests in `.github/workflows/ci.yml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build shared platform foundations required by all user stories.

**CRITICAL**: No user story work starts before this phase is complete.

- [ ] T009 Create initial Flyway migration for core schema in `backend/src/main/resources/db/migration/V1__init_core_schema.sql`
- [ ] T010 Implement Facility SINGLE_TABLE inheritance entities in `backend/src/main/java/com/smartcampus/entities/facility/`
- [ ] T011 [P] Implement base repositories in `backend/src/main/java/com/smartcampus/repositories/`
- [ ] T012 Implement DTO base packages and mapper conventions in `backend/src/main/java/com/smartcampus/dto/` and `backend/src/main/java/com/smartcampus/util/mapping/`
- [ ] T013 Implement global API error model and exception handler in `backend/src/main/java/com/smartcampus/controllers/advice/GlobalExceptionHandler.java`
- [ ] T014 [P] Implement Bean Validation configuration and message bundles in `backend/src/main/java/com/smartcampus/config/ValidationConfig.java` and `backend/src/main/resources/messages.properties`
- [ ] T015 Implement OAuth2 + JWT security baseline in `backend/src/main/java/com/smartcampus/config/SecurityConfig.java`
- [ ] T016 [P] Implement JWT utility and auth filter in `backend/src/main/java/com/smartcampus/config/security/`
- [ ] T017 [P] Implement local upload storage + static resource serving in `backend/src/main/java/com/smartcampus/config/FileStorageConfig.java` and `backend/src/main/java/com/smartcampus/services/storage/LocalFileStorageService.java`
- [ ] T018 Implement timezone and public-holiday provider foundation in `backend/src/main/java/com/smartcampus/config/TimePolicyConfig.java` and `backend/src/main/java/com/smartcampus/services/calendar/PublicHolidayService.java`
- [ ] T019 [P] Implement observer interfaces and event envelope in `backend/src/main/java/com/smartcampus/observers/`
- [ ] T020 [P] Implement strategy interface and role-policy resolver scaffold in `backend/src/main/java/com/smartcampus/strategy/quota/`
- [ ] T021 [P] Implement workflow chain interfaces for approvals and escalation in `backend/src/main/java/com/smartcampus/workflow/`
- [ ] T022 Document module ownership and feature traceability rules in `docs/ownership-and-traceability.md`

**Checkpoint**: Foundation complete, user story phases can proceed.

---

## Phase 3: User Story 1 - Secure Access and Role Governance (Priority: P1)

**Goal**: Deliver OAuth login, JWT issuance, role enforcement, and suspended-user access restrictions.

**Independent Test**: Authenticate users by role, verify endpoint RBAC, and confirm only allowed suspended-user actions remain accessible.

### Tests for User Story 1

- [ ] T023 [P] [US1] Add unit tests for OAuth auth and JWT services in `backend/src/test/java/com/smartcampus/unit/auth/AuthServiceTest.java`
- [ ] T024 [P] [US1] Add unit tests for suspended-user guard rules in `backend/src/test/java/com/smartcampus/unit/auth/SuspensionPolicyServiceTest.java`
- [ ] T025 [P] [US1] Add contract tests for auth callback/profile endpoints in `backend/src/test/java/com/smartcampus/contract/auth/AuthContractTest.java`
- [ ] T026 [P] [US1] Add integration tests for RBAC and suspended-user exceptions in `backend/src/test/java/com/smartcampus/integration/auth/RbacIntegrationTest.java`

### Implementation for User Story 1

- [ ] T027 [P] [US1] Implement User and Role entities in `backend/src/main/java/com/smartcampus/entities/auth/`
- [ ] T028 [P] [US1] Implement auth request/response DTOs in `backend/src/main/java/com/smartcampus/dto/auth/`
- [ ] T029 [US1] Implement Google OAuth exchange service in `backend/src/main/java/com/smartcampus/services/auth/OAuthAuthService.java`
- [ ] T030 [US1] Implement JWT issuance and validation service in `backend/src/main/java/com/smartcampus/services/auth/JwtTokenService.java`
- [ ] T031 [US1] Implement suspended-user policy service in `backend/src/main/java/com/smartcampus/services/auth/SuspensionPolicyService.java`
- [ ] T032 [US1] Implement auth/profile controllers with DTO boundaries in `backend/src/main/java/com/smartcampus/controllers/AuthController.java`
- [ ] T033 [US1] Enforce endpoint role annotations for auth/profile access in `backend/src/main/java/com/smartcampus/config/security/EndpointAuthorizationConfig.java`
- [ ] T034 [US1] Implement frontend auth state and guarded routes in `frontend/src/features/auth/` and `frontend/src/routes/ProtectedRoute.tsx`

**Checkpoint**: US1 is independently testable and deployable.

---

## Phase 4: User Story 2 - Facility Discovery and Policy-Compliant Booking (Priority: P1)

**Goal**: Deliver facility search and booking creation with capacity, overlap, recurrence, timezone, and admin-on-behalf support.

**Independent Test**: Search facilities and submit booking requests with valid/invalid combinations, including optimistic-lock conflict responses.

### Tests for User Story 2

- [ ] T035 [P] [US2] Add unit tests for facility search and factory logic in `backend/src/test/java/com/smartcampus/unit/facility/FacilityServiceTest.java`
- [ ] T036 [P] [US2] Add unit tests for booking builder and booking service rules in `backend/src/test/java/com/smartcampus/unit/booking/BookingServiceTest.java`
- [ ] T037 [P] [US2] Add contract tests for facilities and bookings endpoints in `backend/src/test/java/com/smartcampus/contract/booking/BookingContractTest.java`
- [ ] T038 [P] [US2] Add integration tests for overlap conflict and recurrence holiday skips in `backend/src/test/java/com/smartcampus/integration/booking/BookingConcurrencyIntegrationTest.java`

### Implementation for User Story 2

- [ ] T039 [P] [US2] Implement facility subtypes and metadata fields in `backend/src/main/java/com/smartcampus/entities/facility/`
- [ ] T040 [P] [US2] Implement FacilityFactory in `backend/src/main/java/com/smartcampus/factories/FacilityFactory.java`
- [ ] T041 [US2] Implement facility query service and repository specifications in `backend/src/main/java/com/smartcampus/services/facility/FacilityService.java`
- [ ] T042 [US2] Implement facilities search controller with DTO output in `backend/src/main/java/com/smartcampus/controllers/FacilityController.java`
- [ ] T043 [P] [US2] Implement Booking entity with `@Version` and recurrence fields in `backend/src/main/java/com/smartcampus/entities/booking/Booking.java`
- [ ] T044 [P] [US2] Implement BookingBuilder in `backend/src/main/java/com/smartcampus/util/booking/BookingBuilder.java`
- [ ] T045 [US2] Implement booking service (capacity, overlap, 409 conflicts, recurrence skips, timezone) in `backend/src/main/java/com/smartcampus/services/booking/BookingService.java`
- [ ] T046 [US2] Implement booking controller including admin `bookedFor` support in `backend/src/main/java/com/smartcampus/controllers/BookingController.java`
- [ ] T047 [US2] Implement frontend facility search and booking forms in `frontend/src/features/facilities/` and `frontend/src/features/bookings/`

**Checkpoint**: US2 is independently testable and deployable.

---

## Phase 5: User Story 3 - Approval, Quota Enforcement, and Suspension Lifecycle (Priority: P2)

**Goal**: Deliver quota strategy rules, approval routing, check-in/no-show logic, automatic suspension, and appeals.

**Independent Test**: Execute approval paths by role, enforce quotas/peak-hour/advance windows, record no-shows, and process appeals.

### Tests for User Story 3

- [ ] T048 [P] [US3] Add unit tests for quota strategy engine in `backend/src/test/java/com/smartcampus/unit/quota/QuotaPolicyEngineTest.java`
- [ ] T049 [P] [US3] Add unit tests for approval workflow chain in `backend/src/test/java/com/smartcampus/unit/workflow/ApprovalWorkflowServiceTest.java`
- [ ] T050 [P] [US3] Add unit tests for no-show and suspension services in `backend/src/test/java/com/smartcampus/unit/auth/NoShowSuspensionServiceTest.java`
- [ ] T051 [P] [US3] Add contract tests for approve/check-in/appeal endpoints in `backend/src/test/java/com/smartcampus/contract/workflow/ApprovalAndAppealContractTest.java`
- [ ] T052 [P] [US3] Add integration tests for multi-role permissive policy and suspension lifecycle in `backend/src/test/java/com/smartcampus/integration/workflow/QuotaApprovalIntegrationTest.java`

### Implementation for User Story 3

- [ ] T053 [P] [US3] Implement quota strategies (`Student`, `Lecturer`, `Admin`) in `backend/src/main/java/com/smartcampus/strategy/quota/`
- [ ] T054 [US3] Implement QuotaPolicyEngine and effective-role resolver in `backend/src/main/java/com/smartcampus/services/booking/QuotaPolicyEngine.java`
- [ ] T055 [US3] Implement booking approval chain handlers in `backend/src/main/java/com/smartcampus/workflow/approval/`
- [ ] T056 [US3] Implement approval orchestration service in `backend/src/main/java/com/smartcampus/services/booking/ApprovalWorkflowService.java`
- [ ] T057 [US3] Implement check-in service (QR/manual) and no-show evaluator in `backend/src/main/java/com/smartcampus/services/booking/CheckInService.java`
- [ ] T058 [US3] Implement suspension and appeal services/controllers in `backend/src/main/java/com/smartcampus/services/auth/AppealService.java` and `backend/src/main/java/com/smartcampus/controllers/AppealController.java`
- [ ] T059 [US3] Implement frontend approval queue and appeal screens in `frontend/src/features/approvals/` and `frontend/src/features/appeals/`

**Checkpoint**: US3 is independently testable and deployable.

---

## Phase 6: User Story 4 - Maintenance Ticketing with SLA Escalation (Priority: P2)

**Goal**: Deliver ticket lifecycle, attachments, comments, assignment, SLA deadlines, and escalation chain.

**Independent Test**: Create tickets with attachments, process status transitions, enforce comment visibility rules, and validate escalation actions.

### Tests for User Story 4

- [ ] T060 [P] [US4] Add unit tests for ticket state machine in `backend/src/test/java/com/smartcampus/unit/ticket/TicketStateMachineTest.java`
- [ ] T061 [P] [US4] Add unit tests for escalation service and SLA deadlines in `backend/src/test/java/com/smartcampus/unit/ticket/EscalationServiceTest.java`
- [ ] T062 [P] [US4] Add unit tests for attachment validation and thumbnail generation in `backend/src/test/java/com/smartcampus/unit/ticket/AttachmentServiceTest.java`
- [ ] T063 [P] [US4] Add contract tests for ticket/comment/assignment endpoints in `backend/src/test/java/com/smartcampus/contract/ticket/TicketContractTest.java`
- [ ] T064 [P] [US4] Add integration tests for escalation level actions and internal-comment visibility in `backend/src/test/java/com/smartcampus/integration/ticket/TicketEscalationIntegrationTest.java`

### Implementation for User Story 4

- [ ] T065 [P] [US4] Implement ticket, comment, attachment, and escalation entities in `backend/src/main/java/com/smartcampus/entities/ticket/`
- [ ] T066 [US4] Implement TicketStateMachine in `backend/src/main/java/com/smartcampus/state/TicketStateMachine.java`
- [ ] T067 [US4] Implement attachment pipeline (MIME/size/sanitization/thumbnailator) in `backend/src/main/java/com/smartcampus/services/ticket/TicketAttachmentService.java`
- [ ] T068 [US4] Implement ticket service and comment visibility rules in `backend/src/main/java/com/smartcampus/services/ticket/TicketService.java`
- [ ] T069 [US4] Implement escalation chain handlers and orchestration in `backend/src/main/java/com/smartcampus/workflow/escalation/` and `backend/src/main/java/com/smartcampus/services/ticket/EscalationService.java`
- [ ] T070 [US4] Implement hourly SLA scheduler in `backend/src/main/java/com/smartcampus/services/ticket/SlaScheduler.java`
- [ ] T071 [US4] Implement ticket controllers with DTO boundaries and RBAC in `backend/src/main/java/com/smartcampus/controllers/TicketController.java`
- [ ] T072 [US4] Implement frontend ticketing, assignment, and comments UI in `frontend/src/features/tickets/`

**Checkpoint**: US4 is independently testable and deployable.

---

## Phase 7: User Story 5 - Operational Notifications and Admin Analytics (Priority: P3)

**Goal**: Deliver multi-channel notifications and admin utilization analytics with underutilization detection.

**Independent Test**: Trigger high/standard events and verify channels, then validate utilization calculations and recommendations.

### Tests for User Story 5

- [ ] T073 [P] [US5] Add unit tests for notification observer routing in `backend/src/test/java/com/smartcampus/unit/notification/NotificationServiceTest.java`
- [ ] T074 [P] [US5] Add unit tests for utilization calculation and underutilization detection in `backend/src/test/java/com/smartcampus/unit/analytics/UtilizationServiceTest.java`
- [ ] T075 [P] [US5] Add contract tests for notification feed and analytics endpoints in `backend/src/test/java/com/smartcampus/contract/analytics/AnalyticsContractTest.java`
- [ ] T076 [P] [US5] Add integration tests for notification channels and daily snapshot jobs in `backend/src/test/java/com/smartcampus/integration/analytics/NotificationAnalyticsIntegrationTest.java`

### Implementation for User Story 5

- [ ] T077 [P] [US5] Implement InAppObserver and EmailObserver in `backend/src/main/java/com/smartcampus/observers/impl/`
- [ ] T078 [US5] Implement NotificationService event routing and templates in `backend/src/main/java/com/smartcampus/services/notification/NotificationService.java`
- [ ] T079 [US5] Implement SMTP adapter configuration for Mailtrap in `backend/src/main/java/com/smartcampus/config/MailConfig.java`
- [ ] T080 [US5] Implement utilization snapshot service and daily scheduler in `backend/src/main/java/com/smartcampus/services/analytics/UtilizationSnapshotService.java`
- [ ] T081 [US5] Implement underutilization and alternative-facility recommendation service in `backend/src/main/java/com/smartcampus/services/analytics/RecommendationService.java`
- [ ] T082 [US5] Implement analytics and notification controllers in `backend/src/main/java/com/smartcampus/controllers/AnalyticsController.java` and `backend/src/main/java/com/smartcampus/controllers/NotificationController.java`
- [ ] T083 [US5] Implement frontend notification center and utilization dashboard in `frontend/src/features/notifications/` and `frontend/src/features/analytics/`

**Checkpoint**: US5 is independently testable and deployable.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Hardening and verification across all stories.

- [ ] T084 [P] Update API and module documentation in `docs/api/` and `docs/architecture/`
- [ ] T085 [P] Add missing service-method unit tests and coverage gate in `backend/pom.xml` and `backend/src/test/java/com/smartcampus/`
- [ ] T086 Optimize critical DB queries and indexes in `backend/src/main/resources/db/migration/V2__performance_indexes.sql`
- [ ] T087 Run security hardening for JWT, RBAC, and upload paths in `backend/src/main/java/com/smartcampus/config/security/` and `backend/src/main/java/com/smartcampus/services/storage/`
- [ ] T088 Validate local Docker persistence and operational runbook in `quickstart.md` and `infra/docker-compose.yml`

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1): No dependencies.
- Foundational (Phase 2): Depends on Setup; blocks all user stories.
- User Stories (Phases 3-7): Depend on Foundational completion.
- Polish (Phase 8): Depends on all completed user stories.

### User Story Dependencies

- US1 (P1): Starts immediately after Foundational.
- US2 (P1): Starts immediately after Foundational; independent from US1 except shared auth context.
- US3 (P2): Depends on US1 auth/RBAC baseline and US2 booking core.
- US4 (P2): Depends on US1 auth/RBAC baseline and Foundational storage/scheduling.
- US5 (P3): Depends on US2 booking events and US4 escalation events.

### Within Each User Story

- Mandatory service unit tests first (must fail before implementation).
- Entities/models before services.
- Services before controllers.
- Backend contract fulfillment before frontend integration.

## Parallel Execution Examples

### User Story 1

- `T023`, `T024`, `T025`, `T026` can run in parallel.
- `T027` and `T028` can run in parallel.

### User Story 2

- `T035`, `T036`, `T037`, `T038` can run in parallel.
- `T039`, `T040`, `T043`, `T044` can run in parallel.

### User Story 3

- `T048`, `T049`, `T050`, `T051`, `T052` can run in parallel.
- `T053` and `T055` can run in parallel.

### User Story 4

- `T060`, `T061`, `T062`, `T063`, `T064` can run in parallel.
- `T065` and `T066` can run in parallel.

### User Story 5

- `T073`, `T074`, `T075`, `T076` can run in parallel.
- `T077` and `T080` can run in parallel.

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete US1 (secure access and role governance).
3. Validate US1 independently before proceeding.

### Incremental Delivery

1. Deliver US2 next for core facility-booking value.
2. Add US3 governance controls.
3. Add US4 maintenance operations.
4. Add US5 notification and analytics intelligence.

### Parallel Team Strategy

1. Shared completion of Setup + Foundational.
2. Module-owner parallelization after foundations:
   - Member 4 leads US1.
   - Member 1 and Member 2 split US2.
   - Member 2 and Member 4 split US3.
   - Member 3 leads US4.
   - Member 1 and Member 4 split US5.
