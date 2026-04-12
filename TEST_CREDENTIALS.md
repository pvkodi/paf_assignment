# Test User Credentials for Email/Password Authentication

All test users are created with the same password for easy testing.

## Login Credentials

| Email | Password | Role | Full Name |
|-------|----------|------|-----------|
| admin@smartcampus.edu | YourPassword123! | ADMIN | System Admin |
| lecturer@smartcampus.edu | YourPassword123! | LECTURER | Dr. Nimal Perera |
| tech@smartcampus.edu | YourPassword123! | TECHNICIAN | S. Fernando |
| facility.manager@smartcampus.edu | YourPassword123! | FACILITY_MANAGER | A. Jayasekara |
| student@smartcampus.edu | YourPassword123! | USER | Pamali Student |

## Steps to Update Database

### 1. Drop Old Seed Data and Migrations
```bash
# Connect to PostgreSQL
psql -h localhost -U smartcampus -d smartcampus

# Drop new migrations first (V3, then V2)
DROP TABLE IF EXISTS cascade ...

# Or use Docker
docker exec smartcampus-postgres psql -U smartcampus -d smartcampus -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

### 2. Backup Old V2 Migration
```bash
# Rename the old seed file
mv backend/api/src/main/resources/db/migration/V2__seed_core_data.sql \
   backend/api/src/main/resources/db/migration/V2__seed_core_data_OLD.sql
```

### 3. Use New Seed File
```bash
# Rename the new file to be the active V2
mv backend/api/src/main/resources/db/migration/V2__seed_core_data_new.sql \
   backend/api/src/main/resources/db/migration/V2__seed_core_data.sql
```

### 4. Rebuild with Docker Compose
```bash
cd infra
docker compose down -v  # Remove volumes to reset database
docker compose up --build
```

## Generate Your Own Password Hashes

If you want to use different passwords, generate bcrypt hashes:

### Online Tool (Quickest)
1. Go to: https://bcrypt-generator.com/
2. Enter your password
3. Set cost to 10
4. Copy the hash
5. Update SQL seed file with new hash

### Command Line (Node.js)
```bash
node -e "console.log(require('bcryptjs').hashSync('your-password-here', 10))"
```

### Python
```bash
python3 -c "import bcrypt; print(bcrypt.hashpw(b'your-password-here', bcrypt.gensalt(10)).decode('utf-8'))"
```

## Current Hash Reference

All test users currently use:
- **Password**: `YourPassword123!`
- **Bcrypt Hash (Strength 12)**: `$2a$12$m87VhA68iLLOJgng0JKBLuQhPkc4Oz9xWcQ1RtD87a9PY3gTjolFa`

This hash was generated with `BCryptPasswordEncoder(strength: 12)` to match the backend configuration.

## Testing Email/Password Login

1. Start the application stack:
   ```bash
   cd infra && docker compose up --build
   ```

2. Open frontend: http://localhost:5173

3. Click "Login with Email/Password" (or similar option)

4. Use any of the credentials above

5. You'll be authenticated and can use the application!

## Backend API Direct Test

```bash
# Get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student@smartcampus.edu",
    "password": "YourPassword123!"
  }'

# Use token to access protected endpoints
curl -H "Authorization: Bearer TOKEN_HERE" \
  http://localhost:8080/api/v1/facilities
```

