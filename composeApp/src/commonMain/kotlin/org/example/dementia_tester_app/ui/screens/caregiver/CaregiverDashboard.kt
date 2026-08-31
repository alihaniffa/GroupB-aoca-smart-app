package org.example.dementia_tester_app.ui.screens.caregiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.example.dementia_tester_app.data.DatabaseResult
import org.example.dementia_tester_app.data.UserProfile
import org.example.dementia_tester_app.data.UserProfileService
import org.example.dementia_tester_app.ui.components.HorizontalMenu
import org.example.dementia_tester_app.ui.components.LoadingSpinner
import org.example.dementia_tester_app.ui.screens.AppointmentHistory
import org.example.dementia_tester_app.ui.screens.BookAppointment
import org.example.dementia_tester_app.ui.screens.FocusFlick
import org.example.dementia_tester_app.ui.screens.TaskSwitch
import org.example.dementia_tester_app.ui.screens.WordRecall
import org.example.dementia_tester_app.ui.screens.dashboard.GamesView
import org.example.dementia_tester_app.ui.screens.dashboard.ProgressView
import org.example.dementia_tester_app.ui.screens.dashboard.RemindersView
import org.example.dementia_tester_app.ui.screens.dashboard.TestView

/**
 * Dashboard shown to users with the CAREGIVER role.
 *
 * The caregiver works on behalf of a selected
 * patient who has been assigned to them.
 */
@Composable
fun CaregiverDashboard() {

    /*
     * Main caregiver dashboard sections.
     */
    val menuItems = listOf(
        "Reminders",
        "Test",
        "Games",
        "Progress",
        "Appointments"
    )

    var selectedMenuItem by remember {
        mutableStateOf(
            menuItems[0]
        )
    }

    /*
     * Stores the currently selected mini game.
     *
     * Empty string means the game selection
     * screen should be displayed.
     */
    var currentGame by remember {
        mutableStateOf("")
    }

    /*
     * Appointment section mode.
     *
     * false -> appointment history
     * true  -> book appointment
     */
    var showBookAppointment by remember {
        mutableStateOf(false)
    }

    val activeMenuColor =
        Color(0xFF66BB23)

    /*
     * Service used to retrieve patients
     * assigned to the logged-in caregiver.
     */
    val userProfileService =
        remember {
            UserProfileService()
        }

    /*
     * All patients assigned to
     * the current caregiver.
     */
    var assignedPatients by remember {
        mutableStateOf<List<UserProfile>>(
            emptyList()
        )
    }

    /*
     * The patient the caregiver is
     * currently managing.
     */
    var selectedPatient by remember {
        mutableStateOf<UserProfile?>(null)
    }

    /*
     * Controls the patient selector dropdown.
     */
    var patientMenuExpanded by remember {
        mutableStateOf(false)
    }

    /*
     * Loading state.
     */
    var isLoading by remember {
        mutableStateOf(true)
    }

    /*
     * Firebase/backend error.
     */
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    /**
     * Create a readable patient label.
     */
    fun patientLabel(
        patient: UserProfile
    ): String {

        return when {

            patient.name.isNotBlank() &&
                    patient.email.isNotBlank() -> {

                "${patient.name} (${patient.email})"
            }

            patient.name.isNotBlank() -> {

                patient.name
            }

            patient.email.isNotBlank() -> {

                patient.email
            }

            else -> {

                "Patient"
            }
        }
    }

    /*
     * Load patients assigned to
     * the current caregiver.
     */
    LaunchedEffect(Unit) {

        userProfileService
            .getPatientsForCurrentCaregiver { result ->

                isLoading =
                    false

                when (result) {

                    is DatabaseResult.Success -> {

                        assignedPatients =
                            result.data

                        errorMessage =
                            null

                        /*
                         * Automatically select the
                         * first assigned patient.
                         */
                        if (
                            selectedPatient == null &&
                            assignedPatients.isNotEmpty()
                        ) {

                            selectedPatient =
                                assignedPatients.first()
                        }
                    }

                    is DatabaseResult.Error -> {

                        assignedPatients =
                            emptyList()

                        selectedPatient =
                            null

                        errorMessage =
                            result.message
                    }
                }
            }
    }

    /*
     * Reset patient-specific screen state whenever
     * the caregiver selects another patient.
     */
    LaunchedEffect(
        selectedPatient?.userId
    ) {

        currentGame =
            ""

        showBookAppointment =
            false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        /*
         * Dashboard heading.
         */
        Text(
            text =
                "Caregiver Dashboard",
            style =
                MaterialTheme
                    .typography
                    .headlineMedium,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                "Manage and support your assigned patient.",
            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )

        when {

            /*
             * Loading assigned patients.
             */
            isLoading -> {

                LoadingSpinner()
            }

            /*
             * Error retrieving patients.
             */
            errorMessage != null -> {

                Text(
                    text =
                        "Unable to load assigned patients: $errorMessage",
                    color =
                        Color.Red,
                    textAlign =
                        TextAlign.Center,
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            /*
             * No patients assigned.
             */
            assignedPatients.isEmpty() -> {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                        )
                ) {

                    Text(
                        text =
                            "No patients are currently assigned to you.",
                        modifier =
                            Modifier.padding(16.dp)
                    )
                }
            }

            /*
             * Caregiver has assigned patient(s).
             */
            else -> {

                /*
                 * Patient context card.
                 */
                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
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
                            .padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Text(
                            text =
                                "Managing Patient",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )

                        /*
                         * Patient selector.
                         */
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            OutlinedButton(
                                onClick = {

                                    patientMenuExpanded =
                                        true
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    text =
                                        selectedPatient
                                            ?.let {
                                                patientLabel(it)
                                            }
                                            ?: "Select patient"
                                )
                            }

                            DropdownMenu(
                                expanded =
                                    patientMenuExpanded,
                                onDismissRequest = {

                                    patientMenuExpanded =
                                        false
                                }
                            ) {

                                assignedPatients
                                    .forEach { patient ->

                                        DropdownMenuItem(
                                            text = {

                                                Text(
                                                    text =
                                                        patientLabel(
                                                            patient
                                                        )
                                                )
                                            },
                                            onClick = {

                                                selectedPatient =
                                                    patient

                                                currentGame =
                                                    ""

                                                showBookAppointment =
                                                    false

                                                patientMenuExpanded =
                                                    false
                                            }
                                        )
                                    }
                            }
                        }

                        /*
                         * Selected patient's basic information.
                         */
                        selectedPatient
                            ?.let { patient ->

                                if (
                                    patient.name.isNotBlank()
                                ) {

                                    Text(
                                        text =
                                            patient.name,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .titleMedium,
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }

                                if (
                                    patient.email.isNotBlank()
                                ) {

                                    Text(
                                        text =
                                            patient.email,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodyMedium
                                    )
                                }
                            }
                    }
                }

                /*
                 * Caregiver feature navigation.
                 */
                HorizontalMenu(
                    menuItems =
                        menuItems,
                    selectedMenuItem =
                        selectedMenuItem,
                    activeColor =
                        activeMenuColor,
                    onMenuItemSelected = {

                        selectedMenuItem =
                            it

                        /*
                         * Leaving Games resets
                         * the current game.
                         */
                        if (
                            it != "Games"
                        ) {

                            currentGame =
                                ""
                        }

                        /*
                         * Leaving Appointments resets
                         * appointment view to history.
                         */
                        if (
                            it != "Appointments"
                        ) {

                            showBookAppointment =
                                false
                        }
                    }
                )

                /*
                 * Caregiver feature content.
                 *
                 * Every feature receives the
                 * selected patient's UID.
                 */
                selectedPatient
                    ?.let { patient ->

                        when (
                            selectedMenuItem
                        ) {

                            /*
                             * View, create, activate,
                             * deactivate and delete reminders
                             * for the selected patient.
                             */
                            "Reminders" -> {

                                currentGame =
                                    ""

                                RemindersView(
                                    targetUserId =
                                        patient.userId,
                                    targetUserName =
                                        patient.name
                                )
                            }

                            /*
                             * The caregiver can take a cognitive
                             * assessment on behalf of the patient.
                             */
                            "Test" -> {

                                currentGame =
                                    ""

                                TestView(
                                    targetUserId =
                                        patient.userId,
                                    targetUserName =
                                        patient.name
                                )
                            }

                            /*
                             * Caregiver Games.
                             */
                            "Games" -> {

                                if (
                                    currentGame.isBlank()
                                ) {

                                    Column {

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    bottom = 8.dp
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
                                                        "Playing games on behalf of",
                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .bodyMedium
                                                )

                                                Text(
                                                    text =
                                                        patient.name
                                                            .ifBlank {
                                                                "Patient"
                                                            },
                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .titleMedium,
                                                    fontWeight =
                                                        FontWeight.Bold
                                                )
                                            }
                                        }

                                        GamesView { game ->

                                            if (
                                                game.isNotBlank()
                                            ) {

                                                currentGame =
                                                    game
                                            }
                                        }
                                    }

                                } else {

                                    when (
                                        currentGame
                                    ) {

                                        "FocusFlick" -> {

                                            FocusFlick(
                                                onReturn = {

                                                    currentGame =
                                                        ""
                                                },
                                                targetUserId =
                                                    patient.userId,
                                                targetUserName =
                                                    patient.name
                                            )
                                        }

                                        "TaskSwitch" -> {

                                            TaskSwitch(
                                                onReturn = {

                                                    currentGame =
                                                        ""
                                                },
                                                targetUserId =
                                                    patient.userId,
                                                targetUserName =
                                                    patient.name
                                            )
                                        }

                                        "WordRecall" -> {

                                            WordRecall(
                                                onReturn = {

                                                    currentGame =
                                                        ""
                                                },
                                                targetUserId =
                                                    patient.userId,
                                                targetUserName =
                                                    patient.name
                                            )
                                        }

                                        else -> {

                                            currentGame =
                                                ""
                                        }
                                    }
                                }
                            }

                            /*
                             * Show the selected patient's
                             * assessment, survey, game and
                             * recent activity progress.
                             */
                            "Progress" -> {

                                currentGame =
                                    ""

                                ProgressView(
                                    targetUserId =
                                        patient.userId,
                                    targetUserName =
                                        patient.name
                                )
                            }

                            /*
                             * View and book appointments
                             * for the selected patient.
                             */
                            "Appointments" -> {

                                currentGame =
                                    ""

                                Column(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            12.dp
                                        )
                                ) {

                                    Card(
                                        modifier =
                                            Modifier.fillMaxWidth(),
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
                                                    "Managing appointments for",
                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodyMedium
                                            )

                                            Text(
                                                text =
                                                    patient.name
                                                        .ifBlank {
                                                            "Patient"
                                                        },
                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .titleMedium,
                                                fontWeight =
                                                    FontWeight.Bold
                                            )
                                        }
                                    }

                                    /*
                                     * Appointment navigation.
                                     */
                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(
                                                8.dp
                                            )
                                    ) {

                                        Button(
                                            onClick = {

                                                showBookAppointment =
                                                    false
                                            },
                                            modifier =
                                                Modifier.weight(1f),
                                            colors =
                                                ButtonDefaults.buttonColors(
                                                    containerColor =
                                                        if (
                                                            !showBookAppointment
                                                        ) {
                                                            activeMenuColor
                                                        } else {
                                                            MaterialTheme
                                                                .colorScheme
                                                                .surfaceVariant
                                                        },
                                                    contentColor =
                                                        if (
                                                            !showBookAppointment
                                                        ) {
                                                            Color.White
                                                        } else {
                                                            MaterialTheme
                                                                .colorScheme
                                                                .onSurfaceVariant
                                                        }
                                                )
                                        ) {

                                            Text(
                                                "History"
                                            )
                                        }

                                        Button(
                                            onClick = {

                                                showBookAppointment =
                                                    true
                                            },
                                            modifier =
                                                Modifier.weight(1f),
                                            colors =
                                                ButtonDefaults.buttonColors(
                                                    containerColor =
                                                        if (
                                                            showBookAppointment
                                                        ) {
                                                            activeMenuColor
                                                        } else {
                                                            MaterialTheme
                                                                .colorScheme
                                                                .surfaceVariant
                                                        },
                                                    contentColor =
                                                        if (
                                                            showBookAppointment
                                                        ) {
                                                            Color.White
                                                        } else {
                                                            MaterialTheme
                                                                .colorScheme
                                                                .onSurfaceVariant
                                                        }
                                                )
                                        ) {

                                            Text(
                                                "Book Appointment"
                                            )
                                        }
                                    }

                                    if (
                                        showBookAppointment
                                    ) {

                                        BookAppointment(
                                            targetUserId =
                                                patient.userId,
                                            targetUserName =
                                                patient.name,
                                            targetUserEmail =
                                                patient.email,
                                            onCancel = {

                                                showBookAppointment =
                                                    false
                                            },
                                            onSuccess = {

                                                /*
                                                 * Return to history after
                                                 * successful booking.
                                                 */
                                                showBookAppointment =
                                                    false
                                            }
                                        )

                                    } else {

                                        AppointmentHistory(
                                            targetUserId =
                                                patient.userId,
                                            targetUserName =
                                                patient.name
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}