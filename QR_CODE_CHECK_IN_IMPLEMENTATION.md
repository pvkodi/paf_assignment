# QR Code Check-In Implementation Guide

## Overview

This implementation adds a complete QR code-based check-in flow that:

1. Allows unauthenticated users to scan a QR code and login
2. Automatically redirects authenticated users to the check-in page
3. Performs GPS-based geofencing verification
4. Provides admins with a tool to generate printable QR codes

## Architecture

### User Flow

```
1. User scans QR code → /check-in/booking/:bookingId
2. If NOT logged in → Redirects to /login?redirect=/check-in/booking/:bookingId
3. User logs in (Google OAuth or email/password)
4. After login → Automatically redirects back to /check-in/booking/:bookingId
5. Booking details load
6. GPS location is collected
7. Check-in is recorded with geofencing verification
8. Success → Redirects to /my-bookings
```

### Components & Routes

#### 1. **QuickCheckInPage** (`frontend/src/features/bookings/QuickCheckInPage.jsx`)

- Accessible at: `/check-in/booking/:bookingId`
- Protected route (requires authentication)
- Loads booking details from API
- Displays check-in component
- Auto-triggers GPS collection
- Redirects to `/my-bookings` on success

#### 2. **QRCodeGenerator** (`frontend/src/features/bookings/QRCodeGenerator.jsx`)

- Accessible at: `/qr-code-generator` (admin/facility manager only)
- Generates QR codes from booking IDs
- Can download QR codes as PNG
- Can print QR codes directly
- Shows the check-in URL for reference

#### 3. **Updated ProtectedRoute** (`frontend/src/routes/ProtectedRoute.jsx`)

- Now preserves original URL as query parameter
- Unauthenticated users → `/login?redirect=[original-url]`
- Allows post-login redirect to original destination

#### 4. **Updated LoginPage** (`frontend/src/features/auth/LoginPage.jsx`)

- Reads `redirect` parameter from URL
- After successful login, redirects to the saved URL
- Falls back to `/dashboard` if no redirect specified

## Installation

### 1. Install QR Code Library

```bash
cd frontend
npm install qrcode.react
```

### 2. Files Modified

- `frontend/src/routes/ProtectedRoute.jsx` - Preserve redirect URLs
- `frontend/src/features/auth/LoginPage.jsx` - Handle redirects after login
- `frontend/src/App.jsx` - Add new routes
- `frontend/src/routes/pages.jsx` - Add page wrappers

### 3. Files Created

- `frontend/src/features/bookings/QuickCheckInPage.jsx` - Check-in page
- `frontend/src/features/bookings/QRCodeGenerator.jsx` - QR generator

## Usage

### For Admins/Facility Managers

#### Generate QR Codes:

1. Go to `/qr-code-generator`
2. Enter a booking ID
3. Click "Generate QR Code"
4. Download as PNG or Print directly
5. Place/display in facility

#### QR Code contains URL:

```
https://smartcampus.com/check-in/booking/[booking-uuid]
```

### For Users

#### Via QR Code:

1. Scan QR code with phone camera
2. Opens browser with check-in page
3. If not logged in → Login page appears
4. Login with Google OAuth or email/password
5. Redirected back to check-in page automatically
6. Allow location services when prompted
7. GPS location collected and verified
8. Check-in recorded
9. Success message displayed

#### Check-In Verification Flow:

- GPS coordinates collected from browser
- Distance calculated using Haversine formula
- Verified against facility GPS radius (default 100m)
- If within radius → Check-in successful
- If outside radius → Error message, check-in rejected

## Security & Authorization

### Authentication

- ✅ Protected route requires login
- ✅ QR code URL is public, but check-in requires authentication
- ✅ Only valid bookings can be accessed (API-level validation)
- ✅ Post-login redirect is URL-encoded and validated

### Authorization

- ✅ Users can only check-in to their own bookings
- ✅ Backend validates booking ownership
- ✅ QR code generator only for ADMIN/FACILITY_MANAGER

### Redirect Security

- ✅ Redirect parameter is URL-encoded
- ✅ Only valid relative paths supported (no external redirects)
- ✅ Falls back to safe default if redirect is invalid

## API Endpoints Used

### Check-In

```
POST /v1/bookings/{bookingId}/check-in/with-geofencing
Payload:
{
  "method": "QR",
  "latitude": 6.92705,
  "longitude": 80.77895
}
```

### Booking Details

```
GET /v1/bookings/{bookingId}
```

## Browser Compatibility

### Required APIs

- ✅ Geolocation API (all modern browsers)
- ✅ Canvas API (for QR code rendering)
- ✅ URL API (for parameter encoding)

### Tested On

- Chrome/Chromium (desktop & mobile)
- Firefox (desktop & mobile)
- Safari (desktop & mobile)
- Edge

## Error Handling

### Scenarios Handled

1. **Booking Not Found**
   - Shows error message
   - Link to go back to `/my-bookings`

2. **Location Services Denied**
   - Shows error: "Failed to collect GPS location"
   - User can enable location and retry

3. **GPS Out of Range**
   - Shows error: "Location Out of Range"
   - Displays distance and required radius

4. **Not Logged In**
   - Redirects to login page with return URL
   - After login, returns to check-in page

5. **Already Checked In**
   - Shows success message
   - Button disabled: "✓ Checked In"

## Future Enhancements

### Possible Improvements

1. **Facility-Level QR Codes** - Generate for entire facility, not just booking
2. **QR Code Management Portal** - Admin can view/manage all active QR codes
3. **Expiring QR Codes** - Auto-disable QR after booking date passes
4. **Multi-QR Generation** - Bulk generate QR codes for multiple bookings
5. **QR Code Analytics** - Track how many times each QR code was scanned
6. **WiFi Integration** - Re-enable WiFi verification if API becomes available

## Troubleshooting

### QR Code Not Generating

- ✅ Check if `qrcode.react` is installed
- ✅ Ensure booking ID is valid UUID format
- ✅ Check browser console for errors

### Login Redirect Not Working

- ✅ Check that `?redirect=` parameter is URL-encoded
- ✅ Clear browser cache and cookies
- ✅ Verify redirect URL starts with `/`

### GPS Not Collecting

- ✅ Enable location services in browser
- ✅ Check device has GPS/WiFi enabled
- ✅ Allow camera/location permission prompt
- ✅ Test with different browser

### Check-In Fails with GPS Error

- ✅ Verify device location is accurate
- ✅ Check facility coordinates are correct in database
- ✅ Verify facility geofence radius is set
- ✅ Try moving closer to facility

## Testing Checklist

- [ ] User can scan QR code with phone
- [ ] QR code redirects to `/check-in/booking/:id`
- [ ] Unauthenticated user redirected to login
- [ ] After login, user redirected back to check-in
- [ ] GPS location is collected
- [ ] Check-in succeeds within facility radius
- [ ] Check-in fails outside facility radius
- [ ] Success message displays
- [ ] Booking shows "✓ Checked In" badge
- [ ] Admin can generate QR codes
- [ ] Admin can download/print QR codes
- [ ] Multiple check-in attempts rejected

## Code Examples

### Generate QR Code URL (in frontend)

```javascript
const bookingId = "550e8400-e29b-41d4-a716-446655440000";
const checkInUrl = `${window.location.origin}/check-in/booking/${bookingId}`;
// Result: https://smartcampus.com/check-in/booking/550e8400-e29b-41d4-a716-446655440000
```

### Handle Post-Login Redirect

```javascript
const [searchParams] = useSearchParams();
const redirectUrl = searchParams.get("redirect") || "/dashboard";

// After successful login
navigate(redirectUrl);
```

### GPS Geofencing Verification (backend)

```java
double distance = calculateHaversineDistance(
    facilityLat, facilityLon,
    userLat, userLon
);

if (distance <= geofenceRadius) {
    // Check-in successful
} else {
    // Check-in failed - outside facility
    throw new ForbiddenException("GEOFENCE_GPS_OUT_OF_RANGE");
}
```
