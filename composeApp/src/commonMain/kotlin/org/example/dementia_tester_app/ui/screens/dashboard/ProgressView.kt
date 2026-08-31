package org.example.dementia_tester_app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.dementia_tester_app.auth.AuthService
import org.example.dementia_tester_app.data.*
import org.example.dementia_tester_app.ui.components.FormColors
import org.example.dementia_tester_app.ui.components.LoadingSpinner
import org.example.dementia_tester_app.ui.components.ProgressSummary
import org.example.dementia_tester_app.ui.components.UserTestResults

/**
 * Progress screen.
 *
 * Normal patient:
 * ProgressView()
 *
 * Caregiver:
 * ProgressView(
 *     targetUserId = selectedPatient.userId,
 *     targetUserName = selectedPatient.name
 * )
 *
 * When a target user is supplied, all progress
 * and recent activity belongs to that patient.
 */
@Composable
fun ProgressView(
    targetUserId: String? = null,
    targetUserName: String? = null
) {

    val tabs = listOf(
        "Assessments",
        "Health Surveys",
        "Mini Games"
    )

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    val authService = remember {
        AuthService()
    }

    /*
     * UID of the account actually logged in.
     */
    val loggedInUserId =
        authService.getCurrentUserId()

    /*
     * UID whose progress should be displayed.
     *
     * Patient:
     * targetUserId == null
     * -> logged-in patient's UID
     *
     * Caregiver:
     * targetUserId != null
     * -> selected patient's UID
     */
    val userId =
        targetUserId
            ?: loggedInUserId

    val isActingOnBehalf =
        targetUserId != null &&
                targetUserId != loggedInUserId

    val progressOwnerName =
        targetUserName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Patient"

    val cognitiveService = remember {
        UserQuizService(
            UserQuizType.CognitiveAssessment
        )
    }

    val healthService = remember {
        UserQuizService(
            UserQuizType.HealthSurvey
        )
    }

    val gameService = remember {
        MiniGameScoresService()
    }

    val activityService = remember {
        ActivityService()
    }

    var assessmentResults by remember {
        mutableStateOf<List<UserResults>>(
            emptyList()
        )
    }

    var healthResults by remember {
        mutableStateOf<List<UserResults>>(
            emptyList()
        )
    }

    var gameResults by remember {
        mutableStateOf<
                Map<GameType, List<GameAttempts>>
                >(
            emptyMap()
        )
    }

    var recentActivities by remember {
        mutableStateOf<List<Activity>>(
            emptyList()
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Reload all assessment, survey and game
     * progress whenever the selected patient
     * changes.
     */
    LaunchedEffect(userId) {

        assessmentResults =
            emptyList()

        healthResults =
            emptyList()

        gameResults =
            emptyMap()

        errorMessage =
            null

        if (userId == null) {

            isLoading =
                false

            errorMessage =
                "Unable to determine user."

            return@LaunchedEffect
        }

        isLoading =
            true

        /*
         * Load cognitive assessment scores
         * for the target user.
         */
        cognitiveService
            .getUserScores(
                userId
            ) { result ->

                when (result) {

                    is DatabaseResult.Success -> {

                        assessmentResults =
                            result.data
                    }

                    is DatabaseResult.Error -> {

                        errorMessage =
                            "Failed to load assessment results: ${result.message}"
                    }
                }

                /*
                 * Load health survey scores
                 * for the same target user.
                 */
                healthService
                    .getUserScores(
                        userId
                    ) { healthResult ->

                        when (healthResult) {

                            is DatabaseResult.Success -> {

                                healthResults =
                                    healthResult.data
                            }

                            is DatabaseResult.Error -> {

                                if (
                                    errorMessage == null
                                ) {

                                    errorMessage =
                                        "Failed to load health survey results: ${healthResult.message}"
                                }
                            }
                        }

                        /*
                         * Load all mini-game attempts
                         * for the target user.
                         */
                        val gamesMap =
                            mutableMapOf<
                                    GameType,
                                    List<GameAttempts>
                                    >()

                        var gamesLoaded =
                            0

                        val totalGames =
                            GameType.entries.size

                        if (
                            totalGames == 0
                        ) {

                            gameResults =
                                emptyMap()

                            isLoading =
                                false
                        }

                        GameType.entries
                            .forEach { type ->

                                gameService
                                    .getUserGameAttempts(
                                        userId,
                                        type
                                    ) { gameResult ->

                                        when (gameResult) {

                                            is DatabaseResult.Success -> {

                                                gamesMap[type] =
                                                    gameResult.data
                                            }

                                            is DatabaseResult.Error -> {

                                                if (
                                                    errorMessage == null
                                                ) {

                                                    errorMessage =
                                                        "Failed to load game results: ${gameResult.message}"
                                                }
                                            }
                                        }

                                        gamesLoaded++

                                        if (
                                            gamesLoaded ==
                                            totalGames
                                        ) {

                                            gameResults =
                                                gamesMap

                                            isLoading =
                                                false
                                        }
                                    }
                            }
                    }
            }
    }

    /*
     * Load recent activity for the user whose
     * progress is currently being displayed.
     *
     * Patient:
     * -> current patient's activities
     *
     * Caregiver:
     * -> selected patient's activities
     */
    LaunchedEffect(userId) {

        recentActivities =
            emptyList()

        if (userId == null) {
            return@LaunchedEffect
        }

        activityService
            .getActivitiesFlowForUser(
                userId
            )
            .collect { activities ->

                recentActivities =
                    activities.take(5)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp
            )
            .verticalScroll(
                rememberScrollState()
            )
    ) {

        /*
         * Caregiver context indicator.
         */
        if (isActingOnBehalf) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 12.dp
                    ),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                    )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {

                    Text(
                        text =
                            "Viewing progress for",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )

                    Text(
                        text =
                            progressOwnerName,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            FormColors.green
                    )
                }
            }
        }

        Text(
            text =
                if (isActingOnBehalf) {
                    "$progressOwnerName's Progress"
                } else {
                    "Progress Analytics"
                },
            fontSize =
                24.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface,
            modifier =
                Modifier.padding(
                    vertical = 16.dp
                )
        )

        /*
         * Summary statistic cards.
         */
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            StatCard(
                label =
                    "Tests",
                value =
                    assessmentResults
                        .flatMap {
                            it.attempts
                        }
                        .size
                        .toString(),
                modifier =
                    Modifier.weight(1f)
            )

            StatCard(
                label =
                    "Surveys",
                value =
                    healthResults
                        .flatMap {
                            it.attempts
                        }
                        .size
                        .toString(),
                modifier =
                    Modifier.weight(1f)
            )

            StatCard(
                label =
                    "Games",
                value =
                    gameResults
                        .values
                        .flatten()
                        .flatMap {
                            it.attempts
                        }
                        .size
                        .toString(),
                modifier =
                    Modifier.weight(1f)
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    24.dp
                )
        )

        /*
         * Recent activity.
         *
         * In normal mode this belongs to the
         * logged-in patient.
         *
         * In caregiver mode this belongs to
         * the selected patient.
         */
        if (
            recentActivities.isNotEmpty()
        ) {

            Text(
                text =
                    if (isActingOnBehalf) {
                        "$progressOwnerName's Recent Activity"
                    } else {
                        "Recent Activity"
                    },
                fontSize =
                    18.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            recentActivities
                .forEach { activity ->

                    RecentActivityItem(
                        activity
                    )
                }

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )
        }

        errorMessage
            ?.let { message ->

                Text(
                    text =
                        message,
                    color =
                        Color.Red,
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    modifier =
                        Modifier.padding(
                            bottom = 12.dp
                        )
                )
            }

        TabRow(
            selectedTabIndex =
                selectedTab,
            containerColor =
                Color.Transparent,
            contentColor =
                FormColors.green,
            divider = {}
        ) {

            tabs.forEachIndexed {
                    index,
                    title ->

                Tab(
                    selected =
                        selectedTab ==
                                index,
                    onClick = {

                        selectedTab =
                            index
                    },
                    text = {

                        Text(
                            text =
                                title,
                            fontWeight =
                                if (
                                    selectedTab ==
                                    index
                                ) {

                                    FontWeight.Bold

                                } else {

                                    FontWeight.Normal
                                },
                            color =
                                if (
                                    selectedTab ==
                                    index
                                ) {

                                    FormColors.green

                                } else {

                                    MaterialTheme
                                        .colorScheme
                                        .onSurface
                                }
                        )
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )

        if (isLoading) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        200.dp
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                LoadingSpinner()
            }

        } else {

            when (
                selectedTab
            ) {

                0 -> {

                    AssessmentTab(
                        assessmentResults
                    )
                }

                1 -> {

                    HealthSurveyTab(
                        healthResults
                    )
                }

                2 -> {

                    GamesTab(
                        gameResults
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(
                    32.dp
                )
        )
    }
}

@Composable
fun RecentActivityItem(
    activity: Activity
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 4.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(
                    androidx.compose
                        .foundation
                        .shape
                        .CircleShape
                )
                .background(
                    FormColors.green
                )
        )

        Spacer(
            modifier =
                Modifier.width(
                    12.dp
                )
        )

        Column {

            Text(
                text =
                    activity.title,
                fontSize =
                    14.sp,
                fontWeight =
                    FontWeight.Medium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )

            Text(
                text =
                    activity
                        .type
                        .value
                        .capitalize(),
                fontSize =
                    12.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier =
            modifier,
        elevation =
            CardDefaults
                .cardElevation(
                    2.dp
                ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                            .copy(
                                alpha =
                                    0.5f
                            )
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    12.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    value,
                fontSize =
                    20.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    FormColors.green
            )

            Text(
                text =
                    label,
                fontSize =
                    12.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
fun AssessmentTab(
    results: List<UserResults>
) {

    val attempts =
        results.flatMap {
            it.attempts
        }

    if (
        attempts.isEmpty()
    ) {

        EmptyState(
            "No assessments completed yet."
        )

    } else {

        ProgressSummary(
            attempts.last()
        )

        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )

        UserTestResults(
            GraphableAttempts(
                results
            )
        )
    }
}

@Composable
fun HealthSurveyTab(
    results: List<UserResults>
) {

    val attempts =
        results.flatMap {
            it.attempts
        }

    if (
        attempts.isEmpty()
    ) {

        EmptyState(
            "No health surveys completed yet."
        )

    } else {

        ProgressSummary(
            attempts.last()
        )

        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )

        UserTestResults(
            GraphableAttempts(
                results
            )
        )
    }
}

@Composable
fun GamesTab(
    gameResults:
    Map<GameType, List<GameAttempts>>
) {

    val allAttempts =
        gameResults
            .values
            .flatten()

    if (
        allAttempts.isEmpty()
    ) {

        EmptyState(
            "No games played yet."
        )

    } else {

        var selectedGame by remember {
            mutableStateOf(
                GameType.COMPLEX_ATTENTION
            )
        }

        ScrollableTabRow(
            selectedTabIndex =
                GameType.entries
                    .indexOf(
                        selectedGame
                    ),
            containerColor =
                Color.Transparent,
            contentColor =
                FormColors.green,
            edgePadding =
                0.dp
        ) {

            GameType.entries
                .forEach { type ->

                    Tab(
                        selected =
                            selectedGame ==
                                    type,
                        onClick = {

                            selectedGame =
                                type
                        },
                        text = {

                            Text(
                                text =
                                    type.name
                                        .replace(
                                            "_",
                                            " "
                                        )
                                        .lowercase()
                                        .capitalize(),
                                color =
                                    if (
                                        selectedGame ==
                                        type
                                    ) {

                                        FormColors.green

                                    } else {

                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                    }
                            )
                        }
                    )
                }
        }

        val specificResults =
            gameResults[
                selectedGame
            ]
                ?: emptyList()

        if (
            specificResults.isEmpty()
        ) {

            EmptyState(
                "No data for this game."
            )

        } else {

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            UserTestResults(
                GraphableAttempts(
                    specificResults
                )
            )
        }
    }
}

@Composable
fun EmptyState(
    message: String
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                150.dp
            ),
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                message,
            color =
                Color.Gray,
            textAlign =
                TextAlign.Center
        )
    }
}

/**
 * Capitalize the first character of a string.
 */
fun String.capitalize(): String {

    return replaceFirstChar {

        if (
            it.isLowerCase()
        ) {

            it.titlecase()

        } else {

            it.toString()
        }
    }
}