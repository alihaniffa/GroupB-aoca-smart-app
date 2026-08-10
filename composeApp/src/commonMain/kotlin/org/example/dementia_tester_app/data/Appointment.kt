package org.example.dementia_tester_app.data

/**
 * Represents the current state of an appointment.
 */
enum class AppointmentStatus {
    Upcoming,
    Completed,
    Cancelled;

    companion object {
        /**
         * Convert a stored string value into an AppointmentStatus.
         * Defaults to Upcoming when the value is missing or invalid.
         */
        fun fromString(value: String): AppointmentStatus {
            return entries.find {
                it.name.equals(value, ignoreCase = true)
            } ?: Upcoming
        }
    }
}

/**
 * Represents an appointment stored in Firebase Realtime Database.
 *
 * Appointments are stored under:
 * Appointments/{userId}/{appointmentId}
 */
data class Appointment(

    // Appointment identifiers
    val id: String = "",
    val userId: String = "",

    // Doctor details
    val doctorId: String = "",
    val doctor: String = "",
    val doctorEmail: String = "",

    // Appointment details
    val type: String = "",
    val date: String = "",
    val time: String = "",
    val status: AppointmentStatus = AppointmentStatus.Upcoming,
    val reason: String = "",

    // Patient details
    val patientName: String = "",
    val patientEmail: String = "",

    // Audit timestamps
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {

    /**
     * Convert the Appointment into a map for Firebase Realtime Database.
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,

            "doctorId" to doctorId,
            "doctor" to doctor,
            "doctorEmail" to doctorEmail,

            "type" to type,
            "date" to date,
            "time" to time,
            "status" to status.name,
            "reason" to reason,

            "patientName" to patientName,
            "patientEmail" to patientEmail,

            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {

        /**
         * Create an Appointment from Firebase Realtime Database data.
         */
        fun fromMap(
            map: Map<*, *>,
            id: String
        ): Appointment {

            fun getString(key: String): String {
                return map[key]?.toString() ?: ""
            }

            fun getLong(key: String): Long {
                return when (val value = map[key]) {
                    is Number -> value.toLong()
                    is String -> value.toLongOrNull() ?: 0L
                    else -> 0L
                }
            }

            return Appointment(
                id = id,
                userId = getString("userId"),

                doctorId = getString("doctorId"),
                doctor = getString("doctor"),
                doctorEmail = getString("doctorEmail"),

                type = getString("type"),
                date = getString("date"),
                time = getString("time"),
                status = AppointmentStatus.fromString(
                    getString("status")
                ),
                reason = getString("reason"),

                patientName = getString("patientName"),
                patientEmail = getString("patientEmail"),

                createdAt = getLong("createdAt"),
                updatedAt = getLong("updatedAt")
            )
        }
    }
}
// previous code
/*
package org.example.dementia_tester_app.data

enum class AppointmentStatus {
    Upcoming,
    Completed,
    Cancelled;

    companion object {
        fun fromString(value: String): AppointmentStatus =
            entries.find { it.name.lowercase() == value.lowercase() } ?: Upcoming
    }
}

data class Appointment(
    val id: String = "",
    val userId: String = "",
    val doctor: String = "",
    val type: String = "",
    val date: String = "",
    val time: String = "",
    val status: AppointmentStatus = AppointmentStatus.Upcoming,
    val reason: String = "",
    val patientName: String = "",
    val patientEmail: String = "",
    val doctorEmail: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id"           to id,
        "userId"       to userId,
        "doctor"       to doctor,
        "type"         to type,
        "date"         to date,
        "time"         to time,
        "status"       to status.name,
        "reason"       to reason,
        "patientName"  to patientName,
        "patientEmail" to patientEmail,
        "doctorEmail"  to doctorEmail
    )

    companion object {
        fun fromMap(map: Map<*, *>, id: String): Appointment {
            fun str(key: String) = (map[key] as? String) ?: ""
            return Appointment(
                id           = id,
                userId       = str("userId"),
                doctor       = str("doctor"),
                type         = str("type"),
                date         = str("date"),
                time         = str("time"),
                status       = AppointmentStatus.fromString(str("status")),
                reason       = str("reason"),
                patientName  = str("patientName"),
                patientEmail = str("patientEmail"),
                doctorEmail  = str("doctorEmail")
            )
        }
    }
}
*/