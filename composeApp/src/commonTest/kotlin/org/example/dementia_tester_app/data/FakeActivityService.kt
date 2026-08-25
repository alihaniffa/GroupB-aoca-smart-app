package org.example.dementia_tester_app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FakeActivityService : ActivityServiceInterface {

    private val activities =
        mutableListOf<Activity>()

    private val activitiesFlow =
        MutableStateFlow<List<Activity>>(emptyList())

    var shouldSucceed = true

    var errorMessage =
        "Failed to log activity"

    override fun logActivity(
        activity: Activity,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        if (shouldSucceed) {

            activities.add(activity)

            activitiesFlow.value =
                activities.toList()

            callback(
                DatabaseResult.Success(Unit)
            )

        } else {

            callback(
                DatabaseResult.Error(
                    errorMessage
                )
            )
        }
    }

    override fun getActivitiesFlow():
        Flow<List<Activity>> =
        activitiesFlow.asStateFlow()

    override fun getTodaySummary(
        callback:
            (DatabaseResult<Map<String, Int>>) -> Unit
    ) {
        val timeZone =
            TimeZone.currentSystemDefault()

        val today =
            Clock.System.now()
                .toLocalDateTime(timeZone)
                .date

        val todaysActivities =
            activities.filter { activity ->

                activity.timestamp
                    .toLocalDateTime(timeZone)
                    .date == today
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

        callback(
            DatabaseResult.Success(summary)
        )
    }
}