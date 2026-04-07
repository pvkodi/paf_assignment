# Data Model: Smart Campus Operations Hub

## Modeling Notes
- Persistence: PostgreSQL 15 via Spring Data JPA/Hibernate.
- Inheritance: Facility hierarchy uses SINGLE_TABLE strategy.
- Concurrency: Booking uses optimistic locking via version column.
- Time basis: Campus local timezone with DST support.

## Entities

### User
- Fields:
  - id (UUID)
  - googleSubject (string, unique)
  - email (string, unique)
  - displayName (string)
  - roles (set<Role>)
  - active (boolean)
  - suspendedUntil (timestamp, nullable)
  - noShowCount (integer, default 0)
  - createdAt, updatedAt (timestamp)
- Validation:
  - email must be valid institutional email format.
- Relationships:
  - One-to-many Bookings (requestedBy)
  - One-to-many Bookings (bookedFor)
  - One-to-many SuspensionAppeals
  - One-to-many TicketComments

### Facility (base)
- Fields:
  - id (UUID)
  - facilityCode (string, unique)
  - name (string)
  - type (enum: LECTURE_HALL, LAB, MEETING_ROOM, AUDITORIUM, EQUIPMENT, SPORTS_FACILITY)
  - capacity (integer)
  - location (string)
  - building (string)
  - floor (string)
  - status (enum: ACTIVE, OUT_OF_SERVICE)
  - availabilityStart (local time)
  - availabilityEnd (local time)
  - createdAt, updatedAt (timestamp)
- Validation:
  - capacity > 0
  - availabilityStart < availabilityEnd
- Relationships:
  - One-to-many Bookings
  - One-to-many MaintenanceTickets

### LectureHall (Facility subtype)
- Fields:
  - avEquipment (string/list)
  - wheelchairAccessible (boolean)

### Lab (Facility subtype)
- Fields:
  - labType (string)
  - softwareList (string/list)
  - safetyEquipment (string/list)

### MeetingRoom (Facility subtype)
- Fields:
  - avEnabled (boolean)
  - cateringAllowed (boolean)

### Auditorium (Facility subtype)
- Fields:
  - stageType (string)
  - soundSystem (string)

### Equipment (Facility subtype)
- Fields:
  - brand (string)
  - model (string)
  - serialNumber (string, unique)
  - maintenanceSchedule (string)

### SportsFacility (Facility subtype)
- Fields:
  - sportsType (string)
  - equipmentAvailable (string/list)

### Booking
- Fields:
  - id (UUID)
  - facilityId (UUID)
  - requestedByUserId (UUID)
  - bookedForUserId (UUID)
  - bookingDate (date)
  - startTime (local time)
  - endTime (local time)
  - purpose (string)
  - attendees (integer)
  - status (enum: PENDING, APPROVED, REJECTED, CANCELLED)
  - recurrenceRule (string, nullable)
  - isRecurringMaster (boolean)
  - timezone (string, default campus timezone)
  - version (long, optimistic lock)
  - createdAt, updatedAt (timestamp)
- Validation:
  - startTime < endTime
  - attendees > 0
  - attendees <= facility.capacity
  - booking datetime within advance-window policy
- Relationships:
  - Many-to-one Facility
  - Many-to-one User (requestedBy)
  - Many-to-one User (bookedFor)
  - One-to-many ApprovalSteps
  - One-to-many CheckInRecords

### ApprovalStep
- Fields:
  - id (UUID)
  - bookingId (UUID)
  - stepOrder (integer)
  - approverRole (enum: LECTURER, FACILITY_MANAGER, ADMIN)
  - decision (enum: PENDING, APPROVED, REJECTED)
  - decidedByUserId (UUID, nullable)
  - decidedAt (timestamp, nullable)
  - note (string, nullable)
- Validation:
  - stepOrder >= 1

### CheckInRecord
- Fields:
  - id (UUID)
  - bookingId (UUID)
  - method (enum: QR, MANUAL)
  - checkedInByUserId (UUID, nullable)
  - checkedInAt (timestamp)

### SuspensionAppeal
- Fields:
  - id (UUID)
  - userId (UUID)
  - reason (string)
  - status (enum: SUBMITTED, APPROVED, REJECTED)
  - reviewedByUserId (UUID, nullable)
  - reviewedAt (timestamp, nullable)

### MaintenanceTicket
- Fields:
  - id (UUID)
  - facilityId (UUID)
  - createdByUserId (UUID)
  - assignedTechnicianUserId (UUID, nullable)
  - category (enum: ELECTRICAL, PLUMBING, HVAC, IT_NETWORKING, STRUCTURAL, CLEANING, SAFETY, OTHER)
  - priority (enum: LOW, MEDIUM, HIGH, CRITICAL)
  - status (enum: OPEN, IN_PROGRESS, RESOLVED, CLOSED, REJECTED)
  - title (string)
  - description (string)
  - slaDueAt (timestamp)
  - escalationLevel (integer, default 0)
  - createdAt, updatedAt, resolvedAt, closedAt (timestamp, nullable)
- Validation:
  - description non-empty
- Relationships:
  - Many-to-one Facility
  - Many-to-one User (createdBy)
  - Many-to-one User (assignedTechnician)
  - One-to-many TicketComments
  - One-to-many TicketAttachments
  - One-to-many EscalationEvents

### TicketComment
- Fields:
  - id (UUID)
  - ticketId (UUID)
  - authorUserId (UUID)
  - body (text)
  - visibility (enum: PUBLIC, INTERNAL)
  - createdAt, updatedAt (timestamp)
  - deletedAt (timestamp, nullable)
- Validation:
  - body non-empty

### TicketAttachment
- Fields:
  - id (UUID)
  - ticketId (UUID)
  - originalFilename (string)
  - sanitizedFilename (string)
  - contentType (enum: image/jpeg, image/png, image/gif, image/webp)
  - sizeBytes (long)
  - filePath (string)
  - thumbnailPath (string)
  - createdAt (timestamp)
- Validation:
  - max 3 attachments per ticket
  - sizeBytes <= 5 * 1024 * 1024

### EscalationEvent
- Fields:
  - id (UUID)
  - ticketId (UUID)
  - level (enum: LEVEL_1, LEVEL_2, LEVEL_3)
  - triggeredAt (timestamp)
  - actionSummary (text)
  - incidentReportPath (string, nullable)

### Notification
- Fields:
  - id (UUID)
  - recipientUserId (UUID)
  - type (enum)
  - severity (enum: STANDARD, HIGH)
  - channels (set<IN_APP, EMAIL>)
  - title (string)
  - message (text)
  - deliveredAt (timestamp, nullable)
  - readAt (timestamp, nullable)
  - createdAt (timestamp)

### UtilizationSnapshot
- Fields:
  - id (UUID)
  - facilityId (UUID)
  - snapshotDate (date)
  - availableHours (decimal)
  - bookedHours (decimal)
  - utilizationPercent (decimal)
  - underutilized (boolean)
  - consecutiveUnderutilizedDays (integer)
- Validation:
  - utilizationPercent = (bookedHours / availableHours) * 100 when availableHours > 0

## State Transitions

### Booking status
- PENDING -> APPROVED
- PENDING -> REJECTED
- APPROVED -> CANCELLED
- Terminal: REJECTED, CANCELLED

### Maintenance ticket status
- OPEN -> IN_PROGRESS
- IN_PROGRESS -> RESOLVED
- RESOLVED -> CLOSED
- OPEN -> REJECTED
- IN_PROGRESS -> REJECTED
- Terminal: CLOSED, REJECTED

### Suspension appeal status
- SUBMITTED -> APPROVED
- SUBMITTED -> REJECTED

## Indexing and constraints
- Unique: user.email, user.googleSubject, facility.facilityCode, equipment.serialNumber.
- Booking overlap guard: index on (facility_id, booking_date, start_time, end_time).
- Ticket SLA queries: index on (priority, status, sla_due_at).
- Utilization queries: index on (facility_id, snapshot_date).
