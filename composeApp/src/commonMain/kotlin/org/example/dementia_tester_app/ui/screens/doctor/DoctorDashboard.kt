package org.example.dementia_tester_app.ui.screens.doctor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
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

/**
 * Doctor Dashboard screen.
 *
 * This dashboard is shown to users with userType = DOCTOR.
 *
 * Current project implementation:
 * - DOCTOR also acts as ADMIN.
 * - Doctors only see patients assigned to them.
 * - Doctors can assign currently unassigned patients to themselves in the Assignments tab.
 * - Doctors/admins can assign caregivers to patients in the Assignments tab.
 */
@Composable
fun DoctorDashboard() {

    val menuItems = listOf(
        "View",
        "Compare",
        "Games",
        "Assignments"
    )

    var selectedMenuItem by remember {
        mutableStateOf(menuItems[0])
    }

    val headerColor =
        Color(0xFF66BB23)

    val activeMenuColor =
        headerColor

    /*
     * Patients assigned to the current doctor.
     */
    var userProfiles by remember {
        mutableStateOf<List<UserProfile>>(
            emptyList()
        )
    }

    var formattedUserList by remember {
        mutableStateOf<List<String>>(
            emptyList()
        )
    }

    var userMap by remember {
        mutableStateOf<Map<String, UserProfile>>(
            emptyMap()
        )
    }

    /*
     * Patients who do not currently
     * have an assigned doctor.
     */
    var unassignedPatients by remember {
        mutableStateOf<List<UserProfile>>(
            emptyList()
        )
    }

    /*
     * Patient selected for assignment
     * to the current doctor.
     */
    var selectedUnassignedPatient by remember {
        mutableStateOf<UserProfile?>(null)
    }

    /*
     * Controls the doctor-patient assignment
     * dropdown.
     */
    var assignmentMenuExpanded by remember {
        mutableStateOf(false)
    }

    /*
     * All caregiver accounts available
     * for assignment.
     */
    var caregivers by remember {
        mutableStateOf<List<UserProfile>>(
            emptyList()
        )
    }

    /*
     * Patient selected for caregiver assignment.
     *
     * This list uses patients already assigned
     * to the current doctor.
     */
    var selectedCaregiverPatient by remember {
        mutableStateOf<UserProfile?>(null)
    }

    /*
     * Caregiver selected for assignment.
     */
    var selectedCaregiver by remember {
        mutableStateOf<UserProfile?>(null)
    }

    /*
     * Dropdown states for caregiver assignment.
     */
    var caregiverPatientMenuExpanded by remember {
        mutableStateOf(false)
    }

    var caregiverMenuExpanded by remember {
        mutableStateOf(false)
    }

    /*
     * Main dashboard loading state.
     */
    var isLoading by remember {
        mutableStateOf(true)
    }

    /*
     * Loading state while assigning
     * a patient to the doctor.
     */
    var isAssigning by remember {
        mutableStateOf(false)
    }

    /*
     * Loading state while assigning
     * a caregiver to a patient.
     */
    var isAssigningCaregiver by remember {
        mutableStateOf(false)
    }

    /*
     * Error returned when loading
     * assigned patients.
     */
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Patient assignment result message.
     */
    var assignmentMessage by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Caregiver loading error.
     */
    var caregiverLoadMessage by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Caregiver assignment result message.
     */
    var caregiverAssignmentMessage by remember {
        mutableStateOf<String?>(null)
    }

    val userProfileService =
        remember {
            UserProfileService()
        }

    /**
     * Creates a readable label for
     * a patient or caregiver.
     */
    fun profileLabel(
        profile: UserProfile
    ): String {

        return when {

            profile.name.isNotBlank() &&
                    profile.email.isNotBlank() -> {

                "${profile.name} (${profile.email})"
            }

            profile.name.isNotBlank() -> {

                profile.name
            }

            profile.email.isNotBlank() -> {

                profile.email
            }

            else -> {

                profile.userId
            }
        }
    }

    /**
     * Load patients already assigned
     * to the current doctor.
     */
    fun loadAssignedPatients() {

        isLoading =
            true

        errorMessage =
            null

        userProfileService
            .getAllUsers { result ->

                isLoading =
                    false

                when (result) {

                    is DatabaseResult.Success -> {

                        userProfiles =
                            result.data

                        formattedUserList =
                            userProfiles.map {
                                profileLabel(it)
                            }

                        userMap =
                            userProfiles.associateBy {
                                profileLabel(it)
                            }

                        /*
                         * If the patient currently selected
                         * for caregiver assignment is no
                         * longer in this doctor's patient
                         * list, clear the selection.
                         */
                        val selectedPatientId =
                            selectedCaregiverPatient
                                ?.userId

                        if (
                            selectedPatientId != null &&
                            userProfiles.none {
                                it.userId ==
                                        selectedPatientId
                            }
                        ) {

                            selectedCaregiverPatient =
                                null
                        }
                    }

                    is DatabaseResult.Error -> {

                        errorMessage =
                            result.message

                        userProfiles =
                            emptyList()

                        formattedUserList =
                            emptyList()

                        userMap =
                            emptyMap()

                        selectedCaregiverPatient =
                            null
                    }
                }
            }
    }

    /**
     * Load patients who do not currently
     * have a doctor.
     */
    fun loadUnassignedPatients() {

        userProfileService
            .getUnassignedPatients { result ->

                when (result) {

                    is DatabaseResult.Success -> {

                        unassignedPatients =
                            result.data

                        val selectedId =
                            selectedUnassignedPatient
                                ?.userId

                        if (
                            selectedId != null &&
                            unassignedPatients.none {
                                it.userId ==
                                        selectedId
                            }
                        ) {

                            selectedUnassignedPatient =
                                null
                        }
                    }

                    is DatabaseResult.Error -> {

                        assignmentMessage =
                            "Unable to load unassigned patients: ${result.message}"
                    }
                }
            }
    }

    /**
     * Load all caregiver accounts.
     *
     * Only a doctor/admin can perform
     * this operation.
     */
    fun loadCaregivers() {

        caregiverLoadMessage =
            null

        userProfileService
            .getAllCaregivers { result ->

                when (result) {

                    is DatabaseResult.Success -> {

                        caregivers =
                            result.data

                        val selectedId =
                            selectedCaregiver
                                ?.userId

                        if (
                            selectedId != null &&
                            caregivers.none {
                                it.userId ==
                                        selectedId
                            }
                        ) {

                            selectedCaregiver =
                                null
                        }
                    }

                    is DatabaseResult.Error -> {

                        caregivers =
                            emptyList()

                        selectedCaregiver =
                            null

                        caregiverLoadMessage =
                            "Unable to load caregivers: ${result.message}"
                    }
                }
            }
    }

    /**
     * Refresh all doctor/admin dashboard
     * assignment data.
     */
    fun refreshDashboard() {

        loadAssignedPatients()
        loadUnassignedPatients()
        loadCaregivers()
    }

    /*
     * Load data when the dashboard opens.
     */
    LaunchedEffect(Unit) {

        refreshDashboard()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        /*
         * Existing doctor dashboard menu with Assignments tab.
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
            }
        )

        /*
         * Doctor dashboard content.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 16.dp),
            contentAlignment =
                Alignment.TopCenter
        ) {

            when {

                isLoading -> {

                    LoadingSpinner()
                }

                selectedMenuItem == "Assignments" -> {

                    DoctorAssignmentsScreen(
                        unassignedPatients = unassignedPatients,
                        selectedUnassignedPatient = selectedUnassignedPatient,
                        assignmentMenuExpanded = assignmentMenuExpanded,
                        onAssignmentMenuExpandedChange = { assignmentMenuExpanded = it },
                        onSelectUnassignedPatient = { selectedUnassignedPatient = it },
                        isAssigning = isAssigning,
                        assignmentMessage = assignmentMessage,
                        onClearAssignmentMessage = { assignmentMessage = null },
                        onAssignPatient = { patient ->
                            isAssigning = true
                            assignmentMessage = null

                            userProfileService.assignPatientToCurrentDoctor(
                                patientId = patient.userId
                            ) { result ->
                                isAssigning = false
                                when (result) {
                                    is DatabaseResult.Success -> {
                                        assignmentMessage =
                                            "${profileLabel(patient)} assigned successfully."
                                        selectedUnassignedPatient = null
                                        refreshDashboard()
                                    }
                                    is DatabaseResult.Error -> {
                                        assignmentMessage =
                                            "Assignment failed: ${result.message}"
                                    }
                                }
                            }
                        },
                        userProfiles = userProfiles,
                        selectedCaregiverPatient = selectedCaregiverPatient,
                        caregiverPatientMenuExpanded = caregiverPatientMenuExpanded,
                        onCaregiverPatientMenuExpandedChange = { caregiverPatientMenuExpanded = it },
                        onSelectCaregiverPatient = { selectedCaregiverPatient = it },
                        caregivers = caregivers,
                        selectedCaregiver = selectedCaregiver,
                        caregiverMenuExpanded = caregiverMenuExpanded,
                        onCaregiverMenuExpandedChange = { caregiverMenuExpanded = it },
                        onSelectCaregiver = { selectedCaregiver = it },
                        caregiverLoadMessage = caregiverLoadMessage,
                        isAssigningCaregiver = isAssigningCaregiver,
                        caregiverAssignmentMessage = caregiverAssignmentMessage,
                        onClearCaregiverAssignmentMessage = { caregiverAssignmentMessage = null },
                        onAssignCaregiver = { patient, caregiver ->
                            isAssigningCaregiver = true
                            caregiverAssignmentMessage = null

                            userProfileService.assignCaregiverToPatient(
                                patientId = patient.userId,
                                caregiverId = caregiver.userId
                            ) { result ->
                                isAssigningCaregiver = false
                                when (result) {
                                    is DatabaseResult.Success -> {
                                        caregiverAssignmentMessage =
                                            "${profileLabel(caregiver)} assigned to ${profileLabel(patient)} successfully."
                                        selectedCaregiverPatient = null
                                        selectedCaregiver = null
                                        refreshDashboard()
                                    }
                                    is DatabaseResult.Error -> {
                                        caregiverAssignmentMessage =
                                            "Caregiver assignment failed: ${result.message}"
                                    }
                                }
                            }
                        },
                        profileLabel = { profileLabel(it) }
                    )
                }

                errorMessage != null -> {

                    Text(
                        text =
                            "Error loading patients: $errorMessage",
                        color =
                            Color.Red,
                        textAlign =
                            TextAlign.Center,
                        modifier =
                            Modifier.padding(
                                16.dp
                            )
                    )
                }

                userProfiles.isEmpty() -> {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text =
                                "No patients are currently assigned to you.",
                            textAlign =
                                TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text =
                                "Go to the Assignments tab to assign patients to your list.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {

                    when (
                        selectedMenuItem
                    ) {

                        "View" -> {

                            DoctorViewScreen(
                                formattedUserList,
                                userMap
                            )
                        }

                        "Compare" -> {

                            DoctorCompareScreen(
                                formattedUserList,
                                userMap
                            )
                        }

                        "Games" -> {

                            DoctorGamesScreen(
                                formattedUserList,
                                userMap
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Assignments tab screen containing Doctor-Patient and Caregiver-Patient assignment cards.
 */
@Composable
private fun DoctorAssignmentsScreen(
    unassignedPatients: List<UserProfile>,
    selectedUnassignedPatient: UserProfile?,
    assignmentMenuExpanded: Boolean,
    onAssignmentMenuExpandedChange: (Boolean) -> Unit,
    onSelectUnassignedPatient: (UserProfile) -> Unit,
    isAssigning: Boolean,
    assignmentMessage: String?,
    onClearAssignmentMessage: () -> Unit,
    onAssignPatient: (UserProfile) -> Unit,
    userProfiles: List<UserProfile>,
    selectedCaregiverPatient: UserProfile?,
    caregiverPatientMenuExpanded: Boolean,
    onCaregiverPatientMenuExpandedChange: (Boolean) -> Unit,
    onSelectCaregiverPatient: (UserProfile) -> Unit,
    caregivers: List<UserProfile>,
    selectedCaregiver: UserProfile?,
    caregiverMenuExpanded: Boolean,
    onCaregiverMenuExpandedChange: (Boolean) -> Unit,
    onSelectCaregiver: (UserProfile) -> Unit,
    caregiverLoadMessage: String?,
    isAssigningCaregiver: Boolean,
    caregiverAssignmentMessage: String?,
    onClearCaregiverAssignmentMessage: () -> Unit,
    onAssignCaregiver: (UserProfile, UserProfile) -> Unit,
    profileLabel: (UserProfile) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        /*
         * Doctor -> Patient assignment.
         */
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Assign Patient",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Select an unassigned patient to add them to your patient list.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (unassignedPatients.isEmpty()) {
                    Text(
                        text = "There are currently no unassigned patients.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onAssignmentMenuExpandedChange(true) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedUnassignedPatient?.let { profileLabel(it) }
                                    ?: "Select patient"
                            )
                        }

                        DropdownMenu(
                            expanded = assignmentMenuExpanded,
                            onDismissRequest = { onAssignmentMenuExpandedChange(false) }
                        ) {
                            unassignedPatients.forEach { patient ->
                                DropdownMenuItem(
                                    text = {
                                        Text(profileLabel(patient))
                                    },
                                    onClick = {
                                        onSelectUnassignedPatient(patient)
                                        onAssignmentMenuExpandedChange(false)
                                        onClearAssignmentMessage()
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val patient = selectedUnassignedPatient ?: return@Button
                            onAssignPatient(patient)
                        },
                        enabled = selectedUnassignedPatient != null && !isAssigning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAssigning) {
                            Text("Assigning...")
                        } else {
                            Text("Assign Patient")
                        }
                    }
                }

                assignmentMessage?.let { message ->
                    val isError = message.startsWith("Assignment failed") ||
                            message.startsWith("Unable")

                    Text(
                        text = message,
                        color = if (isError) Color.Red else Color(0xFF388E3C),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        /*
         * Doctor/Admin -> Caregiver assignment.
         */
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Assign Caregiver",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Select one of your patients and assign a caregiver to support them.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Patient",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                if (userProfiles.isEmpty()) {
                    Text(
                        text = "You do not currently have any assigned patients.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onCaregiverPatientMenuExpandedChange(true) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedCaregiverPatient?.let { profileLabel(it) }
                                    ?: "Select patient"
                            )
                        }

                        DropdownMenu(
                            expanded = caregiverPatientMenuExpanded,
                            onDismissRequest = { onCaregiverPatientMenuExpandedChange(false) }
                        ) {
                            userProfiles.forEach { patient ->
                                DropdownMenuItem(
                                    text = {
                                        Text(profileLabel(patient))
                                    },
                                    onClick = {
                                        onSelectCaregiverPatient(patient)
                                        onCaregiverPatientMenuExpandedChange(false)
                                        onClearCaregiverAssignmentMessage()
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Caregiver",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                caregiverLoadMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color.Red
                    )
                }

                if (caregivers.isEmpty() && caregiverLoadMessage == null) {
                    Text(
                        text = "No caregiver accounts are currently available.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (caregivers.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onCaregiverMenuExpandedChange(true) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedCaregiver?.let { profileLabel(it) }
                                    ?: "Select caregiver"
                            )
                        }

                        DropdownMenu(
                            expanded = caregiverMenuExpanded,
                            onDismissRequest = { onCaregiverMenuExpandedChange(false) }
                        ) {
                            caregivers.forEach { caregiver ->
                                DropdownMenuItem(
                                    text = {
                                        Text(profileLabel(caregiver))
                                    },
                                    onClick = {
                                        onSelectCaregiver(caregiver)
                                        onCaregiverMenuExpandedChange(false)
                                        onClearCaregiverAssignmentMessage()
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val patient = selectedCaregiverPatient ?: return@Button
                        val caregiver = selectedCaregiver ?: return@Button
                        onAssignCaregiver(patient, caregiver)
                    },
                    enabled = selectedCaregiverPatient != null &&
                            selectedCaregiver != null &&
                            !isAssigningCaregiver,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAssigningCaregiver) {
                        Text("Assigning caregiver...")
                    } else {
                        Text("Assign Caregiver")
                    }
                }

                caregiverAssignmentMessage?.let { message ->
                    val isError = message.startsWith("Caregiver assignment failed")

                    Text(
                        text = message,
                        color = if (isError) Color.Red else Color(0xFF388E3C),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
