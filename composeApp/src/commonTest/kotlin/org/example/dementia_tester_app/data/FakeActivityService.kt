package org.example.dementia_tester_app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FakeActivityService : ActivityServiceInterface {

    /*
     * Stores all fake activities.
     */
    private val activities =
        mutableListOf<Activity>()

    /*
     * Real-time activity flow used by tests.
     */
    private val activitiesFlow =
        MutableStateFlow<List<Activity>>(
            emptyList()
        )

    var shouldSucceed =
        true

    var errorMessage =
        "Failed to log activity"

    /**
     * Log activity for the current fake user.
     */
    override fun logActivity(
        activity: Activity,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {

        if (shouldSucceed) {

            activities.add(
                activity
            )

            activitiesFlow.value =
                activities.toList()

            callback(
                DatabaseResult.Success(
                    Unit
                )
            )

        } else {

            callback(
                DatabaseResult.Error(
                    errorMessage
                )
            )
        }
    }

    /**
     * Log activity for a specific user.
     *
     * This mirrors the new caregiver-aware
     * ActivityService method.
     */
    override fun logActivityForUser(
        userId: String,
        activity: Activity,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {

        if (shouldSucceed) {

            val activityForUser =
                activity.copy(
                    userId = userId
                )

            activities.add(
                activityForUser
            )

            activitiesFlow.value =
                activities.toList()

            callback(
                DatabaseResult.Success(
                    Unit
                )
            )

        } else {

            callback(
                DatabaseResult.Error(
                    errorMessage
                )
            )
        }
    }

    /**
     * Return all fake activities.
     */
    override fun getActivitiesFlow():
            Flow<List<Activity>> =

        activitiesFlow
            .asStateFlow()

    /**
     * Return activities for a specific user.
     */
    override fun getActivitiesFlowForUser(
        userId: String
    ): Flow<List<Activity>> {

        /*
         * This fake keeps one shared flow.
         *
         * For unit-test compatibility, filter
         * activities by userId when possible.
         */
        val filtered =
            activities.filter {
                it.userId == userId
            }

        return MutableStateFlow(
            filtered
        ).asStateFlow()
    }

    /**
     * Get today's summary for all activities
     * in the fake service.
     */
    override fun getTodaySummary(
        callback:
            (
            DatabaseResult<
                    Map<String, Int>
                    >
        ) -> Unit
    ) {

        val summary =
            createTodaySummary(
                activities
            )

        callback(
            DatabaseResult.Success(
                summary
            )
        )
    }

    /**
     * Get today's summary for a specific user.
     */
    override fun getTodaySummaryForUser(
        userId: String,
        callback:
            (
            DatabaseResult<
                    Map<String, Int>
                    >
        ) -> Unit
    ) {

        val userActivities =
            activities.filter {
                it.userId == userId
            }

        val summary =
            createTodaySummary(
                userActivities
            )

        callback(
            DatabaseResult.Success(
                summary
            )
        )
    }

    /**
     * Build today's activity summary.
     */
    private fun createTodaySummary(
        sourceActivities: List<Activity>
    ): Map<String, Int> {

        val timeZone =
            TimeZone.currentSystemDefault()

        val today =
            Clock.System.now()
                .toLocalDateTime(
                    timeZone
                )
                .date

        val todaysActivities =
            sourceActivities
                .filter { activity ->

                    activity.timestamp
                        .toLocalDateTime(
                            timeZone
                        )
                        .date ==
                            today
                }

        val summary =
            todaysActivities
                .groupingBy {
                    it.type.value
                }
                .eachCount()
                .toMutableMap()

        summary["total"] =
            todaysActivities.size

        return summary
    }
}