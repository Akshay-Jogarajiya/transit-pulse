#!/usr/bin/env bash
# TransitPulse startup script
# Usage: CLAUDE_API_KEY=sk-ant-... ./start.sh

set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "=== TransitPulse ==="
echo ""

# Check Java
if ! command -v java &>/dev/null; then echo "ERROR: Java 21 not found." && exit 1; fi
JAVA_VER=$(java -version 2>&1 | head -1 | grep -oP '(?<=version ")[^"]+')
echo "Java: $JAVA_VER"

# Check Node
if ! command -v node &>/dev/null; then echo "ERROR: Node.js not found." && exit 1; fi
echo "Node: $(node --version)"

# Check API key
if [ -z "$CLAUDE_API_KEY" ]; then
  echo "WARN: CLAUDE_API_KEY not set — AI narrative will use fallback template."
fi

echo ""
echo "Starting backend on port 8080..."
cd "$ROOT/backend"
mvn spring-boot:run -q &
BACKEND_PID=$!

echo "Waiting for backend to start..."
for i in $(seq 1 30); do
  if curl -s http://localhost:8080/api/analysis/status &>/dev/null; then break; fi
  sleep 1
done
echo "Backend ready."

echo ""
echo "Starting frontend on port 3000..."
cd "$ROOT/frontend"
npm run dev &
FRONTEND_PID=$!

echo ""
echo "============================================"
echo " TransitPulse is running"
echo " Frontend: http://localhost:3000"
echo " Backend:  http://localhost:8080"
echo ""
echo " Sample data: backend/src/main/resources/sample-data/"
echo ""
echo " Press Ctrl+C to stop both servers."
echo "============================================"

# Trap to kill both on exit
trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT TERM

wait
