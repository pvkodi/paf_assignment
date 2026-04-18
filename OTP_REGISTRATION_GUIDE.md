# OTP-Based Registration Flow - Testing Guide

## Overview

This guide explains the new OTP-based registration system that replaces the admin-approval flow. Users can now register instantly with a valid @smartcampus.edu email address.

## New Architecture

### What Changed

- **OLD FLOW**: User registers → Admin approves → User can login
- **NEW FLOW**: User enters email → OTP sent → User verifies OTP → Auto-registered → User can login immediately

### Domain Restriction

- **ONLY** emails ending with `@smartcampus.edu` are accepted
- Invalid domain emails are rejected with error message

## Backend Changes

### New Database Table

- **Table**: `otp_verification`
- **Fields**: `id`, `email`, `code`, `status`, `expires_at`, `created_at`, `verified_at`, `attempts`
- **OTP Expiration**: 10 minutes (configurable via `otp.expiration-minutes`)
- **Max Attempts**: 5 failed attempts before lockout (configurable via `otp.max-attempts`)

### New Endpoints

#### 1. Send OTP

```
POST /api/v1/auth/otp/send
Content-Type: application/json

Request Body:
{
  "email": "student@smartcampus.edu"
}

Response (201 Created):
{
  "otpId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "student@smartcampus.edu",
  "expiresAt": "2026-04-18T14:35:00",
  "expirationMinutes": 10,
  "message": "OTP sent successfully to your email. Please check your inbox."
}

Error Responses:
- 400 Bad Request: Invalid email domain (not @smartcampus.edu)
- 400 Bad Request: Invalid email format
- 500 Internal Server Error: Email sending failed
```

#### 2. Verify OTP and Register

```
POST /api/v1/auth/otp/verify-and-register
Content-Type: application/json

Request Body:
{
  "email": "student@smartcampus.edu",
  "otp": "123456",
  "displayName": "John Doe",
  "password": "SecurePassword123",
  "confirmPassword": "SecurePassword123",
  "roleRequested": "USER",
  "registrationNumber": "20230001234",
  "employeeNumber": null
}

Response (201 Created):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2026-04-19T14:35:00",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "email": "student@smartcampus.edu",
    "displayName": "John Doe",
    "roles": ["USER"]
  }
}

Error Responses:
- 400 Bad Request: Invalid OTP code
- 400 Bad Request: OTP has expired
- 400 Bad Request: Maximum verification attempts exceeded
- 400 Bad Request: Passwords do not match
- 400 Bad Request: Email already registered
- 400 Bad Request: Missing role-specific credentials
- 500 Internal Server Error: Registration failed
```

### New Services

#### OtpService

- `generateOtpCode()` - Generates 6-digit random OTP
- `sendOtpToEmail(email)` - Validates domain, saves OTP, publishes event
- `verifyOtp(email, code)` - Validates OTP, marks as verified
- `recordFailedAttempt(email, code)` - Tracks failed attempts for brute-force protection
- `hasPendingOtp(email)` - Checks if pending OTP exists
- `getPendingOtpForEmail(email)` - Gets latest pending OTP
- `cleanupExpiredOtps()` - Cleanup task for expired OTPs

#### OtpEmailObserver

- Listens for `OTP_SENT` events
- Sends formatted email with 6-digit code to user
- Uses event-driven architecture (HIGH severity events only)

#### RegistrationRequestService - New Method

- `registerUserFromOtp(VerifyOtpAndRegisterDTO)` - Creates User directly (no admin approval)

### New DTOs

- `SendOtpRequestDTO` - Request to send OTP
- `SendOtpResponseDTO` - Response with OTP details
- `VerifyOtpAndRegisterDTO` - Request to verify OTP and register
- `OtpRegistrationResponseDTO` - Response after registration (includes user and tokens)

## Frontend Changes

### New Route

- **Path**: `/register-otp`
- **Component**: `OtpRegistrationPage.jsx`
- **Public Route** (no authentication required)

### Registration Flow (3 Steps)

#### Step 1: Email Entry

- User enters email address
- Frontend validates email format
- Frontend validates @smartcampus.edu domain
- On submit: Calls `/api/v1/auth/otp/send`
- On success: Move to Step 2

#### Step 2: OTP Entry

- Display: "We've sent a 6-digit code to [email]"
- Input field for 6-digit OTP code
- Countdown timer showing time remaining
- Resend button (requests new OTP)
- On submit: Move to Step 3 (actual verification happens here)

#### Step 3: Registration Details

- Full name input
- Role dropdown (Student, Lecturer, Technician, Facility Manager)
- Role-specific credential (Registration # for students, Employee # for staff)
- Password input (min 8 characters)
- Confirm password input
- On submit: Calls `/api/v1/auth/otp/verify-and-register`
- On success: User is logged in, redirected to dashboard

### Key Features

- **Back Button**: Navigate to previous step
- **Error Handling**: Clear error messages for validation failures
- **OTP Countdown**: Visual timer showing expiration
- **Resend**: Ability to request new OTP if expired
- **Auto-Login**: After successful registration, user is immediately logged in with JWT tokens

## Testing Scenarios

### Test 1: Successful Registration Flow

1. Navigate to `/register-otp`
2. Enter email: `test.student@smartcampus.edu`
3. Click "Send OTP"
4. Check email for OTP code
5. Enter OTP code (6 digits)
6. Click "Verify & Continue"
7. Fill in registration details:
   - Name: "Test Student"
   - Role: "USER"
   - Registration #: "20230001234"
   - Password: "TestPassword123"
   - Confirm: "TestPassword123"
8. Click "Complete Registration"
9. Verify: Redirected to dashboard, user is logged in

### Test 2: Invalid Email Domain

1. Enter email: `test@gmail.com`
2. Click "Send OTP"
3. Verify: Error message "Only @smartcampus.edu email addresses are accepted"

### Test 3: OTP Expiration

1. Send OTP
2. Wait 10+ minutes (or modify `otp.expiration-minutes` in config)
3. Enter OTP code
4. Click "Verify & Continue"
5. Verify: Error message "OTP has expired. Please request a new one."
6. Verify: Flow resets to Step 1

### Test 4: Invalid OTP Code

1. Send OTP
2. Enter wrong OTP code (e.g., "000000")
3. Click "Verify & Continue"
4. Verify: Error message "Invalid OTP code"
5. Verify: Attempt counter increments

### Test 5: Max Attempts Exceeded

1. Send OTP
2. Enter wrong code 5 times (or configured max-attempts)
3. On 6th attempt:
4. Verify: Error message "Maximum verification attempts exceeded. Please request a new OTP."

### Test 6: OTP Resend

1. Send OTP (first code sent)
2. Click "Resend"
3. Verify: New OTP is generated
4. Check email: Should have received new code
5. Enter new code
6. Verify: Works correctly

### Test 7: Password Mismatch

1. Complete Steps 1-2 successfully
2. In Step 3, enter different passwords
3. Click "Complete Registration"
4. Verify: Error message "Passwords do not match"

### Test 8: Missing Role Credentials

1. Complete Steps 1-2
2. In Step 3, select role but don't enter credential
3. Click "Complete Registration"
4. Verify: Error message "Registration number is required for STUDENT role" (or employee number for other roles)

### Test 9: Duplicate Email

1. User 1: Complete successful registration with `test@smartcampus.edu`
2. User 2: Try to register with same email
3. In Step 3: Click "Complete Registration"
4. Verify: Error message "Email already registered"

### Test 10: Email Sending Verification

1. Send OTP to valid email
2. Check email inbox
3. Verify email contains:
   - Subject: "🔐 Your OTP Code for Smart Campus Registration"
   - 6-digit OTP code
   - Expiration time (10 minutes)
   - Security notice about not sharing code
   - Smart Campus branding

## Postman Testing

### Collection: Smart Campus OTP Registration

```json
{
  "info": {
    "name": "OTP Registration",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Send OTP",
      "request": {
        "method": "POST",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "url": {
          "raw": "http://localhost:8080/api/v1/auth/otp/send",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "auth", "otp", "send"]
        },
        "body": {
          "mode": "raw",
          "raw": "{\n  \"email\": \"test.student@smartcampus.edu\"\n}"
        }
      }
    },
    {
      "name": "Verify OTP and Register",
      "request": {
        "method": "POST",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "url": {
          "raw": "http://localhost:8080/api/v1/auth/otp/verify-and-register",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "auth", "otp", "verify-and-register"]
        },
        "body": {
          "mode": "raw",
          "raw": "{\n  \"email\": \"test.student@smartcampus.edu\",\n  \"otp\": \"123456\",\n  \"displayName\": \"Test Student\",\n  \"password\": \"TestPassword123\",\n  \"confirmPassword\": \"TestPassword123\",\n  \"roleRequested\": \"USER\",\n  \"registrationNumber\": \"20230001234\"\n}"
        }
      }
    }
  ]
}
```

## Configuration

### application.properties or application.yml

```properties
# OTP Configuration
otp.expiration-minutes=10
otp.max-attempts=5

# Email Configuration
spring.mail.host=smtp.mailtrap.io
spring.mail.port=587
spring.mail.username=${MAILTRAP_USERNAME}
spring.mail.password=${MAILTRAP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## Migration from Old System

### For Existing Users

- Old admin-approval flow still works for backward compatibility
- Both `/api/v1/auth/register` (old) and `/api/v1/auth/otp/*` (new) endpoints coexist
- Admin panel still exists and can approve/reject old-style registrations

### For New Registrations

- Default: Users should use OTP flow (`/register-otp`)
- Old admin-approval still available if needed
- Organizations can choose which flow to use

## Troubleshooting

### OTP Not Received

1. Check email spam/junk folder
2. Verify email server configuration
3. Check server logs for email sending errors
4. Verify database has otp_verification table (migration V12)

### OTP Expired

1. Re-request OTP (click Resend or go back to Step 1)
2. Default expiration is 10 minutes
3. Check server time synchronization

### Registration Fails with "Email Already Registered"

1. Check if user already has an account
2. If account was deleted, data might still be in trash
3. Contact admin to verify user status

### Cannot Complete Step 2 → Step 3

1. Verify OTP code is correct (check email)
2. Verify OTP hasn't expired (check timer)
3. Verify no more than 5 failed attempts

## Database Cleanup

### Periodic Cleanup of Expired OTPs

```sql
-- Manual cleanup (run periodically)
DELETE FROM otp_verification WHERE status = 'PENDING' AND expires_at < NOW();

-- Or use OtpService.cleanupExpiredOtps() - can be scheduled via Spring Scheduler
```

## Security Considerations

1. **OTP Codes**: 6-digit random codes (1,000,000 possible values)
2. **Rate Limiting**: Max 5 verification attempts per OTP
3. **Expiration**: OTPs expire after 10 minutes
4. **Email Domain**: Only @smartcampus.edu accepted
5. **Password Hashing**: BCrypt with strength 12
6. **One-Use**: OTP can only be used once (verified status)

## Future Enhancements

1. SMS-based OTP as alternative to email
2. TOTP (Time-based OTP) support
3. QR code for auto-fill in mobile apps
4. Bulk OTP generation for events
5. OTP statistics and audit logs
6. Rate limiting per IP address
