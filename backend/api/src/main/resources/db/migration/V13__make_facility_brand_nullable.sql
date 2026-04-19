-- Allow non-equipment facilities (e.g., lecture halls, labs) to persist without a brand.
ALTER TABLE public.facility
    ALTER COLUMN brand DROP NOT NULL;
