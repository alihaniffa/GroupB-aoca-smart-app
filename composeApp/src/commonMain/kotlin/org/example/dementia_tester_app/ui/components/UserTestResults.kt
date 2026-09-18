package org.example.dementia_tester_app.ui.components


import android.annotation.SuppressLint
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import org.example.dementia_tester_app.data.AttemptType
import org.example.dementia_tester_app.data.GameAttempt
import org.example.dementia_tester_app.data.GameResult
import org.example.dementia_tester_app.data.GraphableAttempts
import org.example.dementia_tester_app.data.UserAttempts
import org.example.dementia_tester_app.data.UserResults
import org.example.dementia_tester_app.utils.Entry
import org.example.dementia_tester_app.utils.computeMLPrediction
import kotlin.math.max
import kotlin.math.min

//  ────── Constants ────────────────────────────────────────────────────────────────────────────────────

private object ChartColors {
    val USER_1_LINE = Color(0xFFE57373)
    val USER_2_LINE = Color(0xFF64B5F6)
    val USER_1_POINT = Color(0xFFFF8C00)
    val USER_2_POINT = Color(0xFF2196F3)
    val USER_1_SHADE = Color(0x1AFFA500)
    val USER_2_SHADE = Color(0x1A2196F3)
    val CROSSHAIR_1 = Color(0x99E65100)
    val CROSSHAIR_2 = Color(0x991976D2)
    val AXIS = Color.DarkGray
    val HEADER = Color(0xFF66BB23)
}

private object ChartDims {
    val HEIGHT = 220.dp
    val PADDING = 15.dp
    val AXIS_SPACE = 20.dp
    val POINT_BOX = 8.dp
    const val POINT_RADIUS = 10f
    const val POINT_RADIUS_SEL = 12f
    const val AXIS_STROKE = 3f
    const val LINE_STROKE = 4f
    const val CROSSHAIR_STROKE = 2.5f
    const val LABEL_OFFSET = 20f
    val X_LABEL_TOP = 4.dp
    val X_LABEL_FONT = 12.sp
    val TITLE_FONT = 18.sp
    val TITLE_PADDING = 8.dp
}

private object TextSizes {
    val CARD_TITLE = 16.sp
    val OVERVIEW = 16.sp
    val DETAIL = 14.sp
}

//──────── Shared Helpers ────────────────────────────────────────────────────────────────────────────────────────

/**
 * Calculates pixel positions for each score to be plotted on a chart.
 *
 * @param scores List of score values to be plotted
 * @param width Width of the canvas in pixels
 * @param height Height of the canvas in pixels
 * @param minScore Minimum score value for scaling the y-axis
 * @param maxScore Maximum score value for scaling the y-axis
 * @param maxPoints Maximum number of points for scaling the x-axis (defaults to scores.size)
 * @return List of Offset objects representing the pixel positions of each score
 */
private fun calcPoints(
    scores: List<Int>,
    width: Float,
    height: Float,
    minScore: Int,
    maxScore: Int,
    maxPoints: Int = scores.size
): List<Offset> = scores.mapIndexed { i, s ->
    val x = width * i / max(1, maxPoints - 1)
    // FIX: When all scores are equal (minScore == maxScore), the original formula
    // produced y = height (bottom of chart) for every point, because:
    //   y = height - height * (s - minScore) / max(1, 0) = height - 0 = height
    // This made a perfect score like 100/100 appear at the very bottom.
    // Fix: detect the flat-line case and pin all points to the vertical midpoint
    // instead, which is visually unambiguous and clearly not a zero score.
    val y = if (maxScore == minScore) {
        height / 2f
    } else {
        height - height * (s - minScore) / (maxScore - minScore).toFloat()
    }
    Offset(x, y)
}

/**
 * Creates a path for the shaded area beneath a line on a chart.
 *
 * @param points List of points that form the line
 * @param height Height of the canvas in pixels
 * @return Path object representing the shaded area beneath the line
 */
private fun shadedPath(points: List<Offset>, height: Float): Path =
    Path().apply {
        moveTo(0f, height)
        if (points.isNotEmpty()) {
            lineTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, height)
        }
        close()
    }

/**
 * Draws X-axis labels at regular intervals below a chart.
 *
 * @param count Total number of labels to display
 * @param startIndex Starting index for label numbering (default: 0)
 * @param labelStep Interval between displayed labels (default: 1)
 */
@Composable
private fun XAxisLabels(count: Int, startIndex: Int = 0, labelStep: Int = 1) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(count) { i ->
            if (i % labelStep == 0 || i == count - 1) {
                Text(
                    text = (i + startIndex + 1).toString(),
                    fontSize = ChartDims.X_LABEL_FONT,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = ChartDims.X_LABEL_TOP)
                )
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}

/**
 * Filters user attempts based on the selected filter option.
 *
 * @param attempts List of user attempts to filter
 * @param filterOption String indicating which filter to apply ("Last 5 attempts", "Last 10 attempts", or "All attempts")
 * @return Pair containing the filtered list of attempts and the starting index
 */
private fun filterAttempts(
    attempts: List<Any>,
    filterOption: String
): Pair<List<Any>, Int> = when (filterOption) {
    "Last 5 attempts" -> {
        val c = min(5, attempts.size)
        val s = attempts.size - c
        attempts.takeLast(c) to s
    }
    "Last 10 attempts" -> {
        val c = min(10, attempts.size)
        val s = attempts.size - c
        attempts.takeLast(c) to s
    }
    else -> attempts to 0
}

//───── Card Composables ────────────────────────────────────────────────────────────────────────────────────────

/**
 * Displays a card with an overview of test results for a specific attempt.
 *
 * @param attempt The UserAttempts object containing the test results
 * @param attemptNumber The number of the attempt (1-based)
 * @param userName Optional name of the user (for comparison view)
 */
@Composable
private fun ResultsOverviewCard(
    attempt: UserAttempts,
    attemptNumber: Int,
    userName: String? = null
) {
    val title = userName?.let { "$it - Results Overview" } ?: "Results Overview"
    ResultCard(title) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Attempt: $attemptNumber", fontSize = TextSizes.OVERVIEW)
                Text("Total Score: ${attempt.totalScore} / ${attempt.totalQuestions * 4}", fontSize = TextSizes.OVERVIEW)
            }
            Column(Modifier.weight(1f)) {
                Text("Date: ${attempt.lastUpdated}", fontSize = TextSizes.OVERVIEW)
                Text(
                    "NCD Category: ${attempt.ncdCategory.ifEmpty { "Not Categorized" }}",
                    fontSize = TextSizes.OVERVIEW
                )
            }
        }
    }
}

/**
 * Displays a card with cognitive situation scores for a specific attempt.
 *
 * @param attempt The UserAttempts object containing the cognitive scores
 * @param userName Optional name of the user (for comparison view)
 */
@Composable
private fun CognitiveDetailsCard(
    attempt: UserAttempts,
    userName: String? = null
) {
    val title = userName?.let { "$it - Cognitive Details" } ?: "Cognitive Details"
    ResultCard(title) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                with(attempt.cognitiveSituationScores) {
                    Text("Corrective Measures: $correctiveMeasures", fontSize = TextSizes.DETAIL)
                    Text("Decision Making: $decisionMaking", fontSize = TextSizes.DETAIL)
                    Text("Emotional Intelligence: $emotionalIntelligence", fontSize = TextSizes.DETAIL)
                    Text("Memory Tasks: $memoryTasks", fontSize = TextSizes.DETAIL)
                }
            }
            Column(Modifier.weight(1f)) {
                with(attempt.cognitiveSituationScores) {
                    Text("Language Issues: $languageIssues", fontSize = TextSizes.DETAIL)
                    Text("Processing Time: $processingTime", fontSize = TextSizes.DETAIL)
                    Text("Simple Instructions: $simpleInstructions", fontSize = TextSizes.DETAIL)
                    Text("Visual Tasks: $visualTasks", fontSize = TextSizes.DETAIL)
                }
            }
        }
    }
}

/**
 * Displays a card with domain scores for a specific attempt.
 *
 * @param attempt The UserAttempts object containing the domain scores
 * @param userName Optional name of the user (for comparison view)
 */
@Composable
private fun DomainDetailsCard(
    attempt: UserAttempts,
    userName: String? = null
) {
    val title = userName?.let { "$it - Domain Details" } ?: "Domain Details"
    ResultCard(title) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                with(attempt.domainScores) {
                    Text("Complex Attention: $complexAttentions", fontSize = TextSizes.DETAIL)
                    Text("Executive Function: $executiveFunction", fontSize = TextSizes.DETAIL)
                    Text("Language: $language", fontSize = TextSizes.DETAIL)
                }
            }
            Column(Modifier.weight(1f)) {
                with(attempt.domainScores) {
                    Text("Learning & Memory: $learningAndMemory", fontSize = TextSizes.DETAIL)
                    Text("Perceptual Motor: $perceptualMotor", fontSize = TextSizes.DETAIL)
                    Text("Social Cognition: $socialCognition", fontSize = TextSizes.DETAIL)
                }
            }
        }
    }
}

/**
 * Displays a card with machine learning predictions based on historical test scores.
 *
 * @param allAttempts The complete list of user attempts used for prediction
 * @param userName Optional name of the user (for comparison view)
 */
@Composable
private fun MLPredictionCard(
    allAttempts: List<UserAttempts>,
    userName: String? = null
) {
    val title = userName?.let { "$it - ML Prediction" } ?: "ML Prediction"
    ResultCard(title) {
        Column(Modifier.fillMaxWidth()) {
            val entries = allAttempts.mapIndexed { i, ua -> Entry(i + 1.0, ua.totalScore.toDouble()) }
            val mlText  = computeMLPrediction(entries)
            val (color, rec) = when {
                mlText.contains("Not enough data") -> Color.Gray to "Continue exercises; more data needed."
                mlText.contains("Already at the highest") -> Color(0xFFF44336) to "Schedule specialist evaluation immediately."
                mlText.contains("Not progressing") -> Color(0xFF4CAF50) to "Continue regular cognitive exercises."
                mlText.contains("Next stage") -> {
                    val weeks = mlText.substringAfter("in next ").substringBefore(" weeks").toIntOrNull() ?: 0
                    when {
                        weeks > 24 -> Color(0xFF4CAF50) to "Continue; reassess in 6+ months."
                        weeks > 12 -> Color(0xFFFFC107) to "Increase exercises; follow up in 3 months."
                        else -> Color(0xFFF44336) to "Schedule evaluation within 1 month."
                    }
                }
                else -> Color.Gray to "Consult a healthcare professional."
            }
            Text(mlText, fontSize = TextSizes.OVERVIEW, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(8.dp))
            Text("This prediction uses linear regression on historical scores.", fontSize = TextSizes.OVERVIEW)
            Spacer(Modifier.height(8.dp))
            Text("Recommendation: $rec", fontSize = TextSizes.OVERVIEW, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Reusable card component with a green header for displaying result sections.
 *
 * @param title The title to display in the card header
 * @param content The composable content to display in the card body
 */
@Composable
private fun ResultCard(title: String, content: @Composable () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(ChartColors.HEADER)
                    .padding(8.dp)
            ) {
                Text(title, fontSize = TextSizes.CARD_TITLE, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}

/**
 * Displays detailed information for a selected attempt, including overview, cognitive details,
 * domain details, and ML prediction.
 *
 * @param attempt The UserAttempts object containing the test results
 * @param attemptNumber The number of the attempt (1-based)
 * @param allAttempts The complete list of user attempts used for ML prediction
 */
@Composable
private fun AttemptDetails(attempt: UserAttempts, attemptNumber: Int, allAttempts: List<UserAttempts>) {
    Column(Modifier.fillMaxWidth()) {
        ResultsOverviewCard(attempt, attemptNumber)
        Spacer(Modifier.height(16.dp))
        CognitiveDetailsCard(attempt)
        Spacer(Modifier.height(16.dp))
        DomainDetailsCard(attempt)
        Spacer(Modifier.height(16.dp))
        MLPredictionCard(allAttempts)
    }
}

/**
 * Displays detailed information for a selected attempt in comparison mode,
 * including overview, cognitive details, domain details, and ML prediction with user name.
 *
 * @param attempt The UserAttempts object containing the test results
 * @param attemptNumber The number of the attempt (1-based)
 * @param allAttempts The complete list of user attempts used for ML prediction
 * @param userName The name of the user whose data is being displayed
 */
@Composable
private fun ComparativeAttemptDetails(
    attempt: UserAttempts,
    attemptNumber: Int,
    allAttempts: List<UserAttempts>,
    userName: String
) {
    Column(Modifier.fillMaxWidth()) {
        ResultsOverviewCard(attempt, attemptNumber, userName)
        Spacer(Modifier.height(16.dp))
        CognitiveDetailsCard(attempt, userName)
        Spacer(Modifier.height(16.dp))
        DomainDetailsCard(attempt, userName)
        Spacer(Modifier.height(16.dp))
        MLPredictionCard(allAttempts, userName)
    }
}

/**
 * Displays information for a selected health survey attempt.
 *
 * @param attempt The UserAttempts object containing the health survey results
 * @param attemptNumber The number of the attempt (1-based)
 */
@Composable
private fun HealthSurveyDetails(attempt: UserAttempts, attemptNumber: Int) {
    Column(Modifier.fillMaxWidth()) {
        ResultsOverviewCard(attempt, attemptNumber)
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Displays information for a selected minigame attempt along with some overall statistics for
 * that particular minigame.
 *
 * @param attempt The GameAttempt object containing the game results
 * @param attemptNumber The number of the game attempt (1-based)
 * @param gameResult The overall statistics for that particular game
 */
@Composable
private fun MiniGameAttemptDetails(
    attempt: GameAttempt,
    attemptNumber: Int,
    gameResult: GameResult?,
) {
    Column(Modifier.fillMaxWidth()) {
        ResultCard(title = "Results Overview") {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Attempt: $attemptNumber", fontSize = TextSizes.OVERVIEW)
                    Text("Total Score: ${attempt.score} / 100", fontSize = TextSizes.OVERVIEW)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (gameResult != null) {
            ResultCard(title = "Overall Results for ${gameResult.gameName}") {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Total Attempts: ${gameResult.totalAttempts}", fontSize = TextSizes.OVERVIEW)
                        Text("Last Played: ${gameResult.lastPlayed}", fontSize = TextSizes.OVERVIEW)
                        Text("Average Score: ${gameResult.averageScore}", fontSize = TextSizes.OVERVIEW)
                        Text("Lowest Score: ${gameResult.minScore}", fontSize = TextSizes.OVERVIEW)
                        Text("Highest Score: ${gameResult.maxScore}", fontSize = TextSizes.OVERVIEW)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

//──────── Chart Components ──────────────────────────────────────────────────────────────────

/**
 * Displays a line graph showing total scores for a single user's test or game attempts.
 * Each point on the graph is clickable to view detailed information.
 *
 * @param attempts List of integers representing scores to display in the graph
 * @param selectedIndex Index of the currently selected attempt (or null if none selected)
 * @param onPointSelected Callback function when a point is clicked, receives the index of the selected point
 * @param crosshairVisible Whether to display crosshair lines for the selected point
 * @param startAttemptIndex Starting index for attempt numbering (default: 0)
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ScoreLineGraph(
    attempts: List<Int>,
    selectedIndex: Int?,
    onPointSelected: (Int) -> Unit,
    crosshairVisible: Boolean,
    startAttemptIndex: Int = 0
) {
    val scores = attempts
    val minS = 0
    val highestScore = scores.maxOrNull() ?: 0
    val maxS = max(10, highestScore + max(2, highestScore / 10))
    val labelStep = if (scores.size > 10) 2 else 1

    Column(
        Modifier
            .fillMaxWidth()
            .padding(
                start = ChartDims.PADDING,
                end = ChartDims.PADDING,
                top = ChartDims.AXIS_SPACE,
                bottom = ChartDims.AXIS_SPACE
            )
    ) {
        Text(
            "Total scores",
            fontSize = ChartDims.TITLE_FONT,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = ChartDims.TITLE_PADDING)
        )

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(ChartDims.HEIGHT)
        ) {
            val density = LocalDensity.current
            val pts = calcPoints(
                scores,
                constraints.maxWidth.toFloat(),
                constraints.maxHeight.toFloat(),
                minS, maxS
            )

            Canvas(Modifier.fillMaxSize()) {
                drawPath(shadedPath(pts, size.height), ChartColors.USER_1_SHADE)
                drawLine(ChartColors.AXIS, Offset(0f, size.height), Offset(size.width, size.height), ChartDims.AXIS_STROKE)
                drawLine(ChartColors.AXIS, Offset(0f, 0f), Offset(0f, size.height), ChartDims.AXIS_STROKE)
                pts.windowed(2).forEach { (a, b) ->
                    drawLine(ChartColors.USER_1_LINE, a, b, ChartDims.LINE_STROKE)
                }
                pts.forEachIndexed { i, p ->
                    val sel = (selectedIndex == i)
                    drawCircle(
                        ChartColors.USER_1_POINT,
                        radius = if (sel) ChartDims.POINT_RADIUS_SEL else ChartDims.POINT_RADIUS,
                        center = p
                    )
                }
                selectedIndex?.takeIf { it in pts.indices && crosshairVisible }?.let { i ->
                    val p = pts[i]
                    drawLine(ChartColors.CROSSHAIR_1, Offset(0f, p.y), Offset(size.width, p.y), ChartDims.CROSSHAIR_STROKE)
                    drawLine(ChartColors.CROSSHAIR_1, Offset(p.x, 0f), Offset(p.x, size.height), ChartDims.CROSSHAIR_STROKE)
                }
            }

            pts.forEachIndexed { i, p ->
                val xDp = with(density) { p.x.toDp() }
                val yDp = with(density) { p.y.toDp() }

                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = xDp - ChartDims.POINT_BOX / 2, y = yDp - ChartDims.POINT_BOX / 2)
                        .size(ChartDims.POINT_BOX)
                        .clickable { onPointSelected(i) }
                )

                val xOffset = if (i == scores.lastIndex && p.x / constraints.maxWidth.toFloat() > 0.8f) (-20).dp else 6.dp
                Text(
                    text = scores[i].toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = xDp + xOffset, y = yDp - ChartDims.LABEL_OFFSET.dp)
                )
            }
        }

        XAxisLabels(count = attempts.size, startIndex = startAttemptIndex, labelStep = labelStep)
    }
}

/**
 * Displays a comparative line graph showing total scores for two users' test attempts.
 * Each point on the graph is clickable to view detailed information.
 *
 * @param attempts1 List of UserAttempts objects for the first user
 * @param attempts2 List of UserAttempts objects for the second user
 * @param selectedUser Which user's point is selected (1 for user1, 2 for user2, null for none)
 * @param selectedIndex Index of the currently selected attempt (or null if none selected)
 * @param onPointSelected Callback function when a point is clicked, receives the user (1 or 2) and index
 * @param crosshairVisible Whether to display crosshair lines for the selected point
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ComparativeScoreLineGraph(
    attempts1: List<UserAttempts>,
    attempts2: List<UserAttempts>,
    selectedUser: Int?,
    selectedIndex: Int?,
    onPointSelected: (Int, Int) -> Unit,
    crosshairVisible: Boolean
) {
    val s1 = attempts1.map { it.totalScore }
    val s2 = attempts2.map { it.totalScore }
    val minS = 0
    val highestScore = max(
        s1.maxOrNull() ?: 0,
        s2.maxOrNull() ?: 0
    )
    val maxS = max(10, highestScore + max(2, highestScore / 10))
    val maxPts = max(s1.size, s2.size)
    val labelStep = if (maxPts > 10) 2 else 1

    Column(
        Modifier
            .fillMaxWidth()
            .padding(
                start = ChartDims.PADDING,
                end = ChartDims.PADDING,
                top = ChartDims.AXIS_SPACE,
                bottom = ChartDims.AXIS_SPACE
            )
    ) {
        Text(
            "Total scores comparison",
            fontSize = ChartDims.TITLE_FONT,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = ChartDims.TITLE_PADDING)
        )

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(ChartDims.HEIGHT)
        ) {
            val density = LocalDensity.current
            val pts1 = calcPoints(s1, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), minS, maxS, maxPts)
            val pts2 = calcPoints(s2, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), minS, maxS, maxPts)

            Canvas(Modifier.fillMaxSize()) {
                drawPath(shadedPath(pts1, size.height), ChartColors.USER_1_SHADE)
                drawPath(shadedPath(pts2, size.height), ChartColors.USER_2_SHADE)
                drawLine(ChartColors.AXIS, Offset(0f, size.height), Offset(size.width, size.height), ChartDims.AXIS_STROKE)
                drawLine(ChartColors.AXIS, Offset(0f, 0f), Offset(0f, size.height), ChartDims.AXIS_STROKE)

                pts1.windowed(2).forEach { (a, b) ->
                    drawLine(ChartColors.USER_1_LINE, a, b, ChartDims.LINE_STROKE)
                }
                pts2.windowed(2).forEach { (a, b) ->
                    drawLine(ChartColors.USER_2_LINE, a, b, ChartDims.LINE_STROKE)
                }

                pts1.forEachIndexed { i, p ->
                    val sel = (selectedUser == 1 && selectedIndex == i)
                    drawCircle(
                        ChartColors.USER_1_POINT,
                        radius = if (sel) ChartDims.POINT_RADIUS_SEL else ChartDims.POINT_RADIUS,
                        center = p
                    )
                }
                pts2.forEachIndexed { i, p ->
                    val sel = (selectedUser == 2 && selectedIndex == i)
                    drawCircle(
                        ChartColors.USER_2_POINT,
                        radius = if (sel) ChartDims.POINT_RADIUS_SEL else ChartDims.POINT_RADIUS,
                        center = p
                    )
                }

                selectedUser?.takeIf { crosshairVisible && selectedIndex != null }?.let { u ->
                    val pts = if (u == 1) pts1 else pts2
                    pts.getOrNull(selectedIndex!!)?.let { p ->
                        val c = if (u == 1) ChartColors.CROSSHAIR_1 else ChartColors.CROSSHAIR_2
                        drawLine(c, Offset(0f, p.y), Offset(size.width, p.y), ChartDims.CROSSHAIR_STROKE)
                        drawLine(c, Offset(p.x, 0f), Offset(p.x, size.height), ChartDims.CROSSHAIR_STROKE)
                    }
                }
            }

            pts1.forEachIndexed { i, p ->
                val xDp = with(density) { p.x.toDp() }
                val yDp = with(density) { p.y.toDp() }

                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = xDp - ChartDims.POINT_BOX / 2, y = yDp - ChartDims.POINT_BOX / 2)
                        .size(ChartDims.POINT_BOX)
                        .clickable { onPointSelected(1, i) }
                )
                val xOffset = if (i == s1.lastIndex && p.x / constraints.maxWidth.toFloat() > 0.8f) (-20).dp else 6.dp
                Text(
                    text = s1[i].toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChartColors.USER_1_LINE,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = xDp + xOffset, y = yDp - ChartDims.LABEL_OFFSET.dp)
                )
            }

            pts2.forEachIndexed { i, p ->
                val xDp = with(density) { p.x.toDp() }
                val yDp = with(density) { p.y.toDp() }

                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = xDp - ChartDims.POINT_BOX / 2, y = yDp - ChartDims.POINT_BOX / 2)
                        .size(ChartDims.POINT_BOX)
                        .clickable { onPointSelected(2, i) }
                )
                val xOffset = if (i == s2.lastIndex && p.x / constraints.maxWidth.toFloat() > 0.8f) (-20).dp else 6.dp
                Text(
                    text = s2[i].toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChartColors.USER_2_LINE,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = xDp + xOffset, y = yDp - ChartDims.LABEL_OFFSET.dp)
                )
            }
        }

        XAxisLabels(count = maxPts, labelStep = labelStep)
    }
}

//────── Main Container ────────────────────────────────────────────────────────────────────────────────────────

/**
 * Main component for displaying user test results with interactive charts and detailed information.
 * Can display results for a single user or compare results between two users.
 *
 * @param results List of GraphableAttempts objects containing results for the first user
 * @param results2 Optional list of UserResults objects for the second user (for comparison mode)
 * @param user1Name Optional name of the first user (required for comparison mode)
 * @param user2Name Optional name of the second user (required for comparison mode)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTestResults(
    results: GraphableAttempts,
    results2: List<UserResults>? = null,
    user1Name: String? = null,
    user2Name: String? = null,
) {
    val isComparison = results2 != null && user1Name != null && user2Name != null
    val all1 = results.attempts
    val all2 = if (isComparison) results2!!.flatMap { it.attempts } else emptyList()
    if (all1.isEmpty() && (!isComparison || all2.isEmpty())) return

    var selUser by remember { mutableStateOf<Int?>(null) }
    var selIdx  by remember { mutableStateOf<Int?>(null) }
    var crossVis by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var filterOpt by remember { mutableStateOf("All attempts") }

    val options = listOf("Last 5 attempts", "Last 10 attempts", "All attempts")
    val info1 = remember(filterOpt, all1) { filterAttempts(all1, filterOpt) }
    val info2 = remember(filterOpt, all2) { filterAttempts(all2, filterOpt) }

    val f1 = info1.first
    val s1 = info1.second
    val f2 = info2.first
    val s2 = info2.second

    if (isComparison) {
        selIdx?.takeIf { idx ->
            val rel = if (selUser == 1) f1 else f2
            idx >= rel.size
        }?.let { selIdx = null; selUser = null }
    } else {
        selIdx?.takeIf { it >= f1.size }?.let { selIdx = null }
        selUser = if (selIdx != null) 1 else null
    }

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), Arrangement.End) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.width(200.dp).padding(bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = filterOpt,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                    },
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            BorderStroke(2.dp, Color.Gray),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    options.forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = opt,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = { filterOpt = opt; expanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            }
        }

        OutlinedCard(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
            if (isComparison) {
                ComparativeScoreLineGraph(
                    attempts1 = f1 as List<UserAttempts>,
                    attempts2 = f2 as List<UserAttempts>,
                    selectedUser = selUser,
                    selectedIndex = selIdx,
                    onPointSelected = { u, i ->
                        if (selUser == u && selIdx == i) {
                            selUser = null; selIdx = null; crossVis = false
                        } else {
                            selUser = u; selIdx = i; crossVis = true
                        }
                    },
                    crosshairVisible = crossVis
                )
            } else {
                ScoreLineGraph(
                    attempts = results.getFilteredScores(f1),
                    selectedIndex = selIdx,
                    onPointSelected = { i ->
                        if (selIdx == i) {
                            selIdx = null; selUser = null; crossVis = false
                        } else {
                            selIdx = i; selUser = 1; crossVis = true
                        }
                    },
                    crosshairVisible = crossVis,
                    startAttemptIndex = s1
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (selIdx != null && selUser != null) {
            var all = if (selUser == 1) all1 else all2
            val start = if (selUser == 1) s1 else s2
            val actual = selIdx!! + start
            if (actual in all.indices) {
                if (results.type == AttemptType.COGNITIVE_ASSESSMENT) {
                    all = all as List<UserAttempts>
                    if (isComparison) {
                        ComparativeAttemptDetails(
                            all[actual], actual + 1, all,
                            if (selUser == 1) user1Name!! else user2Name!!
                        )
                    } else {
                        AttemptDetails(attempt = all[actual], attemptNumber = actual + 1, allAttempts = all)
                    }
                }
                if (results.type == AttemptType.HEALTH_SURVEY) {
                    all = all as List<UserAttempts>
                    HealthSurveyDetails(attempt = all[actual], attemptNumber = actual + 1)
                }
                if (results.type == AttemptType.MINIGAME) {
                    all = all as List<GameAttempt>
                    MiniGameAttemptDetails(attempt = all[actual], attemptNumber = actual + 1, results.gameResult)
                }
            }
        } else {
            OutlinedCard(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Text(
                    "Click on a point in the graph to view details",
                    fontSize = TextSizes.DETAIL,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                )
            }
        }
    }
}

