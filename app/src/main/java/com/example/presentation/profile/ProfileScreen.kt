package com.example.presentation.profile

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.StartOfWeek
import com.example.domain.usecase.AppAccentPalette
import com.example.domain.usecase.AppCrestIcon
import com.example.domain.usecase.GamificationReward
import com.example.presentation.components.LevelingInfo
import com.example.presentation.components.MetricPillTile
import com.example.presentation.rewards.crestImageVector
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.ThemeMode
import com.example.utils.DateUtils
import com.example.utils.ShareMilestoneHelper
import com.example.widget.HabitWidgetProvider
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onNavigateToAchievements: () -> Unit,
    onNavigateToRewards: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var nameDraft by remember(state.userName) { mutableStateOf(state.userName) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri -> uri?.let { viewModel.exportBackupJson(it) } }
    )

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.restoreBackupJson(it) } }
    )

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri -> uri?.let { viewModel.exportCsvHistory(it) } }
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> viewModel.updateNotificationsEnabled(granted) }
    )

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
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
                Text(
                    text = "Profile & Settings",
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            // User Profile Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.crestIcon != AppCrestIcon.CLASSIC_BOLT || state.levelStatus.level >= 7) {
                                Icon(
                                    imageVector = if (state.crestIcon != AppCrestIcon.CLASSIC_BOLT) {
                                        crestImageVector(state.crestIcon)
                                    } else {
                                        Icons.Rounded.WorkspacePremium
                                    },
                                    contentDescription = state.crestIcon.label,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Text(
                                    text = state.userName.firstOrNull()?.uppercase() ?: "H",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.userName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = "Level ${state.levelStatus.level} • ${state.levelStatus.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        IconButton(
                            onClick = {
                                ShareMilestoneHelper.shareLevelAndAchievements(
                                    context = context,
                                    userName = state.userName,
                                    levelStatus = state.levelStatus,
                                    bestStreak = state.bestStreak,
                                    unlockedAchievementsCount = state.unlockedAchievementsCount,
                                    totalAchievementsCount = state.totalAchievementsCount
                                )
                            },
                            modifier = Modifier.testTag("profile_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share level and achievements",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = {
                                nameDraft = state.userName
                                showEditNameDialog = true
                            },
                            modifier = Modifier.testTag("edit_profile_name_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit name",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricPillTile(
                        icon = Icons.Rounded.ListAlt,
                        label = "Created",
                        value = "${state.totalHabitsCreated}",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPillTile(
                        icon = Icons.Rounded.CheckCircle,
                        label = "Completed",
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

            // Leveling Info & Next Milestone Card
            item {
                LevelingInfo(
                    levelStatus = state.levelStatus,
                    crestIcon = state.crestIcon,
                    showXpBreakdown = true,
                    onClick = onNavigateToRewards,
                    modifier = Modifier.testTag("profile_leveling_info")
                )
            }

            // Unlockable Rewards & Themes
            item {
                val lv = state.levelStatus
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unlockable App Themes", style = MaterialTheme.typography.titleSmall)
                        }

                        AppAccentPalette.entries.forEach { palette ->
                            val unlocked = lv.level >= palette.requiredLevel
                            val equipped = state.accentPalette == palette
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { viewModel.selectAccentPalette(palette) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (equipped) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                border = BorderStroke(
                                    width = if (equipped) 1.5.dp else 1.dp,
                                    color = if (equipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            equipped -> Icons.Rounded.Check
                                            unlocked -> Icons.Rounded.Palette
                                            else -> Icons.Rounded.Lock
                                        },
                                        contentDescription = palette.label,
                                        tint = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(palette.label, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            palette.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (equipped) {
                                        Text(
                                            "Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        val heatmapReward = GamificationReward.CONSISTENCY_HEATMAP
                        val heatmapUnlocked = heatmapReward in lv.unlockedRewards
                        Text(
                            text = if (heatmapUnlocked) {
                                "Level 5 Reward Unlocked: 90-Day Consistency Heatmap is active in Stats."
                            } else {
                                "Level 5 Reward: Reach 500 XP to unlock the 90-Day Consistency Heatmap in Stats."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Rewards & Cosmetics Shortcut
            item {
                SettingsRowCard(
                    icon = Icons.Rounded.WorkspacePremium,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Rewards & Cosmetics",
                    subtitle = "${state.unlockedRewardsCount} of ${state.totalRewardsCount} unlocked • Themes & crest icons",
                    onClick = onNavigateToRewards,
                    modifier = Modifier.testTag("profile_rewards_row")
                )
            }

            // Achievements Shortcut
            item {
                SettingsRowCard(
                    icon = Icons.Rounded.EmojiEvents,
                    iconTint = StreakAmber,
                    title = "Achievements Gallery",
                    subtitle = "${state.unlockedAchievementsCount} of ${state.totalAchievementsCount} milestones unlocked",
                    onClick = onNavigateToAchievements
                )
            }

            // Home Screen Widget Pinning
            item {
                SettingsRowCard(
                    icon = Icons.Rounded.Widgets,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "Home Screen Widget",
                    subtitle = "Track today's habits without opening the app",
                    onClick = {
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val provider = ComponentName(context, HabitWidgetProvider::class.java)
                        val pinned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                            appWidgetManager.isRequestPinAppWidgetSupported
                        ) {
                            appWidgetManager.requestPinAppWidget(provider, null, null)
                        } else {
                            false
                        }
                        if (!pinned) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Long-press your home screen and select Widgets -> HabitFlow to add it."
                                )
                            }
                        }
                    },
                    modifier = Modifier.testTag("profile_add_widget_row")
                )
            }

            // Preferences Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Preferences", style = MaterialTheme.typography.titleMedium)

                        // Theme Selection
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.DarkMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Theme", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ThemeMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = state.themeMode == mode,
                                        onClick = { viewModel.updateThemeMode(mode) },
                                        label = {
                                            Text(
                                                when (mode) {
                                                    ThemeMode.SYSTEM -> "System"
                                                    ThemeMode.LIGHT -> "Light"
                                                    ThemeMode.DARK -> "Dark"
                                                }
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        // Notifications Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Habit Reminders", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Enable local scheduled notifications",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = state.notificationsEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.updateNotificationsEnabled(enabled)
                                    }
                                }
                            )
                        }

                        // Start of Week
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start of Week", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StartOfWeek.entries.forEach { startDay ->
                                    FilterChip(
                                        selected = state.startOfWeek == startDay,
                                        onClick = { viewModel.updateStartOfWeek(startDay) },
                                        label = { Text(startDay.label) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Data Backup & Export
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Data & Export", style = MaterialTheme.typography.titleMedium)

                    SettingsRowCard(
                        icon = Icons.Rounded.Backup,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Backup Data (JSON)",
                        subtitle = "Save habits, completions, and achievements locally",
                        onClick = {
                            exportBackupLauncher.launch("habitflow_backup_${DateUtils.formatIsoDate(DateUtils.today())}.json")
                        }
                    )

                    SettingsRowCard(
                        icon = Icons.Rounded.Restore,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "Restore Backup",
                        subtitle = "Import a previously saved HabitFlow JSON backup",
                        onClick = {
                            restoreBackupLauncher.launch(arrayOf("application/json", "text/plain"))
                        }
                    )

                    SettingsRowCard(
                        icon = Icons.Rounded.Download,
                        iconTint = SuccessEmerald,
                        title = "Export Habit History (CSV)",
                        subtitle = "Spreadsheet-compatible log of all habit completions",
                        onClick = {
                            exportCsvLauncher.launch("habitflow_history_${DateUtils.formatIsoDate(DateUtils.today())}.csv")
                        }
                    )
                }
            }

            // About & Privacy
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Application", style = MaterialTheme.typography.titleMedium)

                    SettingsRowCard(
                        icon = Icons.Rounded.Info,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "About HabitFlow",
                        subtitle = "Version 1.0.0 • Offline-first Room & Jetpack Compose",
                        onClick = { showAboutDialog = true }
                    )

                    SettingsRowCard(
                        icon = Icons.Rounded.PrivacyTip,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        title = "Privacy Policy",
                        subtitle = "100% local storage • Zero external tracking",
                        onClick = { showPrivacyDialog = true }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Profile Name") },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateUserName(nameDraft)
                        showEditNameDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About HabitFlow") },
            text = {
                Text(
                    "HabitFlow v1.0.0 is an offline-first smart habit tracker built natively with Kotlin, Jetpack Compose, Material Design 3, Room Database, WorkManager, and Hilt."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Text(
                    "Your privacy comes first. All habits, completion logs, achievements, and preferences are stored strictly locally on your device using Room and DataStore. HabitFlow does not collect analytics, track behavior, or transmit your personal data to external servers. Backups and CSV exports are created only when you explicitly choose a destination."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Understood") }
            }
        )
    }
}

@Composable
private fun SettingsRowCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
