package org.example.dementia_tester_app.data

import kotlinx.coroutines.flow.Flow

/**
 * Interface for activity tracking service.
 *
 * Normal methods operate on the currently
 * authenticated user.
 *
 * Target-user methods allow an authorised
 * caregiver to work with an assigned
 * patient's activities.
 */
interface ActivityServiceInterface {

    /**
     * Log an activity for the currently
     * authenticated user.
     */
    fun logActivity(
        activity: Activity,
        callback: (DatabaseResult<Unit>) -> Unit
    )

    /**
     * Log an activity for a specific user.
     *
     * Used when a caregiver performs an
     * activity on behalf of a patient.
     */
    fun logActivityForUser(
        userId: String,
        activity: Activity,
        callback: (DatabaseResult<Unit>) -> Unit
    )

    /**
     * Observe activities belonging to the
     * currently authenticated user.
     */
    fun getActivitiesFlow():
            Flow<List<Activity>>

    /**
     * Observe activities belonging to a
     * specific user.
     */
    fun getActivitiesFlowForUser(
        userId: String
    ): Flow<List<Activity>>

    /**
     * Get today's activity summary for the
     * currently authenticated user.
     */
    fun getTodaySummary(
        callback: (
            DatabaseResult<Map<String, Int>>
        ) -> Unit
    )

    /**
     * Get today's activity summary for a
     * specific user.
     */
    fun getTodaySummaryForUser(
        userId: String,
        callback: (
            DatabaseResult<Map<String, Int>>
        ) -> Unit
    )
}

/**
 * Platform-specific ActivityService.
 */
expect class ActivityService() :
    ActivityServiceInterface {

    override fun logActivity(
        activity: Activity,
        callback: (DatabaseResult<Unit>) -> Unit
    )

    override fun logActivityForUser(
        userId: String,
        activity: Activity,
        callback: (DatabaseResult<Unit>) -> Unit
    )

    override fun getActivitiesFlow():
            Flow<List<Activity>>

    override fun getActivitiesFlowForUser(
        userId: String
    ): Flow<List<Activity>>

    override fun getTodaySummary(
        callback: (
            DatabaseResult<Map<String, Int>>
        ) -> Unit
    )

    override fun getTodaySummaryForUser(
        userId: String,
        callback: (
            DatabaseResult<Map<String, Int>>
        ) -> Unit
    )
}