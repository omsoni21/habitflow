package com.example.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.usecase.GamificationEngine
import com.example.domain.usecase.GamificationReward
import com.example.presentation.components.AchievementUnlockBanner
import com.example.presentation.components.AnimatedProgressRing
import com.example.presentation.components.ConfettiParticleOverlay
import com.example.presentation.components.EmptyHabitsState
import com.example.presentation.components.HabitDashboardCard
import com.example.presentation.components.LevelingInfo
import com.example.presentation.components.MetricPillTile
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessEmerald
import com.example.utils.ShareMilestoneHelper

@Composable
fun HomeScreen(
    onNavigateToAddHabit: () -> Unit,
    onNavigateToHabitDetail: (Long) -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToRewards: () -> Unit = {},
    handleCelebrationExternally: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            is HomeUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)
                        .testTag("home_habit_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        AchievementUnlockBanner(
                            achievement = state.newlyUnlockedAchievement,
                            onDismiss = viewModel::dismissAchievementBanner
                        )
                    }

                    // Header Greeting & Date
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.formattedDate.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${state.greeting}, ${state.userName}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(onClick = onNavigateToAchievements)
                                    .testTag("home_achievements_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (state.levelStatus.level >= 7) {
                                            Icons.Rounded.WorkspacePremium
                                        } else {
                                            Icons.Rounded.EmojiEvents
                                        },
                                        contentDescription = "Achievements",
                                        tint = StreakAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Lv.${state.levelStatus.level}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Leveling Info & Next Milestone Card
                    item {
                        LevelingInfo(
                            levelStatus = state.levelStatus,
                            crestIcon = state.crestIcon,
                            modifier = Modifier.testTag("home_xp_card"),
                            onClick = onNavigateToRewards
                        )
                    }

                    // Hero Progress Gradient Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary,
                                            Color(0xFF0284C7)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedProgressRing(
                                    progress = state.completionPercentage,
                                    size = 108.dp,
                                    strokeWidth = 10.dp
                                )

                                Spacer(modifier = Modifier.width(18.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Daily Flow",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (state.totalHabitsToday == 0) {
                                            "Ready to begin"
                                        } else if (state.completedHabitsToday == state.totalHabitsToday) {
                                            "All habits complete!"
                                        } else {
                                            "${state.completedHabitsToday} of ${state.totalHabitsToday} completed"
                                        },
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White.copy(alpha = 0.18f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFDE68A),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Best ${state.longestStreak}d",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3 KPI Tiles Row (Total, Completed, Pending)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricPillTile(
                                icon = Icons.Rounded.ListAlt,
                                label = "Today",
                                value = "${state.totalHabitsToday}",
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricPillTile(
                                icon = Icons.Rounded.CheckCircle,
                                label = "Completed",
                                value = "${state.completedHabitsToday}",
                                accentColor = SuccessEmerald,
                                modifier = Modifier.weight(1f)
                            )
                            MetricPillTile(
                                icon = Icons.Rounded.PendingActions,
                                label = "Pending",
                                value = "${state.pendingHabitsToday}",
                                accentColor = StreakAmber,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Motivational Quote Card
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FormatQuote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "\"${state.quote.first}\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "— ${state.quote.second}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = viewModel::cycleQuote,
                                    modifier = Modifier.testTag("cycle_quote_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Next quote",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Today's Habits Section Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.dashboard_today_habits),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            if (state.todayHabits.isNotEmpty()) {
                                Text(
                                    text = "${(state.completionPercentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (state.todayHabits.isEmpty()) {
                        item {
                            EmptyHabitsState(
                                title = stringResource(R.string.dashboard_empty_title),
                                subtitle = stringResource(R.string.dashboard_empty_subtitle),
                                actionLabel = stringResource(R.string.dashboard_add_habit),
                                onActionClick = onNavigateToAddHabit
                            )
                        }
                    } else {
                        items(
                            items = state.todayHabits,
                            key = { it.habit.id }
                        ) { item ->
                            HabitDashboardCard(
                                item = item,
                                onCardClick = { onNavigateToHabitDetail(item.habit.id) },
                                onQuickComplete = { viewModel.toggleHabitCompletion(item.habit) },
                                onIncrement = { viewModel.incrementHabitProgress(item.habit) }
                            )
                        }
                    }
                }

                // Level Up Celebration Dialog
                state.pendingLevelUpTo?.let { newLevel ->
                    val unlockedNow = GamificationReward.entries.filter { it.requiredLevel <= newLevel }
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissLevelUpDialog(newLevel) },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Level Up",
                                tint = StreakAmber,
                                modifier = Modifier.size(36.dp)
                            )
                        },
                        title = {
                            Text("Level Up! Level $newLevel")
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "You reached ${GamificationEngine.titleForLevel(newLevel)} rank with ${state.levelStatus.totalXp} XP!",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                val newlyGranted = unlockedNow.filter { it.requiredLevel == newLevel }
                                if (newlyGranted.isNotEmpty()) {
                                    Text(
                                        text = "New Reward Unlocked:",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    newlyGranted.forEach { reward ->
                                        Text(
                                            text = "• ${reward.title}: ${reward.description}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.dismissLevelUpDialog(newLevel) }) {
                                Text("Awesome!")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    ShareMilestoneHelper.shareLevelAndAchievements(
                                        context = context,
                                        userName = state.userName,
                                        levelStatus = state.levelStatus,
                                        bestStreak = state.longestStreak,
                                        unlockedAchievementsCount = state.unlockedAchievementsCount,
                                        totalAchievementsCount = com.example.domain.model.AchievementType.entries.size
                                    )
                                },
                                modifier = Modifier.testTag("level_up_share_button")
                            ) {
                                Text("Share")
                            }
                        }
                    )
                }

                if (!handleCelebrationExternally) {
                    ConfettiParticleOverlay(
                        celebration = state.activeCelebration,
                        onDismiss = viewModel::dismissCelebration
                    )
                }
            }
        }
    }
}
