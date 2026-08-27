package org.example.dementia_tester_app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

/**
 * Android implementation of AppointmentService using
 * Firebase Realtime Database.
 *
 * Appointments are stored under:
 *
 * Appointments/{userId}/{appointmentId}
 *
 * Normal operations use the currently authenticated
 * user's UID.
 *
 * Target-user operations allow an authorised caregiver
 * to work with an assigned patient's appointments.
 */
actual class AppointmentService actual constructor() {

    private val auth =
        FirebaseAuth.getInstance()

    private val database =
        FirebaseDatabase
            .getInstance()
            .reference

    /*
     * Main location for appointment records
     * in Realtime Database.
     */
    private val collectionPath =
        "Appointments"

    /**
     * Create a new appointment for the
     * currently signed-in user.
     */
    actual fun createAppointment(
        appointment: Appointment,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {

        val userId =
            auth.currentUser?.uid

        if (userId == null) {

            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )

            return
        }

        createAppointmentForUser(
            userId = userId,
            appointment = appointment,
            callback = callback
        )
    }

    /**
     * Create a new appointment for a
     * specific user.
     *
     * This allows an authorised caregiver
     * to create an appointment on behalf
     * of an assigned patient.
     */
    actual fun createAppointmentForUser(
        userId: String,
        appointment: Appointment,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {

        /*
         * A Firebase-authenticated account
         * is required for all operations.
         */
        if (auth.currentUser == null) {

            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )

            return
        }

        if (userId.isBlank()) {

            callback(
                DatabaseResult.Error(
                    "Invalid target user"
                )
            )

            return
        }

        /*
         * Ensure required appointment details
         * are provided.
         */
        if (
            appointment.date.isBlank() ||
            appointment.time.isBlank()
        ) {

            callback(
                DatabaseResult.Error(
                    "Appointment date and time are required"
                )
            )

            return
        }

        /*
         * Generate a unique appointment ID
         * under the target user's node.
         */
        val newAppointmentRef =
            database
                .child(collectionPath)
                .child(userId)
                .push()

        val appointmentId =
            newAppointmentRef.key

        /*
         * Stop if Firebase fails to generate
         * an appointment ID.
         */
        if (appointmentId == null) {

            callback(
                DatabaseResult.Error(
                    "Failed to generate appointment ID"
                )
            )

            return
        }

        /*
         * The appointment belongs to the
         * target user.
         *
         * In normal patient mode this is the
         * authenticated patient's UID.
         *
         * In caregiver mode this is the
         * selected patient's UID.
         */
        val newAppointment =
            appointment.copy(
                id = appointmentId,
                userId = userId
            )

        val appointmentData =
            newAppointment
                .toMap()
                .toMutableMap()

        /*
         * Firebase generates these timestamps
         * on the server.
         */
        appointmentData[
            "createdAt"
        ] =
            ServerValue.TIMESTAMP

        appointmentData[
            "updatedAt"
        ] =
            ServerValue.TIMESTAMP

        newAppointmentRef
            .setValue(
                appointmentData
            )
            .addOnSuccessListener {

                callback(
                    DatabaseResult.Success(
                        Unit
                    )
                )
            }
            .addOnFailureListener { e ->

                callback(
                    DatabaseResult.Error(
                        "Failed to book appointment: ${e.message}"
                    )
                )
            }
    }

    /**
     * Get all appointments belonging to
     * the currently signed-in user.
     */
    actual fun getAppointments(
        callback: (
            DatabaseResult<List<Appointment>>
        ) -> Unit
    ) {

        val userId =
            auth.currentUser?.uid

        if (userId == null) {

            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )

            return
        }

        getAppointmentsForUser(
            userId = userId,
            callback = callback
        )
    }

    /**
     * Get all appointments belonging to
     * a specific user.
     *
     * This allows an authorised caregiver
     * to retrieve the selected patient's
     * appointments.
     */
    actual fun getAppointmentsForUser(
        userId: String,
        callback: (
            DatabaseResult<List<Appointment>>
        ) -> Unit
    ) {

        if (auth.currentUser == null) {

            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )

            return
        }

        if (userId.isBlank()) {

            callback(
                DatabaseResult.Error(
                    "Invalid target user"
                )
            )

            return
        }

        database
            .child(collectionPath)
            .child(userId)
            .addListenerForSingleValueEvent(
                object :
                    ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        try {

                            val appointments =
                                mutableListOf<
                                        Appointment
                                        >()

                            for (
                            child in snapshot.children
                            ) {

                                val data =
                                    child.value
                                            as? Map<*, *>
                                        ?: continue

                                val appointmentId =
                                    child.key
                                        ?: continue

                                appointments.add(
                                    Appointment.fromMap(
                                        data,
                                        appointmentId
                                    )
                                )
                            }

                            callback(
                                DatabaseResult.Success(
                                    appointments
                                )
                            )

                        } catch (
                            e: Exception
                        ) {

                            callback(
                                DatabaseResult.Error(
                                    "Failed to parse appointments: ${e.message}"
                                )
                            )
                        }
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        callback(
                            DatabaseResult.Error(
                                "Failed to load appointments: ${error.message}"
                            )
                        )
                    }
                }
            )
    }

    /**
     * Update the status of an appointment
     * belonging to the currently signed-in user.
     */
    actual fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: AppointmentStatus,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {

        val userId =
            auth.currentUser?.uid

        if (userId == null) {

            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )

            return
        }

        updateAppointmentStatusForUser(
            userId = userId,
            appointmentId = appointmentId,
            newStatus = newStatus,
            callback = callback
        )
    }

    /**
     * Update the status of an appointment
     * belonging to a specific user.
     *
     * This allows an authorised caregiver
     * to update the selected patient's
     * appointment.
     */
    actual fun updateAppointmentStatusForUser(
        userId: String,
        appointmentId: String,
        newStatus: AppointmentStatus,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {

        if (auth.currentUser == null) {

            callback(
                DatabaseResult.Error(
                    "No user is signed in"
                )
            )

            return
        }

        if (userId.isBlank()) {

            callback(
                DatabaseResult.Error(
                    "Invalid target user"
                )
            )

            return
        }

        /*
         * Ensure a valid appointment ID
         * was provided.
         */
        if (appointmentId.isBlank()) {

            callback(
                DatabaseResult.Error(
                    "Invalid appointment ID"
                )
            )

            return
        }

        val appointmentRef =
            database
                .child(collectionPath)
                .child(userId)
                .child(appointmentId)

        val updates =
            mapOf<String, Any>(
                "status" to
                        newStatus.name,

                "updatedAt" to
                        ServerValue.TIMESTAMP
            )

        appointmentRef
            .updateChildren(
                updates
            )
            .addOnSuccessListener {

                callback(
                    DatabaseResult.Success(
                        Unit
                    )
                )
            }
            .addOnFailureListener { e ->

                callback(
                    DatabaseResult.Error(
                        "Failed to update status: ${e.message}"
                    )
                )
            }
    }
}