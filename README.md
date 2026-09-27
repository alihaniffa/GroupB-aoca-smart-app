# AoCA Smart App / Dementia Tester

[![Android CI](https://github.com/alihaniffa/GroupB-aoca-smart-app/actions/workflows/android-ci.yml/badge.svg)](https://github.com/alihaniffa/GroupB-aoca-smart-app/actions/workflows/android-ci.yml)
[![Firebase Rules Tests](https://github.com/alihaniffa/GroupB-aoca-smart-app/actions/workflows/firebase-test.yml/badge.svg)](https://github.com/alihaniffa/GroupB-aoca-smart-app/actions/workflows/firebase-test.yml)
[![PR Checks](https://github.com/alihaniffa/GroupB-aoca-smart-app/actions/workflows/pr-checks.yml/badge.svg)](https://github.com/alihaniffa/GroupB-aoca-smart-app/actions/workflows/pr-checks.yml)
[![Android AAB Build](https://github.com/alihaniffa/GroupB-aoca-smart-app/actions/workflows/android-aab.yml/badge.svg)](https://github.com/alihaniffa/GroupB-aoca-smart-app/actions/workflows/android-aab.yml)

## Table of Contents
- [Project Overview](#project-overview)
  - [Key Features](#key-features)
  - [Architecture & Tech Stack](#architecture--tech-stack)
  - [Directory Structure](#directory-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Firebase Configuration](#firebase-configuration)
  - [Building and Running](#building-and-running)
    - [Android](#android)
    - [iOS](#ios)
- [Testing & Quality Assurance](#testing--quality-assurance)
  - [Automated Unit Tests](#automated-unit-tests)
  - [Firebase Security Rules Emulator Tests](#firebase-security-rules-emulator-tests)
  - [QA & Audit Documentation](#qa--audit-documentation)
- [Deployment & Release Engineering](#deployment--release-engineering)
  - [Android (.aab Google Play Release)](#android-aab-google-play-release)
  - [iOS (App Store Connect / TestFlight)](#ios-app-store-connect--testflight)
- [CI/CD Workflows](#cicd-workflows)
- [Learn More](#learn-more)

---

## Project Overview

The **AoCA (Agent-oriented Cognition-based Smart Assistant) Smart App** is a cross-platform mobile application developed using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. The app is engineered to evaluate, monitor, and support cognitive abilities in individuals living with, or at risk of, dementia. 

It provides cognitive assessments, routine daily reminders, appointment booking, and secure communication channels between patients, carers, and clinicians.

### Key Features
- **Patient Dashboard:** Centralised hub displaying daily reminders, scheduled assessments, cognitive mini-games, and progress metrics.
- **Cognitive Assessments & Surveys:** Structured tests and questionnaires evaluating memory, attention, executive function, and longitudinal cognitive health.
- **Interactive Mini-Games:** Evidence-aligned cognitive stimulation tasks assessing reaction time and pattern recognition.
- **Personalised Reminders & Local Notifications:** System-level push notifications for medications, surveys, appointments, and routine daily activities.
- **Secure Clinical Messaging (Chat):** Real-time, peer-to-peer messaging powered by Firebase Realtime Database with symmetric room identifiers, role-based access control, and active 2-second message polling.
- **Doctor & Clinician Portal:** Dedicated clinical dashboard enabling verified physicians to review patient progress, manage caregiver assignments, and initiate direct consultations.
- **Google Sign-In & Email Authentication:** Multi-provider authentication synchronised directly with Realtime Database user profiles.
- **Elderly-Centric Accessibility:** High-contrast colorways, dynamic typography scaling, and non-wrapping horizontal menu navigation tailored for elderly legibility.

---

## Architecture & Tech Stack

The application employs a decoupled **Clean Architecture** pattern across multiplatform layers:

* **Target SDKs:** Android `compileSdk: 36`, `targetSdk: 36` (Android 16), `minSdk: 24` | iOS 16+.
* **Version:** `4.2` (`versionCode: 6`).
* **UI Layer:** Compose Multiplatform (Material 3).
* **Domain Layer:** Decoupled service interfaces (`AuthServiceInterface`, `UserSettingsServiceInterface`, `ActivityServiceInterface`, `UserProfileService`).
* **Test Doubles:** Configurable in-memory fakes (`FakeAuthService`, `FakeUserSettingsService`, `FakeActivityService`, `FakeUserProfileService`) for sub-second, deterministic offline test execution.
* **Backend:** Single source of truth using **Firebase Realtime Database (RTDB)** for user profiles, settings, appointments, and real-time chat.
* **Security Layer:** Role-based security rules in `database.rules.json` with strict PII access controls and client-side privilege escalation blocks.

### Directory Structure
```
GroupB-aoca-smart-app/
├── composeApp/                     # Shared Compose Multiplatform module
│   ├── build.gradle.kts            # Android & multiplatform dependencies (SDK 36)
│   ├── src/
│   │   ├── commonMain/kotlin/      # Shared domain logic and UI
│   │   │   └── org/example/dementia_tester_app/
│   │   │       ├── App.kt          # Application navigation and lifecycle root
│   │   │       ├── auth/           # Authentication interfaces, Google Sign-In handlers
│   │   │       ├── data/           # UserProfile, UserSettings, and Activity interfaces
│   │   │       ├── ui/             # Compose screens (Chat, DoctorDashboard, Login, Surveys)
│   │   │       └── utils/          # Validation, styling, and helper utilities
│   │   ├── androidMain/kotlin/     # Android platform implementations (Auth, Services)
│   │   ├── iosMain/kotlin/         # iOS platform implementations
│   │   └── commonTest/kotlin/      # Automated unit test suites and fakes
│   │       ├── auth/               # AuthLogicTest, AuthFlowTest, GoogleSignInLogicTest
│   │       ├── data/               # SettingsPersistenceTest, ActivityLogicTest
│   │       └── utils/              # SignUpValidationTest
├── iosApp/                         # Native iOS Xcode wrapper and CocoaPods
├── docs/                           # Architectural, security, and QA documentation
│   └── SECURITY_AUDIT.md           # Formal audit log of Firebase security rules
├── .github/workflows/              # GitHub Actions CI/CD automation pipelines
│   ├── android-ci.yml              # Automated unit tests on push/PR
│   ├── pr-checks.yml               # PR validation and build verification
│   ├── firebase-test.yml           # Firebase security rules emulator testing
│   └── android-aab.yml             # Signed production Google Play AAB bundler
├── database.rules.json             # Hardened Realtime Database security rules
├── database-rules.test.js          # Jest unit tests for Firebase security rules
└── firebase.json                   # Firebase emulator configuration
```

---

## Getting Started

### Prerequisites
- **JDK:** OpenJDK 22 or newer
- **IDE:** Android Studio Ladybug / IntelliJ IDEA 2024.2+ with KMP plugin
- **iOS Development (Optional):** macOS with Xcode 16+ and CocoaPods installed
- **Node.js (Optional):** Node.js 18+ for running local Firebase security rules emulator tests

### Firebase Configuration
The application requires Firebase credentials to communicate with authentication and Realtime Database services:

1. Log into the Firebase Console and open project `dementia-tester-4bfbe`.
2. Under **Project Settings** > **Your Apps**:
   - Download `google-services.json` (Android) and place it in the `composeApp/` root directory.
   - Download `GoogleService-Info.plist` (iOS) and place it in the `iosApp/` directory.
3. *Note: Never commit `google-services.json` or `GoogleService-Info.plist` to version control.*

### Building and Running

#### Android
```bash
# Verify unit tests execute cleanly
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug

# Install and run on connected device/emulator
./gradlew installDebug
```

#### iOS
1. Navigate to the `iosApp/` directory:
   ```bash
   cd iosApp
   pod install
   ```
2. Open `iosApp.xcworkspace` in Xcode.
3. Select an iOS Simulator or connected physical iPhone and press **Run (Cmd + R)**.

---

## Testing & Quality Assurance

Quality assurance is verified through automated pipelines that execute on every pull request and push to `main`.

### Automated Unit Tests
The shared module includes 7 automated unit test suites covering authentication, session handling, validation logic, profile persistence, and clinical activity tracking:
```bash
./gradlew testDebugUnitTest
```
*Typical execution time: ~600 ms.*

### Firebase Security Rules Emulator Tests
Firebase Realtime Database access control and data validation rules are continuously verified against the local Firebase emulator using Jest:
```bash
# Install dependencies
npm install

# Run rules test suite against local emulator
npm test
```
The test suite (`database-rules.test.js`) verifies:
- Unauthenticated requests are completely rejected.
- Patients cannot harvest other patient profiles (PII protection).
- Non-doctor accounts cannot modify `userType` to elevate privileges.
- Chat room access is strictly restricted to participating UIDs.
- Messages require valid timestamps, matching `senderId`, and max length constraints.

### QA & Audit Documentation
- [`AUTOMATED_TEST_REPORT.md`](AUTOMATED_TEST_REPORT.md) — Comprehensive traceability matrix mapping automated tests to manual QA test cases (T02–T16, T28).
- [`MANUAL_TEST_REPORT.md`](MANUAL_TEST_REPORT.md) — Record of manual functional and accessibility test evaluations.
- [`docs/SECURITY_AUDIT.md`](docs/SECURITY_AUDIT.md) — Detailed security audit documenting all identified vulnerabilities and rule patches.
- [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) — Tracked edge cases and future roadmap items.

---

## Deployment & Release Engineering

### Android (.aab Google Play Release)
Google Play Store deployments are fully automated using GitHub Actions.

1. Ensure `versionCode` and `versionName` are incremented in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) and [`composeApp/build.gradle.kts`](composeApp/build.gradle.kts).
2. Trigger the **Android AAB Build** workflow in GitHub Actions (`.github/workflows/android-aab.yml`).
3. The workflow decodes the repository release keystore (`release.jks`), executes the unit test suites, and compiles a signed production Android App Bundle:
   ```bash
   ./gradlew :composeApp:bundleRelease
   ```
4. Download the generated `.aab` artifact from the workflow run and upload it to the [Google Play Console](https://play.google.com/console) under the target track (Internal Testing, Closed Testing, or Production).

### iOS (App Store Connect / TestFlight)
1. Open the project in Xcode (`iosApp/iosApp.xcworkspace`).
2. Select the app target, inspect **Identity**, and confirm the **Version** number.
3. Trigger the `.github/workflows/iOS-deploy.yml` workflow to compile and upload the signed archive directly to TestFlight via App Store Connect.

---

## CI/CD Workflows

| Workflow | File | Trigger | Description |
| :--- | :--- | :--- | :--- |
| **Android CI** | [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml) | Push / PR to `main` | Runs `./gradlew testDebugUnitTest` and verifies debug compilation. |
| **PR Checks** | [`.github/workflows/pr-checks.yml`](.github/workflows/pr-checks.yml) | Pull Requests | Validates code formatting, dependency graph, and unit test pass rates. |
| **Firebase Rules** | [`.github/workflows/firebase-test.yml`](.github/workflows/firebase-test.yml) | Rules / PR updates | Boots Firebase emulator and executes `database-rules.test.js`. |
| **Android AAB** | [`.github/workflows/android-aab.yml`](.github/workflows/android-aab.yml) | Manual dispatch / Tag | Signs and compiles production `.aab` with keystore secrets. |

---

## Learn More
- [Compose Multiplatform Documentation](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Kotlin Multiplatform Mobile (KMP)](https://kotlinlang.org/docs/multiplatform.html)
- [Firebase Realtime Database Security Rules](https://firebase.google.com/docs/database/security)
- [Android Developer Guides (Target SDK 36)](https://developer.android.com/about/versions/16)
