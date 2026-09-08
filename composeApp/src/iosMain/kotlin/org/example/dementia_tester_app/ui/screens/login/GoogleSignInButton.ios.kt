package org.example.dementia_tester_app.ui.screens.login

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * iOS implementation of the Google Sign-In button.
 *
 * Google Sign-In is not currently configured for iOS.
 * Email/password authentication remains available.
 */
@Composable
actual fun GoogleSignInButton(
    onSignInSuccess: () -> Unit,
    onSignInError: (String) -> Unit
) {
    OutlinedButton(
        onClick = {
            onSignInError(
                "Google Sign-In is not currently available on iOS."
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(
            text = "Sign in with Google"
        )
    }
}