package com.example.presentation.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.GamificationRepository
import com.example.domain.usecase.AppAccentPalette
import com.example.domain.usecase.AppCrestIcon
import com.example.domain.usecase.GamificationEngine
import com.example.domain.usecase.GamificationReward
import com.example.domain.usecase.LevelStatus
import com.example.domain.usecase.XpBreakdown
import com.example.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RewardsFilter(val label: String) {
    ALL("All"),
    UNLOCKED("Unlocked"),
    LOCKED("Locked")
}

data class RewardsUiState(
    val levelStatus: LevelStatus = GamificationEngine.buildLevelStatusFromXp(XpBreakdown(0, 0, 0, 0)),
    val selectedPalette: AppAccentPalette = AppAccentPalette.ELECTRIC_INDIGO,
    val selectedCrest: AppCrestIcon = AppCrestIcon.CLASSIC_BOLT,
    val selectedFilter: RewardsFilter = RewardsFilter.ALL,
    val unlockedCount: Int = 0,
    val totalCount: Int = AppAccentPalette.entries.size + AppCrestIcon.entries.size + 1,
    val statusMessage: String? = null
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(RewardsFilter.ALL)
    private val statusMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            gamificationRepository.ensureInitialized(DateUtils.today())
        }
    }

    val uiState: StateFlow<RewardsUiState> = combine(
        gamificationRepository.observeLevelStatus(),
        preferencesRepository.userPreferences,
        selectedFilter,
        statusMessage
    ) { levelStatus, prefs, filter, message ->
        val unlockedPalettes = AppAccentPalette.entries.count { levelStatus.level >= it.requiredLevel }
        val unlockedCrests = AppCrestIcon.entries.count { levelStatus.level >= it.requiredLevel }
        val heatmapUnlocked = if (GamificationReward.CONSISTENCY_HEATMAP in levelStatus.unlockedRewards) 1 else 0

        RewardsUiState(
            levelStatus = levelStatus,
            selectedPalette = prefs.accentPalette,
            selectedCrest = prefs.crestIcon,
            selectedFilter = filter,
            unlockedCount = unlockedPalettes + unlockedCrests + heatmapUnlocked,
            totalCount = AppAccentPalette.entries.size + AppCrestIcon.entries.size + 1,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RewardsUiState()
    )

    fun selectFilter(filter: RewardsFilter) {
        selectedFilter.value = filter
    }

    fun equipPalette(palette: AppAccentPalette) {
        val currentLevel = uiState.value.levelStatus.level
        if (currentLevel < palette.requiredLevel) {
            val requiredXp = GamificationEngine.xpThresholdForLevel(palette.requiredLevel)
            statusMessage.value = "Reach Level ${palette.requiredLevel} ($requiredXp XP) to unlock ${palette.label}"
            return
        }
        viewModelScope.launch {
            preferencesRepository.updateAccentPalette(palette)
            statusMessage.value = "${palette.label} theme equipped!"
        }
    }

    fun equipCrest(crest: AppCrestIcon) {
        val currentLevel = uiState.value.levelStatus.level
        if (currentLevel < crest.requiredLevel) {
            val requiredXp = GamificationEngine.xpThresholdForLevel(crest.requiredLevel)
            statusMessage.value = "Reach Level ${crest.requiredLevel} ($requiredXp XP) to unlock ${crest.label}"
            return
        }
        viewModelScope.launch {
            preferencesRepository.updateCrestIcon(crest)
            statusMessage.value = "${crest.label} crest equipped!"
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }
}
