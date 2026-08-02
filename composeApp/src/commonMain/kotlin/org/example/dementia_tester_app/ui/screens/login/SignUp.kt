package org.example.dementia_tester_app.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.dementia_tester_app.ui.components.*
import org.example.dementia_tester_app.auth.AuthResult
import org.example.dementia_tester_app.auth.AuthService
import org.example.dementia_tester_app.auth.AuthServiceInterface
import org.example.dementia_tester_app.data.UserProfile
import org.example.dementia_tester_app.data.UserProfileService
import org.example.dementia_tester_app.data.DatabaseResult
import org.example.dementia_tester_app.data.UserType
import org.example.dementia_tester_app.ui.components.LoadingSpinner
import org.example.dementia_tester_app.utils.*
import androidx.compose.material3.MaterialTheme // Added Import


/**
 * SignUp screen with user details and emergency contact information
 */
@Composable
fun SignUp(
    authService: AuthServiceInterface = remember { AuthService() },
    onBack: () -> Unit = {},
    onSignUpSuccess: (String) -> Unit = {_ ->}
) {
    val NAME = "name"
    val EMAIL = "email"
    val DATE_OF_BIRTH = "dateOfBirth"
    val PASSWORD = "password"
    val CONFIRM_PASSWORD = "confirmPassword"
    val PHONE_NUMBER = "phoneNumber"
    val ADDRESS = "address"
    val SUBURB = "suburb"
    val STATE = "state"
    val POSTCODE = "postcode"
    val COUNTRY = "country"
    val GENDER = "gender"
    val EMERGENCY_NAME = "emergencyName"
    val EMERGENCY_EMAIL = "emergencyEmail"
    val EMERGENCY_RELATION = "emergencyRelation"
    val EMERGENCY_PHONE_NUMBER = "emergencyPhoneNumber"
    
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var suburb by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var postcode by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    var emergencyName by remember { mutableStateOf("") }
    var emergencyEmail by remember { mutableStateOf("") }
    var emergencyRelation by remember { mutableStateOf("") }
    var emergencyPhoneNumber by remember { mutableStateOf("") }

    var fieldErrors by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var passwordsMatchError by remember { mutableStateOf(false) }
    
    fun isFieldError(field: String): Boolean = fieldErrors[field] == true
    
    fun clearFieldError(field: String) {
        if (fieldErrors.containsKey(field)) {
            fieldErrors = fieldErrors - field
        }
    }
    
    fun updateFieldError(field: String, hasError: Boolean) {
        fieldErrors = if (hasError) fieldErrors + (field to true) else fieldErrors - field
    }

    var showErrorMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("Please enter all required fields") }

    val userProfileService = remember { UserProfileService() }
    var isLoading by remember { mutableStateOf(false) }

    fun handleSignUp(email: String, password: String) {
        isLoading = true
        authService.signUp(email, password) { result ->
            when (result) {
                is AuthResult.Success -> {
                    val userProfile = UserProfile(
                        name = name,
                        dateOfBirth = dateOfBirth,
                        email = email.trim(),
                        phoneNumber = phoneNumber.trim(),
                        userType = UserType.USER,
                        address = address,
                        suburb = suburb,
                        state = state,
                        postcode = postcode,
                        country = country,
                        gender = gender,
                        emergencyName = emergencyName,
                        emergencyEmail = emergencyEmail.trim(),
                        emergencyRelation = emergencyRelation,
                        emergencyPhoneNumber = emergencyPhoneNumber.trim()
                    )

                    userProfileService.updateUserProfile(userProfile) { dbResult ->
                        isLoading = false
                        when (dbResult) {
                            is DatabaseResult.Success -> onSignUpSuccess(email)
                            is DatabaseResult.Error -> {
                                errorMessage = "Signup successful but profile save failed: ${dbResult.message}"
                                showErrorMessage = true
                            }
                        }
                    }
                }
                is AuthResult.Error -> {
                    isLoading = false
                    errorMessage = result.message
                    showErrorMessage = true
                }
            }
        }
    }

    val scrollState = rememberScrollState()
    
    fun onSignUpClick() {
        fieldErrors = validateFields(
            mapOf(
                NAME to name, EMAIL to email, DATE_OF_BIRTH to dateOfBirth,
                PASSWORD to password, CONFIRM_PASSWORD to confirmPassword,
                PHONE_NUMBER to phoneNumber, ADDRESS to address,
                SUBURB to suburb, STATE to state, POSTCODE to postcode,
                COUNTRY to country, GENDER to gender, EMERGENCY_NAME to emergencyName,
                EMERGENCY_EMAIL to emergencyEmail, EMERGENCY_RELATION to emergencyRelation,
                EMERGENCY_PHONE_NUMBER to emergencyPhoneNumber
            )
        )

        if (fieldErrors.isNotEmpty()) {
            errorMessage = "Please enter all required fields"; showErrorMessage = true
        } else if (!email.isValidEmail()) {
            updateFieldError(EMAIL, true); errorMessage = "Please enter a valid email address"; showErrorMessage = true
        } else if (emergencyEmail.isNotEmpty() && !emergencyEmail.isValidEmail()) {
            updateFieldError(EMERGENCY_EMAIL, true); errorMessage = "Please enter a valid emergency contact email address"; showErrorMessage = true
        } else if (!phoneNumber.isValidPhoneNumber()) {
            updateFieldError(PHONE_NUMBER, true); errorMessage = "Please enter a valid phone number"; showErrorMessage = true
        } else if (!emergencyPhoneNumber.isValidPhoneNumber()) {
            updateFieldError(EMERGENCY_PHONE_NUMBER, true); errorMessage = "Please enter a valid emergency contact phone number"; showErrorMessage = true
        } else if (password != confirmPassword) {
            passwordsMatchError = true; errorMessage = "Passwords do not match"; showErrorMessage = true
        } else if (calculateAgeFromDateOfBirth(dateOfBirth) == null) {
            updateFieldError(DATE_OF_BIRTH, true); errorMessage = "Age cannot be 0. Please select a valid date of birth"; showErrorMessage = true
        } else {
            handleSignUp(email.trim(), password)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title
        Text(
            text = "Sign Up",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            // ADDED: Color.Black replacement
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 70.dp, bottom = 24.dp)
        )

        Text(
            text = "Your Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            // ADDED: Color.Black replacement
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        FormTextField(
            value = name,
            onValueChange = { name = it; clearFieldError(NAME); showErrorMessage = false },
            label = "Name",
            isError = isFieldError(NAME),
            imeAction = ImeAction.Next
        )

        FormTextField(
            value = email,
            onValueChange = { email = it; clearFieldError(EMAIL); showErrorMessage = false },
            label = "Email",
            isError = isFieldError(EMAIL),
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )
        
        DateField(
            date = dateOfBirth,
            onDateChange = { dateOfBirth = it; clearFieldError(DATE_OF_BIRTH); showErrorMessage = false },
            label = "Date of Birth",
            isError = isFieldError(DATE_OF_BIRTH),
            isEditable = true,
            allowDatesAfterToday = false
        )

        FormTextField(
            value = password,
            onValueChange = { password = it; clearFieldError(PASSWORD); passwordsMatchError = false; showErrorMessage = false },
            label = "Password",
            isError = isFieldError(PASSWORD) || passwordsMatchError,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            imeAction = ImeAction.Next
        )

        FormTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; clearFieldError(CONFIRM_PASSWORD); passwordsMatchError = false; showErrorMessage = false },
            label = "Confirm Password",
            isError = isFieldError(CONFIRM_PASSWORD) || passwordsMatchError,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            imeAction = ImeAction.Next
        )

        FormTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it; clearFieldError(PHONE_NUMBER); showErrorMessage = false },
            label = "Phone Number",
            isError = isFieldError(PHONE_NUMBER),
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        )

        Text(
            text = "Home Address",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            // ADDED: Color.Black replacement
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)
        )

        FormTextField(value = address, onValueChange = { address = it; clearFieldError(ADDRESS); showErrorMessage = false }, label = "Address", isError = isFieldError(ADDRESS), imeAction = ImeAction.Next)
        FormTextField(value = suburb, onValueChange = { suburb = it; clearFieldError(SUBURB); showErrorMessage = false }, label = "Suburb", isError = isFieldError(SUBURB), imeAction = ImeAction.Next)
        FormTextField(value = state, onValueChange = { state = it; clearFieldError(STATE); showErrorMessage = false }, label = "State", isError = isFieldError(STATE), imeAction = ImeAction.Next)
        FormTextField(value = postcode, onValueChange = { postcode = it; clearFieldError(POSTCODE); showErrorMessage = false }, label = "Postcode", isError = isFieldError(POSTCODE), keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
        FormTextField(value = country, onValueChange = { country = it; clearFieldError(COUNTRY); showErrorMessage = false }, label = "Country", isError = isFieldError(COUNTRY), imeAction = ImeAction.Next)

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text = "Gender",
                // ADDED: tint/Color.Black replacement
                color = if (isFieldError(GENDER)) FormColors.errorColor else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val genderOptions = listOf("Male", "Female")

            Column(modifier = Modifier.selectableGroup().fillMaxWidth()) {
                genderOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = gender == option,
                                onClick = { gender = option; clearFieldError(GENDER); showErrorMessage = false },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = gender == option,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = FormColors.green,
                                unselectedColor = if (isFieldError(GENDER)) FormColors.errorColor else FormColors.green
                            )
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            // ADDED: Color.Black replacement
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            if (isFieldError(GENDER)) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 16.dp).background(FormColors.errorColor))
            }
        }

        Text(
            text = "Emergency Contact",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            // ADDED: Color.Black replacement
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        FormTextField(value = emergencyName, onValueChange = { emergencyName = it; clearFieldError(EMERGENCY_NAME); showErrorMessage = false }, label = "Name", isError = isFieldError(EMERGENCY_NAME), imeAction = ImeAction.Next)
        FormTextField(value = emergencyEmail, onValueChange = { emergencyEmail = it; clearFieldError(EMERGENCY_EMAIL); showErrorMessage = false }, label = "Email", isError = isFieldError(EMERGENCY_EMAIL), keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)

        val relationOptions = listOf("Spouse", "Parent", "Other family", "Friend", "Other")
        FormDropdown(
            label = "Relation",
            value = emergencyRelation,
            options = relationOptions,
            onValueChange = { emergencyRelation = it; clearFieldError(EMERGENCY_RELATION); showErrorMessage = false },
            isError = isFieldError(EMERGENCY_RELATION),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        FormTextField(
            value = emergencyPhoneNumber,
            onValueChange = { emergencyPhoneNumber = it; clearFieldError(EMERGENCY_PHONE_NUMBER); showErrorMessage = false },
            label = "Phone Number",
            isError = isFieldError(EMERGENCY_PHONE_NUMBER),
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = { onSignUpClick() }
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = { onSignUpClick() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FormColors.green),
            enabled = !isLoading
        ) {
            if (isLoading) LoadingSpinner() else Text("Signup")
        }

        ErrorMessage(show = showErrorMessage, message = errorMessage)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { onBack() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = FormColors.green)
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
