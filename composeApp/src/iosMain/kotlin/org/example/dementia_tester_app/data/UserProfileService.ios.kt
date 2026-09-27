package org.example.dementia_tester_app.data

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseDatabase.FIRDatabase
import cocoapods.FirebaseDatabase.FIRDataSnapshot
import platform.Foundation.NSDictionary
import platform.Foundation.NSNull

/**
 * iOS actual implementation of UserProfileService.
 *
 * Mirrors the Android implementation (UserProfileService.android.kt)
 * for logic parity, including doctor/caregiver/patient role
 * verification and the assignedDoctorId / assignedCaregiverId /
 * CaregiverPatients assignment flows introduced alongside the
 * role-based access work.
 *
 * Profile image upload is temporarily disabled on iOS while
 * Firebase Storage / NSData interop is being resolved.
 */
actual class UserProfileService actual constructor() :
    UserProfileServiceInterface {

    private val dbPath = "UserProfiles"
    private val caregiverPatientsPath = "CaregiverPatients"

    private fun currentUserId(): String? =
        FIRAuth.auth()?.currentUser()?.uid()

    private fun rootRef() =
        FIRDatabase.database()?.reference()

    private fun snapshotToMap(
        snapshot: FIRDataSnapshot
    ): Map<String, Any?>? {

        val value = snapshot.value

        return when (value) {
            is Map<*, *> ->
                value as? Map<String, Any?>

            is NSDictionary ->
                nsDictionaryToMap(value)

            else ->
                null
        }
    }

    private fun nsDictionaryToMap(
        dict: NSDictionary
    ): Map<String, Any?> {

        val result =
            mutableMapOf<String, Any?>()

        val keyEnumerator =
            dict.keyEnumerator()

        while (true) {
            val rawKey =
                keyEnumerator.nextObject()
                    ?: break

            val key =
                rawKey.toString()

            val value =
                dict.objectForKey(rawKey)

            result[key] =
                when (value) {
                    is NSDictionary ->
                        nsDictionaryToMap(value)

                    is NSNull ->
                        null

                    else ->
                        value
                }
        }

        return result
    }

    private fun snapshotChildren(
        snapshot: FIRDataSnapshot?
    ): List<FIRDataSnapshot> {

        if (snapshot == null) {
            return emptyList()
        }

        val result =
            mutableListOf<FIRDataSnapshot>()

        val enumerator =
            snapshot.children

        while (true) {
            val child =
                enumerator.nextObject()
                    ?: break

            if (child is FIRDataSnapshot) {
                result.add(child)
            }
        }

        return result
    }

    // ------------------------------------------------------------------
    // getCurrentUserProfile
    // ------------------------------------------------------------------

    actual override fun getCurrentUserProfile(
        callback: (DatabaseResult<UserProfile>) -> Unit
    ) {

        val userId =
            currentUserId()

        if (userId == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        val ref =
            rootRef()
                ?.child(dbPath)
                ?.child(userId)

        if (ref == null) {
            callback(
                DatabaseResult.Error(
                    "Firebase not initialized"
                )
            )
            return
        }

        FirebaseDatabaseIosHelper.observeValueOnce(
            query = ref
        ) { snapshot ->

            if (
                snapshot == null ||
                !snapshot.exists()
            ) {
                callback(
                    DatabaseResult.Error(
                        "Profile not found. Please create a profile."
                    )
                )

                return@observeValueOnce
            }

            try {
                val data =
                    snapshotToMap(snapshot)

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

            } catch (t: Throwable) {
                callback(
                    DatabaseResult.Error(
                        "Failed to parse user profile: ${t.message}"
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // getUserProfile
    // ------------------------------------------------------------------

    actual override fun getUserProfile(
        userId: String,
        callback: (DatabaseResult<UserProfile>) -> Unit
    ) {
        if (userId.isBlank()) {
            callback(
                DatabaseResult.Error(
                    "User ID cannot be blank"
                )
            )
            return
        }

        val ref =
            rootRef()
                ?.child(dbPath)
                ?.child(userId)

        if (ref == null) {
            callback(
                DatabaseResult.Error(
                    "Firebase not initialized"
                )
            )
            return
        }

        FirebaseDatabaseIosHelper.observeValueOnce(
            query = ref
        ) { snapshot ->

            if (
                snapshot == null ||
                !snapshot.exists()
            ) {
                callback(
                    DatabaseResult.Error(
                        "Profile not found"
                    )
                )

                return@observeValueOnce
            }

            try {
                val data =
                    snapshotToMap(snapshot)

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

            } catch (t: Throwable) {
                callback(
                    DatabaseResult.Error(
                        "Failed to parse user profile: ${t.message}"
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // updateUserProfile
    // ------------------------------------------------------------------

    actual override fun updateUserProfile(
        userProfile: UserProfile,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {

        val currentUser =
            FIRAuth.auth()?.currentUser()

        val userId =
            currentUser?.uid()

        if (userId == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        val profileRef =
            rootRef()
                ?.child(dbPath)
                ?.child(userId)

        if (profileRef == null) {
            callback(
                DatabaseResult.Error(
                    "Firebase not initialized"
                )
            )
            return
        }

        FirebaseDatabaseIosHelper.observeValueOnce(
            query = profileRef
        ) { snapshot ->

            val updates =
                userProfile
                    .toMap()
                    .toMutableMap()

            updates["userId"] =
                userId

            updates["updatedAt"] =
                cocoapods.FirebaseDatabase
                    .FIRServerValue
                    .timestamp()

            if (
                snapshot != null &&
                snapshot.exists()
            ) {

                val data =
                    snapshotToMap(snapshot)

                val existingUserType =
                    (data?.get("userType") as? String)
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: UserType.USER.value

                val existingEmail =
                    (data?.get("email") as? String)
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: (
                                currentUser.email()
                                    ?: ""
                                )

                val existingAssignedDoctorId =
                    (
                            data?.get(
                                "assignedDoctorId"
                            ) as? String
                            ) ?: ""

                val existingAssignedCaregiverId =
                    (
                            data?.get(
                                "assignedCaregiverId"
                            ) as? String
                            ) ?: ""

                updates["userType"] =
                    existingUserType

                updates["email"] =
                    existingEmail

                updates["assignedDoctorId"] =
                    existingAssignedDoctorId

                updates["assignedCaregiverId"] =
                    existingAssignedCaregiverId

                val existingCreatedAt =
                    data?.get("createdAt")

                updates["createdAt"] =
                    existingCreatedAt
                        ?: cocoapods.FirebaseDatabase
                            .FIRServerValue
                            .timestamp()

            } else {

                val profileEmail =
                    updates["email"]
                            as? String

                if (
                    profileEmail.isNullOrBlank()
                ) {
                    updates["email"] =
                        currentUser.email()
                            ?: ""
                }

                updates["userType"] =
                    UserType.USER.value

                updates["assignedDoctorId"] =
                    ""

                updates["assignedCaregiverId"] =
                    ""

                updates["createdAt"] =
                    cocoapods.FirebaseDatabase
                        .FIRServerValue
                        .timestamp()
            }

            val objcMap:
                    Map<Any?, Any?> =
                updates.entries.associate {
                        (key, value) ->

                    (key as Any?) to
                            (value ?: NSNull())
                }

            profileRef.updateChildValues(
                objcMap
            ) { error, _ ->

                if (error == null) {
                    callback(
                        DatabaseResult.Success(
                            Unit
                        )
                    )
                } else {
                    callback(
                        DatabaseResult.Error(
                            "Failed to update user profile: ${error.localizedDescription}"
                        )
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Shared helper: verify the current user's role before an operation
    // ------------------------------------------------------------------

    private fun verifyRole(
        expected: UserType,
        notFoundMessage: String,
        unauthorizedMessage: String,
        onVerified: (
            currentUserId: String
        ) -> Unit,
        onError: (
            DatabaseResult.Error
        ) -> Unit
    ) {

        val userId =
            currentUserId()

        if (userId == null) {
            onError(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        val ref =
            rootRef()
                ?.child(dbPath)
                ?.child(userId)

        if (ref == null) {
            onError(
                DatabaseResult.Error(
                    "Firebase not initialized"
                )
            )
            return
        }

        FirebaseDatabaseIosHelper.observeValueOnce(
            query = ref
        ) { snapshot ->

            if (
                snapshot == null ||
                !snapshot.exists()
            ) {
                onError(
                    DatabaseResult.Error(
                        notFoundMessage
                    )
                )

                return@observeValueOnce
            }

            val data =
                snapshotToMap(snapshot)

            val userType =
                data?.get("userType")
                        as? String

            if (
                userType !=
                expected.value
            ) {
                onError(
                    DatabaseResult.Error(
                        unauthorizedMessage
                    )
                )

                return@observeValueOnce
            }

            onVerified(userId)
        }
    }

    // ------------------------------------------------------------------
    // getAllUsers — patients assigned to the current doctor
    // ------------------------------------------------------------------

    actual override fun getAllUsers(
        callback:
            (DatabaseResult<List<UserProfile>>) ->
        Unit
    ) {

        verifyRole(
            expected =
                UserType.DOCTOR,

            notFoundMessage =
                "Doctor profile not found.",

            unauthorizedMessage =
                "Not authorized. Only doctors can access patient data.",

            onVerified = {
                    doctorId ->

                fetchAssignedPatients(
                    doctorId,
                    callback
                )
            },

            onError = {
                callback(it)
            }
        )
    }

    private fun fetchAssignedPatients(
        doctorId: String,
        callback:
            (DatabaseResult<List<UserProfile>>) ->
        Unit
    ) {

        val ref =
            rootRef()
                ?.child(dbPath)
                ?.queryOrderedByChild(
                    "assignedDoctorId"
                )
                ?.queryEqualToValue(
                    doctorId
                )

        if (ref == null) {
            callback(
                DatabaseResult.Error(
                    "Firebase not initialized"
                )
            )
            return
        }

        FirebaseDatabaseIosHelper.observeValueOnce(
            query = ref
        ) { snapshot ->

            try {

                val patients =
                    mutableListOf<UserProfile>()

                snapshotChildren(
                    snapshot
                ).forEach {
                        childSnapshot ->

                    val data =
                        snapshotToMap(
                            childSnapshot
                        )
                            ?: return@forEach

                    if (
                        data["userType"]
                                as? String
                        != UserType.USER.value
                    ) {
                        return@forEach
                    }

                    patients.add(
                        UserProfile.fromMap(
                            data,
                            childSnapshot.key()
                                ?: ""
                        )
                    )
                }

                callback(
                    DatabaseResult.Success(
                        patients
                    )
                )

            } catch (t: Throwable) {
                callback(
                    DatabaseResult.Error(
                        "Failed to fetch assigned patients: ${t.message}"
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // getUnassignedPatients
    // ------------------------------------------------------------------

    actual override fun getUnassignedPatients(
        callback:
            (DatabaseResult<List<UserProfile>>) ->
        Unit
    ) {

        verifyRole(
            expected =
                UserType.DOCTOR,

            notFoundMessage =
                "Doctor profile not found.",

            unauthorizedMessage =
                "Not authorized. Only doctors can access unassigned patients.",

            onVerified = {

                val ref =
                    rootRef()
                        ?.child(dbPath)

                if (ref == null) {
                    callback(
                        DatabaseResult.Error(
                            "Firebase not initialized"
                        )
                    )

                    return@verifyRole
                }

                FirebaseDatabaseIosHelper.observeValueOnce(
                    query = ref
                ) { snapshot ->

                    try {

                        val unassigned =
                            mutableListOf<UserProfile>()

                        snapshotChildren(
                            snapshot
                        ).forEach {
                                childSnapshot ->

                            val data =
                                snapshotToMap(
                                    childSnapshot
                                )
                                    ?: return@forEach

                            if (
                                data["userType"]
                                        as? String
                                != UserType.USER.value
                            ) {
                                return@forEach
                            }

                            val assignedDoctorId =
                                (
                                        data[
                                            "assignedDoctorId"
                                        ] as? String
                                        ) ?: ""

                            if (
                                assignedDoctorId
                                    .isNotBlank()
                            ) {
                                return@forEach
                            }

                            unassigned.add(
                                UserProfile.fromMap(
                                    data,
                                    childSnapshot.key()
                                        ?: ""
                                )
                            )
                        }

                        callback(
                            DatabaseResult.Success(
                                unassigned
                            )
                        )

                    } catch (
                        t: Throwable
                    ) {
                        callback(
                            DatabaseResult.Error(
                                "Failed to parse unassigned patients: ${t.message}"
                            )
                        )
                    }
                }
            },

            onError = {
                callback(it)
            }
        )
    }

    // ------------------------------------------------------------------
    // assignPatientToCurrentDoctor
    // ------------------------------------------------------------------

    actual override fun assignPatientToCurrentDoctor(
        patientId: String,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {

        if (patientId.isBlank()) {
            callback(
                DatabaseResult.Error(
                    "Invalid patient ID"
                )
            )
            return
        }

        verifyRole(
            expected =
                UserType.DOCTOR,

            notFoundMessage =
                "Doctor profile not found",

            unauthorizedMessage =
                "Not authorized. Only doctors can assign patients.",

            onVerified = {
                    doctorId ->

                val patientRef =
                    rootRef()
                        ?.child(dbPath)
                        ?.child(patientId)

                if (patientRef == null) {
                    callback(
                        DatabaseResult.Error(
                            "Firebase not initialized"
                        )
                    )

                    return@verifyRole
                }

                FirebaseDatabaseIosHelper.observeValueOnce(
                    query = patientRef
                ) { patientSnapshot ->

                    if (
                        patientSnapshot == null ||
                        !patientSnapshot.exists()
                    ) {
                        callback(
                            DatabaseResult.Error(
                                "Patient profile not found"
                            )
                        )

                        return@observeValueOnce
                    }

                    val patientData =
                        snapshotToMap(
                            patientSnapshot
                        )

                    if (
                        patientData
                            ?.get(
                                "userType"
                            ) as? String
                        != UserType.USER.value
                    ) {
                        callback(
                            DatabaseResult.Error(
                                "The selected profile is not a patient"
                            )
                        )

                        return@observeValueOnce
                    }

                    patientRef
                        .child(
                            "assignedDoctorId"
                        )
                        .setValue(
                            doctorId
                        ) {
                                error,
                                _ ->

                            if (
                                error == null
                            ) {
                                callback(
                                    DatabaseResult.Success(
                                        Unit
                                    )
                                )
                            } else {
                                callback(
                                    DatabaseResult.Error(
                                        "Failed to assign patient: ${error.localizedDescription}"
                                    )
                                )
                            }
                        }
                }
            },

            onError = {
                callback(it)
            }
        )
    }

    // ------------------------------------------------------------------
    // getPatientsForCurrentCaregiver
    // ------------------------------------------------------------------

    actual override fun getPatientsForCurrentCaregiver(
        callback:
            (DatabaseResult<List<UserProfile>>) ->
        Unit
    ) {

        verifyRole(
            expected =
                UserType.CAREGIVER,

            notFoundMessage =
                "Caregiver profile not found.",

            unauthorizedMessage =
                "Not authorized. Only caregivers can access caregiver patient data.",

            onVerified = {
                    caregiverId ->

                val mappingRef =
                    rootRef()
                        ?.child(
                            caregiverPatientsPath
                        )
                        ?.child(
                            caregiverId
                        )

                if (mappingRef == null) {
                    callback(
                        DatabaseResult.Error(
                            "Firebase not initialized"
                        )
                    )

                    return@verifyRole
                }

                FirebaseDatabaseIosHelper.observeValueOnce(
                    query = mappingRef
                ) { mappingSnapshot ->

                    if (
                        mappingSnapshot == null ||
                        !mappingSnapshot.exists()
                    ) {
                        callback(
                            DatabaseResult.Success(
                                emptyList()
                            )
                        )

                        return@observeValueOnce
                    }

                    val patientIds =
                        mutableListOf<String>()

                    snapshotChildren(
                        mappingSnapshot
                    ).forEach {
                            childSnapshot ->

                        childSnapshot
                            .key()
                            ?.let {
                                patientIds.add(
                                    it
                                )
                            }
                    }

                    if (
                        patientIds.isEmpty()
                    ) {
                        callback(
                            DatabaseResult.Success(
                                emptyList()
                            )
                        )

                        return@observeValueOnce
                    }

                    val patientProfiles =
                        mutableListOf<UserProfile>()

                    var completed =
                        0

                    var callbackCompleted =
                        false

                    patientIds.forEach {
                            patientId ->

                        val patientRef =
                            rootRef()
                                ?.child(
                                    dbPath
                                )
                                ?.child(
                                    patientId
                                )

                        if (patientRef == null) {
                            completed += 1

                            if (
                                completed ==
                                patientIds.size &&
                                !callbackCompleted
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

                            return@forEach
                        }

                        FirebaseDatabaseIosHelper.observeValueOnce(
                            query = patientRef
                        ) { patientSnapshot ->

                            if (
                                callbackCompleted
                            ) {
                                return@observeValueOnce
                            }

                            try {

                                if (
                                    patientSnapshot != null &&
                                    patientSnapshot.exists()
                                ) {

                                    val data =
                                        snapshotToMap(
                                            patientSnapshot
                                        )

                                    val userType =
                                        data
                                            ?.get(
                                                "userType"
                                            )
                                                as? String

                                    val assignedCaregiverId =
                                        (
                                                data
                                                    ?.get(
                                                        "assignedCaregiverId"
                                                    )
                                                        as? String
                                                ) ?: ""

                                    if (
                                        userType ==
                                        UserType.USER.value &&
                                        assignedCaregiverId ==
                                        caregiverId
                                    ) {
                                        patientProfiles.add(
                                            UserProfile.fromMap(
                                                data,
                                                patientId
                                            )
                                        )
                                    }
                                }

                                completed +=
                                    1

                                if (
                                    completed ==
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

                            } catch (
                                t: Throwable
                            ) {

                                if (
                                    !callbackCompleted
                                ) {
                                    callbackCompleted =
                                        true

                                    callback(
                                        DatabaseResult.Error(
                                            "Failed to parse caregiver patient: ${t.message}"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },

            onError = {
                callback(it)
            }
        )
    }

    // ------------------------------------------------------------------
    // getAllCaregivers
    // ------------------------------------------------------------------

    actual override fun getAllCaregivers(
        callback:
            (DatabaseResult<List<UserProfile>>) ->
        Unit
    ) {

        verifyRole(
            expected =
                UserType.DOCTOR,

            notFoundMessage =
                "Doctor profile not found.",

            unauthorizedMessage =
                "Not authorized. Only doctors can access caregivers.",

            onVerified = {

                val ref =
                    rootRef()
                        ?.child(dbPath)

                if (ref == null) {
                    callback(
                        DatabaseResult.Error(
                            "Firebase not initialized"
                        )
                    )

                    return@verifyRole
                }

                FirebaseDatabaseIosHelper.observeValueOnce(
                    query = ref
                ) { snapshot ->

                    try {

                        val caregivers =
                            mutableListOf<UserProfile>()

                        snapshotChildren(
                            snapshot
                        ).forEach {
                                childSnapshot ->

                            val data =
                                snapshotToMap(
                                    childSnapshot
                                )
                                    ?: return@forEach

                            if (
                                data["userType"]
                                        as? String
                                != UserType.CAREGIVER.value
                            ) {
                                return@forEach
                            }

                            caregivers.add(
                                UserProfile.fromMap(
                                    data,
                                    childSnapshot.key()
                                        ?: ""
                                )
                            )
                        }

                        callback(
                            DatabaseResult.Success(
                                caregivers
                            )
                        )

                    } catch (
                        t: Throwable
                    ) {
                        callback(
                            DatabaseResult.Error(
                                "Failed to parse caregivers: ${t.message}"
                            )
                        )
                    }
                }
            },

            onError = {
                callback(it)
            }
        )
    }

    // ------------------------------------------------------------------
    // getAllDoctors
    // ------------------------------------------------------------------

    actual override fun getAllDoctors(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    ) {
        val userId = currentUserId()
        if (userId == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        val ref = rootRef()?.child(dbPath)
        if (ref == null) {
            callback(DatabaseResult.Error("Firebase not initialized"))
            return
        }

        FirebaseDatabaseIosHelper.observeValueOnce(query = ref) { snapshot ->
            try {
                val doctors = mutableListOf<UserProfile>()
                snapshotChildren(snapshot).forEach { childSnapshot ->
                    val data = snapshotToMap(childSnapshot) ?: return@forEach
                    if (data["userType"] as? String != UserType.DOCTOR.value) return@forEach
                    doctors.add(
                        UserProfile.fromMap(
                            data,
                            childSnapshot.key() ?: ""
                        )
                    )
                }
                callback(DatabaseResult.Success(doctors))
            } catch (t: Throwable) {
                callback(DatabaseResult.Error("Failed to parse doctors: " + t.message))
            }
        }
    }

    // ------------------------------------------------------------------
    // assignCaregiverToPatient
    // ------------------------------------------------------------------

    actual override fun assignCaregiverToPatient(
        patientId: String,
        caregiverId: String,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {

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

        verifyRole(
            expected =
                UserType.DOCTOR,

            notFoundMessage =
                "Doctor profile not found.",

            unauthorizedMessage =
                "Not authorized. Only doctors can assign caregivers.",

            onVerified = {

                val patientRef =
                    rootRef()
                        ?.child(dbPath)
                        ?.child(patientId)

                if (patientRef == null) {
                    callback(
                        DatabaseResult.Error(
                            "Firebase not initialized"
                        )
                    )

                    return@verifyRole
                }

                FirebaseDatabaseIosHelper.observeValueOnce(
                    query = patientRef
                ) { patientSnapshot ->

                    if (
                        patientSnapshot == null ||
                        !patientSnapshot.exists()
                    ) {
                        callback(
                            DatabaseResult.Error(
                                "Patient profile not found."
                            )
                        )

                        return@observeValueOnce
                    }

                    val patientData =
                        snapshotToMap(
                            patientSnapshot
                        )

                    if (
                        patientData
                            ?.get(
                                "userType"
                            ) as? String
                        != UserType.USER.value
                    ) {
                        callback(
                            DatabaseResult.Error(
                                "The selected profile is not a patient."
                            )
                        )

                        return@observeValueOnce
                    }

                    val previousCaregiverId =
                        (
                                patientData[
                                    "assignedCaregiverId"
                                ] as? String
                                ) ?: ""

                    val caregiverRef =
                        rootRef()
                            ?.child(dbPath)
                            ?.child(
                                caregiverId
                            )

                    if (caregiverRef == null) {
                        callback(
                            DatabaseResult.Error(
                                "Firebase not initialized"
                            )
                        )

                        return@observeValueOnce
                    }

                    FirebaseDatabaseIosHelper.observeValueOnce(
                        query = caregiverRef
                    ) { caregiverSnapshot ->

                        if (
                            caregiverSnapshot == null ||
                            !caregiverSnapshot.exists()
                        ) {
                            callback(
                                DatabaseResult.Error(
                                    "Caregiver profile not found."
                                )
                            )

                            return@observeValueOnce
                        }

                        val caregiverData =
                            snapshotToMap(
                                caregiverSnapshot
                            )

                        if (
                            caregiverData
                                ?.get(
                                    "userType"
                                ) as? String
                            != UserType.CAREGIVER.value
                        ) {
                            callback(
                                DatabaseResult.Error(
                                    "The selected profile is not a caregiver."
                                )
                            )

                            return@observeValueOnce
                        }

                        // Atomic multi-path update:
                        // assign new caregiver,
                        // add CaregiverPatients mapping,
                        // and remove previous mapping
                        // if reassigning.

                        val updates =
                            mutableMapOf<
                                    Any?,
                                    Any?
                                    >(
                                "/$dbPath/$patientId/assignedCaregiverId" to
                                        caregiverId,

                                "/$caregiverPatientsPath/$caregiverId/$patientId" to
                                        true
                            )

                        if (
                            previousCaregiverId
                                .isNotBlank() &&
                            previousCaregiverId !=
                            caregiverId
                        ) {
                            updates[
                                "/$caregiverPatientsPath/$previousCaregiverId/$patientId"
                            ] = NSNull()
                        }

                        val root =
                            rootRef()

                        if (root == null) {
                            callback(
                                DatabaseResult.Error(
                                    "Firebase not initialized"
                                )
                            )

                            return@observeValueOnce
                        }

                        root.updateChildValues(
                            updates
                        ) {
                                error,
                                _ ->

                            if (
                                error == null
                            ) {
                                callback(
                                    DatabaseResult.Success(
                                        Unit
                                    )
                                )
                            } else {
                                callback(
                                    DatabaseResult.Error(
                                        "Failed to assign caregiver: ${error.localizedDescription}"
                                    )
                                )
                            }
                        }
                    }
                }
            },

            onError = {
                callback(it)
            }
        )
    }

    // ------------------------------------------------------------------
    // uploadProfileImage
    // ------------------------------------------------------------------
    // Temporarily disabled on iOS.
    //
    // The method remains here because UserProfileServiceInterface
    // requires it. This allows the rest of the iOS profile,
    // doctor, patient and caregiver functionality to be compiled
    // independently while Firebase Storage / NSData interop is fixed.
    // ------------------------------------------------------------------

    actual override fun uploadProfileImage(
        imageBytes: ByteArray,
        callback: (DatabaseResult<String>) -> Unit
    ) {
        callback(
            DatabaseResult.Error(
                "Profile image upload is temporarily unavailable on iOS"
            )
        )
    }
}