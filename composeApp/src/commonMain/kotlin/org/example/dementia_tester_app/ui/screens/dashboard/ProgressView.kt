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

@Composable
fun ProgressView() {
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

    val userId =
        authService.getCurrentUserId()

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
                >(emptyMap())
    }

    val activityService = remember {
        ActivityService()
    }

    var recentActivities by remember {
        mutableStateOf<List<Activity>>(
            emptyList()
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(userId) {
        if (userId == null) {
            return@LaunchedEffect
        }

        isLoading = true

        cognitiveService.getUserScores(
            userId
        ) { result ->

            if (
                result is DatabaseResult.Success
            ) {
                assessmentResults =
                    result.data
            }

            healthService.getUserScores(
                userId
            ) { healthResult ->

                if (
                    healthResult
                            is DatabaseResult.Success
                ) {
                    healthResults =
                        healthResult.data
                }

                // Fetch mini-game results
                val gamesMap =
                    mutableMapOf<
                            GameType,
                            List<GameAttempts>
                            >()

                var gamesLoaded = 0

                val totalGames =
                    GameType.entries.size

                GameType.entries.forEach {
                        type ->

                    gameService
                        .getUserGameAttempts(
                            userId,
                            type
                        ) { gameResult ->

                            if (
                                gameResult
                                        is DatabaseResult.Success
                            ) {
                                gamesMap[type] =
                                    gameResult.data
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

    // Collect the user's activity history.
    LaunchedEffect(userId) {
        if (userId == null) {
            return@LaunchedEffect
        }

        activityService
            .getActivitiesFlow()
            .collect { activities ->

                // Show only the latest five activities.
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
        Text(
            text = "Progress Analytics",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface,
            modifier =
                Modifier.padding(
                    vertical = 16.dp
                )
        )

        // Statistic cards
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Tests",
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
                label = "Surveys",
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
                label = "Games",
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
                Modifier.height(24.dp)
        )

        // Recent activity preview
        if (
            recentActivities.isNotEmpty()
        ) {
            Text(
                text = "Recent Activity",
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            recentActivities.forEach {
                    activity ->

                RecentActivityItem(
                    activity
                )
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
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
                            text = title,
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
                Modifier.height(16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment =
                    Alignment.Center
            ) {
                LoadingSpinner()
            }

        } else {
            when (selectedTab) {
                0 ->
                    AssessmentTab(
                        assessmentResults
                    )

                1 ->
                    HealthSurveyTab(
                        healthResults
                    )

                2 ->
                    GamesTab(
                        gameResults
                    )
            }
        }

        Spacer(
            modifier =
                Modifier.height(32.dp)
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
                    androidx.compose.foundation
                        .shape.CircleShape
                )
                .background(
                    FormColors.green
                )
        )

        Spacer(
            modifier =
                Modifier.width(12.dp)
        )

        Column {
            Text(
                text = activity.title,
                fontSize = 14.sp,
                fontWeight =
                    FontWeight.Medium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )

            Text(
                text =
                    activity.type.value
                        .capitalize(),
                fontSize = 12.sp,
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
        modifier = modifier,
        elevation =
            CardDefaults
                .cardElevation(2.dp),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                            .copy(
                                alpha = 0.5f
                            )
                )
    ) {
        Column(
            modifier =
                Modifier.padding(12.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    FormColors.green
            )

            Text(
                text = label,
                fontSize = 12.sp,
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

    if (attempts.isEmpty()) {
        EmptyState(
            "No assessments completed yet."
        )
    } else {
        ProgressSummary(
            attempts.last()
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        UserTestResults(
            GraphableAttempts(results)
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

    if (attempts.isEmpty()) {
        EmptyState(
            "No health surveys completed yet."
        )
    } else {
        ProgressSummary(
            attempts.last()
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        UserTestResults(
            GraphableAttempts(results)
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

    if (allAttempts.isEmpty()) {
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
            edgePadding = 0.dp
        ) {
            GameType.entries.forEach {
                    type ->

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
            gameResults[selectedGame]
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
                    Modifier.height(16.dp)
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
            .height(150.dp),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Gray,
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
        if (it.isLowerCase()) {
            it.titlecase()
        } else {
            it.toString()
        }
    }
}