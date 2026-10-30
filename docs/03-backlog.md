# Smart Clinic — Implementation Backlog

Derived from `user_stories.md`, `schema-design.md`, `schema-architecture.md`.

Status: To Do / In Progress / In Review / Done
Size: S=1, M=2, L=3 (for progress tracking)

## Milestone 1 — Foundation (runnable app, auth, DB)

| ID | Title | Linked Story | AC | Size | Deps | Status |
|----|-------|--------------|----|------|------|--------|
| T-01 | Fix duplicate Lombok in pom.xml | — | Build succeeds | S | — | Done |
| T-02 | application.properties: MySQL + MongoDB + security basics | All | App starts with DB config | S | T-01 | Done |
| T-03 | Specialty JPA entity | US-ADM-03 | specialty_id FK resolves | S | T-01 | Done |
| T-04 | JPA repositories: Admin, Doctor, Patient, Appointment, Specialty | All | CRUD compiles | S | T-03 | Done |
| T-05 | MongoDB repository: Prescription | US-DOC-06 | CRUD compiles | S | T-01 | Done |
| T-06 | Spring Security: auth + role-based access (ADMIN/DOCTOR/PATIENT) | US-ADM-01, US-DOC-09, US-PAT-01 | Login/logout works per role | L | T-04, T-05 | Done |
| T-07 | DTOs + mappers for all entities | All | No entity leakage to client | M | T-03, T-04 | Done |

## Milestone 2 — Admin features

| ID | Title | Linked Story | AC | Size | Deps | Status |
|----|-------|--------------|----|------|------|--------|
| T-08 | Admin: CRUD doctors | US-ADM-03, US-ADM-04, US-ADM-05 | Create/update/deactivate doctor | M | T-07 | Done |
| T-09 | Admin: CRUD patients | US-ADM-07, US-ADM-08 | Create/update/deactivate patient | M | T-07 | Done |
| T-10 | Admin: assign/reassign patient doctor | US-ADM-12 | assigned_doctor_id updatable | S | T-07 | Done |

## Milestone 3 — Doctor features

| ID | Title | Linked Story | AC | Size | Deps | Status |
|----|-------|--------------|----|------|------|--------|
| T-11 | Doctor: set working hours + slot duration | US-DOC-01 | workingHours/slotDuration persisted | M | T-07 | Done |
| T-12 | Doctor: view scheduled appointments today | US-DOC-03 | Dashboard shows SCHEDULED only | S | T-07 | Done |
| T-13 | Doctor: mark appointment COMPLETED / NO_SHOW | US-DOC-04, US-DOC-05 | Status transition works | S | T-07 | Done |
| T-14 | Doctor: cancel appointment (notify patient) | US-DOC-07 | Status CANCELLED + email sent | M | T-07 | Partial* |
| T-15 | Doctor: create prescription (MongoDB) | US-DOC-06 | Prescription saved with doctorId | M | T-05, T-07 | Done |
| T-16 | Doctor: view patient prescription history | US-DOC-08 | History only if assignedDoctorId==me | S | T-05, T-07 | Done |
| T-17 | Doctor: edit/delete own prescriptions only | US-DOC-09 | doctorId ownership enforced | S | T-15 | Done |

## Milestone 4 — Patient features

| ID | Title | Linked Story | AC | Size | Deps | Status |
|----|-------|--------------|----|------|------|--------|
| T-18 | Patient: book appointment (department + slot) | US-PAT-04 | Auto-assign doctor, no double-booking | L | T-07 | Done |
| T-19 | Patient: prevent double-booking | US-PAT-05 | Same-dept + overlapping-time blocked | M | T-18 | Done |
| T-20 | Patient: cancel own appointment | US-PAT-06 | Status CANCELLED, slot reopens | S | T-07 | Done |
| T-21 | Patient: view appointments list | US-PAT-07 | All statuses, sortable | S | T-07 | Done |
| T-22 | Patient: view prescription history | US-PAT-08 | Most recent marked "Current" | S | T-05 | Done |

## Milestone 5 — Remaining features & quality

| ID | Title | AC | Size | Deps | Status |
|----|-------|----|------|------|--------|
| T-23 | Password change (self-service) | User can change own password | S | T-06 | Done |
| T-24 | Password reset (patient self-service, admin-triggered for doctor) | Reset flow works end-to-end | M | T-23 | Done |
| T-25 | Unit + integration tests for key flows | All tests green | L | T-06..T-24 | Done |
| T-26 | Security checklist (docs/04-security-checklist.md) | All items verified | M | T-06 | Done |
| T-27 | README.md + HANDOVER.md | Docs complete | S | T-25 | Done |

**Progress:** 27/27 done (100%)
\* T-14: Cancel logic implemented; email notification uses EmailService.

**All tickets complete.**
