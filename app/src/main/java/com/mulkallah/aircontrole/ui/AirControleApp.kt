package com.mulkallah.aircontrole.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mulkallah.aircontrole.core.prefs.AirControlePreferences
import com.mulkallah.aircontrole.ui.home.HomeScreen
import com.mulkallah.aircontrole.ui.navigation.Destinations
import com.mulkallah.aircontrole.ui.onboarding.OnboardingPage
import com.mulkallah.aircontrole.ui.onboarding.OnboardingScreen
import com.mulkallah.aircontrole.ui.onboarding.ReadyScreen
import com.mulkallah.aircontrole.ui.onboarding.WelcomeScreen
import com.mulkallah.aircontrole.ui.picker.AppPickerScreen
import com.mulkallah.aircontrole.ui.customize.CustomizeGesturesScreen
import com.mulkallah.aircontrole.ui.settings.SettingsScreen
import com.mulkallah.aircontrole.ui.training.GestureTrainingScreen

@Composable
fun AirControleApp(preferences: AirControlePreferences) {
    val onboardingDone by preferences.onboardingComplete.collectAsState(initial = false)
    val navController = rememberNavController()
    val start = if (onboardingDone) Destinations.Home else Destinations.Welcome

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            NavHost(navController = navController, startDestination = start) {
                composable(Destinations.Welcome) {
                    WelcomeScreen(onStart = { navController.navigate(Destinations.Camera) })
                }
                composable(Destinations.Camera) {
                    OnboardingScreen(
                        page = OnboardingPage.Camera,
                        onNext = { navController.navigate(Destinations.Accessibility) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Destinations.Accessibility) {
                    OnboardingScreen(
                        page = OnboardingPage.Accessibility,
                        onNext = { navController.navigate(Destinations.Overlay) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Destinations.Overlay) {
                    OnboardingScreen(
                        page = OnboardingPage.Overlay,
                        onNext = { navController.navigate(Destinations.Notifications) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Destinations.Notifications) {
                    OnboardingScreen(
                        page = OnboardingPage.Notifications,
                        onNext = { navController.navigate(Destinations.Ready) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Destinations.Ready) {
                    ReadyScreen(
                        onDone = {
                            navController.navigate(Destinations.Home) {
                                popUpTo(Destinations.Welcome) { inclusive = true }
                            }
                        },
                        preferences = preferences,
                    )
                }
                composable(Destinations.Home) {
                    HomeScreen(
                        preferences = preferences,
                        onSettings = { navController.navigate(Destinations.Settings) },
                        onTraining = { navController.navigate(Destinations.Training) },
                        onCustomize = { navController.navigate(Destinations.Customize) },
                        onAddApp = { navController.navigate(Destinations.AppPicker) },
                    )
                }
                composable(Destinations.Settings) {
                    SettingsScreen(
                        preferences = preferences,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Destinations.Training) {
                    GestureTrainingScreen(
                        preferences = preferences,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Destinations.Customize) {
                    CustomizeGesturesScreen(
                        preferences = preferences,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Destinations.AppPicker) {
                    AppPickerScreen(
                        preferences = preferences,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
