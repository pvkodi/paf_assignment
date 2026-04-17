# Quick Test Guide - Booking Features Rebuild

## What Was Fixed

1. **Admin Bookings Page** - Now displays bookings correctly
2. **Availability Display** - Shows accurate list of booked times
3. **Time Conflict Detection** - Prevents booking conflicts on the client side
4. **Filter Defaults** - Shows all bookings by default (not filtered to APPROVED only)

## Step-by-Step Testing

### Test 1: Admin Bookings Page ✓

**Goal:** Verify admin page displays bookings without empty table issue

**Steps:**
```
1. Navigate to http://localhost:3000/admin/bookings
2. You should be logged in as ADMIN or FACILITY_MANAGER
3. Verify: Table shows booking data (not empty)
4. Check: Column headers visible (Facility, Date & Time, Purpose, etc.)
5. Status filter shows "All Statuses" by default
```

**Expected Results:**
- ✅ Table displays bookings if they exist in database
- ✅ Can see facility names, times, purposes
- ✅ Bookings sorted by date (newest first)

**Troubleshooting:**
- Check browser console for "Admin bookings fetched: count: X"
- If count is 0, verify bookings exist in database
- Test filter changes - select "APPROVED" and check if results filter correctly

---

### Test 2: Booking Form - Availability Display ✓

**Goal:** Verify availability shows booked times correctly

**Steps:**
```
1. Navigate to /bookings (or open facility booking modal)
2. Select a facility
3. Select a date (preferably one with existing bookings)
4. Observe: Availability panel appears
```

**Expected Results:**
- ✅ Panel shows green background if date is fully available
- ✅ Panel shows orange background if there are booked times
- ✅ Lists times like "14:00 - 15:30 (Lab Session)"
- ✅ Shows guidance: "Select a time outside these sessions"

**Troubleshooting:**
- Check console for "Availability debug:" log
- Verify `totalSlots` > 0 if availability endpoint works
- If fallback to `/v1/bookings` appears in logs, availability endpoint might not exist

---

### Test 3: Time Conflict Detection ✓

**Goal:** Verify form prevents booking conflicting times

**Steps:**
```
1. In availability display, note booked times (e.g., "14:00 - 15:00")
2. Try to book same time:
   - Set Start Time: 13:45
   - Set End Time: 14:30
3. Click "Create Booking"
4. Observe validation error
```

**Expected Results:**
- ✅ Form shows red error box: "Your selected time conflicts with an existing booking"
- ✅ Create button remains disabled while error exists
- ✅ Change to non-conflicting time (e.g., 15:00 - 16:00)
- ✅ Error disappears and booking proceeds

**Conflict Logic:**
```
OVERLAPS if: userStart < bookingEnd AND userEnd > bookingStart

Examples:
- Booked: 14:00 - 15:00
- Try 13:45 - 14:30 → CONFLICT (overlaps first 15 mins)
- Try 14:30 - 15:30 → CONFLICT (overlaps first 30 mins)
- Try 15:00 - 16:00 → NO CONFLICT (starts exactly when booked ends)
- Try 13:00 - 14:00 → NO CONFLICT (ends exactly when booked starts)
```

---

### Test 4: Manual Time Input ✓

**Goal:** Verify time inputs accept flexible formats

**Steps:**
```
1. In booking form, focus Start Time field
2. Type "9:30" or "09:30" or "14:45"
3. Observe: Field accepts the time
4. Set End Time to later time (e.g., "10:30")
5. Try booking
```

**Expected Results:**
- ✅ Time input accepts HH:MM format
- ✅ Both 09:30 and 9:30 work
- ✅ Both 14:45 and 2:45 PM work
- ✅ Start < End is validated
- ✅ No overlap with booked times is checked

---

### Test 5: Filter Changes (AdminBookingsView) ✓

**Goal:** Verify filters work on admin bookings page

**Steps:**
```
1. Navigate to /admin/bookings
2. Change "Status" filter to "PENDING"
3. Click "Apply Filters" or it auto-applies after 1s
4. Table updates to show only PENDING bookings
5. Try different date ranges
6. Try "Cancelled" status
```

**Expected Results:**
- ✅ Table updates when filters change
- ✅ Status filters (APPROVED, PENDING, REJECTED, CANCELLED) work
- ✅ Date range filters work
- ✅ Can combine multiple filters
- ✅ "Reset Filters" returns to all statuses

---

## Console Debugging

### Availability Debug Output
```javascript
// Look for this in browser console when selecting a date in booking form
"Availability debug:", {
  facilityId: "550e8400-e29b-41d4-a716-446655440000",
  date: "2024-01-15",
  totalSlots: 4,
  bookedSlots: 2,
  sampleSlot: {
    status: "booked",
    startTime: "14:00",
    endTime: "15:00"
  }
}
```

### Admin Bookings Debug Output
```javascript
// Look for this when loading /admin/bookings page
"Admin bookings fetched:", {
  count: 4,
  params: {
    status: "",
    from: "2024-01-01",
    to: "2024-01-31"
  },
  sampleBooking: {
    id: "550e8400-e29b-41d4-a716-446655440000",
    facility: {id: "...", name: "Lab 101"},
    bookingDate: "2024-01-15",
    startTime: "14:00",
    endTime: "15:00",
    status: "APPROVED"
  }
}
```

---

## Endpoints Reference

### Working Endpoints
- `GET /v1/bookings` - Get current user's bookings
- `GET /v1/bookings/pending-approvals` - Get pending approvals for current user
- `GET /v1/bookings/admin/all` - Get all bookings (admin view) ← **Now used by AdminBookingsView**
- `GET /v1/bookings/availability/{facilityId}?date=YYYY-MM-DD` - Get availability ← **Now used by BookingForm**
- `POST /v1/bookings` - Create booking
- `POST /v1/bookings/{bookingId}/approve` - Approve booking
- `POST /v1/bookings/{bookingId}/reject` - Reject booking

### Not Yet Implemented
- `POST /v1/bookings/{bookingId}/cancel` - Cancel booking (button disabled on frontend)

---

## Common Issues & Solutions

| Issue | Symptom | Solution |
|-------|---------|----------|
| **Admin page empty** | Table shows no rows | Check: 1) Bookings exist in DB 2) User is ADMIN/FACILITY_MANAGER 3) Console for errors |
| **Availability not loading** | "Loading availability..." stays forever | Check: 1) Facility endpoint exists 2) Network tab for 404/500 3) Fallback to /v1/bookings |
| **Time conflict not detected** | Can book over existing booking | Check: 1) bookedTimes has data 2) Time comparison logic 3) Console for validation error |
| **Wrong times shown** | Shows different times than booked | Check: 1) Timezone conversion 2) Time format (HH:MM) 3) startTime/endTime field names |
| **Filters not working** | Filter changes don't affect table | Check: 1) handleFilterChange called 2) fetchAdminBookings triggered 3) Backend filter params sent |

---

## Backend Verification

If admin page shows no data, verify backend:

```sql
-- Check if bookings exist
SELECT COUNT(*) as total FROM bookings;

-- Check booking status values
SELECT DISTINCT status FROM bookings;

-- Check sample booking data
SELECT id, facility_id, booking_date, start_time, end_time, status 
FROM bookings 
LIMIT 5;

-- Check pagination
SELECT COUNT(*) FROM bookings 
WHERE booking_date >= '2024-01-01' AND booking_date <= '2024-01-31';
```

---

## Next Steps if Issues Found

1. **AdminBookingsView still shows no data:**
   - Verify `/v1/bookings/admin/all` endpoint returns data
   - Check if endpoint requires different role/permission
   - Fall back to combined `/v1/bookings` + `/v1/bookings/pending-approvals`

2. **Availability endpoint not working:**
   - Use `/v1/bookings` fallback (already implemented)
   - Verify endpoint path and request format

3. **Cancel button needed:**
   - Implement `POST /v1/bookings/{bookingId}/cancel` backend endpoint
   - Re-enable cancel button in AdminBookingsView (line 349)

---

## Files Modified

- ✅ `frontend/src/features/bookings/BookingForm.jsx`
  - Updated availability fetch to use dedicated endpoint
  - Added time conflict detection
  - Improved availability display UI

- ✅ `frontend/src/features/bookings/AdminBookingsView.jsx`
  - Updated to use `/v1/bookings/admin/all` endpoint
  - Changed default status filter to show all
  - Disabled cancel button pending backend implementation
  - Simplified filtering logic

- ✅ `BOOKING_FEATURES_REBUILD.md` (this file)
  - Complete documentation of changes
  - Implementation details
  - Testing guide

---

## Rollback Plan

If critical issues found:

1. Revert to previous fetch methods:
   - AdminBookingsView: Combine `/v1/bookings` + `/v1/bookings/pending-approvals`
   - BookingForm: Use `/v1/bookings` for all availability

2. Disable availability display until fixed

3. Remove time conflict validation until working correctly

All changes are isolated to these two files and can be reverted without affecting other parts of the app.
