# Smart Clinic Management System — Database Schema Design

This document defines the database schema for the Smart Clinic Management System, split
across two data stores per the system's architecture: **MySQL** for structured, relational
data (Admin, Doctor, Patient, Appointment records) and **MongoDB** for flexible,
document-based data (Prescriptions). Design decisions below are justified based on the
business rules established during requirements analysis (user stories).

---

## 1. MySQL Database Design

### Overview of Tables
| Table | Purpose |
|---|---|
| `admin` | Single administrative account |
| `specialties` | Lookup table of clinic departments/specialties |
| `doctors` | Doctor accounts, availability settings |
| `patients` | Patient accounts and current doctor assignment |
| `appointments` | Scheduled visits linking patients and doctors |
| `doctor_blocked_slots` | Time ranges a doctor marks as unavailable |

---

### 1.1 `admin`

```sql
CREATE TABLE admin (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Design notes:**
- Only one row is expected to ever exist in this table (seeded at setup, not self-registered), per our decision that there is a single fixed Admin account.
- `password_hash` — passwords are never stored in plain text; hashed via Spring Security (BCrypt).

---

### 1.2 `specialties`

```sql
CREATE TABLE specialties (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(100) NOT NULL UNIQUE
);
```

**Design notes:**
- Modeled as a separate lookup table rather than a hardcoded ENUM/string on the `doctors`
  table, so Admin can add new clinic departments without requiring a schema migration.
- This table directly powers the Patient's department-selection step when booking
  and the Admin's "patients per specialty" donut chart statistics view.

---

### 1.3 `doctors`

```sql
CREATE TABLE doctors (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name              VARCHAR(100) NOT NULL,
    last_name               VARCHAR(100) NOT NULL,
    email                   VARCHAR(150) NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    specialty_id            INT NOT NULL,
    working_hours_start     TIME NOT NULL DEFAULT '09:00:00',
    working_hours_end       TIME NOT NULL DEFAULT '17:00:00',
    slot_duration_minutes   INT NOT NULL DEFAULT 30,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_doctor_specialty
        FOREIGN KEY (specialty_id) REFERENCES specialties(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_slot_duration
        CHECK (slot_duration_minutes > 0),

    CONSTRAINT chk_working_hours
        CHECK (working_hours_end > working_hours_start)
);
```

**Design notes:**
- `is_active` implements **soft delete** — deactivated doctors are hidden from active
  listings but never physically removed, preserving appointment/prescription history.
- `working_hours_start/end` + `slot_duration_minutes` are used to **derive** available
  booking slots dynamically — there is intentionally **no** `max_patients_per_day` column,
  since that number is calculated, not stored, avoiding two competing sources of truth.
- `ON DELETE RESTRICT` on the specialty FK prevents accidentally deleting a specialty that
  still has doctors assigned to it.

---

### 1.4 `patients`

```sql
CREATE TABLE patients (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(150) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    phone               VARCHAR(20) NOT NULL,
    date_of_birth       DATE,
    assigned_doctor_id  BIGINT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_patient_assigned_doctor
        FOREIGN KEY (assigned_doctor_id) REFERENCES doctors(id)
        ON DELETE SET NULL
);
```

**Design notes:**
- `assigned_doctor_id` is **nullable** and implements the **single-field doctor assignment
  model** — this is the field Admin updates when reassigning a patient to a new doctor. We
  deliberately avoided a separate "doctor history" table, since the `prescriptions`
  collection in MongoDB already preserves authorship history per prescription — adding a
  second history table would create duplicate, driftable sources of truth.
- `ON DELETE SET NULL` — if a doctor record were ever hard-deleted (not expected under our
  soft-delete policy, but included as a safety net), affected patients become unassigned
  rather than causing a referential integrity failure.
- `is_active` — same soft-delete pattern as `doctors`.

---

### 1.5 `appointments`

```sql
CREATE TABLE appointments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id          BIGINT NOT NULL,
    doctor_id           BIGINT NOT NULL,
    appointment_date    DATE NOT NULL,
    appointment_time    TIME NOT NULL,
    status              ENUM('SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')
                        NOT NULL DEFAULT 'SCHEDULED',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_appointment_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_appointment_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors(id)
        ON DELETE RESTRICT
);
```

**Design notes:**
- `status` ENUM directly mirrors the four terminal states agreed upon in the Doctor/Patient
  user stories: `SCHEDULED`, `COMPLETED`, `NO_SHOW`, `CANCELLED` — all transitions are
  **manual, explicit actions** (never automatic based on time).
- **No UNIQUE constraint on `(doctor_id, appointment_date, appointment_time)` — this is a
  deliberate design decision, explained in detail below.**

#### 📌 Why there's no DB-level unique constraint on doctor/date/time

A natural instinct is to add `UNIQUE(doctor_id, appointment_date, appointment_time)` to
guarantee no double-booking. However, this system uses **soft deletes for cancellations** —
a cancelled appointment's row is never removed, only its `status` changes to `CANCELLED`.

If a plain UNIQUE constraint existed, a **cancelled** row would permanently "occupy" that
doctor/date/time combination, blocking any future patient from booking that now-free slot —
directly violating the rule that cancelled slots reopen immediately for anyone.

**Chosen solution:** Double-booking prevention is enforced in the **Service layer**, not
the database schema. Before inserting a new appointment, the application runs:

```sql
SELECT * FROM appointments 
WHERE doctor_id = ? AND appointment_date = ? AND appointment_time = ? 
AND status = 'SCHEDULED';
```

If a matching row exists, the booking is rejected; otherwise, it's allowed — even if a
`CANCELLED` row exists at that exact slot. This correctly supports instant slot reopening
on cancellation.

**Trade-off acknowledged:** This approach relies on application logic rather than an
unbreakable database constraint, so in theory a bug, direct DB write, or race condition
could create a duplicate booking. A more defensive alternative exists (a MySQL generated
column that nulls out non-`SCHEDULED` rows, paired with a unique index on that column) to
get database-level enforcement while still allowing cancelled rows to coexist. This was
considered but intentionally **not implemented**, as Service-layer validation is deemed
sufficient for this project's scope.

- `ON DELETE RESTRICT` on both FKs ensures appointment history can never be orphaned by an
  accidental hard delete elsewhere — consistent with our system-wide soft-delete philosophy.

---

### 1.6 `doctor_blocked_slots`

```sql
CREATE TABLE doctor_blocked_slots (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id       BIGINT NOT NULL,
    start_datetime  DATETIME NOT NULL,
    end_datetime    DATETIME NOT NULL,
    reason          VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_blocked_slot_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_blocked_range
        CHECK (end_datetime > start_datetime)
);
```

**Design notes:**
- Supports Doctor blocking off vacation/lunch/conference time. The Service layer checks
  this table against existing `appointments` to generate a warning (e.g., "You have 2
  patients in this window") before confirming a block.
- `ON DELETE CASCADE` here (unlike `appointments`) is intentional — blocked time slots have
  no standalone value once a doctor is removed; they aren't part of the clinical/audit
  history we're protecting elsewhere.

---

### Entity Relationship Summary

```
specialties (1) ──< (many) doctors
doctors     (1) ──< (many) patients          [via assigned_doctor_id]
doctors     (1) ──< (many) appointments
patients    (1) ──< (many) appointments
doctors     (1) ──< (many) doctor_blocked_slots
```

---

## 2. MongoDB Collection Design

### Collection: `prescriptions`

**Why MongoDB instead of MySQL:** Prescriptions have a naturally nested, variable-length
structure (a patient may be prescribed 1 medicine or 5), and the clinical `note`/`diagnosis`
fields are free-text and schema-flexible. Forcing this into a relational structure would
require an extra `medicines` join table for no real benefit, whereas MongoDB's document
model represents it naturally in a single record.

```json
{
  "_id": "ObjectId('665f1a2b3c4d5e6f7a8b9c0d')",
  "appointmentId": 10234,
  "doctorId": 15,
  "patientId": 582,
  "patient": {
    "firstName": "Maria",
    "lastName": "Gomez",
    "contact": "+1-555-0147"
  },
  "medicines": [
    {
      "name": "Amoxicillin",
      "dosage": "500mg",
      "duration": "7 days"
    },
    {
      "name": "Ibuprofen",
      "dosage": "200mg",
      "duration": "As needed, max 3x/day"
    }
  ],
  "diagnosis": "Mild upper respiratory infection",
  "note": "Patient reports mild penicillin sensitivity in the past; monitor for reaction. Advised increased fluid intake.",
  "createdAt": "2025-03-01T10:32:00Z"
}
```

**Field-by-field design notes:**
- `doctorId` is **permanent** — always the original author, even if the patient is later
  reassigned to a different doctor (authorship is never transferred).
- `patientId` is a reference to the MySQL `patients` table — used for reliable lookups/joins,
  independent of the embedded display fields below it.
- `patient` (embedded object) is duplicated here for fast display without an extra MySQL
  lookup; `patientId` above remains the source of truth if this ever goes stale.
- `medicines` is an array to support zero, one, or many medicines per prescription,
  satisfying the "could have a list of medicines" requirement.
- `note` doubles as general clinical history/observations (e.g., allergies), per the
  decision to avoid a separate Medical History collection and keep the model simple.

**Additional design notes:**
- **No "current doctor" field is stored on the prescription itself.** Visibility is
  determined dynamically: a Doctor can view this document only if
  `patients.assigned_doctor_id` (in MySQL) currently equals their own ID — enforced at the
  Service layer, not inside the document.
- **Edit/Delete restriction:** application logic only allows mutation of a prescription
  where `doctorId` matches the logged-in doctor. A newly assigned doctor must create a
  **new** document rather than editing someone else's — this collection is treated as an
  **append-only clinical ledger** once authored.
- Indexing recommendation: create a compound index on `{ patientId: 1, createdAt: -1 }` to
  efficiently support the Patient's "view full history, most recent first" view and the
  Doctor's equivalent lookup, including the "most recent = Current badge" display logic.

---

## 3. Cross-Database Relationship Note

Since `patientId` and `doctorId` inside MongoDB documents reference primary keys in MySQL
tables, **referential integrity across the two databases is not enforced by either database
engine** — this is a known trade-off of polyglot persistence. It is the responsibility of
the **Service layer** to validate that referenced IDs exist before writing to MongoDB (e.g.,
confirming `patientId` exists in the `patients` table before saving a new prescription).