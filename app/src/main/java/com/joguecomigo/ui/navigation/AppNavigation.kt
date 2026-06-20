package com.joguecomigo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.joguecomigo.ui.screens.CheckersScreen
import com.joguecomigo.ui.screens.HomeScreen
import com.joguecomigo.ui.screens.ProgressScreen
import com.joguecomigo.ui.screens.SettingsScreen
import com.joguecomigo.ui.screens.SudokuScreen

/** Rotas de navegação do aplicativo. */
object Routes {
    const val HOME = "home"
    const val SUDOKU = "sudoku"
    const val CHECKERS = "checkers"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
}

/** Grafo de navegação principal do "Jogue Comigo". */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onPlaySudoku = { navController.navigate(Routes.SUDOKU) },
                onPlayCheckers = { navController.navigate(Routes.CHECKERS) },
                onOpenProgress = { navController.navigate(Routes.PROGRESS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SUDOKU) {
            SudokuScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CHECKERS) {
            CheckersScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PROGRESS) {
            ProgressScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
