-- Core schema migration aligned with current JPA/Hibernate mappings
-- Generated from Hibernate ddl-auto=create on 2026-04-11

CREATE TABLE public.approval_step (
    step_order integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    decided_at timestamp(6) without time zone,
    booking_id uuid NOT NULL,
    decided_by_user_id uuid,
    id uuid NOT NULL,
    approver_role character varying(50) NOT NULL,
    decision character varying(50) NOT NULL,
    note character varying(500),
    CONSTRAINT approval_step_approver_role_check CHECK (((approver_role)::text = ANY ((ARRAY['USER'::character varying, 'LECTURER'::character varying, 'TECHNICIAN'::character varying, 'FACILITY_MANAGER'::character varying, 'ADMIN'::character varying])::text[]))),
    CONSTRAINT approval_step_decision_check CHECK (((decision)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT approval_step_step_order_check CHECK ((step_order >= 1))
);



CREATE TABLE public.booking (
    attendees integer NOT NULL,
    booking_date date NOT NULL,
    end_time time(0) without time zone NOT NULL,
    is_recurring_master boolean NOT NULL,
    start_time time(0) without time zone NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    booked_for_user_id uuid NOT NULL,
    facility_id uuid NOT NULL,
    id uuid NOT NULL,
    requested_by_user_id uuid NOT NULL,
    status character varying(50) NOT NULL,
    timezone character varying(50) NOT NULL,
    purpose character varying(500) NOT NULL,
    recurrence_rule character varying(500),
    CONSTRAINT booking_attendees_check CHECK ((attendees >= 1)),
    CONSTRAINT booking_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'CANCELLED'::character varying])::text[])))
);



CREATE TABLE public.check_in (
    checked_in_at timestamp(6) without time zone NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    booking_id uuid NOT NULL,
    checked_in_by_user_id uuid,
    id uuid NOT NULL,
    method character varying(20) NOT NULL,
    CONSTRAINT check_in_method_check CHECK (((method)::text = ANY ((ARRAY['QR'::character varying, 'MANUAL'::character varying])::text[])))
);



CREATE TABLE public.facility (
    av_enabled boolean,
    availability_end time(0) without time zone NOT NULL,
    availability_start time(0) without time zone NOT NULL,
    capacity integer NOT NULL,
    catering_allowed boolean,
    wheelchair_accessible boolean,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    facility_code character varying(50) NOT NULL,
    facility_type character varying(50) NOT NULL,
    floor character varying(50),
    status character varying(50) NOT NULL,
    type character varying(50) NOT NULL,
    brand character varying(100) NOT NULL,
    building character varying(100),
    lab_type character varying(100),
    model character varying(100),
    serial_number character varying(100),
    sound_system character varying(100),
    sports_type character varying(100),
    stage_type character varying(100),
    location character varying(255),
    maintenance_schedule character varying(255),
    name character varying(255) NOT NULL,
    CONSTRAINT facility_capacity_check CHECK ((capacity >= 1)),
    CONSTRAINT facility_facility_type_check CHECK (((facility_type)::text = ANY ((ARRAY['FACILITY'::character varying, 'AUDITORIUM'::character varying, 'EQUIPMENT'::character varying, 'LAB'::character varying, 'LECTURE_HALL'::character varying, 'MEETING_ROOM'::character varying, 'SPORTS_FACILITY'::character varying])::text[]))),
    CONSTRAINT facility_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'OUT_OF_SERVICE'::character varying])::text[]))),
    CONSTRAINT facility_type_check CHECK (((type)::text = ANY ((ARRAY['LECTURE_HALL'::character varying, 'LAB'::character varying, 'MEETING_ROOM'::character varying, 'AUDITORIUM'::character varying, 'EQUIPMENT'::character varying, 'SPORTS_FACILITY'::character varying])::text[])))
);



CREATE TABLE public.lab_safety_equipment (
    lab_id uuid NOT NULL,
    safety_equipment character varying(255)
);



CREATE TABLE public.lab_software_list (
    lab_id uuid NOT NULL,
    software_item character varying(255)
);



CREATE TABLE public.lecture_hall_av_equipment (
    lecture_hall_id uuid NOT NULL,
    av_equipment character varying(255)
);



CREATE TABLE public.maintenance_ticket (
    escalation_level integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    sla_due_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    assigned_technician_user_id uuid,
    created_by_user_id uuid NOT NULL,
    facility_id uuid NOT NULL,
    id uuid NOT NULL,
    category character varying(50) NOT NULL,
    priority character varying(50) NOT NULL,
    status character varying(50) NOT NULL,
    title character varying(200) NOT NULL,
    description text NOT NULL,
    CONSTRAINT maintenance_ticket_category_check CHECK (((category)::text = ANY ((ARRAY['ELECTRICAL'::character varying, 'PLUMBING'::character varying, 'HVAC'::character varying, 'IT_NETWORKING'::character varying, 'STRUCTURAL'::character varying, 'CLEANING'::character varying, 'SAFETY'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT maintenance_ticket_priority_check CHECK (((priority)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'CRITICAL'::character varying])::text[]))),
    CONSTRAINT maintenance_ticket_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'IN_PROGRESS'::character varying, 'RESOLVED'::character varying, 'CLOSED'::character varying, 'REJECTED'::character varying])::text[])))
);



CREATE TABLE public.notification_channels (
    notification_id uuid NOT NULL,
    channel character varying(255),
    CONSTRAINT notification_channels_channel_check CHECK (((channel)::text = ANY ((ARRAY['IN_APP'::character varying, 'EMAIL'::character varying])::text[])))
);



CREATE TABLE public.notifications (
    created_at timestamp(6) without time zone NOT NULL,
    delivered_at timestamp(6) without time zone,
    read_at timestamp(6) without time zone,
    id uuid NOT NULL,
    recipient_user_id uuid NOT NULL,
    action_label character varying(255),
    action_url character varying(255),
    entity_reference character varying(255),
    event_id character varying(255),
    event_type character varying(255) NOT NULL,
    message text NOT NULL,
    severity character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    CONSTRAINT notifications_severity_check CHECK (((severity)::text = ANY ((ARRAY['HIGH'::character varying, 'STANDARD'::character varying])::text[])))
);



CREATE TABLE public.sports_facility_equipment_available (
    sports_facility_id uuid NOT NULL,
    equipment_item character varying(255)
);



CREATE TABLE public.suspension_appeal (
    created_at timestamp(6) without time zone NOT NULL,
    reviewed_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    reviewed_by_user_id uuid,
    user_id uuid NOT NULL,
    status character varying(50) NOT NULL,
    decision character varying(500),
    reason character varying(1000) NOT NULL,
    CONSTRAINT suspension_appeal_status_check CHECK (((status)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);



CREATE TABLE public.ticket_attachment (
    file_size bigint NOT NULL,
    uploaded_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    ticket_id uuid NOT NULL,
    uploaded_by_user_id uuid NOT NULL,
    mime_type character varying(50) NOT NULL,
    checksum_hash character varying(64) NOT NULL,
    file_name character varying(255) NOT NULL,
    file_path text NOT NULL,
    thumbnail_path text
);



CREATE TABLE public.ticket_comment (
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    author_user_id uuid NOT NULL,
    id uuid NOT NULL,
    ticket_id uuid NOT NULL,
    visibility character varying(50) NOT NULL,
    content text NOT NULL,
    CONSTRAINT ticket_comment_visibility_check CHECK (((visibility)::text = ANY ((ARRAY['PUBLIC'::character varying, 'INTERNAL'::character varying])::text[])))
);



CREATE TABLE public.ticket_escalation (
    from_level integer NOT NULL,
    to_level integer NOT NULL,
    escalated_at timestamp(6) without time zone NOT NULL,
    escalated_by_user_id uuid NOT NULL,
    id uuid NOT NULL,
    new_assignee_user_id uuid,
    previous_assignee_user_id uuid,
    ticket_id uuid NOT NULL,
    escalation_reason character varying(255) NOT NULL,
    notes text,
    CONSTRAINT ticket_escalation_from_level_check CHECK ((from_level >= 0)),
    CONSTRAINT ticket_escalation_to_level_check CHECK ((to_level >= 0))
);



CREATE TABLE public."user" (
    active boolean NOT NULL,
    no_show_count integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    suspended_until timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    display_name character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    google_subject character varying(255) NOT NULL
);



CREATE TABLE public.user_roles (
    user_id uuid NOT NULL,
    role character varying(50) NOT NULL,
    CONSTRAINT user_roles_role_check CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'LECTURER'::character varying, 'TECHNICIAN'::character varying, 'FACILITY_MANAGER'::character varying, 'ADMIN'::character varying])::text[])))
);



CREATE TABLE public.utilization_snapshots (
    available_hours numeric(10,2) NOT NULL,
    booked_hours numeric(10,2) NOT NULL,
    consecutive_underutilized_days integer,
    snapshot_date date NOT NULL,
    underutilized boolean NOT NULL,
    utilization_percent numeric(5,2) NOT NULL,
    facility_id uuid NOT NULL,
    id uuid NOT NULL
);



ALTER TABLE ONLY public.approval_step
    ADD CONSTRAINT approval_step_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.booking
    ADD CONSTRAINT booking_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.check_in
    ADD CONSTRAINT check_in_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.facility
    ADD CONSTRAINT facility_facility_code_key UNIQUE (facility_code);



ALTER TABLE ONLY public.facility
    ADD CONSTRAINT facility_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.facility
    ADD CONSTRAINT facility_serial_number_key UNIQUE (serial_number);



ALTER TABLE ONLY public.maintenance_ticket
    ADD CONSTRAINT maintenance_ticket_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_event_id_key UNIQUE (event_id);



ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.suspension_appeal
    ADD CONSTRAINT suspension_appeal_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.ticket_attachment
    ADD CONSTRAINT ticket_attachment_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.ticket_comment
    ADD CONSTRAINT ticket_comment_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.ticket_escalation
    ADD CONSTRAINT ticket_escalation_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_email_key UNIQUE (email);



ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_google_subject_key UNIQUE (google_subject);



ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role);



ALTER TABLE ONLY public.utilization_snapshots
    ADD CONSTRAINT utilization_snapshots_pkey PRIMARY KEY (id);



CREATE INDEX idx_appeal_created_at ON public.suspension_appeal USING btree (created_at);



CREATE INDEX idx_appeal_status ON public.suspension_appeal USING btree (status);



CREATE INDEX idx_appeal_user ON public.suspension_appeal USING btree (user_id);



CREATE INDEX idx_approval_step_booking ON public.approval_step USING btree (booking_id);



CREATE INDEX idx_approval_step_order ON public.approval_step USING btree (booking_id, step_order);



CREATE INDEX idx_attachment_ticket ON public.ticket_attachment USING btree (ticket_id);



CREATE INDEX idx_attachment_uploaded_at ON public.ticket_attachment USING btree (uploaded_at);



CREATE INDEX idx_attachment_uploaded_by ON public.ticket_attachment USING btree (uploaded_by_user_id);



CREATE INDEX idx_booking_booked_for ON public.booking USING btree (booked_for_user_id);



CREATE INDEX idx_booking_date ON public.booking USING btree (booking_date);



CREATE INDEX idx_booking_facility ON public.booking USING btree (facility_id);



CREATE INDEX idx_booking_requested_by ON public.booking USING btree (requested_by_user_id);



CREATE INDEX idx_booking_status ON public.booking USING btree (status);



CREATE INDEX idx_check_in_booking ON public.check_in USING btree (booking_id);



CREATE INDEX idx_check_in_timestamp ON public.check_in USING btree (checked_in_at);



CREATE INDEX idx_comment_author ON public.ticket_comment USING btree (author_user_id);



CREATE INDEX idx_comment_created_at ON public.ticket_comment USING btree (created_at);



CREATE INDEX idx_comment_deleted_at ON public.ticket_comment USING btree (deleted_at);



CREATE INDEX idx_comment_ticket ON public.ticket_comment USING btree (ticket_id);



CREATE INDEX idx_escalation_escalated_at ON public.ticket_escalation USING btree (escalated_at);



CREATE INDEX idx_escalation_escalated_by ON public.ticket_escalation USING btree (escalated_by_user_id);



CREATE INDEX idx_escalation_ticket ON public.ticket_escalation USING btree (ticket_id);



CREATE INDEX idx_escalation_to_level ON public.ticket_escalation USING btree (to_level);



CREATE INDEX idx_notifications_timeline ON public.notifications USING btree (created_at DESC);



CREATE INDEX idx_notifications_unread ON public.notifications USING btree (recipient_user_id, read_at);



CREATE INDEX idx_notifications_user_timeline ON public.notifications USING btree (recipient_user_id, created_at DESC);



CREATE INDEX idx_ticket_assigned_to ON public.maintenance_ticket USING btree (assigned_technician_user_id);



CREATE INDEX idx_ticket_created_at ON public.maintenance_ticket USING btree (created_at);



CREATE INDEX idx_ticket_created_by ON public.maintenance_ticket USING btree (created_by_user_id);



CREATE INDEX idx_ticket_escalation_level ON public.maintenance_ticket USING btree (escalation_level);



CREATE INDEX idx_ticket_facility ON public.maintenance_ticket USING btree (facility_id);



CREATE INDEX idx_ticket_priority ON public.maintenance_ticket USING btree (priority);



CREATE INDEX idx_ticket_sla_due_at ON public.maintenance_ticket USING btree (sla_due_at);



CREATE INDEX idx_ticket_status ON public.maintenance_ticket USING btree (status);



CREATE INDEX idx_utilization_date ON public.utilization_snapshots USING btree (snapshot_date DESC);



CREATE INDEX idx_utilization_facility_date ON public.utilization_snapshots USING btree (facility_id, snapshot_date DESC);



CREATE INDEX idx_utilization_underutilized ON public.utilization_snapshots USING btree (underutilized, snapshot_date DESC);



ALTER TABLE ONLY public.ticket_attachment
    ADD CONSTRAINT fk1lor48sejbp4pd6204um2r64 FOREIGN KEY (ticket_id) REFERENCES public.maintenance_ticket(id);



ALTER TABLE ONLY public.ticket_escalation
    ADD CONSTRAINT fk1pxn8scof05evyfiqxq91id56 FOREIGN KEY (new_assignee_user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.suspension_appeal
    ADD CONSTRAINT fk4tmupn7qrkws4hu8mdq3munmv FOREIGN KEY (reviewed_by_user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.booking
    ADD CONSTRAINT fk6io8j4ov8vlpwc9wc37179ca1 FOREIGN KEY (facility_id) REFERENCES public.facility(id);



ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fk7ivp84f52aa3vd7ndq0oh0279 FOREIGN KEY (user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.booking
    ADD CONSTRAINT fk8oxul73rrdf6fq1pywpwmecxs FOREIGN KEY (requested_by_user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.lecture_hall_av_equipment
    ADD CONSTRAINT fk8w6i63n2ingr6yd2nb6lp2mft FOREIGN KEY (lecture_hall_id) REFERENCES public.facility(id);



ALTER TABLE ONLY public.ticket_comment
    ADD CONSTRAINT fk9famtu3bo5b1fm77q7n5aucw4 FOREIGN KEY (ticket_id) REFERENCES public.maintenance_ticket(id);



ALTER TABLE ONLY public.check_in
    ADD CONSTRAINT fk_check_in_booking FOREIGN KEY (booking_id) REFERENCES public.booking(id);



ALTER TABLE ONLY public.check_in
    ADD CONSTRAINT fk_check_in_user FOREIGN KEY (checked_in_by_user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.maintenance_ticket
    ADD CONSTRAINT fkba8c14fgfh33n01n2gv9aru9l FOREIGN KEY (facility_id) REFERENCES public.facility(id);



ALTER TABLE ONLY public.ticket_escalation
    ADD CONSTRAINT fkew9gba0m3wttruu0vsk9c0hp2 FOREIGN KEY (ticket_id) REFERENCES public.maintenance_ticket(id);



ALTER TABLE ONLY public.utilization_snapshots
    ADD CONSTRAINT fkgdy6kmruunho3efxgslserfxm FOREIGN KEY (facility_id) REFERENCES public.facility(id);



ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fkgify96xou00owlbn9mg2v3pm2 FOREIGN KEY (recipient_user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.lab_software_list
    ADD CONSTRAINT fkgmkxcx0sdy8ehvyqx0dvu6vi9 FOREIGN KEY (lab_id) REFERENCES public.facility(id);



ALTER TABLE ONLY public.maintenance_ticket
    ADD CONSTRAINT fkh3t7cvtb6a2y1hjcl94cuqbco FOREIGN KEY (created_by_user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.maintenance_ticket
    ADD CONSTRAINT fkhs4c9d3ny0lwmkoo3e2sseijm FOREIGN KEY (assigned_technician_user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.ticket_escalation
    ADD CONSTRAINT fkk29kyypd77v0m44tkgb1pmh7c FOREIGN KEY (previous_assignee_user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.booking
    ADD CONSTRAINT fkkb0rm6qisuqva0tqj9wbbnrwp FOREIGN KEY (booked_for_user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.lab_safety_equipment
    ADD CONSTRAINT fkkeqsunb602daeytf5v2t14y6m FOREIGN KEY (lab_id) REFERENCES public.facility(id);



ALTER TABLE ONLY public.ticket_escalation
    ADD CONSTRAINT fkkkqj28trax7mepju6epa9fq8r FOREIGN KEY (escalated_by_user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.ticket_comment
    ADD CONSTRAINT fkl3hj2q62nh104qfvggxxopauw FOREIGN KEY (author_user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.approval_step
    ADD CONSTRAINT fklo7hpsfxlpf4ttbl925dwidvy FOREIGN KEY (booking_id) REFERENCES public.booking(id);



ALTER TABLE ONLY public.notification_channels
    ADD CONSTRAINT fkmpsidir1onjqphb9jl5a0ie2s FOREIGN KEY (notification_id) REFERENCES public.notifications(id);



ALTER TABLE ONLY public.ticket_attachment
    ADD CONSTRAINT fkpxsf2pw6pmxpe7dp5vgjn6i49 FOREIGN KEY (uploaded_by_user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.approval_step
    ADD CONSTRAINT fks4tlr87vhvqt8r0s9h6aijw46 FOREIGN KEY (decided_by_user_id) REFERENCES public."user"(id);



ALTER TABLE ONLY public.sports_facility_equipment_available
    ADD CONSTRAINT fkselhfe98qow3ia3kqec08wg3u FOREIGN KEY (sports_facility_id) REFERENCES public.facility(id);



ALTER TABLE ONLY public.suspension_appeal
    ADD CONSTRAINT fktnkyr76gth5vf7avkk8dj1bc FOREIGN KEY (user_id) REFERENCES public."user"(id);


