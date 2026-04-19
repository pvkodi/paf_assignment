-- Create registration_requests table for user registration approval workflow
CREATE TABLE public.registration_request (
    id uuid NOT NULL PRIMARY KEY,
    email character varying(255) NOT NULL,
    display_name character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role_requested character varying(50) NOT NULL,
    registration_number character varying(50),
    employee_number character varying(50),
    status character varying(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason text,
    reviewed_by_admin_id uuid,
    created_at timestamp(6) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at timestamp(6) without time zone,
    CONSTRAINT registration_request_role_check CHECK (((role_requested)::text = ANY ((ARRAY['USER'::character varying, 'LECTURER'::character varying, 'TECHNICIAN'::character varying, 'FACILITY_MANAGER'::character varying, 'ADMIN'::character varying])::text[]))),
    CONSTRAINT registration_request_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT registration_request_role_fields_check CHECK (
        CASE 
            WHEN (role_requested)::text = 'USER'::text THEN registration_number IS NOT NULL
            WHEN (role_requested)::text != 'USER'::text THEN employee_number IS NOT NULL
            ELSE TRUE
        END
    )
);

-- Index for quick lookups of pending requests
CREATE INDEX idx_registration_request_status ON public.registration_request(status);

-- Index for email lookups (for checking if email already has pending request)
CREATE INDEX idx_registration_request_email_status ON public.registration_request(email, status);

-- Foreign key constraint for reviewed_by_admin_id (nullable, optional audit trail)
ALTER TABLE public.registration_request
ADD CONSTRAINT fk_registration_request_reviewed_by_admin
FOREIGN KEY (reviewed_by_admin_id) REFERENCES public."user"(id) ON DELETE SET NULL;
