package org.example.dementia_tester_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

// Words list
val words = arrayOf(
    "apple",
    "banana",
    "grape",
    "zebra",
    "mountain",
    "volcano",
    "giraffe",
    "umbrella",
    "oxygen",
    "nebula",
    "asteroid",
    "quantum",
    "eclipse",
    "galaxy",
    "hypnosis",
    "jungle",
    "koala",
    "labyrinth",
    "mystery",
    "nocturnal",
    "obsidian",
    "paradox",
    "quasar",
    "resonance",
    "spectrum",
    "telepathy",
    "universe",
    "vortex",
    "wavelength",
    "xenon",
    "yonder",
    "zephyr",
    "satellite",
    "binary",
    "comet",
    "drizzle",
    "echo",
    "fractal",
    "glacier",
    "horizon",
    "isotope",
    "jigsaw",
    "krypton",
    "lunar",
    "mirage"
)

/**
 * Word Recall mini game.
 *
 * Normal patient:
 *
 * WordRecall(
 *     onReturn = { ... }
 * )
 *
 * Caregiver:
 *
 * WordRecall(
 *     onReturn = { ... },
 *     targetUserId = selectedPatient.userId,
 *     targetUserName = selectedPatient.name
 * )
 *
 * If targetUserId is provided, both the score
 * and activity are stored against the selected
 * patient's UID.
 */
@Composable
fun WordRecall(
    onReturn: () -> Unit,
    targetUserId: String? = null,
    targetUserName: String? = null
) {

    var round by remember {
        mutableStateOf(1)
    }

    var score by remember {
        mutableStateOf(0)
    }

    // phase = 0: memorise phase
    // phase = 1: simple addition phase
    // phase = 2: enter words phase
    var phase by remember {
        mutableStateOf(0)
    }

    // Phase timer
    var tick by remember {
        mutableStateOf(10)
    }

    // Snapshot of shuffled words, stable for each round.
    var currentWords by remember {
        mutableStateOf(
            words.toList().shuffled()
        )
    }

    LaunchedEffect(round) {
        currentWords =
            words.toList().shuffled()
    }

    // Addends for the sum
    var a1 by remember {
        mutableStateOf(1)
    }

    var a2 by remember {
        mutableStateOf(1)
    }

    // User output for the sum
    var r1 by remember {
        mutableStateOf("")
    }

    // User output for the words
    var w1 by remember {
        mutableStateOf("")
    }

    var w2 by remember {
        mutableStateOf("")
    }

    var w3 by remember {
        mutableStateOf("")
    }

    var w4 by remember {
        mutableStateOf("")
    }

    var w5 by remember {
        mutableStateOf("")
    }

    var w6 by remember {
        mutableStateOf("")
    }

    var showbox by remember {
        mutableStateOf(false)
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
     * UID that should own the game score
     * and activity.
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

    /*
     * Detect caregiver-on-behalf mode.
     */
    val isActingOnBehalf =
        targetUserId != null &&
                targetUserId != loggedInUserId

    /*
     * Readable patient name for the UI.
     */
    val gameOwnerName =
        targetUserName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Patient"

    // Prevent submit() from firing twice.
    var submitted by remember {
        mutableStateOf(false)
    }

    // Run the countdown once.
    LaunchedEffect(Unit) {
        for (i in 10 downTo 0) {
            tick = i
            delay(1000)
        }
    }

    /*
     * Submit score and activity.
     *
     * Both are saved under userId, which can
     * be either the authenticated patient or
     * the caregiver's selected patient.
     */
    fun submit() {

        val scoreService =
            MiniGameScoresService()

        val activityService =
            ActivityService()

        userId?.let { uid ->

            /*
             * Save the Word Recall score under
             * the correct patient's UID.
             */
            scoreService.addUserGameAttempt(
                uid,
                GameType.LEARNING_AND_MEMORY,
                score
            ) {
                // Ignore result
            }

            /*
             * Log the activity under the same
             * patient's UID.
             *
             * Normal patient:
             * uid = logged-in patient's UID
             *
             * Caregiver:
             * uid = selected patient's UID
             */
            activityService
                .logActivityForUser(
                    userId = uid,
                    activity = Activity(
                        title =
                            "Game Played: Word Recall",
                        type =
                            ActivityType.GAME,
                        description =
                            if (isActingOnBehalf) {
                                "Scored $score in Learning and Memory with caregiver assistance"
                            } else {
                                "Scored $score in Learning and Memory"
                            }
                    )
                ) {
                    /*
                     * Activity logging should not
                     * prevent the score from being
                     * submitted if it fails.
                     */
                }
        }
    }

    /*
     * Prevent duplicate submissions.
     */
    fun submitOnce() {
        if (!submitted) {
            submitted = true
            submit()
        }
    }

    // Phase manager for phase 0 and 1
    LaunchedEffect(tick) {
        if (
            phase == 0 &&
            tick == 0
        ) {
            a1 = arrayOf(
                1, 2, 3, 4, 5,
                6, 7, 8, 9
            ).random()

            a2 = arrayOf(
                1, 2, 3, 4, 5,
                6, 7, 8, 9
            ).random()

            phase = 1
        }
    }

    // Phase manager for phase 2
    fun newRound() {

        val wordInputs =
            listOf(
                w1,
                w2,
                w3,
                w4,
                w5,
                w6
            )

        // Round 1 = 4 words,
        // Round 2 = 5 words,
        // Round 3 = 6 words
        val wordsShown =
            round + 3

        for (i in 0 until wordsShown) {
            if (
                wordInputs[i] ==
                currentWords[i]
            ) {
                score += 1
            }
        }

        r1 = ""
        w1 = ""
        w2 = ""
        w3 = ""
        w4 = ""
        w5 = ""
        w6 = ""

        if (round < 3) {
            round += 1
            phase = 0
            tick = 10
        } else {
            showbox = true
        }
    }

    /*
     * Final score dialog.
     */
    if (showbox) {
        AlertDialog(
            onDismissRequest = {
                submitOnce()
                onReturn()
            },
            title = {
                Text(
                    text =
                        if (isActingOnBehalf) {
                            "Submit score for $gameOwnerName"
                        } else {
                            "Submit your score"
                        }
                )
            },
            text = {
                Text(
                    text =
                        if (isActingOnBehalf) {
                            "The score is $score. Submit this score for $gameOwnerName?"
                        } else {
                            "Your score is $score. Submit score?"
                        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        /*
         * Show the selected patient when
         * caregiver mode is active.
         */
        if (isActingOnBehalf) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = 12.dp
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
                            "Playing on behalf of",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )

                    Text(
                        text =
                            gameOwnerName,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

        Row {

            // Round display
            Text(
                text = "Round: ",
                fontSize = 16.sp,
                modifier =
                    Modifier.padding(
                        top = 4.dp
                    )
            )

            Text(
                text = "$round",
                fontWeight =
                    FontWeight.Bold,
                fontSize = 32.sp,
                textAlign =
                    TextAlign.Center
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
                    Modifier.padding(
                        top = 4.dp
                    )
            )

            Text(
                text = "$score",
                fontWeight =
                    FontWeight.Bold,
                fontSize = 32.sp,
                textAlign =
                    TextAlign.Center
            )
        }

        // Memorise phase
        if (phase == 0) {

            Row {
                Text(
                    text =
                        "Memorise the words. ($tick seconds left)",
                    fontSize = 19.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .secondary,
                    modifier =
                        Modifier.padding(
                            top = 10.dp
                        )
                )
            }

            for (
            i in 1..round + 3
            ) {
                Row(
                    modifier = Modifier
                        .padding(5.dp)
                        .background(
                            Color.LightGray
                        )
                        .fillMaxWidth()
                        .padding(5.dp)
                ) {
                    val word =
                        currentWords[i - 1]

                    Text(
                        text =
                            "$i. $word",
                        fontSize = 26.sp
                    )
                }
            }
        }

        // Addition phase
        if (phase == 1) {

            Row {
                Text(
                    text =
                        "Solve: $a1 + $a2",
                    fontSize = 19.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .secondary,
                    modifier =
                        Modifier.padding(
                            top = 10.dp
                        )
                )
            }

            Row {
                TextField(
                    value = r1,
                    onValueChange = { input ->

                        if (
                            input.matches(
                                Regex("^\\d*$")
                            ) &&
                            input.length < 3
                        ) {
                            r1 = input
                        }
                    },
                    label = {
                        Text("Answer")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp
                        ),
                    keyboardOptions =
                        KeyboardOptions
                            .Default
                            .copy(
                                keyboardType =
                                    KeyboardType
                                        .Number
                            )
                )
            }

            Row(
                modifier =
                    Modifier.padding(
                        top = 10.dp
                    )
            ) {
                if (
                    r1.toIntOrNull() ==
                    a1 + a2
                ) {
                    Button(
                        onClick = {
                            phase = 2
                        },
                        modifier =
                            Modifier.size(
                                110.dp,
                                35.dp
                            ),
                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        Color.Green
                                )
                    ) {
                        Text(
                            "Submit",
                            color =
                                Color.Black
                        )
                    }

                } else {

                    Text(
                        text =
                            "Get the correct answer to submit.",
                        color =
                            Color.Red,
                        fontStyle =
                            FontStyle.Italic
                    )
                }
            }
        }

        // Recall phase
        if (phase == 2) {

            Row {
                Text(
                    text =
                        "Recall the words.",
                    fontSize = 19.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .secondary,
                    modifier =
                        Modifier.padding(
                            top = 10.dp
                        )
                )
            }

            Row {
                TextField(
                    value = w1,
                    onValueChange = {
                        if (it.length < 20) {
                            w1 = it
                        }
                    },
                    label = {
                        Text("Word 1")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp
                        )
                )
            }

            Row {
                TextField(
                    value = w2,
                    onValueChange = {
                        if (it.length < 20) {
                            w2 = it
                        }
                    },
                    label = {
                        Text("Word 2")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp
                        )
                )
            }

            Row {
                TextField(
                    value = w3,
                    onValueChange = {
                        if (it.length < 20) {
                            w3 = it
                        }
                    },
                    label = {
                        Text("Word 3")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp
                        )
                )
            }

            Row {
                TextField(
                    value = w4,
                    onValueChange = {
                        if (it.length < 20) {
                            w4 = it
                        }
                    },
                    label = {
                        Text("Word 4")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp
                        )
                )
            }

            if (round >= 2) {

                Row {
                    TextField(
                        value = w5,
                        onValueChange = {
                            if (
                                it.length < 20
                            ) {
                                w5 = it
                            }
                        },
                        label = {
                            Text("Word 5")
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    top =
                                        10.dp
                                )
                    )
                }
            }

            if (round == 3) {

                Row {
                    TextField(
                        value = w6,
                        onValueChange = {
                            if (
                                it.length < 20
                            ) {
                                w6 = it
                            }
                        },
                        label = {
                            Text("Word 6")
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    top =
                                        10.dp
                                )
                    )
                }
            }

            Row(
                modifier =
                    Modifier.padding(
                        top = 10.dp
                    )
            ) {
                Button(
                    onClick = {
                        newRound()
                    },
                    modifier =
                        Modifier.size(
                            110.dp,
                            35.dp
                        ),
                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    Color.Green
                            )
                ) {
                    Text(
                        "Submit",
                        color =
                            Color.Black
                    )
                }
            }
        }
    }
}