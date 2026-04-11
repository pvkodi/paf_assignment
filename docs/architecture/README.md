# Smart Campus Operations Hub - Architecture Documentation

## Overview

This directory contains architecture diagrams and design documentation for the Smart Campus Operations Hub system. The system implements a comprehensive university facilities management platform with authentication, facility booking, maintenance ticketing, and analytics capabilities.

## Architecture Diagrams

### UML Class Diagram

**File**: `class-diagram.mmd` / `class-diagram.png`

**Scope**: Domain model covering 60+ entity and service classes across 6 architectural layers

**Contents**:
- **Enumerations**: Role, BookingStatus, TicketStatus, TicketPriority, NotificationChannel, and more
- **Domain Entities** (Auth Layer): User, Role-based permissions
- **Domain Entities** (Facility Layer): Facility hierarchy (LectureHall, Lab, MeetingRoom, Auditorium, Equipment, SportsFacility)
- **Domain Entities** (Booking Layer): Booking, ApprovalStep, CheckInRecord, SuspensionAppeal
- **Domain Entities** (Ticket Layer): MaintenanceTicket, TicketComment, TicketAttachment, EscalationEvent
- **Domain Entities** (Notification Layer): Notification, Observer pattern, EventPublisher
- **Domain Entities** (Analytics Layer): UtilizationSnapshot
- **DTO Layer**: Request/Response DTOs for all major domains (UserResponseDTO, FacilityResponseDTO, BookingResponseDTO, TicketResponseDTO, NotificationResponseDTO)
- **Service Layer**: Core services (BookingService, TicketService, NotificationService, UserService, FacilityService, AnalyticsService)
- **Repository Layer**: Spring Data JPA repository interfaces (BookingRepository, TicketRepository, UserRepository, FacilityRepository, NotificationRepository, UtilizationSnapshotRepository)
- **Controller Layer**: REST API controllers (BookingController, TicketController, NotificationController, AnalyticsController)

**Relationships**:
- **1:N (One-to-Many)**: Facility → Bookings, MaintenanceTickets; User → Bookings, Tickets, Notifications; MaintenanceTicket → Comments, Attachments, Escalations
- **N:1 (Many-to-One)**: Booking → Facility, User; MaintenanceTicket → Facility, User
- **Inheritance**: Facility is base class for 6 facility types (LectureHall, Lab, MeetingRoom, Auditorium, Equipment, SportsFacility)
- **Composition**: Booking contains ApprovalSteps and CheckInRecords
- **Interfaces**: Observer pattern for notification subscribers; Repository interfaces for data access
- **Service Integration**: Services depend on repositories and orchestrate business logic

**Cardinality Notation**:
- `1` = Exactly one
- `0..1` = Zero or one
- `*` = Zero or more
- `1..*` = One or more

## Architectural Layers

### 1. Domain Layer (Entities)
Core business entities representing the problem domain:
- **Auth Domain**: User with multi-role support, suspension tracking, no-show counting
- **Facility Domain**: Facility base class with 6 subtypes using single-table inheritance
- **Booking Domain**: Booking with workflow state, approval steps, check-in records
- **Ticket Domain**: MaintenanceTicket with SLA tracking, escalation hierarchy, attachments
- **Notification Domain**: Observer-based notification system with multiple channels (in-app, email)
- **Analytics Domain**: UtilizationSnapshot for facility usage analytics

### 2. Service Layer
Business logic and workflows:
- **BookingService**: Creates, updates, validates bookings; handles conflict detection; orchestrates approval workflow
- **TicketService**: Manages ticket lifecycle, SLA monitoring, escalation logic, comment/attachment management
- **NotificationService**: Sends notifications, publishes domain events, manages subscriber notifications
- **UserService**: User lifecycle, suspension/appeal management, authentication integration
- **FacilityService**: Facility search/filtering, availability checking
- **AnalyticsService**: Utilization calculations, underutilized facility identification

### 3. Repository Layer
Data persistence abstraction (Spring Data JPA):
- Repository interfaces extending Spring Data JPA
- Query methods for common use cases
- Custom specifications for complex queries
- Transaction management

### 4. Controller Layer
REST API endpoints (Spring MVC):
- **BookingController**: `/api/bookings` endpoints
- **TicketController**: `/api/tickets` endpoints  
- **NotificationController**: `/api/notifications` endpoints
- **AnalyticsController**: `/api/analytics` endpoints
- **AuthController**: OAuth authentication endpoints

### 5. DTO Layer
Data Transfer Objects for request/response:
- **Request DTOs**: BookingRequestDTO, TicketRequestDTO, UserRequestDTO, FacilityRequestDTO
- **Response DTOs**: All major domain entities have corresponding response DTOs
- **Base DTOs**: BaseResponseDTO, BaseRequestDTO with common fields (id, timestamps)

## Database Schema Relationships

```
User (1) ──────── (*) Booking
         ├─────── (*) SuspensionAppeal
         ├─────── (*) TicketComment
         └─────── (*) MaintenanceTicket

Facility (1) ───── (*) Booking
         ├────── (*) MaintenanceTicket
         └────── (*) UtilizationSnapshot

Booking (1) ─────── (*) ApprovalStep
        └────────── (*) CheckInRecord

MaintenanceTicket (1) ── (*) TicketComment
                    ├──── (*) TicketAttachment
                    └──── (*) EscalationEvent

Notification (*) ────── (1) User

User (1) ────────────── (*) Role (many-to-many via SINGLE_TABLE)
```

## Key Design Patterns

### 1. **Inheritance Hierarchy (Facility)**
- **Strategy**: Single-table inheritance
- **Reason**: Shares common facility attributes (code, name, capacity, location) while supporting facility-type-specific attributes
- **Types**: LectureHall, Lab, MeetingRoom, Auditorium, Equipment, SportsFacility

### 2. **Observer Pattern (Notifications)**
- **Purpose**: Notify multiple subscribers of domain events (booking approved, ticket escalated, etc.)
- **Implementation**: EventPublisher with Observer interface; NotificationObserver as concrete implementation
- **Events**: BookingApproved, BookingRejected, TicketCreated, TicketEscalated, SuspensionApplied, etc.

### 3. **Strategy Pattern (Approval Workflows)**
- **Purpose**: Flexible approval workflows based on booking attributes
- **Implementation**: ApprovalWorkflow strategy with pluggable decision rules
- **Rules**: Lecturer → Facility Manager → Admin for certain facility types; reduced path for others

### 4. **Strategy Pattern (Escalation)**
- **Purpose**: Time-based escalation of maintenance tickets
- **Implementation**: EscalationStrategy with level-based actions
- **Levels**: LEVEL_1 (reassign), LEVEL_2 (notify manager), LEVEL_3 (incident report + escalation)

### 5. **Repository Pattern**
- **Purpose**: Abstract data access logic
- **Implementation**: Spring Data JPA repositories with custom query methods
- **Benefit**: Testability, loose coupling, easy migration between persistence mechanisms

### 6. **Optimistic Locking (Booking)**
- **Purpose**: Prevent concurrent booking conflicts
- **Implementation**: `version` column on Booking entity
- **Behavior**: API returns 409 Conflict if version mismatch during update

## Integration Points

### OAuth 2.0 Integration
- Validates tokens from institutional OAuth provider
- Creates/updates User entities based on OAuth subject and email
- Manages session tokens and role-based access control

### Email Notifications
- NotificationService integrates with SMTP for email delivery
- Multi-channel support: in-app + email for high-priority events
- Standard events: in-app only; high-priority events: both channels

### File Storage
- TicketAttachment files stored in configurable local/cloud storage
- Automatic thumbnail generation for image attachments
- File type/size validation (max 5MB, images only)

## Related Documentation

- [Feature Specification](../../specs/001-feat-pamali-smart-campus-ops-hub/spec.md) - User stories and requirements
- [Data Model](../../specs/001-feat-pamali-smart-campus-ops-hub/data-model.md) - Entity definitions and relationships
- [API Contracts](../../specs/001-feat-pamali-smart-campus-ops-hub/contracts/openapi.yaml) - OpenAPI specification
- [Deployment Plan](../../.azure/plan.md) - Azure deployment architecture
- [Ownership & Traceability](../ownership-and-traceability.md) - Feature-to-code mapping

## Statistics

| Category | Count |
|----------|-------|
| **Entity Classes** | 25+ |
| **Enumeration Types** | 10+ |
| **DTO Classes** | 15+ |
| **Service Classes** | 6+ |
| **Repository Interfaces** | 6+ |
| **Controller Classes** | 5+ |
| **Domain Events** | 10+ |
| **Database Tables** | 17+ |
| **API Endpoints** | 40+ |
| **Total Classes** | 60+~ |

## Diagram Specifications

- **Format**: Mermaid classDiagram syntax
- **Layout**: Logical grouping by architectural layer
- **Rendering**: High-resolution PNG (1200+ width)
- **Last Updated**: 2026-04-11
- **Scope**: Production domain model (simplified from full system for readability)
