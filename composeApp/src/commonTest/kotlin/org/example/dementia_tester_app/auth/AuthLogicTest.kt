package org.example.dementia_tester_app.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthLogicTest {

    @Test
    fun `sign in with valid credentials succeeds`() {
        val fake = FakeAuthService().apply { signInResult = AuthResult.Success }
        var result: AuthResult? = null

        fake.signIn("test@example.com", "correctPass123") { result = it }

        assertTrue(result is AuthResult.Success)
        assertTrue(fake.isUserSignedIn())
        assertEquals("test@example.com", fake.getCurrentUserEmail())
    }

    @Test
    fun `sign in with wrong password returns error`() {
        val fake = FakeAuthService().apply {
            signInResult = AuthResult.Error("Invalid password")
        }
        var result: AuthResult? = null

        fake.signIn("test@example.com", "wrongPass") { result = it }

        assertTrue(result is AuthResult.Error)
        assertEquals("Invalid password", (result as AuthResult.Error).message)
        assertTrue(!fake.isUserSignedIn())
    }
}
