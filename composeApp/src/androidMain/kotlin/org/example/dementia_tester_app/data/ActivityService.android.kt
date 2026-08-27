package org.example.dementia_tester_app.data

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Android implementation of ActivityService
 * using Firebase Realtime Database.
 *
 * Activities are stored under:
 *
 * Activities/{userId}/{activityId}
 *
 * Normal operations use the authenticated
 * user's UID.
 *
 * Target-user operations allow an authorised
 * caregiver to work with an assigned
 * patient's activities.
 */
actual class ActivityService actual constructor() :
    ActivityServiceInterface {

    private val auth =
        FirebaseAuth.getInstance()

    private val database =
        Firebase.database.reference

    private val dbPath =
        "Activities"

    private val tag =
        "ActivityService"

    /**
     * Return the Activities reference for a
     * specific user.
     *
     * Firebase security rules determine whether
     * the authenticated account has permission
     * to access the requested user's data.
     */
    private fun getActivitiesRefForUser(
        userId: String
    ): DatabaseReference? {

        if (auth.currentUser == null) {
            return null
        }

        if (userId.isBlank()) {
            return null
        }

        return database
            .child(dbPath)
            .child(userId)
    }

    /**
     * Return the Activities reference for the
     * currently authenticated user.
     */
    private fun getCurrentUserActivitiesRef():
            DatabaseReference? {

        val userId =
            auth.currentUser?.uid
                ?: return null

        return getActivitiesRefForUser(
            userId
        )
    }

    /**
     * Log an activity for the currently
     * authenticated user.
     */
    override actual fun logActivity(
        activity: Activity,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {

        val currentUserId =
            auth.currentUser?.uid

        if (currentUserId == null) {

            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )

            return
        }

        logActivityForUser(
            userId = currentUserId,
            activity = activity,
            callback = callback
        )
    }

    /**
     * Log an activity for a specific user.
     *
     * This is used when a caregiver performs
     * an activity on behalf of an assigned
     * patient.
     */
    override actual fun logActivityForUser(
        userId: String,
        activity: Activity,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {

        val userActivitiesRef =
            getActivitiesRefForUser(
                userId
            )

        if (userActivitiesRef == null) {

            callback(
                DatabaseResult.Error(
                    "No user is signed in or target user is invalid"
                )
            )

            return
        }

        val newActivityRef =
            userActivitiesRef.push()

        val activityId =
            newActivityRef.key

        if (activityId == null) {

            callback(
                DatabaseResult.Error(
                    "Failed to generate activity ID"
                )
            )

            return
        }

        /*
         * Important:
         *
         * userId is the owner of the activity.
         * In caregiver mode this is the selected
         * patient's UID, not the caregiver UID.
         */
        val activityWithId =
            activity.copy(
                id = activityId,
                userId = userId
            )

        newActivityRef
            .setValue(
                activityWithId.toMap()
            )
            .addOnSuccessListener {

                Log.d(
                    tag,
                    "Activity logged for $userId: ${activityWithId.title}"
                )

                callback(
                    DatabaseResult.Success(
                        Unit
                    )
                )
            }
            .addOnFailureListener { e ->

                Log.e(
                    tag,
                    "Failed to log activity for $userId",
                    e
                )

                callback(
                    DatabaseResult.Error(
                        "Failed to log activity: ${e.message}"
                    )
                )
            }
    }

    /**
     * Observe activities belonging to the
     * currently authenticated user.
     */
    override actual fun getActivitiesFlow():
            Flow<List<Activity>> {

        val currentUserId =
            auth.currentUser?.uid

        if (currentUserId == null) {

            return callbackFlow {
                trySend(emptyList())
                close()
            }
        }

        return getActivitiesFlowForUser(
            currentUserId
        )
    }

    /**
     * Observe activities belonging to a
     * specific user.
     *
     * Used by the caregiver when viewing
     * an assigned patient's activity history.
     */
    override actual fun getActivitiesFlowForUser(
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

            val listener =
                object :
                    ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val activities =
                            snapshot.children
                                .mapNotNull { child ->

                                    @Suppress(
                                        "UNCHECKED_CAST"
                                    )
                                    val data =
                                        child.value
                                                as? Map<String, Any?>
                                            ?: return@mapNotNull null

                                    val activityId =
                                        child.key
                                            ?: return@mapNotNull null

                                    Activity.fromMap(
                                        data,
                                        activityId
                                    )
                                }
                                .sortedByDescending {

                                    it.timestamp
                                        .toEpochMilliseconds()
                                }

                        trySend(
                            activities
                        )
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        Log.e(
                            tag,
                            "Activity listener cancelled for $userId: ${error.message}"
                        )

                        close(
                            error.toException()
                        )
                    }
                }

            ref.addValueEventListener(
                listener
            )

            awaitClose {

                ref.removeEventListener(
                    listener
                )
            }
        }

    /**
     * Get today's activity summary for the
     * currently authenticated user.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override actual fun getTodaySummary(
        callback: (
            DatabaseResult<Map<String, Int>>
        ) -> Unit
    ) {

        val currentUserId =
            auth.currentUser?.uid

        if (currentUserId == null) {

            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )

            return
        }

        getTodaySummaryForUser(
            userId = currentUserId,
            callback = callback
        )
    }

    /**
     * Get today's activity summary for a
     * specific user.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override actual fun getTodaySummaryForUser(
        userId: String,
        callback: (
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
            kotlinx.datetime.LocalDateTime(
                year =
                    today.year,
                monthNumber =
                    today.monthNumber,
                dayOfMonth =
                    today.dayOfMonth,
                hour =
                    0,
                minute =
                    0
            )
                .toInstant(
                    timeZone
                )
                .toEpochMilliseconds()

        ref.get()
            .addOnSuccessListener { snapshot ->

                val summary =
                    mutableMapOf<String, Int>()

                var total =
                    0

                snapshot.children
                    .forEach { child ->

                        val timestamp =
                            when (
                                val value =
                                    child
                                        .child(
                                            "timestamp"
                                        )
                                        .value
                            ) {

                                is Number ->
                                    value.toLong()

                                is String ->
                                    value
                                        .toLongOrNull()
                                        ?: 0L

                                else ->
                                    0L
                            }

                        if (
                            timestamp >=
                            startOfToday
                        ) {

                            total++

                            val type =
                                child
                                    .child(
                                        "type"
                                    )
                                    .value
                                    ?.toString()
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: ActivityType
                                        .OTHER
                                        .value

                            summary[type] =
                                summary
                                    .getOrDefault(
                                        type,
                                        0
                                    ) + 1
                        }
                    }

                summary["total"] =
                    total

                callback(
                    DatabaseResult.Success(
                        summary
                    )
                )
            }
            .addOnFailureListener { e ->

                callback(
                    DatabaseResult.Error(
                        "Failed to get summary: ${e.message}"
                    )
                )
            }
    }
}