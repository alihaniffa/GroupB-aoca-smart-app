package org.example.dementia_tester_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.dementia_tester_app.data.Activity
import org.example.dementia_tester_app.data.ActivityService
import org.example.dementia_tester_app.data.ActivityType
import org.example.dementia_tester_app.data.DatabaseResult

/**
 * Activities screen
 */
@Composable
fun Activities() {
    val activityService = remember { ActivityService() }
    val activities by activityService
        .getActivitiesFlow()
        .collectAsState(initial = null)

    var todaySummary by remember {
        mutableStateOf<Map<String, Int>?>(null)
    }

    var isLoadingSummary by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        activityService.getTodaySummary { result ->
            if (result is DatabaseResult.Success) {
                todaySummary = result.data
            }

            isLoadingSummary = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Daily Activities",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        DailySummarySection(
            summary = todaySummary,
            isLoading = isLoadingSummary
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        when {
            activities == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            activities!!.isEmpty() -> {
                EmptyState()
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp),
                    contentPadding =
                        PaddingValues(bottom = 16.dp)
                ) {
                    items(activities!!) { activity ->
                        ActivityItem(activity)
                    }
                }
            }
        }
    }
}

@Composable
fun DailySummarySection(
    summary: Map<String, Int>?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Today's Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color =
                        MaterialTheme.colorScheme.primary,
                    trackColor =
                        MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
                    SummaryItem(
                        label = "Total",
                        count = summary?.get("total") ?: 0,
                        icon = Icons.Default.Assessment
                    )

                    SummaryItem(
                        label = "Tasks",
                        count = summary?.get(
                            ActivityType.REMINDER.value
                        ) ?: 0,
                        icon = Icons.Default.CheckCircle
                    )

                    SummaryItem(
                        label = "Tests",
                        count = summary?.get(
                            ActivityType.TEST.value
                        ) ?: 0,
                        icon =
                            Icons.AutoMirrored.Filled.FactCheck
                    )

                    SummaryItem(
                        label = "Games",
                        count = summary?.get(
                            ActivityType.GAME.value
                        ) ?: 0,
                        icon = Icons.Default.Games
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryItem(
    label: String,
    count: Int,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color =
                MaterialTheme.colorScheme.onPrimaryContainer
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActivityItem(
    activity: Activity
) {
    val icon = when (activity.type) {
        ActivityType.GAME ->
            Icons.Default.Games

        ActivityType.TEST ->
            Icons.AutoMirrored.Filled.FactCheck

        ActivityType.REMINDER ->
            Icons.Default.Notifications

        ActivityType.APPOINTMENT ->
            Icons.Default.Event

        ActivityType.OTHER ->
            Icons.Default.History
    }

    val iconColor = when (activity.type) {
        ActivityType.GAME ->
            Color(0xFF4CAF50)

        ActivityType.TEST ->
            Color(0xFF2196F3)

        ActivityType.REMINDER ->
            Color(0xFFFF9800)

        ActivityType.APPOINTMENT ->
            Color(0xFFE91E63)

        ActivityType.OTHER ->
            MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color =
                            iconColor.copy(alpha = 0.1f),
                        shape =
                            RoundedCornerShape(8.dp)
                    ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor
                )
            }

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = activity.title,
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        MaterialTheme.colorScheme.onSurface
                )

                if (activity.description.isNotEmpty()) {
                    Text(
                        text = activity.description,
                        style =
                            MaterialTheme.typography.bodyMedium,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = formatTimestamp(
                        activity.timestamp
                    ),
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                            .copy(alpha = 0.7f),
                    modifier =
                        Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement =
            Arrangement.Center,
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
                    .copy(alpha = 0.3f)
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "No activities yet",
            style =
                MaterialTheme.typography.titleMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text =
                "Your recent activities will appear here",
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
                    .copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

fun formatTimestamp(
    timestamp: Instant
): String {
    val dateTime =
        timestamp.toLocalDateTime(
            TimeZone.currentSystemDefault()
        )

    return "${dateTime.dayOfMonth}/" +
            "${dateTime.monthNumber}/" +
            "${dateTime.year} " +
            "${dateTime.hour}:" +
            dateTime.minute
                .toString()
                .padStart(2, '0')
}

// previous code
/*
package org.example.dementia_tester_app.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.dementia_tester_app.data.Activity
import org.example.dementia_tester_app.data.ActivityService
import org.example.dementia_tester_app.data.DatabaseResult

/**
 * Activities screen
 */
@Composable
fun Activities() {
    val activityService = remember { ActivityService() }
    val activities by activityService.getActivitiesFlow().collectAsState(initial = null)
    var todaySummary by remember { mutableStateOf<Map<String, Int>?>(null) }
    var isLoadingSummary by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        activityService.getTodaySummary { result ->
            if (result is DatabaseResult.Success) {
                todaySummary = result.data
            }
            isLoadingSummary = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Daily Activities",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        DailySummarySection(todaySummary, isLoadingSummary)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        when {
            activities == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            activities!!.isEmpty() -> {
                EmptyState()
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(activities!!) { activity ->
                        ActivityItem(activity)
                    }
                }
            }
        }
    }
}

@Composable
fun DailySummarySection(summary: Map<String, Int>?, isLoading: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryItem("Total", summary?.get("total") ?: 0, Icons.Default.Assessment)
                    SummaryItem("Tasks", summary?.get("reminder") ?: 0, Icons.Default.CheckCircle)
                    SummaryItem("Tests", summary?.get("test") ?: 0, Icons.AutoMirrored.Filled.FactCheck)
                    SummaryItem("Games", summary?.get("game") ?: 0, Icons.Default.Games)
                }
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, count: Int, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActivityItem(activity: Activity) {
    val icon = when (activity.type) {
        "game" -> Icons.Default.Games
        "test" -> Icons.AutoMirrored.Filled.FactCheck
        "reminder" -> Icons.Default.Notifications
        "appointment" -> Icons.Default.Event
        else -> Icons.Default.History
    }

    // Keeping these specific semantic colors but wrapping in Material logic if needed
    val iconColor = when (activity.type) {
        "game" -> Color(0xFF4CAF50)
        "test" -> Color(0xFF2196F3)
        "reminder" -> Color(0xFFFF9800)
        "appointment" -> Color(0xFFE91E63)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (activity.description.isNotEmpty()) {
                    Text(
                        text = activity.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatTimestamp(activity.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No activities yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Your recent activities will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

fun formatTimestamp(timestamp: Instant): String {
    val dateTime = timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.dayOfMonth}/${dateTime.monthNumber}/${dateTime.year} ${dateTime.hour}:${dateTime.minute.toString().padStart(2, '0')}"
}
*/