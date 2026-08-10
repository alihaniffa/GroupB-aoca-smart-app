package org.example.dementia_tester_app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

/**
 * Android implementation of UserProfileService using Firebase
 * Realtime Database and Firebase Storage.
 */
actual class UserProfileService actual constructor() : UserProfileServiceInterface {

    private val auth = FirebaseAuth.getInstance()
    private val database = Firebase.database.reference
    private val storage = Firebase.storage

    // Location where user profiles are stored in Realtime Database.
    private val dbPath = "UserProfiles"

    /**
     * Get the current signed-in user's profile.
     */
    actual override fun getCurrentUserProfile(
        callback: (DatabaseResult<UserProfile>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        database.child(dbPath).child(userId).get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    callback(
                        DatabaseResult.Error(
                            "Profile not found. Please create a profile."
                        )
                    )
                    return@addOnSuccessListener
                }

                try {
                    val data = snapshot.value as? Map<*, *>

                    if (data != null) {
                        val profile = UserProfile.fromMap(data, userId)
                        callback(DatabaseResult.Success(profile))
                    } else {
                        callback(
                            DatabaseResult.Error("Profile data is empty")
                        )
                    }
                } catch (e: Exception) {
                    callback(
                        DatabaseResult.Error(
                            "Failed to parse user profile: ${e.message}"
                        )
                    )
                }
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Failed to get user profile: ${e.message}"
                    )
                )
            }
    }

    /**
     * Create or update the current signed-in user's profile.
     */
    actual override fun updateUserProfile(
        userProfile: UserProfile,
        callback: (DatabaseResult<Unit>) -> Unit
    ) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        val userId = currentUser.uid
        val profileRef = database.child(dbPath).child(userId)

        profileRef.get()
            .addOnSuccessListener { snapshot ->

                val updates = userProfile.toMap().toMutableMap()

                // Always use the authenticated user's UID.
                updates["userId"] = userId

                // Updated whenever the profile is saved.
                updates["updatedAt"] = ServerValue.TIMESTAMP

                if (snapshot.exists()) {

                    val data = snapshot.value as? Map<*, *>

                    // Preserve the existing user role.
                    val existingUserType =
                        data?.get("userType")?.toString()
                            ?.takeIf { it.isNotBlank() }
                            ?: UserType.USER.value

                    // Preserve the existing email where possible.
                    val existingEmail =
                        data?.get("email")?.toString()
                            ?.takeIf { it.isNotBlank() }
                            ?: currentUser.email.orEmpty()

                    updates["userType"] = existingUserType
                    updates["email"] = existingEmail

                    // Preserve the original profile creation timestamp.
                    val existingCreatedAt = data?.get("createdAt")

                    if (existingCreatedAt != null) {
                        updates["createdAt"] = existingCreatedAt
                    } else {
                        updates["createdAt"] = ServerValue.TIMESTAMP
                    }

                } else {

                    val profileEmail = updates["email"] as? String

                    if (profileEmail.isNullOrBlank()) {
                        updates["email"] = currentUser.email.orEmpty()
                    }

                    // New profiles start as regular users.
                    updates["userType"] = UserType.USER.value

                    // Set only when the profile is first created.
                    updates["createdAt"] = ServerValue.TIMESTAMP
                }

                profileRef.updateChildren(updates)
                    .addOnSuccessListener {
                        callback(DatabaseResult.Success(Unit))
                    }
                    .addOnFailureListener { e ->
                        callback(
                            DatabaseResult.Error(
                                "Failed to update user profile: ${e.message}"
                            )
                        )
                    }
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Pre-update fetch failed: ${e.message}"
                    )
                )
            }
    }

    /**
     * Get all regular users.
     * Only users with the doctor role are allowed to perform this operation.
     */
    actual override fun getAllUsers(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        database.child(dbPath).child(userId).get()
            .addOnSuccessListener { snapshot ->

                val data = snapshot.value as? Map<*, *>
                val userType = data?.get("userType")?.toString()

                if (userType != UserType.DOCTOR.value) {
                    callback(
                        DatabaseResult.Error(
                            "Not authorized. Only doctors can access user data."
                        )
                    )
                    return@addOnSuccessListener
                }

                fetchAllUsers(callback)
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Authorization check failed: ${e.message}"
                    )
                )
            }
    }

    /**
     * Fetch profiles whose userType is "user".
     */
    private fun fetchAllUsers(
        callback: (DatabaseResult<List<UserProfile>>) -> Unit
    ) {
        database.child(dbPath)
            .orderByChild("userType")
            .equalTo(UserType.USER.value)
            .get()
            .addOnSuccessListener { snapshot ->

                try {
                    val userProfiles = snapshot.children.mapNotNull { child ->

                        val data =
                            child.value as? Map<*, *>
                                ?: return@mapNotNull null

                        UserProfile.fromMap(
                            data,
                            child.key ?: ""
                        )
                    }

                    callback(
                        DatabaseResult.Success(userProfiles)
                    )

                } catch (e: Exception) {
                    callback(
                        DatabaseResult.Error(
                            "Failed to fetch users: ${e.message}"
                        )
                    )
                }
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Failed to fetch users: ${e.message}"
                    )
                )
            }
    }

    /**
     * Upload the current user's profile image to Firebase Storage
     * and save its download URL in Realtime Database.
     */
    actual override fun uploadProfileImage(
        imageBytes: ByteArray,
        callback: (DatabaseResult<String>) -> Unit
    ) {
        if (imageBytes.isEmpty()) {
            callback(
                DatabaseResult.Error(
                    "The selected image is empty"
                )
            )
            return
        }

        val userId = auth.currentUser?.uid

        if (userId == null) {
            callback(DatabaseResult.Error("No user is signed in"))
            return
        }

        val profileImageRef =
            storage.reference.child(
                "profile_images/${userId}.jpg"
            )

        profileImageRef.putBytes(imageBytes)
            .addOnSuccessListener {

                profileImageRef.downloadUrl
                    .addOnSuccessListener { uri ->

                        val url = uri.toString()

                        database.child(dbPath)
                            .child(userId)
                            .child("profileImageUrl")
                            .setValue(url)
                            .addOnSuccessListener {
                                callback(
                                    DatabaseResult.Success(url)
                                )
                            }
                            .addOnFailureListener { e ->
                                callback(
                                    DatabaseResult.Error(
                                        "Image uploaded, but failed to save " +
                                                "profile URL: ${e.message}"
                                    )
                                )
                            }
                    }
                    .addOnFailureListener { e ->
                        callback(
                            DatabaseResult.Error(
                                "Failed to get download URL: ${e.message}"
                            )
                        )
                    }
            }
            .addOnFailureListener { e ->
                callback(
                    DatabaseResult.Error(
                        "Failed to upload image: ${e.message}"
                    )
                )
            }
    }
}