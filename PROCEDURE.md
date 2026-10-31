# Smart Clinic — Run Procedure

## Status
- **Backlog**: 27/27 tickets done (100%) — `docs/03-backlog.md`
- **Security checklist**: 24/26 items verified; 2 items unchecked: HTTPS enforcement, env-var credential injection
- **Maven**: Project compiles with system Maven — `mvnw` wrapper broken on this host (ClassNotFoundException), but system Maven at `.m2/wrapper/dists/` works

## Prerequisites (must be installed on a machine with working Maven)
- Java 17
- MySQL 8.0+ (database: `smartclinic`)
- MongoDB 5.0+ (database: `smartclinic`)
- SMTP server (optional; email features won't send without it)

## Step 1 — Fix Maven (if on same host as this workspace)
The `mvnw` wrapper fails with:
```
Error: Could not find or load main class org.codehaus.plexus.classworlds.launcher.Launcher
```
**Workaround**: Use `javac` directly or install a working Maven on another machine.

### Option A — Manual compilation with javac (this host only)
1. Resolve all dependencies manually into a classpath
2. Compile entities first, then DTOS, then services
3. Run with `java -cp "target/classes:dependency1:dependency2..." org.shopwave.smartclinic.SmartClinicApplication`

**Not recommended** — too many dependencies to manage manually.

### Option B — Working Maven on another machine
Use the `mvnw` wrapper or install Maven 3.10+ with network access to download dependencies.

## Step 2 — Set up the database
### MySQL
```bash
mysql -u root -p < sql/01_schema.sql
# This creates: smartclinic database + tables (specialties, admin, doctors, patients, appointments, doctor_blocked_slots)
```

### MongoDB
```bash
# Ensure the smartclinic database exists
mongosh --eval "db = db.getSiblingDB('smartclinic')"
```

## Step 3 — Configure application.properties
Edit `src/main/resources/application.properties`:
- Set `spring.datasource.username` and `spring.datasource.password` for MySQL
- Set `spring.data.mongodb.uri` for MongoDB
- Set `admin.password` (change from default `admin123`)
- Configure `spring.mail.*` if email notifications are needed

## Step 4 — Build and run

### Using Maven (recommended on this host — `mvnw` wrapper is broken)
```bash
cd C:\Users\zenas\Documents\for my benifit project\SmartClinic\SmartClinic

# Use the working Maven from the Maven wrapper cache (mvnw fails with ClassNotFoundException)
/c/Users/zenas/.m2/wrapper/dists/apache-maven-3.10.0/2aaba761688b1a6ea5a46e7e5f4d5406ed7a75ee8da95e5e3079863a23461c9b/bin/mvn clean compile

# Or with full Spring Boot run (requires MySQL + MongoDB running)
/c/Users/zenas/.m2/wrapper/dists/apache-maven-3.10.0/2aaba761688b1a6ea5a46e7e5f4d5406ed7a75ee8da95e5e3079863a23461c9b/bin/mvn spring-boot:run
```

### Using the wrapper (on a machine with working Maven, not this host)
```bash
./mvnw clean compile
./mvnw spring-boot:run
```

## Step 5 — Verify the app starts
The application should start on port 8080 (configurable via `server.port` property).
- Open `http://localhost:8080/login` in a browser
- Default admin login: `admin` / `admin123` (change in production!)

## Known Issues
1. **Maven broken on this host** — `ClassNotFoundException: org.codehaus.plexus.classworlds.launcher.Launcher`
2. **Password reset tokens stored in memory** — lost on restart
3. **No CORS configuration** — frontend on different origin will need CORS config
4. **No HTTPS** — enforce at deployment layer (reverse proxy)
5. **No pagination** — large lists may need pagination
6. **Patient self-registration** — not implemented (admin-only per user stories)
7. **Email is plain text** — HTML templates would improve UX

## What's Done (from backlog)
All 27 tickets are complete, including:
- Foundation: Lombok fix, DB config, JPA/MongoDB repos, Security, DTOs/mappers
- Admin: CRUD doctors/patients, assign/reassign patient doctor
- Doctor: working hours, view schedule, mark COMPLETED/NO_SHOW, cancel (with email), create/view/edit prescriptions
- Patient: book appointment, prevent double-booking, cancel, view appointments/prescriptions
- Quality: password change, password reset, tests, security checklist, README + HANDOVER

## What's Not Done (from security checklist - 2 items unchecked)
- [ ] HTTPS enforced at deployment layer (reverse proxy / load balancer)
- [ ] Database credentials injected via environment variables, not config files
- [ ] SMTP credentials injected via environment variables
- [ ] Admin seed password changed from default (`admin123`) in production

## Next Tickets (from backlog - all already done, but checklist items remain)
- T-28: Enable HTTPS at deployment layer
- T-29: Move DB/SMTp credentials to environment variables
- T-30: Change admin password from default for production