package org.example.dementia_tester_app.auth

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SignUpLogicTest {

    @Test
    fun `sign up with valid email and strong password succeeds`() {
        val fake = FakeAuthService().apply { signInResult = AuthResult.Success }
        var result: AuthResult? = null

        fake.signUp("newuser@example.com", "StrongPass123!") { result = it }

        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun `sign up with weak password returns error`() {
        val fake = FakeAuthService().apply {
            signInResult = AuthResult.Error("Password is too weak.")
        }
        var result: AuthResult? = null

        fake.signUp("newuser@example.com", "123") { result = it }

        assertTrue(result is AuthResult.Error)
        assertEquals("Password is too weak.", (result as AuthResult.Error).message)
    }

    @Test
    fun `sign up with existing email returns collision error`() {
        val fake = FakeAuthService().apply {
            signInResult = AuthResult.Error("User with this email already exists.")
        }
        var result: AuthResult? = null

        fake.signUp("existing@example.com", "ValidPass123!") { result = it }

        assertTrue(result is AuthResult.Error)
        assertEquals("User with this email already exists.", (result as AuthResult.Error).message)
    }
}