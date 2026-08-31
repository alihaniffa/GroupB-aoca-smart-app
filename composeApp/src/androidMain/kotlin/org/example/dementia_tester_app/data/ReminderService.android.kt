package org.example.dementia_tester_app.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.example.dementia_tester_app.notifications.NotificationManagerProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Android implementation of ReminderService.
 *
 * Normal patient operations use the authenticated UID.
 *
 * Caregiver operations can explicitly provide the
 * selected patient's UID.
 */
actual class ReminderService actual constructor() {

    private val database: FirebaseDatabase =
        FirebaseDatabase.getInstance()

    private val auth: FirebaseAuth =
        FirebaseAuth.getInstance()

    private val notificationManager =
        NotificationManagerProvider
            .getNotificationManager()

    /**
     * Schedule or cancel a local reminder notification.
     *
     * Local notifications are only scheduled when the
     * reminder belongs to the currently logged-in user.
     *
     * If a caregiver creates a reminder for a patient,
     * we do not schedule that reminder on the caregiver's
     * device because it belongs to the patient.
     */
    private fun scheduleLocalNotification(
        reminder: Reminder
    ) {

        val currentUserId =
            auth.currentUser?.uid

        /*
         * Do not schedule somebody else's reminder
         * on the currently logged-in device.
         */
        if (
            reminder.userId != null &&
            reminder.userId != currentUserId
        ) {
            return
        }

        if (
            reminder.taskActive &&
            !reminder.taskTime.isNullOrBlank() &&
            !reminder.id.isNullOrBlank()
        ) {

            try {

                val timeString =
                    reminder.taskTime!!

                val sdf =
                    SimpleDateFormat(
                        "hh:mm a",
                        Locale.US
                    )

                val normalizedTimeString =
                    timeString
                        .replace(
                            "AM",
                            " AM"
                        )
                        .replace(
                            "PM",
                            " PM"
                        )

                val date =
                    sdf.parse(
                        normalizedTimeString
                    )

                if (date != null) {

                    val calendar =
                        Calendar.getInstance()

                    val now =
                        Calendar.getInstance()

                    val timeCalendar =
                        Calendar.getInstance()

                    timeCalendar.time =
                        date

                    calendar.set(
                        Calendar.HOUR_OF_DAY,
                        timeCalendar.get(
                            Calendar.HOUR_OF_DAY
                        )
                    )

                    calendar.set(
                        Calendar.MINUTE,
                        timeCalendar.get(
                            Calendar.MINUTE
                        )
                    )

                    calendar.set(
                        Calendar.SECOND,
                        0
                    )

                    calendar.set(
                        Calendar.MILLISECOND,
                        0
                    )

                    /*
                     * If the selected time has already
                     * passed today, schedule tomorrow.
                     */
                    if (
                        calendar.before(
                            now
                        )
                    ) {

                        calendar.add(
                            Calendar.DATE,
                            1
                        )
                    }

                    val message =
                        "Reminder: ${reminder.taskName} (${reminder.taskType})"

                    notificationManager
                        .scheduleNotificationAt(
                            reminder.id!!,
                            message,
                            calendar.timeInMillis
                        )

                    Log.d(
                        "ReminderService",
                        "Scheduled notification for ${reminder.id} at ${calendar.time}"
                    )
                }

            } catch (
                e: Exception
            ) {

                Log.e(
                    "ReminderService",
                    "Failed to schedule notification: ${e.message}"
                )
            }

        } else if (
            !reminder.id.isNullOrBlank()
        ) {

            notificationManager
                .cancelNotification(
                    reminder.id!!
                )

            Log.d(
                "ReminderService",
                "Cancelled notification for ${reminder.id}"
            )
        }
    }

    /**
     * Get the reminder reference for a specific UID.
     */
    private fun getRemindersRefForUser(
        userId: String
    ): DatabaseReference? {

        if (
            auth.currentUser == null
        ) {
            return null
        }

        if (
            userId.isBlank()
        ) {
            return null
        }

        return database
            .getReference(
                "Reminders"
            )
            .child(
                userId
            )
    }

    /**
     * Get reminder reference belonging to
     * the authenticated user.
     */
    private fun getCurrentUserRemindersRef():
            DatabaseReference? {

        val userId =
            auth.currentUser?.uid
                ?: return null

        return getRemindersRefForUser(
            userId
        )
    }

    /**
     * Create a reminder for the currently
     * authenticated user.
     */
    actual fun createReminder(
        reminder: Reminder,
        callback: (ReminderResult<Unit>) -> Unit
    ) {

        val currentUserId =
            auth.currentUser?.uid

        if (
            currentUserId == null
        ) {

            callback(
                ReminderResult.Error(
                    "User not authenticated."
                )
            )

            return
        }

        createReminderForUser(
            userId =
                currentUserId,
            reminder =
                reminder,
            callback =
                callback
        )
    }

    /**
     * Create a reminder for a specific user.
     *
     * Firebase security rules determine whether
     * the authenticated account is authorised
     * to write to this user's reminder node.
     */
    actual fun createReminderForUser(
        userId: String,
        reminder: Reminder,
        callback: (ReminderResult<Unit>) -> Unit
    ) {

        val remindersRef =
            getRemindersRefForUser(
                userId
            )

        if (
            remindersRef == null
        ) {

            callback(
                ReminderResult.Error(
                    "User not authenticated or invalid target user."
                )
            )

            return
        }

        val newReminderRef =
            reminder.id
                ?.let {
                    remindersRef
                        .child(it)
                }
                ?: remindersRef.push()

        /*
         * Ensure reminder object contains
         * its generated Firebase ID.
         */
        reminder.id =
            newReminderRef.key

        /*
         * The reminder belongs to the target
         * user, not necessarily the logged-in user.
         */
        reminder.userId =
            userId

        val toStore:
                Map<String, Any?> =
            mapOf(
                "taskType" to
                        (
                                reminder.taskType
                                    ?: ""
                                ),

                "taskTime" to
                        (
                                reminder.taskTime
                                    ?: ""
                                ),

                "taskName" to
                        (
                                reminder.taskName
                                    ?: ""
                                ),

                /*
                 * Existing database format stores
                 * taskActive as 1 or 0.
                 */
                "taskActive" to
                        if (
                            reminder.taskActive
                        ) {
                            1
                        } else {
                            0
                        }
            )

        newReminderRef
            .setValue(
                toStore
            )
            .addOnSuccessListener {

                /*
                 * Only schedules locally if userId
                 * belongs to the current user.
                 */
                scheduleLocalNotification(
                    reminder
                )

                callback(
                    ReminderResult.Success(
                        Unit
                    )
                )
            }
            .addOnFailureListener { e ->

                callback(
                    ReminderResult.Error(
                        "Failed to create reminder: ${e.message}"
                    )
                )
            }
    }

    /**
     * Get reminders for the supplied user ID.
     */
    actual fun getReminders(
        userId: String,
        callback: (ReminderResult<List<Reminder>>) -> Unit
    ) {

        val remindersRef =
            getRemindersRefForUser(
                userId
            )

        if (
            remindersRef == null
        ) {

            callback(
                ReminderResult.Error(
                    "User not authenticated or invalid target user."
                )
            )

            return
        }

        remindersRef
            .addListenerForSingleValueEvent(
                object :
                    ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val reminders =
                            mutableListOf<Reminder>()

                        for (
                        child in snapshot.children
                        ) {

                            val taskType =
                                child
                                    .child(
                                        "taskType"
                                    )
                                    .value
                                    ?.toString()

                            val taskTime =
                                child
                                    .child(
                                        "taskTime"
                                    )
                                    .value
                                    ?.toString()

                            val taskName =
                                child
                                    .child(
                                        "taskName"
                                    )
                                    .value
                                    ?.toString()

                            val raw =
                                child
                                    .child(
                                        "taskActive"
                                    )
                                    .value

                            val taskActive =
                                when (
                                    raw
                                ) {

                                    is Boolean ->
                                        raw

                                    is Number ->
                                        raw.toInt() != 0

                                    is String ->
                                        raw.equals(
                                            "true",
                                            ignoreCase =
                                                true
                                        ) ||
                                                raw == "1"

                                    else ->
                                        true
                                }

                            val reminder =
                                Reminder(
                                    id =
                                        child.key,

                                    /*
                                     * Important:
                                     * use requested userId rather
                                     * than authenticated UID.
                                     */
                                    userId =
                                        userId,

                                    taskType =
                                        taskType,

                                    taskTime =
                                        taskTime,

                                    taskName =
                                        taskName,

                                    taskActive =
                                        taskActive
                                )

                            reminders.add(
                                reminder
                            )
                        }

                        callback(
                            ReminderResult.Success(
                                reminders
                            )
                        )
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        callback(
                            ReminderResult.Error(
                                "Firebase read cancelled: ${error.message}"
                            )
                        )
                    }
                }
            )
    }

    /**
     * Update reminder belonging to the
     * currently authenticated user.
     */
    actual fun updateReminder(
        reminderId: String,
        updates: Map<String, Any?>,
        callback: (ReminderResult<Unit>) -> Unit
    ) {

        val currentUserId =
            auth.currentUser?.uid

        if (
            currentUserId == null
        ) {

            callback(
                ReminderResult.Error(
                    "User not authenticated."
                )
            )

            return
        }

        updateReminderForUser(
            userId =
                currentUserId,
            reminderId =
                reminderId,
            updates =
                updates,
            callback =
                callback
        )
    }

    /**
     * Update reminder belonging to a
     * specified user.
     */
    actual fun updateReminderForUser(
        userId: String,
        reminderId: String,
        updates: Map<String, Any?>,
        callback: (ReminderResult<Unit>) -> Unit
    ) {

        val remindersRef =
            getRemindersRefForUser(
                userId
            )

        if (
            remindersRef == null
        ) {

            callback(
                ReminderResult.Error(
                    "User not authenticated or invalid target user."
                )
            )

            return
        }

        if (
            reminderId.isBlank()
        ) {

            callback(
                ReminderResult.Error(
                    "Invalid reminder ID."
                )
            )

            return
        }

        val normalizedUpdates =
            updates.mapValues {
                    (
                        key,
                        value
                    ) ->

                if (
                    key == "taskActive"
                ) {

                    when (
                        value
                    ) {

                        is Boolean ->
                            if (value) {
                                1
                            } else {
                                0
                            }

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
                                    ignoreCase =
                                        true
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

        val filteredUpdates =
            normalizedUpdates
                .filterKeys {

                    it != "id" &&
                            it != "userId"
                }

        remindersRef
            .child(
                reminderId
            )
            .updateChildren(
                filteredUpdates
            )
            .addOnSuccessListener {

                /*
                 * Fetch complete reminder after
                 * update for notification scheduling.
                 */
                remindersRef
                    .child(
                        reminderId
                    )
                    .get()
                    .addOnSuccessListener { snapshot ->

                        val taskType =
                            snapshot
                                .child(
                                    "taskType"
                                )
                                .value
                                ?.toString()

                        val taskTime =
                            snapshot
                                .child(
                                    "taskTime"
                                )
                                .value
                                ?.toString()

                        val taskName =
                            snapshot
                                .child(
                                    "taskName"
                                )
                                .value
                                ?.toString()

                        val raw =
                            snapshot
                                .child(
                                    "taskActive"
                                )
                                .value

                        val taskActive =
                            when (
                                raw
                            ) {

                                is Boolean ->
                                    raw

                                is Number ->
                                    raw.toInt() != 0

                                is String ->
                                    raw.equals(
                                        "true",
                                        ignoreCase =
                                            true
                                    ) ||
                                            raw == "1"

                                else ->
                                    true
                            }

                        val updatedReminder =
                            Reminder(
                                id =
                                    reminderId,
                                userId =
                                    userId,
                                taskType =
                                    taskType,
                                taskTime =
                                    taskTime,
                                taskName =
                                    taskName,
                                taskActive =
                                    taskActive
                            )

                        scheduleLocalNotification(
                            updatedReminder
                        )
                    }

                callback(
                    ReminderResult.Success(
                        Unit
                    )
                )
            }
            .addOnFailureListener { e ->

                callback(
                    ReminderResult.Error(
                        "Failed to update reminder: ${e.message}"
                    )
                )
            }
    }

    /**
     * Delete reminder belonging to
     * authenticated user.
     */
    actual fun deleteReminder(
        reminderID: String,
        callback: (ReminderResult<Unit>) -> Unit
    ) {

        val currentUserId =
            auth.currentUser?.uid

        if (
            currentUserId == null
        ) {

            callback(
                ReminderResult.Error(
                    "User not authenticated."
                )
            )

            return
        }

        deleteReminderForUser(
            userId =
                currentUserId,
            reminderID =
                reminderID,
            callback =
                callback
        )
    }

    /**
     * Delete reminder belonging to
     * a specified user.
     */
    actual fun deleteReminderForUser(
        userId: String,
        reminderID: String,
        callback: (ReminderResult<Unit>) -> Unit
    ) {

        val remindersRef =
            getRemindersRefForUser(
                userId
            )

        if (
            remindersRef == null
        ) {

            callback(
                ReminderResult.Error(
                    "User not authenticated or invalid target user."
                )
            )

            return
        }

        if (
            reminderID.isBlank()
        ) {

            callback(
                ReminderResult.Error(
                    "Invalid reminder ID."
                )
            )

            return
        }

        remindersRef
            .child(
                reminderID
            )
            .removeValue()
            .addOnSuccessListener {

                /*
                 * Only cancel local notification
                 * when this reminder belongs to the
                 * authenticated account.
                 */
                if (
                    userId ==
                    auth.currentUser?.uid
                ) {

                    notificationManager
                        .cancelNotification(
                            reminderID
                        )
                }

                callback(
                    ReminderResult.Success(
                        Unit
                    )
                )
            }
            .addOnFailureListener { e ->

                callback(
                    ReminderResult.Error(
                        "Failed to delete reminder: ${e.message}"
                    )
                )
            }
    }
}