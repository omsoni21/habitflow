package com.example.presentation.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.StartOfWeek
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessEmerald
import com.example.utils.DateUtils
import com.example.utils.HabitCatalog
import java.time.DayOfWeek
import kotlin.math.roundToInt

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = DateUtils.today()

    val orderedDaysOfWeek = if (state.startOfWeek == StartOfWeek.SUNDAY) {
        listOf(
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )
    } else {
        DayOfWeek.entries
    }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Habit Calendar",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    TextButton(
                        onClick = viewModel::jumpToToday,
                        modifier = Modifier.testTag("calendar_today_button")
                    ) {
                        Text("Today")
                    }
                }
            }

            // Monthly Grid Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Month Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = viewModel::previousMonth,
                                modifier = Modifier.testTag("calendar_prev_month")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = "Previous month"
                                )
                            }
                            Text(
                                text = DateUtils.formatMonthYear(state.currentMonth),
                                style = MaterialTheme.typography.titleLarge
                            )
                            IconButton(
                                onClick = viewModel::nextMonth,
                                modifier = Modifier.testTag("calendar_next_month")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "Next month"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Weekday Headers
                        Row(modifier = Modifier.fillMaxWidth()) {
                            orderedDaysOfWeek.forEach { day ->
                                Text(
                                    text = DateUtils.shortDayName(day),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 6-Week Grid
                        state.monthCells.chunked(7).forEach { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                week.forEach { cell ->
                                    val isSelected = cell.date == state.selectedDate
                                    val isToday = cell.date == today

                                    val cellBg = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        !cell.inCurrentMonth -> Color.Transparent
                                        cell.status == CalendarDayStatus.COMPLETED_ALL ->
                                            SuccessEmerald.copy(alpha = 0.16f)
                                        cell.status == CalendarDayStatus.PARTIAL ->
                                            StreakAmber.copy(alpha = 0.16f)
                                        cell.status == CalendarDayStatus.MISSED ->
                                            ErrorRed.copy(alpha = 0.12f)
                                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                                    }

                                    val dotColor = when (cell.status) {
                                        CalendarDayStatus.COMPLETED_ALL -> SuccessEmerald
                                        CalendarDayStatus.PARTIAL -> StreakAmber
                                        CalendarDayStatus.MISSED -> ErrorRed
                                        else -> Color.Transparent
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.95f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(cellBg)
                                            .border(
                                                width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                                color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.selectDate(cell.date) },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "${cell.date.dayOfMonth}",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = when {
                                                isSelected -> Color.White
                                                !cell.inCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected && dotColor != Color.Transparent) Color.White else dotColor)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Legend Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CalendarLegendItem(color = SuccessEmerald, label = "Complete")
                            CalendarLegendItem(color = StreakAmber, label = "Partial")
                            CalendarLegendItem(color = ErrorRed, label = "Missed")
                        }
                    }
                }
            }

            // Selected Day Summary Header
            item {
                val pct = (state.selectedDateCompletionPercentage * 100f).roundToInt()
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = DateUtils.formatFullDate(state.selectedDate),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (state.selectedDateHabits.isEmpty()) {
                                        "No habits scheduled"
                                    } else {
                                        "${state.selectedDateHabits.count { it.isCompleted }} of ${state.selectedDateHabits.size} habits completed"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (state.selectedDateHabits.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { state.selectedDateCompletionPercentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (pct >= 100) SuccessEmerald else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Habits on Selected Day
            items(
                items = state.selectedDateHabits,
                key = { it.habit.id }
            ) { item ->
                val habit = item.habit
                val accent = Color(habit.color)
                val canToggle = !state.selectedDate.isAfter(today)

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(enabled = canToggle) {
                            viewModel.toggleHabitOnSelectedDate(habit)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = HabitCatalog.iconForKey(habit.icon),
                                contentDescription = habit.title,
                                tint = accent
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(habit.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${item.completedValue}/${habit.target} ${habit.targetUnit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        item.isCompleted -> SuccessEmerald
                                        state.selectedDate.isBefore(today) -> ErrorRed.copy(alpha = 0.15f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.isCompleted) Icons.Rounded.Check else Icons.Rounded.Close,
                                contentDescription = if (item.isCompleted) "Completed" else "Not completed",
                                tint = when {
                                    item.isCompleted -> Color.White
                                    state.selectedDate.isBefore(today) -> ErrorRed
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
