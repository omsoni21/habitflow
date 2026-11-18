package com.example.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.usecase.AppCrestIcon
import com.example.domain.usecase.GamificationEngine
import com.example.domain.usecase.GamificationReward
import com.example.domain.usecase.LevelStatus
import com.example.presentation.rewards.crestImageVector
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StreakAmber
import kotlin.math.roundToInt

@Composable
fun LevelingInfo(
    levelStatus: LevelStatus,
    modifier: Modifier = Modifier,
    crestIcon: AppCrestIcon = AppCrestIcon.CLASSIC_BOLT,
    showXpBreakdown: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = levelStatus.progressToNextLevel.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "leveling_xp_progress"
    )
    val nextLevel = levelStatus.level + 1
    val nextLevelTitle = GamificationEngine.titleForLevel(nextLevel)
    val nextRewardMilestone = GamificationReward.entries.firstOrNull {
        it.requiredLevel > levelStatus.level
    }

    val cardModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Current Level & Total XP Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (levelStatus.level >= 7 && crestIcon == AppCrestIcon.CLASSIC_BOLT) {
                                Icons.Rounded.WorkspacePremium
                            } else {
                                crestImageVector(crestIcon)
                            },
                            contentDescription = "Current level",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Level ${levelStatus.level} • ${levelStatus.title}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("leveling_info_current_level")
                        )
                        Text(
                            text = "${(levelStatus.progressToNextLevel * 100).roundToInt()}% toward Level $nextLevel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "${levelStatus.totalXp} / ${levelStatus.nextLevelXp} XP",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // XP Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("leveling_info_progress_bar"),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            // Next Level Milestone Pill
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("leveling_info_next_milestone"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (nextRewardMilestone?.requiredLevel == nextLevel) {
                                Icons.Rounded.AutoAwesome
                            } else {
                                Icons.Rounded.Flag
                            },
                            contentDescription = "Next milestone",
                            tint = if (nextRewardMilestone?.requiredLevel == nextLevel) {
                                StreakAmber
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val milestoneText = if (nextRewardMilestone != null && nextRewardMilestone.requiredLevel == nextLevel) {
                            "Next: Level $nextLevel • Unlocks ${nextRewardMilestone.title}"
                        } else if (nextRewardMilestone != null) {
                            "Next: Level $nextLevel ($nextLevelTitle) • Lv.${nextRewardMilestone.requiredLevel}: ${nextRewardMilestone.title}"
                        } else {
                            "Next Milestone: Level $nextLevel • $nextLevelTitle"
                        }
                        Text(
                            text = milestoneText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${levelStatus.xpRemainingForNextLevel} XP left",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showXpBreakdown) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Completions: ${levelStatus.breakdown.completionXp} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Streaks: ${levelStatus.breakdown.streakMilestoneXp} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Perfect Days: ${levelStatus.breakdown.perfectDayXp} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Badges: ${levelStatus.breakdown.achievementXp} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
