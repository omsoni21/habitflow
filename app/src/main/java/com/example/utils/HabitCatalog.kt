package com.example.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitSuggestion
import java.time.LocalTime

data class HabitIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object HabitCatalog {
    val icons: List<HabitIconOption> = listOf(
        HabitIconOption("water_drop", "Hydration", Icons.Rounded.WaterDrop),
        HabitIconOption("menu_book", "Reading", Icons.Rounded.MenuBook),
        HabitIconOption("fitness_center", "Workout", Icons.Rounded.FitnessCenter),
        HabitIconOption("self_improvement", "Meditation", Icons.Rounded.SelfImprovement),
        HabitIconOption("wb_sunny", "Morning", Icons.Rounded.WbSunny),
        HabitIconOption("directions_walk", "Walking", Icons.Rounded.DirectionsWalk),
        HabitIconOption("directions_run", "Running", Icons.Rounded.DirectionsRun),
        HabitIconOption("code", "Coding", Icons.Rounded.Code),
        HabitIconOption("school", "Study", Icons.Rounded.School),
        HabitIconOption("bedtime", "Sleep", Icons.Rounded.Bedtime),
        HabitIconOption("edit_note", "Journal", Icons.Rounded.EditNote),
        HabitIconOption("favorite", "Wellness", Icons.Rounded.Favorite),
        HabitIconOption("brush", "Creative", Icons.Rounded.Brush),
        HabitIconOption("music_note", "Music", Icons.Rounded.MusicNote),
        HabitIconOption("savings", "Finance", Icons.Rounded.Savings),
        HabitIconOption("auto_stories", "Learning", Icons.Rounded.AutoStories)
    )

    fun iconForKey(key: String): ImageVector =
        icons.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.CheckCircle

    val predefinedSuggestions: List<HabitSuggestion> = listOf(
        HabitSuggestion(
            title = "Drink Water",
            description = "Stay hydrated throughout the day for clear focus and energy.",
            icon = "water_drop",
            category = HabitCategory.HEALTH,
            color = 0xFF0284C7L,
            target = 8,
            targetUnit = "glasses",
            defaultReminderTime = LocalTime.of(9, 0)
        ),
        HabitSuggestion(
            title = "Read Books",
            description = "Read at least 20 pages daily to expand your mind.",
            icon = "menu_book",
            category = HabitCategory.LEARNING,
            color = 0xFF7C3AEDL,
            target = 20,
            targetUnit = "pages",
            defaultReminderTime = LocalTime.of(20, 30)
        ),
        HabitSuggestion(
            title = "Exercise",
            description = "Strength or cardio movement to boost stamina and mood.",
            icon = "fitness_center",
            category = HabitCategory.FITNESS,
            color = 0xFFE11D48L,
            target = 1,
            targetUnit = "session",
            defaultReminderTime = LocalTime.of(17, 30)
        ),
        HabitSuggestion(
            title = "Meditation",
            description = "Quiet mindful breathing to calm stress and sharpen clarity.",
            icon = "self_improvement",
            category = HabitCategory.MINDFULNESS,
            color = 0xFF0D9488L,
            target = 10,
            targetUnit = "mins",
            defaultReminderTime = LocalTime.of(7, 30)
        ),
        HabitSuggestion(
            title = "Wake Up Early",
            description = "Start the morning peacefully before the day gets busy.",
            icon = "wb_sunny",
            category = HabitCategory.LIFESTYLE,
            color = 0xFFF59E0BL,
            target = 1,
            targetUnit = "time",
            defaultReminderTime = LocalTime.of(6, 30)
        ),
        HabitSuggestion(
            title = "Walking",
            description = "Take a refreshing outdoor walk to recharge your body.",
            icon = "directions_walk",
            category = HabitCategory.FITNESS,
            color = 0xFF10B981L,
            target = 30,
            targetUnit = "mins",
            defaultReminderTime = LocalTime.of(12, 30)
        ),
        HabitSuggestion(
            title = "Coding Practice",
            description = "Build projects or solve problems to level up engineering skill.",
            icon = "code",
            category = HabitCategory.PRODUCTIVITY,
            color = 0xFF4F46E5L,
            target = 45,
            targetUnit = "mins",
            defaultReminderTime = LocalTime.of(19, 0)
        ),
        HabitSuggestion(
            title = "Study",
            description = "Dedicated deep-focus study session without distractions.",
            icon = "school",
            category = HabitCategory.LEARNING,
            color = 0xFF2563EBL,
            target = 1,
            targetUnit = "session",
            defaultReminderTime = LocalTime.of(16, 0)
        ),
        HabitSuggestion(
            title = "Sleep Early",
            description = "Wind down screens and get restorative sleep on time.",
            icon = "bedtime",
            category = HabitCategory.HEALTH,
            color = 0xFF7C3AEDL,
            target = 1,
            targetUnit = "time",
            defaultReminderTime = LocalTime.of(22, 0)
        ),
        HabitSuggestion(
            title = "Journaling",
            description = "Reflect on gratitude, wins, and tomorrow's top priority.",
            icon = "edit_note",
            category = HabitCategory.MINDFULNESS,
            color = 0xFFDB2777L,
            target = 1,
            targetUnit = "entry",
            defaultReminderTime = LocalTime.of(21, 15)
        )
    )

    val onboardingGoals: List<String> = listOf(
        "Health & Hydration",
        "Fitness & Energy",
        "Mindfulness & Calm",
        "Deep Focus & Productivity",
        "Continuous Learning",
        "Better Sleep Rhythm"
    )

    /**
     * Analyzes selected onboarding goals and generates 3-5 relevant pre-defined habit suggestions
     * with habit name, description, icon, category, color, and daily target.
     */
    fun recommendSuggestionsForGoals(goals: Set<String>): List<HabitSuggestion> {
        val byTitle = predefinedSuggestions.associateBy { it.title }
        val result = linkedSetOf<HabitSuggestion>()

        fun addIfPresent(title: String) {
            byTitle[title]?.let { result.add(it) }
        }

        if ("Health & Hydration" in goals) {
            addIfPresent("Drink Water")
            addIfPresent("Walking")
        }
        if ("Fitness & Energy" in goals) {
            addIfPresent("Exercise")
            addIfPresent("Walking")
        }
        if ("Mindfulness & Calm" in goals) {
            addIfPresent("Meditation")
            addIfPresent("Journaling")
        }
        if ("Deep Focus & Productivity" in goals) {
            addIfPresent("Coding Practice")
            addIfPresent("Wake Up Early")
        }
        if ("Continuous Learning" in goals) {
            addIfPresent("Read Books")
            addIfPresent("Study")
        }
        if ("Better Sleep Rhythm" in goals) {
            addIfPresent("Sleep Early")
            addIfPresent("Journaling")
        }

        // Ensure 3 to 5 balanced recommendations even if only 1 goal or none were selected
        val fallbackOrder = listOf(
            "Drink Water",
            "Read Books",
            "Meditation",
            "Exercise",
            "Sleep Early"
        )
        for (title in fallbackOrder) {
            if (result.size >= 3) break
            addIfPresent(title)
        }

        return result.take(5)
    }

    val motivationalQuotes: List<Pair<String, String>> = listOf(
        "Small daily improvements over time lead to stunning results." to "Robin Sharma",
        "We are what we repeatedly do. Excellence, then, is not an act, but a habit." to "Aristotle",
        "You do not rise to the level of your goals. You fall to the level of your systems." to "James Clear",
        "Success is the sum of small efforts, repeated day in and day out." to "Robert Collier",
        "Motivation gets you started. Habit keeps you going." to "Jim Ryun",
        "Consistency is harder when no one is clapping for you. Keep going anyway." to "HabitFlow"
    )
}
