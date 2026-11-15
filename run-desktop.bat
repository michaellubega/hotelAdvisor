@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
if not defined STARVIEW_PORT set "STARVIEW_PORT=8080"
where py >nul 2>nul
if errorlevel 1 (
  where python >nul 2>nul
  if errorlevel 1 (
    echo Python 3.10 or newer is required. Install it from https://www.python.org/downloads/
    pause
    exit /b 1
  )
  set "PYTHON=python"
) else (
  set "PYTHON=py -3"
)
%PYTHON% --version
if errorlevel 1 (
  echo Python could not be started.
  pause
  exit /b 1
)
%PYTHON% -c "import sys; raise SystemExit(0 if sys.version_info >= (3,10) else 1)"
if errorlevel 1 (
  echo Python 3.10 or newer is required.
  pause
  exit /b 1
)
powershell -NoProfile -Command "try { (Invoke-WebRequest -UseBasicParsing http://127.0.0.1:%STARVIEW_PORT% -TimeoutSec 2).StatusCode } catch { exit 1 }" >nul 2>nul
if errorlevel 1 (
  start "Starview Hotel Server" /min cmd /c "%PYTHON% server.py > starview-server.log 2>&1"
  set "READY=0"
  for /L %%i in (1,1,25) do (
    powershell -NoProfile -Command "try { (Invoke-WebRequest -UseBasicParsing http://127.0.0.1:%STARVIEW_PORT% -TimeoutSec 1).StatusCode } catch { exit 1 }" >nul 2>nul
    if not errorlevel 1 set "READY=1"
    if "!READY!"=="1" goto :open
    timeout /t 1 /nobreak >nul
  )
  echo Starview did not start. Check starview-server.log for details.
  type starview-server.log
  pause
  exit /b 1
)
:open
start "" http://127.0.0.1:%STARVIEW_PORT%
exit /b 0
