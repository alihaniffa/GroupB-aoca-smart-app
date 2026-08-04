package org.example.dementia_tester_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.dementia_tester_app.data.Question

/**
 * A reusable radio button option component for survey questions.
 */
@Composable
fun RadioOption(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelect() }
            .border(
                width = 1.dp,
                color = if (selected) FormColors.green else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                if (selected) FormColors.green.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = FormColors.green,
                unselectedColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) FormColors.green else MaterialTheme.colorScheme.onSurface // Applied change
        )
    }
}

/**
 * A reusable checkbox option component for survey questions that allow multiple selections.
 */
@Composable
fun CheckboxOption(
    text: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .border(
                width = 1.dp,
                color = if (selected) FormColors.green else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(4.dp)
            )
            .background(
                if (selected) FormColors.green.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = FormColors.green,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) FormColors.green else MaterialTheme.colorScheme.onSurface // Applied change
        )
    }
}

/**
 * A reusable question component.
 */
@Composable
fun QuestionComponent(
    question: Question,
    questionNumber: Int,
    totalQuestions: Int,
    onAnswerSelected: (String) -> Unit,
    onMultipleAnswersSelected: (List<String>) -> Unit = {},
    onBackClicked: () -> Unit,
    onSaveAndExitClicked: () -> Unit,
    onNextClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 700.dp)
            .background(MaterialTheme.colorScheme.surface) // Applied change
            .clip(RoundedCornerShape(12.dp))
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Question number and domain:
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Question $questionNumber of $totalQuestions",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface // Applied change
            )
            Text(
                text = question.domain,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = FormColors.green
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar showing overall assessment completion
        val progressFraction = questionNumber.toFloat() / totalQuestions.toFloat()
        val animatedProgress by animateFloatAsState(targetValue = progressFraction)
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = FormColors.green,
            trackColor = FormColors.green.copy(alpha = 0.2f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Question text:
        Text(
            text = question.questionText,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface // Applied change
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Answer options:
        if (question.allowMultipleAnswers) {
            var selectedOptions by remember(question.id) {
                mutableStateOf(question.selectedAnswers.toMutableList())
            }

            question.options.forEach { option ->
                key(option, selectedOptions.contains(option)) {
                    CheckboxOption(
                        text = option,
                        selected = selectedOptions.contains(option),
                        onToggle = {
                            val updatedOptions = selectedOptions.toMutableList()
                            if (updatedOptions.contains(option)) {
                                updatedOptions.remove(option)
                            } else {
                                updatedOptions.add(option)
                            }
                            selectedOptions = updatedOptions
                            onMultipleAnswersSelected(updatedOptions)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        } else {
            var selectedOption by remember(question.id) { mutableStateOf(question.selectedAnswer) }

            question.options.forEach { option ->
                key(option, selectedOption) {
                    RadioOption(
                        text = option,
                        selected = option == selectedOption,
                        onSelect = {
                            selectedOption = option
                            onAnswerSelected(option)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onBackClicked,
                modifier = Modifier
                    .height(48.dp)
                    .width(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Back")
            }

            Button(
                onClick = onSaveAndExitClicked,
                modifier = Modifier
                    .height(48.dp)
                    .width(120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Save & Exit")
            }

            Button(
                onClick = onNextClicked,
                modifier = Modifier
                    .height(48.dp)
                    .width(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FormColors.green,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Next")
            }
        }
    }
}
