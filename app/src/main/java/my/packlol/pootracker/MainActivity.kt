package my.packlol.pootracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.PopUpToBuilder
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.packlol.pootracker.local.UserPrefs
import my.packlol.pootracker.local.UserTheme
import my.packlol.pootracker.ui.MainUiState
import my.packlol.pootracker.ui.MainVM
import my.packlol.pootracker.ui.navigation.AppNavigator
import my.packlol.pootracker.ui.navigation.Screen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.log


class MainActivity : ComponentActivity(), KoinComponent {

    private val networkMonitor by inject<NetworkMonitor>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val mainVM = koinViewModel<MainVM>()
            val mainUiState by mainVM.mainUiState.collectAsState()

            when (mainUiState) {
                MainUiState.Loading -> {

                }
                is MainUiState.Success -> {
                    PoopApp(mainUiState = mainUiState as? MainUiState.Success ?: return@setContent)
                }
            }
        }
    }

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
            loggedIn = mainUiState.loggedIn,
            snackbarHostState = snackbarHostState
        )

        val connected by poopAppState.connected.collectAsState()

        LaunchedEffect(poopAppState) {
            delay(3000)
            var prevConnected = connected
            snapshotFlow { connected }.collect { connected ->
                if (!connected) {
                    prevConnected = false
                    poopAppState.showSnackbar(
                        message = "not connected to a network",
                        duration = SnackbarDuration.Short
                    )
                } else {
                    if (prevConnected) { return@collect }
                    poopAppState.showSnackbar(
                        message = "reconnected to a network",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Surface(
                Modifier.padding(paddingValues)
            ) {
                AppNavigator(poopAppState = poopAppState)
            }
        }
    }
}

@Composable
fun rememberPoopAppState(
    networkMonitor: NetworkMonitor,
    windowSizeClass: WindowSizeClass,
    userPrefs: UserPrefs,
    loggedIn: Boolean,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState,
): PoopAppState {

    LaunchedEffect(loggedIn) {
        var signedInBefore = false
        snapshotFlow { loggedIn }.collect { loggedIn ->
            if (loggedIn) {
                signedInBefore = true
                return@collect
            }
            if (userPrefs.useOffline || !signedInBefore) {
                return@collect
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "logged out",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    return remember(
        networkMonitor,
        windowSizeClass,
        userPrefs,
        loggedIn,
        coroutineScope,
        navController,
        snackbarHostState
    ) {
        PoopAppState(
            windowSizeClass,
            networkMonitor,
            userPrefs,
            loggedIn,
            coroutineScope,
            navController,
            snackbarHostState
        )
    }
}


@Stable
class PoopAppState(
    private val windowSizeClass: WindowSizeClass,
    networkMonitor: NetworkMonitor,
    private val userPrefs: UserPrefs,
    val loggedIn: Boolean,
    private val coroutineScope: CoroutineScope,
    val navController: NavHostController,
    private val snackbarHostState: SnackbarHostState
) {
    val onboarded = userPrefs.onboarded

    val darkTheme: Boolean
        @Composable get() = userPrefs.theme == UserTheme.DarkTheme || (userPrefs.theme == UserTheme.DeviceTheme && isSystemInDarkTheme())

    val shouldShowBottomBar: Boolean
        get() = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val shouldShowNavRail: Boolean
        get() = !shouldShowBottomBar

    private val shouldShowLogInScreen: Boolean
        get() = !loggedIn && !userPrefs.useOffline

    val startRoute: String
        get () = if (shouldShowLogInScreen) { Screen.Auth.route }
        else { Screen.Home.route }

    val connected = networkMonitor.isOnline
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun showSnackbar(
        message: String,
        duration: SnackbarDuration
    ) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = duration
            )
        }
    }
}



