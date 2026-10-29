# Smart Clinic Management System — User Stories

This document captures all user stories for the three core roles in the Smart Clinic
Management System: **Admin**, **Doctor**, and **Patient**. Each story follows the standard
format (`As a [role], I want to [action], so that [benefit]`) with clear acceptance criteria
to guide implementation and testing.

---

## 🔑 Admin User Stories

### US-ADM-01: Admin Login
**As an** Admin, **I want to** log into the system with my credentials, **so that** I can securely access the admin dashboard.

**Acceptance Criteria:**
- Given valid credentials, when I submit the login form, then I'm redirected to the Admin Dashboard.
- Given invalid credentials, then an error is shown and access is denied.
- Only one Admin account exists (seeded, not self-registered).

---

### US-ADM-02: Admin Logout
**As an** Admin, **I want to** log out of the system, **so that** my session is securely closed.

**Acceptance Criteria:**
- Given I'm logged in, when I click logout, then my session is invalidated and I'm redirected to the login page.

---

### US-ADM-03: Create a Doctor Account with Specialty
**As an** Admin, **I want to** create a new Doctor account and assign their department/specialty during creation, **so that** doctors are properly categorized from day one.

**Acceptance Criteria:**
- Given doctor details (name, email, specialty), when submitted, a Doctor account is created in MySQL with that specialty.
- Specialty is selected from a predefined list of clinic departments.
- No doctor self-registration — Admin-only creation.

---

### US-ADM-04: Update a Doctor's Info/Specialty
**As an** Admin, **I want to** update a Doctor's profile (including specialty), **so that** records stay accurate if their role/department changes.

**Acceptance Criteria:**
- Given a Doctor ID, when I edit their details and save, changes persist across the system.

---

### US-ADM-05: Deactivate a Doctor Account (Soft Delete)
**As an** Admin, **I want to** deactivate a Doctor account when they leave the clinic, **so that** they're hidden from active listings while their appointment/prescription history is preserved.

**Acceptance Criteria:**
- Given a Doctor ID, a confirmation warning is shown before deactivation.
- Doctor is marked `inactive`, not physically deleted.
- Inactive doctors are excluded from active doctor searches but history remains intact.

---

### US-ADM-06: Create a Patient Record
**As an** Admin, **I want to** create a new patient record, **so that** patients are properly onboarded into the system.

**Acceptance Criteria:**
- Given required patient details, when submitted, patient is saved and appears in the patient list.
- Required fields are validated before saving.

---

### US-ADM-07: Update a Patient Record
**As an** Admin, **I want to** edit a patient's information, **so that** records remain accurate and current.

**Acceptance Criteria:**
- Given a patient ID, when fields are updated and saved, changes persist.
- Invalid data is rejected with a clear error message.

---

### US-ADM-08: Deactivate a Patient Record (Soft Delete)
**As an** Admin, **I want to** deactivate a patient record, **so that** outdated entries are hidden from active lists while their clinical history remains intact.

**Acceptance Criteria:**
- Given a patient ID, a confirmation warning shows if they have existing appointments/prescriptions.
- On confirmation, patient is marked `inactive` — not physically deleted.
- Inactive patients don't appear in active searches but remain queryable for history/audit.

---

### US-ADM-09: Reset a Doctor's Password
**As an** Admin, **I want to** trigger a password reset for a Doctor when requested, **so that** they can regain access if locked out.

**Acceptance Criteria:**
- Given a Doctor's registered email, when Admin initiates reset, a system-generated temporary password is sent to that email.
- Doctor logs in with it and can set a new password afterward.
- Admin never sees/sets the password directly.

---

### US-ADM-10: View Patient Distribution by Specialty (Donut Chart)
**As an** Admin, **I want to** view a donut chart showing the percentage of patients per clinic specialty, with the total patient count shown in the center, **so that** I can make informed decisions about expanding or improving specific services.

**Acceptance Criteria:**
- Given existing appointment/specialty data, the statistics view renders a donut chart with % breakdown per specialty.
- Total patient count is displayed in the center of the donut.
- **Status: Deferred to post-MVP** (core CRUD features are built first).

---

### US-ADM-11: Reassign a Patient to a Different Doctor
**As an** Admin, **I want to** update which Doctor is currently assigned to a Patient, **so that** care continues if the original doctor becomes unavailable.

**Acceptance Criteria:**
- Admin updates `Patient.assignedDoctorId` to a new Doctor.
- The new doctor immediately gains full view access to the patient's prescription/appointment history.
- The previous doctor loses future visibility (but remains permanently credited as author on their own past prescriptions).

---

## 🩺 Doctor User Stories

### Availability & Schedule

### US-DOC-01: Set/Update Working Hours
**As a** Doctor, **I want to** set and update my working hours and slot duration, **so that** patients can only book appointments during times I'm actually available.

**Acceptance Criteria:**
- I can set `workingHours` (start/end) and `slotDurationMinutes`.
- Changes apply to future availability only; existing booked appointments are unaffected.

---

### US-DOC-02: Block Off Unavailable Time
**As a** Doctor, **I want to** mark specific time blocks as unavailable, **so that** patients cannot book during those times.

**Acceptance Criteria:**
- If the blocked range conflicts with existing `SCHEDULED` appointments, I'm warned with the list of affected appointments before confirming.
- I can proceed anyway (and manually handle those appointments) or cancel the block action.

---

### Appointment Management

### US-DOC-03: View Daily Appointment Schedule
**As a** Doctor, **I want to** see my scheduled appointments for the day, **so that** I know who I'm seeing and when.

**Acceptance Criteria:**
- Dashboard shows only `SCHEDULED` appointments, sorted by time, for patients currently `assignedDoctorId = me`.
- Cancelled appointments are hidden.

---

### US-DOC-04: Mark Appointment as Completed
**As a** Doctor, **I want to** manually mark an appointment as completed once the visit actually finishes, **so that** records reflect real outcomes, not assumed time slots.

**Acceptance Criteria:**
- Given a `SCHEDULED` appointment, I can mark it `COMPLETED` at any time — a deliberate click, never automatic.

---

### US-DOC-05: Mark Appointment as No-Show
**As a** Doctor, **I want to** mark an appointment as `NO_SHOW` after the scheduled time passes, **so that** records reflect the patient never arrived.

**Acceptance Criteria:**
- Manual action only; no automatic detection or live queue reordering.

---

### US-DOC-06: Cancel an Appointment (Doctor-Initiated)
**As a** Doctor, **I want to** cancel an appointment on my end, **so that** the patient is informed immediately and the slot is freed.

**Acceptance Criteria:**
- Status becomes `CANCELLED`; slot reopens immediately.
- Patient receives a **notification** (e.g., email) right away — no delay tolerated.

---

### Prescriptions

### US-DOC-07: Create a Prescription
**As a** Doctor, **I want to** create a prescription for a patient during or after their appointment, **so that** their treatment (and any clinical notes/history) is documented.

**Acceptance Criteria:**
- Includes patient reference, list of medicines (name, dosage, duration), diagnosis, and free-text note.
- Note field may also be used for general observations (allergies, history) even without new medicine — no separate entity needed.
- Saved in MongoDB, permanently linked to me (`doctorId`) and the appointment.

---

### US-DOC-08: View Patient Prescription History
**As a** Doctor, **I want to** view the full prescription history of a patient currently assigned to me, **so that** I have complete context for their treatment, even across previous doctors.

**Acceptance Criteria:**
- Full history visible only if `patient.assignedDoctorId == me`.
- Access denied otherwise — no exceptions, including "urgent" cases (handled physically/administratively, not in-system).

---

### US-DOC-09: Edit/Delete My Own Prescriptions Only
**As a** Doctor, **I want to** edit or delete only prescriptions I personally created, **so that** clinical accountability stays accurate.

**Acceptance Criteria:**
- Can edit/delete only where `prescription.doctorId == me`.
- For patients reassigned to me, I can only **create new** entries — never alter a previous doctor's work.

---

### Account

### US-DOC-10: Doctor Login/Logout
**As a** Doctor, **I want to** log in and out securely, **so that** my schedule and patient data stay protected.

**Acceptance Criteria:**
- Given valid credentials, I'm redirected to my Doctor Dashboard.
- Given invalid credentials, an error is shown and access is denied.
- Logging out invalidates my session and redirects me to the login page.

---

### US-DOC-11: Change My Own Password
**As a** Doctor, **I want to** change my password after Admin creates my account (or after a reset), **so that** I control my own access credentials.

**Acceptance Criteria:**
- Given I'm logged in, I can submit a new password which replaces the old one.
- Old password is invalidated immediately.

---

## 🧑‍⚕️ Patient User Stories

### Account & Access

### US-PAT-01: Patient Login
**As a** Patient, **I want to** log into the system with my credentials, **so that** I can securely access my appointments and records.

**Acceptance Criteria:**
- Given valid credentials, I'm redirected to my Patient dashboard.
- Given invalid credentials, an error is shown and access is denied.

---

### US-PAT-02: Patient Logout
**As a** Patient, **I want to** log out of the system, **so that** my session is securely closed.

**Acceptance Criteria:**
- Given I'm logged in, clicking logout invalidates my session and redirects me to the login page.

---

### US-PAT-03: Self-Service Password Reset
**As a** Patient, **I want to** reset my own password directly via email, **so that** I can regain access without needing Admin involvement.

**Acceptance Criteria:**
- Given my registered email, requesting "Forgot Password" sends a reset link/token to that email.
- I click the link, set a new password, and can log in immediately.
- No Admin action required anywhere in this flow.

---

### Booking Appointments

### US-PAT-04: Book an Appointment by Department & Time Slot
**As a** Patient, **I want to** select a department and an available time slot on a future date, **so that** I can schedule a visit with an available doctor.

**Acceptance Criteria:**
- Given a selected department and a date that is **at least tomorrow** (no same-day booking), the system shows open time slots derived from each doctor's `workingHours` and `slotDurationMinutes`.
- Upon selecting a slot, the system auto-assigns the **first available doctor** in that department for that slot.
- I **cannot** select a specific doctor directly — only a department.
- If no slots are available that day, I see a clear message and must choose another date myself (no auto-advancing to the next day).

---

### US-PAT-05: Prevent Duplicate/Overlapping Appointments
**As a** Patient, **I want to** be prevented from double-booking the same department or overlapping time slots, **so that** my schedule and the clinic's resources stay conflict-free.

**Acceptance Criteria:**
- Given an active (`SCHEDULED`) appointment in Department X, a new booking attempt in Department X is blocked until the existing one is completed or cancelled.
- Given an active appointment at Time T, any new booking overlapping Time T is blocked, regardless of department.
- Appointments in different departments at non-overlapping times are allowed.

---

### Managing Appointments

### US-PAT-06: Cancel an Appointment
**As a** Patient, **I want to** cancel my own upcoming appointment, **so that** I can free up my schedule and the slot for someone else.

**Acceptance Criteria:**
- Given an upcoming `SCHEDULED` appointment, cancelling changes its status to `CANCELLED` (soft, never deleted).
- The time slot becomes immediately available for any other patient to book — first-come, first-served, no waitlist.
- Cancelled appointments no longer appear on the Doctor's active dashboard.
- *Note: if the Patient cancels, no notification is sent — this is the reverse of Doctor-initiated cancellation, which does require notifying the patient.*

---

### US-PAT-07: View My Appointments
**As a** Patient, **I want to** view a list of my upcoming and past appointments, **so that** I can keep track of my visits.

**Acceptance Criteria:**
- I can see all my appointments with status (`SCHEDULED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`).
- List is sortable/filterable by date or status.

---

### US-PAT-08: View My Prescription History
**As a** Patient, **I want to** view my full prescription history with the most recent one clearly marked as current, **so that** I know what treatment is active now while still having access to my past records.

**Acceptance Criteria:**
- I can view all prescriptions ever issued to me, sorted by date (most recent first).
- The most recent prescription is visually marked as **"Current"** (e.g., badge/highlight).
- Older prescriptions are still visible but visually de-emphasized (e.g., greyed out).
- I can see medicines, dosage, duration, diagnosis, and notes for each prescription — same content a Doctor sees, no restricted fields (full transparency on my own medical record).