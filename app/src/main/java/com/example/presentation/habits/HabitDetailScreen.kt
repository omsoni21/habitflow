package com.example.presentation.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.usecase.HabitScheduleHelper
import com.example.presentation.components.MetricPillTile
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessEmerald
import com.example.utils.DateUtils
import com.example.utils.HabitCatalog
import com.example.utils.ShareMilestoneHelper
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: HabitDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val habit = state.habit

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habit Insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (habit != null) {
                        IconButton(
                            onClick = {
                                ShareMilestoneHelper.shareHabitStreak(
                                    context = context,
                                    habit = habit,
                                    streakInfo = state.streakInfo,
                                    levelStatus = state.levelStatus
                                )
                            },
                            modifier = Modifier.testTag("habit_detail_share_button")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share Streak")
                        }
                        IconButton(onClick = { onEdit(habit.id) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit Habit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete Habit",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (habit == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Habit not found", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        val accentColor = Color(habit.color)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Summary Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = HabitCatalog.iconForKey(habit.icon),
                                    contentDescription = habit.title,
                                    tint = accentColor,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(habit.title, style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    text = "${habit.category.label} • ${HabitScheduleHelper.formatScheduleLabel(habit)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (habit.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = habit.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = viewModel::toggleTodayCompletion,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isCompletedToday) SuccessEmerald else accentColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.isCompletedToday) {
                                    "Completed Today (Tap to Undo)"
                                } else {
                                    "Mark Today Complete (${state.todayProgressValue}/${habit.target} ${habit.targetUnit})"
                                }
                            )
                        }
                    }
                }

                // 4 Streak KPI Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricPillTile(
                        icon = Icons.Rounded.LocalFireDepartment,
                        label = "Current Streak",
                        value = "${state.streakInfo.currentStreak}d",
                        accentColor = StreakAmber,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPillTile(
                        icon = Icons.Rounded.EmojiEvents,
                        label = "Longest Streak",
                        value = "${state.streakInfo.longestStreak}d",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricPillTile(
                        icon = Icons.Rounded.TaskAlt,
                        label = "Total Days",
                        value = "${state.streakInfo.totalCompletedDays}",
                        accentColor = SuccessEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPillTile(
                        icon = Icons.Rounded.Percent,
                        label = "Completion Rate",
                        value = "${(state.streakInfo.completionRate * 100).roundToInt()}%",
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedButton(
                    onClick = {
                        ShareMilestoneHelper.shareHabitStreak(
                            context = context,
                            habit = habit,
                            streakInfo = state.streakInfo,
                            levelStatus = state.levelStatus
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("habit_detail_share_streak_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share ${state.streakInfo.currentStreak}-Day Streak")
                }

                // 30-Day Activity Grid
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Last 30 Days",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap any past date to log or undo completion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        val today = DateUtils.today()
                        val days = (29 downTo 0).map { today.minusDays(it.toLong()) }
                        days.chunked(6).forEach { weekRow ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                weekRow.forEach { date ->
                                    val completed = date in state.completedDatesSet
                                    val scheduled = HabitScheduleHelper.isScheduledForDate(
                                        habit,
                                        date,
                                        ignoreActiveFlag = true
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    completed -> accentColor
                                                    scheduled -> MaterialTheme.colorScheme.surfaceVariant
                                                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                                                }
                                            )
                                            .clickable { viewModel.toggleDateCompletion(date) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${date.dayOfMonth}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (completed) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Lifecycle Controls (Pause / Archive)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!habit.isArchived) {
                        OutlinedButton(
                            onClick = viewModel::togglePause,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (habit.isActive) Icons.Rounded.PauseCircleOutline else Icons.Rounded.PlayCircleOutline,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (habit.isActive) "Pause" else "Resume")
                        }
                    }
                    OutlinedButton(
                        onClick = viewModel::toggleArchive,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (habit.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (habit.isArchived) "Unarchive" else "Archive")
                    }
                }
            }
        }
    }

    if (showDeleteDialog && habit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Habit?") },
            text = { Text("Permanently delete \"${habit.title}\" and all its history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteHabit(onBack)
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
