ALTER TABLE public.facility
    DROP CONSTRAINT IF EXISTS facility_status_check;

ALTER TABLE public.facility
    ADD CONSTRAINT facility_status_check
    CHECK (((status)::text = ANY ((ARRAY['ACTIVE', 'OUT_OF_SERVICE', 'MAINTENANCE'])::text[])));
