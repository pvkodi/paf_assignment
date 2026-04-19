# SQL Setup Scripts - Phase 2: Master Data

## HOW TO RUN
1. Open psql client: `psql -U smartcampus -d smartcampus -h localhost`
2. Copy/paste each section below
3. Verify data with SELECT queries provided

---

## SECTION 1: CREATE TEST USERS & ROLES

```sql
-- Create test users with different roles
INSERT INTO "user" (id, google_subject, email, display_name, active, suspended_until, no_show_count)
VALUES 
  ('00000001-0000-0000-0000-000000000001', 'sub_admin_001', 'admin@campus.edu', 'Admin User', true, NULL, 0),
  ('00000001-0000-0000-0000-000000000002', 'sub_lecturer_001', 'lecturer@campus.edu', 'Dr. James Lecturer', true, NULL, 0),
  ('00000001-0000-0000-0000-000000000003', 'sub_student_001', 'student@campus.edu', 'Alice Student', true, NULL, 0),
  ('00000001-0000-0000-0000-000000000004', 'sub_facmgr_001', 'facmgr@campus.edu', 'Bob Facility Manager', true, NULL, 0),
  ('00000001-0000-0000-0000-000000000005', 'sub_tech_001', 'technician@campus.edu', 'Charlie Technician', true, NULL, 0),
  ('00000001-0000-0000-0000-000000000006', 'sub_student_002', 'student2@campus.edu', 'David Student', true, NULL, 0)
ON CONFLICT (id) DO NOTHING;

-- Assign roles
INSERT INTO user_roles (user_id, role) 
VALUES
  ('00000001-0000-0000-0000-000000000001', 'ADMIN'),
  ('00000001-0000-0000-0000-000000000002', 'LECTURER'),
  ('00000001-0000-0000-0000-000000000003', 'STUDENT'),
  ('00000001-0000-0000-0000-000000000004', 'FACILITY_MANAGER'),
  ('00000001-0000-0000-0000-000000000005', 'MAINTENANCE_STAFF'),
  ('00000001-0000-0000-0000-000000000006', 'STUDENT')
ON CONFLICT DO NOTHING;

-- VERIFY: Check users created
SELECT u.id, u.email, u.display_name, array_agg(r.role) as roles
FROM "user" u
LEFT JOIN user_roles r ON u.id = r.user_id
GROUP BY u.id, u.email, u.display_name
ORDER BY u.email;
```

---

## SECTION 2: CREATE TEST FACILITIES

```sql
-- Create meeting room (capacity 20) - for testing peak-hour restrictions
INSERT INTO facility 
(id, facility_code, name, type, capacity, location, building, floor, status, availability_start, availability_end)
VALUES
  ('10000001-0000-0000-0000-000000000001', 'LH-101', 'Main Lecture Hall', 'LECTURE_HALL', 150, 'Building A', 'A', '1', 'ACTIVE', '08:00', '20:00'),
  ('10000001-0000-0000-0000-000000000002', 'LAB-B201', 'Computer Lab B', 'LAB', 50, 'Building B', 'B', '2', 'ACTIVE', '08:00', '18:00'),
  ('10000001-0000-0000-0000-000000000003', 'MR-C101', 'Meeting Room 1', 'MEETING_ROOM', 20, 'Building C', 'C', '1', 'ACTIVE', '08:00', '18:00'),
  ('10000001-0000-0000-0000-000000000004', 'AUD-D001', 'Main Auditorium', 'AUDITORIUM', 500, 'Building D', 'D', '1', 'ACTIVE', '08:00', '22:00'),
  ('10000001-0000-0000-0000-000000000005', 'SF-E101', 'Sports Facility', 'SPORTS_FACILITY', 200, 'Building E', 'E', '1', 'ACTIVE', '07:00', '21:00')
ON CONFLICT (facility_code) DO NOTHING;

-- VERIFY: Check facilities created
SELECT id, facility_code, name, type, capacity, status
FROM facility
ORDER BY facility_code;
```

---

## SECTION 3: CREATE SUSPENDED USER (FOR APPEALS TESTING)

```sql
-- Create a suspended user to test appeals workflow
INSERT INTO "user" (id, google_subject, email, display_name, active, suspended_until, no_show_count)
VALUES 
  ('00000001-0000-0000-0000-000000000007', 'sub_suspended_001', 'suspended@campus.edu', 'Eve Suspended', true, NOW() + INTERVAL '7 days', 3)
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles (user_id, role) 
VALUES ('00000001-0000-0000-0000-000000000007', 'STUDENT')
ON CONFLICT DO NOTHING;

-- VERIFY: Check suspended user
SELECT u.id, u.email, u.suspended_until, u.no_show_count, array_agg(r.role) as roles
FROM "user" u
LEFT JOIN user_roles r ON u.id = r.user_id
WHERE u.id = '00000001-0000-0000-0000-000000000007'
GROUP BY u.id, u.email;
```

---

## SECTION 4: CREATE HISTORICAL BOOKING DATA (FOR ANALYTICS)

```sql
-- Create some bookings across different facilities to test analytics
-- This simulates 30 days of booking pattern data

-- High utilization facility (Lecture Hall - 80%)
INSERT INTO booking 
(id, facility_id, requested_by, booked_for, booking_date, start_time, end_time, purpose, attendees, status, created_at, requested_at)
SELECT 
  gen_random_uuid(), 
  '10000001-0000-0000-0000-000000000001',
  '00000001-0000-0000-0000-000000000002',
  '00000001-0000-0000-0000-000000000002',
  (CURRENT_DATE - INTERVAL '30 days' + INTERVAL '1 day' * (gen_random_int(0, 29)))::date,
  (LPAD(gen_random_int(8, 16)::text, 2, '0') || ':00')::time,
  (LPAD(gen_random_int(17, 18)::text, 2, '0') || ':00')::time,
  'Lecture Session',
  gen_random_int(80, 140),
  'APPROVED',
  NOW(),
  NOW()
FROM generate_series(1, 25);

-- Low utilization facility (Meeting Room - 20%)
INSERT INTO booking 
(id, facility_id, requested_by, booked_for, booking_date, start_time, end_time, purpose, attendees, status, created_at, requested_at)
SELECT 
  gen_random_uuid(), 
  '10000001-0000-0000-0000-000000000003',
  '00000001-0000-0000-0000-000000000003',
  '00000001-0000-0000-0000-000000000003',
  (CURRENT_DATE - INTERVAL '30 days' + INTERVAL '1 day' * (gen_random_int(0, 29)))::date,
  (LPAD(gen_random_int(9, 16)::text, 2, '0') || ':00')::time,
  (LPAD(gen_random_int(17, 17)::text, 2, '0') || ':00')::time,
  'Meeting',
  gen_random_int(3, 8),
  'APPROVED',
  NOW(),
  NOW()
FROM generate_series(1, 8);

-- Medium utilization facility (Lab - 60%)
INSERT INTO booking 
(id, facility_id, requested_by, booked_for, booking_date, start_time, end_time, purpose, attendees, status, created_at, requested_at)
SELECT 
  gen_random_uuid(), 
  '10000001-0000-0000-0000-000000000002',
  '00000001-0000-0000-0000-000000000002',
  '00000001-0000-0000-0000-000000000002',
  (CURRENT_DATE - INTERVAL '30 days' + INTERVAL '1 day' * (gen_random_int(0, 29)))::date,
  (LPAD(gen_random_int(8, 15)::text, 2, '0') || ':00')::time,
  (LPAD(gen_random_int(16, 17)::text, 2, '0') || ':00')::time,
  'Lab Session',
  gen_random_int(25, 45),
  'APPROVED',
  NOW(),
  NOW()
FROM generate_series(1, 15);

-- VERIFY: Check booking data created
SELECT 
  f.facility_code, 
  f.name, 
  COUNT(*) as booking_count,
  COUNT(*)::float / 30 as daily_average
FROM booking b
JOIN facility f ON b.facility_id = f.id
WHERE b.booking_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY f.id, f.facility_code, f.name
ORDER BY booking_count DESC;
```

---

## SECTION 5: CREATE TEST TICKETS (FOR TICKET TESTING)

```sql
-- Create tickets with different priorities and statuses
INSERT INTO maintenance_ticket 
(id, facility_id, reporter_id, title, description, category, priority, status, sla_deadline, created_at)
VALUES 
  (gen_random_uuid(), '10000001-0000-0000-0000-000000000001', '00000001-0000-0000-0000-000000000003', 'Broken projector', 'Projector keeps disconnecting', 'ELECTRICAL', 'MEDIUM', 'OPEN', NOW() + INTERVAL '24 hours', NOW()),
  (gen_random_uuid(), '10000001-0000-0000-0000-000000000002', '00000001-0000-0000-0000-000000000003', 'AC not working', 'Room temperature too high', 'HVAC', 'HIGH', 'OPEN', NOW() + INTERVAL '8 hours', NOW()),
  (gen_random_uuid(), '10000001-0000-0000-0000-000000000003', '00000001-0000-0000-0000-000000000003', 'Broken chair', 'Chair wheel damaged', 'OTHER', 'LOW', 'OPEN', NOW() + INTERVAL '72 hours', NOW()),
  (gen_random_uuid(), '10000001-0000-0000-0000-000000000004', '00000001-0000-0000-0000-000000000006', 'Fire alarm broken', 'Fire alarm not responding to tests', 'SAFETY', 'CRITICAL', 'OPEN', NOW() + INTERVAL '4 hours', NOW());

-- VERIFY: Check tickets created
SELECT id, title, category, priority, status, sla_deadline
FROM maintenance_ticket
ORDER BY priority DESC, created_at DESC;
```

---

## SECTION 6: CREATE TEST NOTIFICATIONS (FOR NOTIFICATION TESTING)

```sql
-- Create sample notifications for testing the notification feed
INSERT INTO notification 
(id, recipient_user_id, type, severity, title, message, related_entity_id, is_read, created_at)
VALUES
  (gen_random_uuid(), '00000001-0000-0000-0000-000000000003', 'TICKET_UPDATE', 'HIGH', 'Ticket Assigned', 'Your maintenance ticket has been assigned to a technician', 'ticket-id-1', false, NOW() - INTERVAL '1 hour'),
  (gen_random_uuid(), '00000001-0000-0000-0000-000000000003', 'BOOKING_APPROVED', 'STANDARD', 'Booking Approved', 'Your booking request has been approved', 'booking-id-1', false, NOW() - INTERVAL '2 hours'),
  (gen_random_uuid(), '00000001-0000-0000-0000-000000000003', 'ESCALATION_ALERT', 'HIGH', 'Escalation Warning', 'Ticket SLA deadline approaching in 2 hours', 'ticket-id-2', true, NOW() - INTERVAL '3 hours'),
  (gen_random_uuid(), '00000001-0000-0000-0000-000000000002', 'APPROVAL_REQUEST', 'HIGH', 'Approval Needed', 'A booking requires your lecturer approval', 'booking-id-2', false, NOW() - INTERVAL '30 minutes');

-- VERIFY: Check notifications created
SELECT id, recipient_user_id, type, title, is_read, created_at
FROM notification
ORDER BY created_at DESC
LIMIT 10;
```

---

## SECTION 7: CLEANUP SCRIPTS (IF NEEDED)

```sql
-- DELETE ALL TEST DATA (CAREFUL!)
-- Uncomment these only if you want to reset completely

-- DELETE FROM ticket_comment WHERE ticket_id IN (SELECT id FROM maintenance_ticket);
-- DELETE FROM ticket_attachment WHERE ticket_id IN (SELECT id FROM maintenance_ticket);
-- DELETE FROM maintenance_ticket;
-- DELETE FROM booking WHERE requested_by IN (SELECT id FROM "user" WHERE email LIKE '%@campus.edu');
-- DELETE FROM user_roles WHERE user_id IN (SELECT id FROM "user" WHERE email LIKE '%@campus.edu');
-- DELETE FROM suspension_appeal WHERE user_id IN (SELECT id FROM "user" WHERE email LIKE '%@campus.edu');
-- DELETE FROM notification WHERE recipient_user_id IN (SELECT id FROM "user" WHERE email LIKE '%@campus.edu');
-- DELETE FROM "user" WHERE email LIKE '%@campus.edu';
-- DELETE FROM facility WHERE facility_code LIKE '%-%';
```

---

## VERIFICATION CHECKLIST

After running all sections, verify everything is set up correctly:

```sql
-- 1. Check all users created
SELECT COUNT(*) as user_count FROM "user";
-- Expected: >= 7

-- 2. Check all roles assigned
SELECT u.email, array_agg(r.role) as roles
FROM "user" u
LEFT JOIN user_roles r ON u.id = r.user_id
WHERE u.email LIKE '%@campus.edu'
GROUP BY u.email
ORDER BY u.email;

-- 3. Check all facilities created
SELECT COUNT(*) as facility_count FROM facility;
-- Expected: 5

-- 4. Check bookings created (if Section 4 ran)
SELECT COUNT(*) as booking_count FROM booking;
-- Expected: >= 48 (if analytics data created)

-- 5. Check suspended user setup
SELECT email, suspended_until, no_show_count FROM "user"
WHERE email = 'suspended@campus.edu';

-- 6. Check tickets created
SELECT COUNT(*) as ticket_count FROM maintenance_ticket;
-- Expected: >= 4

-- 7. Check notifications created
SELECT COUNT(*) as notification_count FROM notification;
-- Expected: >= 4

-- 8. Check database size
SELECT pg_size_pretty(pg_database_size('smartcampus')) as db_size;
```

---

## QUICK RESET BETWEEN TEST PHASES

If you want to clear specific entities without losing user/facility setup:

```sql
-- Clear all tickets but keep users/facilities
DELETE FROM ticket_comment;
DELETE FROM ticket_attachment;
DELETE FROM escalation_history;
DELETE FROM maintenance_ticket;

-- Clear all bookings but keep users/facilities
DELETE FROM booking_instance;
DELETE FROM booking;

-- Clear all appeals
DELETE FROM suspension_appeal;

-- Clear all notifications
DELETE FROM notification;

-- VERIFY
SELECT 'Users' as entity, COUNT(*) FROM "user"
UNION ALL
SELECT 'Facilities', COUNT(*) FROM facility
UNION ALL
SELECT 'Tickets', COUNT(*) FROM maintenance_ticket
UNION ALL
SELECT 'Bookings', COUNT(*) FROM booking
UNION ALL
SELECT 'Appeals', COUNT(*) FROM suspension_appeal
UNION ALL
SELECT 'Notifications', COUNT(*) FROM notification;
```
