package org.example.dementia_tester_app.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.dementia_tester_app.ui.components.*
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.Composable
import dementiatesterapp.composeapp.generated.resources.Res
import dementiatesterapp.composeapp.generated.resources.icon_transparent
import org.example.dementia_tester_app.auth.AuthResult
import org.example.dementia_tester_app.auth.AuthServiceInterface
import org.example.dementia_tester_app.auth.AuthService
import org.example.dementia_tester_app.ui.components.LoadingSpinner
import org.example.dementia_tester_app.utils.validateFields
import org.example.dementia_tester_app.utils.isValidEmail

/**
 * Expect component for Google Sign In Button
 */
@Composable
expect fun GoogleSignInButton(
    onSignInSuccess: () -> Unit,
    onSignInError: (String) -> Unit
)

@Composable
fun LoginIcon() {
    Image(
        painter = painterResource(Res.drawable.icon_transparent),
        contentDescription = "Dementia Tester Logo",
        modifier = Modifier.size(200.dp)
    )
}

/**
 * Login screen with email and password fields, login button, and sign-up button
 * @param onLogin Callback to be invoked when the login button is clicked
 * @param onSignUp Callback to be invoked when the sign up button is clicked
 * @param onForgotPassword Callback to be invoked when the forgot password link is clicked
 */
@Composable
fun Login(
    authService: AuthServiceInterface = remember { AuthService() },
    onLogin: (String) -> Unit = { _ -> },
    onSignUp: () -> Unit = {},
    onForgotPassword: () -> Unit = {}
) {
    val EMAIL = "email"
    val PASSWORD = "password"

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Using a single map to track all field error states
    var fieldErrors by remember { mutableStateOf(mapOf<String, Boolean>()) }

    // Helper function to get error state for a field
    fun isFieldError(field: String): Boolean = fieldErrors[field] == true

    // Helper function to clear error for a field
    fun clearFieldError(field: String) {
        if (fieldErrors.containsKey(field)) {
            fieldErrors = fieldErrors - field
        }
    }

    var showErrorMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("Please enter all required fields") }

    var isLoading by remember { mutableStateOf(false) }

    fun handleLogin() {
        val trimmedEmail = email.trim()
        // Step 1: Check for empty fields using the shared validation utility
        fieldErrors = validateFields(
            mapOf(
                EMAIL to trimmedEmail,
                PASSWORD to password
            )
        )

        if (fieldErrors.isNotEmpty()) {
            errorMessage = "Please enter all required fields"
            showErrorMessage = true
            // Step 2: Check email format before making any network call using shared utility
        } else if (!trimmedEmail.isValidEmail()) {
            fieldErrors = mapOf(EMAIL to true)
            errorMessage = "Please enter a valid email address"
            showErrorMessage = true
        } else {
            // All fields are valid, proceed with login
            isLoading = true
            authService.signIn(trimmedEmail, password) { result ->
                isLoading = false
                when (result) {
                    is AuthResult.Success -> {
                        onLogin(trimmedEmail)
                    }
                    is AuthResult.Error -> {
                        errorMessage = result.message
                        showErrorMessage = true
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
                .padding(bottom = 240.dp), // Space for the bottom buttons
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // Changed to Top for better scrolling
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            
            // App Title
            Text(
                text = "AoCA Smart App",
                fontSize = 24.sp,
            )

            // App Icon
            LoginIcon()

            // Email Field
            FormTextField(
                value = email,
                onValueChange = {
                    email = it
                    clearFieldError(EMAIL)
                    showErrorMessage = false
                },
                label = "Email",
                isError = isFieldError(EMAIL),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )

            // Password Field
            FormTextField(
                value = password,
                onValueChange = {
                    password = it
                    clearFieldError(PASSWORD)
                    showErrorMessage = false
                },
                label = "Password",
                isError = isFieldError(PASSWORD),
                keyboardType = KeyboardType.Password,
                isPassword = true,
                imeAction = ImeAction.Done,
    keyboardActions = KeyboardActions(
        onDone = { handleLogin() } 
    )
)
            // Forgot Password Link
            TextButton(
                onClick = { onForgotPassword() },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Forgot your password?",
                    color = FormColors.green
                )
            }
            
            // General Error Message
            ErrorMessage(show = showErrorMessage, message = errorMessage)
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Bottom section with Login and Sign Up buttons
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                // Login Button
                Button(
                    onClick = { handleLogin() },
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
                        Text("Login")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sign Up Button
                OutlinedButton(
                    onClick = { onSignUp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = FormColors.green
                    ),
                    enabled = !isLoading
                ) {
                    Text("Sign Up")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google OAuth Button
                GoogleSignInButton(
                    onSignInSuccess = {
                        onLogin(authService.getCurrentUserEmail() ?: "")
                    },
                    onSignInError = { error ->
                        errorMessage = error
                        showErrorMessage = true
                    }
                )
            }
        }
    }
}
