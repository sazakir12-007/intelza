package com.ht.intelza.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ht.intelza.ui.home.HomeScreen
import com.ht.intelza.ui.scan.TestCardsScreen

object Routes {
    const val HOME = "home"
    const val TEST_CARDS = "test-cards"
}

@Composable
fun IntelzaApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onTestCards = { navController.navigate(Routes.TEST_CARDS) })
        }
        composable(Routes.TEST_CARDS) {
            TestCardsScreen(onBack = { navController.popBackStack() })
        }
    }
}
