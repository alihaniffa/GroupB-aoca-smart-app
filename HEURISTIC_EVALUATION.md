# Heuristic Evaluation

## 1. Introduction

This document records an internal usability review of the Dementia Tester App. The review was carried out using Nielsen's usability heuristics to identify interface features that work well and areas that may require improvement.

The evaluation was completed using the Android version of the application in an Android emulator. The review focused mainly on navigation, readability, user control, consistency, system feedback and error prevention.

This was an internal usability review rather than formal user testing with external participants.

## 2. Evaluation Method

The application was reviewed screen by screen while performing common tasks such as:

- Navigating through the dashboard.
- Viewing reminders.
- Accessing cognitive assessments.
- Viewing games and progress information.
- Using the side navigation menu.
- Starting and completing the health survey.
- Reviewing survey questions and answer options.
- Checking survey progress and navigation controls.

The findings were compared against relevant Nielsen usability heuristics.

## 3. Severity Levels

The following severity levels were used:

- **Low** – Minor issue or positive usability observation that does not significantly affect use.
- **Medium** – Usability issue that may cause confusion or difficulty and should be improved.
- **High** – Serious usability issue that could prevent users from completing an important task.

## 4. Findings

| Screen | Heuristic | Finding | Severity | Recommendation |
| --- | --- | --- | --- | --- |
| Dashboard - Reminders | Visibility of system status | The reminder card clearly shows the reminder name, time and whether the reminder is enabled or disabled | Low | Keep the current reminder status indicators |
| Dashboard - Reminders | Consistency and standards | The Reminders, Test, Games and Progress tabs use a consistent navigation style | Low | Maintain the same tab design and terminology |
| Dashboard - Reminders | Recognition rather than recall | Important actions such as Create Reminder, enable/disable and delete are visible on the screen | Low | Continue keeping important actions visible |
| Dashboard - Reminders | Aesthetic and minimalist design | The screen is simple and the Create Reminder button is easy to identify | Low | Maintain the uncluttered layout |
| Dashboard - Reminders | Accessibility / readability | Some smaller grey supporting text may be difficult for older users to read | Medium | Increase text contrast and consider slightly larger supporting text |
| Dashboard - Test | Visibility of system status | Previous attempts clearly show their status and progress, for example Incomplete and 3/24 | Low | Keep assessment status and progress visible |
| Dashboard - Test | Recognition rather than recall | View, Continue and Start New Attempt actions are clearly available | Low | Keep the main assessment actions visible |
| Dashboard - Test | User control and freedom | Users can continue an unfinished assessment or begin a new attempt | Low | Continue providing both options |
| Dashboard - Test | Consistency and standards | The selected Test tab is highlighted in the same way as the other dashboard tabs | Low | Maintain consistent tab highlighting |
| Dashboard - Test | Accessibility / readability | Some assessment descriptions, timestamps and progress information use relatively small grey text | Medium | Improve text contrast and consider increasing the text size |
| Dashboard - Games | Match between system and real world | Each game includes a short description that explains what the user needs to do | Low | Keep the simple descriptions for each game |
| Dashboard - Games | Recognition rather than recall | The available games are shown as visible cards with titles and descriptions | Low | Keep all game choices clearly visible |
| Dashboard - Games | Consistency and standards | The Games tab follows the same navigation style as the other dashboard sections | Low | Maintain the same navigation style |
| Dashboard - Games | Aesthetic and minimalist design | The page is uncluttered and the three games are clearly separated | Low | Preserve the simple card-based layout |
| Dashboard - Games | Accessibility / readability | Some supporting text is smaller and lighter than the main headings | Medium | Increase the contrast and size of supporting text where appropriate |
| Dashboard - Progress | Visibility of system status | The screen displays counts for tests, surveys and games together with assessment scores and percentages | Low | Keep the progress information clearly visible |
| Dashboard - Progress | Match between system and real world | Labels such as Tests, Surveys, Games, Assessment Summary and Domain Breakdown are understandable | Low | Continue using clear and familiar wording |
| Dashboard - Progress | Recognition rather than recall | Assessment, Health Surveys and Mini Games are visible as separate progress categories | Low | Keep progress categories visible |
| Dashboard - Progress | Aesthetic and minimalist design | Information is divided into cards and sections, making the progress information easier to understand | Low | Maintain the clear grouping of information |
| Dashboard - Progress | Accessibility / readability | Some text is relatively small and the Assessments label wraps onto two lines | Medium | Adjust text size or spacing so labels are easier to read |
| Side Menu | Recognition rather than recall | Major app sections are available from one menu, including Dashboard, Health Survey, Activities, Book Appointment, Appointment History, Contact, Chat, Settings, Help and Logout | Low | Keep the main navigation options visible and clearly labelled |
| Side Menu | Consistency and standards | Menu options consistently use an icon together with a text label | Low | Maintain the icon-and-label navigation style |
| Side Menu | User control and freedom | Users can quickly move to major sections of the application and access Logout from the menu | Low | Keep direct navigation to major screens |
| Side Menu | Aesthetic and minimalist design | Although the menu contains several options, they are vertically organised and easy to scan | Low | Maintain clear spacing between menu items |
| Side Menu | Accessibility / readability | Some profile information and menu text use lighter colours that may be difficult for older users to read | Medium | Improve contrast for secondary text |
| Health Survey - Introduction | Match between system and real world | The introduction explains what the health survey is for and describes the areas it covers | Low | Keep the introductory explanation |
| Health Survey - Introduction | Visibility of system status | Users are told that the survey contains 11 questions and takes approximately 5–7 minutes | Low | Keep the question count and estimated completion time visible |
| Health Survey - Introduction | User control and freedom | Back and Start buttons allow users to leave the screen or begin the survey | Low | Keep both actions clearly available |
| Health Survey - Introduction | Error prevention | Information about the survey is provided before users begin | Low | Keep an introduction before starting the survey |
| Health Survey - Introduction | Accessibility / readability | The introductory information contains several paragraphs of relatively small text | Medium | Increase text size and consider breaking the information into shorter sections |
| Health Survey - Questions | Visibility of system status | Each survey screen displays the current question number, such as Question 1 of 11 | Low | Keep the question progress indicator visible |
| Health Survey - Questions | Match between system and real world | Most questions use familiar language and provide clear response options | Low | Continue using simple and understandable wording |
| Health Survey - Questions | User control and freedom | Back, Save & Exit and Next buttons are consistently available during the survey | Low | Keep these navigation options available |
| Health Survey - Questions | Recognition rather than recall | Answer choices are displayed directly on each question screen | Low | Continue displaying all available choices |
| Health Survey - Questions | Consistency and standards | The layout, answer controls and navigation buttons remain consistent between survey questions | Low | Maintain the same question layout throughout the survey |
| Health Survey - Questions | Accessibility / readability | Question text is generally easy to read, although some category labels are smaller and less noticeable | Low | Consider slightly increasing the size of category labels |
| Health Survey - Question 7 | Error prevention | The app allows the "None" option to be selected at the same time as another activity such as "Walking", creating contradictory answers | Medium | Make "None" mutually exclusive so selecting it clears other activities and selecting another activity clears "None" |
| Health Survey - Question 7 | Recognition rather than recall | Checkboxes clearly indicate that multiple activities can be selected | Low | Keep checkboxes for questions where multiple responses are appropriate |
| Health Survey - Question 10 | Aesthetic and minimalist design | The question is longer than most other questions and wraps across several lines | Medium | Shorten lengthy questions where possible or divide them into simpler wording |
| Health Survey - Question 11 | Visibility of system status | The screen clearly displays Question 11 of 11, showing that the user has reached the final question | Low | Keep the final question indicator visible |
| Health Survey - Question 11 | Consistency and standards | The final question still uses a button labelled Next even though there are no further questions | Medium | Change the button label to Finish or Submit on the final question |
| Health Survey - Attempt Summary | Visibility of system status | The summary screen shows the total score and displays individual questions together with recorded answers | Low | Keep the detailed summary available before finishing |
| Health Survey - Attempt Summary | Error prevention | The survey can reach the summary screen while some questions are recorded as "Not answered" | Medium | Require an answer before continuing or clearly warn the user about unanswered questions |
| Health Survey - Attempt Summary | Recognition rather than recall | Users can review their previous questions and recorded responses directly on the summary screen | Low | Keep the question-and-answer review available |
| Health Survey - Attempt Summary | Accessibility / readability | The summary contains a large amount of information using relatively small text | Medium | Increase text size and spacing or provide a simpler summary layout |
| Book Appointment | Recognition rather than recall | The booking form clearly separates doctor selection, appointment type, date, available time slots and reason for appointment | Low | Keep the booking form divided into clearly labelled sections |
| Book Appointment | Match between system and real world | Appointment types such as Consultation, Medication, Assessment, Therapy and Telehealth use familiar healthcare terminology | Low | Keep appointment type labels simple and familiar |
| Book Appointment | Consistency and standards | Form controls use a consistent green outline and card-based layout | Low | Maintain consistent styling throughout the booking form |
| Book Appointment | Accessibility / readability | Some labels and placeholder text are relatively small and light | Medium | Increase text contrast and consider slightly larger labels and placeholder text |
| Book Appointment - Doctor Selection | Recognition rather than recall | The doctor dropdown displays available doctors as a visible list | Low | Keep the doctor list visible in the dropdown |
| Book Appointment - Date Selection | Match between system and real world | The date picker uses a familiar calendar layout with month navigation and selectable dates | Low | Keep the standard calendar-style date picker |
| Book Appointment - Date Selection | User control and freedom | The date picker provides Cancel and OK actions | Low | Keep both Cancel and OK options |
| Book Appointment - Available Slots | Visibility of system status | Available appointment times are displayed after selecting a date and the selected time is highlighted | Low | Keep available time slots visible and clearly highlight the selected slot |
| Book Appointment - Form Validation | Error prevention | When a required field is missing, the app prevents submission and displays "Please fill in all required fields" | Low | Keep the current validation behaviour and clear error message |
| Appointment History | Visibility of system status | The history screen shows the selected doctor, appointment type, date, time and appointment status | Low | Keep appointment details and status visible |
| Appointment History | Recognition rather than recall | Saved appointment information is displayed directly in the history screen | Low | Keep appointment information clearly summarised in each history card |
| Settings | Recognition rather than recall | Settings are grouped into clearly labelled sections for Accessibility, Notifications and Account | Low | Keep settings grouped into clear categories |
| Settings | Consistency and standards | Expandable sections use the same green header style and arrow indicator throughout the screen | Low | Maintain the same expandable-section design |
| Settings - Accessibility | Flexibility and efficiency of use | Users can adjust text size and enable options such as High Contrast Mode, Screen Reader, Reduce Motion, Dark Mode and Color Blind Mode | Low | Keep accessibility preferences easy to find and adjust |
| Settings - Accessibility | Recognition rather than recall | The text size control displays visible choices for Small, Medium and Large | Low | Keep the available text-size options clearly labelled |
| Settings - Notifications | User control and freedom | Users can individually enable or disable appointment, medication, test, app update and email notifications | Low | Keep separate notification controls so users can choose their preferences |
| Settings - Account | User control and freedom | Account settings provide controls for Data Sharing, Sync with Cloud, Change Password and Delete Account | Low | Keep account-management options clearly labelled and accessible |
| Settings | Visibility of system status | The app displays a "Settings loaded successfully." message, providing feedback that settings have loaded | Low | Keep status feedback but avoid displaying it repeatedly when it is no longer needed |
| Settings - Account | Aesthetic and minimalist design | "Settings loaded successfully." appears more than once on the same screen, adding unnecessary repeated information | Medium | Display the success message only once or remove it automatically after a short period |
| Settings - Accessibility | Visibility of system status | Enabling Dark Mode changes the application appearance immediately, giving clear feedback that the setting has taken effect | Low | Keep immediate visual feedback when accessibility settings are changed |
| Settings - Accessibility | Consistency and standards | Dark Mode remained enabled after leaving and returning to Settings | Low | Keep saved accessibility preferences persistent across screens |
| Chat | Recognition rather than recall | Existing conversations are shown as visible cards with names, recent message previews and timestamps | Low | Keep conversation summaries visible so users can quickly recognise each chat |
| Chat | Visibility of system status | Unread message counts are displayed as badges on conversations | Low | Keep unread indicators visible |
| Chat | User control and freedom | Users can search existing chats or start a new conversation from the main Chat screen | Low | Keep both search and Start New Chat actions available |
| Chat - New Conversation | Recognition rather than recall | Starting a new chat presents a visible list of available contacts such as Dr. Smith, Nurse Johnson and Family Caregiver | Low | Keep selectable contacts visible rather than requiring users to remember names |
| Chat - Conversation | Match between system and real world | The conversation screen uses a familiar messaging layout with a message field and Send button | Low | Maintain the familiar chat layout |
| Chat - Conversation | Visibility of system status | Sent messages appear immediately in the conversation as message bubbles | Low | Keep immediate visual feedback after sending a message |
| Chat - Conversation | Accessibility / readability | Some message preview text and timestamps on the chat list are relatively small and light | Medium | Increase contrast and consider slightly larger preview and timestamp text |

## 5. Main Usability Issues Identified

The internal review identified several areas that may benefit from improvement.

### 5.1 Text Readability

Some supporting information throughout the application is displayed using smaller grey text. This may be difficult for older users or users with reduced vision to read.

**Suggested improvement:** Increase the contrast and, where appropriate, the size of supporting text.

### 5.2 Long Survey Questions

Some health survey questions contain considerably more text than others. Longer questions may require additional effort for users with cognitive impairment to understand.

**Suggested improvement:** Use shorter sentences and simpler wording where possible.

### 5.3 Contradictory Survey Responses

Question 7 of the Health Survey allows users to select an activity such as "Walking" while also selecting "None". Both options remain selected at the same time.

This creates a contradictory response and represents an error-prevention issue.

**Suggested improvement:** Make the "None" option mutually exclusive. Selecting "None" should automatically clear other activities, while selecting another activity should clear "None".

### 5.4 Unanswered Survey Questions

The survey can reach the Attempt Summary even when a question has not been answered. The summary then displays the response as "Not answered".

**Suggested improvement:** Either require users to answer each question before continuing or display a clear warning before submission that some questions remain unanswered.

### 5.5 Final Survey Button

The final survey question displays a button labelled "Next", even though the user has reached Question 11 of 11.

**Suggested improvement:** Replace "Next" with a clearer final action such as "Finish" or "Submit".

### 5.6 Chat Readability

The chat workflow was functional and easy to understand. Existing conversations, new-chat selection and message sending all worked as expected.

Some message preview text and timestamps on the main Chat screen use relatively small, light text.

**Suggested improvement:** Increase the contrast and size of secondary chat information where appropriate.

### 5.7 Appointment Booking

The appointment booking workflow was generally clear and consistent. Doctor selection, appointment type, date and available time slots were easy to identify.

The form also prevented submission when required information was missing and displayed a clear validation message.

No major usability issue was identified in the appointment booking workflow during this review.

