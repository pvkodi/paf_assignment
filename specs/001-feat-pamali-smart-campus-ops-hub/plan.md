# Implementation Plan: VenueLink Operations Hub

**Branch**: `001-feat-pamali-smart-campus-ops-hub` | **Date**: 2026-03-23 | **Spec**: /Users/pamigee/Desktop/paf_assignment/specs/001-feat-pamali-smart-campus-ops-hub/spec.md
**Input**: Feature specification from `/specs/001-feat-pamali-smart-campus-ops-hub/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Deliver a VenueLink Operations Hub that unifies authentication, facility discovery,
policy-driven bookings, approval/escalation workflows, ticketing, notifications, and
admin analytics in a locally deployable architecture. The technical approach is a Spring
Boot 3.x Java 21 monolith with strict layered architecture and explicit design patterns,
paired with a React 18 frontend, PostgreSQL 15, local filesystem uploads, scheduled
operational jobs, and CI-enforced testing/quality gates.

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Java 21 (backend), TypeScript/JavaScript on Node 20+ (frontend)  
**Primary Dependencies**: Spring Boot 3.x, Spring Security 6, Spring Data JPA, Hibernate, jjwt, JavaMailSender, Thumbnailator, React 18, Vite, React Router v6, Axios, TailwindCSS  
**Storage**: PostgreSQL 15 (Docker) and local filesystem `/uploads` (Docker volume mount)  
**Testing**: JUnit 5, Mockito, Spring Boot Test, frontend test runner as configured, GitHub Actions CI  
**Target Platform**: Docker Compose local environment (Linux containers) with browser frontend
**Project Type**: Web application (monolithic backend + SPA frontend)  
**Performance Goals**: p95 API latency < 300ms for standard CRUD/search, deterministic 409 response under booking conflicts, scheduled SLA checks complete within hourly window  
**Constraints**: No cloud services; strict DTO boundaries; RBAC on all endpoints; local timezone with DST; 24x7 SLA timing; local uploads only; service-method unit tests mandatory  
**Scale/Scope**: 5 core domains (Auth, Facilities, Booking/Quota/Approvals, Tickets/Escalation, Notifications/Analytics), 5 roles, 40+ functional requirements, 4 module owners

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Layering gate: PASS. Planned architecture enforces `Controller -> Service -> Repository`.
- API boundary gate: PASS. Contract and spec use request/response DTOs only.
- Security gate: PASS. RBAC matrix and OAuth+JWT flow defined for all endpoint groups.
- Pattern gate: PASS. Required patterns mapped to concrete components and module owners.
- Quality gate: PASS. Mandatory service-method unit tests included in test strategy.
- REST gate: PASS. OpenAPI contract includes status semantics and structured error schemas.
- Validation gate: PASS. DTO validation is part of request contracts and design constraints.
- File handling gate: PASS. Local `/uploads`, MIME/type checks, size limits, and sanitized names defined.
- Ownership and traceability gate: PASS. Team/module ownership explicitly mapped; commit traceability required.

Post-design re-check (after Phase 1 artifacts): PASS. `research.md`, `data-model.md`,
`contracts/openapi.yaml`, and `quickstart.md` remain constitution-compliant with no
violations requiring complexity justification.

## Project Structure

### Documentation (this feature)

```text
specs/001-feat-pamali-smart-campus-ops-hub/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
backend/
├── src/
│   ├── config/
│   ├── controllers/
│   ├── dto/
│   ├── entities/
│   ├── factories/
│   ├── observers/
│   ├── repositories/
│   ├── services/
│   ├── state/
│   ├── strategy/
│   ├── workflow/
│   └── util/
├── uploads/
└── src/test/
  ├── contract/
  ├── integration/
  └── unit/

frontend/
├── src/
│   ├── app/
│   ├── components/
│   ├── features/
│   ├── pages/
│   ├── routes/
│   ├── services/
│   └── state/
└── tests/

infra/
├── docker-compose.yml
└── postgres/

.github/
└── workflows/

specs/001-feat-pamali-smart-campus-ops-hub/
├── contracts/
│   └── openapi.yaml
├── data-model.md
├── plan.md
├── quickstart.md
└── research.md

tests/
├── e2e/
└── performance/
```

**Structure Decision**: Choose the web-application split (`backend` + `frontend`) with
shared local infrastructure (`infra/docker-compose.yml`) and spec-driven contract files
under `specs/001-feat-pamali-smart-campus-ops-hub`. This structure matches the provided
stack, keeps module ownership explicit, and preserves monolith consistency while enabling
independent frontend delivery.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |
