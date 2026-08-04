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
