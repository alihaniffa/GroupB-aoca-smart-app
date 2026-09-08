package org.example.dementia_tester_app.data

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseDatabase.FIRDataSnapshot
import cocoapods.FirebaseDatabase.FIRDatabase
import platform.Foundation.NSDictionary
import platform.Foundation.NSNull

/**
 * iOS implementation of UserSettingsService.
 *
 * User settings are stored in Firebase Realtime Database under:
 *
 * UserSettings/{userId}
 */
actual class UserSettingsService actual constructor() :
    UserSettingsServiceInterface {

    private val dbPath = "UserSettings"

    /**
     * Return the currently signed-in Firebase user's UID.
     */
    private fun currentUserId(): String? {
        return FIRAuth.auth()
            ?.currentUser()
            ?.uid()
    }

    /**
     * Return the Firebase Realtime Database root reference.
     */
    private fun rootRef() =
        FIRDatabase.database()
            ?.reference()

    // ------------------------------------------------------------------
    // loadSettings
    // ------------------------------------------------------------------

    actual override fun loadSettings(
        callback: (DatabaseResult<UserSettings>) -> Unit
    ) {
        val userId =
            currentUserId()

        if (userId == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        val ref =
            rootRef()
                ?.child(dbPath)
                ?.child(userId)

        if (ref == null) {
            callback(
                DatabaseResult.Error(
                    "Firebase not initialized"
                )
            )
            return
        }

        FirebaseDatabaseIosHelper.observeValueOnce(
            query = ref
        ) { snapshot ->

            if (
                snapshot == null ||
                !snapshot.exists()
            ) {
                callback(
                    DatabaseResult.Success(
                        UserSettings()
                    )
                )
                return@observeValueOnce
            }

            try {
                val data =
                    snapshotToMap(
                        snapshot
                    )

                if (data != null) {
                    callback(
                        DatabaseResult.Success(
                            UserSettings.fromMap(
                                data
                            )
                        )
                    )
                } else {
                    callback(
                        DatabaseResult.Success(
                            UserSettings()
                        )
                    )
                }

            } catch (t: Throwable) {
                callback(
                    DatabaseResult.Error(
                        "Failed to parse settings: ${t.message}"
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // saveSettings
    // ------------------------------------------------------------------

    actual override fun saveSettings(
        settings: UserSettings,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        val userId =
            currentUserId()

        if (userId == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )
            return
        }

        val ref =
            rootRef()
                ?.child(dbPath)
                ?.child(userId)

        if (ref == null) {
            callback(
                DatabaseResult.Error(
                    "Firebase not initialized"
                )
            )
            return
        }

        val objcMap:
                Map<Any?, Any?> =
            settings
                .toMap()
                .entries
                .associate {
                        (key, value) ->

                    (key as Any?) to
                            (value ?: NSNull())
                }

        ref.updateChildValues(
            values = objcMap,
            withCompletionBlock = {
                    error,
                    _ ->

                if (error == null) {
                    callback(
                        DatabaseResult.Success(
                            Unit
                        )
                    )
                } else {
                    callback(
                        DatabaseResult.Error(
                            "Failed to save settings: ${error.localizedDescription}"
                        )
                    )
                }
            }
        )
    }

    // ------------------------------------------------------------------
    // Firebase snapshot conversion
    // ------------------------------------------------------------------

    private fun snapshotToMap(
        snapshot: FIRDataSnapshot
    ): Map<String, Any?>? {

        val value =
            snapshot.value

        return when (value) {

            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                value as? Map<String, Any?>
            }

            is NSDictionary ->
                nsDictionaryToMap(
                    value
                )

            else ->
                null
        }
    }

    // ------------------------------------------------------------------
    // NSDictionary conversion
    // ------------------------------------------------------------------

    private fun nsDictionaryToMap(
        dict: NSDictionary
    ): Map<String, Any?> {

        val result =
            mutableMapOf<String, Any?>()

        val keyEnumerator =
            dict.keyEnumerator()

        while (true) {

            val rawKey =
                keyEnumerator.nextObject()
                    ?: break

            val key =
                rawKey.toString()

            val value =
                dict.objectForKey(
                    rawKey
                )

            result[key] =
                when (value) {

                    is NSDictionary ->
                        nsDictionaryToMap(
                            value
                        )

                    is NSNull ->
                        null

                    else ->
                        value
                }
        }

        return result
    }
}