package org.example.dementia_tester_app.data

import kotlinx.coroutines.flow.Flow

/**
 * Interface for activity tracking service
 */
interface ActivityServiceInterface {
    fun logActivity(activity: Activity, callback: (DatabaseResult<Unit>) -> Unit)
    fun getActivitiesFlow(): Flow<List<Activity>>
    fun getTodaySummary(callback: (DatabaseResult<Map<String, Int>>) -> Unit)
}

expect class ActivityService() : ActivityServiceInterface {
    /**
     * Log a new activity
     * @param activity The activity to log
     */
    override fun logActivity(activity: Activity, callback: (DatabaseResult<Unit>) -> Unit)

    /**
     * Get all activities for the current user
     * @return A flow of activity lists that updates in real-time
     */
    override fun getActivitiesFlow(): Flow<List<Activity>>

    /**
     * Get a summary of activities for today
     * @param callback Callback with the summary map (type to count)
     */
    override fun getTodaySummary(callback: (DatabaseResult<Map<String, Int>>) -> Unit)
}
