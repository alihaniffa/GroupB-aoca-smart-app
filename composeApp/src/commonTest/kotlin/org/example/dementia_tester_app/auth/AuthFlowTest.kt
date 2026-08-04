package org.example.dementia_tester_app.auth

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class AuthFlowTest {

    // T05: Sign up creates a new account
    @Test
    fun `sign up with valid credentials creates account successfully`() {
        val fake = FakeAuthService()
        var result: AuthResult? = null

        fake.signUp("newuser@example.com", "SecurePass123") { result = it }

        assertTrue(result is AuthResult.Success)
        assertTrue(fake.isUserSignedIn())
        assertEquals("newuser@example.com", fake.getCurrentUserEmail())
    }

    @Test
    fun `sign up fails when email already registered`() {
        val fake = FakeAuthService()
        fake.signUp("existing@example.com", "SecurePass123") {}
        fake.signOut()

        var result: AuthResult? = null
        fake.signUp("existing@example.com", "AnotherPass456") { result = it }

        assertTrue(result is AuthResult.Error)
    }

    // T06: Weak password triggers a warning
    @Test
    fun `sign up with weak password returns validation error`() {
        val fake = FakeAuthService()
        var result: AuthResult? = null

        fake.signUp("weakpass@example.com", "123") { result = it }

        assertTrue(result is AuthResult.Error)
        assertTrue((result as AuthResult.Error).message.contains("at least"))
        assertTrue(!fake.isUserSignedIn())
    }

    @Test
    fun `change password to weak value is rejected`() {
        val fake = FakeAuthService()
        fake.signUp("user@example.com", "GoodPass123") {}

        var result: AuthResult? = null
        fake.changePassword("abc") { result = it }

        assertTrue(result is AuthResult.Error)
    }

    // T07: Password reset workflow
    @Test
    fun `password reset succeeds for registered email`() {
        val fake = FakeAuthService()
        fake.signUp("resetme@example.com", "SecurePass123") {}
        fake.signOut()

        var result: AuthResult? = null
        fake.sendPasswordResetEmail("resetme@example.com") { result = it }

        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun `password reset fails for unregistered email`() {
        val fake = FakeAuthService()
        var result: AuthResult? = null

        fake.sendPasswordResetEmail("nobody@example.com") { result = it }

        assertTrue(result is AuthResult.Error)
    }

    @Test
    fun `sign in fails with incorrect password after sign up`() {
        val fake = FakeAuthService()
        fake.signUp("checklogin@example.com", "CorrectPass123") {}
        fake.signOut()

        var result: AuthResult? = null
        fake.signIn("checklogin@example.com", "WrongPassword") { result = it }

        assertTrue(result is AuthResult.Error)
    }

    @Test
    fun `simulated network failure returns error on sign up`() {
        val fake = FakeAuthService().apply { shouldFailNextOperation = true; nextErrorMessage = "Network error" }
        var result: AuthResult? = null

        fake.signUp("failcase@example.com", "SecurePass123") { result = it }

        assertTrue(result is AuthResult.Error)
        assertEquals("Network error", (result as AuthResult.Error).message)
    }
}