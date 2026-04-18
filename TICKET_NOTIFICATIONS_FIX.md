# Ticket Notifications - Missing userId Fix

## Issue Found

When creating tickets, no notifications were appearing in the inbox and no records were created in the database.

## Root Cause

The `InAppObserver` was looking for a `"userId"` key in the event metadata, but the ticket notifications weren't including it. The observer checks:

```java
if (event.getMetadata() != null && event.getMetadata().containsKey("userId")) {
    userIdString = (String) event.getMetadata().get("userId");
}

if (userIdString == null) {
    log.warn("InAppObserver: userId not found in metadata for event: {}", event.getEventType());
    return;
}
```

If `userId` is missing, the observer logs a warning and returns without creating a notification.

## Solution Applied

Updated all four ticket notification events to include `"userId"` in their metadata Maps. The userId is the full UUID string of the affected user.

### Changes Made:

1. **TICKET_CREATED**: Added `"userId": createdBy.getId().toString()` to metadata
2. **TICKET_ASSIGNED**: Added `"userId": technician.getId().toString()` to metadata

3. **TICKET_STATUS_CHANGED**:
   - Refactored to store `List<User>` instead of `Set<Long>`
   - Added `"userId": affectedUser.getId().toString()` to metadata for each affected user

4. **TICKET_COMMENT_ADDED**:
   - Refactored to store `List<User>` instead of `Set<Long>`
   - Removed Long conversion issues
   - Properly excludes comment author from notifications using `User` object comparison
   - Added `"userId": affectedUser.getId().toString()` to metadata

## Example: Fixed TICKET_CREATED Event

**Before:**

```java
.metadata(Map.of(
    "ticketId", savedTicket.getId().toString(),
    "facilityId", facility.getId().toString(),
    "priority", priority.name(),
    "category", category.name()
))
```

**After:**

```java
.metadata(java.util.Map.of(
    "userId", createdBy.getId().toString(),  // ✅ ADDED
    "ticketId", savedTicket.getId().toString(),
    "facilityId", facility.getId().toString(),
    "priority", priority.name(),
    "category", category.name()
))
```

## Flow After Fix

1. Ticket is created in database
2. `TicketService.createTicket()` publishes `TICKET_CREATED` event with `userId` in metadata
3. `NotificationServiceImpl.publish()` validates and routes event
4. `InAppObserver.handleEvent()` receives event
5. InAppObserver extracts `userId` from metadata ✅ (NOW WORKS)
6. InAppObserver creates `Notification` record in database
7. Frontend queries `/v1/notifications` and displays notification ✅

## How to Test

1. Rebuild and restart the backend:

   ```bash
   cd backend/api
   ./mvnw clean compile
   ./mvnw spring-boot:run
   ```

2. Create a new ticket from the frontend

3. Check the notifications:
   - Frontend: Go to Notifications center
   - Database: Query `notification` table for new records with the ticket ID

4. You should now see:
   - ✅ Notification appears in the NotificationCenter component
   - ✅ Database has `Notification` record with `event_type = 'TICKET_CREATED'`
   - ✅ Notification has correct title, description, and action URL

## Files Modified

- [TicketService.java](backend/api/src/main/java/com/sliitreserve/api/services/ticket/TicketService.java)
  - Added `userId` to all four notification event metadata Maps
  - Refactored user tracking from `Set<Long>` to `List<User>` for better UUID handling
  - Improved author exclusion logic for comment notifications

## Compilation Status

✅ Code compiles successfully with no errors or warnings
