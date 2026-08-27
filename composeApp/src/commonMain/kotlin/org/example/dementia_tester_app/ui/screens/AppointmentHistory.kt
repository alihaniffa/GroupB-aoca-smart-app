package org.example.dementia_tester_app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.dementia_tester_app.auth.AuthService
import org.example.dementia_tester_app.data.Appointment
import org.example.dementia_tester_app.data.AppointmentService
import org.example.dementia_tester_app.data.AppointmentStatus
import org.example.dementia_tester_app.data.DatabaseResult
import org.example.dementia_tester_app.ui.components.FormColors

/**
 * Appointment History screen.
 *
 * Normal patient:
 *
 * AppointmentHistory()
 *
 * Caregiver:
 *
 * AppointmentHistory(
 *     targetUserId = selectedPatient.userId,
 *     targetUserName = selectedPatient.name
 * )
 *
 * When targetUserId is provided, appointments are loaded
 * from the selected patient's appointment node.
 */
@Composable
fun AppointmentHistory(
    targetUserId: String? = null,
    targetUserName: String? = null
) {

    var appointments by remember {
        mutableStateOf<List<Appointment>>(
            emptyList()
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var loadError by remember {
        mutableStateOf<String?>(null)
    }

    var selectedAppointment by remember {
        mutableStateOf<Appointment?>(null)
    }

    val appointmentService =
        remember {
            AppointmentService()
        }

    val authService =
        remember {
            AuthService()
        }

    val loggedInUserId =
        authService.getCurrentUserId()

    /*
     * Determine whether the screen is being
     * viewed by a caregiver on behalf of a patient.
     */
    val isActingOnBehalf =
        targetUserId != null &&
                targetUserId != loggedInUserId

    val appointmentOwnerName =
        targetUserName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Patient"

    /*
     * Load appointments whenever the selected
     * target user changes.
     */
    LaunchedEffect(
        targetUserId,
        loggedInUserId
    ) {

        isLoading = true
        loadError = null
        appointments = emptyList()
        selectedAppointment = null

        /*
         * Common result handler for both
         * patient and caregiver modes.
         */
        val handleResult:
                    (
            DatabaseResult<List<Appointment>>
        ) -> Unit =
            { result ->

                isLoading = false

                when (result) {

                    is DatabaseResult.Success -> {
                        appointments =
                            result.data
                    }

                    is DatabaseResult.Error -> {
                        loadError =
                            result.message
                    }
                }
            }

        /*
         * Caregiver:
         * load the selected patient's
         * appointments.
         *
         * Patient:
         * load the authenticated user's
         * appointments.
         */
        if (isActingOnBehalf) {

            appointmentService
                .getAppointmentsForUser(
                    userId =
                        targetUserId!!,
                    callback =
                        handleResult
                )

        } else {

            appointmentService
                .getAppointments(
                    callback =
                        handleResult
                )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        if (selectedAppointment == null) {

            /*
             * Caregiver context banner.
             */
            if (isActingOnBehalf) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = 16.dp
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
                                "Viewing appointments for",
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

            Text(
                text =
                    if (isActingOnBehalf) {
                        "$appointmentOwnerName's Appointments"
                    } else {
                        "Appointment History"
                    },
                fontSize =
                    22.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                modifier =
                    Modifier.padding(
                        bottom = 16.dp
                    )
            )

            when {

                isLoading -> {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color =
                                FormColors.green
                        )
                    }
                }

                loadError != null -> {

                    Text(
                        text =
                            "Failed to load appointments: $loadError",
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                        modifier =
                            Modifier.padding(
                                top = 16.dp
                            )
                    )
                }

                appointments.isEmpty() -> {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                if (isActingOnBehalf) {
                                    "No appointments found for $appointmentOwnerName."
                                } else {
                                    "No appointments found."
                                },
                            color =
                                Color.Gray
                        )
                    }
                }

                else -> {

                    LazyColumn(
                        verticalArrangement =
                            Arrangement.spacedBy(
                                12.dp
                            )
                    ) {

                        items(
                            appointments
                        ) { appointment ->

                            AppointmentItem(
                                appointment =
                                    appointment,
                                onClick = {
                                    selectedAppointment =
                                        appointment
                                }
                            )
                        }
                    }
                }
            }

        } else {

            /*
             * Appointment detail view.
             *
             * Back returns to the appointment
             * list instead of leaving the screen.
             */
            AppointmentDetailView(
                appointment =
                    selectedAppointment!!,
                onBack = {
                    selectedAppointment =
                        null
                }
            )
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun AppointmentItem(
    appointment: Appointment,
    onClick: () -> Unit
) {

    Card(
        onClick =
            onClick,
        modifier =
            Modifier.fillMaxWidth(),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    2.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            )
    ) {

        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        appointment.doctor,
                    fontWeight =
                        FontWeight.Bold,
                    fontSize =
                        18.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )

                Text(
                    text =
                        appointment.type,
                    color =
                        Color.Gray,
                    fontSize =
                        14.sp
                )

                Text(
                    text =
                        "${appointment.date} at ${appointment.time}",
                    fontSize =
                        14.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    modifier =
                        Modifier.padding(
                            top = 4.dp
                        )
                )
            }

            StatusBadge(
                status =
                    appointment.status
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: AppointmentStatus
) {

    val bg =
        when (status) {

            AppointmentStatus.Upcoming ->
                Color(0xFFE3F2FD)

            AppointmentStatus.Completed ->
                Color(0xFFE8F5E9)

            AppointmentStatus.Cancelled ->
                Color(0xFFFFEBEE)
        }

    val text =
        when (status) {

            AppointmentStatus.Upcoming ->
                Color(0xFF1976D2)

            AppointmentStatus.Completed ->
                Color(0xFF388E3C)

            AppointmentStatus.Cancelled ->
                Color(0xFFD32F2F)
        }

    Surface(
        color =
            bg,
        shape =
            RoundedCornerShape(
                16.dp
            )
    ) {

        Text(
            text =
                status.name,
            color =
                text,
            fontSize =
                12.sp,
            fontWeight =
                FontWeight.Medium,
            modifier =
                Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 4.dp
                )
        )
    }
}

@Composable
fun AppointmentDetailView(
    appointment: Appointment,
    onBack: () -> Unit
) {

    Column {

        Row(
            verticalAlignment =
                Alignment.CenterVertically,
            modifier =
                Modifier.padding(
                    bottom = 16.dp
                )
        ) {

            TextButton(
                onClick =
                    onBack
            ) {

                Text(
                    text =
                        "< Back",
                    color =
                        FormColors.green,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                Modifier.width(
                    8.dp
                )
            )

            Text(
                text =
                    "Appointment Details",
                fontSize =
                    20.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )
        }

        Card(
            modifier =
                Modifier.fillMaxWidth(),
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
                modifier =
                    Modifier.padding(
                        16.dp
                    )
            ) {

                DetailRow(
                    "Doctor",
                    appointment.doctor
                )

                DetailRow(
                    "Type",
                    appointment.type
                )

                DetailRow(
                    "Date",
                    appointment.date
                )

                DetailRow(
                    "Time",
                    appointment.time
                )

                Row(
                    modifier =
                        Modifier.padding(
                            vertical = 8.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "Status: ",
                        fontWeight =
                            FontWeight.Bold,
                        modifier =
                            Modifier.width(
                                100.dp
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    )

                    StatusBadge(
                        status =
                            appointment.status
                    )
                }

                if (
                    appointment.reason
                        .isNotEmpty()
                ) {

                    Spacer(
                        Modifier.height(
                            8.dp
                        )
                    )

                    Text(
                        text =
                            "Reason:",
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    )

                    Text(
                        text =
                            appointment.reason,
                        modifier =
                            Modifier.padding(
                                top = 4.dp
                            ),
                        color =
                            Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier.padding(
                vertical = 8.dp
            )
    ) {

        Text(
            text =
                "$label:",
            fontWeight =
                FontWeight.Bold,
            modifier =
                Modifier.width(
                    100.dp
                ),
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )

        Text(
            text =
                value,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )
    }
}