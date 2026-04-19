-- V16: Seed facility availability windows for default facilities
-- This matches the facilities seeded in V2 migration.
-- Expanded to cover Monday-Sunday for all core facilities and a large batch of additional facilities.

-- 1. SPECIFIC CORE FACILITIES (From V2)
-- Main Lecture Hall A101 (Mon-Sun 08:00 - 18:00)
INSERT INTO public.facility_availability_windows (facility_id, day_of_week, start_time, end_time)
SELECT 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', d, '08:00:00', '18:00:00'
FROM unnest(ARRAY['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']) d
ON CONFLICT DO NOTHING;

-- Computing Lab C203 (Mon-Sun 08:00 - 20:00)
INSERT INTO public.facility_availability_windows (facility_id, day_of_week, start_time, end_time)
SELECT 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', d, '08:00:00', '20:00:00'
FROM unnest(ARRAY['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']) d
ON CONFLICT DO NOTHING;

-- Portable Projector - Epson (Mon-Sun 08:00 - 17:00)
INSERT INTO public.facility_availability_windows (facility_id, day_of_week, start_time, end_time)
SELECT 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', d, '08:00:00', '17:00:00'
FROM unnest(ARRAY['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']) d
ON CONFLICT DO NOTHING;

-- Indoor Sports Court (Mon-Sun 06:00 - 22:00)
INSERT INTO public.facility_availability_windows (facility_id, day_of_week, start_time, end_time)
SELECT 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', d, '06:00:00', '22:00:00'
FROM unnest(ARRAY['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']) d
ON CONFLICT DO NOTHING;


-- 2. DYNAMIC BATCH SEEDING (FOR 50+ FACILITIES)

-- Seed Auditoriums (AUD-01 to AUD-10)
INSERT INTO public.facility_availability_windows (facility_id, day_of_week, start_time, end_time)
SELECT f.id, d, '08:00:00', '21:00:00'
FROM public.facility f, unnest(ARRAY['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']) d
WHERE f.facility_code LIKE 'AUD-0%'
ON CONFLICT DO NOTHING;

-- Seed Lecture Halls (LH-001 to LH-020)
INSERT INTO public.facility_availability_windows (facility_id, day_of_week, start_time, end_time)
SELECT f.id, d, '08:00:00', '20:00:00'
FROM public.facility f, unnest(ARRAY['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']) d
WHERE f.facility_code LIKE 'LH-00%' OR f.facility_code LIKE 'LH-01%' OR f.facility_code LIKE 'LH-02%'
ON CONFLICT DO NOTHING;

-- Seed Computing Labs (LAB-001 to LAB-015)
INSERT INTO public.facility_availability_windows (facility_id, day_of_week, start_time, end_time)
SELECT f.id, d, '08:30:00', '19:00:00'
FROM public.facility f, unnest(ARRAY['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']) d
WHERE f.facility_code LIKE 'LAB-00%' OR f.facility_code LIKE 'LAB-01%'
ON CONFLICT DO NOTHING;

-- Seed Meeting Rooms (MR-001 to MR-015)
INSERT INTO public.facility_availability_windows (facility_id, day_of_week, start_time, end_time)
SELECT f.id, d, '08:00:00', '18:00:00'
FROM public.facility f, unnest(ARRAY['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']) d
WHERE f.facility_code LIKE 'MR-00%' OR f.facility_code LIKE 'MR-01%'
ON CONFLICT DO NOTHING;

-- Seed Sports Facilities (SPORT-01 to SPORT-10)
INSERT INTO public.facility_availability_windows (facility_id, day_of_week, start_time, end_time)
SELECT f.id, d, '06:00:00', '22:00:00'
FROM public.facility f, unnest(ARRAY['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']) d
WHERE f.facility_code LIKE 'SPORT-0%' OR f.facility_code LIKE 'SPORT-10'
ON CONFLICT DO NOTHING;
