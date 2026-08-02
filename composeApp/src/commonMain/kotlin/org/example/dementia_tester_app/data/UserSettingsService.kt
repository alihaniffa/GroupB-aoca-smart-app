package org.example.dementia_tester_app.data

interface UserSettingsServiceInterface {
    fun loadSettings(callback: (DatabaseResult<UserSettings>) -> Unit)
    fun saveSettings(settings: UserSettings, callback: (DatabaseResult<Unit>) -> Unit)
}
/**
 * Holds all 12 user-configurable toggles / preferences.
 * Persisted to Firebase at UserSettings/{userId} (fixes issue #11).
 */
data class UserSettings(
    val textSize:              String  = "Medium",
    val highContrastMode:      Boolean = false,
    val screenReader:          Boolean = false,
    val reduceMotion:          Boolean = false,
    val colorBlindMode:        Boolean = false,
    val appointmentReminders:  Boolean = true,
    val medicationReminders:   Boolean = true,
    val testReminders:         Boolean = true,
    val appUpdates:            Boolean = false,
    val emailNotifications:    Boolean = false,
    val dataSharing:           Boolean = false,
    val syncWithCloud:         Boolean = true,
    val darkMode:              Boolean = false

) {
    fun toMap(): Map<String, Any> = mapOf(
        "textSize"             to textSize,
        "highContrastMode"     to highContrastMode,
        "screenReader"         to screenReader,
        "reduceMotion"         to reduceMotion,
        "colorBlindMode"       to colorBlindMode,
        "appointmentReminders" to appointmentReminders,
        "medicationReminders"  to medicationReminders,
        "testReminders"        to testReminders,
        "appUpdates"           to appUpdates,
        "emailNotifications"   to emailNotifications,
        "dataSharing"          to dataSharing,
        "syncWithCloud"        to syncWithCloud,
        "darkMode"             to darkMode
    )

    companion object {
        fun fromMap(map: Map<*, *>): UserSettings {
            fun bool(k: String, d: Boolean)  = (map[k] as? Boolean) ?: d
            fun str (k: String, d: String)   = (map[k] as? String)  ?: d
            return UserSettings(
                textSize             = str ("textSize",             "Medium"),
                highContrastMode     = bool("highContrastMode",     false),
                screenReader         = bool("screenReader",         false),
                reduceMotion         = bool("reduceMotion",         false),
                colorBlindMode       = bool("colorBlindMode",       false),
                appointmentReminders = bool("appointmentReminders", true),
                medicationReminders  = bool("medicationReminders",  true),
                testReminders        = bool("testReminders",        true),
                appUpdates           = bool("appUpdates",           false),
                emailNotifications   = bool("emailNotifications",   false),
                dataSharing          = bool("dataSharing",          false),
                syncWithCloud        = bool("syncWithCloud",        true),
                darkMode             = bool("darkMode",             false)
            )
        }
    }
}
/**
 * Expect class - load/save UserSettings from Firebase.
 * Fixes issue #11 (settings reset on every app restart).
 */
expect class UserSettingsService() : UserSettingsServiceInterface {
    override fun loadSettings(callback: (DatabaseResult<UserSettings>) -> Unit)
    override fun saveSettings(settings: UserSettings, callback: (DatabaseResult<Unit>) -> Unit)
}

