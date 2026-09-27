package org.example.dementia_tester_app.data

/**
 * Interface for user profile service.
 *
 * This will be implemented differently for Android and iOS.
 */
interface UserProfileServiceInterface {

    fun getCurrentUserProfile(
        callback: (DatabaseResult<UserProfile>) -> Unit
    )

    fun getUserProfile(
        userId: String,
        callback: (DatabaseResult<UserProfile>) -> Unit
    )

    fun updateUserProfile(
        userProfile: UserProfile,
        callback: (DatabaseResult<Unit>) -> Unit
    )

    /**
     * Get patients assigned to the currently signed-in doctor.
     */
    fun getAllUsers(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    )

    /**
     * Get patients who are not currently assigned to any doctor.
     */
    fun getUnassignedPatients(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    )

    /**
     * Assign a patient to the currently signed-in doctor.
     */
    fun assignPatientToCurrentDoctor(
        patientId: String,
        callback: (DatabaseResult<Unit>) -> Unit
    )

    /**
     * Get patients assigned to the currently signed-in caregiver.
     */
    fun getPatientsForCurrentCaregiver(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    )

    /**
     * Get all caregiver profiles.
     *
     * Used by the doctor/admin when selecting a caregiver
     * to assign to a patient.
     */
    fun getAllCaregivers(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    )

    /**
     * Get all doctor profiles.
     */
    fun getAllDoctors(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    )

    /**
     * Assign a caregiver to a patient.
     *
     * The currently signed-in user must be a doctor/admin.
     */
    fun assignCaregiverToPatient(
        patientId: String,
        caregiverId: String,
        callback: (DatabaseResult<Unit>) -> Unit
    )

    fun uploadProfileImage(
        imageBytes: ByteArray,
        callback: (DatabaseResult<String>) -> Unit
    )
}

expect class UserProfileService() : UserProfileServiceInterface {

    /**
     * Get the current user's profile.
     */
    override fun getCurrentUserProfile(
        callback: (DatabaseResult<UserProfile>) -> Unit
    )

    /**
     * Get a specific user's profile by userId.
     */
    override fun getUserProfile(
        userId: String,
        callback: (DatabaseResult<UserProfile>) -> Unit
    )

    /**
     * Update the current user's profile.
     */
    override fun updateUserProfile(
        userProfile: UserProfile,
        callback: (DatabaseResult<Unit>) -> Unit
    )

    /**
     * Get patients assigned to the currently signed-in doctor.
     */
    override fun getAllUsers(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    )

    /**
     * Get patients who are not currently assigned to any doctor.
     */
    override fun getUnassignedPatients(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    )

    /**
     * Assign a patient to the currently signed-in doctor.
     *
     * The Android implementation will:
     * - verify that the current user is a doctor
     * - verify that the target profile is a patient/user
     * - write the doctor's UID into the patient's assignedDoctorId
     */
    override fun assignPatientToCurrentDoctor(
        patientId: String,
        callback: (DatabaseResult<Unit>) -> Unit
    )

    /**
     * Get patients assigned to the currently signed-in caregiver.
     *
     * The Android implementation will:
     * - verify that the current user is a caregiver
     * - query patients using assignedCaregiverId
     * - return only USER/patient profiles
     */
    override fun getPatientsForCurrentCaregiver(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    )

    /**
     * Get all caregiver profiles.
     *
     * The Android implementation will verify that the
     * current user is a doctor/admin before returning them.
     */
    override fun getAllCaregivers(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    )

    /**
     * Get all doctor profiles.
     */
    override fun getAllDoctors(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    )

    /**
     * Assign a caregiver to a patient.
     *
     * The Android implementation will:
     * - verify that the current user is a doctor/admin
     * - verify that patientId belongs to a USER
     * - verify that caregiverId belongs to a CAREGIVER
     * - write caregiverId into the patient's assignedCaregiverId
     */
    override fun assignCaregiverToPatient(
        patientId: String,
        caregiverId: String,
        callback: (DatabaseResult<Unit>) -> Unit
    )

    /**
     * Upload a profile image.
     *
     * @param imageBytes The image data as a ByteArray
     * @param callback Callback containing the uploaded image URL
     */
    override fun uploadProfileImage(
        imageBytes: ByteArray,
        callback: (DatabaseResult<String>) -> Unit
    )
}
