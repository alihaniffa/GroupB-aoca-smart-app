package org.example.dementia_tester_app.data

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
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
 * Android implementation of ActivityService using Firebase Realtime Database.
 *
 * Activities are stored under:
 * Activities/{userId}/{activityId}
 */
actual class ActivityService actual constructor() : ActivityServiceInterface {

    private val auth = FirebaseAuth.getInstance()
    private val database = Firebase.database.reference
    private val dbPath = "Activities"
    private val tag = "ActivityService"

    private fun getUserActivitiesRef() =
        auth.currentUser?.uid?.let { userId ->
            database.child(dbPath).child(userId)
        }

    /**
     * Log a new activity for the currently signed-in user.
     */
    override actual fun logActivity(
        activity: Activity,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        val userId = currentUser.uid
        val userActivitiesRef =
            database.child(dbPath).child(userId)

        val newActivityRef = userActivitiesRef.push()
        val activityId = newActivityRef.key

        if (activityId == null) {
            callback(
                DatabaseResult.Error(
                    "Failed to generate activity ID"
                )
            )
            return
        }

        val activityWithId = activity.copy(
            id = activityId,
            userId = userId
        )

        newActivityRef
            .setValue(activityWithId.toMap())
            .addOnSuccessListener {
                Log.d(
                    tag,
                    "Activity logged: ${activityWithId.title}"
                )

                callback(DatabaseResult.Success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to log activity", e)

                callback(
                    DatabaseResult.Error(
                        "Failed to log activity: ${e.message}"
                    )
                )
            }
    }

    /**
     * Observe all activities for the currently signed-in user.
     */
    override actual fun getActivitiesFlow():
            Flow<List<Activity>> = callbackFlow {

        val ref = getUserActivitiesRef()

        if (ref == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val activities =
                    snapshot.children.mapNotNull { child ->

                        @Suppress("UNCHECKED_CAST")
                        val data =
                            child.value as? Map<String, Any?>
                                ?: return@mapNotNull null

                        val activityId =
                            child.key ?: return@mapNotNull null

                        Activity.fromMap(
                            data,
                            activityId
                        )
                    }
                        .sortedByDescending {
                            it.timestamp.toEpochMilliseconds()
                        }

                trySend(activities)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    tag,
                    "Activity listener cancelled: ${error.message}"
                )

                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    /**
     * Get a summary of today's activities grouped by activity type.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override actual fun getTodaySummary(
        callback: (DatabaseResult<Map<String, Int>>) -> Unit
    ) {
        val ref = getUserActivitiesRef()

        if (ref == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(timeZone).date

        val startOfToday =
            kotlinx.datetime.LocalDateTime(
                year = today.year,
                monthNumber = today.monthNumber,
                dayOfMonth = today.dayOfMonth,
                hour = 0,
                minute = 0
            )
                .toInstant(timeZone)
                .toEpochMilliseconds()

        ref.get()
            .addOnSuccessListener { snapshot ->

                val summary =
                    mutableMapOf<String, Int>()

                var total = 0

                snapshot.children.forEach { child ->

                    val timestamp =
                        when (val value =
                            child.child("timestamp").value) {

                            is Number ->
                                value.toLong()

                            is String ->
                                value.toLongOrNull() ?: 0L

                            else ->
                                0L
                        }

                    if (timestamp >= startOfToday) {

                        total++

                        val type =
                            child.child("type")
                                .value
                                ?.toString()
                                ?.takeIf { it.isNotBlank() }
                                ?: ActivityType.OTHER.value

                        summary[type] =
                            summary.getOrDefault(type, 0) + 1
                    }
                }

                summary["total"] = total

                callback(
                    DatabaseResult.Success(summary)
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


// previous code
/*
package org.example.dementia_tester_app.data

import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant

/**
 * Android implementation of ActivityService using Firebase Realtime Database.
 * Migrated from Firestore for consistency and to fix permission issues.
 */
actual class ActivityService actual constructor() : ActivityServiceInterface {
    private val auth = FirebaseAuth.getInstance()
    private val database = Firebase.database.reference
    private val dbPath = "Activities"
    private val tag = "ActivityService"

    private fun getUserActivitiesRef() = 
        auth.currentUser?.uid?.let { userId ->
            database.child(dbPath).child(userId)
        }

    override actual fun logActivity(activity: Activity, callback: (DatabaseResult<Unit>) -> Unit) {
        val ref = getUserActivitiesRef()
        if (ref == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        val newActivityRef = ref.push()
        val id = newActivityRef.key ?: ""
        val activityWithId = activity.copy(id = id)

        newActivityRef.setValue(activityWithId.toMap())
            .addOnSuccessListener {
                Log.d(tag, "Activity logged: ${activity.title}")
                callback(DatabaseResult.Success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to log activity", e)
                callback(DatabaseResult.Error("Failed to log activity: ${e.message}"))
            }
    }

    override actual fun getActivitiesFlow(): Flow<List<Activity>> = callbackFlow {
        val ref = getUserActivitiesRef()
        
        if (ref == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activities = snapshot.children.mapNotNull { child ->
                    val data = child.value as? Map<String, Any?> ?: return@mapNotNull null
                    Activity.fromMap(data, child.key ?: "")
                }.sortedByDescending { it.timestamp.toEpochMilliseconds() }
                trySend(activities)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Listen failed: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)

        awaitClose { 
            ref.removeEventListener(listener) 
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override actual fun getTodaySummary(callback: (DatabaseResult<Map<String, Int>>) -> Unit) {
        val ref = getUserActivitiesRef()
        if (ref == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(tz).date
        
        val startOfToday = kotlinx.datetime.LocalDateTime(today.year, today.monthNumber, today.dayOfMonth, 0, 0)
            .toInstant(tz).toEpochMilliseconds()

        ref.get().addOnSuccessListener { snapshot ->
            val summary = mutableMapOf<String, Int>()
            var total = 0
            
            snapshot.children.forEach { child ->
                val timestamp = child.child("timestamp").value?.toString()?.toLongOrNull() ?: 0L
                if (timestamp >= startOfToday) {
                    total++
                    val type = child.child("type").value?.toString() ?: "other"
                    summary[type] = summary.getOrDefault(type, 0) + 1
                }
            }
            summary["total"] = total
            callback(DatabaseResult.Success(summary))
        }.addOnFailureListener { e ->
            callback(DatabaseResult.Error("Failed to get summary: ${e.message}"))
        }
    }
}
*/