package org.example.dementia_tester_app.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.dementia_tester_app.ui.components.*
import org.example.dementia_tester_app.auth.AuthResult
import org.example.dementia_tester_app.auth.AuthService
import org.example.dementia_tester_app.auth.AuthServiceInterface
import org.example.dementia_tester_app.ui.components.LoadingSpinner
import org.example.dementia_tester_app.ui.components.SuccessMessage
import org.example.dementia_tester_app.utils.isEmptyTrimmed

/**
 * Forgot Password screen with an email field and submit button
 * @param onBack Callback to be invoked when the user wants to go back to the login screen
 */
@Composable
fun ForgotPassword(
    authService: AuthServiceInterface = remember { AuthService() },
    onBack: () -> Unit = {},
    onResetSent: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    
    // Error states
    var emailError by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("Please enter your email") }
    
    // Success message state
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Page Title
        Text(
            text = "Forgot Password",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Email Field
        FormTextField(
            value = email,
            onValueChange = { 
                email = it
                emailError = false
                showErrorMessage = false
            },
            label = "Email",
            isError = emailError,
            keyboardType = KeyboardType.Email,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Submit Button
        Button(
            onClick = {
                val trimmedEmail = email.trim()
                val isEmailEmpty = trimmedEmail.isEmpty()

                if (isEmailEmpty) {
                    emailError = true
                    errorMessage = "Please enter your email"
                    showErrorMessage = true
                } else {
                    isLoading = true
                    authService.sendPasswordResetEmail(trimmedEmail) { result ->
                        isLoading = false
                        when (result) {
                            is AuthResult.Success -> {
                                successMessage = "Password reset email has been sent to $trimmedEmail"
                                showSuccessMessage = true
                            }
                            is AuthResult.Error -> {
                                errorMessage = result.message
                                showErrorMessage = true
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FormColors.green
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                LoadingSpinner()
            } else {
                Text("Submit")
            }
        }

        ErrorMessage(show = showErrorMessage, message = errorMessage)
        
        // Display a success message if available
        successMessage?.let {
            SuccessMessage(
                message = it,
                isVisible = showSuccessMessage,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { onBack() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = FormColors.green
            )
        ) {
            Text("Back to Login")
        }
    }
}