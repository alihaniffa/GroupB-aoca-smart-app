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
 * Appointments/{userId}/{appointmentId}
 */
actual class AppointmentService actual constructor() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // Main location for appointment records in Realtime Database.
    private val collectionPath = "Appointments"

    /**
     * Create a new appointment for the currently signed-in user.
     */
    actual fun createAppointment(
        appointment: Appointment,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        // Ensure required appointment details are provided.
        if (appointment.date.isBlank() || appointment.time.isBlank()) {
            callback(
                DatabaseResult.Error(
                    "Appointment date and time are required"
                )
            )
            return
        }

        // Generate a unique appointment ID.
        val newAppointmentRef =
            database.child(collectionPath)
                .child(userId)
                .push()

        val appointmentId = newAppointmentRef.key

        // Stop if Firebase fails to generate an appointment ID.
        if (appointmentId == null) {
            callback(
                DatabaseResult.Error(
                    "Failed to generate appointment ID"
                )
            )
            return
        }

        // Attach the authenticated user and generated appointment ID.
        val newAppointment = appointment.copy(
            id = appointmentId,
            userId = userId
        )

        val appointmentData =
            newAppointment.toMap().toMutableMap()

        // Firebase generates the actual timestamp on the server.
        appointmentData["createdAt"] = ServerValue.TIMESTAMP
        appointmentData["updatedAt"] = ServerValue.TIMESTAMP

        newAppointmentRef
            .setValue(appointmentData)
            .addOnSuccessListener {
                callback(DatabaseResult.Success(Unit))
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
     * Get all appointments belonging to the currently signed-in user.
     */
    actual fun getAppointments(
        callback: (DatabaseResult<List<Appointment>>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        database.child(collectionPath)
            .child(userId)
            .addListenerForSingleValueEvent(
                object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val appointments =
                                mutableListOf<Appointment>()

                            for (child in snapshot.children) {
                                val data =
                                    child.value as? Map<*, *>
                                        ?: continue

                                val appointmentId =
                                    child.key ?: continue

                                appointments.add(
                                    Appointment.fromMap(
                                        data,
                                        appointmentId
                                    )
                                )
                            }

                            callback(
                                DatabaseResult.Success(appointments)
                            )

                        } catch (e: Exception) {
                            callback(
                                DatabaseResult.Error(
                                    "Failed to parse appointments: ${e.message}"
                                )
                            )
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
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
     * Update the status of an existing appointment.
     */
    actual fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: AppointmentStatus,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        // Ensure a valid appointment ID was provided.
        if (appointmentId.isBlank()) {
            callback(DatabaseResult.Error("Invalid appointment ID"))
            return
        }

        val appointmentRef =
            database.child(collectionPath)
                .child(userId)
                .child(appointmentId)

        val updates = mapOf<String, Any>(
            "status" to newStatus.name,
            "updatedAt" to ServerValue.TIMESTAMP
        )

        appointmentRef
            .updateChildren(updates)
            .addOnSuccessListener {
                callback(DatabaseResult.Success(Unit))
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

// previous code
/*
package org.example.dementia_tester_app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Android actual — writes/reads appointments in Firebase Realtime DB.
 * Nested under userId to match security rules and ensure consistency.
 */
actual class AppointmentService {
    private val auth = FirebaseAuth.getInstance() // accesses Firebase Authentication and is used to identify the currently signed-in user.
    private val database = FirebaseDatabase.getInstance().reference // gets a reference to the root of Firebase Realtime Database
    private val collectionPath = "Appointments" // defines the main database location where appointment records are stored

    actual fun createAppointment(appointment: Appointment, callback: (DatabaseResult<Unit>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) { 
            callback(DatabaseResult.Error("No user is signed in"))
            return 
        }

        // ensure required appointment details are provided
        if (appointment.date.isBlank() || appointment.time.isBlank()) {
            callback(DatabaseResult.Error("Appointment date and time are required"))
            return
        }

        // Generate a unique ID using push() under the user's specific node
        val newApptRef = database.child(collectionPath).child(userId).push()

        // ensure a valid appointment ID was generated before saving
        val id = newApptRef.key
        // stop if firebase fails to generate an appointment ID
        if (id == null) {
            callback(DatabaseResult.Error("Failed to generate appointment ID"))
            return
        }

        val appt = appointment.copy(id = id, userId = userId)
        
        newApptRef.setValue(appt.toMap())
            .addOnSuccessListener { callback(DatabaseResult.Success(Unit)) }
            .addOnFailureListener { e ->
                callback(DatabaseResult.Error("Failed to book appointment: ${e.message}"))
            }
    }

    actual fun getAppointments(callback: (DatabaseResult<List<Appointment>>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) { 
            callback(DatabaseResult.Error("No user is signed in"))
            return 
        }

        database.child(collectionPath).child(userId).orderByChild("date")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val list = mutableListOf<Appointment>()
                        for (child in snapshot.children) {
                            val data = child.value as? Map<*, *> ?: continue
                            list.add(Appointment.fromMap(data, child.key ?: ""))
                        }
                        callback(DatabaseResult.Success(list))
                    } catch (e: Exception) {
                        callback(DatabaseResult.Error("Failed to parse appointments: ${e.message}"))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(DatabaseResult.Error("Failed to load appointments: ${error.message}"))
                }
            })
    }

    actual fun updateAppointmentStatus(appointmentId: String, newStatus: AppointmentStatus, callback: (DatabaseResult<Unit>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        // ensure a valid appointment ID was provided
        if (appointmentId.isBlank()) {
            callback(DatabaseResult.Error("Invalid appointment ID"))
            return
        }

        database.child(collectionPath).child(userId).child(appointmentId).child("status")
            .setValue(newStatus.name)
            .addOnSuccessListener { callback(DatabaseResult.Success(Unit)) }
            .addOnFailureListener { e ->
                callback(DatabaseResult.Error("Failed to update status: ${e.message}"))
            }
    }
}
*/