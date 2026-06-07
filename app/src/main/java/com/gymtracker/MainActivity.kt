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
import com.gymtracker.network.NotionOAuthManager
import com.gymtracker.ui.screens.*
import com.gymtracker.ui.viewmodels.ProgressViewModel
import com.gymtracker.ui.viewmodels.WorkoutSplitViewModel
import android.content.Intent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        SessionManager.init(this)
        SessionManager.authToken?.let { com.gymtracker.network.ApiClient.setAuthToken(it) }
        
        handleIntent(intent)
        
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        val data = intent?.data

        if (Intent.ACTION_VIEW == action && data != null) {
            if (data.scheme == "traym" && data.host == "notion-auth") {
                val code = data.getQueryParameter("code")
                if (code != null) {
                    NotionOAuthManager.handleAuthCode(code)
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
    
    val splitViewModel: WorkoutSplitViewModel = viewModel()
    
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

    val onSmartNavigate: (String) -> Unit = { route ->
        if (route.startsWith("statDetail") || route.startsWith("explore?dayIndex")) {
            navController.navigate(route)
        } else if (route == "pop") {
            navController.popBackStack()
        } else {
            onNavigateToBottomTab(route)
        }
    }

    val startRoute = if (SessionManager.authToken != null) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                currentRoute = currentRoute,
                onNavigate = onSmartNavigate
            )
        }
        composable("workout") {
            WorkoutSplitScreen(
                currentRoute = currentRoute,
                onNavigate = onSmartNavigate,
                viewModel = splitViewModel
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
                onNavigate = onSmartNavigate
            )
        }
        composable("community") {
            CommunityScreen(
                currentRoute = currentRoute,
                onNavigate = onSmartNavigate
            )
        }
        composable("statDetail/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "muscles"
            StatDetailScreen(
                type = type,
                onBack = { navController.popBackStack() },
                viewModel = viewModel()
            )
        }
        composable("profile") {
            ProfileScreen(
                currentRoute = currentRoute,
                onNavigate = onSmartNavigate
            )
        }
        composable("explore?dayIndex={dayIndex}") { backStackEntry ->
            val dayIndexStr = backStackEntry.arguments?.getString("dayIndex")
            val dayIndex = dayIndexStr?.toIntOrNull()
            ExploreScreen(
                currentRoute = currentRoute,
                onNavigate = onSmartNavigate,
                selectionDayIndex = dayIndex,
                splitViewModel = splitViewModel
            )
        }
    }
}
