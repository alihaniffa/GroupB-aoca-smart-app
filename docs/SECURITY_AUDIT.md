# Security Rules Audit Log

Living record of Firebase Realtime Database Security Rules audits for this project.
Each entry checks every read/write path used in the codebase against `database.rules.json`,
confirming coverage and correct scoping. Re-run whenever rules or role logic change
significantly (new roles, new top-level nodes, new permission patterns).

---

## 2026-08-13 — Initial audit

**Trigger:** Activities node permission bug (12/08) — writes were silently failing because
the new `Activities` node had no corresponding rule, and root rules default to deny.

**Scope checked:** UserProfiles, MiniGameScores, HealthSurvey, CognitiveAssessment,
Reminders, Appointments, UserSettings, Activities.

**Result:** Clean. All paths had corresponding, correctly scoped rules. Activities gap
already fixed prior to this audit (owner-only `$uid` rule added, matching the
Reminders/Appointments pattern).

**Auditor:** Ali

---

## 2026-08-28 — Post role-based access merge

**Trigger:** PR #5 (Michelle — doctor/caregiver/patient role flows) and PR #7
(Pierre — chat feature, migration of all rules from Firestore to Realtime Database)
both merged into main within the same session. Two independent contributors modifying
rules in parallel is the exact pattern that caused the 12/08 Activities bug, so a
re-audit was done before building further on top of these changes.

**Scope checked:**
- `UserProfiles/{uid}/assignedDoctorId` and `assignedCaregiverId` (new fields introduced
  by PR #5's doctor-patient/caregiver assignment flow)
- `CaregiverPatients/{caregiverId}/{patientId}` (new top-level node for caregiver
  assignment relationships)
- `chatRooms` (new, from PR #7's chat feature)
- All previously audited nodes, re-checked for regressions

**Findings during PR #7 review (fixed before merge, not carried into this result):**
- `chatRooms` list query had a bypass (`request.query.limit != null`) letting any
  authenticated user enumerate all chat rooms regardless of participation. Fixed by
  scoping caregiver/general access to actual room participation, matching the
  `CaregiverPatients`-scoped pattern used elsewhere in the file.
- `UserProfiles` profile creation only allowed `userType == 'user'` or `'doctor'`,
  which would have blocked caregiver account creation once PR #5 merged. Fixed by
  adding `'caregiver'` to the allowed values.
- `assignedCaregiverId` write rule allowed any caregiver to self-assign to any patient,
  bypassing the doctor-controlled assignment flow (no `newData.val() === auth.uid`-style
  restriction, unlike `assignedDoctorId`). Fixed by restricting writes to doctor accounts
  only.
- Firestore's pre-existing `UserProfiles` collection (separate from the Realtime Database
  one the rest of the app uses) was confirmed to not be written by the app's actual
  sign-up flow. Decision made to consolidate entirely onto Realtime Database rather than
  maintain two profile data sources — all rules and the chat feature were migrated
  accordingly.

**Result after fixes:** Clean. `assignedDoctorId`/`assignedCaregiverId` correctly covered
under `UserProfiles/$uid` with appropriate write restrictions. `CaregiverPatients` read
scoped to the caregiver themself or any doctor; write restricted to doctor only, matching
the intended assignment flow. `chatRooms` correctly scoped post-fix. No further gaps
found across any node.

**Note (not a bug, worth tracking):** `CaregiverPatients/$caregiverId/.read` allows any
doctor to read any caregiver's patient list, not just patients the doctor themself is
assigned to. Likely intentional given doctors currently also act as admin (per Dr Fareed's
direction, 13/08), but worth revisiting if admin/doctor scope narrows in a future
iteration.

**Auditor:** Ali

---

## 2026-09-27 — Security Rules Hardening & Unintended Access Mitigation

**Trigger:** Security review and user request to ensure database rules are robust and secure, blocking unintended access.

**Scope checked:**
- `UserProfiles` collection read & individual profile scoping
- `UserProfiles/{uid}` privilege escalation prevention & assignment tampering protection
- `UserSettings` privacy scoping
- `chatRooms` privacy scoping & participant-only message validation
- Role separation for `doctor`, `caregiver`, and `user` across all nodes

**Findings and Fixes:**
1. **Critical PII Data Leakage (`UserProfiles/.read: "auth != null"`):**
   - *Issue:* Root `UserProfiles` node had `.read: "auth != null"`, allowing any signed-in patient or malicious authenticated user to dump all user records (full names, emails, phone numbers, home addresses, emergency contacts) and completely bypassing child `$uid` rules.
   - *Fix:* Restricted `UserProfiles/.read` strictly to doctors (`root.child('UserProfiles').child(auth.uid).child('userType').val() === 'doctor'`) or indexed public doctor discovery queries (`query.orderByChild === 'userType' && query.equalTo === 'doctor'`). Regular patients and caregivers cannot dump the user directory and can only read individual profiles permitted under `$uid`.
2. **Privilege Escalation on `UserProfiles/{uid}`:**
   - *Issue:* Users could update their own profile and change `userType` to `'doctor'`, granting themselves full clinical data access.
   - *Fix:* Added `.validate` rule prohibiting existing users from altering their `userType` (`!data.exists() || data.child('userType').val() === newData.child('userType').val() || root.child('UserProfiles').child(auth.uid).child('userType').val() === 'doctor'`). Also validated that `userType` must be one of `'user'`, `'doctor'`, or `'caregiver'`.
3. **Patient Assignment Tampering:**
   - *Issue:* A user writing to `$uid` could arbitrarily modify `assignedDoctorId` or `assignedCaregiverId`.
   - *Fix:* Added `.validate` rules to `assignedDoctorId` and `assignedCaregiverId` enforcing that values cannot be modified on existing profiles unless written by a doctor account. Removed caregiver write permission on `assignedDoctorId`.
4. **Chat Room Eavesdropping by Doctors:**
   - *Issue:* `chatRooms` and `messages` allowed any doctor account to read or write to any chat room, even rooms they were not invited to or participating in.
   - *Fix:* Restricted chat room reads and writes strictly to participating users (`data.child('participants').child(auth.uid).val() === true`). Doctors only have access to rooms where they are legitimate participants.
5. **Chat Message Payload Abuse & Spoofing:**
   - *Issue:* Messages did not enforce data types, length limits, or timestamp constraints.
   - *Fix:* Added validations for string types, non-empty text up to 5,000 characters, valid numeric timestamp, and sender ID match (`newData.child('senderId').val() === auth.uid`).
6. **UserSettings Exposure:**
   - *Issue:* `UserSettings` permitted doctors and caregivers to read and write a patient's personal display and device settings.
   - *Fix:* Scoped `UserSettings` strictly to the account owner (`auth.uid === $uid`).
7. **CaregiverPatients Data Integrity:**
   - *Issue:* Caregiver-patient mappings lacked value validation.
   - *Fix:* Added `.validate` ensuring child values are boolean (`true`) or null on removal.

**Result after fixes:** Verified robust. All unintended data dump vectors, privilege escalations, unauthorized assignment alterations, and eavesdropping paths are blocked.

**Auditor:** Pierre
