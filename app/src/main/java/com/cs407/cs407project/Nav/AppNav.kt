package com.cs407.cs407project.navigation

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cs407.cs407project.R
import com.cs407.cs407project.data.GymRivalsCloudRepository
import com.cs407.cs407project.ui.GymRivalsHomeScreen
import com.cs407.cs407project.ui.repcounter.RepCounterScreen
import com.cs407.cs407project.ui.settings.SettingsScreen
import com.cs407.cs407project.ui.strength.StrengthWorkoutScreen
import com.cs407.cs407project.ui.tabs.LogScreen
import com.cs407.cs407project.ui.tabs.ProfileScreen
import com.cs407.cs407project.ui.tabs.ProgressScreen
import com.cs407.cs407project.ui.tabs.RivalsScreen
import com.cs407.cs407project.ui.track.TrackRunScreen
import com.example.gymrivals.ui.GymRivalsLoginScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.androidgamesdk.gametextinput.Settings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

// ---------------- ROUTES ----------------

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
    const val Settings = "settings"
}

// route -> (emoji to label)
private val BottomItems = listOf(
    Routes.Home to ("🏠" to "Home"),
    Routes.Log to ("📝" to "Log"),
    Routes.Progress to ("📈" to "Progress"),
    Routes.Rivals to ("🤼" to "Rivals"),
    Routes.Profile to ("👤" to "Profile")
)

// ---------------- ROOT APP NAV ----------------

@Composable
fun AppNav(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    // ---- Google Sign-In config ----
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // Launcher for Google Sign-In
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            auth.signInWithCredential(credential)
                .addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        // Save/update Firestore user profile
                        GymRivalsCloudRepository.saveBasicProfile()

                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Login) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        Log.e(
                            "AppNav",
                            "Firebase signInWithCredential failed",
                            signInTask.exception
                        )
                    }
                }
        } catch (e: ApiException) {
            Log.e("AppNav", "Google sign-in failed", e)
        }
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        NavHost(
            navController = navController,
            startDestination = Routes.Login
        ) {
            // ---------- LOGIN (no bottom bar) ----------
            composable(Routes.Login) {
                GymRivalsLoginScreen(
                    onLogin = { _, _, _ ->
                        // Email/password flow is handled inside the screen and then calls this.
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Login) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoogleLogin = {
                        launcher.launch(googleSignInClient.signInIntent)
                    }
                )
            }

            // ---------- TAB DESTINATIONS (with bottom nav) ----------

            tabDestination(Routes.Home, currentRoute, navController) {
                // Use your dynamic home version here; if yours takes no args, just:
                GymRivalsHomeScreen()
            }

            tabDestination(Routes.Log, currentRoute, navController) {
                LogScreen(
                    onTrackRun = { navController.navigate(Routes.TrackRun) },
                    onAddStrength = { navController.navigate(Routes.Strength) },
                    onRepCounter = { navController.navigate(Routes.RepCounter) }
                )
            }

            tabDestination(Routes.Progress, currentRoute, navController) {
                ProgressScreen()
            }

            tabDestination(Routes.Rivals, currentRoute, navController) {
                RivalsScreen()
            }

            tabDestination(Routes.Profile, currentRoute, navController) {
                ProfileScreen(
                    onSettingsClick = { navController.navigate(Routes.Settings) }
                )
            }

            // ---------- STANDALONE SCREENS (no bottom nav) ----------

            composable(Routes.TrackRun) {
                TrackRunScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.Strength) {
                StrengthWorkoutScreen(
                    onBack = { navController.popBackStack() },
                    onSubmit = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.RepCounter) {
                RepCounterScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        // If you’re using GoogleSignInClient, you can also call googleSignInClient.signOut()
                        navController.navigate(Routes.Login) {
                            popUpTo(Routes.Home) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

// ---------------- TAB SCAFFOLD HELPERS ----------------

private fun NavGraphBuilder.tabDestination(
    route: String,
    selectedRoute: String?,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    composable(route) {
        val current = selectedRoute ?: route
        val selectedIndex = BottomItems.indexOfFirst { it.first == current }
            .takeIf { it >= 0 } ?: 0

        TabScaffold(
            items = BottomItems.map { it.second },
            selectedIndex = selectedIndex,
            onSelect = { index ->
                val dest = BottomItems[index].first
                if (dest != current) {
                    navController.navigate(dest) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(BottomItems.first().first) { saveState = true }
                    }
                }
            }
        ) {
            content()
        }
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
        Column(Modifier.weight(1f)) {
            content()
        }
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
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (emoji, label) ->
                val selected = index == selectedIndex
                val bg = if (selected) Color(0xFFEEF2FF) else Color.Transparent
                val textColor = if (selected) Color(0xFF4F46E5) else Color(0xFF6B7280)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(bg)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}