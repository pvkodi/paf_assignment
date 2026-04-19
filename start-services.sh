#!/bin/bash

# ============================================
# VenueLink Operations Hub - Startup Script
# ============================================
# This script starts all required services:
# - PostgreSQL (Docker)
# - MailDev (Docker)
# - Backend API (Spring Boot)
# - Frontend Dev Server (Vite)
#
# Prerequisites:
# - Docker and Docker Compose installed
# - Java 21+ available
# - Node.js 20+ with npm installed
#
# Usage: 
#   chmod +x start-services.sh
#   ./start-services.sh
# ============================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}✓${NC} $1"
}

log_error() {
    echo -e "${RED}ERROR:${NC} $1" >&2
}

log_warning() {
    echo -e "${YELLOW}WARNING:${NC} $1"
}

# Clear screen
clear

echo "============================================"
echo "VenueLink Hub - Full Stack Startup"
echo "============================================"
echo ""

# Check prerequisites
log_info "Checking prerequisites..."
echo ""

# Check Docker
if ! command -v docker &> /dev/null; then
    log_error "Docker is not installed"
    echo "Visit https://docs.docker.com/install/"
    exit 1
fi
log_success "Docker found: $(docker --version)"

# Check Docker Compose
if ! docker compose version &> /dev/null; then
    log_error "Docker Compose not found"
    exit 1
fi
log_success "Docker Compose found: $(docker compose version --short)"

# Check Java
if ! command -v java &> /dev/null; then
    log_warning "Java not found in PATH - Backend may not start"
else
    log_success "Java found: $(java -version 2>&1 | head -1)"
fi

# Check Node.js
if ! command -v node &> /dev/null; then
    log_error "Node.js is not installed"
    echo "Visit https://nodejs.org/"
    exit 1
fi
log_success "Node.js found: $(node --version)"
log_success "npm found: $(npm --version)"
echo ""

# Navigate to project root
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

# Start infrastructure (Docker)
log_info "Starting infrastructure (PostgreSQL, MailDev)..."
cd infra
docker compose down 2>/dev/null || true
docker compose up -d

if [ $? -ne 0 ]; then
    log_error "Failed to start Docker Compose services"
    exit 1
fi

log_success "Docker Compose services started"
echo "   - PostgreSQL: postgresql://localhost:5432/smartcampus"
echo "   - MailDev: http://localhost:1080"
echo ""

# Wait for PostgreSQL to be ready
log_info "Waiting for PostgreSQL to be ready (this may take 10-15 seconds)..."
max_attempts=30
attempt=0

while [ $attempt -lt $max_attempts ]; do
    if docker exec smartcampus-postgres pg_isready -U smartcampus -d smartcampus >/dev/null 2>&1; then
        log_success "PostgreSQL is ready"
        break
    fi
    
    attempt=$((attempt + 1))
    if [ $attempt -eq $max_attempts ]; then
        log_error "PostgreSQL did not become ready in time"
        log_info "Try: docker compose logs postgres"
        exit 1
    fi
    
    sleep 1
done
echo ""

cd ..

# Detect OS
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" || "$OSTYPE" == "cygwin" ]]; then
    log_info "Windows environment detected - using .cmd scripts"
    
    # Start Backend
    log_info "Starting Backend API (Spring Boot)..."
    start cmd /k "cd backend/api && mvnw.cmd spring-boot:run"
    log_success "Backend starting in new window (http://localhost:8080)"
    sleep 3
    
    # Start Frontend
    log_info "Starting Frontend (Vite Dev Server)..."
    start cmd /k "cd frontend && npm run dev"
    log_success "Frontend starting in new window (http://localhost:5173)"
else
    # Unix-like system (Linux/macOS)
    
    # Start Backend in background
    log_info "Starting Backend API (Spring Boot)..."
    cd backend/api
    ./mvnw spring-boot:run &
    BACKEND_PID=$!
    log_success "Backend started (PID: $BACKEND_PID)"
    echo "   URL: http://localhost:8080"
    cd ../..
    
    # Wait for backend to start
    sleep 5
    
    # Start Frontend in background
    log_info "Starting Frontend (Vite Dev Server)..."
    cd frontend
    npm run dev &
    FRONTEND_PID=$!
    log_success "Frontend started (PID: $FRONTEND_PID)"
    echo "   URL: http://localhost:5173"
    cd ..
fi

echo ""
echo "============================================"
echo "✓ All services started successfully!"
echo "============================================"
echo ""
echo "Access the application:"
echo "- Frontend:       http://localhost:5173"
echo "- Backend API:    http://localhost:8080"
echo "- API Docs:       http://localhost:8080/swagger-ui.html"
echo "- Email Testing:  http://localhost:1080"
echo "- Database:       postgresql://localhost:5432/smartcampus"
echo ""
echo "Default credentials (development only):"
echo "- Database: smartcampus / smartcampus"
echo ""
echo "Next steps:"
echo "1. Wait for backend to fully start (check: 'Started ApiApplication')"
echo "2. Open http://localhost:5173 in your browser"
echo "3. Click 'Login with Google' to authenticate"
echo ""
echo "Useful commands:"
echo "- View logs:      docker compose logs -f (from ./infra)"
echo "- Stop services:  docker compose down (from ./infra)"
echo "- Check health:   docker compose ps (from ./infra)"
echo ""
echo "============================================"
echo ""

# Wait for processes (Unix only)
if [[ ! ("$OSTYPE" == "msys" || "$OSTYPE" == "win32" || "$OSTYPE" == "cygwin") ]]; then
    wait $BACKEND_PID $FRONTEND_PID 2>/dev/null || true
fi

echo "==================================="
echo "Frontend: http://localhost:5173"
echo "Backend:  http://localhost:8080"
echo "==================================="
