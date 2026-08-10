package org.example.dementia_tester_app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.dementia_tester_app.auth.AuthService
import org.example.dementia_tester_app.data.Activity
import org.example.dementia_tester_app.data.ActivityService
import org.example.dementia_tester_app.data.ActivityType
import org.example.dementia_tester_app.data.GameType
import org.example.dementia_tester_app.data.MiniGameScoresService
import kotlin.random.Random

@Composable
fun Triangle(colour: String) {
    Canvas(
        modifier = Modifier.size(100.dp)
    ) {
        val path = Path().apply {
            moveTo(size.width / 2, 0f)
            lineTo(0f, size.height)
            lineTo(size.width, size.height)
            close()
        }

        var c = Color.Red

        if (colour == "Red") {
            c = Color.Red
        }

        if (colour == "Blue") {
            c = Color.Blue
        }

        if (colour == "Green") {
            c = Color.Green
        }

        if (colour == "Yellow") {
            c = Color.Yellow
        }

        drawPath(
            path = path,
            color = c
        )
    }
}

@Composable
fun Square(colour: String) {
    Canvas(
        modifier = Modifier.size(100.dp)
    ) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, size.height)
            lineTo(size.width, size.height)
            lineTo(size.width, 0f)
            lineTo(0f, 0f)
            close()
        }

        var c = Color.Red

        if (colour == "Red") {
            c = Color.Red
        }

        if (colour == "Blue") {
            c = Color.Blue
        }

        if (colour == "Green") {
            c = Color.Green
        }

        if (colour == "Yellow") {
            c = Color.Yellow
        }

        drawPath(
            path = path,
            color = c
        )
    }
}

@Composable
fun Circle(colour: String) {
    Canvas(
        modifier = Modifier.size(100.dp)
    ) {
        var c = Color.Red

        if (colour == "Red") {
            c = Color.Red
        }

        if (colour == "Blue") {
            c = Color.Blue
        }

        if (colour == "Green") {
            c = Color.Green
        }

        if (colour == "Yellow") {
            c = Color.Yellow
        }

        drawCircle(c)
    }
}

@Composable
fun TaskSwitch(
    onReturn: () -> Unit
) {
    var showbox by remember {
        mutableStateOf(false)
    }

    var shape by remember {
        mutableStateOf("square")
    }

    var wrongshape by remember {
        mutableStateOf("triangle")
    }

    var colour by remember {
        mutableStateOf("red")
    }

    var wrongcolour by remember {
        mutableStateOf("blue")
    }

    var match by remember {
        mutableStateOf("colour")
    }

    var timeleft by remember {
        mutableStateOf(30)
    }

    var score by remember {
        mutableStateOf(0)
    }

    var shapenumber by remember {
        mutableStateOf(0)
    }

    var correctchoice by remember {
        mutableStateOf("Correct")
    }

    var incorrectchoice by remember {
        mutableStateOf("Correct")
    }

    var order by remember {
        mutableStateOf(true)
    }

    val authService = remember {
        AuthService()
    }

    // Prevent submit() from firing twice.
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
                GameType.EXECUTIVE_FUNCTION,
                score
            ) {
                // Ignore result
            }

            activityService.logActivity(
                Activity(
                    title = "Game Played: Task Switch",
                    type = ActivityType.GAME,
                    description =
                        "Scored $score in Executive Function"
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

    // Run the countdown once.
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

    // Generate the next shape and colour combination.
    LaunchedEffect(shapenumber) {
        val shapes =
            arrayOf(
                "Square",
                "Circle",
                "Triangle"
            )

        shapes.shuffle()

        val colours =
            arrayOf(
                "Red",
                "Blue",
                "Green",
                "Yellow"
            )

        colours.shuffle()

        shape = shapes[0]
        wrongshape = shapes[1]

        colour = colours[0]
        wrongcolour = colours[1]

        match =
            arrayOf(
                "shape",
                "colour"
            ).random()

        if (match == "shape") {
            correctchoice = shape
            incorrectchoice = wrongshape
        } else {
            correctchoice = colour
            incorrectchoice = wrongcolour
        }

        order = Random.nextBoolean()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row {

            // Time display
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
                modifier =
                    Modifier.weight(1f)
            )

            // Quit button
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

            // Score display
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

        Row(
            modifier =
                Modifier.padding(16.dp)
        ) {
            Text(
                text =
                    "Match by: $match",
                textAlign =
                    TextAlign.Center,
                fontSize = 20.sp,
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        Row(
            horizontalArrangement =
                Arrangement.Center,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            if (shape == "Triangle") {
                Triangle(colour)
            }

            if (shape == "Square") {
                Square(colour)
            }

            if (shape == "Circle") {
                Circle(colour)
            }
        }

        if (order) {

            Row(
                horizontalArrangement =
                    Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Button(
                    onClick = {
                        score += 1
                        shapenumber += 1
                    },
                    modifier =
                        Modifier.size(
                            110.dp,
                            35.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color.DarkGray
                        )
                ) {
                    Text(correctchoice)
                }
            }

            Row(
                horizontalArrangement =
                    Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
            ) {
                Button(
                    onClick = {
                        shapenumber += 1
                    },
                    modifier =
                        Modifier.size(
                            110.dp,
                            35.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color.DarkGray
                        )
                ) {
                    Text(incorrectchoice)
                }
            }

        } else {

            Row(
                horizontalArrangement =
                    Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Button(
                    onClick = {
                        shapenumber += 1
                    },
                    modifier =
                        Modifier.size(
                            110.dp,
                            35.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color.DarkGray
                        )
                ) {
                    Text(incorrectchoice)
                }
            }

            Row(
                horizontalArrangement =
                    Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
            ) {
                Button(
                    onClick = {
                        score += 1
                        shapenumber += 1
                    },
                    modifier =
                        Modifier.size(
                            110.dp,
                            35.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color.DarkGray
                        )
                ) {
                    Text(correctchoice)
                }
            }
        }
    }
}