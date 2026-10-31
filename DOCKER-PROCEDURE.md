# Smart Clinic — Docker Run Procedure

## Prerequisites
- Docker Desktop installed and running (Docker Desktop for Windows)
- Docker Desktop must be running (start it from Windows Start menu)

## Step 1 — Start Docker Desktop
1. Open Windows Start menu
2. Launch "Docker Desktop"
3. Wait until the whale icon appears in the system tray (bottom-right)
4. Click the whale icon → "View in Docker Desktop" to verify it's running

## Step 2 — Start the infrastructure (MySQL + MongoDB)
Open a terminal (cmd or PowerShell) and run:

```bash
cd C:\Users\zenas\Documents\for my benifit project\SmartClinic\SmartClinic
docker-compose up -d
```

This will start:
- **mysql** container on port 3306 (MySQL 8.0)
- **mongo** container on port 27017 (MongoDB 5.0)

Wait a few seconds for the databases to initialize:
```bash
docker-compose ps
```

You should see both containers running:

```
NAME               STATUS              PORTS
smartclinic-mysql  running   0.0.0.0:3306->3306/tcp, :::3306->:::3306/tcp
smartclinic-mongo  running   0.0.0.0:27017->27017/tcp, :::27017->:::27017/tcp
```

## Step 3 — Build and run the application

```bash
# Build the Spring Boot app Docker image
docker-compose up --build app
```

This will:
1. Build the Java app using Maven wrapper (`./mvnw`)
2. Create the Docker image
3. Start the app container on port 8080

Wait for the build to complete. You'll see output like:
```
Building SmartClinic ...
...
BUILD SUCCESS
```

## Step 4 — Access the application

Open your web browser and go to:

```
http://localhost:8080/login
```

## Login Credentials

**Default Admin Account:**
- Username: `admin`
- Password: `admin123` (change in production!)

## API Endpoints

The app exposes REST APIs at `http://localhost:8080/api/...`

Key endpoints (from README.md):
- `POST /api/appointments` — book appointment (Patient)
- `GET /api/appointments/patient/{id}` — patient's appointments
- `GET /api/appointments/doctor/{id}/today` — doctor's schedule
- `POST /api/prescriptions` — create prescription (Doctor)
- `GET /api/prescriptions/patient/{id}` — patient's prescription history
- `GET /api/specialties` — list all specialties
- `GET /api/doctors` — list active doctors
- `GET /api/patients` — list active patients

## Step 5 — Stop the services

When you're done, stop everything:

```bash
# Stop the app (keep containers for next time)
docker-compose down

# Or stop all containers including databases
docker-compose down -v  # -v removes named volumes too
```

## Docker Configuration Files

### `docker-compose.yml`
- MySQL 8.0 on port 3306
- MongoDB 5.0 on port 27017  
- Spring Boot app on port 8080
- All environment variables configured (no hardcoded secrets)
- Network bridge between containers

### `Dockerfile`
- Multi-stage build (build with Maven, run with JRE only)
- Based on `eclipse-temurin:17` (Java 17)
- Uses Maven wrapper (`./mvnw`) for building
- Final image is ~100MB (only JRE, no build tools)

## Need a Different Database Credential?

Edit `docker-compose.yml` and change the environment variables:
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_USER` / `MYSQL_PASSWORD` 
- `MONGO_ROOT_PASSWORD`
- `ADMIN_PASSWORD`
- `JWT_SECRET` (must be min 32 chars)

## Troubleshooting

### If Docker Desktop won't start:
- Ensure Windows has virtualization enabled in BIOS (VT-d/AMD-V)
- Right-click Docker Desktop → Settings → Resources → Advanced
- Allocate more CPU/RAM if needed

### If containers fail to start:
```bash
# Check logs
docker-compose logs

# Or check individual container logs
docker logs smartclinic-mysql
docker logs smartclinic-mongo
docker logs smartclinic-app
```

### If Maven build fails inside Docker:
The Dockerfile uses `./mvnw` (Maven wrapper). If it fails, you can:
1. Run `chmod +x ./mvnw` to make it executable
2. Or replace with system Maven: `mvn clean package -DskipTests`

### If MySQL connection fails:
The app connects to `mysql:3306` (Docker internal network name). 
The `createDatabaseIfNotExist=true` in the JDBC URL ensures the database is created automatically.

### If MongoDB connection fails:
The app connects to `mongodb://mongo:27017/smartclinic`.
The `MONGO_INITDB_DATABASE: smartclinic` in docker-compose creates the database on first start.

## Full Workflow Summary

```bash
# 1. Start Docker Desktop (Windows Start menu)

# 2. Start databases
cd C:\Users\zenas\Documents\for my benifit project\SmartClinic\SmartClinic
docker-compose up -d

# 3. Build and run app
docker-compose up --build app

# 4. Browse to http://localhost:8080/login
#    Login: admin / admin123

# 5. When done: docker-compose down
```

The Docker setup completely eliminates the need for local MySQL/MongoDB installations. All databases, build tools, and the application run inside isolated containers.