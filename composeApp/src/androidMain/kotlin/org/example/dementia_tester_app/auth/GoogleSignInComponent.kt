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

@Composable
fun GoogleSignInComponent(
    onSignInSuccess: () -> Unit,
    onSignInError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val database = remember { Firebase.database.reference }

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
                                    val profileRef = database.child("UserProfiles").child(user.uid)
                                    profileRef.get().addOnSuccessListener { snapshot ->
                                        if (!snapshot.exists()) {
                                            val newProfile = UserProfile(
                                                userId = user.uid,
                                                name = user.displayName ?: (account.displayName ?: ""),
                                                email = user.email ?: (account.email ?: ""),
                                                userType = UserType.USER
                                            )
                                            val updates = newProfile.toMap().toMutableMap()
                                            updates["createdAt"] = ServerValue.TIMESTAMP
                                            updates["updatedAt"] = ServerValue.TIMESTAMP

                                            profileRef.setValue(updates)
                                                .addOnSuccessListener {
                                                    Log.d("GoogleAuth", "RTDB profile created")
                                                    onSignInSuccess()
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("GoogleAuth", "Failed to create RTDB profile", e)
                                                    onSignInError("Failed to setup user profile: ${e.message}")
                                                }
                                        } else {
                                            Log.d("GoogleAuth", "Existing RTDB profile found")
                                            onSignInSuccess()
                                        }
                                    }.addOnFailureListener { e ->
                                        Log.e("GoogleAuth", "Failed to check RTDB profile", e)
                                        // Still allow sign-in if snapshot check failed
                                        onSignInSuccess()
                                    }
                                } else {
                                    onSignInError("User not found after sign in.")
                                }
                            } else {
                                Log.e("GoogleAuth", "Firebase auth failed", authResult.exception)
                                onSignInError(authResult.exception?.localizedMessage ?: "Auth failed")
                            }
                        }
                } ?: run {
                    onSignInError("Failed to obtain ID token from Google.")
                }
            } catch (e: ApiException) {
                Log.e("GoogleAuth", "Google sign in failed", e)
                onSignInError("Google sign in canceled or failed.")
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
