-- Add checked_in_by user reference to booking table for tracking who performed check-in

-- Step 1: Add checked_in_by_user_id foreign key column to booking table
ALTER TABLE booking ADD COLUMN checked_in_by_user_id UUID;

-- Step 2: Add foreign key constraint
ALTER TABLE booking
ADD CONSTRAINT fk_booking_checked_in_by_user
FOREIGN KEY (checked_in_by_user_id) REFERENCES "user"(id) ON DELETE SET NULL;

-- Step 3: Create index for querying check-ins by user
CREATE INDEX idx_booking_checked_in_by_user ON booking(checked_in_by_user_id);

-- Step 4: Create composite index for finding check-ins by status and user
CREATE INDEX idx_booking_checked_in_status_user ON booking(status, checked_in_by_user_id) 
WHERE status IN ('CHECKED_IN', 'NO_SHOW');
