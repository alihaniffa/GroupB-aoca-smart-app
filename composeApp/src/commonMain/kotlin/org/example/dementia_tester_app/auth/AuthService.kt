package org.example.dementia_tester_app.auth

/**
 * Result class to handle authentication operations
 */
sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Interface for authentication service
 */
 interface AuthServiceInterface {
    fun signIn(email: String, password: String, callback: (AuthResult) -> Unit)
    fun signUp(email: String, password: String, callback: (AuthResult) -> Unit)
    fun sendPasswordResetEmail(email: String, callback: (AuthResult) -> Unit)
    fun sendEmailVerification(callback: (AuthResult) -> Unit)
    fun isEmailVerified(): Boolean
    fun reloadUser(callback: (AuthResult) -> Unit)
    fun signOut()
    fun isUserSignedIn(): Boolean
    fun getCurrentUserId(): String?
    fun getCurrentUserEmail(): String?
    fun changePassword(newPassword: String, callback: (AuthResult) -> Unit)
    fun deleteAccount(callback: (AuthResult) -> Unit)
}

expect class AuthService() : AuthServiceInterface {

    /**
     * Sign in with email and password
     * @param email User's email
     * @param password User's password
     * @param callback Callback to be invoked with the result of the operation
     */
    override fun signIn(email: String, password: String, callback: (AuthResult) -> Unit)

    /**
     * Sign up with email and password
     * @param email User's email
     * @param password User's password
     * @param callback Callback to be invoked with the result of the operation
     */
    override fun signUp(email: String, password: String, callback: (AuthResult) -> Unit)

    /**
     * Send password reset email
     * @param email User's email
     * @param callback Callback to be invoked with the result of the operation
     */
    override fun sendPasswordResetEmail(email: String, callback: (AuthResult) -> Unit)

    /**
     * Send email verification to the current user
     * @param callback Callback to be invoked with the result of the operation
     */
    override fun sendEmailVerification(callback: (AuthResult) -> Unit)

    /**
     * Check if the current user's email is verified
     * @return true if the email is verified, false otherwise
     */
    override fun isEmailVerified(): Boolean

    /**
     * Reload the current user's data to get the latest status
     * @param callback Callback to be invoked with the result of the operation
     */
    override fun reloadUser(callback: (AuthResult) -> Unit)

    /**
     * Sign out the current user
     */
    override fun signOut()

    /**
     * Check if a user is currently signed in
     * @return true if a user is signed in, false otherwise
     */
    override fun isUserSignedIn(): Boolean

    /**
     * Get the current user's ID
     * @return the user's ID if signed in, null otherwise
     */
    override fun getCurrentUserId(): String?

    /**
     * Get the current user's email
     * @return the user's email if signed in, null otherwise
     */
    override fun getCurrentUserEmail(): String?

    /**
     * Change password for the currently signed-in user
     */
    override fun changePassword(newPassword: String, callback: (AuthResult) -> Unit)

    /**
     * Delete the currently signed-in user account
     */
    override fun deleteAccount(callback: (AuthResult) -> Unit)
}
