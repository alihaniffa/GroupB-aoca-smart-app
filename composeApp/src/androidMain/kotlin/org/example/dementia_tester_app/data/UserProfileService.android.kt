package org.example.dementia_tester_app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import android.util.Log

/**
 * Android implementation of UserProfileService using Firebase Realtime Database and Storage.
 * Migrated from Firestore to fix permission issues and ensure consistency.
 */
actual class UserProfileService actual constructor() : UserProfileServiceInterface {
    private val auth = FirebaseAuth.getInstance() //gets the currently signed-in user
    private val database = Firebase.database.reference //accesses the root of Realtime Database
    private val storage = Firebase.storage //accesses Firebase Storage
    private val dbPath = "UserProfiles" //defines where user profiles are stored.

    /**
     * Get the current user's profile from Realtime Database
     */
    actual override fun getCurrentUserProfile(callback: (DatabaseResult<UserProfile>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }
        
        database.child(dbPath).child(userId).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    callback(DatabaseResult.Error("Profile not found. Please create a profile."))
                    return@addOnSuccessListener
                }
                
                try {
                    val data = snapshot.value as? Map<*, *> // possible issue
                    if (data != null) {
                        val profile = UserProfile.fromMap(data, userId)
                        callback(DatabaseResult.Success(profile))
                    } else {
                        callback(DatabaseResult.Error("Profile data is empty"))
                    }
                } catch (e: Exception) {
                    callback(DatabaseResult.Error("Failed to parse user profile: ${e.message}"))
                }
            }
            .addOnFailureListener { e ->
                callback(DatabaseResult.Error("Failed to get user profile: ${e.message}"))
            }
    }

    /**
     * Update the current user's profile in Realtime Database
     */
    actual override fun updateUserProfile(userProfile: UserProfile, callback: (DatabaseResult<Unit>) -> Unit) {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid
        if (userId == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        database.child(dbPath).child(userId).get()
            .addOnSuccessListener { snapshot ->
                val updates = userProfile.toMap().toMutableMap()
                
                if (snapshot.exists()) {
                    val data = snapshot.value as? Map<*, *>
                    val existingUserType = data?.get("userType")?.toString() ?: "user"
                    val existingEmail = data?.get("email")?.toString() ?.takeIf{ it.isNotBlank() } ?: currentUser.email.orEmpty()
                    
                    updates["userType"] = existingUserType
                    updates["email"] = existingEmail
                } else {
                    val profileEmail = updates["email"] as? String

                    if (profileEmail.isNullOrBlank()) {
                        updates["email"] = currentUser.email.orEmpty()
                    }

                    if (updates["userType"] == null) {
                        updates["userType"] = "user"
                    }

                    //if (updates["email"] == null || (updates["email"] as String).isEmpty()) {
                      //  updates["email"] = currentUser.email ?: ""
                    //}
                }
                
                database.child(dbPath).child(userId).updateChildren(updates)
                    .addOnSuccessListener {
                        callback(DatabaseResult.Success(Unit))
                    }
                    .addOnFailureListener { e ->
                        callback(DatabaseResult.Error("Failed to update user profile: ${e.message}"))
                    }
            }
            .addOnFailureListener { e ->
                callback(DatabaseResult.Error("Pre-update fetch failed: ${e.message}"))
            }
    }

    /**
     * Get all users with userType = User
     */
    actual override fun getAllUsers(callback: (DatabaseResult<List<UserProfile>>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }
        
        database.child(dbPath).child(userId).get()
            .addOnSuccessListener { snapshot ->
                val data = snapshot.value as? Map<*, *>
                val userType = data?.get("userType")?.toString()
                
                if (userType != "doctor") {
                    callback(DatabaseResult.Error("Not authorized. Only doctors can access user data."))
                    return@addOnSuccessListener
                }
                
                fetchAllUsers(callback)
            }
            .addOnFailureListener { e ->
                callback(DatabaseResult.Error("Authorization check failed: ${e.message}"))
            }
    }
    
    private fun fetchAllUsers(callback: (DatabaseResult<List<UserProfile>>) -> Unit) {
        database.child(dbPath)
            .orderByChild("userType")
            .equalTo("user")
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val userProfiles = snapshot.children.mapNotNull { child ->
                        val data = child.value as? Map<*, *> ?: return@mapNotNull null
                        UserProfile.fromMap(data, child.key ?: "")
                    }
                    callback(DatabaseResult.Success(userProfiles))
                } catch (e: Exception) {
                    callback(DatabaseResult.Error("Failed to fetch users: ${e.message}"))
                }
            }
            .addOnFailureListener { e ->
                callback(DatabaseResult.Error("Failed to fetch users: ${e.message}"))
            }
    }

    /**
     * Upload a profile image to Firebase Storage and return the download URL
     */
    actual override fun uploadProfileImage(imageBytes: ByteArray, callback: (DatabaseResult<String>) -> Unit) {
        if (imageBytes.isEmpty()) {
            callback(DatabaseResult.Error("The selected image is empty"))
            return
        }

        val userId = auth.currentUser?.uid

        if (userId == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        val profileImageRef = storage.reference.child("profile_images/${userId}.jpg")

        profileImageRef.putBytes(imageBytes)
            .addOnSuccessListener {
                profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                    val url = uri.toString()

                    database.child(dbPath).child(userId).child("profileImageUrl").setValue(url)
                        .addOnSuccessListener {
                            callback(DatabaseResult.Success(url))
                        }
                        .addOnFailureListener {
                            //callback(DatabaseResult.Success(url)) //possible issue -> even when saving the image URL fails, the app still reports success.
                            // possible fix
                            e -> callback(
                                DatabaseResult.Error(
                                    "Image uploaded, but failed to save profile URL: ${e.message}"
                                )
                            )
                        }
                }.addOnFailureListener { e ->
                    callback(DatabaseResult.Error("Failed to get download URL: ${e.message}"))
                }
            }
            .addOnFailureListener { e ->
                callback(DatabaseResult.Error("Failed to upload image: ${e.message}"))
            }
    }
}
