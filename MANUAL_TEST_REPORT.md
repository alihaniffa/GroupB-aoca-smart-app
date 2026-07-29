# Manual Test Report

## Project

Dementia Tester App

## Testers

Tshering Dorji  
Manila Shrestha  


## Testing Environment

* Android Studio
* Android Emulator
* Kotlin / Compose Multiplatform
* Firebase Authentication
* Firebase Realtime Database / Firestore

## Objective

The objective of this manual testing activity was to review the main user workflows of the Dementia Tester App and identify any obvious issues affecting usability, navigation, authentication, settings persistence, and general application functionality.

## Test Cases

| ID  | Area                | Test Case                        | Expected Result                     | Actual Result                       | Status   |
| --- | ------------------- | -------------------------------- | ----------------------------------- | ----------------------------------- | -------- |
| T01 | App Launch          | Open the app in Android emulator | App opens without crashing          | App opened successfully             | Pass     |
| T02 | Login               | Enter valid email and password   | User logs in successfully           | Login worked as expected            | Pass     |
| T03 | Login               | Enter incorrect password         | Error message appears               | Error message displayed             | Pass     |
| T04 | Login               | Enter invalid email format       | Validation or error message appears | Error message displayed             | Pass     |
| T05 | Sign Up             | Review sign up process           | User can create a new account       | Sign up screen functioned correctly | Reviewed |
| T06 | Sign Up             | Use weak password                | Weak password warning appears       | Warning message displayed           | Reviewed |
| T07 | Password Reset      | Review password reset workflow   | Reset process is available          | Password reset option available     | Reviewed |
| T08 | Settings            | Open settings screen             | Settings screen loads correctly     | Screen loaded successfully          | Pass     |
| T09 | Settings            | Change settings option           | Setting updates correctly           | Setting change observed             | Pass     |
| T10 | Settings            | Reopen app after saving settings | Settings remain available           | Settings appeared to persist        | Reviewed |
| T11 | Profile             | Open profile page                | Profile page loads correctly        | Profile page opened successfully    | Pass     |
| T12 | Appointment         | Open appointment page            | Appointment page loads correctly    | Page opened successfully            | Reviewed |
| T13 | Appointment History | Access appointment history       | History page is accessible          | History page opened successfully    | Reviewed |
| T14 | Notifications       | Review reminder functionality    | Reminder feature is available       | Reminder feature observed           | Reviewed |
| T15 | Navigation          | Move between main screens        | No crashes occur                    | Navigation worked correctly         | Pass     |

## Additional Usability Tests by Manila Shrestha

| ID  | Area           | Test Case                         | Expected Result                   | Actual Result                                       | Status      |
| --- | -------------- | --------------------------------- | --------------------------------  | --------------------------------------------------  | ----------- |
| T16 | Readability    | Review font size and text clarity | Text is easy for older users      | Some small grey text may be difficult to read       | Issue Found |
| T17 | Navigation     | Test screen labels and navigation | Each option opens the right page  | Main tabs opened the expected screens               | Pass        |
| T18 | Buttons        | Review button size and spacing    | Buttons are easy to identify      | Buttons were clearly visible and easy to tap        | Pass        |
| T19 | Instructions   | Review assessment wording         | Instructions are simple and clear | Some assessment questions were lengthy              | Issue Found |
| T20 | Error Handling | Submit incomplete reminder form   | Clear validation messages appear  | Missing fields were highlighted with clear messages | Pass        |

## Issues Found

No major blocking issues were identified during this testing review. The application launched successfully and the core screens were accessible through the Android emulator. However, additional automated testing would be beneficial for Firebase-related functionality, form validation, and Compose UI components.

Additional usability testing identified the following areas for improvement:

* Some smaller grey text may be difficult for older users to read.
* Some assessment questions are lengthy and could be simplified for users with cognitive impairment.

## Recommendations

* Add automated unit tests for validation logic.
* Add Firebase mock tests for authentication and settings persistence.
* Add UI tests for important screens such as Login, Sign Up, Settings, and Appointments.
* Integrate automated testing into the CI/CD workflow.
* Perform additional testing on physical Android devices in addition to the emulator.

## Conclusion

The manual testing review confirmed that the main application screens were accessible and that key user workflows functioned correctly within the Android emulator. The additional usability review identified minor concerns relating to small grey text and lengthy assessment questions. Further automated and physical device testing is recommended to improve accessibility, reliability, and reduce the risk of future regressions.