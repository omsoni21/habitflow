package com.example.presentation.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Habit
import com.example.domain.model.HabitCategory
import com.example.domain.usecase.HabitScheduleHelper
import com.example.presentation.components.EmptyHabitsState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.utils.DateUtils
import com.example.utils.HabitCatalog

@Composable
fun HabitsScreen(
    onNavigateToAddHabit: () -> Unit,
    onNavigateToEditHabit: (Long) -> Unit,
    onNavigateToHabitDetail: (Long) -> Unit,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var habitPendingDelete by remember { mutableStateOf<Habit?>(null) }
    val hasActiveFilter = state.searchQuery.isNotBlank() || state.selectedCategory != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "My Habits",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = { Text("Search by title or category...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search habits") },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.updateSearchQuery("") },
                                modifier = Modifier.testTag("habits_search_clear")
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habits_search_input")
                )
            }

            // Status Tabs
            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                HabitFilterTab.entries.forEach { tab ->
                    val count = when (tab) {
                        HabitFilterTab.ACTIVE -> state.totalActiveCount
                        HabitFilterTab.PAUSED -> state.totalPausedCount
                        HabitFilterTab.ARCHIVED -> state.totalArchivedCount
                    }
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text("${tab.label} ($count)") },
                        modifier = Modifier.testTag("habits_tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedCategory == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text("All") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("habits_category_all")
                )
                HabitCategory.entries.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.label) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("habits_category_${category.name.lowercase()}")
                    )
                }
            }

            if (hasActiveFilter && state.items.isNotEmpty()) {
                Text(
                    text = "${state.items.size} matching ${if (state.items.size == 1) "habit" else "habits"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .testTag("habits_search_result_count")
                )
            }

            if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (hasActiveFilter) {
                        EmptyHabitsState(
                            title = "No matching habits",
                            subtitle = "Try another habit title or category, or clear your search filters.",
                            actionLabel = "Clear Filters",
                            onActionClick = viewModel::clearFilters,
                            modifier = Modifier.testTag("habits_no_matches_state")
                        )
                    } else {
                        EmptyHabitsState(
                            title = "No ${state.selectedTab.label.lowercase()} habits",
                            subtitle = if (state.selectedTab == HabitFilterTab.ACTIVE) {
                                "Create a habit or pick a curated suggestion to get started."
                            } else {
                                "Habits you ${state.selectedTab.label.lowercase()} will appear here."
                            },
                            actionLabel = if (state.selectedTab == HabitFilterTab.ACTIVE) "Create Habit" else null,
                            onActionClick = if (state.selectedTab == HabitFilterTab.ACTIVE) onNavigateToAddHabit else null
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("habits_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = state.items, key = { it.habit.id }) { item ->
                        HabitManagementCard(
                            item = item,
                            onClick = { onNavigateToHabitDetail(item.habit.id) },
                            onEdit = { onNavigateToEditHabit(item.habit.id) },
                            onTogglePause = { viewModel.togglePause(item.habit) },
                            onToggleArchive = { viewModel.toggleArchive(item.habit) },
                            onDelete = { habitPendingDelete = item.habit }
                        )
                    }
                }
            }
        }
    }

    habitPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { habitPendingDelete = null },
            title = { Text("Delete Habit?") },
            text = {
                Text("Deleting \"${target.title}\" will permanently remove its completion history and streaks.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHabit(target.id)
                        habitPendingDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HabitManagementCard(
    item: HabitListItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val habit = item.habit
    val accentColor = Color(habit.color)
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("habit_manage_card_${habit.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = HabitCatalog.iconForKey(habit.icon),
                    contentDescription = habit.title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${habit.category.label} • ${HabitScheduleHelper.formatScheduleLabel(habit)} • ${habit.target} ${habit.targetUnit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = StreakAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${item.streakInfo.currentStreak}d streak",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (habit.reminderEnabled && habit.reminderTime != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = "Reminder set",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = DateUtils.formatDisplayTime(habit.reminderTime),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("habit_menu_${habit.id}")
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    if (!habit.isArchived) {
                        DropdownMenuItem(
                            text = { Text(if (habit.isActive) "Pause" else "Resume") },
                            leadingIcon = {
                                Icon(
                                    if (habit.isActive) Icons.Rounded.PauseCircleOutline else Icons.Rounded.PlayCircleOutline,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onTogglePause()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (habit.isArchived) "Unarchive" else "Archive") },
                        leadingIcon = {
                            Icon(
                                if (habit.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleArchive()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
