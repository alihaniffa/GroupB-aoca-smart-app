package org.example.dementia_tester_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.rememberUpdatedState
import org.example.dementia_tester_app.auth.AuthService
import org.example.dementia_tester_app.data.Activity
import org.example.dementia_tester_app.data.ActivityService
import org.example.dementia_tester_app.data.ActivityType
import org.example.dementia_tester_app.data.AttemptSummary
import org.example.dementia_tester_app.data.DatabaseResult
import org.example.dementia_tester_app.data.UserQuizService
import org.example.dementia_tester_app.data.UserQuizType
import org.example.dementia_tester_app.notifications.LocalNotificationManagerAdapter
import org.example.dementia_tester_app.notifications.NotificationManagerProvider
import org.example.dementia_tester_app.notifications.ReminderHelper
import org.example.dementia_tester_app.notifications.ReminderIds
import org.example.dementia_tester_app.notifications.ReminderPolicy
import org.example.dementia_tester_app.notifications.ReminderChannels
import org.example.dementia_tester_app.ui.components.AttemptsListScreen
import org.example.dementia_tester_app.ui.components.FormColors
import org.example.dementia_tester_app.ui.components.QuestionComponent
import org.example.dementia_tester_app.ui.components.LoadingSpinner
import org.example.dementia_tester_app.data.Question
import org.example.dementia_tester_app.ui.components.QuizSummary

/**
 * Introduction screen for the health survey
 */
@Composable
fun IntroductionScreen(
    onBackToDashboard: () -> Unit,
    onStartSurvey: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "About this survey",
                    style = MaterialTheme.typography.titleLarge,
                    color = FormColors.green,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "This health survey is designed to assess various aspects of your lifestyle that may impact cognitive health. It covers three key areas: Diet & Alcohol Intake, Physical Activity, and Social Engagement.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "The survey consists of 11 questions and should take approximately 5-7 minutes to complete. Your responses will help us provide personalized recommendations for maintaining and improving your cognitive health.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "All information provided is confidential and will only be used to support your health journey.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onBackToDashboard,
                modifier = Modifier
                    .height(48.dp)
                    .width(120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Back")
            }

            Button(
                onClick = onStartSurvey,
                modifier = Modifier
                    .height(48.dp)
                    .width(120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FormColors.green,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Start")
            }
        }
    }
}

/**
 * Health Survey screen
 */
@Composable
fun HealthSurvey(
    onBackToDashboard: () -> Unit = {}
) {
    val healthSurveyService = remember {
        UserQuizService(UserQuizType.HealthSurvey)
    }

    val activityService = remember {
        ActivityService()
    }

    val authService = remember {
        AuthService()
    }

    val notificationManager = remember {
        NotificationManagerProvider.getNotificationManager()
    }

    val reminderScheduler = remember {
        LocalNotificationManagerAdapter(notificationManager)
    }

    val reminderHelper = remember {
        ReminderHelper(reminderScheduler)
    }

    // Define constants for the reminder notification
    val reminderMessage =
        "You left your health survey unfinished — please complete it"

    val reminderDelayMillis =
        60 * 1000 * 30L

    val reminderPolicy =
        ReminderPolicy(channel = ReminderChannels.HEALTH)

    // State for survey questions
    var surveyQuestions by remember {
        mutableStateOf<List<Question>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var currentQuestionIndex by remember {
        mutableStateOf(0)
    }

    var showSummary by remember {
        mutableStateOf(false)
    }

    var showIntroduction by remember {
        mutableStateOf(false)
    }

    // State for user attempts
    var userAttempts by remember {
        mutableStateOf<List<AttemptSummary>>(emptyList())
    }

    var showAttemptsList by remember {
        mutableStateOf(false)
    }

    var selectedAttemptNumber by remember {
        mutableStateOf<Int?>(null)
    }

    var isViewingPreviousAttempt by remember {
        mutableStateOf(false)
    }

    // State for user identification and survey attempt tracking
    val userId = remember {
        authService.getCurrentUserId()
    }

    if (userId == null) {
        errorMessage =
            "User not authenticated. Please log in."

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = errorMessage ?: "An error occurred.",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        return
    }

    var attemptNumber by remember {
        mutableStateOf(1)
    }

    var isSurveyActive by remember {
        mutableStateOf(false)
    }

    // Create a unique reminder ID based on userId and attemptNumber
    val reminderId by remember(
        userId,
        attemptNumber
    ) {
        mutableStateOf(
            ReminderIds.healthSurvey(
                "${userId}_${attemptNumber}"
            )
        )
    }

    val scrollState =
        rememberScrollState()

    // Helper function to fetch questions
    val fetchQuestions = {
            callback: (List<Question>) -> Unit ->

        healthSurveyService.fetchSurveyQuestions { result ->

            isLoading = false

            when (result) {
                is DatabaseResult.Success -> {
                    surveyQuestions =
                        result.data.toMutableStateList()

                    errorMessage = null

                    callback(result.data)
                }

                is DatabaseResult.Error -> {
                    errorMessage =
                        result.message

                    callback(emptyList())
                }
            }
        }
    }

    // Helper function to get the latest attempt number
    val getLatestAttemptNumber = {
            callback: (Int) -> Unit ->

        healthSurveyService.getLatestAttemptNumber(
            userId
        ) { result ->

            when (result) {
                is DatabaseResult.Success -> {
                    attemptNumber =
                        result.data

                    callback(result.data)
                }

                is DatabaseResult.Error -> {
                    println(
                        "Error getting latest attempt number: ${result.message}"
                    )

                    attemptNumber = 1

                    callback(1)
                }
            }
        }
    }

    // Helper function to refresh user attempts
    val refreshUserAttempts = {
            callback: (List<AttemptSummary>) -> Unit ->

        healthSurveyService.getUserAttempts(
            userId
        ) { result ->

            when (result) {
                is DatabaseResult.Success -> {
                    userAttempts =
                        result.data

                    callback(result.data)
                }

                is DatabaseResult.Error -> {
                    println(
                        "Error refreshing user attempts: ${result.message}"
                    )

                    callback(emptyList())
                }
            }
        }
    }

    // Helper function to reset question answers
    val resetQuestionAnswers = {
        surveyQuestions.forEach {
            it.selectedAnswer = null
            it.selectedAnswers.clear()
        }
    }

    // Function to finalize the current attempt
    val finalizeCurrentAttempt = {
        if (
            isSurveyActive &&
            !isLoading &&
            surveyQuestions.isNotEmpty()
        ) {
            healthSurveyService.finalizeAttempt(
                userId,
                attemptNumber
            ) { result ->

                when (result) {
                    is DatabaseResult.Success -> {
                        attemptNumber =
                            result.data

                        println(
                            "Attempt finalized. Next attempt will be: $attemptNumber"
                        )
                    }

                    is DatabaseResult.Error -> {
                        println(
                            "Error finalizing attempt: ${result.message}"
                        )
                    }
                }

                isSurveyActive = false
            }
        }
    }

    // Function to mark the current attempt as incomplete
    val markCurrentAttemptIncomplete = {
        if (
            isSurveyActive &&
            !isLoading
        ) {
            healthSurveyService.markAttemptIncomplete(
                userId,
                attemptNumber
            ) { result ->

                when (result) {
                    is DatabaseResult.Success ->
                        println(
                            "Attempt $attemptNumber marked as incomplete"
                        )

                    is DatabaseResult.Error ->
                        println(
                            "Error marking attempt incomplete: ${result.message}"
                        )
                }

                isSurveyActive = false
            }
        }
    }

    // Helper function to load attempt details
    val loadAttemptDetails = {
            attemptNum: Int,
            callback: (Boolean) -> Unit ->

        healthSurveyService.getAttemptDetails(
            userId,
            attemptNum
        ) { result ->

            when (result) {
                is DatabaseResult.Success -> {
                    surveyQuestions =
                        result.data.toMutableStateList()

                    callback(true)
                }

                is DatabaseResult.Error -> {
                    println(
                        "Error loading attempt details: ${result.message}"
                    )

                    callback(false)
                }
            }
        }
    }

    // Function to mark the current attempt as complete
    val markAttemptComplete = {
        if (
            isSurveyActive &&
            !isLoading
        ) {
            healthSurveyService.markAttemptComplete(
                userId,
                attemptNumber
            ) { result ->

                when (result) {
                    is DatabaseResult.Success -> {
                        println(
                            "Attempt $attemptNumber marked as complete"
                        )
                    }

                    is DatabaseResult.Error -> {
                        println(
                            "Error marking attempt as complete: ${result.message}"
                        )
                    }
                }
            }
        }
    }

    // Use lifecycle observer to handle scheduling when app goes to background
    val lifecycleOwner =
        LocalLifecycleOwner.current

    val isActiveState =
        rememberUpdatedState(isSurveyActive)

    val showSummaryState =
        rememberUpdatedState(showSummary)

    val reminderIdState =
        rememberUpdatedState(reminderId)

    val attemptNumberState =
        rememberUpdatedState(attemptNumber)

    val userIdState =
        rememberUpdatedState(userId)

    DisposableEffect(lifecycleOwner) {

        val observer =
            LifecycleEventObserver { _, event ->

                println(
                    "HealthSurvey: Lifecycle event: $event"
                )

                if (event == Lifecycle.Event.ON_STOP) {

                    println(
                        "HealthSurvey: ON_STOP event triggered"
                    )

                    println(
                        "HealthSurvey: isSurveyActive=${isActiveState.value}, showSummary=${showSummaryState.value}"
                    )

                    if (
                        isActiveState.value &&
                        !showSummaryState.value
                    ) {
                        println(
                            "HealthSurvey: Scheduling notification with id=${reminderIdState.value}"
                        )

                        val success =
                            reminderHelper.upsertIn(
                                id =
                                    reminderIdState.value,
                                message =
                                    reminderMessage,
                                delayMillis =
                                    reminderDelayMillis,
                                policy =
                                    reminderPolicy
                            )

                        if (success) {
                            println(
                                "HealthSurvey: Notification scheduled successfully from lifecycle observer"
                            )
                        } else {
                            println(
                                "HealthSurvey: Failed to schedule notification from lifecycle observer"
                            )
                        }

                        healthSurveyService
                            .markAttemptIncomplete(
                                userIdState.value ?: "",
                                attemptNumberState.value
                            ) { res ->

                                when (res) {
                                    is DatabaseResult.Success ->
                                        println(
                                            "HealthSurvey: Attempt ${attemptNumberState.value} marked incomplete from lifecycle"
                                        )

                                    is DatabaseResult.Error ->
                                        println(
                                            "HealthSurvey: Failed to mark attempt incomplete from lifecycle: ${res.message}"
                                        )
                                }
                            }

                    } else {
                        println(
                            "HealthSurvey: Not scheduling notification - survey not active or summary shown"
                        )
                    }
                }
            }

        lifecycleOwner.lifecycle
            .addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle
                .removeObserver(observer)
        }
    }

    // Fetch questions, get latest attempt number, and check for previous attempts
    LaunchedEffect(Unit) {

        healthSurveyService.hasUserAttempts(
            userId
        ) { hasAttemptsResult ->

            when (hasAttemptsResult) {

                is DatabaseResult.Success -> {

                    val hasAttempts =
                        hasAttemptsResult.data

                    if (hasAttempts) {

                        refreshUserAttempts { _ ->

                            showAttemptsList = true

                            getLatestAttemptNumber { _ ->
                                fetchQuestions { _ -> }
                            }
                        }

                    } else {

                        showIntroduction = true

                        getLatestAttemptNumber { _ ->
                            fetchQuestions { _ -> }
                        }
                    }
                }

                is DatabaseResult.Error -> {
                    println(
                        "Error checking for user attempts: ${hasAttemptsResult.message}"
                    )

                    showIntroduction = true
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .run {
                if (
                    !showSummary &&
                    !showAttemptsList
                ) {
                    this.verticalScroll(scrollState)
                } else {
                    this
                }
            }
            .padding(
                top = 24.dp,
                bottom = 8.dp
            )
    ) {

        if (isLoading) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingSpinner()
            }

        } else if (errorMessage != null) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text =
                            "Error loading questions",
                        style =
                            MaterialTheme.typography.headlineSmall,
                        color = Color.Red
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            errorMessage
                                ?: "Unknown error",
                        style =
                            MaterialTheme.typography.bodyMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Button(
                        onClick =
                            onBackToDashboard,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    FormColors.green,
                                contentColor =
                                    MaterialTheme.colorScheme.surface
                            )
                    ) {
                        Text(
                            "Back to Dashboard"
                        )
                    }
                }
            }

        } else if (
            surveyQuestions.isEmpty()
        ) {

            Box(
                modifier =
                    Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text =
                            "No questions found",
                        style =
                            MaterialTheme.typography.headlineSmall
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Button(
                        onClick =
                            onBackToDashboard,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    FormColors.green,
                                contentColor =
                                    MaterialTheme.colorScheme.surface
                            )
                    ) {
                        Text(
                            "Back to Dashboard"
                        )
                    }
                }
            }

        } else if (showAttemptsList) {

            AttemptsListScreen(
                attempts = userAttempts,
                onBackToDashboard =
                    onBackToDashboard,
                onStartNewAttempt = {

                    showAttemptsList = false
                    showIntroduction = false
                    isViewingPreviousAttempt =
                        false
                    selectedAttemptNumber =
                        null
                    currentQuestionIndex = 0

                    attemptNumber =
                        (
                                userAttempts
                                    .maxOfOrNull {
                                        it.attemptNumber
                                    }
                                    ?: 0
                                ) + 1

                    resetQuestionAnswers()

                    isSurveyActive = true
                },
                onViewAttempt = {
                        attemptNum ->

                    selectedAttemptNumber =
                        attemptNum

                    isViewingPreviousAttempt =
                        true

                    loadAttemptDetails(
                        attemptNum
                    ) { success ->

                        if (success) {
                            showAttemptsList =
                                false

                            showSummary = true
                        }
                    }
                },
                onContinueAttempt = {
                        attemptNum ->

                    selectedAttemptNumber =
                        attemptNum

                    isViewingPreviousAttempt =
                        false

                    attemptNumber =
                        attemptNum

                    val getNextUnansweredIndex = {
                            callback: (Int) -> Unit ->

                        healthSurveyService
                            .getNextUnansweredQuestionIndex(
                                userId,
                                attemptNum,
                                surveyQuestions.size
                            ) { indexResult ->

                                when (indexResult) {

                                    is DatabaseResult.Success -> {
                                        currentQuestionIndex =
                                            indexResult.data

                                        callback(
                                            indexResult.data
                                        )
                                    }

                                    is DatabaseResult.Error -> {
                                        currentQuestionIndex =
                                            0

                                        callback(0)
                                    }
                                }
                            }
                    }

                    loadAttemptDetails(
                        attemptNum
                    ) { success ->

                        if (success) {
                            getNextUnansweredIndex { _ ->

                                showAttemptsList =
                                    false

                                isSurveyActive =
                                    true
                            }
                        }
                    }
                }
            )

        } else if (showIntroduction) {

            IntroductionScreen(
                onBackToDashboard = {
                    onBackToDashboard()
                },
                onStartSurvey = {
                    showIntroduction = false
                    isSurveyActive = true
                }
            )

        } else if (showSummary) {

            QuizSummary(
                questions =
                    surveyQuestions,
                onBackToDashboard = {

                    if (
                        isViewingPreviousAttempt
                    ) {

                        isViewingPreviousAttempt =
                            false

                        selectedAttemptNumber =
                            null

                        showSummary = false

                        showAttemptsList = true

                        refreshUserAttempts { _ -> }

                    } else {

                        markAttemptComplete()

                        println(
                            "HealthSurvey: Cancelling notification with id=$reminderId"
                        )

                        val success =
                            reminderHelper.cancel(
                                reminderId
                            )

                        if (success) {
                            println(
                                "HealthSurvey: Notification cancelled successfully"
                            )
                        } else {
                            println(
                                "HealthSurvey: Failed to cancel notification"
                            )
                        }

                        finalizeCurrentAttempt()

                        // Log activity
                        activityService.logActivity(
                            Activity(
                                title =
                                    "Health Survey Completed",
                                type =
                                    ActivityType.TEST,
                                description =
                                    "Completed attempt #$attemptNumber"
                            )
                        ) {
                            /* Ignore result */
                        }

                        currentQuestionIndex = 0
                        showSummary = false

                        if (
                            userAttempts.isNotEmpty()
                        ) {

                            showAttemptsList =
                                true

                            refreshUserAttempts { _ -> }

                        } else {

                            showIntroduction =
                                true
                        }

                        resetQuestionAnswers()
                    }
                }
            )

        } else {

            val currentQuestion =
                surveyQuestions[
                    currentQuestionIndex
                ]

            QuestionComponent(
                question =
                    currentQuestion,
                questionNumber =
                    currentQuestionIndex + 1,
                totalQuestions =
                    surveyQuestions.size,
                onAnswerSelected = {
                        answer ->

                    val updatedQuestion =
                        currentQuestion.copy(
                            selectedAnswer =
                                answer
                        )

                    val updatedQuestions =
                        surveyQuestions
                            .toMutableList()

                    updatedQuestions[
                        currentQuestionIndex
                    ] = updatedQuestion

                    surveyQuestions =
                        updatedQuestions
                },
                onMultipleAnswersSelected = {
                        answers ->

                    val updatedQuestion =
                        currentQuestion.copy(
                            selectedAnswers =
                                answers.toMutableList()
                        )

                    val updatedQuestions =
                        surveyQuestions
                            .toMutableList()

                    updatedQuestions[
                        currentQuestionIndex
                    ] = updatedQuestion

                    surveyQuestions =
                        updatedQuestions
                },
                onBackClicked = {

                    if (
                        currentQuestionIndex > 0
                    ) {

                        currentQuestionIndex--

                    } else {

                        if (
                            isViewingPreviousAttempt ||
                            selectedAttemptNumber !=
                            null
                        ) {

                            isViewingPreviousAttempt =
                                false

                            selectedAttemptNumber =
                                null

                            showAttemptsList =
                                true

                        } else {

                            markCurrentAttemptIncomplete()

                            showIntroduction =
                                true
                        }
                    }
                },
                onSaveAndExitClicked = {

                    println(
                        "HealthSurvey: Save & Exit clicked, scheduling notification"
                    )

                    val success =
                        reminderHelper.upsertIn(
                            id =
                                reminderId,
                            message =
                                reminderMessage,
                            delayMillis =
                                reminderDelayMillis,
                            policy =
                                reminderPolicy
                        )

                    if (success) {
                        println(
                            "HealthSurvey: Notification scheduled successfully from Save & Exit button"
                        )
                    } else {
                        println(
                            "HealthSurvey: Failed to schedule notification from Save & Exit button"
                        )
                    }

                    markCurrentAttemptIncomplete()

                    onBackToDashboard()
                },
                onNextClicked = {

                    val question =
                        surveyQuestions[
                            currentQuestionIndex
                        ]

                    healthSurveyService
                        .saveUserAnswer(
                            userId =
                                userId,
                            attemptNumber =
                                attemptNumber,
                            question =
                                question,
                            callback = {
                                    result ->

                                when (result) {

                                    is DatabaseResult.Success -> {

                                        if (
                                            currentQuestionIndex <
                                            surveyQuestions.size - 1
                                        ) {

                                            currentQuestionIndex++

                                        } else {

                                            showSummary =
                                                true

                                            markAttemptComplete()

                                            println(
                                                "HealthSurvey: Cancelling notification with id=$reminderId"
                                            )

                                            val success =
                                                reminderHelper
                                                    .cancel(
                                                        reminderId
                                                    )

                                            if (success) {
                                                println(
                                                    "HealthSurvey: Notification cancelled successfully"
                                                )
                                            } else {
                                                println(
                                                    "HealthSurvey: Failed to cancel notification"
                                                )
                                            }
                                        }
                                    }

                                    is DatabaseResult.Error -> {

                                        println(
                                            "Error saving answer: ${result.message}"
                                        )
                                    }
                                }
                            }
                        )
                }
            )
        }
    }
}