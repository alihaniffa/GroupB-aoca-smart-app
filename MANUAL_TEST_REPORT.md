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
| T21 | Health Survey | Select "None" together with another activity in Question 7 | "None" should not be selectable with another activity | "None" and another activity such as "Walking" can remain selected at the same time | Issue Found |
| T22 | Health Survey | Leave a survey question unanswered and continue | User should be warned or prevented from completing with unanswered questions | Survey reached the Attempt Summary with a question marked "Not answered" | Issue Found |
| T23 | Health Survey | Review the final survey question | Final action should clearly indicate survey completion | Question 11 of 11 still displayed a button labelled "Next" | Issue Found |
| T24 | Appointment | Submit booking form with required information missing | Submission should be blocked and validation shown | Submission was blocked and "Please fill in all required fields" was displayed | Pass |
| T25 | Appointment | Complete an appointment booking | Appointment should be saved and appear in appointment history | Booking was completed and appeared in Appointment History with doctor, date, time and status | Pass |
| T26 | Settings | Enable Dark Mode and navigate away from Settings | Dark Mode should apply and remain enabled | Dark Mode applied successfully and remained enabled after returning to Settings | Pass |
| T27 | Settings | Review system feedback messages | Status feedback should appear clearly without unnecessary duplication | "Settings loaded successfully." appeared more than once on the screen | Issue Found |
| T28 | Chat | Start a new conversation and send a message | User should be able to select a contact and send a message | New conversation opened and the sent message appeared immediately | Pass |

## Issues Found

No major blocking issues were identified during this testing review. The application launched successfully and the core screens were accessible through the Android emulator. However, additional automated testing would be beneficial for Firebase-related functionality, form validation, and Compose UI components.

Additional usability testing identified the following areas for improvement:

* Some smaller grey text may be difficult for older users to read.
* Some assessment questions are lengthy and could be simplified for users with cognitive impairment.
* Health Survey Question 7 allows "None" to be selected together with another activity, creating contradictory responses.
* The Health Survey can reach the Attempt Summary with unanswered questions.
* The final Health Survey question uses "Next" instead of a clearer final action such as "Finish" or "Submit".
* The Settings screen can display the "Settings loaded successfully." message more than once.

## Recommendations

* Add automated unit tests for validation logic.
* Add Firebase mock tests for authentication and settings persistence.
* Add UI tests for important screens such as Login, Sign Up, Settings, and Appointments.
* Integrate automated testing into the CI/CD workflow.
* Perform additional testing on physical Android devices in addition to the emulator.
* Make the Health Survey "None" option mutually exclusive where multiple selections are allowed.
* Add validation or a warning for unanswered Health Survey questions before completion.

## Conclusion

The manual testing review confirmed that the main application screens were accessible and that key user workflows functioned correctly within the Android emulator. The additional testing identified several minor usability issues, including small grey text, lengthy survey questions, contradictory Health Survey selections, unanswered survey questions reaching the summary screen, unclear final survey button wording, and duplicated settings feedback.

No major blocking issues were identified. Further automated testing and testing on physical Android devices are recommended to improve accessibility, reliability, and reduce the risk of future regressions.