package org.example.dementia_tester_app.data

/**
 * Data class to represent a reminder.
 */
data class Reminder(
    var id: String? = null,
    var userId: String? = null,
    var taskType: String? = "",
    var taskTime: String? = "",
    var taskName: String? = "",
    var taskActive: Boolean = true
)

/**
 * Result class to handle the results of reminder operations.
 */
sealed class ReminderResult<out T> {

    data class Success<T>(
        val data: T
    ) : ReminderResult<T>()

    data class Error(
        val message: String
    ) : ReminderResult<Nothing>()
}

/**
 * Reminder service.
 *
 * Existing methods continue to operate on the
 * currently authenticated user's reminders.
 *
 * The target-user methods allow an authorised
 * caregiver to manage an assigned patient's reminders.
 */
expect class ReminderService() {

    /**
     * Create a reminder for the currently
     * authenticated user.
     */
    fun createReminder(
        reminder: Reminder,
        callback: (ReminderResult<Unit>) -> Unit
    )

    /**
     * Create a reminder for a specific user.
     *
     * Used when a caregiver creates a reminder
     * on behalf of an assigned patient.
     */
    fun createReminderForUser(
        userId: String,
        reminder: Reminder,
        callback: (ReminderResult<Unit>) -> Unit
    )

    /**
     * Get reminders belonging to a specific user.
     */
    fun getReminders(
        userId: String,
        callback: (ReminderResult<List<Reminder>>) -> Unit
    )

    /**
     * Update a reminder belonging to the
     * currently authenticated user.
     */
    fun updateReminder(
        reminderId: String,
        updates: Map<String, Any?>,
        callback: (ReminderResult<Unit>) -> Unit
    )

    /**
     * Update a reminder belonging to a specific user.
     */
    fun updateReminderForUser(
        userId: String,
        reminderId: String,
        updates: Map<String, Any?>,
        callback: (ReminderResult<Unit>) -> Unit
    )

    /**
     * Delete a reminder belonging to the
     * currently authenticated user.
     */
    fun deleteReminder(
        reminderID: String,
        callback: (ReminderResult<Unit>) -> Unit
    )

    /**
     * Delete a reminder belonging to a specific user.
     */
    fun deleteReminderForUser(
        userId: String,
        reminderID: String,
        callback: (ReminderResult<Unit>) -> Unit
    )
}