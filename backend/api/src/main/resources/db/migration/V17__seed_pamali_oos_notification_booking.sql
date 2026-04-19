-- V17: Seed a deterministic future booking for Pamali Student so OOS cancellation notifications can be tested.
-- This is idempotent and safe to re-run: same booking ID is upserted to a future approved slot.

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
)
SELECT
    'f1700000-0000-0000-0000-000000000001'::uuid,
    f.id,
    u.id,
    u.id,
    CURRENT_DATE + INTERVAL '1 day',
    '14:00:00'::time,
    '16:00:00'::time,
    'Pamali OOS notification seed booking',
    20,
    'APPROVED',
    NULL,
    false,
    'Asia/Colombo',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM public."user" u
JOIN public.facility f ON f.facility_code = 'LAB-C203'
WHERE u.email = 'student@smartcampus.edu'
ON CONFLICT (id) DO UPDATE
SET
    facility_id = EXCLUDED.facility_id,
    requested_by_user_id = EXCLUDED.requested_by_user_id,
    booked_for_user_id = EXCLUDED.booked_for_user_id,
    booking_date = EXCLUDED.booking_date,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    purpose = EXCLUDED.purpose,
    attendees = EXCLUDED.attendees,
    status = EXCLUDED.status,
    recurrence_rule = EXCLUDED.recurrence_rule,
    is_recurring_master = EXCLUDED.is_recurring_master,
    timezone = EXCLUDED.timezone,
    version = 0,
    updated_at = CURRENT_TIMESTAMP;
