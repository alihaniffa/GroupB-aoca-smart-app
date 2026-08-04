package org.example.dementia_tester_app.data

/**
 * Interface for user profile service
 * This will be implemented differently for Android and iOS
 */
interface UserProfileServiceInterface {
    fun getCurrentUserProfile(callback: (DatabaseResult<UserProfile>) -> Unit)
    fun updateUserProfile(userProfile: UserProfile, callback: (DatabaseResult<Unit>) -> Unit)
    fun getAllUsers(callback: (DatabaseResult<List<UserProfile>>) -> Unit)
    fun uploadProfileImage(imageBytes: ByteArray, callback: (DatabaseResult<String>) -> Unit)
}
expect class UserProfileService() : UserProfileServiceInterface {
    /**
     * Get the current user's profile
     * @param callback Callback to be invoked with the result of the operation
     */
    override fun getCurrentUserProfile(callback: (DatabaseResult<UserProfile>) -> Unit)
    
    /**
     * Update the current user's profile
     * @param userProfile The updated user profile
     * @param callback Callback to be invoked with the result of the operation
     */
    override fun updateUserProfile(userProfile: UserProfile, callback: (DatabaseResult<Unit>) -> Unit)
    
    /**
     * Get all users with userType = User
     * @param callback Callback to be invoked with the result of the operation
     */
    override fun getAllUsers(callback: (DatabaseResult<List<UserProfile>>) -> Unit)

    /**
     * Upload a profile image
     * @param imageBytes The image data as a ByteArray
     * @param callback Callback to be invoked with the result of the operation, containing the URL of the uploaded image
     */
    override fun uploadProfileImage(imageBytes: ByteArray, callback: (DatabaseResult<String>) -> Unit)
}