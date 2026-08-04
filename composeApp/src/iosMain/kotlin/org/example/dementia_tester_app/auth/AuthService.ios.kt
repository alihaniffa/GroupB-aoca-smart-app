package org.example.dementia_tester_app.auth

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseAuth.FIRAuthDataResult
import platform.Foundation.NSError
import kotlinx.cinterop.*

//Firebase error codes
private const val EMAIL_IN_USE = 17007L
private const val INVALID_EMAIL = 17008L
private const val WRONG_PASSWORD = 17009L
private const val USER_NOT_FOUND = 17011L
private const val WEAK_PASSWORD = 17026L

actual class AuthService actual constructor() : AuthServiceInterface {
    /**
     * Sign in with email and password
     */
    actual fun signIn(email: String, password: String, callback: (AuthResult) -> Unit) {
        val auth = FIRAuth.auth()
        if (auth == null) {
            callback(AuthResult.Error("Authentication failed: Firebase not initialised"))
            return
        }

        auth.signInWithEmail(email, password = password) { _: FIRAuthDataResult?, error: NSError? ->
            if (error == null) {
                callback(AuthResult.Success)
                return@signInWithEmail
            }

            val message = if (error.domain == "FIRAuthErrorDomain") {
                when (error.code.toLong()) {
                    INVALID_EMAIL   -> "Invalid email address."
                    USER_NOT_FOUND  -> "No account found with this email."
                    WRONG_PASSWORD  -> "Invalid email or password."
                    else            -> error.localizedDescription
                }
            } else {
                error.localizedDescription
            }

            callback(AuthResult.Error(message))
        }
    }

    actual fun signUp(email: String, password: String, callback: (AuthResult) -> Unit) {
        val auth = FIRAuth.auth()
        if (auth == null) {
            callback(AuthResult.Error("Registration failed: Firebase not initialized"))
            return
        }
        auth.createUserWithEmail(email, password = password) { _: FIRAuthDataResult?, error: NSError? ->
            if (error == null) {
                callback(AuthResult.Success)
            } else {
                val message = when (error.code.toLong()) {
                    WEAK_PASSWORD -> "Password is too weak."
                    INVALID_EMAIL -> "Invalid email format."
                    EMAIL_IN_USE -> "An account already exists with this email."
                    else -> error.localizedDescription
                }
                callback(AuthResult.Error(message))
            }
        }
    }

    actual fun sendPasswordResetEmail(email: String, callback: (AuthResult) -> Unit) {
        val auth = FIRAuth.auth()
        if (auth == null) {
            callback(AuthResult.Error("Failed to send password reset email: Firebase not initialized"))
            return
        }
        auth.sendPasswordResetWithEmail(email) { error: NSError? ->
            if (error == null) {
                callback(AuthResult.Success)
            } else {
                val message = if (error.code == USER_NOT_FOUND) {
                    "No user found with this email."
                } else {
                    "Failed to send password reset email: ${error.localizedDescription}"
                }
                callback(AuthResult.Error(message))
            }
        }
    }

    actual fun sendEmailVerification(callback: (AuthResult) -> Unit) {
        val user = FIRAuth.auth()?.currentUser()
        if (user == null) {
            callback(AuthResult.Error("No user is currently signed in"))
            return
        }
        user.sendEmailVerificationWithCompletion { error: NSError? ->
            if (error == null) {
                callback(AuthResult.Success)
            } else {
                callback(AuthResult.Error("Failed to send verification email: ${error.localizedDescription}"))
            }
        }
    }

    actual fun isEmailVerified(): Boolean {
        return FIRAuth.auth()?.currentUser()?.isEmailVerified() ?: false
    }

    actual fun reloadUser(callback: (AuthResult) -> Unit) {
        val user = FIRAuth.auth()?.currentUser()
        if (user == null) {
            callback(AuthResult.Error("No user is currently signed in"))
            return
        }
        user.reloadWithCompletion { error: NSError? ->
            if (error == null) {
                callback(AuthResult.Success)
            } else {
                callback(AuthResult.Error("Failed to reload user: ${error.localizedDescription}"))
            }
        }
    }

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    actual fun signOut() {
        val auth = FIRAuth.auth() ?: return
        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            auth.signOut(error = errorPtr.ptr)
        }
    }

    actual fun isUserSignedIn(): Boolean {
        return FIRAuth.auth()?.currentUser() != null
    }

    actual fun getCurrentUserId(): String? {
        return FIRAuth.auth()?.currentUser()?.uid()
    }

    actual fun getCurrentUserEmail(): String? {
        return FIRAuth.auth()?.currentUser()?.email()
    }

    actual fun changePassword(newPassword: String, callback: (AuthResult) -> Unit) {
        val user = FIRAuth.auth()?.currentUser()
        if (user == null) {
            callback(AuthResult.Error("No user is currently signed in"))
            return
        }
        user.updatePassword(newPassword) { error: NSError? ->
            if (error == null) {
                callback(AuthResult.Success)
            } else {
                callback(AuthResult.Error("Failed to change password: ${error.localizedDescription}"))
            }
        }
    }

    actual fun deleteAccount(callback: (AuthResult) -> Unit) {
        val user = FIRAuth.auth()?.currentUser()
        if (user == null) {
            callback(AuthResult.Error("No user is currently signed in"))
            return
        }
        user.deleteWithCompletion { error: NSError? ->
            if (error == null) {
                callback(AuthResult.Success)
            } else {
                callback(AuthResult.Error("Failed to delete account: ${error.localizedDescription}"))
            }
        }
    }
}
