# Known Issues and Future Testing Work

## Known Issues

1. Automated test coverage extends to 14 of the 31 manual test cases plus 2 automated-only cases, but does not yet reach full coverage of the application.
2. Compose UI component testing has not yet been added for the screens that depend on full UI rendering rather than underlying logic (five test cases remain manual-only for this reason).
3. Notification functionality has mainly been reviewed using the emulator and should also be tested on a physical Android device.
4. Some features rely on live Firebase services, meaning testing results may be affected by network availability or Firebase configuration issues.
5. No automated coverage currently exists specifically for appointment creation/history or reminder and notification preference logic, these are covered by manual test cases (T29–T31 for appointments) but not yet by unit tests.
6. Test coverage is not currently measured or monitored, so untested areas of the codebase aren't systematically tracked.

## Future Work

The following improvements are recommended for future development:

* Add unit tests for appointment creation and appointment history functionality.
* Add tests for notification preferences and reminder features.
* Add Compose UI tests for important screens such as Login, Sign Up, Profile, Settings, and Appointments.
* Measure and monitor test coverage to identify untested areas of the application.
* Test notification functionality on a physical Android device, not only the emulator.
* Extend the existing GitHub Actions test suite to cover these gaps as they're addressed, rather than adding tests without CI coverage.

## Already Addressed

For context, several items originally flagged here have since been resolved:

* Automated test execution through GitHub Actions is configured and runs on every pull request, including a Firebase emulator-backed suite that tests Realtime Database security rules directly.
* Firebase Authentication now has dedicated test coverage (AuthLogicTest.kt, AuthFlowTest.kt, GoogleSignInLogicTest.kt).
* Login and sign-up validation, password reset, and settings persistence all have dedicated unit tests (SignUpLogicTest.kt, SignUpValidationTest.kt, PasswordResetLogicTest.kt, SettingsPersistenceTest.kt, UserSettingsPersistenceTest.kt).

## Summary

The issues listed above do not prevent the Dementia SmartApp from operating, but they highlight areas where testing and quality assurance can still be improved. The team has closed several of the original gaps in this document over the course of the project, most notably CI/CD integration and Firebase Authentication test coverage, and the remaining items (Compose UI testing, appointment/reminder unit tests, and coverage measurement) are the genuine gaps left for a future team to pick up.
