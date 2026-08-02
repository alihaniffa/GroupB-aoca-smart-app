package org.example.dementia_tester_app.auth

class FakeAuthService : AuthServiceInterface {
    var signInResult: AuthResult = AuthResult.Success
    var signedIn = false
    var currentEmail: String? = null

    override fun signIn(email: String, password: String, callback: (AuthResult) -> Unit) {
        if (signInResult is AuthResult.Success) {
            signedIn = true
            currentEmail = email
        }
        callback(signInResult)
    }
    override fun signUp(email: String, password: String, callback: (AuthResult) -> Unit) {
        callback(signInResult)
    }
    override fun sendPasswordResetEmail(email: String, callback: (AuthResult) -> Unit) = callback(AuthResult.Success)
    override fun sendEmailVerification(callback: (AuthResult) -> Unit) = callback(AuthResult.Success)
    override fun isEmailVerified(): Boolean = true
    override fun reloadUser(callback: (AuthResult) -> Unit) = callback(AuthResult.Success)
    override fun signOut() { signedIn = false }
    override fun isUserSignedIn(): Boolean = signedIn
    override fun getCurrentUserId(): String? = if (signedIn) "fake-uid" else null
    override fun getCurrentUserEmail(): String? = currentEmail
    override fun changePassword(newPassword: String, callback: (AuthResult) -> Unit) = callback(AuthResult.Success)
    override fun deleteAccount(callback: (AuthResult) -> Unit) = callback(AuthResult.Success)
}
