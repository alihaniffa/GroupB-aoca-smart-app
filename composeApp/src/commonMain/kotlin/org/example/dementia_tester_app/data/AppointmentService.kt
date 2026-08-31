package org.example.dementia_tester_app.data

expect class AppointmentService() {

    /**
     * Create an appointment for the
     * currently authenticated user.
     */
    fun createAppointment(
        appointment: Appointment,
        callback: (DatabaseResult<Unit>) -> Unit
    )

    /**
     * Create an appointment for a
     * specific user.
     *
     * This is used when a caregiver is
     * acting on behalf of an assigned patient.
     */
    fun createAppointmentForUser(
        userId: String,
        appointment: Appointment,
        callback: (DatabaseResult<Unit>) -> Unit
    )

    /**
     * Get appointments belonging to the
     * currently authenticated user.
     */
    fun getAppointments(
        callback: (DatabaseResult<List<Appointment>>) -> Unit
    )

    /**
     * Get appointments belonging to a
     * specific user.
     *
     * This allows a caregiver to retrieve
     * the selected patient's appointments.
     */
    fun getAppointmentsForUser(
        userId: String,
        callback: (DatabaseResult<List<Appointment>>) -> Unit
    )

    /**
     * Update an appointment belonging to
     * the currently authenticated user.
     */
    fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: AppointmentStatus,
        callback: (DatabaseResult<Unit>) -> Unit
    )

    /**
     * Update an appointment belonging to
     * a specific user.
     *
     * This allows a caregiver to update
     * an assigned patient's appointment.
     */
    fun updateAppointmentStatusForUser(
        userId: String,
        appointmentId: String,
        newStatus: AppointmentStatus,
        callback: (DatabaseResult<Unit>) -> Unit
    )
}