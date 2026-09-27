# Dementia SmartApp — Handover Documentation

This document is for whoever picks up this project next, whether that's a future capstone group or the client's own developers. It covers what you need to get the project running, what state it's in, and what to watch out for.

## Project Overview

Cross-platform mobile app (Android and iOS) supporting people living with dementia, their caregivers, and their treating clinicians. Built with Kotlin Multiplatform and Compose Multiplatform, backed by Firebase.

Three user roles: Patient, Doctor/Admin, and Caregiver. Doctors assign patients to themselves and assign caregivers to patients; caregivers act on behalf of their assigned patient; patients manage their own assessments, reminders, appointments and chat.

This is the second handover of this project. It was originally built by a prior student team (repository `Dementia-Tester/aoca-smart-app`) under a usability-evaluation scope, then picked up by Group B (Trimester 2, 2026) under a revised scope: complete outstanding functionality and prepare the app for public release on the Google Play Store and Apple App Store. This document covers Group B's handover; see the final report for full project history.

## Tech Stack

- Kotlin Multiplatform (KMP) with Compose Multiplatform for shared UI across Android and iOS
- Firebase: Authentication, Realtime Database, Storage
- Gradle / Android Gradle Plugin for the Android build, CocoaPods for iOS Firebase interop
- GitHub Actions for CI (automated tests, security rules tests against the Firebase emulator)

## Getting Set Up

1. Clone the repository.
2. You will need three files that are intentionally **not** committed to the repo (see Security Notes below for why):
   - `composeApp/google-services.json` (Android Firebase config)
   - `iosApp/iosApp/GoogleService-Info.plist` (iOS Firebase config)
   - `keystore.properties` at the repo root (Android release signing credentials)
   
   Contact the current project lead or the Firebase project owner (`dementiatester@gmail.com`) for these. They are not published anywhere in the repo or its history going forward.
3. Firebase project: `dementia-tester-4bfbe` in the Firebase console, registered app package name `com.aoca.dementiatester`.
4. Open the project in Android Studio, let Gradle sync, and run on an emulator or device for Android.
5. For iOS, open `iosApp/iosApp.xcworkspace` (not `.xcodeproj`) in Xcode, run `pod install` in `iosApp/` if pods aren't already resolved, and build from there.

## Repository Structure

- `composeApp/` — shared KMP code plus Android-specific implementation
- `iosApp/` — iOS-specific implementation and Xcode project
- `functions/` — Firebase Cloud Functions
- `docs/SECURITY_AUDIT.md` — living log of Firebase Realtime Database security rule audits, update this whenever rules change
- `MANUAL_TEST_REPORT.md`, `AUTOMATED_TEST_REPORT.md`, `HEURISTIC_EVALUATION.md`, `USER_GUIDE.md` — testing and usability documentation

## Data Model

Firebase Realtime Database, owner-scoped structure:

- `UserProfiles/{uid}` — role is stored in `userType` (lowercase: `user` for patient, `doctor`, `caregiver`), plus `assignedDoctorId` / `assignedCaregiverId` for relationships
- `Appointments/{uid}/{appointmentId}`
- `Reminders/{uid}/{reminderId}`
- `Activities/{uid}/{activityId}` — activity/progress log
- `chatRooms/{roomId}` — real-time chat, room ID is a canonical sorted combination of the two participants' UIDs

Security rules restrict each user to their own data, or, for doctors and caregivers, to the data of patients explicitly assigned to them. Every new feature or data path needs a corresponding rule added, this project has had multiple bugs (Activities node, chat) caused by a new path being added without a matching rule. Check `docs/SECURITY_AUDIT.md` before and after any rules change.

## Deployment

### Google Play

- App is registered as `com.aoca.dementiatester` under the `dementiatester@gmail.com` Play Console account.
- Closed testing track requirements (12+ opted-in testers, 14 consecutive days) have been completed; the app is eligible for production release.
- Release builds are signed using a keystore referenced via `keystore.properties` (not committed) or the `KS_STORE_PASS` / `KS_KEY_PASS` environment variables in CI.
- **Important**: the original inherited GitHub Actions secrets did not match this app's actual signing key, since the previous team's own release keystore could not be recovered in time. The upload key was reset directly through Play Console to resolve this. Whoever has access to the current keystore and Play Console account is the source of truth going forward, back it up somewhere durable and outside version control, this exact problem has already cost two teams significant time.

### Apple App Store

- An Apple Developer account is believed to already exist under the same `dementiatester@gmail.com` account (confirmed by the project supervisor, not yet independently verified in App Store Connect at time of writing).
- App Store submission has not yet been completed. This is the main outstanding deployment task.

## Known Limitations / Remaining Work

- Apple App Store submission not yet completed.
- A separate Admin role (distinct from Doctor) was deferred to a future iteration at the supervisor's direction; Doctor currently also carries admin permissions.
- Some UI/UX polish remains on a small number of screens (minor scrolling/layout issues).
- Google Sign-In is implemented on Android only; iOS currently supports email/password sign-in only.
- Biometric sign-in was scoped in the original proposal but not implemented.
- Formal user testing, participant recruitment and ethics approval were confirmed out of scope for this iteration by the project supervisor; any future clinical/user trial would need this revisited.

## Development Process Notes

- PRs are reviewed against a checklist: CI green, code matches description, tests are meaningful, code is readable. Merge requires at least one approval, green CI, resolved conflicts, and a squash commit; branches are deleted after merge.
- GitHub Actions runs the test suite and a Firebase emulator-backed security rules suite on every pull request, this has caught real bugs before merge (see `docs/SECURITY_AUDIT.md`) and is worth keeping in place rather than bypassing.
- A workflow for building the Android App Bundle (.aab) and one for the iOS build both exist; verify both still run cleanly before relying on them, CI configuration has drifted from the actual signing setup before (see Deployment above).

## Security Notes (Read Before Making the Repo Public)

- `composeApp/release.jks` and `iosApp/iosApp/GoogleService-Info.plist` were committed to the repository history before `.gitignore` rules were added to exclude them. Adding a `.gitignore` entry does not remove a file from history, both are still recoverable from past commits even though current commits no longer show them as tracked changes.
- `release.jks` is the release signing keystore. If this repository is ever made public, treat that key as compromised: rotate/reset the signing key again and ensure the new keystore is never committed.
- Recommended before any future public sharing: use `git filter-repo` (or BFG Repo-Cleaner) to strip both files from history entirely, not just delete them going forward.
- `GoogleService-Info.plist` contains Firebase client config values (API key, client IDs). These are lower risk since they're designed to ship inside the app binary and rely on Firebase Security Rules rather than secrecy, but keeping them out of version control is still the intended practice for this project.

## Testing

- Manual test cases: `MANUAL_TEST_REPORT.md` (31 cases, T01–T31)
- Automated tests: `AUTOMATED_TEST_REPORT.md`, run via `./gradlew testDebugUnitTest`
- Security rules tests: Firebase emulator suite (`database-rules.test.js`), run via GitHub Actions on every pull request
- Five manual test cases remain untested by automation (require full Compose UI rendering); flagged as a good starting point for future instrumented test coverage

## Contacts

- Project supervisor / client contact: Dr Fareed Ud Din, fuddin@une.edu.au
- Original development team (Group B, Trimester 2 2026): see the final report's Group Members table for names and roles
