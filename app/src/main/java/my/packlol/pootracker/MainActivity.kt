package my.packlol.pootracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import my.packlol.pootracker.ui.MainUiState
import my.packlol.pootracker.ui.MainVM
import my.packlol.pootracker.ui.navigation.AppNavigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class MainActivity : ComponentActivity(), KoinComponent {

    private val networkMonitor by inject<NetworkMonitor>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val mainVM = koinViewModel<MainVM>()

            WindowCompat.setDecorFitsSystemWindows(window, false)

            when (val mainUiState = mainVM.mainUiState.collectAsState().value) {
                MainUiState.Loading -> {}
                is MainUiState.Success -> {
                    PoopApp(mainUiState)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PoopApp(
        mainUiState: MainUiState.Success
    ) {
        @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
        val windowSizeClass = calculateWindowSizeClass(activity = this)

        val snackbarHostState = remember { SnackbarHostState() }

        val poopAppState = rememberPoopAppState(
            networkMonitor = networkMonitor,
            windowSizeClass = windowSizeClass,
            userPrefs = mainUiState.userPrefs,
            authState = mainUiState.authState,
            snackbarHostState = snackbarHostState
        )
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {

                    }
                )
            }
        ) { paddingValues ->
            Surface(
                Modifier.padding(paddingValues)
            ) {
                AppNavigator(poopAppState = poopAppState)
            }
        }
    }
}





