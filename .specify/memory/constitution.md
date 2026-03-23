<!--
Sync Impact Report
- Version change: template-placeholder -> 1.0.0
- Modified principles:
	- Placeholder Principle 1 -> I. Layered Architecture Integrity
	- Placeholder Principle 2 -> II. DTO-First API Boundaries
	- Placeholder Principle 3 -> III. Endpoint Security and Role Enforcement
	- Placeholder Principle 4 -> IV. Mandatory Pattern-Driven Domain Design
	- Placeholder Principle 5 -> V. Quality, Validation, and Delivery Discipline
- Added sections:
	- Technology Baseline and Security Constraints
	- Team Ownership and Delivery Workflow
- Removed sections:
	- None
- Templates requiring updates:
	- ✅ .specify/templates/plan-template.md
	- ✅ .specify/templates/spec-template.md
	- ✅ .specify/templates/tasks-template.md
	- ✅ .specify/templates/commands/*.md (no files present)
	- ✅ Runtime guidance docs (README.md, docs/quickstart.md) not present
- Follow-up TODOs:
	- None
-->

# Smart Campus Operations Hub Constitution

## Core Principles

### I. Layered Architecture Integrity
All backend request flow MUST follow `Controller -> Service -> Repository` with no layer skipping.
Controllers MUST contain transport concerns only, services MUST contain business rules, and
repositories MUST contain persistence concerns only. Direct controller-to-repository access,
service-to-web coupling, or embedding SQL in non-repository classes is prohibited. Rationale:
strict layering preserves maintainability, supports module ownership boundaries, and reduces
regression risk in a Java monolith.

### II. DTO-First API Boundaries
Every API request and response MUST use explicit DTOs; JPA entities MUST NOT be exposed at API
boundaries. DTO-to-entity mapping MUST occur in service or dedicated mapper components, and API
contracts MUST document fields, validation constraints, and error formats. Rationale: DTO-only
boundaries prevent persistence leakage, protect internal schema evolution, and provide stable
front-end contracts for React clients.

### III. Endpoint Security and Role Enforcement
Every endpoint MUST enforce role-based access control for at least one of these roles:
`USER`, `LECTURER`, `TECHNICIAN`, `FACILITY_MANAGER`, `ADMIN`. Authorization checks MUST be
declared and testable per endpoint (for example with Spring Security annotations and endpoint
tests). Authentication MUST use Google OAuth plus JWT for session propagation, and unsecured
business endpoints are forbidden. Rationale: campus operations contain privileged workflows that
require explicit least-privilege controls.

### IV. Mandatory Pattern-Driven Domain Design
The following design patterns are mandatory where the related domain behavior exists, and their
implementations MUST be explicit in code structure and naming:
- Factory pattern for facility creation flows.
- Strategy pattern for quota policy selection and evaluation.
- Chain of Responsibility for approval workflow routing and escalation.
- State pattern for ticket status transitions.
- Observer pattern for notifications.
- Builder pattern for booking construction.
- Repository pattern for data access abstractions.
Replacing these with ad hoc branching logic is non-compliant unless the constitution is amended.
Rationale: explicit patterns improve readability, extensibility, and ownership across modules.

### V. Quality, Validation, and Delivery Discipline
All service methods MUST have unit tests, and pull requests MUST fail if any service method lacks
coverage. All request DTO inputs MUST use Bean Validation and MUST be invoked with `@Valid` at
controller boundaries. REST APIs MUST use correct HTTP status codes, consistent naming, and
meaningful structured error responses. File uploads for images MUST enforce MIME/type checks,
size limits, and sanitized filenames. File storage MUST remain local filesystem only; cloud
storage usage is prohibited. Rationale: predictable quality gates and secure input handling are
non-negotiable for operational reliability.

## Technology Baseline and Security Constraints

The approved stack is Spring Boot 3.x on Java 21 (monolith backend), React 18 (frontend), and
PostgreSQL 15 (primary datastore). Authentication baseline is Google OAuth integrated with JWT.
Alternative major stack components require a constitution amendment with migration impact notes.
All API changes MUST preserve backward-compatible DTO contracts unless versioned and approved.

## Team Ownership and Delivery Workflow

The project MUST be partitioned into four modules, with one module and its special functionality
owned by each of the four team members. Each task and pull request MUST identify module owner,
changed module, and verification evidence. Git commits MUST reference the implemented feature
identifier (for example ticket ID, story ID, or feature key). Cross-module changes require review
from affected owners before merge.

## Governance

This constitution is the highest engineering authority for the Smart Campus Operations Hub.
All plans, specs, tasks, and code reviews MUST include an explicit constitutional compliance
check against each core principle.

Amendment process:
1. Propose changes via pull request that includes rationale, affected principles/sections, and
	 migration impact on existing artifacts.
2. Obtain approval from at least three of four module owners, including one owner outside the
	 directly impacted module.
3. Update dependent templates and guidance files in the same change set.

Versioning policy:
- MAJOR for backward-incompatible governance or principle removals/redefinitions.
- MINOR for new principles/sections or materially expanded requirements.
- PATCH for clarifications, wording improvements, and non-semantic refinements.

Compliance review expectations:
- Every feature plan MUST pass all constitution gates before implementation starts.
- Every pull request MUST include evidence for layered architecture, DTO boundaries, RBAC,
	validation, service unit tests, and secure file handling when relevant.
- Violations MUST be documented with remediation tasks before release.

**Version**: 1.0.0 | **Ratified**: 2026-03-23 | **Last Amended**: 2026-03-23
