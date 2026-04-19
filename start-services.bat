@echo off
REM ============================================
REM VenueLink Operations Hub - Startup Script
REM ============================================
REM This script starts all required services:
REM - PostgreSQL (Docker)
REM - MailDev (Docker)
REM - Backend API (Spring Boot)
REM - Frontend Dev Server (Vite)
REM
REM Prerequisites:
REM - Docker Desktop running and Docker Compose installed
REM - Java 21+ available
REM - Node.js 20+ with npm installed
REM
REM Usage: .\start-services.bat
REM ============================================

setlocal enabledelayedexpansion

echo.
echo ============================================
echo VenueLink Hub - Full Stack Startup
echo ============================================
echo.

REM Check prerequisites
echo [1/5] Checking prerequisites...
echo.

REM Check Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not installed or not running
    echo Please start Docker Desktop and try again
    pause
    exit /b 1
)
echo ✓ Docker found

REM Check Docker Compose
docker compose version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker Compose not found
    echo Please install Docker Compose
    pause
    exit /b 1
)
echo ✓ Docker Compose found

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo WARNING: Java not found in PATH
    echo Backend may not start correctly
)
for /f "tokens=*" %%i in ('java -version 2^>^&1') do set JAVA_VERSION=%%i
echo ✓ Java: %JAVA_VERSION%
echo.

REM Check Node.js
node --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Node.js is not installed
    pause
    exit /b 1
)
for /f "tokens=*" %%i in ('node --version') do set NODE_VERSION=%%i
echo ✓ Node.js: %NODE_VERSION%
for /f "tokens=*" %%i in ('npm --version') do set NPM_VERSION=%%i
echo ✓ npm: %NPM_VERSION%
echo.

REM Start infrastructure (Docker)
echo [2/5] Starting infrastructure (PostgreSQL, MailDev)...
cd infra
docker compose down >nul 2>&1
docker compose up -d
if errorlevel 1 (
    echo ERROR: Failed to start Docker Compose services
    pause
    exit /b 1
)
cd ..
echo ✓ Docker Compose services started
echo    - PostgreSQL: http://localhost:5432
echo    - MailDev: http://localhost:1080
echo.

REM Wait for PostgreSQL to be ready
echo [3/5] Waiting for PostgreSQL to be ready (this may take 10-15 seconds)...
set /a attempts=0
set /a max_attempts=30
:wait_postgres
docker exec smartcampus-postgres pg_isready -U smartcampus -d smartcampus >nul 2>&1
if errorlevel 1 (
    if !attempts! geq !max_attempts! (
        echo ERROR: PostgreSQL did not become ready in time
        echo Try: docker compose logs postgres
        pause
        exit /b 1
    )
    set /a attempts=!attempts!+1
    timeout /t 1 /nobreak
    goto wait_postgres
)
echo ✓ PostgreSQL is ready
echo.

REM Start Backend
echo [4/5] Starting Backend API (Spring Boot)...
start "VenueLink - Backend" cmd /k "cd backend\api && echo Starting Spring Boot Backend... && echo. && %~dp0\..\mvnw.cmd spring-boot:run"
echo ✓ Backend starting in new window (http://localhost:8080)
timeout /t 3
echo.

REM Start Frontend
echo [5/5] Starting Frontend (Vite Dev Server)...
start "VenueLink - Frontend" cmd /k "cd frontend && npm run dev"
echo ✓ Frontend starting in new window (http://localhost:5173)
echo.

echo.
echo ============================================
echo ✓ All services started successfully!
echo ============================================
echo.
echo Access the application:
echo - Frontend:       http://localhost:5173
echo - Backend API:    http://localhost:8080
echo - API Docs:       http://localhost:8080/swagger-ui.html
echo - Email Testing:  http://localhost:1080
echo - Database:       localhost:5432 (smartcampus/smartcampus)
echo.
echo Next steps:
echo 1. Wait for backend to fully start (check: "Started ApiApplication in X seconds")
echo 2. Open http://localhost:5173 in your browser
echo 3. Click "Login with Google" to authenticate
echo 4. Check your Google account authentication settings if needed
echo.
echo Tips:
echo - Frontend, backend, and email test windows are separate
echo - Close any window to stop that service
echo - To stop all services: docker compose down (from infra folder)
echo - View logs: docker compose logs -f (from infra folder)
echo.
echo ============================================
echo.

pause

