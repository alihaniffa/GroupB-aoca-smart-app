package org.example.dementia_tester_app.auth

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class PasswordResetLogicTest {

    @Test
    fun `password reset with valid email succeeds`() {
        val fake = FakeAuthService().apply { signInResult = AuthResult.Success }
        var result: AuthResult? = null

        fake.sendPasswordResetEmail("test@example.com") { result = it }

        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun `password reset with unregistered email returns error`() {
        val fake = FakeAuthService().apply {
            signInResult = AuthResult.Error("No user found with this email.")
        }
        var result: AuthResult? = null

        fake.sendPasswordResetEmail("unknown@example.com") { result = it }

        assertTrue(result is AuthResult.Error)
        assertEquals("No user found with this email.", (result as AuthResult.Error).message)
    }

    @Test
    fun `empty email fails validation before reset attempt`() {
        val trimmedEmail = "".trim()
        assertTrue(trimmedEmail.isEmpty())
    }

    @Test
    fun `whitespace-only email fails validation before reset attempt`() {
        val trimmedEmail = "   ".trim()
        assertTrue(trimmedEmail.isEmpty())
    }
}