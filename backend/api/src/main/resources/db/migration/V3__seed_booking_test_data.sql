-- Seed data for testing booking features (US2: Facility Discovery & Policy-Compliant Booking)
-- Includes test facilities, public holidays, quota policies, and booking scenarios
-- Date: April 2026

-- ============================================================================
-- Additional Test Facilities for Booking Feature Testing
-- ============================================================================

-- Add more test users for booking scenarios
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
    ('66666666-6666-6666-6666-666666666666', NULL, 'student2@smartcampus.edu', 'Kamal Jayasinghe', true, NULL, 0, '$2a$12$m87VhA68iLLOJgng0JKBLuQhPkc4Oz9xWcQ1RtD87a9PY3gTjolFa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('77777777-7777-7777-7777-777777777777', NULL, 'student3@smartcampus.edu', 'Shanelle Fernando', true, NULL, 0, '$2a$12$m87VhA68iLLOJgng0JKBLuQhPkc4Oz9xWcQ1RtD87a9PY3gTjolFa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('88888888-8888-8888-8888-888888888888', NULL, 'lecturer2@smartcampus.edu', 'Prof. Ruwandi Kularatne', true, NULL, 0, '$2a$12$m87VhA68iLLOJgng0JKBLuQhPkc4Oz9xWcQ1RtD87a9PY3gTjolFa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Add roles for new users
INSERT INTO public.user_roles (user_id, role) VALUES
    ('66666666-6666-6666-6666-666666666666', 'USER'),
    ('77777777-7777-7777-7777-777777777777', 'USER'),
    ('88888888-8888-8888-8888-888888888888', 'LECTURER')
ON CONFLICT DO NOTHING;

-- Add more facilities for booking testing
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
    -- Seminar Room: Small capacity for group discussions
    (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
        'SR-B205',
        'Seminar Room B205',
        'LECTURE_HALL',
        25,
        'B Block',
        'B',
        '2',
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
        true,
        true,
        false,
        true
    ),
    -- Auditorium: Large capacity for events
    (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
        'AUD-MAIN',
        'Main Auditorium',
        'LECTURE_HALL',
        500,
        'Main Building',
        'M',
        '0',
        'ACTIVE',
        '07:00:00',
        '22:00:00',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'LECTURE_HALL',
        'N/A',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        'Main Stage',
        true,
        true,
        true,
        true
    ),
    -- Advanced Lab: Computer facilities
    (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3',
        'LAB-D302',
        'Advanced Computing Lab D302',
        'LAB',
        50,
        'D Block',
        'D',
        '3',
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
        true,
        NULL,
        true
    ),
    -- Meeting Room: Mid-size for committees
    (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4',
        'MR-E101',
        'Committee Meeting Room E101',
        'LECTURE_HALL',
        15,
        'E Block',
        'E',
        '1',
        'ACTIVE',
        '08:00:00',
        '17:30:00',
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
        false,
        true,
        false,
        true
    )
ON CONFLICT DO NOTHING;

-- Add AV Equipment for new facilities
INSERT INTO public.lecture_hall_av_equipment (lecture_hall_id, av_equipment) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'Projector'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'Video Conference System'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'Projector'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'PA System'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'Lighting System'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4', 'Video Conference System')
ON CONFLICT DO NOTHING;

-- Add Software to new lab
INSERT INTO public.lab_software_list (lab_id, software_item) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'Visual Studio Code'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'Docker'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'Git'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'Java JDK')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- PUBLIC HOLIDAYS (Apr-May 2026)
-- ============================================================================

-- Create public_holiday table if it doesn't exist
CREATE TABLE IF NOT EXISTS public.public_holiday (
    id UUID PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    holiday_name VARCHAR(255) NOT NULL,
    country_code VARCHAR(2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO public.public_holiday (
    id,
    holiday_date,
    holiday_name,
    country_code,
    created_at
) VALUES
    ('99999999-9999-9999-9999-999999999991', '2026-04-14'::date, 'Sinhala & Tamil New Year', 'LK', CURRENT_TIMESTAMP),
    ('99999999-9999-9999-9999-999999999992', '2026-04-15'::date, 'Sinhala & Tamil New Year (Holiday)', 'LK', CURRENT_TIMESTAMP),
    ('99999999-9999-9999-9999-999999999993', '2026-05-01'::date, 'International Workers Day', 'LK', CURRENT_TIMESTAMP),
    ('99999999-9999-9999-9999-999999999994', '2026-05-27'::date, 'Poson Full Moon Poya Day', 'LK', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- TEST BOOKINGS (Non-Recurring)
-- ============================================================================

-- Approved booking (today's facility session)
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
    -- Approved seminar booking
    (
        'ffff0001-0001-0001-0001-000000000001',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
        '66666666-6666-6666-6666-666666666666',
        '66666666-6666-6666-6666-666666666666',
        CURRENT_DATE + INTERVAL '2 days',
        '10:00:00',
        '12:00:00',
        'Database Design Theory Discussion',
        20,
        'APPROVED',
        NULL,
        false,
        'Asia/Colombo',
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    -- Pending booking (to be approved)
    (
        'ffff0001-0001-0001-0001-000000000002',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3',
        '77777777-7777-7777-7777-777777777777',
        '77777777-7777-7777-7777-777777777777',
        CURRENT_DATE + INTERVAL '3 days',
        '14:00:00',
        '16:30:00',
        'Advanced SQL Optimization Workshop',
        40,
        'PENDING',
        NULL,
        false,
        'Asia/Colombo',
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    -- Future booking for quota testing
    (
        'ffff0001-0001-0001-0001-000000000003',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
        '55555555-5555-5555-5555-555555555555',
        '55555555-5555-5555-5555-555555555555',
        CURRENT_DATE + INTERVAL '5 days',
        '18:00:00',
        '20:00:00',
        'Student Cultural Evening',
        200,
        'PENDING',
        NULL,
        false,
        'Asia/Colombo',
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    -- Committee meeting (small group)
    (
        'ffff0001-0001-0001-0001-000000000004',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4',
        '88888888-8888-8888-8888-888888888888',
        '88888888-8888-8888-8888-888888888888',
        CURRENT_DATE + INTERVAL '7 days',
        '09:00:00',
        '10:30:00',
        'Academic Affairs Committee Meeting',
        12,
        'APPROVED',
        NULL,
        false,
        'Asia/Colombo',
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- TEST RECURRING BOOKINGS (Weekly Series)
-- ============================================================================

-- Master recurring booking: Weekly SQL sessions on Mon/Wed/Fri for 12 weeks
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
        'ffff0002-0002-0002-0002-000000000001',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3',
        '88888888-8888-8888-8888-888888888888',
        '88888888-8888-8888-8888-888888888888',
        CURRENT_DATE + INTERVAL '1 day',
        '09:00:00',
        '11:00:00',
        'Database Administration Course - Recurring Sessions',
        35,
        'APPROVED',
        'FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=12',
        true,
        'Asia/Colombo',
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    -- Master recurring booking: Daily standup meetings for 10 days
    (
        'ffff0002-0002-0002-0002-000000000002',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4',
        '22222222-2222-2222-2222-222222222222',
        '22222222-2222-2222-2222-222222222222',
        CURRENT_DATE + INTERVAL '1 day',
        '08:30:00',
        '09:00:00',
        'Daily Team Standup Meeting',
        10,
        'PENDING',
        'FREQ=DAILY;COUNT=10',
        true,
        'Asia/Colombo',
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- APPROVAL STEPS for test bookings
-- ============================================================================

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
    -- Approval for seminar booking
    (
        'aaaa0001-0001-0001-0001-000000000001',
        'ffff0001-0001-0001-0001-000000000001',
        1,
        'LECTURER',
        'APPROVED',
        '88888888-8888-8888-8888-888888888888',
        CURRENT_TIMESTAMP,
        'Approved for seminar use',
        CURRENT_TIMESTAMP
    ),
    -- Pending approval for DB workshop
    (
        'aaaa0001-0001-0001-0001-000000000002',
        'ffff0001-0001-0001-0001-000000000002',
        1,
        'LECTURER',
        'PENDING',
        NULL,
        NULL,
        'Awaiting lecturer approval',
        CURRENT_TIMESTAMP
    ),
    -- Pending approval for cultural event
    (
        'aaaa0001-0001-0001-0001-000000000003',
        'ffff0001-0001-0001-0001-000000000003',
        1,
        'FACILITY_MANAGER',
        'PENDING',
        NULL,
        NULL,
        'Awaiting facility manager approval for large event',
        CURRENT_TIMESTAMP
    ),
    -- Approval for committee meeting
    (
        'aaaa0001-0001-0001-0001-000000000004',
        'ffff0001-0001-0001-0001-000000000004',
        1,
        'ADMIN',
        'APPROVED',
        '11111111-1111-1111-1111-111111111111',
        CURRENT_TIMESTAMP,
        'Approved on behalf of admin committee',
        CURRENT_TIMESTAMP
    ),
    -- Approval for recurring DB course master
    (
        'aaaa0002-0002-0002-0002-000000000001',
        'ffff0002-0002-0002-0002-000000000001',
        1,
        'ADMIN',
        'APPROVED',
        '11111111-1111-1111-1111-111111111111',
        CURRENT_TIMESTAMP,
        'Approved recurring DB admin course series',
        CURRENT_TIMESTAMP
    ),
    -- Pending approval for daily standup master
    (
        'aaaa0002-0002-0002-0002-000000000002',
        'ffff0002-0002-0002-0002-000000000002',
        1,
        'ADMIN',
        'PENDING',
        NULL,
        NULL,
        'Awaiting admin approval for recurring daily standups',
        CURRENT_TIMESTAMP
    )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- UTILIZATION SNAPSHOTS (for analytics testing)
-- ============================================================================

INSERT INTO public.utilization_snapshots (
    id,
    facility_id,
    snapshot_date,
    available_hours,
    booked_hours,
    utilization_percent,
    consecutive_underutilized_days,
    underutilized
) VALUES
    -- Good utilization: 65%
    (
        'dddd0001-0001-0001-0001-000000000001',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
        CURRENT_DATE - INTERVAL '1 day',
        3.5,
        6.5,
        65.0,
        0,
        false
    ),
    -- Underutilized: 25%
    (
        'dddd0001-0001-0001-0001-000000000002',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4',
        CURRENT_DATE - INTERVAL '1 day',
        7.5,
        2.5,
        25.0,
        1,
        true
    ),
    -- Good utilization: 80%
    (
        'dddd0001-0001-0001-0001-000000000003',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3',
        CURRENT_DATE - INTERVAL '1 day',
        2.0,
        8.0,
        80.0,
        0,
        false
    ),
    -- Underutilized: 30%
    (
        'dddd0001-0001-0001-0001-000000000004',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4',
        CURRENT_DATE - INTERVAL '1 day',
        10.5,
        4.5,
        30.0,
        1,
        true
    )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- INSERT NOTIFICATIONS for testing (in-app notification system)
-- ============================================================================

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
    created_at,
    delivered_at
) VALUES
    -- Sample booking approval notification
    (
        'eeee0001-0001-0001-0001-000000000001',
        '66666666-6666-6666-6666-666666666666',
        'BOOKING_APPROVED',
        'STANDARD',
        'Booking Approved',
        'Your seminar room booking for April 16 09:00-11:00 has been approved.',
        'booking:ffff0001-0001-0001-0001-000000000001',
        '/bookings/ffff0001-0001-0001-0001-000000000001',
        'View Booking',
        'evt-001',
        CURRENT_TIMESTAMP - INTERVAL '4 hours',
        CURRENT_TIMESTAMP - INTERVAL '3 hours'
    ),
    -- Sample conflict warning
    (
        'eeee0001-0001-0001-0001-000000000002',
        '77777777-7777-7777-7777-777777777777',
        'BOOKING_CONFLICT_WARNING',
        'HIGH',
        'Booking Time Conflict Detected',
        'The lab you requested (D302) has a scheduling conflict on April 18 14:00. Please choose an alternate time.',
        'facility:bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3',
        '/facilities',
        'Browse Alternatives',
        'evt-002',
        CURRENT_TIMESTAMP - INTERVAL '2 hours',
        CURRENT_TIMESTAMP - INTERVAL '1 hour'
    ),
    -- Sample pod recommendation
    (
        'eeee0001-0001-0001-0001-000000000003',
        '55555555-5555-5555-5555-555555555555',
        'FACILITY_RECOMMENDATION',
        'STANDARD',
        'Alternative Facility Suggested',
        'Based on your request, we recommend Auditorium Main (500 capacity) for your event on April 19.',
        'facility:bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
        '/facilities/bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
        'View Facility',
        'evt-003',
        CURRENT_TIMESTAMP - INTERVAL '6 hours',
        CURRENT_TIMESTAMP - INTERVAL '5 hours'
    )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- Add notification channels (IN_APP for all test notifications)
-- ============================================================================

INSERT INTO public.notification_channels (notification_id, channel) VALUES
    ('eeee0001-0001-0001-0001-000000000001', 'IN_APP'),
    ('eeee0001-0001-0001-0001-000000000002', 'IN_APP'),
    ('eeee0001-0001-0001-0001-000000000003', 'IN_APP')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- Summary for QA Testing
-- ============================================================================

/*
TEST ACCOUNTS (password: YourPassword123!):
- admin@smartcampus.edu (ADMIN)
- lecturer@smartcampus.edu (LECTURER) 
- lecturer2@smartcampus.edu (LECTURER)
- facility.manager@smartcampus.edu (FACILITY_MANAGER)
- student@smartcampus.edu (USER)
- student2@smartcampus.edu (USER)
- student3@smartcampus.edu (USER)
- tech@smartcampus.edu (TECHNICIAN)

TEST FACILITIES:
1. Main Lecture Hall A101 (120 capacity) - 08:00-18:00
2. Computing Lab C203 (40 capacity) - 08:00-20:00
3. Seminar Room B205 (25 capacity) - 08:00-18:00
4. Main Auditorium (500 capacity) - 07:00-22:00
5. Advanced Computing Lab D302 (50 capacity) - 08:00-20:00
6. Committee Meeting Room E101 (15 capacity) - 08:00-17:30

TEST SCENARIOS:
- Approved booking: Seminar on April 16 (10:00-12:00) - 20 attendees
- Pending booking: DB Workshop on April 17 (14:00-16:30) - 40 attendees  
- Pending booking: Cultural Event on April 19 (18:00-20:00) - 200 attendees
- Recurring series: Weekly DB Course (Mon/Wed/Fri 09:00-11:00) 12 weeks
- Recurring series: Daily standup meetings (08:30-09:00) 10 days

PUBLIC HOLIDAYS IN APRIL-MAY 2026:
- April 14-15: Sinhala & Tamil New Year
- May 1: International Workers Day
- May 27: Poson Full Moon Poya Day

QUOTA POLICIES:
- Advance booking window: 30 days
- Peak hours: 12:00-14:00
- Weekly quota per user: 10 hours
- Monthly quota per user: 40 hours
*/
