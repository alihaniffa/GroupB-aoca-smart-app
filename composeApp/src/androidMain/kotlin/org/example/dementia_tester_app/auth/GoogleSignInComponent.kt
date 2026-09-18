package org.example.dementia_tester_app.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import org.example.dementia_tester_app.data.UserProfile
import org.example.dementia_tester_app.data.UserType

object GoogleSignInHelper {
    /**
     * Create the default user profile for a newly signed-in Google user.
     */
    fun createInitialUserProfile(
        userId: String,
        displayName: String?,
        email: String?
    ): UserProfile {
        return UserProfile(
            userId = userId,
            name = displayName.orEmpty(),
            email = email.orEmpty(),
            userType = UserType.USER
        )
    }

    /**
     * Prepare the profile map with server timestamps to persist into Realtime Database.
     */
    fun buildProfileUpdateMap(
        profile: UserProfile,
        timestamp: Any = ServerValue.TIMESTAMP
    ): Map<String, Any> {
        val updates = profile.toMap().toMutableMap()
        updates["createdAt"] = timestamp
        updates["updatedAt"] = timestamp
        return updates
    }
}

object GoogleSignInResultHandler {
    fun getErrorMessageForApiException(e: ApiException?): String {
        return "Google sign in canceled or failed."
    }

    fun getErrorMessageForAuthFailure(exception: Exception?): String {
        return exception?.localizedMessage ?: "Auth failed"
    }
}

class GoogleSignInProfileHandler(
    private val checkProfileExists: (userId: String, onResult: (Boolean, Exception?) -> Unit) -> Unit,
    private val saveProfile: (userId: String, data: Map<String, Any>, onComplete: (Exception?) -> Unit) -> Unit,
    private val logDebug: (String) -> Unit = { msg -> runCatching { Log.d("GoogleAuth", msg) } },
    private val logError: (String, Throwable?) -> Unit = { msg, tr -> runCatching { Log.e("GoogleAuth", msg, tr) } }
) {
    fun syncUserProfile(
        userId: String,
        displayName: String?,
        email: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        checkProfileExists(userId) { exists, checkError ->
            if (checkError != null) {
                logError("Failed to check RTDB profile", checkError)
                onSuccess()
                return@checkProfileExists
            }

            if (!exists) {
                val newProfile = GoogleSignInHelper.createInitialUserProfile(
                    userId = userId,
                    displayName = displayName,
                    email = email
                )
                val updates = GoogleSignInHelper.buildProfileUpdateMap(newProfile)

                saveProfile(userId, updates) { saveError ->
                    if (saveError == null) {
                        logDebug("RTDB profile created")
                        onSuccess()
                    } else {
                        logError("Failed to create RTDB profile", saveError)
                        onError("Failed to setup user profile: ${saveError.message}")
                    }
                }
            } else {
                logDebug("Existing RTDB profile found")
                onSuccess()
            }
        }
    }
}

@Composable
fun GoogleSignInComponent(
    onSignInSuccess: () -> Unit,
    onSignInError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val database = remember { Firebase.database.reference }

    val profileHandler = remember {
        GoogleSignInProfileHandler(
            checkProfileExists = { uid, callback ->
                database.child("UserProfiles").child(uid).get()
                    .addOnSuccessListener { snapshot -> callback(snapshot.exists(), null) }
                    .addOnFailureListener { error -> callback(false, error) }
            },
            saveProfile = { uid, data, callback ->
                database.child("UserProfiles").child(uid).setValue(data)
                    .addOnSuccessListener { callback(null) }
                    .addOnFailureListener { error -> callback(error) }
            }
        )
    }

    val webClientId = "238088670555-acth1uvst0h3ka9nh9g1o8ke1n4poddn.apps.googleusercontent.com"

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    val credential = GoogleAuthProvider.getCredential(idToken, null)

                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { authResult ->
                            if (authResult.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    profileHandler.syncUserProfile(
                                        userId = user.uid,
                                        displayName = user.displayName ?: account.displayName,
                                        email = user.email ?: account.email,
                                        onSuccess = onSignInSuccess,
                                        onError = onSignInError
                                    )
                                } else {
                                    onSignInError("User not found after sign in.")
                                }
                            } else {
                                Log.e("GoogleAuth", "Firebase auth failed", authResult.exception)
                                onSignInError(GoogleSignInResultHandler.getErrorMessageForAuthFailure(authResult.exception))
                            }
                        }
                } ?: run {
                    onSignInError("Failed to obtain ID token from Google.")
                }
            } catch (e: ApiException) {
                Log.e("GoogleAuth", "Google sign in failed", e)
                onSignInError(GoogleSignInResultHandler.getErrorMessageForApiException(e))
            }
        }
    }

    OutlinedButton(
        onClick = { launcher.launch(googleSignInClient.signInIntent) },
        modifier = modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("Continue with Google")
    }
}
