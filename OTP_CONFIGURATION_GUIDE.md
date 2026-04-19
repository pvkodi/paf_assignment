# OTP Registration - Configuration Guide

## Backend Configuration

### 1. Application Properties

Add these properties to `application.properties` or `application.yml`:

#### application.yaml (Recommended)

```yaml
# OTP Configuration
otp:
  expiration-minutes: 10
  max-attempts: 5

# Email Configuration (integrates with existing app.mail namespace)
app:
  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:1025}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    from: ${MAIL_FROM:noreply@smartcampus.local}
    properties:
      mail:
        smtp:
          auth: ${MAIL_SMTP_AUTH:false}
          starttls:
            enable: ${MAIL_STARTTLS_ENABLE:false}
            required: ${MAIL_STARTTLS_REQUIRED:false}
          debug: false

# Note: Database and JWT configuration already present in application.yaml
# (No changes needed - existing config works)
```

#### application.properties (Alternative)

```properties
# ============================================
# OTP Configuration
# ============================================
otp.expiration-minutes=10
otp.max-attempts=5

# ============================================
# Email Configuration (Existing app.mail namespace)
# ============================================
app.mail.host=${MAIL_HOST:localhost}
app.mail.port=${MAIL_PORT:1025}
app.mail.username=${MAIL_USERNAME:}
app.mail.password=${MAIL_PASSWORD:}
app.mail.from=${MAIL_FROM:noreply@smartcampus.local}
app.mail.properties.mail.smtp.auth=${MAIL_SMTP_AUTH:false}
app.mail.properties.mail.smtp.starttls.enable=${MAIL_STARTTLS_ENABLE:false}
app.mail.properties.mail.smtp.starttls.required=${MAIL_STARTTLS_REQUIRED:false}
```

### 2. Environment Variables

Create `.env` file in backend root or set system variables:

```bash
# ============================================
# For Testing with MailHog (Default)
# ============================================
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=noreply@smartcampus.local
MAIL_SMTP_AUTH=false
MAIL_STARTTLS_ENABLE=false
MAIL_STARTTLS_REQUIRED=false

# ============================================
# For Testing with Mailtrap
# ============================================
# Uncomment and set these, comment out MailHog vars above
# MAIL_HOST=smtp.mailtrap.io
# MAIL_PORT=587
# MAIL_USERNAME=your_mailtrap_username
# MAIL_PASSWORD=your_mailtrap_password
# MAIL_FROM=noreply@smartcampus.edu
# MAIL_SMTP_AUTH=true
# MAIL_STARTTLS_ENABLE=true
# MAIL_STARTTLS_REQUIRED=true

# ============================================
# OTP Settings (Optional - defaults work fine)
# ============================================
OTP_EXPIRATION_MINUTES=10
OTP_MAX_ATTEMPTS=5
```

### 3. Email Service Setup Options

#### Option A: MailHog (Default - Recommended for Development)

Already configured by default. No setup needed!

```bash
# Backend will use MailHog on localhost:1025
# View emails at: http://localhost:8025
# No credentials needed
```

#### Option B: Mailtrap (For Testing with Real SMTP)

1. **Create Account**
   - Visit: https://mailtrap.io/
   - Sign up (free tier available)

2. **Create Inbox**
   - Name: "VenueLink OTP Testing"

3. **Get Credentials**
   - Go to: Demo Inbox → Integrations → JavaMail
   - Copy username and password

4. **Update Environment Variables**

   ```bash
   MAIL_HOST=smtp.mailtrap.io
   MAIL_PORT=587
   MAIL_USERNAME=your_username
   MAIL_PASSWORD=your_password
   MAIL_FROM=noreply@smartcampus.edu
   MAIL_SMTP_AUTH=true
   MAIL_STARTTLS_ENABLE=true
   MAIL_STARTTLS_REQUIRED=true
   ```

5. **Test Email Sending**
   ```bash
   # Restart backend with new environment variables
   # When OTP is sent, check Mailtrap inbox
   # Visit: https://mailtrap.io/inboxes → Demo Inbox
   ```

### 4. Production Email Service Setup

#### Option C: Gmail (Not Recommended)

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=app-specific-password  # Use App Password, not regular password
MAIL_SMTP_AUTH=true
MAIL_STARTTLS_ENABLE=true
MAIL_STARTTLS_REQUIRED=true
```

#### Option D: AWS SES (Recommended for Production)

```bash
MAIL_HOST=email-smtp.us-east-1.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=${AWS_SES_USERNAME}
MAIL_PASSWORD=${AWS_SES_PASSWORD}
MAIL_FROM=noreply@smartcampus.edu
MAIL_SMTP_AUTH=true
MAIL_STARTTLS_ENABLE=true
MAIL_STARTTLS_REQUIRED=true
```

#### Option E: SendGrid (Alternative)

```bash
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=${SENDGRID_API_KEY}
MAIL_FROM=noreply@smartcampus.edu
MAIL_SMTP_AUTH=true
MAIL_STARTTLS_ENABLE=true
MAIL_STARTTLS_REQUIRED=true
```

### 5. Run Backend

```bash
cd backend/api

# Build and run
mvn clean install
mvn spring-boot:run

# Verify startup
# Look for: "OtpEmailObserver subscribed to EventPublisher"
# Look for: "OtpService initialized"
# Look for: Migration "V12__create_otp_verification_table" applied
```

## Frontend Configuration

### 1. Environment Variables

Create `.env` file in frontend root:

```env
# API Configuration
VITE_API_URL=http://localhost:8080/api
VITE_API_BASE_URL=http://localhost:8080

# OAuth Configuration (if using Google OAuth)
VITE_GOOGLE_CLIENT_ID=your_google_client_id

# App Configuration
VITE_APP_NAME=VenueLink
VITE_ENV=development
```

### 2. Run Frontend

```bash
cd frontend

# Install dependencies
npm install

# Run development server
npm run dev

# Frontend runs on: http://localhost:5173

# Or with different port
npm run dev -- --port 3000
```

### 3. Test OTP Registration Page

```
Open in browser: http://localhost:5173/register-otp
# Should display:
# - "VenueLink Registration" heading
# - Email input field
# - "Send OTP" button
# - Login link
```

## Database Setup

### 1. PostgreSQL Connection

Ensure PostgreSQL is running with correct connection:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/smart_campus
spring.datasource.username=postgres
spring.datasource.password=your_password

# Flyway migration (automatic)
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

### 2. Verify Migration

```bash
# After backend starts, check PostgreSQL
psql -U postgres -d smart_campus

# List tables
\dt

# Check otp_verification table
SELECT * FROM otp_verification LIMIT 0;

# Check indices
\d otp_verification
```

Expected output:

```
                    Table "public.otp_verification"
       Column    |           Type           |
-----------------+--------------------------+
 id              | uuid                     |
 email           | character varying(255)   |
 code            | character varying(6)     |
 status          | character varying(50)    |
 expires_at      | timestamp with time zone |
 created_at      | timestamp with time zone |
 verified_at     | timestamp with time zone |
 attempts        | integer                  |

Indices:
    "otp_verification_pkey" PRIMARY KEY, btree (id)
    "idx_otp_email" btree (email)
    "idx_otp_email_code" btree (email, code)
    "idx_otp_status" btree (status)
```

## Docker Setup (Optional)

### Docker Compose for PostgreSQL + Mail

```yaml
# docker-compose.yml
version: "3.8"

services:
  postgres:
    image: postgres:15-alpine
    container_name: smart_campus_db
    environment:
      POSTGRES_DB: smart_campus
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  mailhog:
    image: mailhog/mailhog
    container_name: smart_campus_mail
    ports:
      - "1025:1025" # SMTP port
      - "8025:8025" # Web UI port (http://localhost:8025)

volumes:
  postgres_data:
```

Run:

```bash
# Start containers
docker-compose up -d

# Check PostgreSQL
psql -h localhost -U postgres -d smart_campus

# Check Mail (Web UI)
# Open browser: http://localhost:8025
```

Update environment variables or `.env`:

```bash
# For MailHog (testing only)
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_SMTP_AUTH=false
MAIL_STARTTLS_ENABLE=false
MAIL_STARTTLS_REQUIRED=false
```

## Verify Installation

### Checklist

- [ ] Backend running on port 8080
- [ ] Frontend running on port 5173 or 3000
- [ ] PostgreSQL database created
- [ ] Flyway V12 migration applied
- [ ] otp_verification table exists
- [ ] Email service configured (Mailtrap/SES/etc)
- [ ] OTP endpoints working (POST /api/v1/auth/otp/send)
- [ ] Frontend page loads (http://localhost:5173/register-otp)
- [ ] OTP emails sending successfully
- [ ] JWT token generation working

### Test Commands

```bash
# Test backend health
curl http://localhost:8080/api/v1/auth/profile

# Test OTP endpoint
curl -X POST http://localhost:8080/api/v1/auth/otp/send \
  -H "Content-Type: application/json" \
  -d '{"email":"test@smartcampus.edu"}'

# Test database
psql -U postgres -d smart_campus -c "SELECT COUNT(*) FROM otp_verification;"
```

## Logs & Monitoring

### Backend Logs

Watch for these logs to verify everything is working:

```
# Email Observer subscribed
INFO  [EmailObserver] EmailObserver subscribed to EventPublisher

# OTP Service initialized
INFO  [OtpService] OTP created for email: test@smartcampus.edu (expires in 10 minutes)

# OTP Email Observer handling event
INFO  [OtpEmailObserver] Successfully sent HIGH priority email to test@smartcampus.edu

# OTP verified
INFO  [OtpService] OTP verified successfully for email: test@smartcampus.edu

# User registered from OTP
INFO  [RegistrationRequestService] User created from OTP registration: [id] (test@smartcampus.edu)
```

### Common Issues & Solutions

#### Issue: OTP emails not sending

```
Solution:
1. Check Mailtrap credentials in application.properties
2. Verify email service is running
3. Check backend logs for OtpEmailObserver errors
4. Test email configuration: Spring Mail Test endpoint
```

#### Issue: OTP table doesn't exist

```
Solution:
1. Verify Flyway is enabled in application.properties
2. Check db/migration folder has V12 file
3. Check PostgreSQL logs for migration errors
4. Manually run V12 migration script if needed
```

#### Issue: Frontend can't reach backend

```
Solution:
1. Verify VITE_API_URL in .env is correct
2. Check CORS configuration in backend (already configured)
3. Verify backend is running on correct port
4. Check browser console for API errors
```

#### Issue: JWT token not working after registration

```
Solution:
1. Verify token is being returned from verify-and-register endpoint
2. Check token expiration time
3. Verify Authorization header format: "Bearer {token}"
4. Check JWT secret in backend configuration
```

## Performance Tuning

### Database Indices

The migration already creates optimal indices:

- `idx_otp_email` - For finding OTPs by email
- `idx_otp_email_code` - For verifying OTP code
- `idx_otp_status` - For cleanup queries

No additional tuning needed unless you have 100,000+ OTP records.

### Cleanup Schedule

For production, schedule automatic OTP cleanup:

```java
// In a scheduled task
@Scheduled(cron = "0 0 * * * *")  // Daily at midnight
public void cleanupExpiredOtps() {
    otpService.cleanupExpiredOtps();
}
```

## Security Hardening

### HTTPS (Production)

```properties
# Enable HTTPS
server.ssl.enabled=true
server.ssl.key-store=${SSL_KEYSTORE_PATH}
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
```

### Rate Limiting (Optional)

```java
// Add to AuthController or filter
@RateLimiter(limit = "10 per minute")
@PostMapping("/otp/send")
public ResponseEntity<?> sendOtp(...) { }
```

### Email Domain Whitelist

Currently hardcoded to `@smartcampus.edu`. To make configurable:

```properties
# application.properties
otp.allowed-domain=@smartcampus.edu

# Or allow multiple domains
otp.allowed-domains=@smartcampus.edu,@student.smartcampus.edu
```

Then update OtpService to use config:

```java
@Value("${otp.allowed-domain:@smartcampus.edu}")
private String allowedDomain;

public boolean isValidDomain(String email) {
    return email.toLowerCase().endsWith(allowedDomain);
}
```

## Backup & Recovery

### Backup OTP Data

```bash
# Backup otp_verification table
pg_dump -U postgres -d smart_campus -t otp_verification > otp_backup.sql

# Backup full database
pg_dump -U postgres -d smart_campus > full_backup.sql
```

### Restore OTP Data

```bash
# Restore from backup
psql -U postgres -d smart_campus < otp_backup.sql
```

---

**Configuration Complete!** ✅

Next: Run [OTP_QUICK_START.md](OTP_QUICK_START.md) for testing.
