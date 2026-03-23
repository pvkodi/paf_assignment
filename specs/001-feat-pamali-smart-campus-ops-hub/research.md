# Research: Smart Campus Operations Hub

## Decision 1: Backend architecture and layering
- Decision: Use Spring Boot 3.x monolith with strict Controller -> Service -> Repository layering.
- Rationale: Matches constitutional architecture rule, simplifies transactional consistency, and keeps shared policy engines (quota, approvals, escalation) in-process.
- Alternatives considered:
  - Microservices split by module: rejected due to added operational complexity and distributed consistency overhead for this phase.
  - Controller-direct repository calls: rejected because it violates constitution and weakens maintainability.

## Decision 2: Authentication and authorization
- Decision: Use Google OAuth 2.0 authorization code flow for identity, issue JWT (HS256, 24h) for API access, enforce endpoint RBAC via Spring Security 6.
- Rationale: Supports institutional login, stateless API auth, and explicit role checks across all endpoints.
- Alternatives considered:
  - Session-only auth: rejected due to weaker API/mobile compatibility.
  - Cloud identity provider lock-in: rejected by no-cloud constraint.

## Decision 3: Persistence model and facility inheritance
- Decision: Use PostgreSQL 15 with Spring Data JPA/Hibernate, SINGLE_TABLE inheritance for Facility subtypes.
- Rationale: Provides strong relational integrity for bookings/tickets and efficient polymorphic querying for facilities.
- Alternatives considered:
  - TABLE_PER_CLASS: rejected due to complex polymorphic queries.
  - JSON-only subtype fields: rejected due to weaker constraints and indexing.

## Decision 4: Concurrency handling for bookings
- Decision: Apply optimistic locking with @Version on Booking and fail conflicts immediately with HTTP 409 including current version details.
- Rationale: Prevents overlapping race conditions while preserving throughput under moderate contention.
- Alternatives considered:
  - Pessimistic locks: rejected due to lock contention and reduced concurrency.
  - Silent retries: rejected due to nondeterministic behavior and harder UX/debugging.

## Decision 5: Quota and policy resolution
- Decision: Implement QuotaPolicyEngine with Strategy pattern (Student, Lecturer, Admin), and for multi-role users apply most permissive eligible role policy.
- Rationale: Encapsulates policy evolution cleanly and reflects accepted clarification.
- Alternatives considered:
  - Hardcoded if/else in booking service: rejected due to maintainability risk.
  - Most restrictive policy: rejected by clarification decision.

## Decision 6: Approval and escalation orchestration
- Decision: Use Chain of Responsibility for booking approval path and maintenance escalation levels.
- Rationale: Supports variable routing rules (role-based + high-capacity hall sign-off + SLA levels) without nested branching.
- Alternatives considered:
  - Workflow table-only interpreter first: deferred to later if rule variability grows.
  - Monolithic switch-case handlers: rejected for low extensibility.

## Decision 7: Ticket lifecycle management
- Decision: Use State pattern for ticket status transitions (OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED or REJECTED).
- Rationale: Enforces valid transitions and centralizes transition guards.
- Alternatives considered:
  - Free-form status updates: rejected due to invalid transition risk.

## Decision 8: Notification architecture
- Decision: Use Observer pattern with InAppObserver and EmailObserver, severity-based routing (HIGH = email + in-app, STANDARD = in-app).
- Rationale: Clean channel fan-out and easy future extension to additional channels.
- Alternatives considered:
  - Inline email calls in each service: rejected due to duplication and coupling.

## Decision 9: File handling and image processing
- Decision: Store uploads on local filesystem at /uploads only, enforce file type and size constraints, sanitize filenames, generate 200x200 thumbnails with Thumbnailator.
- Rationale: Meets constitution and feature constraints (no cloud), while handling secure media processing.
- Alternatives considered:
  - Cloud object storage: rejected by explicit no-cloud requirement.
  - Store binaries in DB: rejected for operational overhead and backup size growth.

## Decision 10: Time semantics for booking and SLA logic
- Decision: Use campus local timezone with DST support for booking, no-show, reminders, and SLA calculations; SLA clocks are 24x7 elapsed.
- Rationale: Matches business operations and accepted clarifications while avoiding timezone ambiguity.
- Alternatives considered:
  - UTC-only behavior: rejected due to policy misalignment with local campus operations.
  - User-local timezones: rejected due to inconsistent institutional policy enforcement.

## Decision 11: Analytics computation approach
- Decision: Compute daily utilization snapshots via scheduled jobs; define utilization as (booked_hours / available_hours) x 100, excluding maintenance facilities and those with <50 available hours.
- Rationale: Predictable reporting and low query latency for admin dashboards.
- Alternatives considered:
  - On-demand heavy aggregation each request: rejected due to potential performance variability.

## Decision 12: Delivery and quality gates
- Decision: Use JUnit 5 + Mockito + Spring Boot Test and require service-method unit tests for all modified service methods; CI via GitHub Actions.
- Rationale: Constitution requires service-level test coverage and repeatable quality gates.
- Alternatives considered:
  - Integration-test-only strategy: rejected because it misses fine-grained service logic regressions.
