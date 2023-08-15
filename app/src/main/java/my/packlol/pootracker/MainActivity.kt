package my.packlol.pootracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import my.packlol.pootracker.ui.MainVM
import my.packlol.pootracker.ui.screens.PoopScreen
import my.packlol.pootracker.ui.screens.SettingsScreen
import my.packlol.pootracker.ui.theme.PooTrackerTheme
import my.packlol.pootracker.ui.theme.composableFadeAnim
import org.koin.androidx.viewmodel.ext.android.getViewModel


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel = getViewModel<MainVM>()
            val lists by viewModel.juicerList.collectAsState()
            val timeSinceLastPoop by viewModel.timeSinceLastPoop.collectAsState()

            val navController = rememberNavController()

            PooTrackerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "poop_screen"
                    ) {
                        composableFadeAnim("poop_screen") {
                            PoopScreen(
                                logs = lists,
                                timeSinceLastPoop = timeSinceLastPoop,
                                onInsert = { hour, min, second ->
                                    viewModel.insert(hour, min ,second)
                                },
                                navigate = { route ->
                                    navController.navigate(route)
                                }
                            )
                        }
                        composableFadeAnim("settings") {
                            SettingsScreen { route ->
                                navController.navigate(route)
                            }
                        }
                    }
                }
            }
        }
    }
}



