package org.example.dementia_tester_app.data

class FakeUserProfileService : UserProfileServiceInterface {
    var storedProfile: UserProfile? = null
    var shouldSucceed = true
    var errorMessage = "Operation failed"

    override fun getCurrentUserProfile(callback: (DatabaseResult<UserProfile>) -> Unit) {
        val profile = storedProfile
        if (profile != null) {
            callback(DatabaseResult.Success(profile))
        } else {
            callback(DatabaseResult.Error("No profile found"))
        }
    }

    override fun updateUserProfile(userProfile: UserProfile, callback: (DatabaseResult<Unit>) -> Unit) {
        if (shouldSucceed) {
            storedProfile = userProfile
            callback(DatabaseResult.Success(Unit))
        } else {
            callback(DatabaseResult.Error(errorMessage))
        }
    }

    override fun getAllUsers(callback: (DatabaseResult<List<UserProfile>>) -> Unit) {
        callback(DatabaseResult.Success(listOfNotNull(storedProfile)))
    }

    override fun uploadProfileImage(imageBytes: ByteArray, callback: (DatabaseResult<String>) -> Unit) {
        callback(if (shouldSucceed) DatabaseResult.Success("fake-image-url") else DatabaseResult.Error(errorMessage))
    }
}