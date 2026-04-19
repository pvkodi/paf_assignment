--
-- PostgreSQL database dump
--
 
-- Dumped from database version 15.17 (Debian 15.17-1.pgdg13+1)
-- Dumped by pg_dump version 15.17 (Debian 15.17-1.pgdg13+1)
 
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: utilization_snapshots; Type: TABLE DATA; Schema: public; Owner: smartcampus
--

INSERT INTO public.utilization_snapshots (available_hours, booked_hours, consecutive_underutilized_days, snapshot_date, underutilized, utilization_percent, facility_id, id) VALUES (10.00, 7.50, 0, '2026-04-12', false, 75.00, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'f6666666-6666-6666-6666-666666666666') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.utilization_snapshots (available_hours, booked_hours, consecutive_underutilized_days, snapshot_date, underutilized, utilization_percent, facility_id, id) VALUES (12.00, 2.00, 3, '2026-04-12', true, 16.67, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', 'f6666666-6666-6666-6666-666666666667') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.utilization_snapshots (available_hours, booked_hours, consecutive_underutilized_days, snapshot_date, underutilized, utilization_percent, facility_id, id) VALUES (3.50, 6.50, 0, '2026-04-12', false, 65.00, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'dddd0001-0001-0001-0001-000000000001') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.utilization_snapshots (available_hours, booked_hours, consecutive_underutilized_days, snapshot_date, underutilized, utilization_percent, facility_id, id) VALUES (7.50, 2.50, 1, '2026-04-12', true, 25.00, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4', 'dddd0001-0001-0001-0001-000000000002') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.utilization_snapshots (available_hours, booked_hours, consecutive_underutilized_days, snapshot_date, underutilized, utilization_percent, facility_id, id) VALUES (2.00, 8.00, 0, '2026-04-12', false, 80.00, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'dddd0001-0001-0001-0001-000000000003') ON CONFLICT (id) DO NOTHING;


--
-- PostgreSQL database dump complete
--

 
