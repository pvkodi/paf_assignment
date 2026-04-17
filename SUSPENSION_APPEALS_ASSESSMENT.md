# Suspension & Appeals Feature Assessment

**Date**: April 15, 2026  
**Status**: ⚠️ PARTIALLY IMPLEMENTED - GAPS IDENTIFIED

---

## Feature Checklist

### ✅ WORKING CORRECTLY

| Feature | Status | Details |
|---------|--------|---------|
| User submit appeal | ✅ | `AppealService.submitAppeal()` - Users can appeal when suspended |
| Admin approve appeal | ✅ | `AppealService.approveAppeal()` - Releases suspension, resets no-show count |
| Admin reject appeal | ✅ | `AppealService.rejectAppeal()` - Keeps suspension active |
| Appeal retry after rejection | ✅ | Only checks for SUBMITTED status, so rejected appeals can be retried |
| Events for appeals | ✅ | APPEAL_SUBMITTED, APPEAL_APPROVED, APPEAL_REJECTED emitted |
| Appeal history | ✅ | Users can view their appeal history |
| Admin review queue | ✅ | Admins can view pending appeals ordered by submission time |

---

### ❌ MISSING OR BROKEN

| Feature | Status | Issue | Impact |
|---------|--------|-------|--------|
| **Auto-unsuspend when time passes** | ❌ | `isSuspended()` returns false after time expires, but `suspendedUntil` field NOT cleared in DB | Inconsistent state; field remains set forever |
| **Admin manual unsuspend endpoint** | ❌ | No REST endpoint for admin to immediately unsuspend a user | Missing admin capability |
| **Multiple suspensions handling** | ❌ | `applySuspension()` overwrites existing suspension instead of checking if already suspended | Can lose suspension data if suspension triggered while already suspended |
| **Automatic cleanup scheduler** | ❌ | No scheduled job to clean up expired suspensions from database | Database accumulates stale suspension records |
| **Manual unsuspend audit trail** | ❌ | No audit logging for admin unsuspend actions | Missing accountability |

---

## Implementation Analysis

### 1. Auto-Unsuspend When Time Passes

**Current Behavior** (in `SuspensionPolicyService.isSuspended()`):
```java
public boolean isSuspended(User user) {
    if (user == null || user.getSuspendedUntil() == null) {
        return false;
    }
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    boolean suspended = user.getSuspendedUntil().isAfter(now);  // ← Returns false if time passed
    if (suspended) {
        log.debug("User {} is suspended until {}", user.getEmail(), user.getSuspendedUntil());
    }
    return suspended;
}
```

**Problem**: Returns false (allowing operations) but DOES NOT clear `suspendedUntil` from database.

**Expected Behavior**: When time passes, `suspendedUntil` should be set to null in the database.

---

### 2. Admin Manual Unsuspend

**Current State**: 
- No endpoint exists for admin to unsuspend a user
- Only way to unsuspend is through appeal approval
- Missing direct admin control

**Expected**: `POST /api/v1/appeals/users/{userId}/unsuspend` (admin only)

---

### 3. Multiple Suspensions

**Current Code** (in `SuspensionPolicyService.applySuspension()`):
```java
private void applySuspension(User user) {
    LocalDateTime suspendedUntil = LocalDateTime.now(ZoneId.systemDefault())
            .plusDays(SUSPENSION_DAYS);  // ← Always sets to now + 7 days
    
    user.setSuspendedUntil(suspendedUntil);  // ← Overwrites existing suspension
    log.warn("Suspended user {} until {} due to reaching no-show threshold", 
        user.getEmail(), suspendedUntil);
}
```

**Problem**: If user already suspended until future date, this replaces it with now+7 days, potentially shortening suspension.

**Expected**: Should stack (take the later date) or track multiple suspensions separately.

---

## Integration Status Check

### ✅ Properly Integrated
- AppealService → SuspensionPolicyService.releaseSuspension() on approval
- AppealController → AppealService
- Events → EventPublisher for notifications
- AppealRepository queries → finding pending appeals
- CheckInService blocks suspended users from check-in

### ❌ Missing Integration Points
- No scheduler job to clean up expired suspensions
- No admin endpoint for manual unsuspend
- No audit logging for manual unsuspends
- No retry logic validation after appeal rejection

---

## Fix Priority

| Priority | Feature | Effort | Impact |
|----------|---------|--------|--------|
| **HIGH** | Add admin unsuspend endpoint | 1 hour | Enables admin override capability |
| **HIGH** | Implement auto-unsuspend scheduler | 1 hour | Cleans up DB, maintains consistency |
| **MEDIUM** | Handle multiple suspensions (stack) | 0.5 hour | Prevents suspension shortening |
| **MEDIUM** | Audit logging for manual unsuspends | 0.5 hour | Compliance & accountability |
| **LOW** | Validate appeal retry enforcement | 0.5 hour | Verify business logic |

---

## Affected Code Flows

### Flow 1: Suspension Lifecycle (CURRENT)
```
User hits 3 no-shows
    ↓
NoShowScheduler increments noShowCount
    ↓
NoShowScheduler calls applySuspensionIfThresholdReached()
    ↓
applySuspension() sets suspendedUntil = now + 7 days
    ↓
User tries to book → checkSuspensionPolicy() calls isSuspended()
    ↓
isSuspended() checks if suspendedUntil > now
    ↓
If false → Operations allowed (but suspendedUntil still in DB!)
If true → 403 Forbidden
    ↓
[NO AUTO-CLEANUP] suspendedUntil field never cleared
```

### Flow 2: Appeal Approval (CURRENT)
```
User submits appeal (must be suspended)
    ↓
Admin reviews and approves
    ↓
approveAppeal() calls releaseSuspension()
    ↓
releaseSuspension() sets suspendedUntil = null & noShowCount = 0
    ↓
User can operate normally
```

### Flow 3: After Rejection (CURRENT)
```
Admin rejects appeal
    ↓
rejectAppeal() keeps suspension active (no change to suspendedUntil)
    ↓
User can submit another appeal immediately (✅ WORKING)
```

---

## Recommendations

### Immediate (MUST FIX)
1. Add `POST /api/v1/users/{userId}/unsuspend` endpoint (admin only)
2. Add `SuspensionCleanupScheduler` to clean up expired suspensions from DB
3. Fix `applySuspension()` to handle multiple suspensions (take max date)

### Short-term (SHOULD FIX)
4. Add audit logging for manual unsuspends
5. Add validation test for appeal retry after rejection

### Future (NICE TO HAVE)
6. Track multiple suspension reasons/records instead of overwriting
7. Add admin override reason tracking
8. Add user notification when auto-unsuspend happens

---

## Test Cases Needed

```java
// Auto-unsuspend scenario
// 1. User suspended until now-1 hour
// 2. Call checkSuspensionPolicy() or isSuspended()
// 3. Should return false
// 4. [MISSING] Database should be cleaned up

// Multiple suspensions scenario
// 1. User suspended until 2026-04-22
// 2. Another no-show triggers suspension → now + 7 = 2026-04-22 (same date)
// 3. Should handle gracefully (not overwrite prematurely)

// Admin unsuspend scenario
// 1. User suspended
// 2. Admin calls unsuspend endpoint
// 3. suspendedUntil should be null immediately
// 4. Audit entry should be created

// Appeal retry after rejection
// 1. User submits appeal
// 2. Admin rejects appeal
// 3. User submits new appeal
// 4. Should be allowed (✅ currently works)
```

---

## Files Requiring Changes

| File | Change | Type |
|------|--------|------|
| `SuspensionPolicyService.java` | Add auto-unsuspend logic, handle multiple suspensions | UPDATE |
| `AppealController.java` | Add manual unsuspend endpoint | NEW ENDPOINT |
| `AppealService.java` | Add unsuspend method, audit logging | UPDATE |
| `SuspensionCleanupScheduler.java` | Periodic cleanup of expired suspensions | NEW FILE |
| `AuditLog` or new table | Track manual unsuspend actions | SCHEMA |

---

## Current Gaps Summary

✅ **40% Complete** - Basic appeal workflow exists but lacks:
- Auto-cleanup of expired suspensions
- Admin manual unsuspend capability
- Multiple suspension handling
- Audit trail for manual actions
- Scheduler for cleanup
