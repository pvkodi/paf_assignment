# Quickstart: VenueLink Operations Hub

**Last Updated**: 2026-04-11  
**Status**: ✓ Production Ready  
**Full Stack**: Auth (US1), Bookings (US2), Tickets (US4), Notifications (US5)

---

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Architecture Overview](#architecture-overview)
3. [Quick Start (5 minutes)](#quick-start-5-minutes)
4. [Detailed Setup](#detailed-setup)
5. [Verification Checklist](#verification-checklist)
6. [Docker Persistence](#docker-persistence)
7. [Health Checks](#health-checks)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Operational Commands](#operational-commands)
10. [Environment Reference](#environment-reference)

---

## System Requirements

### Prerequisites
- **Java**: OpenJDK 21 or later
- **Maven**: 3.9.0 or later (bundled: `mvnw` included)
- **Node.js**: 20.x LTS or later (npm 10+ included)
- **Docker**: 24.0 or later
- **Docker Compose**: 2.20 or later
- **Git**: 2.40 or later

### Recommended Hardware
- **CPU**: 4+ cores
- **RAM**: 8GB minimum (16GB recommended)
- **Disk**: 20GB free space for Docker images and application data

### Supported Operating Systems
- Ubuntu 22.04 LTS / 24.04 LTS
- macOS 13+ (Intel and Apple Silicon)
- Windows 10/11 (WSL2 with Docker Desktop)
- Red Hat Enterprise Linux 9+

### Verify Prerequisites
```bash
# Check Java
java -version
# Expected: OpenJDK 21 or later

# Check Node.js
node -v
npm -v
# Expected: Node v20.x, npm v10+

# Check Docker
docker --version
docker compose version
# Expected: Docker 24.0+, Docker Compose v2.20+

# Check Git
git --version
# Expected: Git 2.40+
```

---

## Architecture Overview

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                     VenueLink Hub                         │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  Frontend (React 18 + Vite)          Backend (Spring Boot)  │
│  http://localhost:5173                http://localhost:8080  │
│  ├── Auth Pages (OAuth Login)         ├── Auth Service      │
│  ├── Dashboard                        ├── Booking Service   │
│  ├── Booking System                   ├── Ticket Service    │
│  ├── Ticket Management                ├── Notification Svc  │
│  └── Analytics                        └── Admin Endpoints   │
│                                                               │
├─────────────────────────────────────────────────────────────┤
│  Infrastructure (Docker Compose)                             │
│  ├── PostgreSQL 15 (port 5432)                              │
│  │   └── Data volume: infra_postgres_data (persistent)       │
│  └── MailDev (port 1025/1080)                               │
│      └── Email testing & debugging                          │
├─────────────────────────────────────────────────────────────┤
│  External Services                                           │
│  ├── Google OAuth 2.0 (authentication)                      │
│  └── SMTP (Mailtrap for development)                        │
└─────────────────────────────────────────────────────────────┘

Data Flow:
Frontend (React) <--HTTP/WebSocket--> Backend (Spring Boot)
Backend <--JDBC--> PostgreSQL
Backend <--SMTP--> MailDev/Mailtrap
```

### Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Frontend** | React | 18.x | UI framework |
| **Frontend** | Vite | 5.x | Build tool & dev server |
| **Backend** | Spring Boot | 3.x | REST API & scheduling |
| **Database** | PostgreSQL | 15.17 | Primary data store |
| **Persistence** | JPA/Hibernate | 3.x | ORM framework |
| **Security** | Spring Security | 6.x | OAuth 2.0 & authorization |
| **Email** | MailDev | 2.1.0 | Development SMTP server |
| **Container** | Docker | 24.0+ | Service orchestration |

---

## Quick Start (5 minutes)

### Prerequisite: Complete OAuth Setup
Before starting, ensure Google OAuth credentials are configured:
```bash
# Reference: OAUTH_SETUP_GUIDE.md
# Ensure you have:
# ✓ Google OAuth Client ID
# ✓ Google OAuth Client Secret
# ✓ Authorized redirect URIs configured
```

### Option 1: Automatic Start (Recommended)

#### Windows
```bash
# From project root
.\start-services.bat
```

#### Linux/macOS
```bash
# From project root
chmod +x start-services.sh
./start-services.sh
```

#### What This Does:
1. ✓ Validates Docker Compose is installed
2. ✓ Starts PostgreSQL and MailDev containers
3. ✓ Waits for PostgreSQL to be ready
4. ✓ Starts Spring Boot backend (port 8080)
5. ✓ Starts Vite frontend dev server (port 5173)
6. ✓ Opens browser to http://localhost:5173

### Option 2: Manual Start

#### Step 1: Start Infrastructure (Terminal 1)
```bash
cd infra
docker compose up -d
```

Expected output:
```
✓ Container smartcampus-postgres  Started
✓ Container smartcampus-mailtrap  Started
```

#### Step 2: Start Backend (Terminal 2)
```bash
cd backend/api
./mvnw spring-boot:run
```

Expected output:
```
2026-04-11 09:40:00 INFO  com.sliitreserve.api.ApiApplication
Started ApiApplication in 8.234 seconds
```

#### Step 3: Start Frontend (Terminal 3)
```bash
cd frontend
npm install  # First time only
npm run dev
```

Expected output:
```
  VITE v5.0.0  ready in 234 ms

  ➜  Local:   http://localhost:5173/
  ➜  press h to show help
```

### Step 4: Access the Application
- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Email Testing**: http://localhost:1080

### Step 5: Login with Google OAuth
1. Click "Login with Google" on the frontend
2. Authorize the application when prompted
3. You'll be redirected to the dashboard
4. JWT token stored in browser localStorage

---

## Detailed Setup

### 1. Environment Configuration

#### Backend Environment (`backend/api/application.properties` or `.env`)

```properties
# Database Connection
spring.datasource.url=jdbc:postgresql://localhost:5432/smartcampus
spring.datasource.username=smartcampus
spring.datasource.password=smartcampus
spring.jpa.hibernate.ddl-auto=validate

# OAuth 2.0
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,email,profile

# JWT Configuration
app.security.jwt.secret=${JWT_SECRET}
app.security.jwt.expiry-hours=24
app.security.jwt.refresh-expiry-hours=168

# CORS Configuration
app.security.cors.allowed-origins=http://localhost:5173,http://localhost:3000

# Email/SMTP (for notifications)
spring.mail.host=localhost
spring.mail.port=1025
spring.mail.username=maildev
spring.mail.password=maildev
mail.from.address=noreply@smartcampus.local

# Timezone
app.timezone=Asia/Colombo

# Upload Directory
app.upload.dir=./uploads
app.upload.max-file-size=5MB
app.upload.allowed-types=image/jpeg,image/png,image/webp

# Logging
logging.level.root=INFO
logging.level.com.sliitreserve=DEBUG
```

#### Alternative: Environment Variables

```bash
# Windows (PowerShell)
$env:GOOGLE_CLIENT_ID = "your-client-id.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET = "your-client-secret"
$env:JWT_SECRET = "your-long-random-secret-key-min-32-chars"
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/smartcampus"

# Linux/macOS (Bash)
export GOOGLE_CLIENT_ID="your-client-id.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="your-client-secret"
export JWT_SECRET="your-long-random-secret-key-min-32-chars"
export DATABASE_URL="jdbc:postgresql://localhost:5432/smartcampus"
```

#### Frontend Environment (`frontend/.env.local`)

```env
# Backend API
VITE_API_URL=http://localhost:8080
VITE_API_TIMEOUT=30000

# OAuth Configuration
VITE_GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com

# Feature Flags
VITE_ENABLE_ANALYTICS=true
VITE_ENABLE_NOTIFICATIONS=true
VITE_ENABLE_BOOKING_SYSTEM=true
```

### 2. First-Time Setup

```bash
# Clone repository
git clone <repository-url>
cd paf_assignment

# Create uploads directory
mkdir -p uploads

# (Optional) Set up IDE for hot reload
# IntelliJ IDEA: Enable "Build project automatically"
# VS Code: Install "Spring Boot Extension Pack"

# Verify all prerequisites
java -version
npm -v
docker --version
git --version
```

### 3. Database Initialization

```bash
# Automatic: Handled by Spring Boot migrations (Flyway)
# Migrations located in: backend/api/src/main/resources/db/migration/

# Manual verification: Connect to database
docker exec -it smartcampus-postgres psql -U smartcampus -d smartcampus

# List tables (inside psql)
\dt
```

### 4. Running Tests

#### Backend Unit & Integration Tests
```bash
cd backend/api

# Run all tests
./mvnw test -P integration

# Run specific test class
./mvnw test -Dtest=BookingServiceTest

# Run tests with coverage
./mvnw test jacoco:report
# Coverage report: target/site/jacoco/index.html
```

#### Frontend Tests
```bash
cd frontend

# Run tests
npm test

# Run with coverage
npm run test:coverage
```

---

## Verification Checklist

### ✓ Service Health

```bash
# Docker Compose status
cd infra
docker compose ps

# Expected output:
# NAME                   IMAGE              STATUS
# smartcampus-postgres   postgres:15        Up ... (healthy)
# smartcampus-mailtrap   maildev/maildev    Up ... (healthy)
```

### ✓ Database Connectivity

```bash
# Test database connection
docker exec smartcampus-postgres pg_isready -U smartcampus

# Expected: "accepting connections"

# List tables
docker exec smartcampus-postgres psql -U smartcampus -d smartcampus -c "\dt"

# Expected: 17 tables (users, bookings, tickets, etc.)
```

### ✓ Backend API

```bash
# Health check endpoint
curl http://localhost:8080/actu ator/health

# Expected response:
# {"status":"UP","components":{...}}

# API documentation
open http://localhost:8080/swagger-ui.html

# Expected: OpenAPI 3.0 documentation page
```

### ✓ Frontend Application

```bash
# Visit application
open http://localhost:5173

# Expected: Login page with "Login with Google" button
```

### ✓ Email Service

```bash
# Visit MailDev interface
open http://localhost:1080

# Expected: Empty inbox (no emails yet)
```

### ✓ OAuth Integration

```bash
# Complete OAuth login flow:
1. Visit http://localhost:5173
2. Click "Login with Google"
3. Authorize app
4. Verify redirect to dashboard
5. Check browser console: should see JWT token
6. Check localStorage: should have 'auth_token' key
```

### ✓ End-to-End Workflow

```bash
# Test booking creation:
1. Login to frontend
2. Navigate to Bookings
3. Create new booking
4. Verify data appears in database:
   docker exec smartcampus-postgres \
     psql -U smartcampus -d smartcampus \
     -c "SELECT * FROM booking LIMIT 1;"

# Test ticket creation:
1. Create maintenance ticket
2. Upload image attachment
3. Verify file exists: ./uploads/
4. Check notification sent (MailDev: http://localhost:1080)
```

---

## Docker Persistence

### Persistence Strategy

The application uses **named volumes** for data persistence. This ensures data survives container restarts and removals.

#### Volume Configuration

```yaml
# From infra/docker-compose.yml
volumes:
  postgres_data:
    # Managed by Docker - location varies by OS
    # Linux: /var/lib/docker/volumes/infra_postgres_data/_data
    # macOS: ~/Library/Containers/com.docker.docker/Volumes/
    # Windows: C:\ProgramData\Docker\volumes\
```

### Verify Data Persistence

```bash
# Step 1: Check volume exists
docker volume ls
# Expected: infra_postgres_data

# Step 2: Create test data
docker exec smartcampus-postgres psql -U smartcampus -d smartcampus \
  -c "INSERT INTO \"user\" (email, full_name, role, created_at) \
      VALUES ('test@example.com', 'Test User', 'STUDENT', now());"

# Step 3: Stop container
cd infra
docker compose stop postgres

# Step 4: Restart container
docker compose start postgres
# Wait 5-10 seconds for PostgreSQL to initialize

# Step 5: Verify data persists
docker exec smartcampus-postgres psql -U smartcampus -d smartcampus \
  -c "SELECT email FROM \"user\" WHERE email='test@example.com';"
# Expected: test@example.com (row should exist)
```

### Backup & Recovery

#### Backup Database

```bash
# Full backup to SQL file
docker exec smartcampus-postgres pg_dump -U smartcampus smartcampus \
  > backups/smartcampus-$(date +%Y%m%d-%H%M%S).sql

# Backup with custom format (smaller, faster recovery)
docker exec smartcampus-postgres pg_dump -U smartcampus \
  -Fc smartcampus > backups/smartcampus.dump
```

#### Restore from Backup

```bash
# Restore from SQL file
cat backups/smartcampus-20260411-094000.sql | \
  docker exec -i smartcampus-postgres psql -U smartcampus -d smartcampus

# Restore from custom format
docker exec -i smartcampus-postgres pg_restore -U smartcampus \
  -d smartcampus < backups/smartcampus.dump
```

### Cleanup & Volume Management

```bash
# List all volumes
docker volume ls

# Inspect volume details
docker volume inspect infra_postgres_data

# Remove volume (WARNING: destroys data)
docker volume rm infra_postgres_data

# Prune unused volumes
docker volume prune
# This removes volumes not used by any container (safe for cleanup)

# Prune all Docker artifacts (images, containers, volumes)
docker system prune -a --volumes
```

---

## Health Checks

### Automatic Health Checks

Docker automatically monitors service health via health checks defined in `docker-compose.yml`:

```bash
# View health status
docker compose ps

# Watch health in real-time
docker compose ps --no-trunc

# Get detailed health info
docker inspect smartcampus-postgres --format='{{.State.Health.Status}}'
```

### Manual Health Verification

#### PostgreSQL Health

```bash
# Connection test
docker exec smartcampus-postgres pg_isready -U smartcampus -d smartcampus

# Query execution
docker exec smartcampus-postgres psql -U smartcampus smartcampus -c "SELECT 1;"

# Connection pooling (from application)
curl http://localhost:8080/actuator/db
```

#### Backend Health

```bash
# Standard health endpoint
curl http://localhost:8080/actuator/health

# Detailed component health
curl http://localhost:8080/actuator/health/details

# Database connection pool status
curl http://localhost:8080/actuator/metrics/tomcat.connections.current
```

#### MailDev Health

```bash
# Email service status
curl http://localhost:1080/api/config

# Expected response includes email count and storage info
```

### Performance Monitoring

```bash
# View container resource usage
docker stats --no-stream

# Expected output shows CPU%, Memory usage for each service

# Monitor logs in real-time
docker compose logs -f postgres
docker compose logs -f mailtrap
```

---

## Troubleshooting Guide

### Common Issues & Solutions

#### Issue 1: Port Already in Use

**Symptom**: `Error: listen EADDRINUSE: address already in use :::5173`

**Solution**:
```bash
# Find process using port
# Windows
netstat -ano | findstr ":5173"
taskkill /PID <PID> /F

# Linux/macOS
lsof -i :5173
kill -9 <PID>

# Or change port in docker-compose.yml
# Change: "5173:5173" to "5174:5173"
```

#### Issue 2: PostgreSQL Connection Refused

**Symptom**: `FATAL: sorry, too many clients already connected`

**Solution**:
```bash
# Reduce active connections
docker compose restart postgres

# Or increase max connections
docker exec smartcampus-postgres psql -U smartcampus -d smartcampus \
  -c "ALTER SYSTEM SET max_connections=400;"
docker compose restart postgres
```

#### Issue 3: Database Not Initializing

**Symptom**: Tables don't exist, migrations not running

**Solution**:
```bash
# Check migration status
docker exec smartcampus-postgres psql -U smartcampus -d smartcampus \
  -c "SELECT * FROM flyway_schema_history;"

# Manual migration run
cd backend/api
./mvnw flyway:clean
./mvnw flyway:migrate

# Or rebuild container
docker compose stop postgres
docker volume rm infra_postgres_data
docker compose up postgres
```

#### Issue 4: OAuth Login Fails

**Symptom**: Redirect loop, invalid credentials, or CORS errors

**Checklist**:
```bash
# 1. Verify Google credentials in environment
echo $GOOGLE_CLIENT_ID
echo $GOOGLE_CLIENT_SECRET

# 2. Check CORS configuration (backend)
curl -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET" \
  http://localhost:8080/api/users -v

# 3. Verify OAuth redirect URI in Google Console
# Must match exactly: http://localhost:8080/login/oauth2/code/google

# 4. Check browser console (F12) for detailed error messages
```

#### Issue 5: Frontend Can't Connect to Backend

**Symptom**: `Failed to fetch`, `CORS error`, or blank dashboard

**Solution**:
```bash
# Verify backend is running
curl http://localhost:8080/actuator/health

# Check frontend API URL
cat frontend/.env.local
# Ensure VITE_API_URL=http://localhost:8080

# Check CORS headers
curl -v http://localhost:8080/api/users

# Verify network connectivity
ping localhost
```

#### Issue 6: Email Not Sending / MailDev Not Working

**Symptom**: Notifications not shown in MailDev, or errors in logs

**Solution**:
```bash
# Verify MailDev is running and healthy
docker compose ps
# Should show: smartcampus-mailtrap ... Up

# Check SMTP configuration in backend
docker compose logs mailtrap

# Test SMTP connection
telnet localhost 1025
# Should connect successfully

# Access MailDev UI
open http://localhost:1080

# Trigger a test email (via API or frontend action)
```

#### Issue 7: Running Out of Disk Space

**Symptom**: Docker operations fail with "no space left", or slow performance

**Solution**:
```bash
# Check disk usage
docker system df

# Clean up unused Docker artifacts
docker image prune -a           # Remove unused images
docker container prune          # Remove stopped containers
docker volume prune             # Remove unused volumes
docker system prune -a --volumes # Everything

# Or selective cleanup
docker rmi <image-id>           # Remove specific image
docker volume rm infra_postgres_data  # Remove specific volume (data lost)
```

#### Issue 8: Inconsistent Data State

**Symptom**: Data looks corrupted, missing rows, or constraint violations

**Solution**:
```bash
# Backup current state
docker exec smartcampus-postgres pg_dump -U smartcampus smartcampus \
  > backup-before-recovery.sql

# Check database integrity
docker exec smartcampus-postgres psql -U smartcampus -d smartcampus \
  -c "REINDEX DATABASE smartcampus;"

# Or restore from backup
docker exec -i smartcampus-postgres psql -U smartcampus -d smartcampus \
  < backup-before-recovery.sql
```

#### Issue 9: Memory/CPU Usage Extremely High

**Symptom**: Computer becomes unresponsive, fans spinning loud

**Solution**:
```bash
# Monitor container resource usage
docker stats

# Limit container resources
# Edit docker-compose.yml:
# services:
#   postgres:
#     deploy:
#       resources:
#         limits:
#           cpus: '2'
#           memory: 2G

# Restart with limits
docker compose down
docker compose up -d
```

#### Issue 10: "Docker daemon is not running"

**Symptom**: `Error: Cannot connect to Docker daemon`

**Solution**:
```bash
# Start Docker daemon
# Windows: Start "Docker Desktop" application
# macOS: open /Applications/Docker.app
# Linux: sudo systemctl start docker

# Verify Docker is running
docker ps
```

---

## Operational Commands

### Quick Reference

```bash
# Start everything
cd infra && docker compose up -d
cd ../backend/api && ./mvnw spring-boot:run &
cd ../frontend && npm run dev

# Check status
docker compose ps
curl http://localhost:8080/actuator/health

# View logs
docker compose logs -f postgres
docker compose logs -f mailtrap

# Stop everything
docker compose down
# (Data persists in volumes)

# Clean restart (fresh database)
docker compose down
docker volume rm infra_postgres_data
docker compose up -d
```

### Database Management

```bash
# Connect to database
docker exec -it smartcampus-postgres psql -U smartcampus -d smartcampus

# Run SQL query
docker exec smartcampus-postgres psql -U smartcampus -d smartcampus \
  -c "SELECT COUNT(*) FROM booking;"

# Get database size
docker exec smartcampus-postgres psql -U smartcampus -d smartcampus \
  -c "SELECT pg_size_pretty(pg_database_size('smartcampus'));"

# List all users/roles
docker exec smartcampus-postgres psql -U smartcampus -d smartcampus \
  -c "SELECT usename, usesuper, usecreatedb FROM pg_user;"
```

### Container Lifecycle

```bash
# View container details
docker inspect smartcampus-postgres

# Check container logs
docker logs smartcampus-postgres
docker logs -f --tail=50 smartcampus-postgres

# Execute command in running container
docker exec smartcampus-postgres pwd
docker exec -it smartcampus-postgres bash

# Copy files to/from container
docker cp smartcampus-postgres:/var/lib/postgresql/data ./backup-data/
docker cp ./backup.sql smartcampus-postgres:/tmp/
```

### Performance Tuning

```bash
# View running processes
docker exec smartcampus-postgres ps aux

# Get PostgreSQL performance info
docker exec smartcampus-postgres psql -U smartcampus smartcampus \
  -c "SELECT * FROM pg_stat_activity;"

# Kill long-running queries
docker exec smartcampus-postgres psql -U smartcampus smartcampus \
  -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity \
      WHERE state='active' AND query_start < now() - interval '5 min';"
```

---

## Environment Reference

### Backend Configuration Properties

| Property | Default | Required | Description |
|----------|---------|----------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/smartcampus` | Yes | PostgreSQL connection URL |
| `spring.datasource.username` | `smartcampus` | Yes | Database user |
| `spring.datasource.password` | `smartcampus` | Yes | Database password |
| `GOOGLE_CLIENT_ID` | N/A | Yes | OAuth client ID from Google Console |
| `GOOGLE_CLIENT_SECRET` | N/A | Yes | OAuth client secret from Google Console |
| `JWT_SECRET` | N/A | Yes | Secret for signing JWT tokens (min 32 chars) |
| `JWT_EXPIRY_HOURS` | 24 | No | JWT token expiration time |
| `app.timezone` | `Asia/Colombo` | No | Application timezone for bookings |
| `spring.mail.host` | `localhost` | Yes | SMTP host for notifications |
| `spring.mail.port` | 1025 | No | SMTP port (MailDev) |
| `app.upload.dir` | `./uploads` | No | Directory for file uploads |
| `app.upload.max-file-size` | `5MB` | No | Maximum file upload size |

### Frontend Configuration Variables

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `VITE_API_URL` | `http://localhost:8080` | Yes | Backend API base URL |
| `VITE_API_TIMEOUT` | 30000 | No | HTTP request timeout (ms) |
| `VITE_GOOGLE_CLIENT_ID` | N/A | Yes | OAuth client ID (public) |
| `VITE_ENABLE_ANALYTICS` | true | No | Enable analytics features |
| `VITE_ENABLE_NOTIFICATIONS` | true | No | Enable notifications |
| `VITE_ENABLE_BOOKING_SYSTEM` | true | No | Enable booking system |

### Default Credentials (Development Only)

**⚠️ WARNING**: These should NEVER be used in production.

| Service | Username | Password | Port | URL |
|---------|----------|----------|------|-----|
| PostgreSQL | `smartcampus` | `smartcampus` | 5432 | `jdbc:postgresql://localhost:5432/smartcampus` |
| MailDev | (no auth) | - | 1025/1080 | `http://localhost:1080` |
| Backend | - | - | 8080 | `http://localhost:8080` |
| Frontend | - | - | 5173 | `http://localhost:5173` |

### Test Accounts

| Email | Role | Created By | Status |
|-------|------|-----------|--------|
| `admin@smartcampus.local` | ADMIN | Initial setup | ✓ Active |
| `student@smartcampus.local` | STUDENT | OAuth | ✓ Active |
| `lecturer@smartcampus.local` | LECTURER | OAuth | ✓ Active |

---

## Support & Additional Resources

### Documentation Files
- **OAuth Setup**: See [OAUTH_SETUP_GUIDE.md](../OAUTH_SETUP_GUIDE.md)
- **OAuth Checklist**: See [OAUTH_CHECKLIST.md](../OAUTH_CHECKLIST.md)
- **API Documentation**: Available at http://localhost:8080/swagger-ui.html
- **Architecture**: See [plan.md](./plan.md)
- **Data Model**: See [data-model.md](./data-model.md)

### Useful Links
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker Documentation](https://docs.docker.com)
- [Google OAuth 2.0](https://developers.google.com/identity/protocols/oauth2)

### Getting Help

1. **Check the logs**: `docker compose logs -f`
2. **Review this guide**: Search for your issue in the Troubleshooting section
3. **Verify prerequisites**: Run the prerequisite checks above
4. **Check GitHub Issues**: Look for similar problems in the repository
5. **Review API docs**: Open http://localhost:8080/swagger-ui.html

### Reporting Issues

When reporting issues, please include:
- Output of `docker compose ps`
- Output of `docker --version` and `docker compose version`
- Relevant log excerpts: `docker compose logs postgres`
- Steps to reproduce the issue
- Expected vs. actual behavior

---

**Last Verified**: 2026-04-11  
**Next Review**: 2026-05-11
