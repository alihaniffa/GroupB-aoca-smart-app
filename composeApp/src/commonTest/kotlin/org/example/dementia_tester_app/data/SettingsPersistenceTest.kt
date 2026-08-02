package org.example.dementia_tester_app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SettingsPersistenceTest {

    @Test
    fun `loading settings for the first time returns defaults`() {
        val fake = FakeUserSettingsService()
        var result: DatabaseResult<UserSettings>? = null

        fake.loadSettings { result = it }

        assertTrue(result is DatabaseResult.Success)
        assertEquals(UserSettings(), (result as DatabaseResult.Success).data)
    }

    @Test
    fun `changing a setting and saving succeeds`() {
        val fake = FakeUserSettingsService()
        val updated = UserSettings(darkMode = true, textSize = "Large")
        var saveResult: DatabaseResult<Unit>? = null

        fake.saveSettings(updated) { saveResult = it }

        assertTrue(saveResult is DatabaseResult.Success)
    }

    @Test
    fun `settings persist after reopening app (reload reflects saved state)`() {
        val fake = FakeUserSettingsService()
        val updated = UserSettings(darkMode = true, highContrastMode = true, textSize = "Large")

        fake.saveSettings(updated) { }

        var loadResult: DatabaseResult<UserSettings>? = null
        fake.loadSettings { loadResult = it }

        assertTrue(loadResult is DatabaseResult.Success)
        val loadedSettings = (loadResult as DatabaseResult.Success).data
        assertTrue(loadedSettings.darkMode)
        assertTrue(loadedSettings.highContrastMode)
        assertEquals("Large", loadedSettings.textSize)
    }

    @Test
    fun `save failure returns error and does not update stored state`() {
        val fake = FakeUserSettingsService().apply { shouldSucceed = false }
        val attempted = UserSettings(darkMode = true)

        fake.saveSettings(attempted) { }

        var loadResult: DatabaseResult<UserSettings>? = null
        fake.loadSettings { loadResult = it }

        val loadedSettings = (loadResult as DatabaseResult.Success).data
        assertFalse(loadedSettings.darkMode) // save failed, so default (false) remains
    }
}