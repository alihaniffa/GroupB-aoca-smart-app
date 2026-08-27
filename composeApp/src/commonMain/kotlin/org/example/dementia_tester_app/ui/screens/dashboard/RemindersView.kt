package org.example.dementia_tester_app.ui.screens.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.dementia_tester_app.auth.AuthService
import org.example.dementia_tester_app.data.Reminder
import org.example.dementia_tester_app.data.ReminderResult
import org.example.dementia_tester_app.data.ReminderService
import org.example.dementia_tester_app.ui.components.LoadingSpinner
import org.example.dementia_tester_app.ui.components.ReminderCard
import org.example.dementia_tester_app.ui.components.ReminderFormDialog

/**
 * View screen for reminders.
 *
 * Normal patient use:
 *
 * RemindersView()
 *
 * Caregiver use:
 *
 * RemindersView(
 *     targetUserId = selectedPatient.userId,
 *     targetUserName = selectedPatient.name
 * )
 *
 * When targetUserId is provided, reminders are
 * loaded and managed for the selected patient.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersView(
    targetUserId: String? = null,
    targetUserName: String? = null
) {

    var showDialog by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val reminders =
        remember {
            mutableStateListOf<Reminder>()
        }

    val reminderService =
        remember {
            ReminderService()
        }

    val authService =
        remember {
            AuthService()
        }

    /*
     * UID of the account actually logged in.
     */
    val loggedInUserId =
        authService.getCurrentUserId()

    /*
     * UID whose reminders should be managed.
     *
     * Patient:
     * targetUserId == null
     * -> use logged-in patient's UID
     *
     * Caregiver:
     * targetUserId != null
     * -> use selected patient's UID
     */
    val userId =
        targetUserId
            ?: loggedInUserId

    /*
     * True when a caregiver is acting
     * on behalf of a patient.
     */
    val isActingOnBehalf =
        targetUserId != null &&
                targetUserId != loggedInUserId

    /*
     * Patient name shown in caregiver mode.
     */
    val reminderOwnerName =
        targetUserName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Patient"

    val scrollState =
        rememberScrollState()

    /*
     * Load reminders whenever the target
     * patient changes.
     */
    LaunchedEffect(userId) {

        reminders.clear()
        errorMessage = null

        if (userId != null) {

            loadReminders(
                reminderService = reminderService,
                userId = userId,
                reminders = reminders,
                setLoading = {
                    isLoading = it
                },
                onError = {
                    errorMessage = it
                }
            )

        } else {

            errorMessage =
                "Unable to determine user."
        }
    }

    if (isLoading) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 16.dp
                )
                .verticalScroll(
                    scrollState
                ),
            contentAlignment =
                Alignment.Center
        ) {

            LoadingSpinner()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        /*
         * Caregiver context card.
         */
        if (isActingOnBehalf) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = 12.dp
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
                            "Managing reminders for",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )

                    Text(
                        text =
                            reminderOwnerName,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text =
                if (isActingOnBehalf) {
                    "$reminderOwnerName's Reminders"
                } else {
                    "Your Reminders"
                },
            fontSize =
                24.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )

        Text(
            text =
                if (isActingOnBehalf) {
                    "Create and manage reminders for $reminderOwnerName."
                } else {
                    "Create and view your reminders"
                },
            fontSize =
                16.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )

        /*
         * Create reminder button.
         */
        Button(
            onClick = {
                showDialog = true
            },
            shape =
                RoundedCornerShape(
                    8.dp
                ),
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    80.dp
                )
                .padding(
                    vertical = 16.dp
                ),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    contentColor =
                        MaterialTheme
                            .colorScheme
                            .onPrimary
                ),
            enabled =
                userId != null &&
                        !isLoading
        ) {

            Text(
                text =
                    if (isActingOnBehalf) {
                        "CREATE PATIENT REMINDER"
                    } else {
                        "CREATE REMINDER"
                    }
            )
        }

        errorMessage
            ?.let { message ->

                Text(
                    text =
                        message,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error,
                    modifier =
                        Modifier.padding(
                            bottom = 12.dp
                        )
                )
            }

        if (
            !isLoading &&
            reminders.isEmpty()
        ) {

            Text(
                text =
                    if (isActingOnBehalf) {
                        "$reminderOwnerName doesn't have any reminders yet."
                    } else {
                        "You don't have any reminders yet."
                    },
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

        } else {

            LazyColumn(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                items(
                    reminders
                ) { reminder ->

                    ReminderCard(
                        reminder =
                            reminder,

                        onToggle = {
                                id,
                                newValue ->

                            if (
                                userId != null
                            ) {

                                /*
                                 * Caregiver mode:
                                 * update selected patient's reminder.
                                 *
                                 * Normal patient mode:
                                 * use existing update method.
                                 */
                                if (isActingOnBehalf) {

                                    reminderService
                                        .updateReminderForUser(
                                            userId =
                                                userId,
                                            reminderId =
                                                id,
                                            updates =
                                                mapOf(
                                                    "taskActive" to
                                                            newValue
                                                )
                                        ) { result ->

                                            when (result) {

                                                is ReminderResult.Success -> {

                                                    loadReminders(
                                                        reminderService =
                                                            reminderService,
                                                        userId =
                                                            userId,
                                                        reminders =
                                                            reminders,
                                                        setLoading = {
                                                            isLoading = it
                                                        },
                                                        onError = {
                                                            errorMessage = it
                                                        }
                                                    )
                                                }

                                                is ReminderResult.Error -> {

                                                    errorMessage =
                                                        result.message
                                                }
                                            }
                                        }

                                } else {

                                    reminderService
                                        .updateReminder(
                                            id,
                                            mapOf(
                                                "taskActive" to
                                                        newValue
                                            )
                                        ) { result ->

                                            when (result) {

                                                is ReminderResult.Success -> {

                                                    loadReminders(
                                                        reminderService =
                                                            reminderService,
                                                        userId =
                                                            userId,
                                                        reminders =
                                                            reminders,
                                                        setLoading = {
                                                            isLoading = it
                                                        },
                                                        onError = {
                                                            errorMessage = it
                                                        }
                                                    )
                                                }

                                                is ReminderResult.Error -> {

                                                    errorMessage =
                                                        result.message
                                                }
                                            }
                                        }
                                }
                            }
                        },

                        onDelete = { id ->

                            if (
                                userId != null
                            ) {

                                /*
                                 * Caregiver mode:
                                 * delete selected patient's reminder.
                                 */
                                if (isActingOnBehalf) {

                                    reminderService
                                        .deleteReminderForUser(
                                            userId =
                                                userId,
                                            reminderID =
                                                id
                                        ) { result ->

                                            when (result) {

                                                is ReminderResult.Success -> {

                                                    loadReminders(
                                                        reminderService =
                                                            reminderService,
                                                        userId =
                                                            userId,
                                                        reminders =
                                                            reminders,
                                                        setLoading = {
                                                            isLoading = it
                                                        },
                                                        onError = {
                                                            errorMessage = it
                                                        }
                                                    )
                                                }

                                                is ReminderResult.Error -> {

                                                    errorMessage =
                                                        result.message
                                                }
                                            }
                                        }

                                } else {

                                    reminderService
                                        .deleteReminder(
                                            id
                                        ) { result ->

                                            when (result) {

                                                is ReminderResult.Success -> {

                                                    loadReminders(
                                                        reminderService =
                                                            reminderService,
                                                        userId =
                                                            userId,
                                                        reminders =
                                                            reminders,
                                                        setLoading = {
                                                            isLoading = it
                                                        },
                                                        onError = {
                                                            errorMessage = it
                                                        }
                                                    )
                                                }

                                                is ReminderResult.Error -> {

                                                    errorMessage =
                                                        result.message
                                                }
                                            }
                                        }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    /*
     * Create reminder dialog.
     */
    if (showDialog) {

        ReminderFormDialog(
            onDismiss = {

                showDialog =
                    false
            },

            onCreate = {
                    newReminder ->

                if (
                    userId != null
                ) {

                    /*
                     * Caregiver mode:
                     * save reminder under selected
                     * patient's UID.
                     */
                    if (isActingOnBehalf) {

                        reminderService
                            .createReminderForUser(
                                userId =
                                    userId,
                                reminder =
                                    newReminder
                            ) { result ->

                                when (result) {

                                    is ReminderResult.Success -> {

                                        loadReminders(
                                            reminderService =
                                                reminderService,
                                            userId =
                                                userId,
                                            reminders =
                                                reminders,
                                            setLoading = {
                                                isLoading = it
                                            },
                                            onError = {
                                                errorMessage = it
                                            }
                                        )
                                    }

                                    is ReminderResult.Error -> {

                                        errorMessage =
                                            result.message
                                    }
                                }
                            }

                    } else {

                        reminderService
                            .createReminder(
                                newReminder
                            ) { result ->

                                when (result) {

                                    is ReminderResult.Success -> {

                                        loadReminders(
                                            reminderService =
                                                reminderService,
                                            userId =
                                                userId,
                                            reminders =
                                                reminders,
                                            setLoading = {
                                                isLoading = it
                                            },
                                            onError = {
                                                errorMessage = it
                                            }
                                        )
                                    }

                                    is ReminderResult.Error -> {

                                        errorMessage =
                                            result.message
                                    }
                                }
                            }
                    }
                }

                showDialog =
                    false
            }
        )
    }
}

/**
 * Load reminders belonging to the specified user.
 */
private fun loadReminders(
    reminderService: ReminderService,
    userId: String,
    reminders: SnapshotStateList<Reminder>,
    setLoading: (Boolean) -> Unit,
    onError: (String?) -> Unit
) {

    setLoading(true)
    onError(null)

    reminderService
        .getReminders(
            userId
        ) { result ->

            setLoading(false)

            when (result) {

                is ReminderResult.Success -> {

                    reminders.clear()

                    reminders.addAll(
                        result.data
                    )
                }

                is ReminderResult.Error -> {

                    reminders.clear()

                    onError(
                        result.message
                    )
                }
            }
        }
}