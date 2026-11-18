package com.example.presentation.rewards

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.usecase.AppAccentPalette
import com.example.domain.usecase.AppCrestIcon
import com.example.domain.usecase.GamificationEngine
import com.example.domain.usecase.GamificationReward
import com.example.presentation.components.LevelingInfo
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessEmerald

fun crestImageVector(crest: AppCrestIcon): ImageVector = when (crest) {
    AppCrestIcon.CLASSIC_BOLT -> Icons.Rounded.Bolt
    AppCrestIcon.EMBER_FLAME -> Icons.Rounded.LocalFireDepartment
    AppCrestIcon.STAR_CROWN -> Icons.Rounded.AutoAwesome
    AppCrestIcon.DIAMOND_SHIELD -> Icons.Rounded.Diamond
    AppCrestIcon.GOLDEN_CREST -> Icons.Rounded.WorkspacePremium
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    onBack: () -> Unit,
    viewModel: RewardsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    val currentLevel = state.levelStatus.level
    val filteredPalettes = AppAccentPalette.entries.filter { palette ->
        val unlocked = currentLevel >= palette.requiredLevel
        when (state.selectedFilter) {
            RewardsFilter.ALL -> true
            RewardsFilter.UNLOCKED -> unlocked
            RewardsFilter.LOCKED -> !unlocked
        }
    }
    val filteredCrests = AppCrestIcon.entries.filter { crest ->
        val unlocked = currentLevel >= crest.requiredLevel
        when (state.selectedFilter) {
            RewardsFilter.ALL -> true
            RewardsFilter.UNLOCKED -> unlocked
            RewardsFilter.LOCKED -> !unlocked
        }
    }
    val heatmapReward = GamificationReward.CONSISTENCY_HEATMAP
    val heatmapUnlocked = heatmapReward in state.levelStatus.unlockedRewards
    val showHeatmapCard = when (state.selectedFilter) {
        RewardsFilter.ALL -> true
        RewardsFilter.UNLOCKED -> heatmapUnlocked
        RewardsFilter.LOCKED -> !heatmapUnlocked
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rewards & Cosmetics") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("rewards_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp)
                    .testTag("rewards_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Rewards Summary Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = crestImageVector(state.selectedCrest),
                                    contentDescription = state.selectedCrest.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${state.unlockedCount} of ${state.totalCount} Rewards Unlocked",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    modifier = Modifier.testTag("rewards_summary_title")
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Theme: ${state.selectedPalette.label} • Crest: ${state.selectedCrest.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }
                    }
                }

                // Current Level & Progress Card
                item {
                    LevelingInfo(
                        levelStatus = state.levelStatus,
                        crestIcon = state.selectedCrest,
                        modifier = Modifier.testTag("rewards_leveling_info")
                    )
                }

                // Filter Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RewardsFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = state.selectedFilter == filter,
                                onClick = { viewModel.selectFilter(filter) },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("rewards_filter_${filter.name}")
                            )
                        }
                    }
                }

                // Section: Theme Color Palettes
                if (filteredPalettes.isNotEmpty()) {
                    item {
                        SectionHeaderRow(
                            icon = Icons.Rounded.Palette,
                            title = "Theme Color Palettes"
                        )
                    }
                    items(
                        items = filteredPalettes,
                        key = { "palette_${it.name}" }
                    ) { palette ->
                        val unlocked = currentLevel >= palette.requiredLevel
                        val equipped = state.selectedPalette == palette
                        ThemePaletteRewardCard(
                            palette = palette,
                            isUnlocked = unlocked,
                            isEquipped = equipped,
                            onActionClick = { viewModel.equipPalette(palette) }
                        )
                    }
                }

                // Section: App & Profile Crest Icons
                if (filteredCrests.isNotEmpty()) {
                    item {
                        SectionHeaderRow(
                            icon = Icons.Rounded.WorkspacePremium,
                            title = "Cosmetic Crest Icons"
                        )
                    }
                    items(
                        items = filteredCrests,
                        key = { "crest_${it.name}" }
                    ) { crest ->
                        val unlocked = currentLevel >= crest.requiredLevel
                        val equipped = state.selectedCrest == crest
                        CrestRewardCard(
                            crest = crest,
                            isUnlocked = unlocked,
                            isEquipped = equipped,
                            onActionClick = { viewModel.equipCrest(crest) }
                        )
                    }
                }

                // Section: Special Milestones & Analytics Unlocks
                if (showHeatmapCard) {
                    item {
                        SectionHeaderRow(
                            icon = Icons.Rounded.AutoGraph,
                            title = "Feature Milestones"
                        )
                    }
                    item {
                        FeatureUnlockRewardCard(
                            reward = heatmapReward,
                            isUnlocked = heatmapUnlocked,
                            currentXp = state.levelStatus.totalXp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ThemePaletteRewardCard(
    palette: AppAccentPalette,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onActionClick: () -> Unit
) {
    val primaryPreview = Color(palette.previewColors.first())
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onActionClick)
            .testTag("reward_palette_${palette.name}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isEquipped) 1.5.dp else 1.dp,
            color = if (isEquipped) primaryPreview else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color swatch stack
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                palette.previewColors.forEach { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex))
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = palette.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LevelRequirementBadge(
                        requiredLevel = palette.requiredLevel,
                        isUnlocked = isUnlocked
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = palette.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            RewardEquipButton(
                isUnlocked = isUnlocked,
                isEquipped = isEquipped,
                requiredLevel = palette.requiredLevel,
                onClick = onActionClick
            )
        }
    }
}

@Composable
private fun CrestRewardCard(
    crest: AppCrestIcon,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onActionClick: () -> Unit
) {
    val crestColor = Color(crest.accentColor)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onActionClick)
            .testTag("reward_crest_${crest.name}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isEquipped) 1.5.dp else 1.dp,
            color = if (isEquipped) crestColor else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isUnlocked) crestColor.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlocked) crestImageVector(crest) else Icons.Rounded.Lock,
                    contentDescription = crest.label,
                    tint = if (isUnlocked) crestColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = crest.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LevelRequirementBadge(
                        requiredLevel = crest.requiredLevel,
                        isUnlocked = isUnlocked
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = crest.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            RewardEquipButton(
                isUnlocked = isUnlocked,
                isEquipped = isEquipped,
                requiredLevel = crest.requiredLevel,
                onClick = onActionClick
            )
        }
    }
}

@Composable
private fun FeatureUnlockRewardCard(
    reward: GamificationReward,
    isUnlocked: Boolean,
    currentXp: Int
) {
    val targetXp = GamificationEngine.xpThresholdForLevel(reward.requiredLevel)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reward_feature_${reward.name}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isUnlocked) SuccessEmerald.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isUnlocked) SuccessEmerald.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Rounded.AutoGraph else Icons.Rounded.Lock,
                    contentDescription = reward.title,
                    tint = if (isUnlocked) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = reward.title,
                        style = MaterialTheme.typography.titleSmall
                    )
                    LevelRequirementBadge(
                        requiredLevel = reward.requiredLevel,
                        isUnlocked = isUnlocked
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isUnlocked) {
                        "${reward.description} • Active in Statistics"
                    } else {
                        "${reward.description} (${(targetXp - currentXp).coerceAtLeast(0)} XP remaining)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LevelRequirementBadge(
    requiredLevel: Int,
    isUnlocked: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isUnlocked) {
            SuccessEmerald.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Text(
            text = if (isUnlocked) "Unlocked" else "Lv.$requiredLevel",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = if (isUnlocked) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun RewardEquipButton(
    isUnlocked: Boolean,
    isEquipped: Boolean,
    requiredLevel: Int,
    onClick: () -> Unit
) {
    when {
        isEquipped -> {
            Button(
                onClick = onClick,
                enabled = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Equipped",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Equipped", style = MaterialTheme.typography.labelMedium)
            }
        }
        isUnlocked -> {
            OutlinedButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Equip", style = MaterialTheme.typography.labelMedium)
            }
        }
        else -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lv.$requiredLevel",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
