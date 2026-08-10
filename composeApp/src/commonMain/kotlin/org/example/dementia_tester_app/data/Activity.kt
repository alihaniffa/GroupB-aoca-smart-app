package org.example.dementia_tester_app.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Types of activities that can be recorded for a user.
 */
enum class ActivityType(val value: String) {
    GAME("game"),
    TEST("test"),
    REMINDER("reminder"),
    APPOINTMENT("appointment"),
    OTHER("other");

    companion object {
        fun fromString(value: String): ActivityType {
            return entries.find {
                it.value.equals(value, ignoreCase = true)
            } ?: OTHER
        }
    }
}

/**
 * Data model for a user's activity history.
 *
 * Activities are stored under:
 * Activities/{userId}/{activityId}
 */
data class Activity(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val type: ActivityType = ActivityType.OTHER,
    val description: String = "",
    val timestamp: Instant = Clock.System.now()
) {

    /**
     * Convert an Activity to a map for Firebase Realtime Database.
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "title" to title,
            "type" to type.value,
            "description" to description,
            "timestamp" to timestamp.toEpochMilliseconds()
        )
    }

    companion object {

        /**
         * Create an Activity from Firebase Realtime Database data.
         */
        fun fromMap(
            map: Map<String, Any?>,
            id: String
        ): Activity {

            fun getString(key: String): String {
                return map[key]?.toString() ?: ""
            }

            val timestampRaw = map["timestamp"]

            val timestamp = when (timestampRaw) {
                is Number ->
                    Instant.fromEpochMilliseconds(timestampRaw.toLong())

                is String ->
                    timestampRaw.toLongOrNull()
                        ?.let { Instant.fromEpochMilliseconds(it) }
                        ?: Clock.System.now()

                else ->
                    Clock.System.now()
            }

            return Activity(
                id = id,
                userId = getString("userId"),
                title = getString("title"),
                type = ActivityType.fromString(
                    getString("type")
                ),
                description = getString("description"),
                timestamp = timestamp
            )
        }
    }
}

// previous code
/*
package org.example.dementia_tester_app.data


import kotlinx.datetime.Instant
import kotlinx.datetime.Clock

/**
 * Data model for user activities
 */
data class Activity(
    val id: String = "",
    val title: String = "",
    val type: String = "", // "game", "test", "reminder", "appointment"
    val description: String = "",
    val timestamp: Instant = Clock.System.now()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "title" to title,
            "type" to type,
            "description" to description,
            "timestamp" to timestamp.toEpochMilliseconds()
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>, id: String): Activity {
            val title = map["title"] as? String ?: ""
            val type = map["type"] as? String ?: ""
            val description = map["description"] as? String ?: ""
            
            val timestampRaw = map["timestamp"]
            val timestamp = when (timestampRaw) {
                is Long -> Instant.fromEpochMilliseconds(timestampRaw)
                is Number -> Instant.fromEpochMilliseconds(timestampRaw.toLong())
                is String -> timestampRaw.toLongOrNull()?.let { Instant.fromEpochMilliseconds(it) } ?: Clock.System.now()
                else -> Clock.System.now()
            }

            return Activity(
                id = id,
                title = title,
                type = type,
                description = description,
                timestamp = timestamp
            )
        }
    }
}
*/