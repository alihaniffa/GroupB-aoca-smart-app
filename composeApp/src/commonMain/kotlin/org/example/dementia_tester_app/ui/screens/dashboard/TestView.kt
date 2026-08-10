package org.example.dementia_tester_app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.dementia_tester_app.auth.AuthService
import org.example.dementia_tester_app.data.*
import org.example.dementia_tester_app.data.UserQuizType.CognitiveAssessment
import org.example.dementia_tester_app.ui.components.AttemptsListScreen
import org.example.dementia_tester_app.ui.components.FormColors
import org.example.dementia_tester_app.ui.components.LoadingSpinner
import org.example.dementia_tester_app.ui.components.QuestionComponent
import org.example.dementia_tester_app.ui.components.QuizSummary

/**
 * Introduction screen for the cognitive assessment/test page.
 */
@Composable
fun CognitiveIntroductionScreen(
    onBackToDashboard: () -> Unit,
    onStartQuestions: () -> Unit
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
                    text = "About this cognitive assessment",
                    style = MaterialTheme.typography.titleLarge,
                    color = FormColors.green,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This cognitive assessment is designed to assess and track various aspects of your mental health.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The assessment consists of 24 questions, assessing the following psychiatric domains:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                val domains = listOf(
                    "Anger",
                    "Anxiety",
                    "Depression",
                    "Dissociation",
                    "Mania",
                    "Memory",
                    "Personality functioning",
                    "Psychosis",
                    "Repetitive thoughts and behaviours",
                    "Sleep problems",
                    "Somatic symptoms",
                    "Substance use",
                    "Suicidal ideation"
                )

                Column(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    domains.forEach { domain ->
                        Text(
                            text = domain,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your responses will help us provide personalized recommendations for maintaining and improving your cognitive health.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "All information provided is confidential and will only be used to support your health journey.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                    containerColor = Color.LightGray,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Back")
            }

            Button(
                onClick = onStartQuestions,
                modifier = Modifier
                    .height(48.dp)
                    .width(120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FormColors.green,
                    contentColor = Color.White
                )
            ) {
                Text("Start test")
            }
        }
    }
}

/**
 * Cognitive assessment page with question cards.
 */
@Composable
fun QuestionsPage(
    userId: String,
    attemptNumber: Int,
    onSaveAndExit: (Map<String, List<String>>) -> Unit,
    onFinish: (Map<String, List<String>>) -> Unit,
    prefilledAnswers: Map<String, List<String>> = emptyMap(),
    startQuestionIndex: Int = 0,
    readOnly: Boolean = false
) {
    val userQuizService = remember {
        UserQuizService(CognitiveAssessment)
    }

    var questions by remember {
        mutableStateOf<List<Question>>(emptyList())
    }

    var currentQuestionIndex by remember {
        mutableStateOf(startQuestionIndex)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val answersMap = remember {
        mutableStateMapOf<String, List<String>>()
    }

    prefilledAnswers.forEach { (questionId, answerIds) ->
        answersMap[questionId] = answerIds
    }

    LaunchedEffect(attemptNumber, userId) {
        userQuizService.getAttemptDetails(
            userId,
            attemptNumber
        ) { result ->
            when (result) {
                is DatabaseResult.Success -> {
                    questions = result.data
                }

                is DatabaseResult.Error -> {
                    errorMessage = result.message
                }
            }
        }
    }

    if (
        errorMessage == null &&
        questions.isEmpty()
    ) {
        LoadingSpinner()
        return
    }

    errorMessage?.let {
        Text(
            text = it,
            color = Color.Red,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }

    if (questions.isEmpty()) {
        return
    }

    val currentQuestion =
        questions.getOrNull(currentQuestionIndex)
            ?: return

    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            QuestionComponent(
                question = currentQuestion,
                questionNumber = currentQuestionIndex + 1,
                totalQuestions = questions.size,

                onAnswerSelected = { answer ->

                    answersMap[
                        currentQuestion.id.toString()
                    ] = listOf(answer)

                    val updated =
                        questions.toMutableList()

                    updated[currentQuestionIndex] =
                        currentQuestion.copy(
                            selectedAnswer = answer
                        )

                    questions = updated
                },

                onBackClicked = {
                    if (currentQuestionIndex > 0) {
                        currentQuestionIndex--
                    }
                },

                onSaveAndExitClicked = {
                    onSaveAndExit(answersMap)
                },

                onNextClicked = {
                    val question =
                        questions[currentQuestionIndex]

                    if (!readOnly) {
                        userQuizService.saveUserAnswer(
                            userId = userId,
                            attemptNumber = attemptNumber,
                            question = question
                        ) { result ->

                            when (result) {
                                is DatabaseResult.Success -> {

                                    if (
                                        currentQuestionIndex <
                                        questions.lastIndex
                                    ) {
                                        currentQuestionIndex++
                                    } else {
                                        onFinish(answersMap)
                                    }
                                }

                                is DatabaseResult.Error -> {
                                    println(
                                        "CognitiveAssessment: Failed to save answer: ${result.message}"
                                    )
                                }
                            }
                        }

                    } else {

                        if (
                            currentQuestionIndex <
                            questions.lastIndex
                        ) {
                            currentQuestionIndex++
                        } else {
                            onFinish(answersMap)
                        }
                    }
                }
            )
        }
    }
}

/**
 * TestView screen to display cognitive assessment introduction,
 * assessment questions/test, and attempt list.
 */
@Composable
fun TestView() {

    val authService =
        remember { AuthService() }

    val userQuizService =
        remember {
            UserQuizService(
                CognitiveAssessment
            )
        }

    val activityService =
        remember {
            ActivityService()
        }

    val userId =
        authService.getCurrentUserId()

    var showQuestionsPage by remember {
        mutableStateOf(false)
    }

    var questionReadOnly by remember {
        mutableStateOf(false)
    }

    var answersMap by remember {
        mutableStateOf(
            mapOf<String, List<String>>()
        )
    }

    var attemptSummaries by remember {
        mutableStateOf<List<AttemptSummary>>(
            emptyList()
        )
    }

    var currentAttemptNumber by remember {
        mutableStateOf<Int?>(null)
    }

    var inProgressAttemptNumber by remember {
        mutableStateOf<Int?>(null)
    }

    var startQuestionIndex by remember {
        mutableStateOf(0)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var showSummary by remember {
        mutableStateOf(false)
    }

    var showCognitiveIntroduction by remember {
        mutableStateOf(false)
    }

    var summaryQuestions by remember {
        mutableStateOf<List<Question>>(
            emptyList()
        )
    }

    LaunchedEffect(userId) {

        if (userId != null) {

            userQuizService
                .getLatestAttemptNumber(
                    userId
                ) { result ->

                    when (result) {
                        is DatabaseResult.Success -> {
                            currentAttemptNumber =
                                result.data
                        }

                        is DatabaseResult.Error -> {
                            currentAttemptNumber = 1

                            errorMessage =
                                "Failed to fetch latest attempt: ${result.message}"
                        }
                    }
                }

            userQuizService
                .getUserAttempts(
                    userId
                ) { result ->

                    when (result) {

                        is DatabaseResult.Success -> {

                            attemptSummaries =
                                result.data

                            isLoading = false

                            if (
                                result.data.isEmpty()
                            ) {
                                showCognitiveIntroduction =
                                    true

                                showQuestionsPage =
                                    false
                            }
                        }

                        is DatabaseResult.Error -> {

                            errorMessage =
                                "Failed to fetch attempts: ${result.message}"

                            isLoading =
                                false
                        }
                    }
                }

        } else {
            errorMessage =
                "User not logged in"
        }
    }

    if (
        showQuestionsPage &&
        userId != null
    ) {
        val attemptToLoad =
            inProgressAttemptNumber
                ?: currentAttemptNumber

        if (attemptToLoad != null) {

            Column {

                Text(
                    text =
                        "Cognitive Assessment",
                    style =
                        MaterialTheme.typography.headlineSmall,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier.padding(
                            bottom = 12.dp
                        )
                )

                QuestionsPage(
                    userId = userId,
                    attemptNumber =
                        attemptToLoad,

                    onSaveAndExit = {
                            savedAnswers ->

                        answersMap =
                            savedAnswers

                        if (!questionReadOnly) {

                            userQuizService
                                .markAttemptIncomplete(
                                    userId,
                                    attemptToLoad
                                ) {
                                    userQuizService
                                        .getUserAttempts(
                                            userId
                                        ) { result ->

                                            if (
                                                result
                                                        is DatabaseResult.Success
                                            ) {
                                                attemptSummaries =
                                                    result.data
                                            }

                                            inProgressAttemptNumber =
                                                attemptToLoad

                                            showQuestionsPage =
                                                false
                                        }
                                }

                        } else {

                            inProgressAttemptNumber =
                                null

                            showQuestionsPage =
                                false
                        }
                    },

                    onFinish = {
                            savedAnswers ->

                        answersMap =
                            savedAnswers

                        if (!questionReadOnly) {

                            userQuizService
                                .finalizeAttempt(
                                    userId,
                                    attemptToLoad
                                ) { result ->

                                    when (result) {

                                        is DatabaseResult.Success -> {

                                            inProgressAttemptNumber =
                                                null

                                            currentAttemptNumber =
                                                result.data

                                            showQuestionsPage =
                                                false

                                            // Log completed assessment activity.
                                            activityService.logActivity(
                                                Activity(
                                                    title =
                                                        "Cognitive Assessment Completed",
                                                    type =
                                                        ActivityType.TEST,
                                                    description =
                                                        "Completed attempt #$attemptToLoad"
                                                )
                                            ) {
                                                /* Ignore result */
                                            }

                                            userQuizService
                                                .getAttemptDetails(
                                                    userId,
                                                    attemptToLoad
                                                ) { details ->

                                                    when (details) {

                                                        is DatabaseResult.Success -> {

                                                            summaryQuestions =
                                                                details.data

                                                            showSummary =
                                                                true
                                                        }

                                                        is DatabaseResult.Error -> {

                                                            showSummary =
                                                                false
                                                        }
                                                    }

                                                    userQuizService
                                                        .getUserAttempts(
                                                            userId
                                                        ) {
                                                                attemptsResult ->

                                                            if (
                                                                attemptsResult
                                                                        is DatabaseResult.Success
                                                            ) {
                                                                attemptSummaries =
                                                                    attemptsResult.data
                                                            }
                                                        }
                                                }
                                        }

                                        is DatabaseResult.Error -> {

                                            inProgressAttemptNumber =
                                                null

                                            showQuestionsPage =
                                                false
                                        }
                                    }
                                }

                        } else {

                            inProgressAttemptNumber =
                                null

                            showQuestionsPage =
                                false
                        }
                    },

                    startQuestionIndex =
                        startQuestionIndex,

                    prefilledAnswers =
                        answersMap,

                    readOnly =
                        questionReadOnly
                )
            }

            return
        }
    }

    if (
        isLoading ||
        currentAttemptNumber == null
    ) {

        LoadingSpinner()

    } else if (
        showCognitiveIntroduction
    ) {

        CognitiveIntroductionScreen(
            onBackToDashboard = {
                // No-op for now
            },

            onStartQuestions = {

                questionReadOnly =
                    false

                startQuestionIndex =
                    0

                answersMap =
                    emptyMap()

                val newAttempt =
                    (
                            attemptSummaries
                                .maxOfOrNull {
                                    it.attemptNumber
                                } ?: 0
                            ) + 1

                inProgressAttemptNumber =
                    newAttempt

                showCognitiveIntroduction =
                    false

                showQuestionsPage =
                    true
            }
        )

    } else if (showSummary) {

        QuizSummary(
            questions =
                summaryQuestions,

            onBackToDashboard = {

                showSummary =
                    false

                userId?.let { uid ->

                    userQuizService
                        .getUserAttempts(
                            uid
                        ) { result ->

                            if (
                                result
                                        is DatabaseResult.Success
                            ) {
                                attemptSummaries =
                                    result.data
                            }
                        }
                }
            }
        )

    } else {

        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text =
                    "Your Assessments",
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 8.dp
                    )
            )

            Text(
                text =
                    "View your assessment results or start a new assessment.",
                style =
                    MaterialTheme.typography.bodyMedium,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(
                        horizontal = 8.dp
                    )
            )

            errorMessage?.let { message ->

                org.example.dementia_tester_app
                    .ui.components.ErrorMessage(
                        show = true,
                        message = message
                    )
            }

            AttemptsListScreen(
                attempts =
                    attemptSummaries,

                onBackToDashboard = {

                    showCognitiveIntroduction =
                        true
                },

                onStartNewAttempt = {

                    questionReadOnly =
                        false

                    startQuestionIndex =
                        0

                    answersMap =
                        emptyMap()

                    val newAttempt =
                        (
                                attemptSummaries
                                    .maxOfOrNull {
                                        it.attemptNumber
                                    } ?: 0
                                ) + 1

                    inProgressAttemptNumber =
                        newAttempt

                    showQuestionsPage =
                        true
                },

                onViewAttempt = {
                        attemptNum ->

                    if (userId != null) {

                        userQuizService
                            .getAttemptDetails(
                                userId,
                                attemptNum
                            ) { result ->

                                when (result) {

                                    is DatabaseResult.Success -> {

                                        summaryQuestions =
                                            result.data

                                        showSummary =
                                            true

                                        showQuestionsPage =
                                            false
                                    }

                                    is DatabaseResult.Error -> {

                                        errorMessage =
                                            "Failed to load attempt: ${result.message}"
                                    }
                                }
                            }
                    }
                },

                onContinueAttempt = {
                        attemptNum ->

                    if (userId != null) {

                        userQuizService
                            .getAttemptDetails(
                                userId,
                                attemptNum
                            ) { result ->

                                when (result) {

                                    is DatabaseResult.Success -> {

                                        val list =
                                            result.data

                                        val map =
                                            list.associate { question ->

                                                question.id
                                                    .toString() to
                                                        (
                                                                question.selectedAnswer
                                                                    ?.let {
                                                                        listOf(it)
                                                                    }
                                                                    ?: emptyList()
                                                                )
                                            }

                                        answersMap =
                                            map

                                        userQuizService
                                            .getNextUnansweredQuestionIndex(
                                                userId,
                                                attemptNum,
                                                list.size
                                            ) { indexResult ->

                                                val index =
                                                    when (indexResult) {

                                                        is DatabaseResult.Success ->
                                                            indexResult.data

                                                        is DatabaseResult.Error ->
                                                            0
                                                    }

                                                startQuestionIndex =
                                                    index

                                                inProgressAttemptNumber =
                                                    attemptNum

                                                questionReadOnly =
                                                    false

                                                showQuestionsPage =
                                                    true
                                            }
                                    }

                                    is DatabaseResult.Error -> {

                                        errorMessage =
                                            "Failed to continue attempt: ${result.message}"
                                    }
                                }
                            }
                    }
                }
            )
        }
    }
}