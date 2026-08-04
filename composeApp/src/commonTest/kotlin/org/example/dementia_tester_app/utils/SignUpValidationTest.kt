package org.example.dementia_tester_app.utils

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class SignUpValidationTest {

    @Test
    fun `valid email passes format check`() {
        assertTrue("newuser@example.com".isValidEmail())
    }

    @Test
    fun `invalid email fails format check`() {
        assertFalse("not-an-email".isValidEmail())
    }

    @Test
    fun `matching passwords pass equality check`() {
        val password = "StrongPass123!"
        val confirmPassword = "StrongPass123!"
        assertTrue(password == confirmPassword)
    }

    @Test
    fun `mismatched passwords fail equality check`() {
        val password = "StrongPass123!"
        val confirmPassword = "DifferentPass456!"
        assertFalse(password == confirmPassword)
    }

    @Test
    fun `valid phone number passes format check`() {
        assertTrue("0412345678".isValidPhoneNumber())
    }

    @Test
    fun `invalid phone number fails format check`() {
        assertFalse("abc123".isValidPhoneNumber())
    }

    @Test
    fun `future date of birth returns null age`() {
        assertNull(calculateAgeFromDateOfBirth("01/01/2027"))
    }

    @Test
    fun `valid date of birth returns non-null age`() {
        assertNotNull(calculateAgeFromDateOfBirth("15/05/1990"))
    }
}