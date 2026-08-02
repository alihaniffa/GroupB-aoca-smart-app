package org.example.dementia_tester_app.auth

class FakeAuthService : AuthServiceInterface {

    private data class FakeUser(val email: String, var password: String, var emailVerified: Boolean = false)

    private val registeredUsers = mutableMapOf<String, FakeUser>()
    private var currentUser: FakeUser? = null
    private var idCounter = 0
    private val userIds = mutableMapOf<String, String>()

    // When set, short-circuits signIn/signUp/sendPasswordResetEmail with this exact result
    var signInResult: AuthResult? = null

    var shouldFailNextOperation = false
    var nextErrorMessage = "Operation failed"

    private val minPasswordLength = 6

    override fun signIn(email: String, password: String, callback: (AuthResult) -> Unit) {
        signInResult?.let { stubbed ->
            if (stubbed is AuthResult.Success) {
                currentUser = registeredUsers[email] ?: FakeUser(email, password).also {
                    registeredUsers[email] = it
                    userIds[email] = "uid-${idCounter++}"
                }
            }
            callback(stubbed)
            return
        }
        if (shouldFailNextOperation) {
            callback(AuthResult.Error(nextErrorMessage)); shouldFailNextOperation = false; return
        }
        val user = registeredUsers[email]
        if (user == null) {
            callback(AuthResult.Error("No account found for this email")); return
        }
        if (user.password != password) {
            callback(AuthResult.Error("Incorrect password")); return
        }
        currentUser = user
        callback(AuthResult.Success)
    }

    override fun signUp(email: String, password: String, callback: (AuthResult) -> Unit) {
        signInResult?.let { stubbed ->
            if (stubbed is AuthResult.Success) {
                val newUser = FakeUser(email, password)
                registeredUsers[email] = newUser
                userIds[email] = "uid-${idCounter++}"
                currentUser = newUser
            }
            callback(stubbed)
            return
        }
        if (shouldFailNextOperation) {
            callback(AuthResult.Error(nextErrorMessage)); shouldFailNextOperation = false; return
        }
        if (registeredUsers.containsKey(email)) {
            callback(AuthResult.Error("Email already in use")); return
        }
        if (password.length < minPasswordLength) {
            callback(AuthResult.Error("Password should be at least $minPasswordLength characters")); return
        }
        val newUser = FakeUser(email, password)
        registeredUsers[email] = newUser
        userIds[email] = "uid-${idCounter++}"
        currentUser = newUser
        callback(AuthResult.Success)
    }

    override fun sendPasswordResetEmail(email: String, callback: (AuthResult) -> Unit) {
        signInResult?.let { callback(it); return }
        if (shouldFailNextOperation) {
            callback(AuthResult.Error(nextErrorMessage)); shouldFailNextOperation = false; return
        }
        if (!registeredUsers.containsKey(email)) {
            callback(AuthResult.Error("No account found for this email")); return
        }
        callback(AuthResult.Success)
    }

    override fun sendEmailVerification(callback: (AuthResult) -> Unit) {
        if (currentUser == null) { callback(AuthResult.Error("No user signed in")); return }
        callback(AuthResult.Success)
    }

    override fun isEmailVerified(): Boolean = currentUser?.emailVerified ?: false

    override fun reloadUser(callback: (AuthResult) -> Unit) {
        if (currentUser == null) { callback(AuthResult.Error("No user signed in")); return }
        callback(AuthResult.Success)
    }

    override fun signOut() { currentUser = null }

    override fun isUserSignedIn(): Boolean = currentUser != null

    override fun getCurrentUserId(): String? = currentUser?.let { userIds[it.email] }

    override fun getCurrentUserEmail(): String? = currentUser?.email

    override fun changePassword(newPassword: String, callback: (AuthResult) -> Unit) {
        val user = currentUser ?: run { callback(AuthResult.Error("No user signed in")); return }
        if (newPassword.length < minPasswordLength) {
            callback(AuthResult.Error("Password should be at least $minPasswordLength characters")); return
        }
        user.password = newPassword
        callback(AuthResult.Success)
    }

    override fun deleteAccount(callback: (AuthResult) -> Unit) {
        val user = currentUser ?: run { callback(AuthResult.Error("No user signed in")); return }
        registeredUsers.remove(user.email)
        userIds.remove(user.email)
        currentUser = null
        callback(AuthResult.Success)
    }

    fun markCurrentUserVerified() { currentUser?.emailVerified = true }
}