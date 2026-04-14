#!/usr/bin/env bash
# =============================================================================
# start-dev-server.sh - Development Server Launcher
# =============================================================================
#
# Starts the kotlin-htmx development server with auto-recompilation.
# Ktor auto-reload + LiveReload plugin handle browser refresh automatically.
#
# BEHAVIOR:
#   - Kills ALL existing kotlin-htmx server and watch instances before starting
#   - Uses a random port (10000-20000) by default to avoid conflicts
#   - Waits until the server is ready
#   - Starts continuous recompilation (gradlew -t compileKotlin) in background
#   - Opens the browser to the server URL
#   - Blocks until Ctrl+C, then kills all child processes (server + watch)
#   - Writes a status file to dev-logs/.dev-server.json for stop/status commands
#
# USAGE:
#   ./start-dev-server.sh                # Start (blocking, recommended)
#   ./start-dev-server.sh stop           # Stop running server and watch
#   ./start-dev-server.sh status         # Show running server info
#   PORT=9000 ./start-dev-server.sh      # Specific port (default: random 10000-20000)
#   NO_BROWSER=1 ./start-dev-server.sh   # Don't open browser
#   NO_WATCH=1 ./start-dev-server.sh     # Don't start auto-recompilation
#   BACKGROUND=1 ./start-dev-server.sh   # Return immediately (non-blocking)
#
# NOTE:
#   Do NOT use "./gradlew run -t" — the -t flag restarts the entire server on
#   every change instead of hot-reloading. The correct approach is two separate
#   processes: "gradlew run" (server) + "gradlew -t compileKotlin" (recompile).
#   This script handles both automatically.
#
# INTELLIJ USERS:
#   If running the server from IntelliJ, start auto-recompilation separately:
#     ./gradlew -t compileKotlin -x test
#
# =============================================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'
BOLD='\033[1m'

MAX_WAIT_SECONDS=90
LOG_DIR="dev-logs"
STATUS_FILE="$LOG_DIR/.dev-server.json"
MAIN_CLASS="no.mikill.kotlin_htmx.ApplicationKt"

SERVER_PID=""
WATCH_PID=""

is_alive() { kill -0 "$1" 2>/dev/null; }

read_status_file() {
    [ -f "$STATUS_FILE" ] || return 1
    STATUS_SERVER_PID=$(grep -o '"server_pid": *[0-9]*' "$STATUS_FILE" | grep -o '[0-9]*' || true)
    STATUS_WATCH_PID=$(grep -o '"watch_pid": *[0-9]*' "$STATUS_FILE" | grep -o '[0-9]*' || true)
    STATUS_PORT=$(grep -o '"port": *[0-9]*' "$STATUS_FILE" | grep -o '[0-9]*' || true)
    STATUS_LOG=$(grep -o '"log_file": *"[^"]*"' "$STATUS_FILE" | sed 's/"log_file": *"//;s/"$//' || true)
    STATUS_WATCH_LOG=$(grep -o '"watch_log": *"[^"]*"' "$STATUS_FILE" | sed 's/"watch_log": *"//;s/"$//' || true)
}

write_status_file() {
    mkdir -p "$LOG_DIR"
    cat > "$STATUS_FILE" <<EOF
{
  "server_pid": $SERVER_PID,
  "watch_pid": ${WATCH_PID:-0},
  "port": $PORT,
  "log_file": "$LOG_FILE",
  "watch_log": "${WATCH_LOG:-}",
  "started_at": "$(date -Iseconds)"
}
EOF
}

remove_status_file() { rm -f "$STATUS_FILE"; }

cmd_stop() {
    if ! read_status_file; then
        echo -e "${YELLOW}No status file found. Nothing to stop.${NC}"
        exit 0
    fi
    STOPPED=0
    if [ -n "$STATUS_WATCH_PID" ] && [ "$STATUS_WATCH_PID" != "0" ] && is_alive "$STATUS_WATCH_PID"; then
        kill "$STATUS_WATCH_PID" 2>/dev/null || true
        echo -e "${GREEN}✓${NC} Stopped auto-recompile (PID $STATUS_WATCH_PID)"
        STOPPED=$((STOPPED + 1))
    fi
    if [ -n "$STATUS_SERVER_PID" ] && is_alive "$STATUS_SERVER_PID"; then
        kill "$STATUS_SERVER_PID" 2>/dev/null || true
        echo -e "${GREEN}✓${NC} Stopped server (PID $STATUS_SERVER_PID)"
        STOPPED=$((STOPPED + 1))
    fi
    remove_status_file
    if [ $STOPPED -eq 0 ]; then
        echo -e "${YELLOW}Processes from status file were already dead. Cleaned up status file.${NC}"
    else
        echo -e "${GREEN}Stopped $STOPPED process(es).${NC}"
    fi
    exit 0
}

cmd_status() {
    if ! read_status_file; then
        echo -e "${YELLOW}No dev server status file found.${NC}"
        exit 0
    fi
    echo ""
    echo -e "${BOLD}${CYAN}Dev Server Status${NC}"
    echo ""
    if [ -n "$STATUS_SERVER_PID" ] && is_alive "$STATUS_SERVER_PID"; then
        echo -e "  ${BOLD}Server:${NC}  ${GREEN}running${NC} (PID $STATUS_SERVER_PID)"
    else
        echo -e "  ${BOLD}Server:${NC}  ${RED}dead${NC} (was PID $STATUS_SERVER_PID)"
    fi
    echo -e "  ${BOLD}URL:${NC}     http://127.0.0.1:$STATUS_PORT"
    echo -e "  ${BOLD}Log:${NC}     $STATUS_LOG"
    if [ -n "$STATUS_WATCH_PID" ] && [ "$STATUS_WATCH_PID" != "0" ]; then
        if is_alive "$STATUS_WATCH_PID"; then
            echo -e "  ${BOLD}Watch:${NC}   ${GREEN}running${NC} (PID $STATUS_WATCH_PID)"
        else
            echo -e "  ${BOLD}Watch:${NC}   ${RED}dead${NC} (was PID $STATUS_WATCH_PID)"
        fi
        [ -n "$STATUS_WATCH_LOG" ] && echo -e "  ${BOLD}Watch log:${NC} $STATUS_WATCH_LOG"
    else
        echo -e "  ${BOLD}Watch:${NC}   ${YELLOW}not started${NC}"
    fi
    echo ""
    exit 0
}

case "${1:-}" in
    stop)   cmd_stop ;;
    status) cmd_status ;;
    "")     ;; # default: start
    *)
        echo "Usage: $0 [stop|status]" >&2
        exit 1
        ;;
esac

CLEANED_UP=false
cleanup() {
    if [ "$CLEANED_UP" = true ]; then return; fi
    CLEANED_UP=true
    echo ""
    echo -e "${YELLOW}Shutting down...${NC}"
    [ -n "$WATCH_PID" ] && is_alive "$WATCH_PID" && kill "$WATCH_PID" 2>/dev/null && echo -e "  ${GREEN}✓${NC} Stopped auto-recompile (PID $WATCH_PID)"
    [ -n "$SERVER_PID" ] && is_alive "$SERVER_PID" && kill "$SERVER_PID" 2>/dev/null && echo -e "  ${GREEN}✓${NC} Stopped server (PID $SERVER_PID)"
    remove_status_file
    echo -e "${GREEN}Done.${NC}"
}

echo ""
echo -e "${BOLD}${CYAN}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${CYAN}║              kotlin-htmx Development Server                   ║${NC}"
echo -e "${BOLD}${CYAN}╚═══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Kill existing instances
echo -e "${YELLOW}[1/3]${NC} Cleaning up existing server instances..."

if read_status_file; then
    [ -n "$STATUS_WATCH_PID" ] && [ "$STATUS_WATCH_PID" != "0" ] && is_alive "$STATUS_WATCH_PID" && kill "$STATUS_WATCH_PID" 2>/dev/null || true
    [ -n "$STATUS_SERVER_PID" ] && is_alive "$STATUS_SERVER_PID" && kill "$STATUS_SERVER_PID" 2>/dev/null || true
    remove_status_file
fi

JAVA_PIDS=$(pgrep -f "$MAIN_CLASS" 2>/dev/null || true)
PREVIOUS_PORT=""

if [ -z "${PORT:-}" ] && [ -n "$JAVA_PIDS" ]; then
    PID_COUNT=$(echo "$JAVA_PIDS" | wc -w | tr -d ' ')
    if [ "$PID_COUNT" -eq 1 ]; then
        PREVIOUS_PORT=$(lsof -anP -iTCP -sTCP:LISTEN -p "$JAVA_PIDS" 2>/dev/null | awk 'NR>1 {split($9,a,":"); print a[length(a)]}' | head -1)
    fi
fi

PORT=${PORT:-${PREVIOUS_PORT:-$((RANDOM % 10000 + 10000))}}
URL="http://127.0.0.1:$PORT"

KILLED_COUNT=0
GRADLE_PIDS=$(pgrep -f "gradle.*run" 2>/dev/null | xargs -I {} sh -c 'ps -p {} -o args= 2>/dev/null | grep -q kotlin.htmx && echo {}' || true)
if [ -n "$GRADLE_PIDS" ]; then
    echo "$GRADLE_PIDS" | xargs -r kill 2>/dev/null || true
    KILLED_COUNT=$((KILLED_COUNT + $(echo "$GRADLE_PIDS" | wc -w)))
fi

EXISTING_WATCH_PIDS=$(pgrep -f "gradle.*compileKotlin" 2>/dev/null | xargs -I {} sh -c 'ps -p {} -o args= 2>/dev/null | grep -q kotlin.htmx && echo {}' || true)
if [ -n "$EXISTING_WATCH_PIDS" ]; then
    echo "$EXISTING_WATCH_PIDS" | xargs -r kill 2>/dev/null || true
    KILLED_COUNT=$((KILLED_COUNT + $(echo "$EXISTING_WATCH_PIDS" | wc -w)))
fi

if [ -n "$JAVA_PIDS" ]; then
    echo "$JAVA_PIDS" | xargs -r kill 2>/dev/null || true
    KILLED_COUNT=$((KILLED_COUNT + $(echo "$JAVA_PIDS" | wc -w)))
fi

PORT_PIDS=$(lsof -ti:"$PORT" 2>/dev/null || true)
[ -n "$PORT_PIDS" ] && echo "$PORT_PIDS" | xargs -r kill 2>/dev/null || true

if [ $KILLED_COUNT -gt 0 ]; then
    [ -n "$PREVIOUS_PORT" ] && echo -e "      ${GREEN}✓${NC} Killed $KILLED_COUNT existing process(es), reusing port $PREVIOUS_PORT" \
                             || echo -e "      ${GREEN}✓${NC} Killed $KILLED_COUNT existing process(es)"
    sleep 2
else
    echo -e "      ${GREEN}✓${NC} No existing instances found"
fi

# Start server
echo -e "${YELLOW}[2/3]${NC} Starting server on port ${BOLD}$PORT${NC}..."

export SERVER_PORT=$PORT

mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/server-$PORT.log"
./gradlew run > "$LOG_FILE" 2>&1 &
SERVER_PID=$!

echo -e "      Process ID: $SERVER_PID"
echo -e "      Log file: $LOG_FILE"

trap cleanup EXIT INT TERM
write_status_file

# Wait for server
echo -e "${YELLOW}[3/3]${NC} Waiting for server to be ready (max ${MAX_WAIT_SECONDS}s)..."

READY=false
for i in $(seq 1 $MAX_WAIT_SECONDS); do
    if ! is_alive $SERVER_PID; then
        echo ""
        echo -e "${RED}ERROR: Server process died unexpectedly${NC}"
        echo "Last 20 lines of log:"
        tail -20 "$LOG_FILE" 2>/dev/null || echo "(no log available)"
        exit 1
    fi

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$URL" 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "302" ]; then
        READY=true
        break
    fi

    [ $((i % 10)) -eq 0 ] && echo -e "      ... ${i}s"
    sleep 1
done

if [ "$READY" = true ]; then
    echo ""
    echo -e "${GREEN}══════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  ${BOLD}SERVER READY${NC}"
    echo -e "${GREEN}══════════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "  ${BOLD}URL:${NC}  $URL"
    echo -e "  ${BOLD}Port:${NC} $PORT"
    echo -e "  ${BOLD}PID:${NC}  $SERVER_PID"
    echo -e "  ${BOLD}Log:${NC}  $LOG_FILE"
    echo ""

    WATCH_LOG=""
    if [ -z "${NO_WATCH:-}" ]; then
        WATCH_LOG="$LOG_DIR/watch-$PORT.log"
        ./gradlew -t compileKotlin -x test > "$WATCH_LOG" 2>&1 &
        WATCH_PID=$!
        echo -e "  ${GREEN}Auto-recompile:${NC} ${GREEN}active${NC} (PID $WATCH_PID, log: $WATCH_LOG)"
        echo -e "  ${CYAN}Workflow:${NC} Edit Kotlin → auto-recompile → auto-reload → browser morphs"
        echo ""
    else
        echo -e "  ${YELLOW}Auto-recompile:${NC} ${YELLOW}disabled${NC} (NO_WATCH=1)"
        echo -e "  ${CYAN}Start manually:${NC} ./gradlew -t compileKotlin -x test"
        echo ""
    fi

    write_status_file

    echo -e "${GREEN}══════════════════════════════════════════════════════════════════${NC}"
    echo ""

    if [ -z "${NO_BROWSER:-}" ]; then
        if command -v xdg-open &> /dev/null; then
            xdg-open "$URL" 2>/dev/null &
        elif command -v open &> /dev/null; then
            open "$URL" 2>/dev/null &
        fi
    fi

    if [ -z "${BACKGROUND:-}" ]; then
        echo -e "${CYAN}Press Ctrl+C to stop server and watch processes.${NC}"
        echo ""
        wait $SERVER_PID 2>/dev/null || true
    else
        # In background mode, remove the cleanup trap so server/watch survive script exit
        trap - EXIT INT TERM
    fi
    exit 0
else
    echo ""
    echo -e "${RED}ERROR: Server failed to start within ${MAX_WAIT_SECONDS} seconds${NC}"
    echo "Last 30 lines of log:"
    tail -30 "$LOG_FILE" 2>/dev/null || echo "(no log available)"
    kill $SERVER_PID 2>/dev/null || true
    exit 1
fi
