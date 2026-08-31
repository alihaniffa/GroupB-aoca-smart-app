package org.example.dementia_tester_app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.dementia_tester_app.data.Activity
import org.example.dementia_tester_app.data.ActivityService
import org.example.dementia_tester_app.data.ActivityType
import org.example.dementia_tester_app.data.Reminder

/**
 * A reusable component to display a reminder card.
 *
 * @param reminder the reminder to display in the card
 * @param onToggle handles when the toggle switch is clicked
 * @param onDelete handles deleting the reminder
 */
@Composable
fun ReminderCard(
    reminder: Reminder,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    val activityService = remember { ActivityService() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = reminder.taskName ?: "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Time: ${reminder.taskTime}",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    modifier = Modifier.padding(12.dp),
                    checked = reminder.taskActive,
                    onCheckedChange = { newValue ->
                        onToggle(reminder.id!!, newValue)

                        if (newValue) {
                            activityService.logActivity(
                                Activity(
                                    title = "Reminder Completed",
                                    type = ActivityType.REMINDER,
                                    description = reminder.taskName ?: ""
                                )
                            ) {
                                /* Ignore result */
                            }
                        }
                    }
                )

                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clickable {
                            onDelete(reminder.id!!)
                        }
                        .padding(12.dp)
                )
            }
        }
    }
}

// previous code
/*
package org.example.dementia_tester_app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.dementia_tester_app.data.Activity
import org.example.dementia_tester_app.data.ActivityService
import org.example.dementia_tester_app.data.Reminder

/**
 * A reusable component to display a reminder card.
 *
 * @param reminder the reminder to display in the card
 * @param onToggle handles when the toggle switch is clicked
 * @param onDelete handles deleting the reminder
 */
@Composable
fun ReminderCard(
    reminder: Reminder,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    val activityService = remember { ActivityService() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Applied change
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = reminder.taskName ?: "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface // Applied change
                )
                Text(
                    text = "Time: ${reminder.taskTime}",
                    color = MaterialTheme.colorScheme.onSurface // Applied change
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    modifier = Modifier.padding(12.dp),
                    checked = reminder.taskActive,
                    onCheckedChange = { newValue ->
                        onToggle(reminder.id!!, newValue)
                        if (newValue) {
                            activityService.logActivity(
                                Activity(
                                    title = "Reminder Completed",
                                    type = "reminder",
                                    description = reminder.taskName ?: ""
                                )
                            ) { /* Ignore result */ }
                        }
                    }
                )
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface, // Applied change
                    modifier = Modifier
                        .clickable { onDelete(reminder.id!!) }
                        .padding(12.dp)
                )
            }
        }
    }
}
*/