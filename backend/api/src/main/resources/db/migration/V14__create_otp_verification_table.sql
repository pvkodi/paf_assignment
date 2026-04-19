-- Migration V14: Create OTP Verification table for email-based registration
-- This table stores OTP codes sent to users for email verification during registration

CREATE TABLE IF NOT EXISTS public.otp_verification (
    id uuid NOT NULL PRIMARY KEY,
    email character varying(255) NOT NULL,
    code character varying(6) NOT NULL,
    status character varying(50) NOT NULL DEFAULT 'PENDING',
    expires_at timestamp NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at timestamp,
    attempts integer NOT NULL DEFAULT 0,
    
    CONSTRAINT otp_status_check CHECK (status IN ('PENDING', 'VERIFIED'))
);

-- Create indices for efficient OTP lookups
CREATE INDEX IF NOT EXISTS idx_otp_email ON public.otp_verification(email);
CREATE INDEX IF NOT EXISTS idx_otp_email_code ON public.otp_verification(email, code);
CREATE INDEX IF NOT EXISTS idx_otp_status ON public.otp_verification(status);

-- Clean up expired OTPs periodically (optional: can be done via scheduled task)
-- Records with status='VERIFIED' or expired are historical and can be archived
