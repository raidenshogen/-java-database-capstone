# Smart Clinic Management System — User Stories

This document captures all user stories for the three core roles: **Admin**, **Doctor**,
and **Patient**. Each story follows the standard template with acceptance criteria,
priority, estimated effort, and relevant notes/edge cases.

---

## 🔑 Admin User Stories

---

**Title:**
_As an Admin, I want to log into the system with my credentials, so that I can securely access the admin dashboard._

**Acceptance Criteria:**
1. Given valid credentials, when I submit the login form, then I'm redirected to the Admin Dashboard.
2. Given invalid credentials, then an error is shown and access is denied.
3. Only one Admin account exists in the system (seeded, not self-registered).

**Priority:** High
**Story Points:** 2
**Notes:**
- Foundational story — blocks all other Admin functionality until implemented.
- No "Forgot Password" flow needed for Admin since it's a single seeded account (handled outside the app if ever lost).

---

**Title:**
_As an Admin, I want to log out of the system, so that my session is securely closed._

**Acceptance Criteria:**
1. Given I'm logged in, when I click logout, then my session is invalidated.
2. I am redirected to the login page after logout.

**Priority:** High
**Story Points:** 1
**Notes:**
- Simple session-invalidation logic; same pattern reused across all three roles.

---

**Title:**
_As an Admin, I want to create a new Doctor account and assign their department/specialty during creation, so that doctors are properly categorized from day one._

**Acceptance Criteria:**
1. Given doctor details (name, email, specialty), when submitted, a Doctor account is created in MySQL with that specialty.
2. Specialty is selected from a predefined list of clinic departments.
3. No doctor self-registration is allowed — Admin-only creation.

**Priority:** High
**Story Points:** 3
**Notes:**
- Specialty field directly drives the booking system (department-based appointment selection depends on this).

---

**Title:**
_As an Admin, I want to update a Doctor's profile (including specialty), so that records stay accurate if their role/department changes._

**Acceptance Criteria:**
1. Given a Doctor ID, when I edit their details and save, changes persist across the system.
2. Updated specialty immediately reflects in future patient booking options.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- Changing specialty does not retroactively affect already-booked appointments.

---

**Title:**
_As an Admin, I want to deactivate a Doctor account when they leave the clinic, so that they're hidden from active listings while their appointment/prescription history is preserved._

**Acceptance Criteria:**
1. Given a Doctor ID, a confirmation warning is shown before deactivation.
2. Doctor is marked `inactive`, not physically deleted.
3. Inactive doctors are excluded from active doctor searches but history remains intact.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Soft delete only — system-wide policy, no hard-delete toggle.
- Consider warning Admin if the doctor has upcoming `SCHEDULED` appointments before deactivating.

---

**Title:**
_As an Admin, I want to create a new patient record, so that patients are properly onboarded into the system._

**Acceptance Criteria:**
1. Given required patient details, when submitted, patient is saved and appears in the patient list.
2. Required fields are validated before saving.

**Priority:** High
**Story Points:** 3
**Notes:**
- Admin is the only entry point for patient creation (no public self-registration in current scope).

---

**Title:**
_As an Admin, I want to edit a patient's information, so that records remain accurate and current._

**Acceptance Criteria:**
1. Given a patient ID, when fields are updated and saved, changes persist.
2. Invalid data is rejected with a clear error message.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- None.

---

**Title:**
_As an Admin, I want to deactivate a patient record, so that outdated entries are hidden from active lists while their clinical history remains intact._

**Acceptance Criteria:**
1. Given a patient ID, a confirmation warning shows if they have existing appointments/prescriptions.
2. On confirmation, patient is marked `inactive` — not physically deleted.
3. Inactive patients don't appear in active searches but remain queryable for history/audit.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Soft delete only, consistent with Doctor deactivation policy.

---

**Title:**
_As an Admin, I want to trigger a password reset for a Doctor when requested, so that they can regain access if locked out._

**Acceptance Criteria:**
1. Given a Doctor's registered email, when Admin initiates reset, a system-generated temporary password is sent to that email.
2. Doctor logs in with it and can set a new password afterward.
3. Admin never sees or manually sets the password.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Requires email-sending capability (e.g., Spring Mail) — shared technical dependency with Doctor-initiated appointment cancellation notifications.

---

**Title:**
_As an Admin, I want to view a donut chart showing the percentage of patients per clinic specialty, with the total patient count shown in the center, so that I can make informed decisions about expanding or improving specific services._

**Acceptance Criteria:**
1. Given existing appointment/specialty data, the statistics view renders a donut chart with % breakdown per specialty.
2. Total patient count is displayed in the center of the donut.
3. Chart data reflects current active patients (not necessarily real-time).

**Priority:** Low
**Story Points:** 5
**Notes:**
- Deferred to post-MVP — core CRUD features take priority first.

---

**Title:**
_As an Admin, I want to update which Doctor is currently assigned to a Patient, so that care continues if the original doctor becomes unavailable._

**Acceptance Criteria:**
1. Admin updates `Patient.assignedDoctorId` to a new Doctor.
2. The new doctor immediately gains full view access to the patient's prescription/appointment history.
3. The previous doctor loses future visibility into that patient's records.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Original doctor remains permanently credited as author on their own past prescriptions — reassignment does not alter authorship.

---

## 🩺 Doctor User Stories

---

**Title:**
_As a Doctor, I want to set and update my working hours and slot duration, so that patients can only book appointments during times I'm actually available._

**Acceptance Criteria:**
1. I can set `workingHours` (start/end) and `slotDurationMinutes`.
2. Changes apply to future availability only; existing booked appointments are unaffected.
3. Available slots are auto-derived from these values — no manual "max patients" cap needed.

**Priority:** High
**Story Points:** 3
**Notes:**
- Foundational for the entire patient booking engine — must be built before booking features can function.

---

**Title:**
_As a Doctor, I want to mark specific time blocks as unavailable, so that patients cannot book during those times._

**Acceptance Criteria:**
1. If the blocked range conflicts with existing `SCHEDULED` appointments, I'm warned with the list of affected appointments before confirming.
2. I can proceed anyway (handling affected appointments manually) or cancel the block action.

**Priority:** Medium
**Story Points:** 5
**Notes:**
- Requires conflict-detection logic against existing appointments before allowing the block to be saved.

---

**Title:**
_As a Doctor, I want to see my scheduled appointments for the day, so that I know who I'm seeing and when._

**Acceptance Criteria:**
1. Dashboard shows only `SCHEDULED` appointments, sorted by time, for patients where `assignedDoctorId = me`.
2. Cancelled appointments are hidden from this view.

**Priority:** High
**Story Points:** 2
**Notes:**
- No notifications pushed to Doctor — appointments simply appear on this dashboard view.

---

**Title:**
_As a Doctor, I want to manually mark an appointment as completed once the visit actually finishes, so that records reflect real outcomes, not assumed time slots._

**Acceptance Criteria:**
1. Given a `SCHEDULED` appointment, I can mark it `COMPLETED` at any time.
2. This is a deliberate, manual click — never automatically triggered by the clock.

**Priority:** High
**Story Points:** 1
**Notes:**
- Consistent pattern with `NO_SHOW` and `CANCELLED` — all terminal statuses require explicit action, never automatic time-based changes.

---

**Title:**
_As a Doctor, I want to mark an appointment as NO_SHOW after the scheduled time passes, so that records reflect the patient never arrived._

**Acceptance Criteria:**
1. Given a `SCHEDULED` appointment whose time has passed, I can mark it `NO_SHOW`.
2. This is a manual action only — no automatic detection or live queue reordering.

**Priority:** Medium
**Story Points:** 1
**Notes:**
- Live queue management (e.g., skipping and re-inserting late patients) is explicitly deferred to a future epic — out of scope for MVP.

---

**Title:**
_As a Doctor, I want to cancel an appointment on my end, so that the patient is informed immediately and the slot is freed._

**Acceptance Criteria:**
1. Status becomes `CANCELLED`; the slot reopens immediately for other patients.
2. Patient receives a notification (e.g., email) right away — no delay tolerated.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Requires email-sending capability — new technical dependency not previously needed elsewhere (patient-initiated cancellations do not send notifications).

---

**Title:**
_As a Doctor, I want to create a prescription for a patient during or after their appointment, so that their treatment is documented._

**Acceptance Criteria:**
1. Prescription includes patient reference, a list of medicines (name, dosage, duration), diagnosis, and a free-text note.
2. The note field may also be used for general clinical observations (e.g., allergies, history) even without new medicine.
3. Saved in MongoDB, permanently linked to me (`doctorId`) and the related appointment.

**Priority:** High
**Story Points:** 5
**Notes:**
- No separate "Medical History" entity — reuses the existing `note` field by design decision, to keep the model simple.

---

**Title:**
_As a Doctor, I want to view the full prescription history of a patient currently assigned to me, so that I have complete context for their treatment, even across previous doctors._

**Acceptance Criteria:**
1. Full history is visible only if `patient.assignedDoctorId == me`.
2. Access is denied otherwise — no exceptions, including urgent/emergency cases (handled administratively, not in-system).

**Priority:** High
**Story Points:** 3
**Notes:**
- History includes prescriptions written by previously assigned doctors, not just my own.

---

**Title:**
_As a Doctor, I want to edit or delete only the prescriptions I personally created, so that clinical accountability stays accurate._

**Acceptance Criteria:**
1. I can edit/delete a prescription only where `prescription.doctorId == me`.
2. For patients reassigned to me, I can only create new prescription entries — never alter a previous doctor's work.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- Enforces permanent authorship/accountability even after patient reassignment.

---

**Title:**
_As a Doctor, I want to log in and out securely, so that my schedule and patient data stay protected._

**Acceptance Criteria:**
1. Given valid credentials, I'm redirected to my Doctor Dashboard.
2. Given invalid credentials, an error is shown and access is denied.
3. Logging out invalidates my session and redirects me to the login page.

**Priority:** High
**Story Points:** 2
**Notes:**
- Standard pattern shared across all roles.

---

**Title:**
_As a Doctor, I want to change my password after Admin creates my account (or after a reset), so that I control my own access credentials._

**Acceptance Criteria:**
1. Given I'm logged in, I can submit a new password which replaces the old one.
2. The old password is invalidated immediately after the change.

**Priority:** Medium
**Story Points:** 1
**Notes:**
- Self-service password change does not affect Admin's ability to manage my account/records.

---

## 🧑‍⚕️ Patient User Stories

---

**Title:**
_As a Patient, I want to log into the system with my credentials, so that I can securely access my appointments and records._

**Acceptance Criteria:**
1. Given valid credentials, I'm redirected to my Patient dashboard.
2. Given invalid credentials, an error is shown and access is denied.

**Priority:** High
**Story Points:** 2
**Notes:**
- Standard pattern shared across all roles.

---

**Title:**
_As a Patient, I want to log out of the system, so that my session is securely closed._

**Acceptance Criteria:**
1. Given I'm logged in, clicking logout invalidates my session.
2. I am redirected to the login page after logout.

**Priority:** High
**Story Points:** 1
**Notes:**
- None.

---

**Title:**
_As a Patient, I want to reset my own password directly via email, so that I can regain access without needing Admin involvement._

**Acceptance Criteria:**
1. Given my registered email, requesting "Forgot Password" sends a reset link/token to that email.
2. I click the link, set a new password, and can log in immediately.
3. No Admin action is required anywhere in this flow.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Unlike Doctors, Patients reset their own password entirely independently — no Admin involvement at all.

---

**Title:**
_As a Patient, I want to select a department and an available time slot on a future date, so that I can schedule a visit with an available doctor._

**Acceptance Criteria:**
1. Given a selected department and a date at least tomorrow (no same-day booking), the system shows open time slots derived from each doctor's `workingHours` and `slotDurationMinutes`.
2. Upon selecting a slot, the system auto-assigns the first available doctor in that department.
3. I cannot select a specific doctor directly — only a department.
4. If no slots are available that day, I see a clear message and must choose another date myself (no auto-advancing to the next day).

**Priority:** High
**Story Points:** 8
**Notes:**
- Core booking engine — highest complexity story in the system; combines department filtering, slot derivation, and auto-assignment logic.
- Doctor cannot be chosen directly, by design, to prevent patients treating the clinic like private practice selection.

---

**Title:**
_As a Patient, I want to be prevented from double-booking the same department or overlapping time slots, so that my schedule and the clinic's resources stay conflict-free._

**Acceptance Criteria:**
1. Given an active (`SCHEDULED`) appointment in Department X, a new booking attempt in Department X is blocked until the existing one is completed or cancelled.
2. Given an active appointment at Time T, any new booking overlapping Time T is blocked, regardless of department.
3. Appointments in different departments at non-overlapping times are allowed.

**Priority:** High
**Story Points:** 5
**Notes:**
- Validation logic must run at booking time, not just at the UI level, to prevent race conditions.

---

**Title:**
_As a Patient, I want to cancel my own upcoming appointment, so that I can free up my schedule and the slot for someone else._

**Acceptance Criteria:**
1. Given an upcoming `SCHEDULED` appointment, cancelling changes its status to `CANCELLED` (soft, never deleted).
2. The time slot becomes immediately available for any other patient to book — first-come, first-served, no waitlist.
3. Cancelled appointments no longer appear on the Doctor's active dashboard.

**Priority:** High
**Story Points:** 3
**Notes:**
- No notification is sent to anyone on patient-initiated cancellation — this is the reverse of Doctor-initiated cancellation, which does require notifying the patient.

---

**Title:**
_As a Patient, I want to view a list of my upcoming and past appointments, so that I can keep track of my visits._

**Acceptance Criteria:**
1. I can see all my appointments with status (`SCHEDULED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`).
2. The list is sortable/filterable by date or status.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- None.

---

**Title:**
_As a Patient, I want to view my full prescription history with the most recent one clearly marked as current, so that I know what treatment is active now while still having access to my past records._

**Acceptance Criteria:**
1. I can view all prescriptions ever issued to me, sorted by date (most recent first).
2. The most recent prescription is visually marked as "Current" (e.g., badge/highlight).
3. Older prescriptions remain visible but are visually de-emphasized (e.g., greyed out).
4. I can see medicines, dosage, duration, diagnosis, and notes for each prescription — same content a Doctor sees, no restricted fields.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Full history is intentionally preserved rather than hidden, to protect patient rights to their own medical records and avoid hiding unrelated long-term medications.
- "Current" badge is applied to the single most recent prescription overall (not scoped per department) — simplest interpretation for MVP.