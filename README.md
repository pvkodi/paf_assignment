# Smart Campus Operations Hub

Easy setup guide to run the app locally.

## What this starts

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Mail UI: http://localhost:1080
- PostgreSQL: localhost:5432

## Fastest way (recommended)

Run everything with Docker.

1. Open a terminal at the project root.
2. Start all services:

```bash
cd infra
docker compose up --build -d
```

3. Wait around 20-40 seconds for backend startup.
4. Open http://localhost:5173

## Stop everything

```bash
cd infra
docker compose down
```

## Fresh reset (clean database)

Use this if you want a brand new DB with migrations + seed data reapplied.

```bash
cd infra
docker compose down
docker volume rm -f infra_postgres_data
docker compose up --build -d
```

## Alternative: run backend + frontend in dev mode

Use this if you want hot reload for code changes.

1. Start infra only:

```bash
cd infra
docker compose up -d postgres mailtrap
```

2. Start backend (new terminal):

```bash
cd backend/api
./mvnw spring-boot:run
```

3. Start frontend (new terminal):

```bash
cd frontend
npm install
npm run dev
```

## One-command helper scripts

From project root:

- macOS/Linux:

```bash
chmod +x start-services.sh
./start-services.sh
```

- Windows:

```bat
start-services.bat
```

## If something fails

- Port already in use:
	- 5432 (Postgres)
	- 8080 (Backend)
	- 5173 (Frontend)
	- 1080 (Mail UI)
- Check logs:

```bash
cd infra
docker compose logs -f backend
docker compose logs -f postgres
```

## Environment files

These files already exist in this repo:

- backend/.env
- frontend/.env

Required OAuth values:

- backend/.env
	- GOOGLE_CLIENT_ID
	- GOOGLE_CLIENT_SECRET
- frontend/.env
	- VITE_GOOGLE_CLIENT_ID

If needed, recreate from examples:

```bash
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
```
