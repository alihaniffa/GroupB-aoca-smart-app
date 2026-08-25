# Dementia Tester App – User Guide

## 1. Introduction

The Dementia Tester App is designed to support people living with dementia, carers and healthcare professionals. It provides access to cognitive assessments, appointments, reminders, profile information, settings and other support features.

This guide provides basic instructions for navigating and using the main areas of the application.

## 2. Starting the Application

1. Open the application on an Android device or emulator.
2. Wait for the login screen to appear.
3. Enter your registered email address and password.
4. Select the login button to access the application.

If the login details are valid, the application will open the main area of the app.

## 3. Creating an Account

1. Open the sign-up screen from the login page.
2. Enter the required account information.
3. Create a password that meets the application's requirements.
4. Submit the form to continue with account creation.

The sign-up screen provides validation feedback when account details do not meet the required format.

## 4. Resetting a Password

A password reset option is available from the login screen.

1. Select the password reset option.
2. Enter the email address associated with the account.
3. Follow the prompts displayed by the application.

## 5. Main Navigation

The application provides navigation to the main features, including:

- Assessments
- Appointments
- Appointment history
- Reminders
- Profile
- Settings

Select the relevant tab or option to open each screen.

During manual testing, the main navigation options opened the expected screens successfully.

## 6. Completing an Assessment

1. Open the assessment section.
2. Read each question carefully.
3. Select the most appropriate answer.
4. Continue through the available questions.
5. Submit the assessment when finished.

The answer options were clear during manual testing. However, some assessment questions were lengthy and may be more difficult for users with cognitive impairment to process.

Users may benefit from assistance from a carer or healthcare professional when completing longer assessments.

## 7. Appointments

1. Open the appointments section.
2. Review the appointment information displayed.
3. Open the appointment history section to view previous appointment records.

The appointment and appointment-history screens were accessible during manual testing.

## 8. Creating a Reminder

1. Open the reminder section.
2. Enter a task name.
3. Select the required task time.
4. Complete any other required information.
5. Save the reminder.

If required information is missing, the application highlights the relevant fields and displays a clear validation message.

During manual testing, missing task information produced clear feedback explaining which information was required.

## 9. Profile and Settings

### Profile

The profile page allows users to access their account information.

During manual testing, the profile page opened successfully.

### Settings

The settings screen allows users to review and change available application preferences.

During manual testing:

- The settings screen loaded successfully.
- Changes to settings were observed successfully.
- Saved settings appeared to remain available when the application was reopened.

## 10. Accessibility and Usability Notes

During usability testing, the main navigation and buttons were found to be clear and easy to use.

The following usability concerns were identified:

- Some smaller grey text may be difficult for older users to read.
- Some assessment questions may be too lengthy for users with cognitive impairment.

Users who experience difficulty reading smaller text or completing longer questions may benefit from assistance from a carer or healthcare professional.

## 11. Troubleshooting

### Unable to Log In

- Check that the email address is entered correctly.
- Check that the password is correct.
- Use the password reset option if required.
- Review any error message displayed by the application.

### Invalid Login Details

If an incorrect password or invalid email format is entered, the application displays an error message.

Check the entered details and try again.

### Form Cannot Be Submitted

- Check whether any required fields are empty.
- Review the validation message displayed on the screen.
- Enter the missing information.
- Try submitting the form again.

### Application Does Not Open

- Confirm that the device or emulator is running correctly.
- Restart the application.
- Check the internet connection where Firebase services are required.

## 12. Known Usability Considerations

The application was tested using Android Studio and an Android emulator.

No major blocking issues were identified during the manual testing completed so far. However, further testing is recommended, particularly:

- Testing on physical Android devices.
- Additional accessibility testing.
- Further validation of Firebase-related functionality.
- Additional automated testing.
- User testing with suitable participants.

## 13. Support

For unresolved technical issues during project testing, provide the development team or project supervisor with:

- A description of the issue.
- The screen where the issue occurred.
- The steps completed before the issue appeared.
- A screenshot of the issue or error message where possible.

Providing these details can help the team reproduce and investigate the issue more effectively.