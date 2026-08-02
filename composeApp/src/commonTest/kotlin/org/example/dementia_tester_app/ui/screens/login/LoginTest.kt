package org.example.dementia_tester_app.ui.screens.login

import org.example.dementia_tester_app.auth.AuthResult
import org.example.dementia_tester_app.auth.AuthServiceInterface
import org.example.dementia_tester_app.utils.isValidEmail
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeAuthService : AuthServiceInterface {
    var shouldSucceed = true
    var errorMessage = "Invalid credentials"

    override fun signIn(email: String, password: String, callback: (AuthResult) -> Unit) {
        callback(if (shouldSucceed) AuthResult.Success else AuthResult.Error(errorMessage))
    }
    override fun signUp(email: String, password: String, callback: (AuthResult) -> Unit) = callback(AuthResult.Success)
    override fun sendPasswordResetEmail(email: String, callback: (AuthResult) -> Unit) = callback(AuthResult.Success)
    override fun sendEmailVerification(callback: (AuthResult) -> Unit) = callback(AuthResult.Success)
    override fun isEmailVerified(): Boolean = true
    override fun reloadUser(callback: (AuthResult) -> Unit) = callback(AuthResult.Success)
    override fun signOut() {}
    override fun isUserSignedIn(): Boolean = true
    override fun getCurrentUserId(): String? = "fake-uid"
    override fun getCurrentUserEmail(): String? = "test@example.com"
    override fun changePassword(newPassword: String, callback: (AuthResult) -> Unit) = callback(AuthResult.Success)
    override fun deleteAccount(callback: (AuthResult) -> Unit) = callback(AuthResult.Success)
}

class LoginLogicTest {
    @Test
    fun `valid email passes format check`() {
        assertEquals(true, "test@example.com".isValidEmail())
    }

    @Test
    fun `invalid email fails format check`() {
        assertEquals(false, "not-an-email".isValidEmail())
    }
}