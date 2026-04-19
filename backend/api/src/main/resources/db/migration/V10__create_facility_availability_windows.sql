-- Create table for Facility.availabilityWindows element collection.
-- Required by Hibernate validation for the @ElementCollection mapping.

CREATE TABLE IF NOT EXISTS public.facility_availability_windows (
    facility_id uuid NOT NULL,
    day_of_week character varying(10) NOT NULL,
    start_time time(0) without time zone NOT NULL,
    end_time time(0) without time zone NOT NULL,
    CONSTRAINT fk_facility_availability_windows_facility
        FOREIGN KEY (facility_id)
        REFERENCES public.facility(id)
        ON DELETE CASCADE,
    CONSTRAINT facility_availability_windows_day_of_week_check
        CHECK (
            day_of_week IN (
                'MONDAY',
                'TUESDAY',
                'WEDNESDAY',
                'THURSDAY',
                'FRIDAY',
                'SATURDAY',
                'SUNDAY'
            )
        ),
    CONSTRAINT facility_availability_windows_time_range_check
        CHECK (start_time < end_time)
);

CREATE INDEX IF NOT EXISTS idx_facility_availability_windows_facility_id
    ON public.facility_availability_windows (facility_id);
