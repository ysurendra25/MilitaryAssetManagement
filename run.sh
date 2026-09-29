#!/usr/bin/env bash
# Build and run the Military Asset Management System (MAMS).
#   ./run.sh          build + start on http://localhost:8080
# Requires: Java 21+, Maven 3.9+ (frontend is pre-built into backend static resources;
# to rebuild the frontend you also need Node 18+ - see README).
set -e
cd "$(dirname "$0")"

echo "==> Building backend (Spring Boot)..."
(cd backend && mvn -q -DskipTests package)

echo "==> Starting on http://localhost:8080"
(cd backend && java -jar target/mams-backend-1.0.0.jar)
