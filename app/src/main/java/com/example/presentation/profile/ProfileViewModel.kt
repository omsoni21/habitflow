package com.example.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.toDomain
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.GamificationRepository
import com.example.data.repository.HabitRepository
import com.example.domain.model.StartOfWeek
import com.example.domain.usecase.AppAccentPalette
import com.example.domain.usecase.AppCrestIcon
import com.example.domain.usecase.BackupAndExportManager
import com.example.domain.usecase.GamificationEngine
import com.example.domain.usecase.GamificationReward
import com.example.domain.usecase.LevelStatus
import com.example.domain.usecase.StreakCalculator
import com.example.domain.usecase.XpBreakdown
import com.example.notifications.ReminderScheduler
import com.example.ui.theme.ThemeMode
import com.example.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ProfileUiState(
    val userName: String = "Alex",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentPalette: AppAccentPalette = AppAccentPalette.ELECTRIC_INDIGO,
    val crestIcon: AppCrestIcon = AppCrestIcon.CLASSIC_BOLT,
    val notificationsEnabled: Boolean = true,
    val startOfWeek: StartOfWeek = StartOfWeek.MONDAY,
    val totalHabitsCreated: Int = 0,
    val totalCompletedHabits: Int = 0,
    val bestStreak: Int = 0,
    val unlockedAchievementsCount: Int = 0,
    val totalAchievementsCount: Int = 8,
    val unlockedRewardsCount: Int = 2,
    val totalRewardsCount: Int = AppAccentPalette.entries.size + AppCrestIcon.entries.size + 1,
    val levelStatus: LevelStatus = GamificationEngine.buildLevelStatusFromXp(XpBreakdown(0, 0, 0, 0)),
    val statusMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitRepository: HabitRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val gamificationRepository: GamificationRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val statusMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            gamificationRepository.ensureInitialized(DateUtils.today())
        }
    }

    private val habitSummaryFlow = combine(
        habitRepository.observeAllHabits(),
        habitRepository.observeAllCompletions(),
        habitRepository.observeUnlockedAchievements()
    ) { habits, completions, unlocked ->
        Triple(habits, completions, unlocked)
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        habitSummaryFlow,
        gamificationRepository.observeLevelStatus(),
        preferencesRepository.userPreferences,
        statusMessage
    ) { (habits, completions, unlocked), levelStatus, prefs, message ->
        val today = DateUtils.today()
        val habitMap = habits.associateBy { it.id }
        val totalCompletions = completions.count { c ->
            val target = habitMap[c.habitId]?.target ?: 1
            c.value >= target
        }
        val bestStreak = habits.maxOfOrNull {
            StreakCalculator.calculateHabitStreak(it, completions, today).longestStreak
        } ?: 0

        val unlockedRewards = AppAccentPalette.entries.count { levelStatus.level >= it.requiredLevel } +
            AppCrestIcon.entries.count { levelStatus.level >= it.requiredLevel } +
            if (GamificationReward.CONSISTENCY_HEATMAP in levelStatus.unlockedRewards) 1 else 0

        ProfileUiState(
            userName = prefs.userName,
            themeMode = prefs.themeMode,
            accentPalette = prefs.accentPalette,
            crestIcon = prefs.crestIcon,
            notificationsEnabled = prefs.notificationsEnabled,
            startOfWeek = prefs.startOfWeek,
            totalHabitsCreated = habits.size,
            totalCompletedHabits = totalCompletions,
            bestStreak = bestStreak,
            unlockedAchievementsCount = unlocked.size,
            unlockedRewardsCount = unlockedRewards,
            levelStatus = levelStatus,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun updateUserName(name: String) {
        viewModelScope.launch {
            preferencesRepository.updateUserName(name)
            statusMessage.value = "Profile name updated"
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(themeMode)
        }
    }

    fun selectAccentPalette(palette: AppAccentPalette) {
        val currentLevel = uiState.value.levelStatus.level
        if (currentLevel < palette.requiredLevel) {
            statusMessage.value = "Reach Level ${palette.requiredLevel} to unlock ${palette.label}"
            return
        }
        viewModelScope.launch {
            preferencesRepository.updateAccentPalette(palette)
            statusMessage.value = "${palette.label} theme equipped!"
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateNotificationsEnabled(enabled)
            reminderScheduler.rescheduleAllActiveReminders()
        }
    }

    fun updateStartOfWeek(startOfWeek: StartOfWeek) {
        viewModelScope.launch {
            preferencesRepository.updateStartOfWeek(startOfWeek)
        }
    }

    fun exportBackupJson(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val prefs = preferencesRepository.getPreferencesOnce()
                    val habits = habitRepository.getAllHabitEntitiesOnce()
                    val completions = habitRepository.getAllCompletionEntitiesOnce()
                    val achievements = habitRepository.getAllAchievementEntitiesOnce()
                    val jsonString = BackupAndExportManager.encodeBackupJson(
                        userName = prefs.userName,
                        habits = habits,
                        completions = completions,
                        achievements = achievements
                    )
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(jsonString.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open destination file")
                }
            }
            statusMessage.value = if (result.isSuccess) {
                "Backup saved successfully"
            } else {
                "Backup failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun restoreBackupJson(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    } ?: error("Unable to read backup file")
                    val payload = BackupAndExportManager.decodeBackupJson(raw).getOrThrow()
                    habitRepository.restoreFromBackup(payload)
                    preferencesRepository.updateUserName(payload.userName)
                    reminderScheduler.rescheduleAllActiveReminders()
                }
            }
            statusMessage.value = if (result.isSuccess) {
                "Backup restored successfully"
            } else {
                "Restore failed: Invalid or unreadable backup file"
            }
        }
    }

    fun exportCsvHistory(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val domainHabits = habitRepository.getAllHabitEntitiesOnce().map {
                        it.toDomain()
                    }
                    val completions = habitRepository.getAllCompletionEntitiesOnce().map {
                        com.example.domain.model.HabitCompletion(
                            id = it.id,
                            habitId = it.habitId,
                            completionDate = DateUtils.parseIsoDate(it.completionDate),
                            completedAt = it.completedAt,
                            value = it.value
                        )
                    }
                    val csv = BackupAndExportManager.buildCsvExport(domainHabits, completions)
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open CSV destination")
                }
            }
            statusMessage.value = if (result.isSuccess) {
                "CSV history exported successfully"
            } else {
                "CSV export failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }
}
