-- Add missing rejection_reason column to maintenance_ticket table
DO $$ 
BEGIN 
    IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='maintenance_ticket' AND column_name='rejection_reason') THEN
        ALTER TABLE public.maintenance_ticket ADD COLUMN rejection_reason TEXT;
    END IF;
END $$;
