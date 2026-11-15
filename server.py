#!/usr/bin/env python3
"""Starview Hotel Management Suite - local/LAN application server.
Python 3.10+, standard library only. SQLite persistence, hashed passwords and role-scoped data access.
"""
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from pathlib import Path
from urllib.parse import urlparse
import json, sqlite3, secrets, hashlib, hmac, os, mimetypes, time, shutil, threading

ROOT = Path(__file__).resolve().parent
WEB = ROOT / 'resources' / 'web'
DATA = Path(os.environ.get('STARVIEW_DATA_DIR', str(ROOT / 'data'))).resolve()
DATA.mkdir(parents=True, exist_ok=True)
DB = DATA / 'starview.sqlite3'
PORTAL = WEB / 'customer.html'
LOCK = threading.RLock()
PBKDF2_ROUNDS = 310_000

ROLE_DATA = {
 'Administrator': {'rooms','guests','reservations','housekeeping','restaurant','inventory','employees','payments'},
 'Manager': {'rooms','guests','reservations','housekeeping','restaurant','inventory','employees','payments'},
 'Receptionist': {'rooms','guests','reservations','payments','housekeeping'},
 'Accounts': {'rooms','guests','reservations','payments'},
 'Housekeeping': {'rooms','housekeeping'},
 'Restaurant': {'restaurant','guests','rooms','reservations'},
 'Maintenance': {'rooms','housekeeping'},
}
ROLE_PAGES = {
 'Administrator': '*', 'Manager': '*',
 'Receptionist': 'dashboard,rooms,reservations,guests,frontdesk,billing,reports',
 'Accounts': 'dashboard,billing,reports',
 'Housekeeping': 'dashboard,rooms,housekeeping',
 'Restaurant': 'dashboard,restaurant,guests',
 'Maintenance': 'dashboard,rooms,housekeeping',
}

def connect():
    c=sqlite3.connect(DB, timeout=20)
    c.row_factory=sqlite3.Row
    c.execute('PRAGMA journal_mode=WAL')
    c.execute('PRAGMA foreign_keys=ON')
    return c

def hash_password(password, salt=None):
    salt = salt or secrets.token_bytes(16)
    digest=hashlib.pbkdf2_hmac('sha256', password.encode(), salt, PBKDF2_ROUNDS)
    return salt.hex()+':'+digest.hex()

def verify_password(password, encoded):
    try:
        salt_hex, digest_hex=encoded.split(':',1)
        check=hashlib.pbkdf2_hmac('sha256', password.encode(), bytes.fromhex(salt_hex), PBKDF2_ROUNDS).hex()
        return hmac.compare_digest(check,digest_hex)
    except Exception: return False

def init_db():
    with connect() as c:
        c.executescript('''
        CREATE TABLE IF NOT EXISTS users(id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE COLLATE NOCASE, full_name TEXT NOT NULL, role TEXT NOT NULL, password_hash TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1, must_change INTEGER NOT NULL DEFAULT 0, created_at TEXT DEFAULT CURRENT_TIMESTAMP);
        CREATE TABLE IF NOT EXISTS sessions(token_hash TEXT PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, expires_at INTEGER NOT NULL);
        CREATE TABLE IF NOT EXISTS hotel_state(id INTEGER PRIMARY KEY CHECK(id=1), payload TEXT NOT NULL, updated_at TEXT DEFAULT CURRENT_TIMESTAMP);
        CREATE TABLE IF NOT EXISTS audit(id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, username TEXT, action TEXT NOT NULL, detail TEXT, created_at TEXT DEFAULT CURRENT_TIMESTAMP);
        CREATE TABLE IF NOT EXISTS login_failures(ip TEXT PRIMARY KEY, count INTEGER NOT NULL, since_ts INTEGER NOT NULL);
        CREATE TABLE IF NOT EXISTS customers(id INTEGER PRIMARY KEY AUTOINCREMENT, full_name TEXT NOT NULL, email TEXT NOT NULL UNIQUE COLLATE NOCASE, phone TEXT NOT NULL, password_hash TEXT NOT NULL, created_at TEXT DEFAULT CURRENT_TIMESTAMP);
        CREATE TABLE IF NOT EXISTS customer_sessions(token_hash TEXT PRIMARY KEY, customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE, expires_at INTEGER NOT NULL);
        CREATE TABLE IF NOT EXISTS customer_login_failures(ip TEXT PRIMARY KEY, count INTEGER NOT NULL, since_ts INTEGER NOT NULL);
        ''')
        if not c.execute('SELECT 1 FROM users LIMIT 1').fetchone():
            # Match the coursework demo staff account; require a password change at first sign-in.
            c.execute('INSERT INTO users(username,full_name,role,password_hash,must_change) VALUES(?,?,?,?,1)',('admin','System Admin','Administrator',hash_password(os.environ.get('STARVIEW_ADMIN_PASSWORD','1234'))))
        # Seed the same two guest demo accounts used by the Java coursework prototype.
        demo_customers = (
            ('John Doe', 'john@example.com', '+256 700 222 222', 'pass123'),
            ('Mary Smith', 'mary@example.com', '+256 700 333 333', 'mary2024'),
        )
        for full_name, email, phone, password in demo_customers:
            if not c.execute('SELECT 1 FROM customers WHERE email=? COLLATE NOCASE', (email,)).fetchone():
                c.execute('INSERT INTO customers(full_name,email,phone,password_hash) VALUES(?,?,?,?)',
                          (full_name, email, phone, hash_password(password)))
        if not c.execute('SELECT 1 FROM hotel_state WHERE id=1').fetchone():
            c.execute('INSERT INTO hotel_state(id,payload) VALUES(1,?)',(json.dumps(seed_state(),ensure_ascii=False),))

def audit(c, user, action, detail=''):
    c.execute('INSERT INTO audit(user_id,username,action,detail) VALUES(?,?,?,?)', (user['id'] if user else None, user['username'] if user else 'system', action, detail[:500]))

def seed_state():
    # Used only for the first launch; sample data is clearly marked in the UI.
    return {
      'rooms':[{'id':1,'no':'101','type':'Standard Single','floor':'1','capacity':1,'rate':85000,'status':'Available','notes':'Garden view'},{'id':2,'no':'102','type':'Standard Double','floor':'1','capacity':2,'rate':115000,'status':'Occupied','notes':'King bed'},{'id':3,'no':'103','type':'Deluxe','floor':'1','capacity':2,'rate':165000,'status':'Dirty','notes':'Balcony'},{'id':4,'no':'201','type':'Executive Suite','floor':'2','capacity':2,'rate':240000,'status':'Reserved','notes':'Lounge access'},{'id':5,'no':'202','type':'Family Room','floor':'2','capacity':4,'rate':210000,'status':'Available','notes':'Two bedrooms'},{'id':6,'no':'203','type':'Presidential Suite','floor':'2','capacity':3,'rate':420000,'status':'Maintenance','notes':'Premium suite'}],
      'guests':[{'id':1,'name':'Daniel Okello','phone':'+256 700 123 456','email':'daniel@example.com','nationality':'Ugandan','idNo':'CM123456','vip':'Yes','notes':'Prefers quiet room'},{'id':2,'name':'Sarah Namusoke','phone':'+256 772 555 010','email':'sarah@example.com','nationality':'Ugandan','idNo':'CM789012','vip':'No','notes':''}],
      'reservations':[], 'housekeeping':[{'id':1,'room':'103','task':'Full room cleaning','assigned':'Grace','status':'Pending','date':time.strftime('%Y-%m-%d')}],
      'restaurant':[{'id':1,'table':'T-04','item':'Grilled fish','guest':'Walk-in','qty':1,'amount':45000,'status':'Preparing'}],
      'inventory':[{'id':1,'item':'Bath towels','category':'Housekeeping','stock':42,'min':20,'unit':'pcs','supplier':'CleanSupply'},{'id':2,'item':'Drinking water','category':'Food & Beverage','stock':12,'min':25,'unit':'bottles','supplier':'Aqua Ltd'}],
      'employees':[{'id':1,'name':'Grace Auma','role':'Housekeeping','phone':'+256 700 111 222','shift':'Morning','status':'Active'}], 'payments':[]
    }

class Handler(BaseHTTPRequestHandler):
    server_version='StarviewHotel/4.0'
    def log_message(self, fmt, *args): print('%s - %s' % (self.address_string(), fmt%args))
    def send_json(self, code, obj, headers=None):
        body=json.dumps(obj, ensure_ascii=False).encode()
        self.send_response(code); self.send_header('Content-Type','application/json; charset=utf-8'); self.send_header('Content-Length',str(len(body))); self.send_header('Cache-Control','no-store'); self.send_header('X-Content-Type-Options','nosniff'); self.send_header('Referrer-Policy','same-origin'); self.send_header('X-Frame-Options','DENY')
        for k,v in (headers or {}).items(): self.send_header(k,v)
        self.end_headers(); self.wfile.write(body)
    def body(self):
        n=min(int(self.headers.get('Content-Length','0')), 2_000_000)
        raw=self.rfile.read(n)
        return json.loads(raw or b'{}')
    def current_user(self):
        cookie=self.headers.get('Cookie','')
        token=''
        for part in cookie.split(';'):
            if part.strip().startswith('starview_session='): token=part.strip().split('=',1)[1]
        if not token: return None
        th=hashlib.sha256(token.encode()).hexdigest()
        with connect() as c:
            row=c.execute('SELECT u.* FROM sessions s JOIN users u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>? AND u.active=1',(th,int(time.time()))).fetchone()
            return dict(row) if row else None
    def current_customer(self):
        cookie=self.headers.get('Cookie',''); token=''
        for part in cookie.split(';'):
            if part.strip().startswith('starview_customer='): token=part.strip().split('=',1)[1]
        if not token: return None
        th=hashlib.sha256(token.encode()).hexdigest()
        with connect() as c:
            row=c.execute('SELECT cu.* FROM customer_sessions s JOIN customers cu ON cu.id=s.customer_id WHERE s.token_hash=? AND s.expires_at>?',(th,int(time.time()))).fetchone()
            return dict(row) if row else None
    def require_user(self):
        u=self.current_user()
        if not u: self.send_json(401,{'error':'Please sign in again.'}); return None
        return u
    def do_GET(self):
        p=urlparse(self.path).path
        if p.startswith('/api/'):
            u=self.current_user()
            if p=='/api/auth/me':
                return self.send_json(200, {'authenticated':bool(u), 'user':({'username':u['username'],'fullName':u['full_name'],'role':u['role'],'mustChange':bool(u['must_change'])} if u else None)})
            if p=='/api/public/rooms':
                with connect() as c:
                    row=c.execute('SELECT payload FROM hotel_state WHERE id=1').fetchone(); state=json.loads(row['payload']) if row else {}
                return self.send_json(200,[{k:r.get(k) for k in ('no','type','capacity','rate','status','notes')} for r in state.get('rooms',[]) if r.get('status') not in ('Maintenance','Dirty')])
            if p=='/api/customer/me':
                cu=self.current_customer()
                return self.send_json(200,{'authenticated':bool(cu),'customer':({'id':cu['id'],'fullName':cu['full_name'],'email':cu['email'],'phone':cu['phone']} if cu else None)})
            if p=='/api/customer/bookings':
                cu=self.current_customer()
                if not cu: return self.send_json(401,{'error':'Customer sign-in required'})
                with connect() as c:
                    row=c.execute('SELECT payload FROM hotel_state WHERE id=1').fetchone(); state=json.loads(row['payload']) if row else {}
                return self.send_json(200,[r for r in state.get('reservations',[]) if str(r.get('customerEmail','')).lower()==cu['email'].lower()])
            if not u: return self.send_json(401,{'error':'Authentication required'})
            if p=='/api/state':
                with connect() as c:
                    row=c.execute('SELECT payload FROM hotel_state WHERE id=1').fetchone()
                    state=json.loads(row['payload']) if row else {}
                allowed=ROLE_DATA.get(u['role'],set())
                return self.send_json(200, {k:v for k,v in state.items() if k in allowed})
            if p=='/api/users':
                if u['role'] not in ('Administrator','Manager'): return self.send_json(403,{'error':'Administrator permission required'})
                with connect() as c: rows=c.execute('SELECT id,username,full_name,role,active,must_change,created_at FROM users ORDER BY id').fetchall()
                return self.send_json(200,[dict(x) for x in rows])
            if p=='/api/audit':
                if u['role'] not in ('Administrator','Manager'): return self.send_json(403,{'error':'Administrator permission required'})
                with connect() as c: rows=c.execute('SELECT username,action,detail,created_at FROM audit ORDER BY id DESC LIMIT 100').fetchall()
                return self.send_json(200,[dict(x) for x in rows])
            if p=='/api/backup':
                if u['role']!='Administrator': return self.send_json(403,{'error':'Administrator permission required'})
                stamp=time.strftime('%Y%m%d-%H%M%S'); target=DATA/f'starview-backup-{stamp}.sqlite3'
                with connect() as src, sqlite3.connect(target) as dst: src.backup(dst)
                b=target.read_bytes(); self.send_response(200); self.send_header('Content-Type','application/vnd.sqlite3'); self.send_header('Content-Disposition',f'attachment; filename="{target.name}"'); self.send_header('Content-Length',str(len(b))); self.end_headers(); self.wfile.write(b); return
            return self.send_json(404,{'error':'Not found'})
        if p in ('/portal','/portal/'): p='/customer.html'
        path=(WEB / p.lstrip('/')).resolve()
        if not path.is_relative_to(WEB.resolve()) or not path.is_file(): path=WEB/'index.html'
        data=path.read_bytes(); ctype=mimetypes.guess_type(str(path))[0] or 'application/octet-stream'
        self.send_response(200); self.send_header('Content-Type',ctype + ('; charset=utf-8' if ctype.startswith(('text/','application/javascript')) else '')); self.send_header('Content-Length',str(len(data))); self.send_header('X-Content-Type-Options','nosniff'); self.send_header('Referrer-Policy','same-origin'); self.send_header('X-Frame-Options','DENY'); self.end_headers(); self.wfile.write(data)
    def do_POST(self):
        p=urlparse(self.path).path
        try: data=self.body()
        except Exception: return self.send_json(400,{'error':'Invalid request data'})
        if p=='/api/customer/register':
            name=str(data.get('fullName','')).strip(); email=str(data.get('email','')).strip().lower(); phone=str(data.get('phone','')).strip(); password=str(data.get('password',''))
            if not name or len(name)>120 or '@' not in email or len(email)>190 or not phone or len(phone)>40 or len(password)<12:
                return self.send_json(400,{'error':'Enter your name, valid email, phone number and a password of at least 12 characters'})
            try:
                with LOCK, connect() as c:
                    c.execute('INSERT INTO customers(full_name,email,phone,password_hash) VALUES(?,?,?,?)',(name,email,phone,hash_password(password)))
                    cid=c.execute('SELECT last_insert_rowid()').fetchone()[0]
                    row=c.execute('SELECT payload FROM hotel_state WHERE id=1').fetchone(); state=json.loads(row['payload']) if row else seed_state()
                    guests=state.setdefault('guests',[])
                    if not any(str(g.get('email','')).lower()==email for g in guests):
                        guests.append({'id':max([int(g.get('id',0)) for g in guests]+[0])+1,'name':name,'phone':phone,'email':email,'nationality':'','idNo':'','vip':'No','notes':'Registered through customer portal'})
                        c.execute('UPDATE hotel_state SET payload=?,updated_at=CURRENT_TIMESTAMP WHERE id=1',(json.dumps(state,ensure_ascii=False),))
                    token=secrets.token_urlsafe(36); c.execute('INSERT INTO customer_sessions(token_hash,customer_id,expires_at) VALUES(?,?,?)',(hashlib.sha256(token.encode()).hexdigest(),cid,int(time.time())+12*3600))
            except sqlite3.IntegrityError: return self.send_json(409,{'error':'An account with this email already exists'})
            secure='; Secure' if self.headers.get('X-Forwarded-Proto','').lower()=='https' else ''
            return self.send_json(201,{'success':True,'customer':{'fullName':name,'email':email,'phone':phone}},{'Set-Cookie':f'starview_customer={token}; HttpOnly; SameSite=Strict; Path=/; Max-Age=43200{secure}'})
        if p=='/api/customer/login':
            email=str(data.get('email','')).strip().lower(); password=str(data.get('password','')); ip=self.client_address[0]; now=int(time.time())
            with connect() as c:
                fail=c.execute('SELECT count,since_ts FROM customer_login_failures WHERE ip=?',(ip,)).fetchone()
                if fail and fail['count']>=8 and now-fail['since_ts']<900: return self.send_json(429,{'error':'Too many sign-in attempts. Wait 15 minutes and try again.'})
                row=c.execute('SELECT * FROM customers WHERE email=? COLLATE NOCASE',(email,)).fetchone()
                if not row or not verify_password(password,row['password_hash']):
                    if not fail: c.execute('INSERT INTO customer_login_failures(ip,count,since_ts) VALUES(?,?,?)',(ip,1,now))
                    elif now-fail['since_ts']>=900: c.execute('UPDATE customer_login_failures SET count=1,since_ts=? WHERE ip=?',(now,ip))
                    else: c.execute('UPDATE customer_login_failures SET count=count+1 WHERE ip=?',(ip,))
                    return self.send_json(401,{'error':'Incorrect email or password'})
                c.execute('DELETE FROM customer_login_failures WHERE ip=?',(ip,))
                token=secrets.token_urlsafe(36)
                c.execute('INSERT INTO customer_sessions(token_hash,customer_id,expires_at) VALUES(?,?,?)',(hashlib.sha256(token.encode()).hexdigest(),row['id'],now+12*3600))
            secure='; Secure' if self.headers.get('X-Forwarded-Proto','').lower()=='https' else ''
            return self.send_json(200,{'success':True,'customer':{'fullName':row['full_name'],'email':row['email'],'phone':row['phone']}},{'Set-Cookie':f'starview_customer={token}; HttpOnly; SameSite=Strict; Path=/; Max-Age=43200{secure}'})
        if p=='/api/customer/logout':
            cookie=self.headers.get('Cookie',''); token=''
            for part in cookie.split(';'):
                if part.strip().startswith('starview_customer='): token=part.strip().split('=',1)[1]
            with connect() as c: c.execute('DELETE FROM customer_sessions WHERE token_hash=?',(hashlib.sha256(token.encode()).hexdigest(),))
            return self.send_json(200,{'success':True},{'Set-Cookie':'starview_customer=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0'})
        if p=='/api/customer/booking':
            cu=self.current_customer()
            if not cu: return self.send_json(401,{'error':'Please sign in to submit a booking request'})
            room_no=str(data.get('room','')).strip(); start=str(data.get('checkin','')); end=str(data.get('checkout',''))
            try:
                from datetime import date
                adults=int(data.get('adults',1) or 1)
                if date.fromisoformat(end)<=date.fromisoformat(start) or date.fromisoformat(start)<date.today() or adults<1 or adults>12: raise ValueError()
            except Exception: return self.send_json(400,{'error':'Choose valid future check-in/check-out dates and a guest count between 1 and 12'})
            with LOCK, connect() as c:
                row=c.execute('SELECT payload FROM hotel_state WHERE id=1').fetchone(); state=json.loads(row['payload']) if row else seed_state()
                room=next((r for r in state.get('rooms',[]) if str(r.get('no'))==room_no),None)
                if not room or room.get('status') in ('Maintenance','Dirty') or int(room.get('capacity',1))<adults: return self.send_json(400,{'error':'That room is not suitable for the selected guest count'})
                conflict=any(str(r.get('room'))==room_no and r.get('status') not in ('Cancelled','Checked-out') and start<str(r.get('checkout','')) and end>str(r.get('checkin','')) for r in state.get('reservations',[]))
                if conflict: return self.send_json(409,{'error':'That room is already requested or booked for those dates'})
                reservations=state.setdefault('reservations',[]); rid=max([int(r.get('id',0)) for r in reservations]+[1000])+1
                nights=(date.fromisoformat(end)-date.fromisoformat(start)).days; total=int(room.get('rate',0))*nights
                reservations.append({'id':rid,'guest':cu['full_name'],'customerEmail':cu['email'],'room':room_no,'checkin':start,'checkout':end,'adults':adults,'source':'Customer portal','status':'Pending approval','total':total,'paid':0})
                c.execute('UPDATE hotel_state SET payload=?,updated_at=CURRENT_TIMESTAMP WHERE id=1',(json.dumps(state,ensure_ascii=False),))
                c.execute('INSERT INTO audit(username,action,detail) VALUES(?,?,?)',(cu['email'],'CUSTOMER_BOOKING_REQUEST',f'Reservation #{rid} · room {room_no}'))
            return self.send_json(201,{'success':True,'bookingId':rid,'total':total,'currency':'UGX'})
        if p=='/api/auth/login':
            ip=self.client_address[0]
            with connect() as c:
                fail=c.execute('SELECT count,since_ts FROM login_failures WHERE ip=?',(ip,)).fetchone()
                if fail and fail['count']>=8 and time.time()-fail['since_ts']<900: return self.send_json(429,{'error':'Too many attempts. Wait 15 minutes and try again.'})
                row=c.execute('SELECT * FROM users WHERE username=? COLLATE NOCASE AND active=1',(str(data.get('username','')).strip(),)).fetchone()
                if not row or not verify_password(str(data.get('password','')),row['password_hash']):
                    now=int(time.time())
                    if not fail: c.execute('INSERT INTO login_failures(ip,count,since_ts) VALUES(?,?,?)',(ip,1,now))
                    elif now-fail['since_ts']>=900: c.execute('UPDATE login_failures SET count=1,since_ts=? WHERE ip=?',(now,ip))
                    else: c.execute('UPDATE login_failures SET count=count+1 WHERE ip=?',(ip,))
                    audit(c,None,'LOGIN_FAILED',ip); return self.send_json(401,{'error':'Incorrect username or password'})
                c.execute('DELETE FROM login_failures WHERE ip=?',(ip,))
                token=secrets.token_urlsafe(36); th=hashlib.sha256(token.encode()).hexdigest(); exp=int(time.time())+12*3600
                c.execute('INSERT INTO sessions(token_hash,user_id,expires_at) VALUES(?,?,?)',(th,row['id'],exp)); audit(c,dict(row),'LOGIN','Signed in')
            secure='; Secure' if self.headers.get('X-Forwarded-Proto','').lower()=='https' else ''
            return self.send_json(200,{'success':True,'user':{'username':row['username'],'fullName':row['full_name'],'role':row['role'],'mustChange':bool(row['must_change'])}}, {'Set-Cookie':f'starview_session={token}; HttpOnly; SameSite=Strict; Path=/; Max-Age=43200{secure}'})
        u=self.current_user()
        if not u: return self.send_json(401,{'error':'Authentication required'})
        if p=='/api/auth/logout':
            cookie=self.headers.get('Cookie',''); token=''
            for part in cookie.split(';'):
                if part.strip().startswith('starview_session='): token=part.strip().split('=',1)[1]
            with connect() as c:
                c.execute('DELETE FROM sessions WHERE token_hash=?',(hashlib.sha256(token.encode()).hexdigest(),)); audit(c,u,'LOGOUT','Signed out')
            return self.send_json(200,{'success':True},{'Set-Cookie':'starview_session=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0'})
        if p=='/api/auth/password':
            old=str(data.get('currentPassword','')); new=str(data.get('newPassword',''))
            if len(new)<12: return self.send_json(400,{'error':'New password must be at least 12 characters'})
            with connect() as c:
                row=c.execute('SELECT password_hash FROM users WHERE id=?',(u['id'],)).fetchone()
                if not verify_password(old,row['password_hash']): return self.send_json(400,{'error':'Current password is incorrect'})
                cookie=self.headers.get('Cookie',''); token=''
                for part in cookie.split(';'):
                    if part.strip().startswith('starview_session='): token=part.strip().split('=',1)[1]
                current_hash=hashlib.sha256(token.encode()).hexdigest()
                c.execute('UPDATE users SET password_hash=?,must_change=0 WHERE id=?',(hash_password(new),u['id']))
                c.execute('DELETE FROM sessions WHERE user_id=? AND token_hash<>?',(u['id'],current_hash))
                audit(c,u,'PASSWORD_CHANGED','Password updated; other sessions revoked')
            return self.send_json(200,{'success':True})
        if p=='/api/users':
            if u['role'] not in ('Administrator','Manager'): return self.send_json(403,{'error':'Administrator permission required'})
            username=str(data.get('username','')).strip(); full=str(data.get('fullName','')).strip(); role=str(data.get('role','')); pw=str(data.get('password',''))
            if not username or not full or role not in ROLE_DATA or len(pw)<12: return self.send_json(400,{'error':'Enter a username, full name, valid role and password of at least 12 characters'})
            try:
                with connect() as c:
                    c.execute('INSERT INTO users(username,full_name,role,password_hash,must_change) VALUES(?,?,?,?,1)',(username,full,role,hash_password(pw))); audit(c,u,'USER_CREATED',f'{username} · {role}')
            except sqlite3.IntegrityError: return self.send_json(409,{'error':'That username already exists'})
            return self.send_json(201,{'success':True})
        return self.send_json(404,{'error':'Not found'})
    def do_PUT(self):
        p=urlparse(self.path).path; u=self.current_user()
        if not u: return self.send_json(401,{'error':'Authentication required'})
        if p!='/api/state': return self.send_json(404,{'error':'Not found'})
        try: incoming=self.body()
        except Exception: return self.send_json(400,{'error':'Invalid state data'})
        allowed=ROLE_DATA.get(u['role'],set())
        with LOCK, connect() as c:
            row=c.execute('SELECT payload FROM hotel_state WHERE id=1').fetchone()
            state=json.loads(row['payload']) if row else seed_state()
            for key in allowed:
                if key in incoming and isinstance(incoming[key],list): state[key]=incoming[key]
            c.execute('INSERT INTO hotel_state(id,payload,updated_at) VALUES(1,?,CURRENT_TIMESTAMP) ON CONFLICT(id) DO UPDATE SET payload=excluded.payload,updated_at=CURRENT_TIMESTAMP',(json.dumps(state,ensure_ascii=False),))
            audit(c,u,'DATA_UPDATED',','.join(sorted(set(incoming)&allowed)))
        return self.send_json(200,{'success':True})

def automatic_backups():
    backup_dir=DATA/'backups'; backup_dir.mkdir(exist_ok=True)
    last_day=''
    while True:
        now=time.localtime()
        if now.tm_hour==2 and time.strftime('%Y-%m-%d',now)!=last_day:
            stamp=time.strftime('%Y%m%d-%H%M%S'); target=backup_dir/f'starview-auto-{stamp}.sqlite3'
            try:
                with connect() as src, sqlite3.connect(target) as dst: src.backup(dst)
                last_day=time.strftime('%Y-%m-%d',now)
                old=sorted(backup_dir.glob('starview-auto-*.sqlite3'))
                for f in old[:-14]: f.unlink(missing_ok=True)
                print('Automatic database backup created:',target.name)
            except Exception as e: print('Automatic backup failed:',e)
        time.sleep(60)

def main():
    init_db()
    threading.Thread(target=automatic_backups,daemon=True).start()
    host=os.environ.get('STARVIEW_HOST','127.0.0.1'); port=int(os.environ.get('STARVIEW_PORT','8080'))
    print('\nSTARVIEW HOTEL MANAGEMENT SUITE v4.0')
    print(f'Open http://localhost:{port} on this computer')
    print('For hotel LAN access, set STARVIEW_HOST=0.0.0.0 and secure the network first; port',port)
    if os.environ.get('STARVIEW_ADMIN_PASSWORD'):
        print('Administrator initial password was supplied through STARVIEW_ADMIN_PASSWORD.')
    else:
        print('Initial sign-in: admin / ChangeMe123! (change it immediately)')
    print('Set STARVIEW_ADMIN_PASSWORD before first launch to choose another initial password.\n')
    ThreadingHTTPServer((host,port),Handler).serve_forever()
if __name__=='__main__': main()
