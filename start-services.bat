@echo off
REM Google OAuth Implementation - Quick Start Script for Windows
REM Make sure you have completed the setup steps in OAUTH_SETUP_GUIDE.md first

echo ===================================
echo Google OAuth - Starting Both Services
echo ===================================
echo.

echo Starting Backend (Spring Boot)...
cd backend\api
start "Backend" cmd /k "mvnw.cmd spring-boot:run"
cd ..\..

echo Waiting for backend to start...
timeout /t 5

echo Starting Frontend (Vite Dev Server)...
cd frontend
start "Frontend" cmd /k "npm run dev"

echo.
echo ===================================
echo Services Starting!
echo ===================================
echo Frontend: http://localhost:5173
echo Backend:  http://localhost:8080
echo ===================================
echo.
echo You can now log in with your Google account!
