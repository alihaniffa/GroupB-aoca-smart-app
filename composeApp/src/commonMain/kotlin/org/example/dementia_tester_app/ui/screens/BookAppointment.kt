package org.example.dementia_tester_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.dementia_tester_app.auth.AuthService
import org.example.dementia_tester_app.data.Activity
import org.example.dementia_tester_app.data.ActivityService
import org.example.dementia_tester_app.data.ActivityType
import org.example.dementia_tester_app.data.Appointment
import org.example.dementia_tester_app.data.AppointmentService
import org.example.dementia_tester_app.data.AppointmentStatus
import org.example.dementia_tester_app.data.DatabaseResult
import org.example.dementia_tester_app.data.UserProfile
import org.example.dementia_tester_app.data.UserProfileService
import org.example.dementia_tester_app.ui.components.DateField
import org.example.dementia_tester_app.ui.components.ErrorMessage
import org.example.dementia_tester_app.ui.components.FormColors
import org.example.dementia_tester_app.ui.components.FormDropdown
import org.example.dementia_tester_app.ui.components.FormTextField
import org.example.dementia_tester_app.ui.components.SuccessMessage

@Composable
fun SelectableButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                FormColors.green,
                RoundedCornerShape(8.dp)
            )
            .background(
                if (isSelected) {
                    FormColors.green
                } else {
                    FormColors.green.copy(
                        alpha = 0.1f
                    )
                }
            )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text = text,
            color =
                if (isSelected) {
                    Color.White
                } else {
                    FormColors.green
                },
            fontWeight =
                if (isSelected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
            textAlign =
                TextAlign.Center
        )
    }
}

fun validateAppointmentFields(
    doctor: String,
    appointmentType: String,
    date: String,
    time: String,
    reason: String,
    setDoctorError: (Boolean) -> Unit,
    setAppointmentTypeError: (Boolean) -> Unit,
    setDateError: (Boolean) -> Unit,
    setTimeError: (Boolean) -> Unit,
    setReasonError: (Boolean) -> Unit
): Boolean {

    var ok =
        true

    if (doctor.isEmpty()) {
        setDoctorError(true)
        ok = false
    }

    if (appointmentType.isEmpty()) {
        setAppointmentTypeError(true)
        ok = false
    }

    if (date.isEmpty()) {
        setDateError(true)
        ok = false
    }

    if (time.isEmpty()) {
        setTimeError(true)
        ok = false
    }

    if (reason.trim().isEmpty()) {
        setReasonError(true)
        ok = false
    }

    return ok
}

/**
 * Book Appointment screen.
 *
 * Normal patient:
 *
 * BookAppointment()
 *
 * Caregiver:
 *
 * BookAppointment(
 *     targetUserId = patient.userId,
 *     targetUserName = patient.name,
 *     targetUserEmail = patient.email
 * )
 *
 * In caregiver mode, the appointment and activity
 * are saved under the selected patient's UID.
 */
@Composable
fun BookAppointment(
    onCancel: () -> Unit = {},
    onSuccess: () -> Unit = {},
    targetUserId: String? = null,
    targetUserName: String? = null,
    targetUserEmail: String? = null
) {

    val doctors =
        listOf(
            "Dr. Sarah Johnson",
            "Dr. Michael Chen",
            "Dr. Emily Rodriguez",
            "Dr. David Kim",
            "Dr. Jessica Patel"
        )

    val doctorEmails =
        mapOf(
            "Dr. Sarah Johnson" to
                    "sarah.johnson@example.com",

            "Dr. Michael Chen" to
                    "michael.chen@example.com",

            "Dr. Emily Rodriguez" to
                    "emily.rodriguez@example.com",

            "Dr. David Kim" to
                    "david.kim@example.com",

            "Dr. Jessica Patel" to
                    "jessica.patel@example.com"
        )

    val appointmentService =
        remember {
            AppointmentService()
        }

    val activityService =
        remember {
            ActivityService()
        }

    val userProfileService =
        remember {
            UserProfileService()
        }

    val authService =
        remember {
            AuthService()
        }

    val loggedInUserId =
        authService.getCurrentUserId()

    /*
     * User who owns the appointment.
     */
    val userId =
        targetUserId
            ?: loggedInUserId

    val isActingOnBehalf =
        targetUserId != null &&
                targetUserId != loggedInUserId

    val appointmentOwnerName =
        targetUserName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Patient"

    val today =
        Clock.System.now()
            .toLocalDateTime(
                TimeZone.currentSystemDefault()
            )
            .date

    val todayFormatted =
        "${today.dayOfMonth}/" +
                "${today.monthNumber}/" +
                "${today.year}"

    /*
     * Used only in normal patient mode.
     *
     * In caregiver mode we already receive
     * the selected patient's name/email.
     */
    var currentUserProfile by remember {
        mutableStateOf<UserProfile?>(
            null
        )
    }

    LaunchedEffect(
        loggedInUserId,
        isActingOnBehalf
    ) {

        if (
            !isActingOnBehalf &&
            loggedInUserId != null
        ) {

            userProfileService
                .getCurrentUserProfile { result ->

                    if (
                        result
                                is DatabaseResult.Success
                    ) {

                        currentUserProfile =
                            result.data
                    }
                }
        }
    }

    var selectedDoctor by remember {
        mutableStateOf("")
    }

    var selectedAppointmentType by remember {
        mutableStateOf("")
    }

    var selectedDate by remember {
        mutableStateOf(
            todayFormatted
        )
    }

    var selectedTime by remember {
        mutableStateOf("")
    }

    var reasonForAppointment by remember {
        mutableStateOf("")
    }

    var dateSelected by remember {
        mutableStateOf(false)
    }

    var isSubmitting by remember {
        mutableStateOf(false)
    }

    var showErrorMessage by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var showSuccessMessage by remember {
        mutableStateOf(false)
    }

    var successMessage by remember {
        mutableStateOf("")
    }

    var doctorError by remember {
        mutableStateOf(false)
    }

    var appointmentTypeError by remember {
        mutableStateOf(false)
    }

    var dateError by remember {
        mutableStateOf(false)
    }

    var timeError by remember {
        mutableStateOf(false)
    }

    var reasonError by remember {
        mutableStateOf(false)
    }

    fun clearBanners() {
        showErrorMessage = false
        showSuccessMessage = false
    }

    val scrollState =
        rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(
                scrollState
            )
            .padding(
                bottom = 16.dp
            )
    ) {

        /*
         * Caregiver context.
         */
        if (isActingOnBehalf) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                    )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {

                    Text(
                        text =
                            "Booking appointment on behalf of",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )

                    Text(
                        text =
                            appointmentOwnerName,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            FormColors.green
                    )
                }
            }
        }

        // Doctor
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surface
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation =
                        2.dp
                )
        ) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Text(
                    text =
                        "Doctor",
                    fontWeight =
                        FontWeight.Bold,
                    fontSize =
                        18.sp,
                    modifier =
                        Modifier.padding(
                            bottom = 8.dp
                        )
                )

                FormDropdown(
                    label =
                        "Select a doctor",
                    value =
                        selectedDoctor,
                    options =
                        doctors,
                    onValueChange = {

                        selectedDoctor =
                            it

                        doctorError =
                            false

                        clearBanners()
                    },
                    isError =
                        doctorError,
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        }

        // Appointment type
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surface
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation =
                        2.dp
                )
        ) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Text(
                    text =
                        "Select Appointment Type",
                    fontWeight =
                        FontWeight.Bold,
                    fontSize =
                        18.sp,
                    modifier =
                        Modifier.padding(
                            bottom = 8.dp
                        )
                )

                if (appointmentTypeError) {

                    Text(
                        text =
                            "Please select an appointment type",
                        color =
                            FormColors.errorColor,
                        fontSize =
                            12.sp,
                        modifier =
                            Modifier.padding(
                                bottom = 4.dp
                            )
                    )
                }

                val types =
                    listOf(
                        "Consultation",
                        "Carer Support",
                        "Medication",
                        "Assessment",
                        "Therapy",
                        "Telehealth"
                    )

                types
                    .chunked(2)
                    .forEach { row ->

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            row.forEachIndexed {
                                    index,
                                    type ->

                                SelectableButton(
                                    text =
                                        type,
                                    isSelected =
                                        selectedAppointmentType ==
                                                type,
                                    onClick = {

                                        selectedAppointmentType =
                                            type

                                        appointmentTypeError =
                                            false

                                        clearBanners()
                                    },
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .padding(
                                                end =
                                                    if (
                                                        index == 0
                                                    ) {
                                                        8.dp
                                                    } else {
                                                        0.dp
                                                    }
                                            )
                                )
                            }
                        }

                        Spacer(
                            Modifier.height(
                                8.dp
                            )
                        )
                    }
            }
        }

        // Date and time
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surface
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation =
                        2.dp
                )
        ) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Text(
                    text =
                        "Select Date",
                    fontWeight =
                        FontWeight.Bold,
                    fontSize =
                        18.sp,
                    modifier =
                        Modifier.padding(
                            bottom = 8.dp
                        )
                )

                DateField(
                    date =
                        selectedDate,
                    onDateChange = {

                        selectedDate =
                            it

                        dateError =
                            false

                        clearBanners()

                        dateSelected =
                            true
                    },
                    label =
                        "Appointment Date",
                    isError =
                        dateError,
                    isEditable =
                        true,
                    allowDatesAfterToday =
                        true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = 16.dp
                        )
                )

                if (dateSelected) {

                    Text(
                        text =
                            "Available Slots",
                        fontWeight =
                            FontWeight.Medium,
                        fontSize =
                            16.sp,
                        modifier =
                            Modifier.padding(
                                bottom = 8.dp
                            )
                    )

                    if (timeError) {

                        Text(
                            text =
                                "Please select an appointment time",
                            color =
                                FormColors.errorColor,
                            fontSize =
                                12.sp,
                            modifier =
                                Modifier.padding(
                                    bottom = 4.dp
                                )
                        )
                    }

                    listOf(
                        "9:00 AM" to
                                "10:30 AM",
                        "1:00 PM" to
                                "3:30 PM"
                    ).forEach {
                            (
                                first,
                                second
                            ) ->

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            SelectableButton(
                                text =
                                    first,
                                isSelected =
                                    selectedTime ==
                                            first,
                                onClick = {

                                    selectedTime =
                                        first

                                    timeError =
                                        false

                                    clearBanners()
                                },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(
                                            end = 8.dp
                                        )
                            )

                            SelectableButton(
                                text =
                                    second,
                                isSelected =
                                    selectedTime ==
                                            second,
                                onClick = {

                                    selectedTime =
                                        second

                                    timeError =
                                        false

                                    clearBanners()
                                },
                                modifier =
                                    Modifier.weight(1f)
                            )
                        }

                        Spacer(
                            Modifier.height(
                                8.dp
                            )
                        )
                    }
                }
            }
        }

        // Reason
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surface
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation =
                        2.dp
                )
        ) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Text(
                    text =
                        "Reason for Appointment",
                    fontWeight =
                        FontWeight.Bold,
                    fontSize =
                        18.sp,
                    modifier =
                        Modifier.padding(
                            bottom = 8.dp
                        )
                )

                FormTextField(
                    value =
                        reasonForAppointment,
                    onValueChange = {

                        reasonForAppointment =
                            it

                        reasonError =
                            false

                        clearBanners()
                    },
                    label =
                        "Enter reason or comments",
                    isError =
                        reasonError,
                    imeAction =
                        ImeAction.Done,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            120.dp
                        )
                )
            }
        }

        Spacer(
            Modifier.height(
                16.dp
            )
        )

        Box(
            Modifier.padding(
                horizontal = 16.dp
            )
        ) {

            ErrorMessage(
                show =
                    showErrorMessage,
                message =
                    errorMessage
            )
        }

        Box(
            Modifier.padding(
                horizontal = 16.dp
            )
        ) {

            SuccessMessage(
                message =
                    successMessage,
                isVisible =
                    showSuccessMessage,
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            OutlinedButton(
                onClick = {
                    onCancel()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(
                        50.dp
                    ),
                colors =
                    ButtonDefaults
                        .outlinedButtonColors(
                            contentColor =
                                FormColors.green
                        )
            ) {

                Text(
                    "Cancel"
                )
            }

            Button(
                onClick = {

                    if (isSubmitting) {
                        return@Button
                    }

                    val isValid =
                        validateAppointmentFields(
                            doctor =
                                selectedDoctor,
                            appointmentType =
                                selectedAppointmentType,
                            date =
                                selectedDate,
                            time =
                                selectedTime,
                            reason =
                                reasonForAppointment,
                            setDoctorError = {

                                doctorError =
                                    it
                            },
                            setAppointmentTypeError = {

                                appointmentTypeError =
                                    it
                            },
                            setDateError = {

                                dateError =
                                    it
                            },
                            setTimeError = {

                                timeError =
                                    it
                            },
                            setReasonError = {

                                reasonError =
                                    it
                            }
                        )

                    if (!isValid) {

                        errorMessage =
                            "Please fill in all required fields"

                        showErrorMessage =
                            true

                        showSuccessMessage =
                            false

                        return@Button
                    }

                    if (userId == null) {

                        errorMessage =
                            "Unable to determine appointment owner"

                        showErrorMessage =
                            true

                        showSuccessMessage =
                            false

                        return@Button
                    }

                    isSubmitting =
                        true

                    /*
                     * In normal mode these come
                     * from the signed-in profile.
                     *
                     * In caregiver mode they come
                     * from the selected patient.
                     */
                    val patientName =
                        if (isActingOnBehalf) {

                            targetUserName
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Patient"

                        } else {

                            currentUserProfile
                                ?.name
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Patient"
                        }

                    val patientEmail =
                        if (isActingOnBehalf) {

                            targetUserEmail
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: ""

                        } else {

                            currentUserProfile
                                ?.email
                                ?: ""
                        }

                    val appointment =
                        Appointment(
                            doctor =
                                selectedDoctor,
                            type =
                                selectedAppointmentType,
                            date =
                                selectedDate,
                            time =
                                selectedTime,
                            reason =
                                reasonForAppointment,
                            status =
                                AppointmentStatus.Upcoming,
                            patientName =
                                patientName,
                            patientEmail =
                                patientEmail,
                            doctorEmail =
                                doctorEmails[
                                    selectedDoctor
                                ]
                                    ?: ""
                        )

                    /*
                     * Caregiver mode writes to the
                     * patient's appointment node.
                     *
                     * Patient mode keeps using the
                     * existing current-user method.
                     */
                    val handleResult:
                                (
                        DatabaseResult<Unit>
                    ) -> Unit =
                        { result ->

                            isSubmitting =
                                false

                            when (result) {

                                is DatabaseResult.Success -> {

                                    successMessage =
                                        if (isActingOnBehalf) {
                                            "Appointment booked successfully for $appointmentOwnerName!"
                                        } else {
                                            "Appointment booked successfully!"
                                        }

                                    showSuccessMessage =
                                        true

                                    showErrorMessage =
                                        false

                                    /*
                                     * Log appointment activity under
                                     * the same user who owns the
                                     * appointment.
                                     */
                                    activityService
                                        .logActivityForUser(
                                            userId =
                                                userId,
                                            activity =
                                                Activity(
                                                    title =
                                                        "Appointment Booked",
                                                    type =
                                                        ActivityType.APPOINTMENT,
                                                    description =
                                                        if (
                                                            isActingOnBehalf
                                                        ) {
                                                            "Appointment with ${appointment.doctor} on ${appointment.date} booked with caregiver assistance"
                                                        } else {
                                                            "With ${appointment.doctor} on ${appointment.date}"
                                                        }
                                                )
                                        ) {
                                            /*
                                             * Ignore result.
                                             */
                                        }

                                    // Reset form
                                    selectedDoctor =
                                        ""

                                    selectedAppointmentType =
                                        ""

                                    selectedDate =
                                        todayFormatted

                                    selectedTime =
                                        ""

                                    reasonForAppointment =
                                        ""

                                    dateSelected =
                                        false

                                    CoroutineScope(
                                        Dispatchers.Main
                                    ).launch {

                                        delay(
                                            1000
                                        )

                                        onSuccess()
                                    }
                                }

                                is DatabaseResult.Error -> {

                                    errorMessage =
                                        result.message

                                    showErrorMessage =
                                        true

                                    showSuccessMessage =
                                        false
                                }
                            }
                        }

                    if (isActingOnBehalf) {

                        appointmentService
                            .createAppointmentForUser(
                                userId =
                                    userId,
                                appointment =
                                    appointment,
                                callback =
                                    handleResult
                            )

                    } else {

                        appointmentService
                            .createAppointment(
                                appointment =
                                    appointment,
                                callback =
                                    handleResult
                            )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(
                        50.dp
                    ),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            FormColors.green
                    ),
                enabled =
                    !isSubmitting
            ) {

                if (isSubmitting) {

                    CircularProgressIndicator(
                        color =
                            Color.White,
                        modifier =
                            Modifier.height(
                                20.dp
                            )
                    )

                } else {

                    Text(
                        "Submit"
                    )
                }
            }
        }
    }
}