# Smart Clinic — Security Checklist

Derived from `schema-design.md`, `user_stories.md`, and `schema-architecture.md`.

## Authentication

- [x] Single Admin account seeded at startup (not self-registered)
- [x] Admin credentials configurable via `admin.username` / `admin.password` properties
- [x] Doctor and Patient accounts created by Admin only (no public registration)
- [x] Passwords stored as BCrypt hashes (never plain text)
- [x] `passwordHash` field marked `@JsonProperty(access = WRITE_ONLY)` — never returned to client
- [x] Form login with Spring Security `DaoAuthenticationProvider`
- [x] Login failure returns generic error (no user enumeration)
- [x] Session invalidation on logout, `JSESSIONID` cookie deleted
- [x] `ClinicUserDetails` wraps Admin/Doctor/Patient with `isEnabled()` tied to `isActive`

## Authorization

- [x] Role-based access control: ADMIN, DOCTOR, PATIENT roles
- [x] `@PreAuthorize("hasRole('ADMIN')")` on admin-only endpoints
- [x] `@PreAuthorize("hasRole('DOCTOR')")` on doctor-only endpoints
- [x] `@PreAuthorize("hasRole('PATIENT')")` on patient-only endpoints
- [x] Doctor can only view/edit appointments where `doctorId == me`
- [x] Patient can only view/cancel own appointments (`patientId == me`)
- [x] Doctor can only create/edit/delete prescriptions where `doctorId == me` (append-only ledger)
- [x] Patient can only view own prescriptions (`patientId == me`)
- [x] Doctor can only create prescriptions for their assigned patient
- [x] Cross-role access blocked (e.g., patient cannot access `/doctor/**`)

## Input Validation

- [x] Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Email`, `@Positive`) on all request DTOs
- [x] `@Email` on email fields, `@NotBlank` on required text fields
- [x] `GlobalExceptionHandler` converts `MethodArgumentNotValidException` to 400 responses
- [x] `IllegalArgumentException` → 400, `IllegalStateException` → 409, generic → 500
- [x] No stack traces leaked to client in error responses

## Data Protection

- [x] SQL injection prevention via Spring Data JPA parameter binding (no string concatenation)
- [x] Double-booking prevention: service-layer transactional check (not DB UNIQUE constraint)
- [x] Patient double-booking prevention: one SCHEDULED appointment per patient
- [x] Soft deletes via `isActive` flag — no hard deletes for Admin/Doctor/Patient
- [x] Prescription append-only ledger: reassigned doctors create new documents, never edit others'
- [x] Cross-database references (`patientId`, `doctorId` in MongoDB) validated against MySQL before write

## Secrets & Configuration

- [x] No secrets hardcoded in source code
- [x] Database credentials via `spring.datasource.username/password` (env-overridable)
- [x] SMTP credentials via `spring.mail.username/password` (env-overridable)
- [x] Admin seed password via `admin.password` property (env-overridable)
- [x] `.env.example` pattern: all configurable values documented in `application.properties`
- [x] `application.properties` does not contain real credentials (uses `${VAR:default}` placeholders)

## HTTP Security

- [x] CSRF disabled for `/api/**` endpoints (REST API), enabled for form login/logout
- [x] `/login`, `/css/**`, `/js/**`, `/images/**`, `/api/public/**` permitAll
- [x] All other endpoints require authentication (`anyRequest().authenticated()`)
- [x] CORS not configured (MVP: same-origin Thymeleaf + same-origin API)

## Dependencies

- [x] Lombok version managed by Spring Boot parent POM (no duplicate declarations)
- [x] All dependencies declared once in `pom.xml`
- [x] Spring Boot 4.1.2, Java 17

## Deployment

- [ ] HTTPS enforced at deployment layer (reverse proxy / load balancer)
- [ ] Database credentials injected via environment variables, not config files
- [ ] SMTP credentials injected via environment variables
- [ ] Admin seed password changed from default (`admin123`) in production
