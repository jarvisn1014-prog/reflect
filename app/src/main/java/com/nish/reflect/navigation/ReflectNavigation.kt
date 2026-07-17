package com.nish.reflect.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMirrored
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nish.reflect.ReflectViewModel
import com.nish.reflect.ui.screens.insights.InsightsScreen
import com.nish.reflect.ui.screens.journal_list.JournalListScreen
import com.nish.reflect.ui.screens.onboarding.OnboardingScreen
import com.nish.reflect.ui.screens.settings.SettingsScreen
import com.nish.reflect.ui.screens.today.TodayScreen

@Composable
fun ReflectNavHost() {
    val navController = rememberNavController()
    val viewModel: ReflectViewModel = viewModel()
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()

    val startDest = if (!hasSeenOnboarding) "onboarding" else "today"

    NavHost(navController = navController, startDestination = startDest) {

        composable("onboarding") {
            OnboardingScreen(
                onGetStarted = {
                    viewModel.setHasSeenOnboarding()
                    navController.navigate("today") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("today") {
            TodayScreen(
                viewModel = viewModel,
                onOpenList = { navController.navigate("journal_list") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }

        composable("journal_list") {
            JournalListScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("insights") {
            InsightsScreen(viewModel = viewModel)
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}