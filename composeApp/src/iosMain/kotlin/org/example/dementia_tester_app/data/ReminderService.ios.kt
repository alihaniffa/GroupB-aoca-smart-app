package org.example.dementia_tester_app.data

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseDatabase.FIRDatabase
import platform.Foundation.NSDictionary
import platform.Foundation.NSError
import platform.Foundation.NSNull

/**
 * iOS implementation for reminder service backed by
 * Firebase Realtime Database.
 *
 * Reminders are stored under:
 *
 * Reminders/{userId}/{reminderId}
 *
 * The current-user methods are retained for normal patient use,
 * while the ForUser methods allow caregiver/doctor flows to work
 * with a selected patient's reminders.
 */
actual class ReminderService actual constructor() {

    private val remindersPath = "Reminders"

    /**
     * Return the currently signed-in user's Firebase UID.
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
    // createReminder
    // ------------------------------------------------------------------

    actual fun createReminder(
        reminder: Reminder,
        callback: (ReminderResult<Unit>) -> Unit
    ) {
        val userId =
            currentUserId()

        if (userId == null) {
            callback(
                ReminderResult.Error(
                    "User not authenticated."
                )
            )
            return
        }

        createReminderForUser(
            userId = userId,
            reminder = reminder,
            callback = callback
        )
    }

    // ------------------------------------------------------------------
    // createReminderForUser
    // ------------------------------------------------------------------

    actual fun createReminderForUser(
        userId: String,
        reminder: Reminder,
        callback: (ReminderResult<Unit>) -> Unit
    ) {
        if (userId.isBlank()) {
            callback(
                ReminderResult.Error(
                    "Invalid user ID"
                )
            )
            return
        }

        val root =
            rootRef()

        if (root == null) {
            callback(
                ReminderResult.Error(
                    "Firebase not initialized"
                )
            )
            return
        }

        val userRef =
            root
                .child(remindersPath)
                .child(userId)

        val data:
                Map<Any?, Any?> =
            mapOf(
                "taskType" to
                        (reminder.taskType ?: ""),

                "taskTime" to
                        (reminder.taskTime ?: ""),

                "taskName" to
                        (reminder.taskName ?: ""),

                "taskActive" to
                        if (reminder.taskActive) 1 else 0
            )

        val reminderId =
            reminder.id

        if (!reminderId.isNullOrBlank()) {

            val reminderRef =
                userRef.child(
                    reminderId
                )

            reminderRef.setValue(
                data
            ) {
                    error: NSError?,
                    _ ->

                if (error == null) {
                    callback(
                        ReminderResult.Success(
                            Unit
                        )
                    )
                } else {
                    callback(
                        ReminderResult.Error(
                            "Failed to create reminder: ${error.localizedDescription}"
                        )
                    )
                }
            }

            return
        }

        val autoRef =
            userRef.childByAutoId()

        val generatedId =
            autoRef.key()

        if (generatedId.isNullOrBlank()) {
            callback(
                ReminderResult.Error(
                    "Failed to generate key for reminder"
                )
            )
            return
        }

        autoRef.setValue(
            data
        ) {
                error: NSError?,
                _ ->

            if (error == null) {
                callback(
                    ReminderResult.Success(
                        Unit
                    )
                )
            } else {
                callback(
                    ReminderResult.Error(
                        "Failed to create reminder: ${error.localizedDescription}"
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // getReminders
    // ------------------------------------------------------------------

    actual fun getReminders(
        userId: String,
        callback: (ReminderResult<List<Reminder>>) -> Unit
    ) {
        if (userId.isBlank()) {
            callback(
                ReminderResult.Success(
                    emptyList()
                )
            )
            return
        }

        val root =
            rootRef()

        if (root == null) {
            callback(
                ReminderResult.Success(
                    emptyList()
                )
            )
            return
        }

        val ref =
            root
                .child(remindersPath)
                .child(userId)

        FirebaseDatabaseIosHelper.observeValueOnce(
            query = ref
        ) { snapshot ->

            if (
                snapshot == null ||
                !snapshot.exists()
            ) {
                callback(
                    ReminderResult.Success(
                        emptyList()
                    )
                )
                return@observeValueOnce
            }

            try {
                val reminders =
                    mutableListOf<Reminder>()

                val value =
                    snapshot.value

                when (value) {

                    is Map<*, *> -> {

                        for (
                        (key, rawValue)
                        in value
                        ) {
                            val childMap =
                                when (rawValue) {

                                    is Map<*, *> ->
                                        rawValue

                                    is NSDictionary ->
                                        nsDictionaryToKotlinMap(
                                            rawValue
                                        )

                                    else ->
                                        null
                                }
                                    ?: continue

                            val reminder =
                                mapToReminder(
                                    childMap
                                )

                            if (
                                reminder.id
                                    .isNullOrBlank()
                            ) {
                                reminder.id =
                                    key?.toString()
                            }

                            if (
                                reminder.userId
                                    .isNullOrBlank()
                            ) {
                                reminder.userId =
                                    userId
                            }

                            reminders.add(
                                reminder
                            )
                        }
                    }

                    is NSDictionary -> {

                        val keyEnumerator =
                            value.keyEnumerator()

                        while (true) {

                            val rawKey =
                                keyEnumerator
                                    .nextObject()
                                    ?: break

                            val rawValue =
                                value.objectForKey(
                                    rawKey
                                )

                            val childMap =
                                when (rawValue) {

                                    is Map<*, *> ->
                                        rawValue

                                    is NSDictionary ->
                                        nsDictionaryToKotlinMap(
                                            rawValue
                                        )

                                    else ->
                                        null
                                }
                                    ?: continue

                            val reminder =
                                mapToReminder(
                                    childMap
                                )

                            if (
                                reminder.id
                                    .isNullOrBlank()
                            ) {
                                reminder.id =
                                    rawKey.toString()
                            }

                            if (
                                reminder.userId
                                    .isNullOrBlank()
                            ) {
                                reminder.userId =
                                    userId
                            }

                            reminders.add(
                                reminder
                            )
                        }
                    }
                }

                callback(
                    ReminderResult.Success(
                        reminders
                    )
                )

            } catch (t: Throwable) {
                callback(
                    ReminderResult.Error(
                        "Failed to parse reminders: ${t.message}"
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // updateReminder
    // ------------------------------------------------------------------

    actual fun updateReminder(
        reminderId: String,
        updates: Map<String, Any?>,
        callback: (ReminderResult<Unit>) -> Unit
    ) {
        val userId =
            currentUserId()

        if (userId == null) {
            callback(
                ReminderResult.Error(
                    "User not authenticated."
                )
            )
            return
        }

        updateReminderForUser(
            userId = userId,
            reminderId = reminderId,
            updates = updates,
            callback = callback
        )
    }

    // ------------------------------------------------------------------
    // updateReminderForUser
    // ------------------------------------------------------------------

    actual fun updateReminderForUser(
        userId: String,
        reminderId: String,
        updates: Map<String, Any?>,
        callback: (ReminderResult<Unit>) -> Unit
    ) {
        if (userId.isBlank()) {
            callback(
                ReminderResult.Error(
                    "Invalid user ID"
                )
            )
            return
        }

        if (reminderId.isBlank()) {
            callback(
                ReminderResult.Error(
                    "Invalid reminder ID"
                )
            )
            return
        }

        val root =
            rootRef()

        if (root == null) {
            callback(
                ReminderResult.Error(
                    "Firebase not initialized"
                )
            )
            return
        }

        /*
         * Normalize taskActive to 1/0 so the iOS storage
         * representation stays consistent with the existing
         * Android/Firebase data.
         */
        val normalizedUpdates:
                Map<String, Any?> =
            updates.mapValues {
                    (key, value) ->

                if (key == "taskActive") {

                    when (value) {
                        is Boolean ->
                            if (value) 1 else 0

                        is Number ->
                            if (
                                value.toInt() != 0
                            ) {
                                1
                            } else {
                                0
                            }

                        is String ->
                            if (
                                value.equals(
                                    "true",
                                    ignoreCase = true
                                ) ||
                                value == "1"
                            ) {
                                1
                            } else {
                                0
                            }

                        else ->
                            value
                    }

                } else {
                    value
                }
            }

        /*
         * id and userId are represented by the Firebase path,
         * so they should not be overwritten as child fields.
         */
        val sanitizedUpdates =
            normalizedUpdates.filterKeys {
                it != "id" &&
                        it != "userId"
            }

        val objcUpdates:
                Map<Any?, Any?> =
            sanitizedUpdates
                .entries
                .associate {
                        (key, value) ->

                    (key as Any?) to
                            (value ?: NSNull())
                }

        val reminderRef =
            root
                .child(remindersPath)
                .child(userId)
                .child(reminderId)

        reminderRef.updateChildValues(
            values = objcUpdates,
            withCompletionBlock = {
                    error,
                    _ ->

                if (error == null) {
                    callback(
                        ReminderResult.Success(
                            Unit
                        )
                    )
                } else {
                    callback(
                        ReminderResult.Error(
                            "Failed to update reminder: ${error.localizedDescription}"
                        )
                    )
                }
            }
        )
    }

    // ------------------------------------------------------------------
    // deleteReminder
    // ------------------------------------------------------------------

    actual fun deleteReminder(
        reminderID: String,
        callback: (ReminderResult<Unit>) -> Unit
    ) {
        val userId =
            currentUserId()

        if (userId == null) {
            callback(
                ReminderResult.Error(
                    "User not authenticated."
                )
            )
            return
        }

        deleteReminderForUser(
            userId = userId,
            reminderID = reminderID,
            callback = callback
        )
    }

    // ------------------------------------------------------------------
    // deleteReminderForUser
    // ------------------------------------------------------------------

    actual fun deleteReminderForUser(
        userId: String,
        reminderID: String,
        callback: (ReminderResult<Unit>) -> Unit
    ) {
        if (userId.isBlank()) {
            callback(
                ReminderResult.Error(
                    "Invalid user ID"
                )
            )
            return
        }

        if (reminderID.isBlank()) {
            callback(
                ReminderResult.Error(
                    "Invalid reminder ID"
                )
            )
            return
        }

        val root =
            rootRef()

        if (root == null) {
            callback(
                ReminderResult.Error(
                    "Firebase not initialized"
                )
            )
            return
        }

        val reminderRef =
            root
                .child(remindersPath)
                .child(userId)
                .child(reminderID)

        reminderRef.removeValueWithCompletionBlock {
                error,
                _ ->

            if (error == null) {
                callback(
                    ReminderResult.Success(
                        Unit
                    )
                )
            } else {
                callback(
                    ReminderResult.Error(
                        "Failed to delete reminder: ${error.localizedDescription}"
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // NSDictionary conversion
    // ------------------------------------------------------------------

    private fun nsDictionaryToKotlinMap(
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
                        nsDictionaryToKotlinMap(
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

    // ------------------------------------------------------------------
    // Reminder conversion
    // ------------------------------------------------------------------

    private fun mapToReminder(
        map: Map<*, *>
    ): Reminder {

        val id =
            map["id"]
                ?.toString()

        val userId =
            map["userId"]
                ?.toString()

        val taskType =
            map["taskType"]
                ?.toString()

        val taskTime =
            map["taskTime"]
                ?.toString()

        val taskName =
            map["taskName"]
                ?.toString()

        val taskActive =
            anyToBoolean(
                map["taskActive"]
            ) ?: true

        return Reminder(
            id = id,
            userId = userId,
            taskType = taskType,
            taskTime = taskTime,
            taskName = taskName,
            taskActive = taskActive
        )
    }

    private fun anyToBoolean(
        value: Any?
    ): Boolean? {

        return when (value) {

            null ->
                null

            is Boolean ->
                value

            is Number ->
                value.toInt() != 0

            is String ->
                value.equals(
                    "true",
                    ignoreCase = true
                ) ||
                        value == "1"

            else ->
                null
        }
    }
}