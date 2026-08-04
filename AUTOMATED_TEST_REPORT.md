# Automated Test Coverage Report

## Project
Dementia Tester App

## Purpose
This report supplements the original Manual Test Report by documenting automated test coverage added for previously "Reviewed" (manually-only) test cases.

## Coverage Summary

| ID  | Area                 | Test Case                        | Manual Status | Automated Coverage | Test File |
| --- | -------------------- | --------------------------------- | -------------- |-----------------| --------- |
| T05 | Sign Up              | Create a new account              | Reviewed        | Automated       | `SignUpLogicTest.kt` |
| T06 | Sign Up              | Weak password warning             | Reviewed        | Automated       | `SignUpLogicTest.kt` |
| T07 | Password Reset       | Reset workflow availability       | Reviewed        | Automated       | `PasswordResetLogicTest.kt` |
| T10 | Settings             | Settings persist after reopening  | Reviewed        | Automated       | `SettingsPersistenceTest.kt`, `UserSettingsPersistenceTest.kt` |
| T12 | Appointment          | Appointment page loads correctly  | Reviewed        | Automated       | `ActivityLogicTest.kt` |
| T13 | Appointment History  | History page accessible           | Reviewed        | Automated       | `ActivityLogicTest.kt` |
| T14 | Notifications        | Reminder feature availability     | Reviewed        | Automated       | `ActivityLogicTest.kt` |

## Supporting Infrastructure Added

- `ActivityServiceInterface` / `FakeActivityService` — enables testing activity logging, real-time flow updates, and today's-summary grouping without a live Firebase connection.
- `UserSettingsServiceInterface` / `FakeUserSettingsService` — enables testing settings load/save persistence across simulated app restarts.
- `AuthServiceInterface` / `FakeAuthService` — enables testing sign in, sign up, password reset, and password change flows, with support for stubbed results via `signInResult` for isolated scenario testing.

## Test Execution

All tests run via:
`./gradlew clean build`

or specifically:
`./gradlew test`


## Remaining Manual-Only Cases

The following cases remain manually verified only, as they involve full UI rendering/navigation rather than business logic, and are lower priority for unit-level automation:

| ID  | Area       | Test Case                        |
| --- | ---------- | --------------------------------- |
| T01 | App Launch | App opens without crashing        |
| T02 | Login      | Valid credential login            |
| T03 | Login      | Incorrect password error message  |
| T04 | Login      | Invalid email format validation   |
| T08 | Settings   | Settings screen loads correctly   |
| T09 | Settings   | Setting updates correctly         |
| T11 | Profile    | Profile page loads correctly      |
| T15 | Navigation | Move between main screens         |

These would be better suited to Compose UI tests (`ComposeTestRule`) or instrumented tests rather than unit tests, since they require rendering actual screens.

## Recommendations Going Forward

- Add Compose UI tests for T01-T04, T08-T09, T11, and T15 using `createComposeRule()`.
- Wire this test suite into CI/CD (see accompanying GitHub Actions workflow) so regressions are caught automatically on every push/PR.
- Consider adding UI-level integration tests once the unit-level fakes are stable, to test the full stack from Compose screen to fake service.