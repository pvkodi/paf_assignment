# Quickstart: Smart Campus Operations Hub

## Prerequisites
- Java 21
- Maven 3.9+
- Node.js 20+
- Docker and Docker Compose
- GitHub Actions enabled for CI in repository settings

## Repository layout
- backend: Spring Boot monolith
- frontend: React 18 + Vite
- uploads: local file storage mount point

## 1. Start infrastructure
```bash
docker compose up -d postgres mailtrap
```

## 2. Configure backend environment
Create `.env` (or `application-local.yml` mapped vars):
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/smartcampus
SPRING_DATASOURCE_USERNAME=smartcampus
SPRING_DATASOURCE_PASSWORD=smartcampus
GOOGLE_CLIENT_ID=replace-me
GOOGLE_CLIENT_SECRET=replace-me
JWT_SECRET=replace-with-strong-random-string
JWT_EXPIRY_HOURS=24
APP_TIMEZONE=Asia/Colombo
UPLOAD_DIR=./uploads
SMTP_HOST=smtp.mailtrap.io
SMTP_PORT=2525
SMTP_USER=replace-me
SMTP_PASS=replace-me
```

## 3. Initialize uploads directory
```bash
mkdir -p uploads
```

## 4. Run backend
```bash
cd backend
./mvnw spring-boot:run
```

## 5. Run frontend
```bash
cd frontend
npm install
npm run dev
```

## 6. Execute tests
### Backend unit and integration tests
```bash
cd backend
./mvnw test
```

### Frontend tests (if configured)
```bash
cd frontend
npm test
```

## 7. Validate key flows
- Authenticate with Google OAuth and verify JWT issuance.
- Confirm RBAC with each role and suspended-user restrictions.
- Create booking and simulate optimistic lock conflict to verify 409 behavior.
- Submit ticket with valid and invalid images (type/size/count checks).
- Trigger SLA escalation job and verify notification fan-out.
- Verify utilization snapshots and admin-only analytics endpoint.

## 8. Run scheduled jobs locally
- Hourly SLA checks: enabled via Spring `@Scheduled`.
- Daily utilization snapshots: enabled via Spring `@Scheduled`.
- For manual verification in local dev, expose admin/test endpoints or use a profile with reduced schedule intervals.

## 9. Docker persistence checks
- Ensure `uploads` is mounted as a Docker volume in `docker-compose.yml`.
- Restart containers and verify uploaded files and thumbnails persist.

## 10. CI expectations (GitHub Actions)
- Backend: build + unit tests + integration tests.
- Frontend: install + build + tests.
- Quality gates: fail pipeline when service method unit tests are missing for modified services.
