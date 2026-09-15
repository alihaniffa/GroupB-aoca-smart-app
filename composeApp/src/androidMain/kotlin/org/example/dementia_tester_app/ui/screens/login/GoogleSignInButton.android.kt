package org.example.dementia_tester_app.ui.screens.login

import androidx.compose.runtime.Composable
import org.example.dementia_tester_app.auth.GoogleSignInComponent

@Composable
actual fun GoogleSignInButton(
    onSignInSuccess: () -> Unit,
    onSignInError: (String) -> Unit
) {
    GoogleSignInComponent(
        onSignInSuccess = onSignInSuccess,
        onSignInError = onSignInError
    )
}
