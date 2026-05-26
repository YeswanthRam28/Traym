package com.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.theme.AIGymTrackerTheme
import com.gymtracker.ui.theme.AppBlack
import com.gymtracker.auth.SessionManager
import com.gymtracker.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        SessionManager.init(this)
        SessionManager.authToken?.let { com.gymtracker.network.ApiClient.setAuthToken(it) }
        
        // Draw edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            AIGymTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppBlack
                ) {
                    GymTrackerApp()
                }
            }
        }
    }
}

@Composable
fun GymTrackerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    
    // Manage status bar icon color based on current screen
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            val isLightStatusBar = currentRoute == "splash" || currentRoute == "summary"
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightStatusBar
        }
    }

    val onNavigateToBottomTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val startRoute = "home"

    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {
        composable("home") {
            HomeScreen(
                currentRoute = currentRoute,
                onNavigate = onNavigateToBottomTab
            )
        }
        composable("workout") {
            WorkoutSplitScreen(
                currentRoute = currentRoute,
                onNavigate = onNavigateToBottomTab
            )
        }
        composable("summary") {
            WorkoutSummaryScreen(onDone = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = false }
                }
            })
        }
        composable("chat") {
            AiChatScreen(
                currentRoute = currentRoute,
                onNavigate = onNavigateToBottomTab
            )
        }
        composable("progress") {
            ProgressScreen(
                currentRoute = currentRoute,
                onNavigate = onNavigateToBottomTab
            )
        }
        composable("profile") {
            ProfileScreen(
                currentRoute = currentRoute,
                onNavigate = onNavigateToBottomTab
            )
        }
        composable("explore") {
            ExploreScreen(
                currentRoute = currentRoute,
                onNavigate = onNavigateToBottomTab
            )
        }
    }
}
