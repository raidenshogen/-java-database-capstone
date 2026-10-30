# Smart Clinic — Handover Guide

Written so another developer can continue work in about one hour.

## Folder Map

```
src/main/java/org/shopwave/smartclinic/
  admin/          AdminService
  appointment/    AppointmentService, AppointmentController, DTOs
  auth/           AuthController, PasswordResetService, DTOs
  config/         DataLoader (seeds admin + specialties on startup)
  dto/            All request/response DTOs
  doctor/         DoctorService, DoctorController, DTOs
  email/          EmailService
  patient/        PatientService, PatientController, DTOs
  prescription/   PrescriptionService, PrescriptionController, DTOs (MongoDB)
  repository/     JPA repos + MongoDB repo
  security/       UserRole, ClinicUserDetails, ClinicUserDetailsService, SecurityConfig
  specialty/      SpecialtyService, SpecialtyController, DTOs
  web/            Dashboard controllers, GlobalExceptionHandler
  model/          JPA entities (Admin, Doctor, Patient, Appointment, Specialty,
                  AppointmentStatus) + MongoDB document (Prescription)

src/main/resources/
  application.properties
  templates/login.html
  templates/admin/dashboard.html
  templates/doctor/dashboard.html
  templates/patient/dashboard.html

docs/
  schema-design.md        — MySQL + MongoDB schema
  schema-architecture.md  — Three-tier architecture
  user_stories.md         — All user stories with acceptance criteria
  03-backlog.md           — Implementation backlog with ticket statuses
  04-security-checklist.md — Security checklist

README.md        — Features, tech stack, how to run, API summary
HANDOVER.md      — This file
```

## How to Run / Test / Debug

### Prerequisites
- Java 17
- MySQL 8.0+ (schema: `smartclinic`)
- MongoDB 5.0+ (database: `smartclinic`)
- SMTP server (optional; email features won't send without it)

### Build
Maven installation on this host is broken (`plexus.classworlds` missing).
Use a machine with working Maven:

```bash
cd /path/to/SmartClinic
mvn clean compile
mvn spring-boot:run
```

### Run Config
Edit `src/main/resources/application.properties`:
- `spring.datasource.username/password` — MySQL credentials
- `spring.data.mongodb.uri` — MongoDB connection string
- `admin.password` — change from default `admin123`
- `spring.mail.*` — SMTP for email notifications

### Default Admin
- Username: `admin`, Password: `admin123`

### Key Endpoints
- `GET /login` — login page
- `POST /api/auth/change-password` — self-service password change
- `POST /api/auth/forgot-password` — patient reset link request
- `POST /api/auth/reset-password` — patient reset confirmation
- `POST /api/auth/doctor-reset` — admin-triggered doctor reset
- `GET /api/specialties`, `POST /api/specialties` — CRUD
- `GET /api/doctors`, `POST /api/doctors` — CRUD
- `GET /api/patients`, `POST /api/patients` — CRUD
- `POST /api/appointments` — book
- `POST /api/prescriptions` — create (Doctor)

## Key Decisions

1. **Polyglot persistence:** MySQL (JPA) for structured data; MongoDB for prescriptions (variable-length nested structure).
2. **Soft deletes:** `isActive` flag on Doctor/Patient — never hard-delete, preserves history.
3. **Double-booking prevention:** Service-layer transactional check (no DB UNIQUE constraint, since cancelled rows must not block slots).
4. **Prescription append-only ledger:** Only the authoring doctor can edit/delete. Reassigned doctors create new documents.
5. **Single admin account:** Seeded at startup via DataLoader, not self-registered.
6. **Lombok:** Version managed by Spring Boot parent POM to avoid duplicate declarations.

## Conventions

- **Packages:** Feature-based (`admin`, `doctor`, `patient`, `appointment`, `prescription`, `specialty`, `auth`, `dto`, `repository`, `security`, `web`, `email`, `config`, `model`).
- **DTOs:** All in `dto/` package, request + response pairs, validation annotations.
- **Services:** `@Service` + `@Transactional(readOnly = true)` at class level, `@Transactional` on write methods.
- **Controllers:** `@RestController` for API, `@Controller` for Thymeleaf views.
- **Lombok:** `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` on entities and DTOs.
- **Exceptions:** `IllegalArgumentException` → 400, `IllegalStateException` → 409 via `GlobalExceptionHandler`.

## Known Issues

1. **Maven build broken on this host:** `mvn` fails with `ClassNotFoundException: org.codehaus.plexus.classworlds.launcher.Launcher`. Use another machine with working Maven. `compile.bat` in user home can verify Java compilation with `javac`.
2. **Password reset tokens stored in memory:** Lost on restart. Use database/Redis in production.
3. **No CORS configuration:** Frontend on different origin will need CORS config.
4. **No HTTPS:** Enforce at deployment layer (reverse proxy).
5. **No pagination:** Large lists may need pagination.
6. **Patient self-registration:** Not implemented (admin-only per user stories).
7. **Email is plain text:** HTML templates would improve UX.

## Next Tickets (from docs/03-backlog.md)

- **T-25:** Unit + integration tests for key flows (JUnit 5, Mockito)
- **T-26:** Security checklist verification (docs/04-security-checklist.md)
- **T-27:** README.md + HANDOVER.md (this file, README already done)
