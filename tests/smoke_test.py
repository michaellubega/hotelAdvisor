#!/usr/bin/env python3
"""End-to-end smoke test for the Starview Hotel Management Suite."""
from __future__ import annotations
import http.cookiejar
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from datetime import date, timedelta

ROOT = Path(__file__).resolve().parents[1]

class Client:
    def __init__(self, base: str):
        self.base = base
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def call(self, path: str, method: str = 'GET', data=None, raw: bool = False):
        body = None if data is None else json.dumps(data).encode()
        req = urllib.request.Request(self.base + path, data=body,
                                     headers={'Content-Type': 'application/json'}, method=method)
        try:
            with self.opener.open(req, timeout=8) as response:
                payload = response.read()
                return response.status, response.headers, payload if raw else json.loads(payload or b'{}')
        except urllib.error.HTTPError as error:
            payload = error.read()
            return error.code, error.headers, payload if raw else json.loads(payload or b'{}')


def main() -> int:
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        port = sock.getsockname()[1]
    with tempfile.TemporaryDirectory(prefix='starview-smoke-') as data_dir:
        env = os.environ.copy()
        env.update(STARVIEW_DATA_DIR=data_dir, STARVIEW_PORT=str(port), STARVIEW_HOST='127.0.0.1')
        proc = subprocess.Popen([sys.executable, 'server.py'], cwd=ROOT, env=env,
                                stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
        base = f'http://127.0.0.1:{port}'
        try:
            for _ in range(60):
                try:
                    urllib.request.urlopen(base + '/api/public/rooms', timeout=1).close()
                    break
                except Exception:
                    if proc.poll() is not None:
                        raise RuntimeError('Starview server exited during startup')
                    time.sleep(.25)
            else:
                raise RuntimeError('Starview server did not become ready')

            staff, customer = Client(base), Client(base)
            status, _, result = staff.call('/api/auth/login', 'POST',
                                           {'username': 'admin', 'password': '1234'})
            assert status == 200, result
            status, _, state = staff.call('/api/state')
            assert status == 200 and all(key in state for key in
                ('rooms', 'guests', 'reservations', 'housekeeping', 'restaurant', 'inventory', 'employees', 'payments'))

            status, _, result = customer.call('/api/customer/register', 'POST', {
                'fullName': 'Smoke Test Guest', 'email': 'smoke@example.test',
                'phone': '+256700000000', 'password': 'SmokeTestPassword!2026'})
            assert status == 201, result
            arrival = date.today() + timedelta(days=30)
            departure = arrival + timedelta(days=2)
            request = {'room': '101', 'checkin': arrival.isoformat(),
                       'checkout': departure.isoformat(), 'adults': 1}
            status, _, booking = customer.call('/api/customer/booking', 'POST', request)
            assert status == 201 and booking['total'] == 170000, booking
            status, _, history = customer.call('/api/customer/bookings')
            assert status == 200 and len(history) == 1 and history[0]['status'] == 'Pending approval'
            status, _, duplicate = customer.call('/api/customer/booking', 'POST', request)
            assert status == 409, duplicate

            status, _, staff_state = staff.call('/api/state')
            assert any(g.get('email') == 'smoke@example.test' for g in staff_state['guests'])
            assert any(r.get('id') == booking['bookingId'] for r in staff_state['reservations'])
            status, headers, backup = staff.call('/api/backup', raw=True)
            assert status == 200 and headers.get('Content-Type') == 'application/vnd.sqlite3'
            assert backup.startswith(b'SQLite format 3\x00')
            print('PASS: staff authentication, shared SQLite state, customer registration, booking submission, booking history, overlap rejection and database backup.')
            return 0
        finally:
            proc.terminate()
            try:
                proc.wait(timeout=5)
            except subprocess.TimeoutExpired:
                proc.kill()
                proc.wait(timeout=5)

if __name__ == '__main__':
    raise SystemExit(main())
