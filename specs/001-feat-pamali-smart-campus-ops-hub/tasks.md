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

- [x] T001 Create backend module structure in `backend/src/main/java/com/sliitreserve/api/` and `backend/src/test/java/com/sliitreserve/api/`
- [x] T002 Create frontend app shell with route skeleton in `frontend/src/app/` and `frontend/src/routes/`
- [x] T003 [P] Configure Docker Compose services in `infra/docker-compose.yml`
- [x] T004 [P] Add backend dependencies (security, jjwt, mail, thumbnailator, test libs) in `backend/pom.xml`
- [x] T005 [P] Add frontend dependencies (router, axios, tailwind) in `frontend/package.json`
- [x] T006 [P] Configure Tailwind and base styles in `frontend/tailwind.config.js` and `frontend/src/index.css`
- [x] T007 [P] Add environment templates in `backend/.env.example` and `frontend/.env.example`
- [x] T008 Add CI pipeline for backend/frontend build and tests in `.github/workflows/ci.yml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build shared platform foundations required by all user stories.

**CRITICAL**: No user story work starts before this phase is complete.

- [x] T009 Create initial Flyway migration for core schema in `backend/src/main/resources/db/migration/V1__init_core_schema.sql`
- [x] T010 Implement Facility SINGLE_TABLE inheritance entities in `backend/src/main/java/com/sliitreserve/api/entities/facility/`
- [x] T011 [P] Implement base repositories in `backend/src/main/java/com/sliitreserve/api/repositories/`
- [x] T012 Implement DTO base packages and mapper conventions in `backend/src/main/java/com/sliitreserve/api/dto/` and `backend/src/main/java/com/sliitreserve/api/util/mapping/`
- [x] T013 Implement global API error model and exception handler in `backend/src/main/java/com/sliitreserve/api/controllers/advice/GlobalExceptionHandler.java`
- [x] T014 [P] Implement Bean Validation configuration and message bundles in `backend/src/main/java/com/sliitreserve/api/config/ValidationConfig.java` and `backend/src/main/resources/messages.properties`
- [x] T015 Implement OAuth2 + JWT security baseline in `backend/src/main/java/com/sliitreserve/api/config/SecurityConfig.java`
- [x] T016 [P] Implement JWT utility and auth filter in `backend/src/main/java/com/sliitreserve/api/config/security/`
- [x] T017 [P] Implement local upload storage + static resource serving in `backend/src/main/java/com/sliitreserve/api/config/FileStorageConfig.java` and `backend/src/main/java/com/sliitreserve/api/services/storage/LocalFileStorageService.java`
- [x] T018 Implement timezone and public-holiday provider foundation in `backend/src/main/java/com/sliitreserve/api/config/TimePolicyConfig.java` and `backend/src/main/java/com/sliitreserve/api/services/calendar/PublicHolidayService.java`
- [x] T019 [P] Implement observer interfaces and event envelope in `backend/src/main/java/com/sliitreserve/api/observer/`
- [x] T020 [P] Implement strategy interface and role-policy resolver scaffold in `backend/src/main/java/com/sliitreserve/api/strategy/quota/`
- [x] T021 [P] Implement workflow chain interfaces for approvals and escalation in `backend/src/main/java/com/sliitreserve/api/workflow/`
- [ ] T022 Document module ownership and feature traceability rules in `docs/ownership-and-traceability.md`

**Checkpoint**: Foundation complete, user story phases can proceed.

---

## Phase 3: User Story 1 - Secure Access and Role Governance (Priority: P1)

**Goal**: Deliver OAuth login, JWT issuance, role enforcement, and suspended-user access restrictions.

**Independent Test**: Authenticate users by role, verify endpoint RBAC, and confirm only allowed suspended-user actions remain accessible.

### Tests for User Story 1

- [ ] T023 [P] [US1] Add unit tests for OAuth auth and JWT services in `backend/src/test/java/com/sliitreserve/api/unit/auth/AuthServiceTest.java`
- [ ] T024 [P] [US1] Add unit tests for suspended-user guard rules in `backend/src/test/java/com/sliitreserve/api/unit/auth/SuspensionPolicyServiceTest.java`
- [ ] T025 [P] [US1] Add contract tests for auth callback/profile endpoints in `backend/src/test/java/com/sliitreserve/api/contract/auth/AuthContractTest.java`
- [ ] T026 [P] [US1] Add integration tests for RBAC and suspended-user exceptions in `backend/src/test/java/com/sliitreserve/api/integration/auth/RbacIntegrationTest.java`

### Implementation for User Story 1

- [x] T027 [P] [US1] Implement User and Role entities in `backend/src/main/java/com/sliitreserve/api/entities/auth/`
- [x] T028 [P] [US1] Implement auth request/response DTOs in `backend/src/main/java/com/sliitreserve/api/dto/auth/`
- [x] T029 [US1] Implement Google OAuth exchange service in `backend/src/main/java/com/sliitreserve/api/services/auth/OAuthAuthService.java`
- [ ] T030 [US1] Implement JWT issuance and validation service in `backend/src/main/java/com/sliitreserve/api/services/auth/JwtTokenService.java`
- [ ] T031 [US1] Implement suspended-user policy service in `backend/src/main/java/com/sliitreserve/api/services/auth/SuspensionPolicyService.java`
- [ ] T032 [US1] Implement auth/profile controllers with DTO boundaries in `backend/src/main/java/com/sliitreserve/api/controllers/AuthController.java`
- [ ] T033 [US1] Enforce endpoint role annotations for auth/profile access in `backend/src/main/java/com/sliitreserve/api/config/security/EndpointAuthorizationConfig.java`
- [ ] T034 [US1] Implement frontend auth state and guarded routes in `frontend/src/features/auth/` and `frontend/src/routes/ProtectedRoute.tsx`

**Checkpoint**: US1 is independently testable and deployable.

---

## Phase 4: User Story 2 - Facility Discovery and Policy-Compliant Booking (Priority: P1)

**Goal**: Deliver facility search and booking creation with capacity, overlap, recurrence, timezone, and admin-on-behalf support.

**Independent Test**: Search facilities and submit booking requests with valid/invalid combinations, including optimistic-lock conflict responses.

### Tests for User Story 2

- [ ] T035 [P] [US2] Add unit tests for facility search and factory logic in `backend/src/test/java/com/sliitreserve/api/unit/facility/FacilityServiceTest.java`
- [ ] T036 [P] [US2] Add unit tests for booking builder and booking service rules in `backend/src/test/java/com/sliitreserve/api/unit/booking/BookingServiceTest.java`
- [ ] T037 [P] [US2] Add contract tests for facilities and bookings endpoints in `backend/src/test/java/com/sliitreserve/api/contract/booking/BookingContractTest.java`
- [ ] T038 [P] [US2] Add integration tests for overlap conflict and recurrence holiday skips in `backend/src/test/java/com/sliitreserve/api/integration/booking/BookingConcurrencyIntegrationTest.java`

### Implementation for User Story 2

- [ ] T039 [P] [US2] Implement facility subtypes and metadata fields in `backend/src/main/java/com/sliitreserve/api/entities/facility/`
- [ ] T040 [P] [US2] Implement FacilityFactory in `backend/src/main/java/com/sliitreserve/api/factories/FacilityFactory.java`
- [ ] T041 [US2] Implement facility query service and repository specifications in `backend/src/main/java/com/sliitreserve/api/services/facility/FacilityService.java`
- [ ] T042 [US2] Implement facilities search controller with DTO output in `backend/src/main/java/com/sliitreserve/api/controllers/FacilityController.java`
- [ ] T043 [P] [US2] Implement Booking entity with `@Version` and recurrence fields in `backend/src/main/java/com/sliitreserve/api/entities/booking/Booking.java`
- [ ] T044 [P] [US2] Implement BookingBuilder in `backend/src/main/java/com/sliitreserve/api/util/booking/BookingBuilder.java`
- [ ] T045 [US2] Implement booking service (capacity, overlap, 409 conflicts, recurrence skips, timezone) in `backend/src/main/java/com/sliitreserve/api/services/booking/BookingService.java`
- [ ] T046 [US2] Implement booking controller including admin `bookedFor` support in `backend/src/main/java/com/sliitreserve/api/controllers/BookingController.java`
- [ ] T047 [US2] Implement frontend facility search and booking forms in `frontend/src/features/facilities/` and `frontend/src/features/bookings/`

**Checkpoint**: US2 is independently testable and deployable.

---

## Phase 5: User Story 3 - Approval, Quota Enforcement, and Suspension Lifecycle (Priority: P2)

**Goal**: Deliver quota strategy rules, approval routing, check-in/no-show logic, automatic suspension, and appeals.

**Independent Test**: Execute approval paths by role, enforce quotas/peak-hour/advance windows, record no-shows, and process appeals.

### Tests for User Story 3

- [ ] T048 [P] [US3] Add unit tests for quota strategy engine in `backend/src/test/java/com/sliitreserve/api/unit/quota/QuotaPolicyEngineTest.java`
- [ ] T049 [P] [US3] Add unit tests for approval workflow chain in `backend/src/test/java/com/sliitreserve/api/unit/workflow/ApprovalWorkflowServiceTest.java`
- [ ] T050 [P] [US3] Add unit tests for no-show and suspension services in `backend/src/test/java/com/sliitreserve/api/unit/auth/NoShowSuspensionServiceTest.java`
- [ ] T051 [P] [US3] Add contract tests for approve/check-in/appeal endpoints in `backend/src/test/java/com/sliitreserve/api/contract/workflow/ApprovalAndAppealContractTest.java`
- [ ] T052 [P] [US3] Add integration tests for multi-role permissive policy and suspension lifecycle in `backend/src/test/java/com/sliitreserve/api/integration/workflow/QuotaApprovalIntegrationTest.java`

### Implementation for User Story 3

- [ ] T053 [P] [US3] Implement quota strategies (`Student`, `Lecturer`, `Admin`) in `backend/src/main/java/com/sliitreserve/api/strategy/quota/`
- [ ] T054 [US3] Implement QuotaPolicyEngine and effective-role resolver in `backend/src/main/java/com/sliitreserve/api/services/booking/QuotaPolicyEngine.java`
- [ ] T055 [US3] Implement booking approval chain handlers in `backend/src/main/java/com/sliitreserve/api/workflow/approval/`
- [ ] T056 [US3] Implement approval orchestration service in `backend/src/main/java/com/sliitreserve/api/services/booking/ApprovalWorkflowService.java`
- [ ] T057 [US3] Implement check-in service (QR/manual) and no-show evaluator in `backend/src/main/java/com/sliitreserve/api/services/booking/CheckInService.java`
- [ ] T058 [US3] Implement suspension and appeal services/controllers in `backend/src/main/java/com/sliitreserve/api/services/auth/AppealService.java` and `backend/src/main/java/com/sliitreserve/api/controllers/AppealController.java`
- [ ] T059 [US3] Implement frontend approval queue and appeal screens in `frontend/src/features/approvals/` and `frontend/src/features/appeals/`

**Checkpoint**: US3 is independently testable and deployable.

---

## Phase 6: User Story 4 - Maintenance Ticketing with SLA Escalation (Priority: P2)

**Goal**: Deliver ticket lifecycle, attachments, comments, assignment, SLA deadlines, and escalation chain.

**Independent Test**: Create tickets with attachments, process status transitions, enforce comment visibility rules, and validate escalation actions.

### Tests for User Story 4

- [ ] T060 [P] [US4] Add unit tests for ticket state machine in `backend/src/test/java/com/sliitreserve/api/unit/ticket/TicketStateMachineTest.java`
- [ ] T061 [P] [US4] Add unit tests for escalation service and SLA deadlines in `backend/src/test/java/com/sliitreserve/api/unit/ticket/EscalationServiceTest.java`
- [ ] T062 [P] [US4] Add unit tests for attachment validation and thumbnail generation in `backend/src/test/java/com/sliitreserve/api/unit/ticket/AttachmentServiceTest.java`
- [ ] T063 [P] [US4] Add contract tests for ticket/comment/assignment endpoints in `backend/src/test/java/com/sliitreserve/api/contract/ticket/TicketContractTest.java`
- [ ] T064 [P] [US4] Add integration tests for escalation level actions and internal-comment visibility in `backend/src/test/java/com/sliitreserve/api/integration/ticket/TicketEscalationIntegrationTest.java`

### Implementation for User Story 4

- [ ] T065 [P] [US4] Implement ticket, comment, attachment, and escalation entities in `backend/src/main/java/com/sliitreserve/api/entities/ticket/`
- [ ] T066 [US4] Implement TicketStateMachine in `backend/src/main/java/com/sliitreserve/api/state/TicketStateMachine.java`
- [ ] T067 [US4] Implement attachment pipeline (MIME/size/sanitization/thumbnailator) in `backend/src/main/java/com/sliitreserve/api/services/ticket/TicketAttachmentService.java`
- [ ] T068 [US4] Implement ticket service and comment visibility rules in `backend/src/main/java/com/sliitreserve/api/services/ticket/TicketService.java`
- [ ] T069 [US4] Implement escalation chain handlers and orchestration in `backend/src/main/java/com/sliitreserve/api/workflow/escalation/` and `backend/src/main/java/com/sliitreserve/api/services/ticket/EscalationService.java`
- [ ] T070 [US4] Implement hourly SLA scheduler in `backend/src/main/java/com/sliitreserve/api/services/ticket/SlaScheduler.java`
- [ ] T071 [US4] Implement ticket controllers with DTO boundaries and RBAC in `backend/src/main/java/com/sliitreserve/api/controllers/TicketController.java`
- [ ] T072 [US4] Implement frontend ticketing, assignment, and comments UI in `frontend/src/features/tickets/`

**Checkpoint**: US4 is independently testable and deployable.

---

## Phase 7: User Story 5 - Operational Notifications and Admin Analytics (Priority: P3)

**Goal**: Deliver multi-channel notifications and admin utilization analytics with underutilization detection.

**Independent Test**: Trigger high/standard events and verify channels, then validate utilization calculations and recommendations.

### Tests for User Story 5

- [ ] T073 [P] [US5] Add unit tests for notification observer routing in `backend/src/test/java/com/sliitreserve/api/unit/notification/NotificationServiceTest.java`
- [ ] T074 [P] [US5] Add unit tests for utilization calculation and underutilization detection in `backend/src/test/java/com/sliitreserve/api/unit/analytics/UtilizationServiceTest.java`
- [ ] T075 [P] [US5] Add contract tests for notification feed and analytics endpoints in `backend/src/test/java/com/sliitreserve/api/contract/analytics/AnalyticsContractTest.java`
- [ ] T076 [P] [US5] Add integration tests for notification channels and daily snapshot jobs in `backend/src/test/java/com/sliitreserve/api/integration/analytics/NotificationAnalyticsIntegrationTest.java`

### Implementation for User Story 5

- [ ] T077 [P] [US5] Implement InAppObserver and EmailObserver in `backend/src/main/java/com/sliitreserve/api/observers/impl/`
- [ ] T078 [US5] Implement NotificationService event routing and templates in `backend/src/main/java/com/sliitreserve/api/services/notification/NotificationService.java`
- [ ] T079 [US5] Implement SMTP adapter configuration for Mailtrap in `backend/src/main/java/com/sliitreserve/api/config/MailConfig.java`
- [ ] T080 [US5] Implement utilization snapshot service and daily scheduler in `backend/src/main/java/com/sliitreserve/api/services/analytics/UtilizationSnapshotService.java`
- [ ] T081 [US5] Implement underutilization and alternative-facility recommendation service in `backend/src/main/java/com/sliitreserve/api/services/analytics/RecommendationService.java`
- [ ] T082 [US5] Implement analytics and notification controllers in `backend/src/main/java/com/sliitreserve/api/controllers/AnalyticsController.java` and `backend/src/main/java/com/sliitreserve/api/controllers/NotificationController.java`
- [ ] T083 [US5] Implement frontend notification center and utilization dashboard in `frontend/src/features/notifications/` and `frontend/src/features/analytics/`

**Checkpoint**: US5 is independently testable and deployable.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Hardening and verification across all stories.

- [ ] T084 [P] Update API and module documentation in `docs/api/` and `docs/architecture/`
- [ ] T085 [P] Add missing service-method unit tests and coverage gate in `backend/pom.xml` and `backend/src/test/java/com/sliitreserve/api/`
- [ ] T086 Optimize critical DB queries and indexes in `backend/src/main/resources/db/migration/V2__performance_indexes.sql`
- [ ] T087 Run security hardening for JWT, RBAC, and upload paths in `backend/src/main/java/com/sliitreserve/api/config/security/` and `backend/src/main/java/com/sliitreserve/api/services/storage/`
- [ ] T088 Validate local Docker persistence and operational runbook in `quickstart.md` and `infra/docker-compose.yml`

---

## Phase 9: Documentation, Diagrams, and Contribution Governance

**Purpose**: Ensure full requirement coverage in documentation, clear endpoint ownership, and verifiable individual contributions with GitHub + CI.

- [ ] T089 [P] Create architecture overview and deployment diagram docs in `docs/architecture/overview.md` and `docs/architecture/deployment.md`
- [ ] T090 [P] Create UML class diagram source and rendered diagram in `docs/architecture/class-diagram.mmd` and `docs/architecture/class-diagram.png`
- [ ] T091 [P] Create booking workflow sequence diagram and escalation flow diagram in `docs/architecture/booking-sequence.mmd` and `docs/architecture/escalation-sequence.mmd`
- [ ] T092 Create endpoint contribution matrix (endpoint -> owner -> tests -> PR links) in `docs/api/endpoint-contributions.md`
- [ ] T093 [P] Create module ownership and special-function ownership register in `docs/ownership-and-traceability.md`
- [ ] T094 [P] Document GitHub workflow standards (branch naming, PR template usage, labels, reviews) in `docs/github/workflow.md`
- [ ] T095 [P] Add CODEOWNERS mapping by module and endpoint groups in `.github/CODEOWNERS`
- [ ] T096 [P] Add PR template requiring endpoint/test evidence in `.github/pull_request_template.md`
- [ ] T097 [P] Expand CI with backend unit/contract/integration split and frontend checks in `.github/workflows/ci.yml`
- [ ] T098 Add requirement-to-task traceability matrix for FR/AR/SV coverage in `specs/001-feat-pamali-smart-campus-ops-hub/coverage-matrix.md`
- [ ] T099 Add release readiness checklist (API docs, diagrams, tests, contribution proof) in `specs/001-feat-pamali-smart-campus-ops-hub/release-checklist.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1): No dependencies.
- Foundational (Phase 2): Depends on Setup; blocks all user stories.
- User Stories (Phases 3-7): Depend on Foundational completion.
- Polish (Phase 8): Depends on all completed user stories.
- Documentation/Governance (Phase 9): Starts after Phase 2 for baseline docs; final completion depends on Phases 3-8 outputs.

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

1. Shared completion of Setup + Foundational (`T001`-`T022`) before feature branching.
2. After foundations, split by module ownership and special-function ownership so each member can build independently.

### Rebalanced Task Ownership (Authoritative Assignment)

This section is the single source of truth for ownership to avoid confusion. Each task has one primary owner; integration tasks can have reviewers from other modules.

**Load target**: keep each member in a similar band.

- Member 1: 25 tasks (Facilities + Smart Utilization)
- Member 2: 24 tasks (Booking + Fair-Usage Policy)
- Member 3: 25 tasks (Maintenance + Escalation)
- Member 4: 25 tasks (Auth + Approval + Notifications)

**Member 1 (M1) - Facilities + Smart Utilization & Optimization Engine**

- Setup/Foundational: `T001`, `T003`, `T006`, `T010`, `T014`, `T018`
- US1 support: `T026`, `T034`
- US2 facilities core: `T035`, `T039`, `T040`, `T041`, `T042`, `T047`
- US3 integration/UI: `T052`, `T059`
- US5 analytics core: `T074`, `T076`, `T080`, `T081`, `T083`
- Cross-cutting/docs: `T084`, `T089`, `T091`, `T098`

**Member 2 (M2) - Booking + Fair-Usage & Quota Policy Engine**

- Setup/Foundational: `T002`, `T004`, `T011`, `T015`, `T020`, `T022`
- US1 test support: `T023`, `T025`
- US2 booking core: `T036`, `T038`, `T043`, `T044`, `T045`, `T046`
- US3 quota/check-in core: `T048`, `T053`, `T054`, `T057`
- US4 support tests: `T062`, `T064`
- US5 contract support: `T075`
- Cross-cutting/docs: `T085`, `T094`, `T099`

**Member 3 (M3) - Maintenance Tickets + Intelligent Escalation System**

- Setup/Foundational: `T005`, `T007`, `T012`, `T017`, `T021`
- US1 model support: `T027`, `T028`
- US3 suspension/appeal support: `T050`, `T058`
- US4 ticket/escalation core: `T060`, `T061`, `T065`, `T066`, `T067`, `T068`, `T069`, `T070`, `T071`, `T072`
- Cross-cutting/docs: `T086`, `T088`, `T090`, `T092`, `T095`, `T097`

**Member 4 (M4) - Auth + Dynamic Approval + Notifications**

- Setup/Foundational: `T008`, `T009`, `T013`, `T016`, `T019`
- US1 auth core: `T024`, `T029`, `T030`, `T031`, `T032`, `T033`
- US2 contract support: `T037`
- US3 approval core: `T049`, `T051`, `T055`, `T056`
- US4 contract support: `T063`
- US5 notification core: `T073`, `T077`, `T078`, `T079`, `T082`
- Cross-cutting/docs: `T087`, `T093`, `T096`

### Ownership Rules (To Keep Collaboration Easy)

1. Primary owner merges the task PR and maintains the module contract.
2. Cross-module tasks require one reviewer from each dependent module before merge.
3. If a task touches two owned modules, split into two subtasks with separate PRs.
4. Endpoint ownership must be recorded in `docs/api/endpoint-contributions.md` (`T092`).
5. Conflicts are resolved by contract-first approach (OpenAPI/schema, then implementation).

### Cross-Module Contract Checkpoints (Keep Independent Dev Safe)

1. Booking-Approval API contract freeze after `T046` + `T055`.
2. Booking-Analytics event contract freeze before `T080`/`T081`.
3. Ticket-Escalation-Notification event contract freeze before `T069` + `T078`.
4. Auth role claims contract freeze before approval and quota integration (`T054`, `T056`).

### Suggested Branching Model

1. `feature/member1-facility-analytics`
2. `feature/member2-booking-quota`
3. `feature/member3-ticket-escalation`
4. `feature/member4-auth-approval-notification`
5. Merge cadence: daily into `develop` after module tests pass.
