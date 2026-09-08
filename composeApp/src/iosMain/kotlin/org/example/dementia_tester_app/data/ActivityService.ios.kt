package org.example.dementia_tester_app.data

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseFirestoreBridge.FirebaseFirestoreBridge
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSDictionary
import platform.Foundation.NSError

/**
 * iOS implementation of ActivityService using the
 * FirebaseFirestoreBridge Objective-C bridge.
 *
 * Activities are stored under:
 *
 * UserProfiles/{userId}/activities/{activityId}
 *
 * Supports both the currently authenticated user and a selected
 * user/patient for caregiver and doctor functionality.
 */
@OptIn(ExperimentalForeignApi::class)
actual class ActivityService actual constructor() :
    ActivityServiceInterface {

    // ------------------------------------------------------------------
    // Firebase Auth helper
    // ------------------------------------------------------------------

    private fun currentUserId(): String? {
        return FIRAuth.auth()
            ?.currentUser()
            ?.uid()
    }

    // ------------------------------------------------------------------
    // logActivity
    // ------------------------------------------------------------------

    /**
     * Log an activity for the currently authenticated user.
     */
    actual override fun logActivity(
        activity: Activity,
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

        logActivityForUser(
            userId = userId,
            activity = activity,
            callback = callback
        )
    }

    // ------------------------------------------------------------------
    // logActivityForUser
    // ------------------------------------------------------------------

    /**
     * Log an activity for a specific user.
     */
    actual override fun logActivityForUser(
        userId: String,
        activity: Activity,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        if (userId.isBlank()) {
            callback(
                DatabaseResult.Error(
                    "User ID cannot be empty"
                )
            )
            return
        }

        val kotlinMap =
            activity.toMap()

        val objcMap =
            mutableMapOf<Any?, Any?>()

        kotlinMap.forEach { (key, value) ->
            objcMap[key] = value
        }

        FirebaseFirestoreBridge.addActivityForUser(
            userId = userId,
            data = objcMap,
            completion = {
                    error: NSError? ->

                if (error == null) {
                    callback(
                        DatabaseResult.Success(
                            Unit
                        )
                    )
                } else {
                    callback(
                        DatabaseResult.Error(
                            "Failed to log activity: " +
                                    error.localizedDescription
                        )
                    )
                }
            }
        )
    }

    // ------------------------------------------------------------------
    // getActivitiesFlow
    // ------------------------------------------------------------------

    /**
     * Get activities for the currently authenticated user.
     */
    actual override fun getActivitiesFlow():
            Flow<List<Activity>> {

        val userId =
            currentUserId()

        if (userId == null) {
            return callbackFlow {
                trySend(
                    emptyList()
                )

                close()

                awaitClose {
                }
            }
        }

        return getActivitiesFlowForUser(
            userId
        )
    }

    // ------------------------------------------------------------------
    // getActivitiesFlowForUser
    // ------------------------------------------------------------------

    /**
     * Get activities for a specific user.
     *
     * Firestore access is performed through the Objective-C bridge
     * because the Firestore Objective-C classes are not currently
     * exposed correctly through Kotlin/Native cinterop.
     */
    actual override fun getActivitiesFlowForUser(
        userId: String
    ): Flow<List<Activity>> =
        callbackFlow {

            if (userId.isBlank()) {
                trySend(
                    emptyList()
                )

                close()

                return@callbackFlow
            }

            FirebaseFirestoreBridge.getActivitiesForUser(
                userId = userId,
                completion = {
                        activities,
                        error ->

                    if (error != null) {
                        trySend(
                            emptyList()
                        )

                        close()

                        return@getActivitiesForUser
                    }

                    if (activities == null) {
                        trySend(
                            emptyList()
                        )

                        close()

                        return@getActivitiesForUser
                    }

                    try {
                        val result =
                            mutableListOf<Activity>()

                        val count =
                            activities.size

                        for (index in 0 until count) {

                            val rawActivity =
                                activities[index]

                            val dictionary =
                                rawActivity as? NSDictionary
                                    ?: continue

                            val data =
                                nsDictionaryToMap(
                                    dictionary
                                )

                            val documentId =
                                data["_documentId"]
                                    ?.toString()
                                    ?: ""

                            val activityData =
                                data.filterKeys { key ->
                                    key != "_documentId"
                                }

                            result.add(
                                Activity.fromMap(
                                    activityData,
                                    documentId
                                )
                            )
                        }

                        trySend(
                            result
                        )

                    } catch (_: Throwable) {
                        trySend(
                            emptyList()
                        )
                    }

                    close()
                }
            )

            awaitClose {
            }
        }

    // ------------------------------------------------------------------
    // getTodaySummary
    // ------------------------------------------------------------------

    /**
     * Get today's activity summary for the currently authenticated user.
     */
    actual override fun getTodaySummary(
        callback:
            (
            DatabaseResult<Map<String, Int>>
        ) -> Unit
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

        getTodaySummaryForUser(
            userId = userId,
            callback = callback
        )
    }

    // ------------------------------------------------------------------
    // getTodaySummaryForUser
    // ------------------------------------------------------------------

    /**
     * Get today's activity summary for a specific user.
     */
    actual override fun getTodaySummaryForUser(
        userId: String,
        callback:
            (
            DatabaseResult<Map<String, Int>>
        ) -> Unit
    ) {
        if (userId.isBlank()) {
            callback(
                DatabaseResult.Error(
                    "User ID cannot be empty"
                )
            )
            return
        }

        val now =
            Clock.System.now()

        val timeZone =
            TimeZone.currentSystemDefault()

        val today =
            now
                .toLocalDateTime(
                    timeZone
                )
                .date

        val startOfToday =
            LocalDateTime(
                year =
                    today.year,

                monthNumber =
                    today.monthNumber,

                dayOfMonth =
                    today.dayOfMonth,

                hour = 0,

                minute = 0
            )
                .toInstant(
                    timeZone
                )
                .toEpochMilliseconds()

        FirebaseFirestoreBridge.getActivitiesForUser(
            userId = userId,
            fromTimestamp = startOfToday,
            completion = {
                    activities,
                    error ->

                if (error != null) {
                    callback(
                        DatabaseResult.Error(
                            "Failed to get summary: " +
                                    error.localizedDescription
                        )
                    )

                    return@getActivitiesForUser
                }

                if (activities == null) {
                    callback(
                        DatabaseResult.Success(
                            emptyMap()
                        )
                    )

                    return@getActivitiesForUser
                }

                try {
                    val summary =
                        mutableMapOf<String, Int>()

                    val count =
                        activities.size

                    summary["total"] =
                        count

                    for (index in 0 until count) {

                        val rawActivity =
                            activities[index]

                        val dictionary =
                            rawActivity as? NSDictionary
                                ?: continue

                        val data =
                            nsDictionaryToMap(
                                dictionary
                            )

                        val type =
                            data["type"]
                                ?.toString()
                                ?: "other"

                        summary[type] =
                            (summary[type] ?: 0) + 1
                    }

                    callback(
                        DatabaseResult.Success(
                            summary
                        )
                    )

                } catch (t: Throwable) {
                    callback(
                        DatabaseResult.Error(
                            "Failed to parse activity summary: " +
                                    t.message
                        )
                    )
                }
            }
        )
    }

    // ------------------------------------------------------------------
    // Map conversion helper
    // ------------------------------------------------------------------

    private fun nsDictionaryToMap(
        dictionary: NSDictionary
    ): Map<String, Any> {

        val result =
            mutableMapOf<String, Any>()

        val keyEnumerator =
            dictionary.keyEnumerator()

        while (true) {

            val rawKey =
                keyEnumerator.nextObject()
                    ?: break

            val key =
                rawKey.toString()

            val rawValue =
                dictionary.objectForKey(
                    rawKey
                )
                    ?: continue

            result[key] =
                rawValue
        }

        return result
    }
}