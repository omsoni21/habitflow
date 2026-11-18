package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.navigation.HabitFlowNavHost
import com.example.ui.theme.HabitFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefsState by preferencesRepository.userPreferences
                .map<UserPreferences, UserPreferences?> { it }
                .collectAsStateWithLifecycle(initialValue = null)

            val prefs = prefsState
            HabitFlowTheme(
                themeMode = prefs?.themeMode ?: com.example.ui.theme.ThemeMode.SYSTEM,
                accentPalette = prefs?.accentPalette ?: com.example.domain.usecase.AppAccentPalette.ELECTRIC_INDIGO
            ) {
                if (prefs == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    HabitFlowNavHost(onboardingCompleted = prefs.onboardingCompleted)
                }
            }
        }
    }
}
