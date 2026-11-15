#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
python3 - <<'PY'
import sys
if sys.version_info < (3,10):
    raise SystemExit('Python 3.10 or newer is required.')
PY
exec python3 server.py
