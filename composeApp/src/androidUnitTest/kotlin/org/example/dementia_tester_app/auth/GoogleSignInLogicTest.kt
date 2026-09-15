package org.example.dementia_tester_app.auth

import org.example.dementia_tester_app.data.UserProfile
import org.example.dementia_tester_app.data.UserType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoogleSignInLogicTest {

    @Test
    fun `createInitialUserProfile sets correct default patient fields from Google account`() {
        val uid = "google-uid-12345"
        val displayName = "Jane Doe"
        val email = "janedoe@example.com"

        val profile = GoogleSignInHelper.createInitialUserProfile(
            userId = uid,
            displayName = displayName,
            email = email
        )

        assertEquals(uid, profile.userId)
        assertEquals(displayName, profile.name)
        assertEquals(email, profile.email)
        assertEquals(UserType.USER, profile.userType)
        assertEquals("", profile.assignedDoctorId)
        assertEquals("", profile.assignedCaregiverId)
    }

    @Test
    fun `createInitialUserProfile handles null display name and email safely`() {
        val uid = "google-uid-empty"

        val profile = GoogleSignInHelper.createInitialUserProfile(
            userId = uid,
            displayName = null,
            email = null
        )

        assertEquals(uid, profile.userId)
        assertEquals("", profile.name)
        assertEquals("", profile.email)
        assertEquals(UserType.USER, profile.userType)
    }

    @Test
    fun `buildProfileUpdateMap includes RTDB timestamps and compliant schema`() {
        val profile = UserProfile(
            userId = "test-uid",
            name = "Test User",
            email = "test@example.com",
            userType = UserType.USER
        )
        val dummyTimestamp = 1716710400000L

        val map = GoogleSignInHelper.buildProfileUpdateMap(profile, timestamp = dummyTimestamp)

        assertEquals("test-uid", map["userId"])
        assertEquals("Test User", map["fullName"])
        assertEquals("test@example.com", map["email"])
        assertEquals("user", map["userType"])
        assertEquals(dummyTimestamp, map["createdAt"])
        assertEquals(dummyTimestamp, map["updatedAt"])
        assertEquals("", map["assignedDoctorId"])
        assertEquals("", map["assignedCaregiverId"])

        // Ensure UserProfile.fromMap can safely deserialize this RTDB payload
        val parsedProfile = UserProfile.fromMap(map, "test-uid")
        assertEquals(profile.userId, parsedProfile.userId)
        assertEquals(profile.name, parsedProfile.name)
        assertEquals(profile.email, parsedProfile.email)
        assertEquals(UserType.USER, parsedProfile.userType)
    }

    @Test
    fun `syncUserProfile saves new profile when user does not exist in RTDB`() {
        var profileChecked = false
        var profileSaved = false
        var savedData: Map<String, Any>? = null
        var successCalled = false
        var errorMessage: String? = null

        val handler = GoogleSignInProfileHandler(
            checkProfileExists = { uid, onResult ->
                profileChecked = true
                onResult(false, null) // User does not exist yet
            },
            saveProfile = { uid, data, onComplete ->
                profileSaved = true
                savedData = data
                onComplete(null) // Save succeeded
            }
        )

        handler.syncUserProfile(
            userId = "new-user-123",
            displayName = "New User",
            email = "new@example.com",
            onSuccess = { successCalled = true },
            onError = { errorMessage = it }
        )

        assertTrue(profileChecked, "Should check if profile exists")
        assertTrue(profileSaved, "Should save profile for new user")
        assertTrue(successCalled, "Should call onSuccess")
        assertEquals(null, errorMessage)
        assertNotNull(savedData)
        assertEquals("New User", savedData?.get("fullName"))
        assertEquals("new@example.com", savedData?.get("email"))
        assertEquals("user", savedData?.get("userType"))
    }

    @Test
    fun `syncUserProfile does not overwrite profile when user already exists in RTDB`() {
        var profileChecked = false
        var profileSaved = false
        var successCalled = false
        var errorMessage: String? = null

        val handler = GoogleSignInProfileHandler(
            checkProfileExists = { uid, onResult ->
                profileChecked = true
                onResult(true, null) // User already exists
            },
            saveProfile = { uid, data, onComplete ->
                profileSaved = true
                onComplete(null)
            }
        )

        handler.syncUserProfile(
            userId = "existing-user-123",
            displayName = "Existing User",
            email = "existing@example.com",
            onSuccess = { successCalled = true },
            onError = { errorMessage = it }
        )

        assertTrue(profileChecked, "Should check if profile exists")
        assertFalse(profileSaved, "Should NOT overwrite existing profile in RTDB")
        assertTrue(successCalled, "Should call onSuccess")
        assertEquals(null, errorMessage)
    }

    @Test
    fun `syncUserProfile invokes onError when RTDB save fails`() {
        var successCalled = false
        var errorMessage: String? = null

        val handler = GoogleSignInProfileHandler(
            checkProfileExists = { uid, onResult ->
                onResult(false, null)
            },
            saveProfile = { uid, data, onComplete ->
                onComplete(RuntimeException("Network permission denied"))
            }
        )

        handler.syncUserProfile(
            userId = "fail-user-123",
            displayName = "Fail User",
            email = "fail@example.com",
            onSuccess = { successCalled = true },
            onError = { errorMessage = it }
        )

        assertFalse(successCalled, "Should NOT call onSuccess when save fails")
        assertNotNull(errorMessage)
        assertTrue(errorMessage!!.contains("Network permission denied"))
    }

    @Test
    fun `syncUserProfile falls back to onSuccess when checkProfileExists throws network error`() {
        var successCalled = false
        var errorMessage: String? = null

        val handler = GoogleSignInProfileHandler(
            checkProfileExists = { uid, onResult ->
                onResult(false, RuntimeException("Temporary network timeout"))
            },
            saveProfile = { uid, data, onComplete ->
                onComplete(null)
            }
        )

        handler.syncUserProfile(
            userId = "fallback-user-123",
            displayName = "Fallback User",
            email = "fallback@example.com",
            onSuccess = { successCalled = true },
            onError = { errorMessage = it }
        )

        assertTrue(successCalled, "Should allow user through if existence check fails non-fatally")
        assertEquals(null, errorMessage)
    }

    @Test
    fun `GoogleSignInResultHandler returns expected error messages`() {
        val authError = GoogleSignInResultHandler.getErrorMessageForAuthFailure(
            RuntimeException("Invalid credential")
        )
        assertEquals("Invalid credential", authError)

        val nullAuthError = GoogleSignInResultHandler.getErrorMessageForAuthFailure(null)
        assertEquals("Auth failed", nullAuthError)

        val apiError = GoogleSignInResultHandler.getErrorMessageForApiException(null)
        assertEquals("Google sign in canceled or failed.", apiError)
    }
}
