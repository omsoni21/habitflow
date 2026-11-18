package com.example.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.presentation.achievements.AchievementsScreen
import com.example.presentation.calendar.CalendarScreen
import com.example.presentation.components.ConfettiParticleOverlay
import com.example.presentation.habits.AddEditHabitScreen
import com.example.presentation.habits.HabitDetailScreen
import com.example.presentation.habits.HabitsScreen
import com.example.presentation.home.HomeScreen
import com.example.presentation.onboarding.OnboardingScreen
import com.example.presentation.profile.ProfileScreen
import com.example.presentation.rewards.RewardsScreen
import com.example.presentation.statistics.StatisticsScreen
import kotlinx.serialization.Serializable

@Serializable object OnboardingRoute
@Serializable object HomeRoute
@Serializable object HabitsRoute
@Serializable object CalendarRoute
@Serializable object StatisticsRoute
@Serializable object ProfileRoute
@Serializable data class AddEditHabitRoute(val habitId: Long = -1L)
@Serializable data class HabitDetailRoute(val habitId: Long)
@Serializable object AchievementsRoute
@Serializable object RewardsRoute

private data class TopLevelDestination(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val topLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    TopLevelDestination(HabitsRoute, R.string.nav_habits, Icons.Rounded.CheckCircle, Icons.Outlined.CheckCircle, "nav_habits"),
    TopLevelDestination(CalendarRoute, R.string.nav_calendar, Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_calendar"),
    TopLevelDestination(StatisticsRoute, R.string.nav_statistics, Icons.Rounded.BarChart, Icons.Outlined.BarChart, "nav_statistics"),
    TopLevelDestination(ProfileRoute, R.string.nav_profile, Icons.Rounded.Person, Icons.Outlined.Person, "nav_profile")
)

@Composable
fun HabitFlowNavHost(
    onboardingCompleted: Boolean,
    celebrationViewModel: CelebrationOverlayViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val activeCelebration by celebrationViewModel.activeCelebration.collectAsStateWithLifecycle()

    val isTopLevel = topLevelDestinations.any { dest ->
        currentDestination?.hierarchy?.any { it.hasRoute(dest.route::class) } == true
    }

    val useNavigationRail = LocalConfiguration.current.screenWidthDp >= 600

    val startDestination: Any = if (onboardingCompleted) HomeRoute else OnboardingRoute

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (isTopLevel && !useNavigationRail) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(dest.route::class)
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = stringResource(dest.labelRes)
                                )
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                            modifier = Modifier.testTag(dest.tag)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isTopLevel) {
                FloatingActionButton(
                    onClick = { navController.navigate(AddEditHabitRoute(habitId = -1L)) },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_habit")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Create new habit")
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isTopLevel && useNavigationRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(dest.route::class)
                        } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = stringResource(dest.labelRes)
                                )
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                            modifier = Modifier.testTag(dest.tag)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable<OnboardingRoute> {
                        OnboardingScreen(
                            onFinished = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<OnboardingRoute> { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<HomeRoute> {
                        HomeScreen(
                            onNavigateToAddHabit = {
                                navController.navigate(AddEditHabitRoute(habitId = -1L))
                            },
                            onNavigateToHabitDetail = { habitId ->
                                navController.navigate(HabitDetailRoute(habitId))
                            },
                            onNavigateToAchievements = {
                                navController.navigate(AchievementsRoute)
                            },
                            onNavigateToRewards = {
                                navController.navigate(RewardsRoute)
                            },
                            handleCelebrationExternally = true
                        )
                    }

                    composable<HabitsRoute> {
                        HabitsScreen(
                            onNavigateToAddHabit = {
                                navController.navigate(AddEditHabitRoute(habitId = -1L))
                            },
                            onNavigateToEditHabit = { habitId ->
                                navController.navigate(AddEditHabitRoute(habitId))
                            },
                            onNavigateToHabitDetail = { habitId ->
                                navController.navigate(HabitDetailRoute(habitId))
                            }
                        )
                    }

                    composable<CalendarRoute> {
                        CalendarScreen()
                    }

                    composable<StatisticsRoute> {
                        StatisticsScreen(
                            onNavigateToAddHabit = {
                                navController.navigate(AddEditHabitRoute(habitId = -1L))
                            }
                        )
                    }

                    composable<ProfileRoute> {
                        ProfileScreen(
                            onNavigateToAchievements = {
                                navController.navigate(AchievementsRoute)
                            },
                            onNavigateToRewards = {
                                navController.navigate(RewardsRoute)
                            }
                        )
                    }

                    composable<AddEditHabitRoute> {
                        AddEditHabitScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<HabitDetailRoute> {
                        HabitDetailScreen(
                            onBack = { navController.popBackStack() },
                            onEdit = { habitId ->
                                navController.navigate(AddEditHabitRoute(habitId))
                            }
                        )
                    }

                    composable<AchievementsRoute> {
                        AchievementsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<RewardsRoute> {
                        RewardsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
        }

        ConfettiParticleOverlay(
            celebration = activeCelebration,
            onDismiss = celebrationViewModel::dismiss
        )
    }
}
