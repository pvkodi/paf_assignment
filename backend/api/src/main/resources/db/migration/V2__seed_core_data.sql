-- Seed data for local/dev environments.
-- Updated to support email/password authentication (V3 migration)
-- Uses stable UUIDs so related rows can reference each other.

-- ============================================================================
-- TEST CREDENTIALS
-- ============================================================================
-- All test users use password: YourPassword123!
-- Bcrypt hash (strength 12): $2a$12$m87VhA68iLLOJgng0JKBLuQhPkc4Oz9xWcQ1RtD87a9PY3gTjolFa
--
-- Credentials:
-- 1. admin@smartcampus.edu / YourPassword123! (ADMIN)
-- 2. lecturer@smartcampus.edu / YourPassword123! (LECTURER)
-- 3. tech@smartcampus.edu / YourPassword123! (TECHNICIAN)
-- 4. facility.manager@smartcampus.edu / YourPassword123! (FACILITY_MANAGER)
-- 5. student@smartcampus.edu / YourPassword123! (USER)
--
-- To generate bcrypt hashes for your own passwords:
-- Online: https://bcrypt-generator.com/
-- Node.js: node -e "console.log(require('bcryptjs').hashSync('YOUR-PASSWORD', 10))"
-- ============================================================================

-- Users with email/password authentication
INSERT INTO public."user" (
    id,
    google_subject,
    email,
    display_name,
    active,
    suspended_until,
    no_show_count,
    password_hash,
    created_at,
    updated_at
) VALUES
    ('11111111-1111-1111-1111-111111111111', NULL, 'admin@smartcampus.edu', 'System Admin', true, NULL, 0, '$2a$12$m87VhA68iLLOJgng0JKBLuQhPkc4Oz9xWcQ1RtD87a9PY3gTjolFa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222222', NULL, 'lecturer@smartcampus.edu', 'Dr. Nimal Perera', true, NULL, 0, '$2a$12$m87VhA68iLLOJgng0JKBLuQhPkc4Oz9xWcQ1RtD87a9PY3gTjolFa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333333', NULL, 'tech@smartcampus.edu', 'S. Fernando', true, NULL, 0, '$2a$12$m87VhA68iLLOJgng0JKBLuQhPkc4Oz9xWcQ1RtD87a9PY3gTjolFa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('44444444-4444-4444-4444-444444444444', NULL, 'facility.manager@smartcampus.edu', 'A. Jayasekara', true, NULL, 0, '$2a$12$m87VhA68iLLOJgng0JKBLuQhPkc4Oz9xWcQ1RtD87a9PY3gTjolFa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('55555555-5555-5555-5555-555555555555', NULL, 'student@smartcampus.edu', 'Pamali Student', true, NULL, 1, '$2a$12$m87VhA68iLLOJgng0JKBLuQhPkc4Oz9xWcQ1RtD87a9PY3gTjolFa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- User Roles
INSERT INTO public.user_roles (user_id, role) VALUES
    ('11111111-1111-1111-1111-111111111111', 'ADMIN'),
    ('22222222-2222-2222-2222-222222222222', 'LECTURER'),
    ('33333333-3333-3333-3333-333333333333', 'TECHNICIAN'),
    ('44444444-4444-4444-4444-444444444444', 'FACILITY_MANAGER'),
    ('55555555-5555-5555-5555-555555555555', 'USER')
ON CONFLICT DO NOTHING;

-- Facilities
INSERT INTO public.facility (
    id,
    facility_code,
    name,
    type,
    capacity,
    location,
    building,
    floor,
    status,
    availability_start,
    availability_end,
    created_at,
    updated_at,
    facility_type,
    brand,
    model,
    serial_number,
    maintenance_schedule,
    lab_type,
    sports_type,
    stage_type,
    sound_system,
    av_enabled,
    catering_allowed,
    wheelchair_accessible
) VALUES
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
        'LH-A101',
        'Main Lecture Hall A101',
        'LECTURE_HALL',
        120,
        'A Block',
        'A',
        '1',
        'ACTIVE',
        '08:00:00',
        '18:00:00',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'LECTURE_HALL',
        'N/A',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        true
    ),
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
        'LAB-C203',
        'Computing Lab C203',
        'LAB',
        40,
        'C Block',
        'C',
        '2',
        'ACTIVE',
        '08:00:00',
        '20:00:00',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'LAB',
        'N/A',
        NULL,
        NULL,
        NULL,
        'Computer',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        false
    ),
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3',
        'EQ-PRJ-01',
        'Portable Projector - Epson',
        'EQUIPMENT',
        1,
        'Equipment Store',
        'Admin',
        'G',
        'ACTIVE',
        '08:00:00',
        '17:00:00',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'EQUIPMENT',
        'Epson',
        'EB-X06',
        'SN-PRJ-0001',
        'Quarterly service',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        false
    ),
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4',
        'SP-IND-01',
        'Indoor Sports Court',
        'SPORTS_FACILITY',
        60,
        'Sports Complex',
        'S',
        '1',
        'ACTIVE',
        '06:00:00',
        '21:00:00',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'SPORTS_FACILITY',
        'N/A',
        NULL,
        NULL,
        NULL,
        NULL,
        'Badminton',
        NULL,
        NULL,
        NULL,
        NULL,
        false
    )
ON CONFLICT DO NOTHING;

-- Facility AV Equipment
INSERT INTO public.lecture_hall_av_equipment (lecture_hall_id, av_equipment) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'Projector'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'PA System')
ON CONFLICT DO NOTHING;

-- Lab Software
INSERT INTO public.lab_software_list (lab_id, software_item) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'IntelliJ IDEA'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'PostgreSQL Client')
ON CONFLICT DO NOTHING;

-- Lab Safety Equipment
INSERT INTO public.lab_safety_equipment (lab_id, safety_equipment) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'Fire Extinguisher'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'First Aid Kit')
ON CONFLICT DO NOTHING;

-- Sports Facility Equipment
INSERT INTO public.sports_facility_equipment_available (sports_facility_id, equipment_item) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', 'Badminton Nets'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', 'Shuttlecocks')
ON CONFLICT DO NOTHING;

-- Bookings
INSERT INTO public.booking (
    id,
    facility_id,
    requested_by_user_id,
    booked_for_user_id,
    booking_date,
    start_time,
    end_time,
    purpose,
    attendees,
    status,
    recurrence_rule,
    is_recurring_master,
    timezone,
    version,
    created_at,
    updated_at
) VALUES
    (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
        '55555555-5555-5555-5555-555555555555',
        '55555555-5555-5555-5555-555555555555',
        CURRENT_DATE + INTERVAL '1 day',
        '09:00:00',
        '11:00:00',
        'Student seminar preparation',
        30,
        'PENDING',
        NULL,
        false,
        'Asia/Colombo',
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
        '22222222-2222-2222-2222-222222222222',
        '22222222-2222-2222-2222-222222222222',
        CURRENT_DATE,
        '10:00:00',
        '12:00:00',
        'Database practical session',
        35,
        'APPROVED',
        NULL,
        false,
        'Asia/Colombo',
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT DO NOTHING;

-- Approval Steps
INSERT INTO public.approval_step (
    id,
    booking_id,
    step_order,
    approver_role,
    decision,
    decided_by_user_id,
    decided_at,
    note,
    created_at
) VALUES
    (
        'cccccccc-cccc-cccc-cccc-ccccccccccc1',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
        1,
        'LECTURER',
        'PENDING',
        NULL,
        NULL,
        'Waiting for lecturer approval',
        CURRENT_TIMESTAMP
    ),
    (
        'cccccccc-cccc-cccc-cccc-ccccccccccc2',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
        1,
        'LECTURER',
        'APPROVED',
        '22222222-2222-2222-2222-222222222222',
        CURRENT_TIMESTAMP,
        'Approved for lab practical',
        CURRENT_TIMESTAMP
    )
ON CONFLICT DO NOTHING;

-- Check-ins
INSERT INTO public.check_in (
    id,
    booking_id,
    method,
    checked_in_by_user_id,
    checked_in_at,
    created_at
) VALUES
    (
        'dddddddd-dddd-dddd-dddd-ddddddddddd1',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
        'MANUAL',
        '33333333-3333-3333-3333-333333333333',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT DO NOTHING;

-- Tickets
INSERT INTO public.maintenance_ticket (
    id,
    facility_id,
    category,
    priority,
    status,
    title,
    description,
    created_by_user_id,
    assigned_technician_user_id,
    created_at,
    updated_at,
    sla_due_at,
    escalation_level
) VALUES
    (
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
        'IT_NETWORKING',
        'HIGH',
        'IN_PROGRESS',
        'Lab network switch intermittent packet loss',
        'Users in C203 are reporting unstable connectivity during practical sessions. Requesting urgent diagnosis and fix.',
        '22222222-2222-2222-2222-222222222222',
        '33333333-3333-3333-3333-333333333333',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP + INTERVAL '8 hours',
        1
    )
ON CONFLICT DO NOTHING;

-- Ticket Comments
INSERT INTO public.ticket_comment (
    id,
    ticket_id,
    author_user_id,
    content,
    visibility,
    created_at,
    updated_at,
    deleted_at
) VALUES
    (
        'f1111111-1111-1111-1111-111111111111',
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1',
        '33333333-3333-3333-3333-333333333333',
        'Initial assessment completed. Fault appears to be in the edge switch uplink port.',
        'INTERNAL',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        NULL
    )
ON CONFLICT DO NOTHING;

-- Ticket Attachments
INSERT INTO public.ticket_attachment (
    id,
    ticket_id,
    uploaded_by_user_id,
    file_name,
    file_size,
    mime_type,
    file_path,
    thumbnail_path,
    checksum_hash,
    uploaded_at
) VALUES
    (
        'f2222222-2222-2222-2222-222222222222',
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1',
        '33333333-3333-3333-3333-333333333333',
        'switch-port-errors.png',
        24576,
        'image/png',
        '/app/uploads/original/switch-port-errors.png',
        '/app/uploads/thumbnails/switch-port-errors.jpg',
        '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef',
        CURRENT_TIMESTAMP
    )
ON CONFLICT DO NOTHING;

-- Ticket Escalations
INSERT INTO public.ticket_escalation (
    id,
    ticket_id,
    from_level,
    to_level,
    escalated_by_user_id,
    escalated_at,
    escalation_reason,
    previous_assignee_user_id,
    new_assignee_user_id,
    notes
) VALUES
    (
        'f3333333-3333-3333-3333-333333333333',
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1',
        0,
        1,
        '11111111-1111-1111-1111-111111111111',
        CURRENT_TIMESTAMP,
        'SLA approaching for high-priority incident',
        '33333333-3333-3333-3333-333333333333',
        '33333333-3333-3333-3333-333333333333',
        'Escalated for active monitoring'
    )
ON CONFLICT DO NOTHING;

-- Suspension Appeals
INSERT INTO public.suspension_appeal (
    id,
    user_id,
    reason,
    status,
    reviewed_by_user_id,
    reviewed_at,
    decision,
    created_at,
    updated_at
) VALUES
    (
        'f4444444-4444-4444-4444-444444444444',
        '55555555-5555-5555-5555-555555555555',
        'Missed check-in due to timetable conflict and requests suspension waiver.',
        'SUBMITTED',
        NULL,
        NULL,
        NULL,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT DO NOTHING;

-- Notifications
INSERT INTO public.notifications (
    id,
    recipient_user_id,
    event_type,
    severity,
    title,
    message,
    entity_reference,
    action_url,
    action_label,
    event_id,
    delivered_at,
    read_at,
    created_at
) VALUES
    (
        'f5555555-5555-5555-5555-555555555555',
        '55555555-5555-5555-5555-555555555555',
        'BOOKING_REQUEST_SUBMITTED',
        'STANDARD',
        'Booking Submitted',
        'Your booking request was submitted and is awaiting approval.',
        'booking:bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
        '/bookings/bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
        'View Booking',
        'evt-booking-0001',
        CURRENT_TIMESTAMP,
        NULL,
        CURRENT_TIMESTAMP
    ),
    (
        'f5555555-5555-5555-5555-555555555556',
        '33333333-3333-3333-3333-333333333333',
        'TICKET_ESCALATED',
        'HIGH',
        'Ticket Escalated',
        'A high-priority ticket has been escalated for immediate attention.',
        'ticket:eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1',
        '/tickets/eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1',
        'View Ticket',
        'evt-ticket-0001',
        CURRENT_TIMESTAMP,
        NULL,
        CURRENT_TIMESTAMP
    )
ON CONFLICT DO NOTHING;

-- Notification Channels
INSERT INTO public.notification_channels (notification_id, channel) VALUES
    ('f5555555-5555-5555-5555-555555555555', 'IN_APP'),
    ('f5555555-5555-5555-5555-555555555556', 'IN_APP'),
    ('f5555555-5555-5555-5555-555555555556', 'EMAIL')
ON CONFLICT DO NOTHING;

-- Utilization Snapshots
INSERT INTO public.utilization_snapshots (
    id,
    facility_id,
    snapshot_date,
    available_hours,
    booked_hours,
    utilization_percent,
    underutilized,
    consecutive_underutilized_days
) VALUES
    (
        'f6666666-6666-6666-6666-666666666666',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
        CURRENT_DATE - INTERVAL '1 day',
        10.00,
        7.50,
        75.00,
        false,
        0
    ),
    (
        'f6666666-6666-6666-6666-666666666667',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4',
        CURRENT_DATE - INTERVAL '1 day',
        12.00,
        2.00,
        16.67,
        true,
        3
    )
ON CONFLICT DO NOTHING;
