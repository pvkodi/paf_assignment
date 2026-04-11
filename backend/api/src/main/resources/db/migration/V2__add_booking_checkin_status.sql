-- Add check-in fields to booking table and extend booking status enum

-- Step 1: Extend booking_status_enum with CHECKED_IN and NO_SHOW values
ALTER TYPE booking_status_enum ADD VALUE 'CHECKED_IN' AFTER 'CANCELLED';
ALTER TYPE booking_status_enum ADD VALUE 'NO_SHOW' AFTER 'CHECKED_IN';

-- Step 2: Add check-in fields to booking table
ALTER TABLE booking ADD COLUMN check_in_method VARCHAR(50);
ALTER TABLE booking ADD COLUMN checked_in_at TIMESTAMP;

-- Step 3: Create index on checked_in_at for filtering
CREATE INDEX idx_booking_checked_in_at ON booking(checked_in_at) WHERE checked_in_at IS NOT NULL;

-- Step 4: Create index on status for efficient filtering
CREATE INDEX idx_booking_status_checked_in ON booking(status) WHERE status = 'CHECKED_IN' OR status = 'NO_SHOW';
