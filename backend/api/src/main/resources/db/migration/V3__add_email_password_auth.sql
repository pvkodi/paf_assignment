-- Add email/password authentication support to user table
-- Makes google_subject nullable (for users with email/password auth)
-- Adds password_hash column for email/password authentication

-- Step 1: Add password_hash column
ALTER TABLE public."user"
ADD COLUMN password_hash character varying(255);

-- Step 2: Make google_subject nullable
ALTER TABLE public."user"
ALTER COLUMN google_subject DROP NOT NULL;

-- Step 3: Drop the existing NOT NULL constraint check on google_subject via index/constraint management
-- The constraint is implicitly handled by the schema change above; no explicit unique constraint exists in V1
-- (Hibernate manages uniqueness through annotations, not explicit DB constraints)

-- Create a comment documenting the authentication method expectations
COMMENT ON COLUMN public."user".google_subject IS 'Google OAuth Subject (null if using email/password auth)';
COMMENT ON COLUMN public."user".password_hash IS 'Bcrypt-hashed password (null if using OAuth auth)';
