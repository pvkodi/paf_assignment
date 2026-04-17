-- V4: Ensure facility CHECK constraints are present (idempotent)
-- Reconstructed from live database export to match production constraints

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'facility_capacity_check') THEN
        EXECUTE 'ALTER TABLE public.facility ADD CONSTRAINT facility_capacity_check CHECK ((capacity >= 1))';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'facility_facility_type_check') THEN
        EXECUTE $cmd$ALTER TABLE public.facility ADD CONSTRAINT facility_facility_type_check CHECK (((facility_type)::text = ANY ((ARRAY['FACILITY','AUDITORIUM','EQUIPMENT','LAB','LECTURE_HALL','MEETING_ROOM','SPORTS_FACILITY'])::text[])))$cmd$;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'facility_status_check') THEN
        EXECUTE $cmd$ALTER TABLE public.facility ADD CONSTRAINT facility_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE','OUT_OF_SERVICE'])::text[])))$cmd$;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'facility_type_check') THEN
        EXECUTE $cmd$ALTER TABLE public.facility ADD CONSTRAINT facility_type_check CHECK (((type)::text = ANY ((ARRAY['LECTURE_HALL','LAB','MEETING_ROOM','AUDITORIUM','EQUIPMENT','SPORTS_FACILITY'])::text[])))$cmd$;
    END IF;
END
$$;
