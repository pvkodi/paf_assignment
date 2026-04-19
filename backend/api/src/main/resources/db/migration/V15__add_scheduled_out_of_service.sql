-- V15: Add scheduled out-of-service columns to facility table
ALTER TABLE public.facility
ADD COLUMN out_of_service_start TIMESTAMP WITHOUT TIME ZONE,
ADD COLUMN out_of_service_end TIMESTAMP WITHOUT TIME ZONE;

-- Add comment for documentation
COMMENT ON COLUMN public.facility.out_of_service_start IS 'Optional start time for scheduled out-of-service period';
COMMENT ON COLUMN public.facility.out_of_service_end IS 'Optional end time for scheduled out-of-service period (null means indefinite)';
