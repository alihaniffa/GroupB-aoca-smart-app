package org.example.dementia_tester_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.delay
import org.example.dementia_tester_app.auth.AuthService
import org.example.dementia_tester_app.data.Activity
import org.example.dementia_tester_app.data.ActivityService
import org.example.dementia_tester_app.data.ActivityType
import org.example.dementia_tester_app.data.GameType
import org.example.dementia_tester_app.data.MiniGameScoresService
import kotlin.math.max
import kotlin.random.Random

@Composable
fun FocusFlick(
    onReturn: () -> Unit
) {
    var showbox by remember {
        mutableStateOf(false)
    }

    var timeleft by remember {
        mutableStateOf(30)
    }

    var score by remember {
        mutableStateOf(0)
    }

    var x by remember {
        mutableStateOf(0.5)
    }

    var y by remember {
        mutableStateOf(0.5)
    }

    val authService = remember {
        AuthService()
    }

    /*
     * Prevent submit() from running twice.
     *
     * AlertDialog's onDismissRequest may also run when
     * the dialog is dismissed, so this flag prevents
     * duplicate database entries.
     */
    var submitted by remember {
        mutableStateOf(false)
    }

    fun submit() {
        val scoreService =
            MiniGameScoresService()

        val activityService =
            ActivityService()

        val userId =
            authService.getCurrentUserId()

        userId?.let { uid ->

            scoreService.addUserGameAttempt(
                uid,
                GameType.COMPLEX_ATTENTION,
                score
            ) {
                // Ignore result
            }

            activityService.logActivity(
                Activity(
                    title = "Game Played: Focus Flick",
                    type = ActivityType.GAME,
                    description =
                        "Scored $score in Complex Attention"
                )
            ) {
                // Ignore result
            }
        }
    }

    fun submitOnce() {
        if (!submitted) {
            submitted = true
            submit()
        }
    }

    /*
     * Run the full countdown once.
     */
    LaunchedEffect(Unit) {
        for (i in 30 downTo 0) {
            timeleft = i
            delay(1000)
        }

        showbox = true
    }

    if (showbox) {
        AlertDialog(
            onDismissRequest = {
                submitOnce()
                onReturn()
            },
            title = {
                Text("Submit your score")
            },
            text = {
                Text(
                    "Your score is $score. Submit score?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        submitOnce()
                        onReturn()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    /*
     * When the score changes, choose a new random
     * position for the target.
     */
    LaunchedEffect(score) {
        x = Random.nextDouble()
        y = Random.nextDouble()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row {

            Text(
                text = "Time: ",
                fontSize = 16.sp,
                modifier =
                    Modifier.padding(top = 4.dp)
            )

            Text(
                text = "$timeleft",
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onReturn,
                modifier =
                    Modifier.size(
                        110.dp,
                        35.dp
                    ),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color.Red
                    )
            ) {
                Text("Quit")
            }
        }

        Row {

            Text(
                text = "Score: ",
                fontSize = 16.sp,
                modifier =
                    Modifier.padding(top = 4.dp)
            )

            Text(
                text = "$score",
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
        }

        var size by remember {
            mutableStateOf(Size.Zero)
        }

        Row(
            modifier = Modifier
                .padding(
                    horizontal = 6.dp,
                    vertical = 16.dp
                )
                .fillMaxSize()
                .background(Color.LightGray)
                .onGloballyPositioned { coordinates ->
                    size = coordinates.size.toSize()
                }
        ) {

            val density =
                LocalDensity.current.density

            Column(
                Modifier.padding(
                    start = max(
                        0.0,
                        size.width /
                                density *
                                x -
                                64
                    ).dp,
                    top = max(
                        0.0,
                        size.height /
                                density *
                                y -
                                64
                    ).dp,
                    end = 0.dp,
                    bottom = 0.dp
                )
            ) {

                Button(
                    onClick = {
                        if (timeleft > 0) {
                            score += 1
                        }
                    },
                    modifier =
                        Modifier.size(
                            64.dp,
                            64.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color.Red
                        )
                ) {}
            }
        }
    }
}