package org.example.dementia_tester_app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

/**
 * Android implementation of UserProfileService using Firebase
 * Realtime Database and Firebase Storage.
 */
actual class UserProfileService actual constructor() : UserProfileServiceInterface {

    private val auth = FirebaseAuth.getInstance()
    private val database = Firebase.database.reference
    private val storage = Firebase.storage

    private val dbPath = "UserProfiles"
    private val caregiverPatientsPath = "CaregiverPatients"

    /**
     * Get the current signed-in user's profile.
     */
    actual override fun getCurrentUserProfile(
        callback: (DatabaseResult<UserProfile>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        database
            .child(dbPath)
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    callback(
                        DatabaseResult.Error(
                            "Profile not found. Please create a profile."
                        )
                    )
                    return@addOnSuccessListener
                }

                try {
                    val data =
                        snapshot.value as? Map<*, *>

                    if (data != null) {
                        callback(
                            DatabaseResult.Success(
                                UserProfile.fromMap(
                                    data,
                                    userId
                                )
                            )
                        )
                    } else {
                        callback(
                            DatabaseResult.Error(
                                "Profile data is empty"
                            )
                        )
                    }

                } catch (e: Exception) {
                    callback(
                        DatabaseResult.Error(
                            "Failed to parse user profile: ${e.message}"
                        )
                    )
                }
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Failed to get user profile: ${e.message}"
                    )
                )
            }
    }

    /**
     * Create or update the current signed-in user's profile.
     */
    actual override fun updateUserProfile(
        userProfile: UserProfile,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        val currentUser =
            auth.currentUser

        if (currentUser == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        val userId =
            currentUser.uid

        val profileRef =
            database
                .child(dbPath)
                .child(userId)

        profileRef
            .get()
            .addOnSuccessListener { snapshot ->

                val updates =
                    userProfile
                        .toMap()
                        .toMutableMap()

                // Always use the authenticated user's UID.
                updates["userId"] =
                    userId

                // Updated whenever the profile is saved.
                updates["updatedAt"] =
                    ServerValue.TIMESTAMP

                if (snapshot.exists()) {

                    val data =
                        snapshot.value as? Map<*, *>

                    // Preserve the existing user role.
                    val existingUserType =
                        data
                            ?.get("userType")
                            ?.toString()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: UserType.USER.value

                    // Preserve the existing email.
                    val existingEmail =
                        data
                            ?.get("email")
                            ?.toString()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: currentUser
                                .email
                                .orEmpty()

                    // Preserve doctor relationship.
                    val existingAssignedDoctorId =
                        data
                            ?.get("assignedDoctorId")
                            ?.toString()
                            .orEmpty()

                    // Preserve caregiver relationship.
                    val existingAssignedCaregiverId =
                        data
                            ?.get("assignedCaregiverId")
                            ?.toString()
                            .orEmpty()

                    updates["userType"] =
                        existingUserType

                    updates["email"] =
                        existingEmail

                    /*
                     * Preserve both assignment relationships when
                     * users update their own profile.
                     */
                    updates["assignedDoctorId"] =
                        existingAssignedDoctorId

                    updates["assignedCaregiverId"] =
                        existingAssignedCaregiverId

                    // Preserve original creation timestamp.
                    val existingCreatedAt =
                        data?.get("createdAt")

                    if (
                        existingCreatedAt != null
                    ) {
                        updates["createdAt"] =
                            existingCreatedAt
                    } else {
                        updates["createdAt"] =
                            ServerValue.TIMESTAMP
                    }

                } else {

                    val profileEmail =
                        updates["email"] as? String

                    if (
                        profileEmail.isNullOrBlank()
                    ) {
                        updates["email"] =
                            currentUser
                                .email
                                .orEmpty()
                    }

                    /*
                     * New profiles begin as regular USER/patient
                     * profiles.
                     *
                     * DOCTOR = clinician + admin for now.
                     * CAREGIVER remains a separate user type.
                     */
                    updates["userType"] =
                        UserType.USER.value

                    // New patients start with no assignments.
                    updates["assignedDoctorId"] =
                        ""

                    updates["assignedCaregiverId"] =
                        ""

                    updates["createdAt"] =
                        ServerValue.TIMESTAMP
                }

                profileRef
                    .updateChildren(
                        updates
                    )
                    .addOnSuccessListener {
                        callback(
                            DatabaseResult.Success(
                                Unit
                            )
                        )
                    }
                    .addOnFailureListener { e ->
                        callback(
                            DatabaseResult.Error(
                                "Failed to update user profile: ${e.message}"
                            )
                        )
                    }
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Pre-update fetch failed: ${e.message}"
                    )
                )
            }
    }

    /**
     * Get patients assigned to the currently signed-in doctor.
     */
    actual override fun getAllUsers(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    ) {
        val doctorId =
            auth.currentUser?.uid

        if (doctorId == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        // Verify that the current account is a doctor.
        database
            .child(dbPath)
            .child(doctorId)
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    callback(
                        DatabaseResult.Error(
                            "Doctor profile not found."
                        )
                    )
                    return@addOnSuccessListener
                }

                val data =
                    snapshot.value as? Map<*, *>

                val userType =
                    data
                        ?.get("userType")
                        ?.toString()

                if (
                    userType !=
                    UserType.DOCTOR.value
                ) {
                    callback(
                        DatabaseResult.Error(
                            "Not authorized. Only doctors can access patient data."
                        )
                    )
                    return@addOnSuccessListener
                }

                fetchAssignedPatients(
                    doctorId = doctorId,
                    callback = callback
                )
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Authorization check failed: ${e.message}"
                    )
                )
            }
    }

    /**
     * Get patients who are not currently assigned to any doctor.
     *
     * Only doctors can access this list.
     */
    actual override fun getUnassignedPatients(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    ) {
        val doctorId =
            auth.currentUser?.uid

        if (doctorId == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        // Verify that the current account is a doctor.
        database
            .child(dbPath)
            .child(doctorId)
            .get()
            .addOnSuccessListener { doctorSnapshot ->

                if (!doctorSnapshot.exists()) {
                    callback(
                        DatabaseResult.Error(
                            "Doctor profile not found."
                        )
                    )
                    return@addOnSuccessListener
                }

                val doctorData =
                    doctorSnapshot.value as? Map<*, *>

                val doctorUserType =
                    doctorData
                        ?.get("userType")
                        ?.toString()

                if (
                    doctorUserType !=
                    UserType.DOCTOR.value
                ) {
                    callback(
                        DatabaseResult.Error(
                            "Not authorized. Only doctors can access unassigned patients."
                        )
                    )
                    return@addOnSuccessListener
                }

                /*
                 * Fetch all profiles.
                 *
                 * Doctors currently have collection-level access
                 * to UserProfiles, so we can filter unassigned
                 * patients locally.
                 */
                database
                    .child(dbPath)
                    .get()
                    .addOnSuccessListener { patientsSnapshot ->

                        try {
                            val unassignedPatients =
                                patientsSnapshot.children
                                    .mapNotNull { child ->

                                        val data =
                                            child.value
                                                    as? Map<*, *>
                                                ?: return@mapNotNull null

                                        val userType =
                                            data["userType"]
                                                ?.toString()

                                        if (
                                            userType !=
                                            UserType.USER.value
                                        ) {
                                            return@mapNotNull null
                                        }

                                        val assignedDoctorId =
                                            data["assignedDoctorId"]
                                                ?.toString()
                                                .orEmpty()

                                        if (
                                            assignedDoctorId.isNotBlank()
                                        ) {
                                            return@mapNotNull null
                                        }

                                        UserProfile.fromMap(
                                            data,
                                            child.key ?: ""
                                        )
                                    }

                            callback(
                                DatabaseResult.Success(
                                    unassignedPatients
                                )
                            )

                        } catch (e: Exception) {
                            callback(
                                DatabaseResult.Error(
                                    "Failed to parse unassigned patients: ${e.message}"
                                )
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(
                            DatabaseResult.Error(
                                "Failed to fetch unassigned patients: ${e.message}"
                            )
                        )
                    }
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Failed to verify doctor: ${e.message}"
                    )
                )
            }
    }

    /**
     * Assign a patient to the currently signed-in doctor.
     */
    actual override fun assignPatientToCurrentDoctor(
        patientId: String,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        val doctorId =
            auth.currentUser?.uid

        if (doctorId == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        if (patientId.isBlank()) {
            callback(
                DatabaseResult.Error(
                    "Invalid patient ID"
                )
            )
            return
        }

        val doctorRef =
            database
                .child(dbPath)
                .child(doctorId)

        doctorRef
            .get()
            .addOnSuccessListener { doctorSnapshot ->

                if (!doctorSnapshot.exists()) {
                    callback(
                        DatabaseResult.Error(
                            "Doctor profile not found"
                        )
                    )
                    return@addOnSuccessListener
                }

                val doctorData =
                    doctorSnapshot.value
                            as? Map<*, *>

                val doctorUserType =
                    doctorData
                        ?.get("userType")
                        ?.toString()

                if (
                    doctorUserType !=
                    UserType.DOCTOR.value
                ) {
                    callback(
                        DatabaseResult.Error(
                            "Not authorized. Only doctors can assign patients."
                        )
                    )
                    return@addOnSuccessListener
                }

                val patientRef =
                    database
                        .child(dbPath)
                        .child(patientId)

                patientRef
                    .get()
                    .addOnSuccessListener { patientSnapshot ->

                        if (!patientSnapshot.exists()) {
                            callback(
                                DatabaseResult.Error(
                                    "Patient profile not found"
                                )
                            )
                            return@addOnSuccessListener
                        }

                        val patientData =
                            patientSnapshot.value
                                    as? Map<*, *>

                        val patientUserType =
                            patientData
                                ?.get("userType")
                                ?.toString()

                        if (
                            patientUserType !=
                            UserType.USER.value
                        ) {
                            callback(
                                DatabaseResult.Error(
                                    "The selected profile is not a patient"
                                )
                            )
                            return@addOnSuccessListener
                        }

                        patientRef
                            .child(
                                "assignedDoctorId"
                            )
                            .setValue(
                                doctorId
                            )
                            .addOnSuccessListener {
                                callback(
                                    DatabaseResult.Success(
                                        Unit
                                    )
                                )
                            }
                            .addOnFailureListener { e ->
                                callback(
                                    DatabaseResult.Error(
                                        "Failed to assign patient: ${e.message}"
                                    )
                                )
                            }
                    }
                    .addOnFailureListener { e ->
                        callback(
                            DatabaseResult.Error(
                                "Failed to load patient profile: ${e.message}"
                            )
                        )
                    }
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Failed to verify doctor: ${e.message}"
                    )
                )
            }
    }

    /**
     * Get patients assigned to the currently signed-in caregiver.
     *
     * Uses:
     *
     * CaregiverPatients/{caregiverId}/{patientId} = true
     *
     * instead of allowing caregivers to query all UserProfiles.
     */
    actual override fun getPatientsForCurrentCaregiver(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    ) {
        val caregiverId =
            auth.currentUser?.uid

        if (caregiverId == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        /*
         * First verify that the signed-in user
         * really has the CAREGIVER role.
         */
        database
            .child(dbPath)
            .child(caregiverId)
            .get()
            .addOnSuccessListener caregiverCheck@{ caregiverSnapshot ->

                if (!caregiverSnapshot.exists()) {
                    callback(
                        DatabaseResult.Error(
                            "Caregiver profile not found."
                        )
                    )
                    return@caregiverCheck
                }

                val caregiverData =
                    caregiverSnapshot.value
                            as? Map<*, *>

                val caregiverUserType =
                    caregiverData
                        ?.get("userType")
                        ?.toString()

                if (
                    caregiverUserType !=
                    UserType.CAREGIVER.value
                ) {
                    callback(
                        DatabaseResult.Error(
                            "Not authorized. Only caregivers can access caregiver patient data."
                        )
                    )
                    return@caregiverCheck
                }

                /*
                 * Read only this caregiver's mapping.
                 */
                database
                    .child(
                        caregiverPatientsPath
                    )
                    .child(
                        caregiverId
                    )
                    .get()
                    .addOnSuccessListener mappingCheck@{ mappingSnapshot ->

                        if (!mappingSnapshot.exists()) {
                            callback(
                                DatabaseResult.Success(
                                    emptyList()
                                )
                            )
                            return@mappingCheck
                        }

                        val patientIds =
                            mappingSnapshot.children
                                .mapNotNull {
                                    it.key
                                }

                        if (
                            patientIds.isEmpty()
                        ) {
                            callback(
                                DatabaseResult.Success(
                                    emptyList()
                                )
                            )
                            return@mappingCheck
                        }

                        val patientProfiles =
                            mutableListOf<UserProfile>()

                        var completedRequests =
                            0

                        var callbackCompleted =
                            false

                        patientIds.forEach { patientId ->

                            database
                                .child(dbPath)
                                .child(patientId)
                                .get()
                                .addOnSuccessListener patientCheck@{ patientSnapshot ->

                                    if (callbackCompleted) {
                                        return@patientCheck
                                    }

                                    try {

                                        if (
                                            patientSnapshot.exists()
                                        ) {

                                            val patientData =
                                                patientSnapshot.value
                                                        as? Map<*, *>

                                            if (
                                                patientData != null
                                            ) {

                                                val userType =
                                                    patientData[
                                                        "userType"
                                                    ]
                                                        ?.toString()

                                                val assignedCaregiverId =
                                                    patientData[
                                                        "assignedCaregiverId"
                                                    ]
                                                        ?.toString()
                                                        .orEmpty()

                                                /*
                                                 * Safety check:
                                                 *
                                                 * - profile must be USER
                                                 * - assignedCaregiverId must
                                                 *   still match this caregiver
                                                 */
                                                if (
                                                    userType ==
                                                    UserType.USER.value &&
                                                    assignedCaregiverId ==
                                                    caregiverId
                                                ) {

                                                    patientProfiles.add(
                                                        UserProfile.fromMap(
                                                            patientData,
                                                            patientId
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        completedRequests += 1

                                        if (
                                            completedRequests ==
                                            patientIds.size
                                        ) {

                                            callbackCompleted =
                                                true

                                            callback(
                                                DatabaseResult.Success(
                                                    patientProfiles
                                                        .sortedBy {
                                                            it.name
                                                                .lowercase()
                                                        }
                                                )
                                            )
                                        }

                                    } catch (e: Exception) {

                                        if (!callbackCompleted) {

                                            callbackCompleted =
                                                true

                                            callback(
                                                DatabaseResult.Error(
                                                    "Failed to parse caregiver patient: ${e.message}"
                                                )
                                            )
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->

                                    if (!callbackCompleted) {

                                        callbackCompleted =
                                            true

                                        callback(
                                            DatabaseResult.Error(
                                                "Failed to load caregiver patient: ${e.message}"
                                            )
                                        )
                                    }
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(
                            DatabaseResult.Error(
                                "Failed to load caregiver patient mapping: ${e.message}"
                            )
                        )
                    }
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Failed to verify caregiver: ${e.message}"
                    )
                )
            }
    }

    /**
     * Get all caregiver profiles.
     *
     * Only doctors/admins can access this list.
     */
    actual override fun getAllCaregivers(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    ) {
        val doctorId =
            auth.currentUser?.uid

        if (doctorId == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        /*
         * Verify current account is a doctor.
         */
        database
            .child(dbPath)
            .child(doctorId)
            .get()
            .addOnSuccessListener { doctorSnapshot ->

                if (!doctorSnapshot.exists()) {
                    callback(
                        DatabaseResult.Error(
                            "Doctor profile not found."
                        )
                    )
                    return@addOnSuccessListener
                }

                val doctorData =
                    doctorSnapshot.value
                            as? Map<*, *>

                val doctorUserType =
                    doctorData
                        ?.get("userType")
                        ?.toString()

                if (
                    doctorUserType !=
                    UserType.DOCTOR.value
                ) {
                    callback(
                        DatabaseResult.Error(
                            "Not authorized. Only doctors can access caregivers."
                        )
                    )
                    return@addOnSuccessListener
                }

                /*
                 * Doctor has collection-level access to UserProfiles.
                 * Keep only caregiver profiles.
                 */
                database
                    .child(dbPath)
                    .get()
                    .addOnSuccessListener { snapshot ->

                        try {
                            val caregivers =
                                snapshot.children
                                    .mapNotNull { child ->

                                        val data =
                                            child.value
                                                    as? Map<*, *>
                                                ?: return@mapNotNull null

                                        val userType =
                                            data["userType"]
                                                ?.toString()

                                        if (
                                            userType !=
                                            UserType.CAREGIVER.value
                                        ) {
                                            return@mapNotNull null
                                        }

                                        UserProfile.fromMap(
                                            data,
                                            child.key ?: ""
                                        )
                                    }

                            callback(
                                DatabaseResult.Success(
                                    caregivers
                                )
                            )

                        } catch (e: Exception) {
                            callback(
                                DatabaseResult.Error(
                                    "Failed to parse caregivers: ${e.message}"
                                )
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(
                            DatabaseResult.Error(
                                "Failed to fetch caregivers: ${e.message}"
                            )
                        )
                    }
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Failed to verify doctor: ${e.message}"
                    )
                )
            }
    }

    /**
     * Assign a caregiver to a patient.
     *
     * Only a doctor/admin can perform this operation.
     *
     * This writes:
     *
     * UserProfiles/{patientId}/assignedCaregiverId
     *
     * and:
     *
     * CaregiverPatients/{caregiverId}/{patientId}
     *
     * in one atomic Firebase update.
     */
    actual override fun assignCaregiverToPatient(
        patientId: String,
        caregiverId: String,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        val doctorId =
            auth.currentUser?.uid

        if (doctorId == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        if (patientId.isBlank()) {
            callback(
                DatabaseResult.Error(
                    "Invalid patient ID"
                )
            )
            return
        }

        if (caregiverId.isBlank()) {
            callback(
                DatabaseResult.Error(
                    "Invalid caregiver ID"
                )
            )
            return
        }

        /*
         * Verify the current account is a doctor/admin.
         */
        database
            .child(dbPath)
            .child(doctorId)
            .get()
            .addOnSuccessListener doctorCheck@{ doctorSnapshot ->

                if (!doctorSnapshot.exists()) {
                    callback(
                        DatabaseResult.Error(
                            "Doctor profile not found."
                        )
                    )
                    return@doctorCheck
                }

                val doctorData =
                    doctorSnapshot.value
                            as? Map<*, *>

                val doctorUserType =
                    doctorData
                        ?.get("userType")
                        ?.toString()

                if (
                    doctorUserType !=
                    UserType.DOCTOR.value
                ) {
                    callback(
                        DatabaseResult.Error(
                            "Not authorized. Only doctors can assign caregivers."
                        )
                    )
                    return@doctorCheck
                }

                /*
                 * Verify patient.
                 */
                database
                    .child(dbPath)
                    .child(patientId)
                    .get()
                    .addOnSuccessListener patientCheck@{ patientSnapshot ->

                        if (!patientSnapshot.exists()) {
                            callback(
                                DatabaseResult.Error(
                                    "Patient profile not found."
                                )
                            )
                            return@patientCheck
                        }

                        val patientData =
                            patientSnapshot.value
                                    as? Map<*, *>

                        val patientUserType =
                            patientData
                                ?.get("userType")
                                ?.toString()

                        if (
                            patientUserType !=
                            UserType.USER.value
                        ) {
                            callback(
                                DatabaseResult.Error(
                                    "The selected profile is not a patient."
                                )
                            )
                            return@patientCheck
                        }

                        /*
                         * Keep the previous caregiver ID so that
                         * a reassignment can remove the old
                         * CaregiverPatients mapping.
                         */
                        val previousCaregiverId =
                            patientData
                                ?.get(
                                    "assignedCaregiverId"
                                )
                                ?.toString()
                                .orEmpty()

                        /*
                         * Verify selected caregiver.
                         */
                        database
                            .child(dbPath)
                            .child(caregiverId)
                            .get()
                            .addOnSuccessListener caregiverCheck@{ caregiverSnapshot ->

                                if (!caregiverSnapshot.exists()) {
                                    callback(
                                        DatabaseResult.Error(
                                            "Caregiver profile not found."
                                        )
                                    )
                                    return@caregiverCheck
                                }

                                val caregiverData =
                                    caregiverSnapshot.value
                                            as? Map<*, *>

                                val caregiverUserType =
                                    caregiverData
                                        ?.get("userType")
                                        ?.toString()

                                if (
                                    caregiverUserType !=
                                    UserType.CAREGIVER.value
                                ) {
                                    callback(
                                        DatabaseResult.Error(
                                            "The selected profile is not a caregiver."
                                        )
                                    )
                                    return@caregiverCheck
                                }

                                /*
                                 * Build one atomic multi-location
                                 * Firebase update.
                                 */
                                val updates =
                                    hashMapOf<String, Any?>()

                                /*
                                 * Store caregiver relationship
                                 * on the patient.
                                 */
                                updates[
                                    "/$dbPath/$patientId/assignedCaregiverId"
                                ] = caregiverId

                                /*
                                 * Add patient to new caregiver's
                                 * secure mapping.
                                 */
                                updates[
                                    "/$caregiverPatientsPath/$caregiverId/$patientId"
                                ] = true

                                /*
                                 * If the patient previously belonged
                                 * to another caregiver, remove the old
                                 * mapping during the same atomic write.
                                 */
                                if (
                                    previousCaregiverId.isNotBlank() &&
                                    previousCaregiverId != caregiverId
                                ) {

                                    updates[
                                        "/$caregiverPatientsPath/$previousCaregiverId/$patientId"
                                    ] = null
                                }

                                database
                                    .updateChildren(
                                        updates
                                    )
                                    .addOnSuccessListener {
                                        callback(
                                            DatabaseResult.Success(
                                                Unit
                                            )
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        callback(
                                            DatabaseResult.Error(
                                                "Failed to assign caregiver: ${e.message}"
                                            )
                                        )
                                    }
                            }
                            .addOnFailureListener { e ->
                                callback(
                                    DatabaseResult.Error(
                                        "Failed to load caregiver profile: ${e.message}"
                                    )
                                )
                            }
                    }
                    .addOnFailureListener { e ->
                        callback(
                            DatabaseResult.Error(
                                "Failed to load patient profile: ${e.message}"
                            )
                        )
                    }
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Failed to verify doctor: ${e.message}"
                    )
                )
            }
    }

    /**
     * Fetch patients assigned to the specified doctor.
     */
    private fun fetchAssignedPatients(
        doctorId: String,
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    ) {
        database
            .child(dbPath)
            .orderByChild(
                "assignedDoctorId"
            )
            .equalTo(
                doctorId
            )
            .get()
            .addOnSuccessListener { snapshot ->

                try {
                    val patientProfiles =
                        snapshot.children
                            .mapNotNull { child ->

                                val data =
                                    child.value
                                            as? Map<*, *>
                                        ?: return@mapNotNull null

                                val userType =
                                    data["userType"]
                                        ?.toString()

                                if (
                                    userType !=
                                    UserType.USER.value
                                ) {
                                    return@mapNotNull null
                                }

                                UserProfile.fromMap(
                                    data,
                                    child.key ?: ""
                                )
                            }

                    callback(
                        DatabaseResult.Success(
                            patientProfiles
                        )
                    )

                } catch (e: Exception) {
                    callback(
                        DatabaseResult.Error(
                            "Failed to fetch assigned patients: ${e.message}"
                        )
                    )
                }
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Failed to fetch assigned patients: ${e.message}"
                    )
                )
            }
    }

    /**
     * Upload the current user's profile image.
     */
    actual override fun uploadProfileImage(
        imageBytes: ByteArray,
        callback: (DatabaseResult<String>) -> Unit
    ) {
        if (imageBytes.isEmpty()) {
            callback(
                DatabaseResult.Error(
                    "The selected image is empty"
                )
            )
            return
        }

        val userId =
            auth.currentUser?.uid

        if (userId == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        val profileImageRef =
            storage.reference
                .child(
                    "profile_images/${userId}.jpg"
                )

        profileImageRef
            .putBytes(
                imageBytes
            )
            .addOnSuccessListener {

                profileImageRef
                    .downloadUrl
                    .addOnSuccessListener { uri ->

                        val url =
                            uri.toString()

                        database
                            .child(dbPath)
                            .child(userId)
                            .child(
                                "profileImageUrl"
                            )
                            .setValue(
                                url
                            )
                            .addOnSuccessListener {
                                callback(
                                    DatabaseResult.Success(
                                        url
                                    )
                                )
                            }
                            .addOnFailureListener { e ->
                                callback(
                                    DatabaseResult.Error(
                                        "Image uploaded, but failed to save " +
                                                "profile URL: ${e.message}"
                                    )
                                )
                            }
                    }
                    .addOnFailureListener { e ->
                        callback(
                            DatabaseResult.Error(
                                "Failed to get download URL: ${e.message}"
                            )
                        )
                    }
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Failed to upload image: ${e.message}"
                    )
                )
            }
    }
}