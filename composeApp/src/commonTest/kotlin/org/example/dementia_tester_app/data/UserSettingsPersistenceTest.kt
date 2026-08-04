package org.example.dementia_tester_app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserSettingsPersistenceTest {

    @Test
    fun `multiple saves overwrite previous settings correctly`() {
        val fake = FakeUserSettingsService()

        fake.saveSettings(UserSettings(textSize = "Small")) {}
        fake.saveSettings(UserSettings(textSize = "Large", darkMode = true)) {}

        var result: DatabaseResult<UserSettings>? = null
        fake.loadSettings { result = it }

        val settings = (result as DatabaseResult.Success).data
        assertEquals("Large", settings.textSize)
        assertTrue(settings.darkMode)
    }

    @Test
    fun `separate service instances do not share state`() {
        val fakeA = FakeUserSettingsService()
        val fakeB = FakeUserSettingsService()

        fakeA.saveSettings(UserSettings(darkMode = true)) {}

        var resultB: DatabaseResult<UserSettings>? = null
        fakeB.loadSettings { resultB = it }

        assertTrue(!(resultB as DatabaseResult.Success).data.darkMode)
    }
}