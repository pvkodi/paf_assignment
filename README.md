# Smart Campus — Operations Hub

This repository contains the Smart Campus Operations Hub prototype: a Spring Boot backend and a Vite + React frontend, with local infra (Postgres + mail) and a CI baseline.

## Prerequisites

- Java 21 (or later)
- Git (optional)
- Node.js 18+ and npm
- Docker / Docker Compose (for local Postgres + mail)

## Repo layout (important paths)

- Backend module: `backend/api`
- Frontend app: `frontend`
- Local infra compose: `infra/docker-compose.yml`
- CI pipeline: `.github/workflows/ci.yml`
- Tasks/specs: `specs/001-feat-pamali-smart-campus-ops-hub/tasks.md`

## Quick Start (local)

1) Start infrastructure (Postgres + maildev)

```bash
cd infra
docker compose up -d
```

2) Backend (Spring Boot)

- Copy or edit the env template:

```bash
cp ../backend/.env.example ../backend/.env
# edit backend/.env as needed (DB connection, JWT secret, SMTP, etc.)
```

- Run with Maven wrapper from the module directory:

```bash
cd ../backend/api
./mvnw -DskipTests spring-boot:run
```

Or build the artifact and run the JAR:

```bash
./mvnw -DskipTests package
java -jar target/*-SNAPSHOT.jar
```

Note: the backend module lives in `backend/api` (tasks may reference `backend/pom.xml` — use `backend/api/pom.xml`).

3) Frontend (Vite + React)

- Copy the frontend env template and set `VITE_API_BASE_URL` to your backend URL (e.g., `http://localhost:8080/api`):

```bash
cd ../../frontend
cp .env.example .env
```

- Install dependencies and run the dev server:

```bash
npm install
npm run dev
```

- Build for production:

```bash
npm run build
# optionally preview locally (e.g., with `npx serve -s dist`)
```

4) Running tests

- Backend unit tests:

```bash
cd backend/api
./mvnw test
```

- Frontend lint / tests:

```bash
cd frontend
npm run lint
# npm test (if tests are configured)
```

## CI

CI is configured in `.github/workflows/ci.yml` to build/test the backend and frontend. The backend job is pointed at the `backend/api` module.

## Helpful notes

- If you use Docker to run Postgres, ensure `backend/.env` DB connection values match the `infra/docker-compose.yml` credentials.
- The repo's Phase 1 setup tasks (T001–T008) are complete in `specs/001-feat-pamali-smart-campus-ops-hub/tasks.md`.
- If the Maven wrapper (`mvnw`) is missing, run with a local Maven installation (`mvn`) or add the wrapper to the module.

## Next steps (suggested)

- Harden `backend/.env.example` values and add `.env` to `.gitignore` (if not already ignored).
- Update `specs/.../tasks.md` wording to reference `backend/api/pom.xml` for consistency.

---

If you want, I can update `tasks.md` to fix the path wording and add a short `README` section inside the `backend` and `frontend` folders. Would you like me to do that now?
