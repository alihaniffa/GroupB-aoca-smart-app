package org.example.dementia_tester_app.data

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseDatabase.FIRDataSnapshot
import cocoapods.FirebaseDatabase.FIRDatabase
import cocoapods.FirebaseDatabase.FIRDatabaseReference
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

/**
 * iOS implementation of ActivityService
 * using Firebase Realtime Database.
 *
 * Activities are stored under:
 *
 * Activities/{userId}/{activityId}
 *
 * This matches the Android implementation.
 */
@OptIn(ExperimentalForeignApi::class)
actual class ActivityService actual constructor() :
    ActivityServiceInterface {

    private val dbPath =
        "Activities"

    private fun currentUserId(): String? {
        return FIRAuth.auth()
            ?.currentUser()
            ?.uid()
    }

    private fun getActivitiesRefForUser(
        userId: String
    ): FIRDatabaseReference? {

        if (currentUserId() == null) {
            return null
        }

        if (userId.isBlank()) {
            return null
        }

        val database =
            FIRDatabase.database()
                ?: return null

        return database
            .reference()
            .child(dbPath)
            .child(userId)
    }

    /**
     * Log an activity for the currently
     * authenticated user.
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

    /**
     * Log an activity for a specific user.
     *
     * In caregiver mode, userId is the
     * selected patient's UID.
     */
    actual override fun logActivityForUser(
        userId: String,
        activity: Activity,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        val activitiesRef =
            getActivitiesRefForUser(
                userId
            )

        if (activitiesRef == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in or target user is invalid"
                )
            )
            return
        }

        val newActivityRef =
            activitiesRef.childByAutoId()

        val activityId =
            newActivityRef.key()
                ?: run {
                    callback(
                        DatabaseResult.Error(
                            "Failed to generate activity ID"
                        )
                    )
                    return
                }

        val activityWithId =
            activity.copy(
                id = activityId,
                userId = userId
            )

        val data =
            mutableMapOf<Any?, Any?>()

        activityWithId
            .toMap()
            .forEach { (key, value) ->
                data[key] = value
            }

        newActivityRef.setValue(
            value = data,
            withCompletionBlock = { error, _ ->

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

    /**
     * Get activities for the currently
     * authenticated user.
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

    /**
     * Get activities for a specific user.
     */
    actual override fun getActivitiesFlowForUser(
        userId: String
    ): Flow<List<Activity>> =
        callbackFlow {

            val ref =
                getActivitiesRefForUser(
                    userId
                )

            if (ref == null) {
                trySend(
                    emptyList()
                )
                close()

                return@callbackFlow
            }

            FirebaseDatabaseIosHelper.observeValueOnce(
                query = ref,
                callback = { snapshot ->

                    if (snapshot == null) {
                        trySend(
                            emptyList()
                        )
                        close()

                        return@observeValueOnce
                    }

                    try {
                        val activities =
                            parseActivities(
                                snapshot
                            )
                                .sortedByDescending {
                                    it.timestamp
                                        .toEpochMilliseconds()
                                }

                        trySend(
                            activities
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

    /**
     * Get today's activity summary for the
     * currently authenticated user.
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

    /**
     * Get today's activity summary for a
     * specific user.
     */
    actual override fun getTodaySummaryForUser(
        userId: String,
        callback:
            (
            DatabaseResult<Map<String, Int>>
        ) -> Unit
    ) {
        val ref =
            getActivitiesRefForUser(
                userId
            )

        if (ref == null) {
            callback(
                DatabaseResult.Error(
                    "No user is signed in or target user is invalid"
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
                year = today.year,
                monthNumber = today.monthNumber,
                dayOfMonth = today.dayOfMonth,
                hour = 0,
                minute = 0
            )
                .toInstant(
                    timeZone
                )
                .toEpochMilliseconds()

        FirebaseDatabaseIosHelper.observeValueOnce(
            query = ref,
            callback = { snapshot ->

                if (snapshot == null) {
                    callback(
                        DatabaseResult.Error(
                            "Failed to get activity summary"
                        )
                    )

                    return@observeValueOnce
                }

                try {
                    val activities =
                        parseActivities(
                            snapshot
                        )

                    val summary =
                        mutableMapOf<String, Int>()

                    var total =
                        0

                    activities.forEach { activity ->

                        if (
                            activity.timestamp
                                .toEpochMilliseconds() >=
                            startOfToday
                        ) {
                            total++

                            val type =
                                activity.type.value

                            summary[type] =
                                (summary[type] ?: 0) + 1
                        }
                    }

                    summary["total"] =
                        total

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

    /**
     * Convert an Activities snapshot into
     * Activity objects.
     */
    private fun parseActivities(
        snapshot: FIRDataSnapshot
    ): List<Activity> {

        val rootValue =
            snapshot.value()
                    as? NSDictionary
                ?: return emptyList()

        val result =
            mutableListOf<Activity>()

        val keyEnumerator =
            rootValue.keyEnumerator()

        while (true) {
            val rawKey =
                keyEnumerator.nextObject()
                    ?: break

            val activityId =
                rawKey.toString()

            val rawActivity =
                rootValue.objectForKey(
                    rawKey
                ) as? NSDictionary
                    ?: continue

            val data =
                nsDictionaryToMap(
                    rawActivity
                )

            result.add(
                Activity.fromMap(
                    data,
                    activityId
                )
            )
        }

        return result
    }

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

            val rawValue =
                dictionary.objectForKey(
                    rawKey
                )
                    ?: continue

            result[
                rawKey.toString()
            ] = rawValue
        }

        return result
    }
}