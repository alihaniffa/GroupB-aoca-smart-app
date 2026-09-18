package org.example.dementia_tester_app.auth

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseAuth.FIRAuthDataResult
import platform.Foundation.NSError

// Firebase error codes
private const val EMAIL_IN_USE = 17007L
private const val INVALID_EMAIL = 17008L
private const val WRONG_PASSWORD = 17009L
private const val USER_NOT_FOUND = 17011L
private const val WEAK_PASSWORD = 17026L

actual class AuthService actual constructor() : AuthServiceInterface {

    /**
     * Sign in with email and password
     */
    actual override fun signIn(
        email: String,
        password: String,
        callback: (AuthResult) -> Unit
    ) {
        val auth = FIRAuth.auth()

        if (auth == null) {
            callback(
                AuthResult.Error(
                    "Authentication failed: Firebase not initialised"
                )
            )
            return
        }

        auth.signInWithEmail(
            email,
            password = password
        ) { _: FIRAuthDataResult?, error: NSError? ->

            if (error == null) {
                callback(AuthResult.Success)
                return@signInWithEmail
            }

            val message =
                if (error.domain == "FIRAuthErrorDomain") {
                    when (error.code.toLong()) {
                        INVALID_EMAIL ->
                            "Invalid email address."

                        USER_NOT_FOUND ->
                            "No account found with this email."

                        WRONG_PASSWORD ->
                            "Invalid email or password."

                        else ->
                            error.localizedDescription
                    }
                } else {
                    error.localizedDescription
                }

            callback(AuthResult.Error(message))
        }
    }

    /**
     * Sign up with email and password
     */
    actual override fun signUp(
        email: String,
        password: String,
        callback: (AuthResult) -> Unit
    ) {
        val auth = FIRAuth.auth()

        if (auth == null) {
            callback(
                AuthResult.Error(
                    "Registration failed: Firebase not initialized"
                )
            )
            return
        }

        auth.createUserWithEmail(
            email,
            password = password
        ) { _: FIRAuthDataResult?, error: NSError? ->

            if (error == null) {
                callback(AuthResult.Success)
            } else {
                val message =
                    when (error.code.toLong()) {
                        WEAK_PASSWORD ->
                            "Password is too weak."

                        INVALID_EMAIL ->
                            "Invalid email format."

                        EMAIL_IN_USE ->
                            "An account already exists with this email."

                        else ->
                            error.localizedDescription
                    }

                callback(AuthResult.Error(message))
            }
        }
    }

    /**
     * Send password reset email
     */
    actual override fun sendPasswordResetEmail(
        email: String,
        callback: (AuthResult) -> Unit
    ) {
        val auth = FIRAuth.auth()

        if (auth == null) {
            callback(
                AuthResult.Error(
                    "Failed to send password reset email: Firebase not initialized"
                )
            )
            return
        }

        auth.sendPasswordResetWithEmail(email) { error: NSError? ->

            if (error == null) {
                callback(AuthResult.Success)
            } else {
                val message =
                    if (error.code.toLong() == USER_NOT_FOUND) {
                        "No user found with this email."
                    } else {
                        "Failed to send password reset email: ${error.localizedDescription}"
                    }

                callback(AuthResult.Error(message))
            }
        }
    }

    /**
     * Send email verification to the current user
     */
    actual override fun sendEmailVerification(
        callback: (AuthResult) -> Unit
    ) {
        val user = FIRAuth.auth()?.currentUser()

        if (user == null) {
            callback(
                AuthResult.Error(
                    "No user is currently signed in"
                )
            )
            return
        }

        user.sendEmailVerificationWithCompletion { error: NSError? ->

            if (error == null) {
                callback(AuthResult.Success)
            } else {
                callback(
                    AuthResult.Error(
                        "Failed to send verification email: ${error.localizedDescription}"
                    )
                )
            }
        }
    }

    /**
     * Check whether the current user's email is verified.
     */
    actual override fun isEmailVerified(): Boolean {
        return FIRAuth.auth()
            ?.currentUser()
            ?.isEmailVerified()
            ?: false
    }

    /**
     * Reload the current Firebase user.
     */
    actual override fun reloadUser(
        callback: (AuthResult) -> Unit
    ) {
        val user = FIRAuth.auth()?.currentUser()

        if (user == null) {
            callback(
                AuthResult.Error(
                    "No user is currently signed in"
                )
            )
            return
        }

        user.reloadWithCompletion { error: NSError? ->

            if (error == null) {
                callback(AuthResult.Success)
            } else {
                callback(
                    AuthResult.Error(
                        "Failed to reload user: ${error.localizedDescription}"
                    )
                )
            }
        }
    }

    /**
     * Sign out the current user.
     */
    actual override fun signOut() {
        val auth = FIRAuth.auth()
            ?: return

        auth.signOut(
            error = null
        )
    }

    /**
     * Check whether a Firebase user is signed in.
     */
    actual override fun isUserSignedIn(): Boolean {
        return FIRAuth.auth()?.currentUser() != null
    }

    /**
     * Return the current user's Firebase UID.
     */
    actual override fun getCurrentUserId(): String? {
        return FIRAuth.auth()
            ?.currentUser()
            ?.uid()
    }

    /**
     * Return the current user's email address.
     */
    actual override fun getCurrentUserEmail(): String? {
        return FIRAuth.auth()
            ?.currentUser()
            ?.email()
    }

    /**
     * Change the current user's password.
     */
    actual override fun changePassword(
        newPassword: String,
        callback: (AuthResult) -> Unit
    ) {
        val user = FIRAuth.auth()?.currentUser()

        if (user == null) {
            callback(
                AuthResult.Error(
                    "No user is currently signed in"
                )
            )
            return
        }

        user.updatePassword(newPassword) { error: NSError? ->

            if (error == null) {
                callback(AuthResult.Success)
            } else {
                callback(
                    AuthResult.Error(
                        "Failed to change password: ${error.localizedDescription}"
                    )
                )
            }
        }
    }

    /**
     * Delete the current Firebase account.
     */
    actual override fun deleteAccount(
        callback: (AuthResult) -> Unit
    ) {
        val user = FIRAuth.auth()?.currentUser()

        if (user == null) {
            callback(
                AuthResult.Error(
                    "No user is currently signed in"
                )
            )
            return
        }

        user.deleteWithCompletion { error: NSError? ->

            if (error == null) {
                callback(AuthResult.Success)
            } else {
                callback(
                    AuthResult.Error(
                        "Failed to delete account: ${error.localizedDescription}"
                    )
                )
            }
        }
    }
}