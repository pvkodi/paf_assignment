-- Add attachment type column to track PROBLEM vs SOLUTION photos
ALTER TABLE public.ticket_attachment
ADD COLUMN type character varying(50) DEFAULT 'PROBLEM';

-- Add constraint to ensure type is one of the allowed values
ALTER TABLE public.ticket_attachment
ADD CONSTRAINT ticket_attachment_type_check 
CHECK (type IN ('PROBLEM', 'SOLUTION'));

-- Update all existing null values to PROBLEM
UPDATE public.ticket_attachment
SET type = 'PROBLEM'
WHERE type IS NULL;

-- Make the column not nullable after populating defaults
ALTER TABLE public.ticket_attachment
ALTER COLUMN type SET NOT NULL;
