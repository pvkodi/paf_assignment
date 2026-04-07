#!/bin/bash

# Google OAuth Implementation - Quick Start Script
# Make sure you have completes the setup steps in OAUTH_SETUP_GUIDE.md first

echo "==================================="
echo "Google OAuth - Starting Both Services"
echo "==================================="
echo ""

# Check if running on Windows
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    echo "Windows detected - using .cmd scripts"
    
    echo "Starting Backend (Spring Boot)..."
    cd backend/api
    start cmd /k "./mvnw.cmd spring-boot:run"
    cd ../..
    
    timeout /t 5
    
    echo "Starting Frontend (Vite Dev Server)..."
    cd frontend
    start cmd /k "npm run dev"
else
    echo "Unix-like system detected"
    
    # Start backend in background
    echo "Starting Backend (Spring Boot)..."
    cd backend/api
    ./mvnw spring-boot:run &
    BACKEND_PID=$!
    cd ../..
    
    # Wait for backend to start
    sleep 5
    
    # Start frontend
    echo "Starting Frontend (Vite Dev Server)..."
    cd frontend
    npm run dev &
    FRONTEND_PID=$!
    
    # Wait for both processes
    wait $BACKEND_PID $FRONTEND_PID
fi

echo ""
echo "==================================="
echo "Services Started!"
echo "==================================="
echo "Frontend: http://localhost:5173"
echo "Backend:  http://localhost:8080"
echo "==================================="
