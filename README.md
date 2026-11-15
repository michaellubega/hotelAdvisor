# Starview Hotel Management Suite — Version 4.0

A shared hotel operations system with a browser dashboard, a customer booking portal, and a desktop launcher. The browser app and customer portal use the same Python server and SQLite database. The project uses Python's standard library only; no pip packages or internet connection are needed to run it.

## Start the system

**Windows — desktop launcher**
1. Install Python 3.10 or newer.
2. Double-click `run-desktop.bat`.
3. The launcher starts the Starview server and opens the staff dashboard in your default browser.

**Windows — server only**
- Double-click `run-web.bat`, then open `http://127.0.0.1:8080`.

**Linux / macOS — desktop launcher**
```bash
chmod +x run-desktop.sh
./run-desktop.sh
```

**Linux / macOS — server only**
```bash
./run-web.sh
```
Then open `http://127.0.0.1:8080`.

The desktop launcher opens the same shared operational application in a browser. The retained Java Swing coursework prototype is still in `src/`; it is not the live data client. The operational browser app is the supported interface for both desktop and other authorised workstations.

## Demonstration accounts

For a fresh database, the demo accounts match the companion Starview Hotel Advisor coursework project:

- Staff: `admin` / `1234` (administrator; change the password at first sign-in)
- Customer: `john@example.com` / `pass123`
- Customer: `mary@example.com` / `mary2024`

The staff account is marked to require a password change on first sign-in. To choose a different initial staff password before the first launch, set `STARVIEW_ADMIN_PASSWORD` in the environment. Existing databases keep their existing credentials; these demo credentials are seeded only where the corresponding accounts do not already exist. Demo credentials are for local demonstration only, not live use.

## Modules included

1. Dashboard and occupancy overview
2. Room inventory, room types, capacity, rates and room status
3. Reservations, month calendar, date-overlap checks and smart room allocation
4. Guest directory and customer account registration
5. Customer booking portal at `http://127.0.0.1:8080/portal`
6. Front desk arrivals, check-in and check-out
7. Billing, payment recording, balances, printable invoices and receipts
8. Housekeeping tasks and room-readiness updates
9. Restaurant orders and room-charge posting
10. Inventory, minimum-stock alerts and supplier records
11. Employee directory
12. Reports and print view
13. Staff accounts, role-scoped access and audit trail
14. Demonstration rate advisor (illustrative rule-based pricing guidance)
15. Database backup, automatic daily backups and JSON export

## Customer booking flow

Customers open `/portal`, create an account with their name, email, phone and a password of at least 12 characters, then sign in and submit a room/date request. The request appears in the staff **Reservations** module as **Pending approval**. Staff should review it, confirm availability and change the status to **Confirmed** before treating it as a confirmed reservation. Pending requests are checked against overlapping dates to reduce double-booking.

## Staff roles

- **Administrator / Manager:** full operations access
- **Receptionist:** rooms, reservations, guests, front desk, billing and reports
- **Accounts:** billing and reports
- **Housekeeping:** rooms and housekeeping
- **Restaurant:** restaurant, guests and room lookup
- **Maintenance:** rooms and housekeeping

Create staff accounts in **Settings → Staff login accounts**. Staff passwords are hashed with PBKDF2-HMAC-SHA256. Sessions are stored server-side, expire after 12 hours and use HttpOnly/SameSite cookies. Repeated staff login failures are throttled.

## Data and backups

The shared database is stored in `data/starview.sqlite3`. Both the staff dashboard and customer portal read and write to this same database. The server creates daily SQLite backups around 02:00 server-local time and keeps the latest 14 automatic backups in `data/backups/`. Administrators can also download a manual SQLite backup from Settings. Keep a separate off-device copy of the entire `data/` folder.

The first launch seeds sample demonstration records. Use **Clear sample records** on the Administrator dashboard before entering real hotel data.

To use a custom data directory, set `STARVIEW_DATA_DIR` before starting the server. To change the port, set `STARVIEW_PORT` (default `8080`). The server binds to `127.0.0.1` by default. For a trusted hotel LAN only, set `STARVIEW_HOST=0.0.0.0` and configure the host firewall to allow only authorised devices. Do not expose this development server directly to the public internet.

## Important limitations and deployment guidance

- The Rate Advisor is a demonstration calculation using the room's base rate, expected occupancy, a selected demand level and a weekend adjustment. It is not connected to live competitor rates, demand feeds or a validated revenue-management model. Management must approve actual prices.
- The customer portal accepts booking requests; it does not take online deposits or process payment cards. Staff confirm the booking in the Reservations module.
- The application is intended for local operation or a trusted hotel LAN pilot. It is not a hardened public-internet deployment. Before live use with real guest information, deploy behind HTTPS, restrict network access, configure encrypted off-device backups, review local privacy and data-retention requirements, and arrange a security review.
- Do not store full payment-card numbers or CVV values. Process card payments through a compliant payment provider and record only the transaction reference and payment status.
- The server uses one SQLite database and one application process; it is suitable for a small property/pilot. Larger deployments should migrate to PostgreSQL/MySQL and a production application server.

## Run the built-in smoke test

With Python 3.10 or newer, run `python tests/smoke_test.py`. It starts an isolated temporary server and checks staff authentication, shared SQLite state, customer registration, booking requests, overlap prevention and database backup.

## Project layout

- `server.py` — shared HTTP server, authentication, API and SQLite persistence
- `resources/web/index.html` — staff operations dashboard
- `resources/web/customer.html` — guest registration and booking portal
- `resources/web/starview-hotel.jpg` — Starview hotel image
- `src/` — retained Java Swing coursework prototype and source
- `run-desktop.*`, `run-web.*` — launchers
- `data/` — created automatically for the live database and backups


## Supplementary coursework documentation

The `documentation/` folder contains the analysis, system design and pricing, testing and risks, installation/run, user guide and group-record documents supplied with the Starview Hotel Advisor project. These documents are included as supplied.
