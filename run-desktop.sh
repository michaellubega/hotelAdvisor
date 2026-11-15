#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
python3 - <<'PY'
import sys
if sys.version_info < (3,10):
    raise SystemExit('Python 3.10 or newer is required.')
PY
URL="http://127.0.0.1:${STARVIEW_PORT:-8080}"
if ! python3 - "$URL" <<'PY'
import sys, urllib.request
try:
    urllib.request.urlopen(sys.argv[1], timeout=1)
except Exception:
    raise SystemExit(1)
PY
then
  python3 server.py > starview-server.log 2>&1 &
  SERVER_PID=$!
  trap 'kill "$SERVER_PID" 2>/dev/null || true' EXIT INT TERM
  READY=0
  for _ in $(seq 1 30); do
    if python3 - "$URL" <<'PY'
import sys, urllib.request
try: urllib.request.urlopen(sys.argv[1], timeout=1)
except Exception: raise SystemExit(1)
PY
    then READY=1; break; fi
    sleep 1
  done
  if [ "$READY" -ne 1 ]; then
    echo "Starview failed to start. Check starview-server.log."
    cat starview-server.log
    exit 1
  fi
fi
if command -v xdg-open >/dev/null 2>&1; then xdg-open "$URL" >/dev/null 2>&1 || true
elif command -v open >/dev/null 2>&1; then open "$URL" || true
else echo "Open $URL in your browser."; fi
if [ -n "${SERVER_PID:-}" ]; then wait "$SERVER_PID"; fi
