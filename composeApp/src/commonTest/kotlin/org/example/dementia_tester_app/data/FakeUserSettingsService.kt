package org.example.dementia_tester_app.data

class FakeUserSettingsService : UserSettingsServiceInterface {

    private var storedSettings: UserSettings = UserSettings()
    var shouldSucceed = true
    var errorMessage = "Failed to save settings"

    override fun loadSettings(callback: (DatabaseResult<UserSettings>) -> Unit) {
        callback(DatabaseResult.Success(storedSettings))
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