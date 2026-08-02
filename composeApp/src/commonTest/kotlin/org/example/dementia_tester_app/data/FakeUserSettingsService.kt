package org.example.dementia_tester_app.data

class FakeUserSettingsService : UserSettingsServiceInterface {
    private var storedSettings: UserSettings? = null
    var shouldSucceed = true
    var errorMessage = "Failed to load/save settings"

    override fun loadSettings(callback: (DatabaseResult<UserSettings>) -> Unit) {
        val current = storedSettings
        if (current != null) {
            callback(DatabaseResult.Success(current))
        } else {
            callback(DatabaseResult.Success(UserSettings())) // defaults, mirrors first-time load
        }
    }

    override fun saveSettings(settings: UserSettings, callback: (DatabaseResult<Unit>) -> Unit) {
        if (shouldSucceed) {
            storedSettings = settings
            callback(DatabaseResult.Success(Unit))
        } else {
            callback(DatabaseResult.Error(errorMessage))
        }
    }
}