package my.packlol.pootracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import my.packlol.pootracker.PoopAppState
import my.packlol.pootracker.ui.auth.AuthScreen
import my.packlol.pootracker.ui.home.HomeScreen
import my.packlol.pootracker.ui.theme.composableFadeAnim

sealed class Screen(val route: String) {
    data object Home: Screen("home")
    data object Auth: Screen("auth")
}


@Composable
fun AppNavigator(
    poopAppState: PoopAppState,
) {
    val navController = poopAppState.navController

    NavHost(
        navController = navController,
        startDestination = poopAppState.startRoute,
    ) {

        composableFadeAnim(Screen.Auth.route) {
            AuthScreen(poopAppState)
        }

        composableFadeAnim(Screen.Home.route) {
            HomeScreen { destination ->
                navController.navigate(destination.route)
            }
        }
    }
}