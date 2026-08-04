package org.example.dementia_tester_app.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * Android implementation of AuthService using Firebase Auth
 */
actual class AuthService actual constructor() : AuthServiceInterface {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override actual fun signIn(email: String, password: String, callback: (AuthResult) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(AuthResult.Success)
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "Invalid credentials."
                        is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                        else -> task.exception?.message ?: "Authentication failed"
                    }
                    callback(AuthResult.Error(errorMessage))
                }
            }
    }

    override actual fun signUp(email: String, password: String, callback: (AuthResult) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(AuthResult.Success)
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthWeakPasswordException -> "Password is too weak."
                        is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                        is FirebaseAuthUserCollisionException -> "User with this email already exists."
                        else -> task.exception?.message ?: "Registration failed"
                    }
                    callback(AuthResult.Error(errorMessage))
                }
            }
    }

    override actual fun sendPasswordResetEmail(email: String, callback: (AuthResult) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(AuthResult.Success)
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "No user found with this email."
                        else -> "Failed to send password reset email: ${task.exception?.message ?: "Unknown error"}"
                    }
                    callback(AuthResult.Error(errorMessage))
                }
            }
    }

    override actual fun sendEmailVerification(callback: (AuthResult) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            user.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback(AuthResult.Success)
                    } else {
                        val errorMessage = "Failed to send verification email: ${task.exception?.message ?: "Unknown error"}"
                        callback(AuthResult.Error(errorMessage))
                    }
                }
        } else {
            callback(AuthResult.Error("No user is currently signed in"))
        }
    }

    override actual fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    override actual fun reloadUser(callback: (AuthResult) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            user.reload()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback(AuthResult.Success)
                    } else {
                        val errorMessage = "Failed to reload user: ${task.exception?.message ?: "Unknown error"}"
                        callback(AuthResult.Error(errorMessage))
                    }
                }
        } else {
            callback(AuthResult.Error("No user is currently signed in"))
        }
    }

    override actual fun signOut() {
        auth.signOut()
    }

    override actual fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    override actual fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override actual fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }
    override actual fun changePassword(newPassword: String, callback: (AuthResult) -> Unit) {
        val user = auth.currentUser

        if (user != null) {
            user.updatePassword(newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback(AuthResult.Success)
                    } else {
                        val errorMessage =
                            "Failed to change password: ${task.exception?.message ?: "Unknown error"}"
                        callback(AuthResult.Error(errorMessage))
                    }
                }
        } else {
            callback(AuthResult.Error("No user is currently signed in"))
        }
    }

    override actual fun deleteAccount(callback: (AuthResult) -> Unit) {
        val user = auth.currentUser

        if (user != null) {
            user.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback(AuthResult.Success)
                    } else {
                        val errorMessage =
                            "Failed to delete account: ${task.exception?.message ?: "Unknown error"}"
                        callback(AuthResult.Error(errorMessage))
                    }
                }
        } else {
            callback(AuthResult.Error("No user is currently signed in"))
        }
    }
}
