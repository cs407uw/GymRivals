package com.cs407.cs407project.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.cs407.cs407project.ui.GymRivalsHomeScreen
import com.cs407.cs407project.ui.tabs.LogScreen
import com.cs407.cs407project.ui.tabs.ProgressScreen
import com.cs407.cs407project.ui.tabs.RivalsScreen
import com.cs407.cs407project.ui.tabs.ProfileScreen
import com.cs407.cs407project.ui.track.TrackRunScreen
import com.example.gymrivals.ui.GymRivalsLoginScreen
import com.cs407.cs407project.ui.strength.StrengthWorkoutScreen
import com.cs407.cs407project.ui.repcounter.RepCounterScreen

private object Routes {
    const val Login = "login"
    const val Home = "home"
    const val Log = "log"
    const val Progress = "progress"
    const val Rivals = "rivals"
    const val Profile = "profile"
    const val TrackRun = "track_run"
    const val Strength = "strength_workout"
    const val RepCounter = "rep_counter"
}

private val BottomItems = listOf(
    Routes.Home to ("🏠" to "Home"),
    Routes.Log to ("📝" to "Log"),
    Routes.Progress to ("📈" to "Progress"),
    Routes.Rivals to ("🤼" to "Rivals"),
    Routes.Profile to ("👤" to "Profile")
)

@Composable
fun AppNav(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Surface(color = MaterialTheme.colorScheme.background) {
        NavHost(navController, startDestination = Routes.Login) {

            // Login (no bottom bar)
            composable(Routes.Login) {
                GymRivalsLoginScreen(
                    onLogin = { _, _, _ ->
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Login) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoogleLogin = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Login) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Tab destinations (share the bottom bar)
            tabDestination(Routes.Home, currentRoute, navController) {
                GymRivalsHomeScreen(userName = "Alex")
            }
            tabDestination(Routes.Log, currentRoute, navController) {
                LogScreen(
                    onTrackRun = { navController.navigate(Routes.TrackRun) },
                    onAddStrength = { navController.navigate(Routes.Strength) },
                    onRepCounter = { navController.navigate(Routes.RepCounter) }
                )
            }
            tabDestination(Routes.Progress, currentRoute, navController) { ProgressScreen() }
            tabDestination(Routes.Rivals, currentRoute, navController) { RivalsScreen() }
            tabDestination(Routes.Profile, currentRoute, navController) { ProfileScreen() }

            // Track Run (no bottom bar, has back)
            composable(Routes.TrackRun) {
                TrackRunScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.Strength) {
                StrengthWorkoutScreen(
                    onBack = { navController.popBackStack() },
                    onSubmit = { savedWorkout ->
                        // TODO: show a toast/snackbar if you want
                        navController.popBackStack()
                    }
                )
            }

            // Rep Counter (no bottom bar, has back)
            composable(Routes.RepCounter) {
                RepCounterScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** Helper to wrap a route with the shared bottom bar scaffold */
private fun NavGraphBuilder.tabDestination(
    route: String,
    selectedRoute: String?,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    composable(route) {
        val selectedIndex = BottomItems.indexOfFirst { it.first == (selectedRoute ?: route) }
            .coerceAtLeast(0)
        TabScaffold(
            items = BottomItems.map { it.second },
            selectedIndex = selectedIndex,
            onSelect = { index ->
                val dest = BottomItems[index].first
                if (dest != selectedRoute) {
                    navController.navigate(dest) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(BottomItems.first().first) { saveState = true }
                    }
                }
            }
        ) { content() }
    }
}

@Composable
private fun TabScaffold(
    items: List<Pair<String, String>>, // emoji to label
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) { content() }
        BottomEmojiNav(items, selectedIndex, onSelect)
    }
}

@Composable
private fun BottomEmojiNav(
    items: List<Pair<String, String>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Color(0xFFE8ECF3))
            .padding(vertical = 6.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items.forEachIndexed { index, (emoji, label) ->
                val selected = index == selectedIndex
                val pill = if (selected) Color(0xFFEFF6FF) else Color.Transparent
                val txt = if (selected) Color(0xFF2563EB) else Color(0xFF6B7280)
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(pill)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            onSelect(index)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(emoji, fontSize = 16.sp)
                    Text(label, fontSize = 11.sp, color = txt, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}