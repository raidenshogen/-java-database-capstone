# Smart Clinic Management System — Database Schema Design

This document defines the database schema for the Smart Clinic Management System, using
**MySQL** for structured, relational data (Admin, Doctor, Patient, Appointment records) and
**MongoDB** for flexible, document-based data (Prescriptions). Design decisions are
justified based on the business rules established during requirements analysis (user
stories), and reflect core relational database concepts (keys, normalization, indexing,
relationship types, transactions, and concurrency).

---

## MySQL Database Design

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

### Table: `admin`

```sql
CREATE TABLE admin (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

- `id`: Primary Key, Auto Increment
- `username`, `email`: UNIQUE, NOT NULL
- `password_hash`: NOT NULL — passwords are never stored in plain text; hashed via Spring Security (BCrypt)
- Only one row is expected to ever exist in this table (seeded at setup, not self-registered)
- Email format validation (e.g., regex for valid `@` structure) is handled in application code (Service layer / DTO validation), not enforced at the database level

---

### Table: `specialties`

```sql
CREATE TABLE specialties (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(100) NOT NULL UNIQUE
);
```

- `id`: Primary Key, Auto Increment
- `name`: NOT NULL, UNIQUE

**Why this table exists (Normalization):** Specialty names could have been stored directly
as a VARCHAR column on `doctors`. Instead, they're extracted into their own table — a
classic example of **normalization** (specifically, avoiding repeating the same text value
"Cardiology" across hundreds of doctor rows). This:
- Prevents inconsistent spelling/typos of the same specialty across rows
- Lets Admin add/rename a department in one place, instantly reflected everywhere via the FK
- Directly powers the Patient's department-selection step when booking, and the Admin's
  "patients per specialty" statistics view

---

### Table: `doctors`

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

- `id`: Primary Key, Auto Increment
- `specialty_id`: Foreign Key → `specialties(id)`
- `email`: UNIQUE, NOT NULL (format validated in application code, not the database)
- `is_active`: implements **soft delete** — deactivated doctors are hidden from active
  listings but never physically removed, preserving appointment/prescription history
- `working_hours_start/end` + `slot_duration_minutes`: used to **derive** available booking
  slots dynamically. There is intentionally **no** `max_patients_per_day` column, since that
  number is calculated, not stored, avoiding two competing sources of truth
- `ON DELETE RESTRICT` on the specialty FK prevents accidentally deleting a specialty that
  still has doctors assigned to it

**Think deeper — "Should each doctor have their own available time slots?"**
Yes — answered via `working_hours_start/end` + `slot_duration_minutes` per doctor row,
rather than a single clinic-wide schedule. This supports doctors with different consultation
lengths (e.g., a 20-minute slot doctor vs. a 45-minute slot doctor).

**Index recommendation:** `email` is already indexed implicitly via its UNIQUE constraint.
No additional index needed here since lookups are primarily by `id` (PK, auto-indexed) or
`specialty_id` (FK, auto-indexed by InnoDB).

---

### Table: `patients`

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

- `id`: Primary Key, Auto Increment
- `assigned_doctor_id`: Foreign Key → `doctors(id)`, nullable
- `email`: UNIQUE, NOT NULL
- `phone`: NOT NULL (format validated in application code — e.g., regex for digit count/country code — not enforced at the database level)

**Think deeper — "What happens if a patient is deleted? Should appointments also be deleted?"**
**No.** Patients are never hard-deleted — `is_active` implements a soft delete. This
preserves the patient's full appointment and prescription history for audit/legal/continuity
purposes, which was an explicit requirement during design (a patient "could have a problem"
later, and the clinic wants to retain their history).

- `assigned_doctor_id` implements the **single-field doctor assignment model** — this is the
  field Admin updates when reassigning a patient to a new doctor. A separate "doctor history"
  table was deliberately avoided, since the `prescriptions` collection in MongoDB already
  preserves authorship history per prescription — adding a second history table would create
  duplicate, driftable sources of truth.
- `ON DELETE SET NULL` — if a doctor record were ever hard-deleted (not expected under the
  soft-delete policy, but included as a safety net), affected patients become unassigned
  rather than causing a referential integrity failure.

**Index recommendation:** add an index on `assigned_doctor_id` since Doctor dashboards will
frequently query "all patients where assigned_doctor_id = me."

```sql
CREATE INDEX idx_patients_assigned_doctor ON patients(assigned_doctor_id);
```

---

### Table: `appointments`

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

- `id`: Primary Key, Auto Increment
- `doctor_id`: Foreign Key → `doctors(id)`, NOT NULL
- `patient_id`: Foreign Key → `patients(id)`, NOT NULL
- `appointment_date` + `appointment_time`: NOT NULL
- `status`: NOT NULL, defaults to `SCHEDULED`

**Why ENUM instead of the INT (0/1/2) pattern:** A common alternative is to encode status as
a plain integer (`0 = Scheduled, 1 = Completed, 2 = Cancelled`). This project intentionally
uses MySQL's `ENUM` type instead, because:
- It's self-documenting — `status = 'CANCELLED'` is instantly readable in queries and logs,
  with no lookup table or code comment needed to decode a magic number
- It still stores internally as a compact integer under the hood, so there's no real
  performance trade-off versus plain `INT`
- It also includes a fourth state (`NO_SHOW`), which the simpler 0/1/2 example doesn't account for

**Relationship type — explicitly labeled:** This table is a classic resolved **many-to-many**
relationship. A single Patient can have many Appointments across different Doctors/times,
and a single Doctor can have many Appointments across many different Patients — Patient ↔
Doctor is fundamentally many-to-many. The `appointments` table is the **junction table** that
resolves this, while also carrying its own meaningful attributes (`appointment_date`,
`appointment_time`, `status`) beyond just the two foreign keys.

**Think deeper — "Should a doctor be allowed to have overlapping appointments?"**
No. However, **no UNIQUE constraint exists** on `(doctor_id, appointment_date, 
appointment_time)` — this is a deliberate design decision:

Since this system uses **soft deletes for cancellations** (a cancelled appointment's row is
never removed, only its `status` changes to `CANCELLED`), a plain UNIQUE constraint would
cause a **cancelled** row to permanently "occupy" that doctor/date/time slot, blocking any
future patient from booking that now-free slot.

**Chosen solution:** Double-booking prevention is enforced in the **Service layer**. Before
inserting a new appointment, the application runs:

```sql
SELECT * FROM appointments
WHERE doctor_id = ? AND appointment_date = ? AND appointment_time = ?
AND status = 'SCHEDULED';
```

If a matching row exists, the booking is rejected; otherwise, it's allowed — even if a
`CANCELLED` row exists at that exact slot. See the **Concurrency** section below for how
this check behaves safely under simultaneous bookings.

- `ON DELETE RESTRICT` on both FKs ensures appointment history can never be orphaned by an
  accidental hard delete elsewhere, consistent with the system-wide soft-delete philosophy.

**Index recommendation:** this table will be queried constantly to check doctor availability.
Add a composite index:

```sql
CREATE INDEX idx_appointments_doctor_date ON appointments(doctor_id, appointment_date, status);
```

This avoids a full table scan every time the system checks "is this doctor free on this date?" — directly relevant as the table grows into thousands of rows.

---

### Table: `doctor_blocked_slots`

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

- `id`: Primary Key, Auto Increment
- `doctor_id`: Foreign Key → `doctors(id)`
- Supports a doctor blocking off vacation/lunch/conference time. The Service layer checks
  this table against existing `appointments` to generate a warning before confirming a block.
- `ON DELETE CASCADE` here (unlike `appointments`) is intentional — blocked time slots have
  no standalone historical value once a doctor is removed.

---

### Entity Relationship Summary

```
specialties (1) ──< (many) doctors                     [one-to-many]
doctors     (1) ──< (many) patients                     [one-to-many, via assigned_doctor_id]
patients    (many) ──< appointments >── (many) doctors  [many-to-many, resolved by appointments]
doctors     (1) ──< (many) doctor_blocked_slots          [one-to-many]
```

*(No one-to-one relationships exist in this schema — every entity naturally has multiple
related records, so a 1:1 table split was not needed for this system's scope.)*

---

### DML Examples (Data Manipulation Language)

The tables above are defined using DDL (`CREATE TABLE`). Below are representative DML
statements showing how the application actually interacts with this data day-to-day.

**Booking a new appointment (ties to US-PAT-04):**
```sql
INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, status)
VALUES (582, 15, '2025-03-10', '10:00:00', 'SCHEDULED');
```

**Doctor marking a visit as completed (ties to US-DOC-04):**
```sql
UPDATE appointments
SET status = 'COMPLETED'
WHERE id = 10234;
```

**Patient cancelling their own appointment (ties to US-PAT-06):**
```sql
UPDATE appointments
SET status = 'CANCELLED'
WHERE id = 10234 AND patient_id = 582;
```

**Admin querying patient count per specialty (ties to US-ADM-10 statistics view):**
```sql
SELECT s.name AS specialty, COUNT(DISTINCT p.id) AS patient_count
FROM specialties s
JOIN doctors d ON d.specialty_id = s.id
JOIN patients p ON p.assigned_doctor_id = d.id
WHERE p.is_active = TRUE
GROUP BY s.name;
```

---

### Transactions

Booking an appointment conceptually involves two steps: **(1)** check that the slot is still
free, and **(2)** insert the new row. These two steps must be treated as a single atomic
unit — if the check succeeds but the insert fails (or vice versa), the system could end up
in an inconsistent state.

```sql
START TRANSACTION;

SELECT * FROM appointments
WHERE doctor_id = 15 AND appointment_date = '2025-03-10' AND appointment_time = '10:00:00'
AND status = 'SCHEDULED'
FOR UPDATE;  -- locks matching rows until the transaction completes

-- Application logic checks: if no row was returned, proceed to insert
INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, status)
VALUES (582, 15, '2025-03-10', '10:00:00', 'SCHEDULED');

COMMIT;
```

If any step fails, the application issues a `ROLLBACK` instead of `COMMIT`, ensuring no
partial/inconsistent appointment is ever left in the table.

---

### Concurrency Consideration

A realistic risk: **two patients could click "book" for the exact same doctor/date/time
within milliseconds of each other.** Without protection, both could pass the availability
check before either INSERT completes, resulting in an accidental double-booking.

The `FOR UPDATE` row-lock shown in the transaction above mitigates this: the first
transaction to run the `SELECT ... FOR UPDATE` locks that query's matching rows until it
commits, forcing the second concurrent request to wait until the first finishes — at which
point its own check will correctly see the newly inserted `SCHEDULED` row and reject the
duplicate booking.

This is handled at the **Service layer / transaction level** rather than via a database
UNIQUE constraint, for the reasons explained in the `appointments` table section above
(a plain UNIQUE constraint would conflict with the soft-delete/cancellation model).

---

## MongoDB Collection Design

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
- `patientId` is a reference to the MySQL `patients` table — used for reliable lookups/joins.
- `medicines` is an array to support zero, one, or many medicines per prescription.
- `note` doubles as general clinical history/observations (e.g., allergies), avoiding a
  separate Medical History collection and keeping the model simple.

**Think deeper — "Should MongoDB documents include the full patient object or just an ID?"**
**Both, deliberately.** `patientId` is included as the authoritative reference (source of
truth, used for queries like "find all prescriptions for patient X"), while a small embedded
`patient` object (name, contact) is also stored for **fast display** without requiring an
extra round-trip query to MySQL every time a prescription is rendered in the UI. The
trade-off is accepted: if a patient updates their contact info later in MySQL, this embedded
copy could go stale — acceptable here since the `patientId` remains available as the
source of truth for anything requiring up-to-date accuracy.

**Think deeper — "What happens if the schema needs to evolve?"**
MongoDB's schema-less nature directly supports this. For example, if the clinic later wants
to add file attachments (e.g., a scanned lab result) or tags (e.g., `"tags": 
["follow-up-required"]`), these fields can be added to **new** documents immediately, with
no migration required and no impact on existing documents that lack the field. Application
code would simply treat the new field as optional (e.g., default to an empty array if absent).

**Access control notes:**
- A Doctor can view this document only if `patients.assigned_doctor_id` (in MySQL) currently
  equals their own ID — enforced at the Service layer, not inside the document itself.
- Application logic only allows mutation of a prescription where `doctorId` matches the
  logged-in doctor. A newly assigned doctor must create a **new** document rather than
  editing someone else's — this collection is treated as an **append-only clinical ledger**
  once authored.

**Index recommendation:**
```javascript
db.prescriptions.createIndex({ patientId: 1, createdAt: -1 })
```
This supports the Patient's "view full history, most recent first" view and the "most
recent = Current badge" display logic efficiently.

---

## Cross-Database Relationship Note

Since `patientId` and `doctorId` inside MongoDB documents reference primary keys in MySQL
tables, **referential integrity across the two databases is not enforced by either database
engine** — a known trade-off of polyglot persistence. It is the responsibility of the
**Service layer** to validate that referenced IDs exist before writing to MongoDB (e.g.,
confirming `patientId` exists in the `patients` table before saving a new prescription).