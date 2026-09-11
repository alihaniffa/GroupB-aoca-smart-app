# Automated Test Coverage Report

## Project
Dementia Tester App

## Purpose
This report supplements the original Manual Test Report by documenting automated test coverage added for previously "Reviewed" or manual test cases.

## Coverage Summary

| ID  | Area                 | Test Case                        | Manual Status | Automated Coverage | Test File |
| --- | -------------------- | --------------------------------- | -------------- |-----------------| --------- |
| T02 | Login                | Valid credential login            | Pass           | Automated       | `AuthLogicTest.kt` |
| T03 | Login                | Incorrect password error message  | Pass           | Automated       | `AuthLogicTest.kt`, `AuthFlowTest.kt` |
| T04 | Login / Validation   | Invalid email format validation   | Pass           | Automated       | `SignUpValidationTest.kt` |
| T05 | Sign Up              | Create a new account              | Reviewed       | Automated       | `SignUpLogicTest.kt`, `AuthFlowTest.kt` |
| T06 | Sign Up              | Weak password warning             | Reviewed       | Automated       | `SignUpLogicTest.kt`, `AuthFlowTest.kt` |
| T07 | Password Reset       | Reset workflow availability       | Reviewed       | Automated       | `PasswordResetLogicTest.kt`, `AuthFlowTest.kt` |
| T10 | Settings             | Settings persist after reopening  | Reviewed        | Automated       | `SettingsPersistenceTest.kt`, `UserSettingsPersistenceTest.kt` |
| T12 | Appointment          | Appointment page loads correctly  | Reviewed       | Automated       | `ActivityLogicTest.kt` |
| T13 | Appointment History  | History page accessible           | Reviewed       | Automated       | `ActivityLogicTest.kt` |
| T14 | Notifications        | Reminder feature availability     | Reviewed       | Automated       | `ActivityLogicTest.kt` |
| T16 | Google Sign-In       | Google profile sync & RTDB schema | Reviewed       | Automated       | `GoogleSignInLogicTest.kt` |
| T28 | Chat / Security      | Chat rooms and database rules     | Pass           | Automated       | `database-rules.test.js` |
| —   | Validation           | Phone, DOB, password match checks | Pass           | Automated       | `SignUpValidationTest.kt` |
| —   | Database Rules       | Unauthenticated & caregiver rules | Pass           | Automated       | `database-rules.test.js` |

## Supporting Infrastructure

- `GoogleSignInHelper` / `GoogleSignInProfileHandler` / `GoogleSignInResultHandler` — enables testing Google account profile generation, Realtime Database schema alignment, new vs. existing user branch handling, and network failure resilience.
- `ActivityServiceInterface` / `FakeActivityService` — enables testing activity logging, real-time flow updates, and today's-summary grouping without a live Firebase connection.
- `UserSettingsServiceInterface` / `FakeUserSettingsService` — enables testing settings load/save persistence across simulated app restarts.
- `AuthServiceInterface` / `FakeAuthService` — enables testing sign in, sign up, password reset, and password change flows, with support for stubbed results via `signInResult` for isolated scenario testing.
- `@firebase/rules-unit-testing` — enables local RTDB security rule verification against emulated Firebase backend.

## Test Execution

### Kotlin / Android Unit Tests
Run all unit tests via:
```bash
./gradlew testDebugUnitTest
```

### Firebase Security Rules Tests
Run RTDB security rules tests via Firebase emulator:
```bash
firebase emulators:exec --only database --project demo-test-project "npx jest database-rules.test.js"
```

## Remaining Manual-Only Cases

The following cases remain manually verified only, as they involve full Compose UI rendering/navigation rather than underlying business logic:

| ID  | Area       | Test Case                        |
| --- | ---------- | --------------------------------- |
| T01 | App Launch | App opens without crashing        |
| T08 | Settings   | Settings screen loads correctly   |
| T09 | Settings   | Setting updates correctly         |
| T11 | Profile    | Profile page loads correctly      |
| T15 | Navigation | Move between main screens         |

These would be better suited to Compose UI tests (`ComposeTestRule`) or instrumented tests rather than unit tests, since they require rendering actual screens.

## Recommendations Going Forward

- Add Compose UI tests for T01, T08-T09, T11, and T15 using `createComposeRule()`.
- Wire this test suite into CI/CD (see accompanying GitHub Actions workflow) so regressions are caught automatically on every push/PR.
- Consider adding UI-level integration tests once the unit-level fakes are stable, to test the full stack from Compose screen to fake service.
