package org.example.dementia_tester_app.data

class FakeUserProfileService : UserProfileServiceInterface {

    var storedProfile: UserProfile? = null
    var shouldSucceed = true
    var errorMessage = "Operation failed"

    override fun getCurrentUserProfile(
        callback: (DatabaseResult<UserProfile>) -> Unit
    ) {
        val profile = storedProfile

        if (profile != null) {
            callback(
                DatabaseResult.Success(profile)
            )
        } else {
            callback(
                DatabaseResult.Error(
                    "No profile found"
                )
            )
        }
    }

    override fun updateUserProfile(
        userProfile: UserProfile,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        if (shouldSucceed) {
            storedProfile = userProfile

            callback(
                DatabaseResult.Success(Unit)
            )
        } else {
            callback(
                DatabaseResult.Error(
                    errorMessage
                )
            )
        }
    }

    override fun getAllUsers(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    ) {
        if (!shouldSucceed) {
            callback(
                DatabaseResult.Error(
                    errorMessage
                )
            )
            return
        }

        val profile = storedProfile

        val assignedPatients =
            if (
                profile != null &&
                profile.userType == UserType.USER &&
                profile.assignedDoctorId.isNotBlank()
            ) {
                listOf(profile)
            } else {
                emptyList()
            }

        callback(
            DatabaseResult.Success(
                assignedPatients
            )
        )
    }

    override fun getUnassignedPatients(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    ) {
        if (!shouldSucceed) {
            callback(
                DatabaseResult.Error(
                    errorMessage
                )
            )
            return
        }

        val profile = storedProfile

        val unassignedPatients =
            if (
                profile != null &&
                profile.userType == UserType.USER &&
                profile.assignedDoctorId.isBlank()
            ) {
                listOf(profile)
            } else {
                emptyList()
            }

        callback(
            DatabaseResult.Success(
                unassignedPatients
            )
        )
    }

    override fun assignPatientToCurrentDoctor(
        patientId: String,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        if (!shouldSucceed) {
            callback(
                DatabaseResult.Error(
                    errorMessage
                )
            )
            return
        }

        val profile = storedProfile

        if (profile == null) {
            callback(
                DatabaseResult.Error(
                    "No profile found"
                )
            )
            return
        }

        if (profile.userId != patientId) {
            callback(
                DatabaseResult.Error(
                    "Patient profile not found"
                )
            )
            return
        }

        if (profile.userType != UserType.USER) {
            callback(
                DatabaseResult.Error(
                    "The selected profile is not a patient"
                )
            )
            return
        }

        storedProfile = profile.copy(
            assignedDoctorId = "fake-doctor-id"
        )

        callback(
            DatabaseResult.Success(Unit)
        )
    }

    override fun getPatientsForCurrentCaregiver(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    ) {
        if (!shouldSucceed) {
            callback(
                DatabaseResult.Error(
                    errorMessage
                )
            )
            return
        }

        val profile = storedProfile

        val caregiverPatients =
            if (
                profile != null &&
                profile.userType == UserType.USER &&
                profile.assignedCaregiverId.isNotBlank()
            ) {
                listOf(profile)
            } else {
                emptyList()
            }

        callback(
            DatabaseResult.Success(
                caregiverPatients
            )
        )
    }

    override fun getAllCaregivers(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    ) {
        if (!shouldSucceed) {
            callback(
                DatabaseResult.Error(
                    errorMessage
                )
            )
            return
        }

        val profile = storedProfile

        val caregivers =
            if (
                profile != null &&
                profile.userType == UserType.CAREGIVER
            ) {
                listOf(profile)
            } else {
                emptyList()
            }

        callback(
            DatabaseResult.Success(
                caregivers
            )
        )
    }

    override fun assignCaregiverToPatient(
        patientId: String,
        caregiverId: String,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        if (!shouldSucceed) {
            callback(
                DatabaseResult.Error(
                    errorMessage
                )
            )
            return
        }

        val profile = storedProfile

        if (profile == null) {
            callback(
                DatabaseResult.Error(
                    "No profile found"
                )
            )
            return
        }

        if (profile.userId != patientId) {
            callback(
                DatabaseResult.Error(
                    "Patient profile not found"
                )
            )
            return
        }

        if (profile.userType != UserType.USER) {
            callback(
                DatabaseResult.Error(
                    "The selected profile is not a patient"
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

        storedProfile = profile.copy(
            assignedCaregiverId = caregiverId
        )

        callback(
            DatabaseResult.Success(Unit)
        )
    }

    override fun uploadProfileImage(
        imageBytes: ByteArray,
        callback: (DatabaseResult<String>) -> Unit
    ) {
        if (shouldSucceed) {
            callback(
                DatabaseResult.Success(
                    "fake-image-url"
                )
            )
        } else {
            callback(
                DatabaseResult.Error(
                    errorMessage
                )
            )
        }
    }
}