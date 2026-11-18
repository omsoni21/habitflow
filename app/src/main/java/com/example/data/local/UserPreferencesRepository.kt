package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.StartOfWeek
import com.example.domain.usecase.AppAccentPalette
import com.example.domain.usecase.AppCrestIcon
import com.example.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "habitflow_prefs")

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val userName: String = "Alex",
    val selectedGoals: Set<String> = emptySet(),
    val preferredReminderTime: String = "08:30",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentPalette: AppAccentPalette = AppAccentPalette.ELECTRIC_INDIGO,
    val crestIcon: AppCrestIcon = AppCrestIcon.CLASSIC_BOLT,
    val lastCelebratedLevel: Int = 1,
    val notificationsEnabled: Boolean = true,
    val startOfWeek: StartOfWeek = StartOfWeek.MONDAY
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val USER_NAME = stringPreferencesKey("user_name")
        val SELECTED_GOALS = stringSetPreferencesKey("selected_goals")
        val PREFERRED_REMINDER_TIME = stringPreferencesKey("preferred_reminder_time")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val CREST_ICON = stringPreferencesKey("crest_icon")
        val LAST_CELEBRATED_LEVEL = intPreferencesKey("last_celebrated_level")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val START_OF_WEEK = stringPreferencesKey("start_of_week")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            userName = prefs[Keys.USER_NAME]?.ifBlank { "Alex" } ?: "Alex",
            selectedGoals = prefs[Keys.SELECTED_GOALS] ?: emptySet(),
            preferredReminderTime = prefs[Keys.PREFERRED_REMINDER_TIME] ?: "08:30",
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            accentPalette = runCatching {
                AppAccentPalette.valueOf(prefs[Keys.ACCENT_PALETTE] ?: AppAccentPalette.ELECTRIC_INDIGO.name)
            }.getOrDefault(AppAccentPalette.ELECTRIC_INDIGO),
            crestIcon = runCatching {
                AppCrestIcon.valueOf(prefs[Keys.CREST_ICON] ?: AppCrestIcon.CLASSIC_BOLT.name)
            }.getOrDefault(AppCrestIcon.CLASSIC_BOLT),
            lastCelebratedLevel = prefs[Keys.LAST_CELEBRATED_LEVEL] ?: 1,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            startOfWeek = runCatching {
                StartOfWeek.valueOf(prefs[Keys.START_OF_WEEK] ?: StartOfWeek.MONDAY.name)
            }.getOrDefault(StartOfWeek.MONDAY)
        )
    }

    suspend fun getPreferencesOnce(): UserPreferences = userPreferences.first()

    suspend fun saveOnboardingPreferences(
        userName: String,
        selectedGoals: Set<String>,
        preferredReminderTime: String,
        notificationsEnabled: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
            prefs[Keys.USER_NAME] = userName.trim().ifBlank { "Alex" }
            prefs[Keys.SELECTED_GOALS] = selectedGoals
            prefs[Keys.PREFERRED_REMINDER_TIME] = preferredReminderTime
            prefs[Keys.NOTIFICATIONS_ENABLED] = notificationsEnabled
        }
    }

    suspend fun updateUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name.trim().ifBlank { "Alex" }
        }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateAccentPalette(palette: AppAccentPalette) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCENT_PALETTE] = palette.name
        }
    }

    suspend fun updateCrestIcon(crestIcon: AppCrestIcon) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CREST_ICON] = crestIcon.name
        }
    }

    suspend fun updateLastCelebratedLevel(level: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_CELEBRATED_LEVEL] = level
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateStartOfWeek(startOfWeek: StartOfWeek) {
        context.dataStore.edit { prefs ->
            prefs[Keys.START_OF_WEEK] = startOfWeek.name
        }
    }
}
