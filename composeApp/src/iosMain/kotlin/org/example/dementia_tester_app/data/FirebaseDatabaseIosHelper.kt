package org.example.dementia_tester_app.data

import cocoapods.FirebaseDatabase.FIRDataSnapshot
import cocoapods.FirebaseDatabase.FIRDatabaseQuery
import cocoapods.FirebaseDatabaseBridge.FirebaseDatabaseBridge

/**
 * Central helper for Firebase Realtime Database reads on iOS.
 *
 * FIRDataEventTypeValue is handled by the Objective-C bridge so that
 * Kotlin/Native does not need to interact directly with the problematic
 * Firebase Objective-C enum.
 */
internal object FirebaseDatabaseIosHelper {

    fun observeValueOnce(
        query: FIRDatabaseQuery,
        callback: (FIRDataSnapshot?) -> Unit
    ) {
        FirebaseDatabaseBridge.observeValueOnceForQuery(
            query = query,
            completion = { snapshot ->
                callback(snapshot)
            }
        )
    }
}