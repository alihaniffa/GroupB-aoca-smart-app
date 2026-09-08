package org.example.dementia_tester_app.data

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseDatabase.FIRDataSnapshot
import cocoapods.FirebaseDatabase.FIRDatabase
import platform.Foundation.NSDictionary
import platform.Foundation.NSNull

/**
 * iOS implementation for appointments stored in Firebase Realtime Database.
 *
 * Structure:
 *
 * Appointments/
 *     {userId}/
 *         {appointmentId}/
 *
 * This supports both:
 * - operations for the currently signed-in user
 * - operations for a selected patient/user, used by caregiver/doctor flows
 */
actual class AppointmentService actual constructor() {

    private val collectionPath = "Appointments"

    /**
     * Returns the currently signed-in Firebase user's UID.
     */
    private fun currentUserId(): String? {
        return FIRAuth.auth()
            ?.currentUser()
            ?.uid()
    }

    /**
     * Returns the Firebase Realtime Database root reference.
     */
    private fun rootRef() =
        FIRDatabase.database()
            ?.reference()

    // ------------------------------------------------------------------
    // createAppointment
    // ------------------------------------------------------------------

    actual fun createAppointment(
        appointment: Appointment,
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

        createAppointmentForUser(
            userId = userId,
            appointment = appointment,
            callback = callback
        )
    }

    // ------------------------------------------------------------------
    // createAppointmentForUser
    // ------------------------------------------------------------------

    actual fun createAppointmentForUser(
        userId: String,
        appointment: Appointment,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        if (userId.isBlank()) {
            callback(
                DatabaseResult.Error(
                    "Invalid user ID"
                )
            )
            return
        }

        val ref =
            rootRef()
                ?.child(collectionPath)
                ?.child(userId)

        if (ref == null) {
            callback(
                DatabaseResult.Error(
                    "Firebase not initialized"
                )
            )
            return
        }

        val newAppointmentRef =
            ref.childByAutoId()

        val appointmentId =
            newAppointmentRef.key()

        if (appointmentId.isNullOrBlank()) {
            callback(
                DatabaseResult.Error(
                    "Failed to generate appointment ID"
                )
            )
            return
        }

        val appointmentToSave =
            appointment.copy(
                id = appointmentId,
                userId = userId
            )

        val objcMap:
                Map<Any?, Any?> =
            appointmentToSave
                .toMap()
                .entries
                .associate {
                        (key, value) ->

                    (key as Any?) to
                            (value ?: NSNull())
                }

        newAppointmentRef.setValue(
            objcMap
        ) {
                error,
                _ ->

            if (error == null) {
                callback(
                    DatabaseResult.Success(
                        Unit
                    )
                )
            } else {
                callback(
                    DatabaseResult.Error(
                        "Failed to book appointment: ${error.localizedDescription}"
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // getAppointments
    // ------------------------------------------------------------------

    actual fun getAppointments(
        callback: (DatabaseResult<List<Appointment>>) -> Unit
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

        getAppointmentsForUser(
            userId = userId,
            callback = callback
        )
    }

    // ------------------------------------------------------------------
    // getAppointmentsForUser
    // ------------------------------------------------------------------

    actual fun getAppointmentsForUser(
        userId: String,
        callback: (DatabaseResult<List<Appointment>>) -> Unit
    ) {
        if (userId.isBlank()) {
            callback(
                DatabaseResult.Error(
                    "Invalid user ID"
                )
            )
            return
        }

        val ref =
            rootRef()
                ?.child(collectionPath)
                ?.child(userId)

        if (ref == null) {
            callback(
                DatabaseResult.Error(
                    "Firebase not initialized"
                )
            )
            return
        }

        FirebaseDatabaseIosHelper.observeValueOnce(
            query = ref
        ) { snapshot ->

            if (
                snapshot == null ||
                !snapshot.exists()
            ) {
                callback(
                    DatabaseResult.Success(
                        emptyList()
                    )
                )
                return@observeValueOnce
            }

            try {
                val appointments =
                    mutableListOf<Appointment>()

                snapshotChildren(
                    snapshot
                ).forEach {
                        childSnapshot ->

                    val data =
                        snapshotToMap(
                            childSnapshot
                        )
                            ?: return@forEach

                    val appointmentId =
                        childSnapshot.key()
                            ?: ""

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

            } catch (t: Throwable) {
                callback(
                    DatabaseResult.Error(
                        "Failed to parse appointments: ${t.message}"
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // updateAppointmentStatus
    // ------------------------------------------------------------------

    actual fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: AppointmentStatus,
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

        updateAppointmentStatusForUser(
            userId = userId,
            appointmentId = appointmentId,
            newStatus = newStatus,
            callback = callback
        )
    }

    // ------------------------------------------------------------------
    // updateAppointmentStatusForUser
    // ------------------------------------------------------------------

    actual fun updateAppointmentStatusForUser(
        userId: String,
        appointmentId: String,
        newStatus: AppointmentStatus,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        if (userId.isBlank()) {
            callback(
                DatabaseResult.Error(
                    "Invalid user ID"
                )
            )
            return
        }

        if (appointmentId.isBlank()) {
            callback(
                DatabaseResult.Error(
                    "Invalid appointment ID"
                )
            )
            return
        }

        val ref =
            rootRef()
                ?.child(collectionPath)
                ?.child(userId)
                ?.child(appointmentId)

        if (ref == null) {
            callback(
                DatabaseResult.Error(
                    "Firebase not initialized"
                )
            )
            return
        }

        val updates:
                Map<Any?, Any?> =
            mapOf(
                "status" to newStatus.name
            )

        ref.updateChildValues(
            values = updates,
            withCompletionBlock = {
                    error,
                    _ ->

                if (error == null) {
                    callback(
                        DatabaseResult.Success(
                            Unit
                        )
                    )
                } else {
                    callback(
                        DatabaseResult.Error(
                            "Failed to update appointment status: ${error.localizedDescription}"
                        )
                    )
                }
            }
        )
    }

    // ------------------------------------------------------------------
    // Firebase snapshot helpers
    // ------------------------------------------------------------------

    private fun snapshotChildren(
        snapshot: FIRDataSnapshot
    ): List<FIRDataSnapshot> {

        val result =
            mutableListOf<FIRDataSnapshot>()

        val enumerator =
            snapshot.children

        while (true) {
            val child =
                enumerator.nextObject()
                    ?: break

            if (child is FIRDataSnapshot) {
                result.add(child)
            }
        }

        return result
    }

    private fun snapshotToMap(
        snapshot: FIRDataSnapshot
    ): Map<String, Any?>? {

        val value =
            snapshot.value

        return when (value) {
            is Map<*, *> ->
                value as? Map<String, Any?>

            is NSDictionary ->
                nsDictionaryToMap(
                    value
                )

            else ->
                null
        }
    }

    private fun nsDictionaryToMap(
        dict: NSDictionary
    ): Map<String, Any?> {

        val result =
            mutableMapOf<String, Any?>()

        val keyEnumerator =
            dict.keyEnumerator()

        while (true) {
            val rawKey =
                keyEnumerator.nextObject()
                    ?: break

            val key =
                rawKey.toString()

            val value =
                dict.objectForKey(
                    rawKey
                )

            result[key] =
                when (value) {
                    is NSDictionary ->
                        nsDictionaryToMap(
                            value
                        )

                    is NSNull ->
                        null

                    else ->
                        value
                }
        }

        return result
    }
}