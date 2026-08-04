package org.example.dementia_tester_app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * Android actual — reads/writes UserSettings/{userId} in Firebase Realtime DB.
 * Fixes issue #11: settings now persist across app restarts.
 */
actual class UserSettingsService actual constructor() : UserSettingsServiceInterface {
    private val auth     = FirebaseAuth.getInstance()
    private val database = Firebase.database.reference
    private val dbPath   = "UserSettings"

    override actual fun loadSettings(callback: (DatabaseResult<UserSettings>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) { callback(DatabaseResult.Error("No user is signed in")); return }

        database.child(dbPath).child(userId).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    // First run — return defaults (will be written on first toggle)
                    callback(DatabaseResult.Success(UserSettings()))
                    return@addOnSuccessListener
                }
                try {
                    val data = snapshot.value as? Map<*, *>
                    callback(
                        if (data != null) DatabaseResult.Success(UserSettings.fromMap(data))
                        else              DatabaseResult.Success(UserSettings())
                    )
                } catch (e: Exception) {
                    callback(DatabaseResult.Error("Failed to parse settings: ${e.message}"))
                }
            }
            .addOnFailureListener { e ->
                callback(DatabaseResult.Error("Failed to load settings: ${e.message}"))
            }
    }

    override actual fun saveSettings(settings: UserSettings, callback: (DatabaseResult<Unit>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) { callback(DatabaseResult.Error("No user is signed in")); return }

        database.child(dbPath).child(userId)
            .updateChildren(settings.toMap())
            .addOnSuccessListener { callback(DatabaseResult.Success(Unit)) }
            .addOnFailureListener { e ->
                callback(DatabaseResult.Error("Failed to save settings: ${e.message}"))
            }
    }
}
