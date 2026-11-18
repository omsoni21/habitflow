package com.example.presentation.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.components.AnimatedProgressRing
import com.example.presentation.components.EmptyHabitsState
import com.example.presentation.components.MetricPillTile
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessEmerald
import com.example.utils.HabitCatalog
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import kotlin.math.roundToInt

@Composable
fun StatisticsScreen(
    onNavigateToAddHabit: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Analytics",
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            if (!state.hasHabits) {
                item {
                    EmptyHabitsState(
                        title = "No analytics yet",
                        subtitle = "Create habits and log completions to unlock real charts, streaks, and consistency insights.",
                        actionLabel = "Create First Habit",
                        onActionClick = onNavigateToAddHabit
                    )
                }
                return@LazyColumn
            }

            // Productivity Score Hero Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedProgressRing(
                            progress = state.productivityScore / 100f,
                            size = 96.dp,
                            strokeWidth = 10.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            ringColors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary
                            ),
                            centerContent = {
                                Text(
                                    text = "${state.productivityScore}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Productivity Score",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Calculated from your 7-day completion rate and active streak momentum.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Summary KPI Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricPillTile(
                        icon = Icons.Rounded.CheckCircle,
                        label = "Total Completed",
                        value = "${state.totalCompletedHabits}",
                        accentColor = SuccessEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPillTile(
                        icon = Icons.Rounded.LocalFireDepartment,
                        label = "Best Streak",
                        value = "${state.bestStreak}d",
                        accentColor = StreakAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Chart Card with Daily / Weekly / Monthly Switcher
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = when (state.selectedRange) {
                                StatsTimeRange.DAILY -> "Daily Completion (%)"
                                StatsTimeRange.WEEKLY -> "Weekly Progress (%)"
                                StatsTimeRange.MONTHLY -> "Monthly Progress (%)"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatsTimeRange.entries.forEach { range ->
                                FilterChip(
                                    selected = state.selectedRange == range,
                                    onClick = { viewModel.selectRange(range) },
                                    label = { Text(range.label) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("stats_range_${range.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        when (state.selectedRange) {
                            StatsTimeRange.DAILY -> {
                                val labels = state.dailyPoints.map { it.label }
                                val values = state.dailyPoints.map { (it.percentage * 100f).toDouble() }
                                VicoBarChart(
                                    labels = labels,
                                    values = values,
                                    barColor = MaterialTheme.colorScheme.primary
                                )
                            }

                            StatsTimeRange.WEEKLY -> {
                                val labels = state.weeklyPoints.map { it.first }
                                val values = state.weeklyPoints.map { it.second.toDouble() }
                                VicoBarChart(
                                    labels = labels,
                                    values = values,
                                    barColor = MaterialTheme.colorScheme.secondary
                                )
                            }

                            StatsTimeRange.MONTHLY -> {
                                val labels = state.monthlyPoints.map { it.first }
                                val values = state.monthlyPoints.map { it.second.toDouble() }
                                VicoLineChart(
                                    labels = labels,
                                    values = values
                                )
                            }
                        }
                    }
                }
            }

            // Level 5 Reward: 90-Day Consistency Heatmap
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "90-Day Consistency Heatmap",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (state.isHeatmapUnlocked) {
                                    SuccessEmerald.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = if (state.isHeatmapUnlocked) "Unlocked" else "Level 5 Reward",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (state.isHeatmapUnlocked) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (state.isHeatmapUnlocked) {
                            Text(
                                text = "Daily completion intensity across the past 90 days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            state.heatmap90Days.chunked(15).forEach { rowDays ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rowDays.forEach { point ->
                                        val intensityColor = when {
                                            point.scheduledCount == 0 -> MaterialTheme.colorScheme.surfaceContainerLow
                                            point.percentage >= 1f -> MaterialTheme.colorScheme.primary
                                            point.percentage >= 0.5f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            point.percentage > 0f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(intensityColor)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "You are currently Level ${state.currentLevel}. Reach Level 5 (500 XP) by completing habits, streaks, and achievements to unlock this multi-month visualization.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Consistency Insights (Most & Least Consistent)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.mostConsistentHabit?.let { best ->
                        ConsistencyHighlightCard(
                            title = "Most Consistent",
                            habitTitle = best.habit.title,
                            ratePercent = (best.completionRate * 100).roundToInt(),
                            icon = Icons.Rounded.TrendingUp,
                            accentColor = SuccessEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    state.leastConsistentHabit?.let { needsFocus ->
                        ConsistencyHighlightCard(
                            title = "Needs Attention",
                            habitTitle = needsFocus.habit.title,
                            ratePercent = (needsFocus.completionRate * 100).roundToInt(),
                            icon = Icons.Rounded.TrendingDown,
                            accentColor = ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Habit-Wise Completion Rate Breakdown
            item {
                Text(
                    text = "Habit-Wise Completion Rate",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(items = state.habitStats, key = { it.habit.id }) { stat ->
                val accent = Color(stat.habit.color)
                val pct = (stat.completionRate * 100).roundToInt()
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = HabitCatalog.iconForKey(stat.habit.icon),
                                    contentDescription = stat.habit.title,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stat.habit.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${stat.totalCompletedDays} completions • Best ${stat.longestStreak}d",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = accent
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { stat.completionRate },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = accent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VicoBarChart(
    labels: List<String>,
    values: List<Double>,
    barColor: Color
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val safeValues = if (values.isEmpty()) listOf(0.0) else values

    LaunchedEffect(safeValues) {
        modelProducer.runTransaction {
            columnSeries { series(safeValues) }
        }
    }

    val bottomFormatter = remember(labels) {
        CartesianValueFormatter { _, x, _ ->
            labels.getOrElse(x.toInt()) { "" }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(
                        fill = Fill(barColor.toArgb()),
                        thickness = 18.dp,
                        shape = CorneredShape.rounded(topLeftPercent = 35, topRightPercent = 35)
                    )
                )
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomFormatter)
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
    )
}

@Composable
private fun VicoLineChart(
    labels: List<String>,
    values: List<Double>
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val safeValues = if (values.isEmpty()) listOf(0.0) else values

    LaunchedEffect(safeValues) {
        modelProducer.runTransaction {
            lineSeries { series(safeValues) }
        }
    }

    val bottomFormatter = remember(labels) {
        CartesianValueFormatter { _, x, _ ->
            labels.getOrElse(x.toInt()) { "" }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomFormatter)
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
    )
}

@Composable
private fun ConsistencyHighlightCard(
    title: String,
    habitTitle: String,
    ratePercent: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = habitTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$ratePercent% consistency",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
