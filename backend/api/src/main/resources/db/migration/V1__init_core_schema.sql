-- Smart Campus Operations Hub - Core Schema Migration
-- Initial database schema for all core entities

-- Create role enum type
CREATE TYPE user_role_enum AS ENUM ('STUDENT', 'LECTURER', 'FACILITY_MANAGER', 'MAINTENANCE_STAFF', 'ADMIN');

-- Create facility type enum
CREATE TYPE facility_type_enum AS ENUM ('LECTURE_HALL', 'LAB', 'MEETING_ROOM', 'AUDITORIUM', 'EQUIPMENT', 'SPORTS_FACILITY');

-- Create facility status enum
CREATE TYPE facility_status_enum AS ENUM ('ACTIVE', 'OUT_OF_SERVICE');

-- Create booking status enum
CREATE TYPE booking_status_enum AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED');

-- Create approval decision enum
CREATE TYPE approval_decision_enum AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

-- Create check-in method enum
CREATE TYPE checkin_method_enum AS ENUM ('QR', 'MANUAL');

-- Create suspension appeal status enum
CREATE TYPE suspension_appeal_status_enum AS ENUM ('SUBMITTED', 'APPROVED', 'REJECTED');

-- Create ticket category enum
CREATE TYPE ticket_category_enum AS ENUM ('ELECTRICAL', 'PLUMBING', 'HVAC', 'IT_NETWORKING', 'STRUCTURAL', 'CLEANING', 'SAFETY', 'OTHER');

-- Create ticket priority enum
CREATE TYPE ticket_priority_enum AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');

-- Create ticket status enum
CREATE TYPE ticket_status_enum AS ENUM ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED');

-- Create comment visibility enum
CREATE TYPE comment_visibility_enum AS ENUM ('PUBLIC', 'INTERNAL');

-- Create escalation level enum
CREATE TYPE escalation_level_enum AS ENUM ('LEVEL_1', 'LEVEL_2', 'LEVEL_3');

-- Create notification type enum
CREATE TYPE notification_type_enum AS ENUM ('BOOKING_APPROVED', 'BOOKING_REJECTED', 'BOOKING_REMINDER', 'TICKET_UPDATE', 'APPROVAL_REQUEST', 'ESCALATION_ALERT', 'SUSPENSION_NOTICE', 'APPEAL_DECISION', 'MAINTENANCE_UPDATE');

-- Create notification severity enum
CREATE TYPE notification_severity_enum AS ENUM ('STANDARD', 'HIGH');

-- Create notification channels array type
CREATE TYPE notification_channel AS ENUM ('IN_APP', 'EMAIL');

-- ============================================================
-- USER TABLE
-- ============================================================
CREATE TABLE "user" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    google_subject VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT true NOT NULL,
    suspended_until TIMESTAMP NULL,
    no_show_count INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- USER_ROLES junction table for many-to-many relationship
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role user_role_enum NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

-- Create indexes for user
CREATE INDEX idx_user_email ON "user"(email);
CREATE INDEX idx_user_google_subject ON "user"(google_subject);
CREATE INDEX idx_user_active ON "user"(active);
CREATE INDEX idx_user_suspended_until ON "user"(suspended_until);

-- ============================================================
-- FACILITY TABLE (SINGLE_TABLE INHERITANCE)
-- ============================================================
CREATE TABLE facility (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    type facility_type_enum NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    location VARCHAR(255) NOT NULL,
    building VARCHAR(100),
    floor VARCHAR(50),
    status facility_status_enum DEFAULT 'ACTIVE' NOT NULL,
    availability_start TIME NOT NULL,
    availability_end TIME NOT NULL CHECK (availability_start < availability_end),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    -- SINGLE_TABLE inheritance discriminator and subtype fields
    disc_type VARCHAR(50),
    -- LectureHall fields
    av_equipment TEXT,
    wheelchair_accessible BOOLEAN,
    -- Lab fields
    lab_type VARCHAR(100),
    software_list TEXT,
    safety_equipment TEXT,
    -- MeetingRoom fields
    av_enabled BOOLEAN,
    catering_allowed BOOLEAN,
    -- Auditorium fields
    stage_type VARCHAR(100),
    sound_system VARCHAR(100),
    -- Equipment fields
    brand VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(255) UNIQUE,
    maintenance_schedule TEXT,
    -- SportsFacility fields
    sports_type VARCHAR(100),
    equipment_available TEXT
);

-- Create indexes for facility
CREATE INDEX idx_facility_code ON facility(facility_code);
CREATE INDEX idx_facility_type ON facility(type);
CREATE INDEX idx_facility_status ON facility(status);
CREATE INDEX idx_facility_building_floor ON facility(building, floor);

-- ============================================================
-- BOOKING TABLE
-- ============================================================
CREATE TABLE booking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL,
    requested_by_user_id UUID NOT NULL,
    booked_for_user_id UUID NOT NULL,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL CHECK (start_time < end_time),
    purpose TEXT NOT NULL,
    attendees INTEGER NOT NULL CHECK (attendees > 0),
    status booking_status_enum DEFAULT 'PENDING' NOT NULL,
    recurrence_rule TEXT,
    is_recurring_master BOOLEAN DEFAULT false NOT NULL,
    timezone VARCHAR(100) DEFAULT 'Asia/Colombo' NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (facility_id) REFERENCES facility(id) ON DELETE RESTRICT,
    FOREIGN KEY (requested_by_user_id) REFERENCES "user"(id) ON DELETE RESTRICT,
    FOREIGN KEY (booked_for_user_id) REFERENCES "user"(id) ON DELETE RESTRICT
);

-- Create indexes for booking
CREATE INDEX idx_booking_facility_date_time ON booking(facility_id, booking_date, start_time, end_time);
CREATE INDEX idx_booking_status ON booking(status);
CREATE INDEX idx_booking_requested_by ON booking(requested_by_user_id);
CREATE INDEX idx_booking_booked_for ON booking(booked_for_user_id);
CREATE INDEX idx_booking_date ON booking(booking_date);

-- ============================================================
-- APPROVAL_STEP TABLE
-- ============================================================
CREATE TABLE approval_step (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    step_order INTEGER NOT NULL CHECK (step_order >= 1),
    approver_role user_role_enum NOT NULL,
    decision approval_decision_enum DEFAULT 'PENDING' NOT NULL,
    decided_by_user_id UUID,
    decided_at TIMESTAMP,
    note TEXT,
    FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE,
    FOREIGN KEY (decided_by_user_id) REFERENCES "user"(id) ON DELETE SET NULL
);

-- Create indexes for approval_step
CREATE INDEX idx_approval_step_booking ON approval_step(booking_id);
CREATE INDEX idx_approval_step_decision ON approval_step(decision);
CREATE INDEX idx_approval_step_approver_role ON approval_step(approver_role);

-- ============================================================
-- CHECK_IN_RECORD TABLE
-- ============================================================
CREATE TABLE check_in_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    method checkin_method_enum NOT NULL,
    checked_in_by_user_id UUID,
    checked_in_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE,
    FOREIGN KEY (checked_in_by_user_id) REFERENCES "user"(id) ON DELETE SET NULL
);

-- Create indexes for check_in_record
CREATE INDEX idx_check_in_record_booking ON check_in_record(booking_id);
CREATE INDEX idx_check_in_record_timestamp ON check_in_record(checked_in_at);

-- ============================================================
-- SUSPENSION_APPEAL TABLE
-- ============================================================
CREATE TABLE suspension_appeal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    reason TEXT NOT NULL,
    status suspension_appeal_status_enum DEFAULT 'SUBMITTED' NOT NULL,
    reviewed_by_user_id UUID,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by_user_id) REFERENCES "user"(id) ON DELETE SET NULL
);

-- Create indexes for suspension_appeal
CREATE INDEX idx_suspension_appeal_user ON suspension_appeal(user_id);
CREATE INDEX idx_suspension_appeal_status ON suspension_appeal(status);

-- ============================================================
-- MAINTENANCE_TICKET TABLE
-- ============================================================
CREATE TABLE maintenance_ticket (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    assigned_technician_user_id UUID,
    category ticket_category_enum NOT NULL,
    priority ticket_priority_enum NOT NULL,
    status ticket_status_enum DEFAULT 'OPEN' NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    sla_due_at TIMESTAMP NOT NULL,
    escalation_level INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    closed_at TIMESTAMP,
    FOREIGN KEY (facility_id) REFERENCES facility(id) ON DELETE RESTRICT,
    FOREIGN KEY (created_by_user_id) REFERENCES "user"(id) ON DELETE RESTRICT,
    FOREIGN KEY (assigned_technician_user_id) REFERENCES "user"(id) ON DELETE SET NULL
);

-- Create indexes for maintenance_ticket
CREATE INDEX idx_ticket_facility ON maintenance_ticket(facility_id);
CREATE INDEX idx_ticket_status ON maintenance_ticket(status);
CREATE INDEX idx_ticket_priority_status_sla ON maintenance_ticket(priority, status, sla_due_at);
CREATE INDEX idx_ticket_created_by ON maintenance_ticket(created_by_user_id);
CREATE INDEX idx_ticket_assigned_to ON maintenance_ticket(assigned_technician_user_id);

-- ============================================================
-- TICKET_COMMENT TABLE
-- ============================================================
CREATE TABLE ticket_comment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL,
    author_user_id UUID NOT NULL,
    body TEXT NOT NULL,
    visibility comment_visibility_enum DEFAULT 'PUBLIC' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES maintenance_ticket(id) ON DELETE CASCADE,
    FOREIGN KEY (author_user_id) REFERENCES "user"(id) ON DELETE RESTRICT
);

-- Create indexes for ticket_comment
CREATE INDEX idx_ticket_comment_ticket ON ticket_comment(ticket_id);
CREATE INDEX idx_ticket_comment_visibility ON ticket_comment(visibility);
CREATE INDEX idx_ticket_comment_author ON ticket_comment(author_user_id);

-- ============================================================
-- TICKET_ATTACHMENT TABLE
-- ============================================================
CREATE TABLE ticket_attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    sanitized_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes <= 5242880),
    file_path TEXT NOT NULL,
    thumbnail_path TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (ticket_id) REFERENCES maintenance_ticket(id) ON DELETE CASCADE
);

-- Create indexes for ticket_attachment
CREATE INDEX idx_ticket_attachment_ticket ON ticket_attachment(ticket_id);
CREATE INDEX idx_ticket_attachment_created ON ticket_attachment(created_at);

-- Constraint: max 3 attachments per ticket (enforced in application layer)

-- ============================================================
-- ESCALATION_EVENT TABLE
-- ============================================================
CREATE TABLE escalation_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL,
    level escalation_level_enum NOT NULL,
    triggered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    action_summary TEXT,
    incident_report_path TEXT,
    FOREIGN KEY (ticket_id) REFERENCES maintenance_ticket(id) ON DELETE CASCADE
);

-- Create indexes for escalation_event
CREATE INDEX idx_escalation_event_ticket ON escalation_event(ticket_id);
CREATE INDEX idx_escalation_event_level ON escalation_event(level);
CREATE INDEX idx_escalation_event_triggered ON escalation_event(triggered_at);

-- ============================================================
-- NOTIFICATION TABLE
-- ============================================================
CREATE TABLE notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL,
    type notification_type_enum NOT NULL,
    severity notification_severity_enum DEFAULT 'STANDARD' NOT NULL,
    channels notification_channel[] DEFAULT ARRAY['IN_APP']::notification_channel[] NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (recipient_user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

-- Create indexes for notification
CREATE INDEX idx_notification_recipient ON notification(recipient_user_id);
CREATE INDEX idx_notification_type ON notification(type);
CREATE INDEX idx_notification_severity ON notification(severity);
CREATE INDEX idx_notification_read ON notification(read_at);
CREATE INDEX idx_notification_created ON notification(created_at);

-- ============================================================
-- UTILIZATION_SNAPSHOT TABLE
-- ============================================================
CREATE TABLE utilization_snapshot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL,
    snapshot_date DATE NOT NULL,
    available_hours DECIMAL(10, 2) NOT NULL CHECK (available_hours >= 0),
    booked_hours DECIMAL(10, 2) NOT NULL CHECK (booked_hours >= 0),
    utilization_percent DECIMAL(5, 2) NOT NULL DEFAULT 0 CHECK (utilization_percent >= 0 AND utilization_percent <= 100),
    underutilized BOOLEAN DEFAULT false NOT NULL,
    consecutive_underutilized_days INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (facility_id) REFERENCES facility(id) ON DELETE CASCADE,
    UNIQUE(facility_id, snapshot_date)
);

-- Create indexes for utilization_snapshot
CREATE INDEX idx_utilization_snapshot_facility_date ON utilization_snapshot(facility_id, snapshot_date);
CREATE INDEX idx_utilization_snapshot_underutilized ON utilization_snapshot(underutilized, facility_id);
CREATE INDEX idx_utilization_snapshot_facility_recent ON utilization_snapshot(facility_id, snapshot_date DESC);

-- ============================================================
-- MIGRATION METADATA
-- ============================================================
-- This migration creates the core schema with:
-- - User and role management (many-to-many relationship)
-- - Facility management with SINGLE_TABLE inheritance
-- - Booking with optimistic locking (version column)
-- - Approval workflow
-- - Check-in tracking
-- - Suspension appeals
-- - Maintenance ticketing with escalation
-- - Comments with visibility control
-- - Attachments with size constraints
-- - Notifications with multiple channels
-- - Utilization snapshots for analytics
-- - Appropriate indexes for query performance
-- - Referential integrity with CASCADE/RESTRICT/SET NULL policies
-- - Check constraints for data validation
