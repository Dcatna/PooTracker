package my.packlol.pootracker

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.packlol.pootracker.local.UserPrefs
import my.packlol.pootracker.local.UserTheme
import my.packlol.pootracker.repository.AuthState
import my.packlol.pootracker.ui.navigation.Screen

@Composable
fun rememberPoopAppState(
    networkMonitor: NetworkMonitor,
    windowSizeClass: WindowSizeClass,
    userPrefs: UserPrefs,
    authState: AuthState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState,
): PoopAppState {

    return remember(
        networkMonitor,
        windowSizeClass,
        userPrefs,
        authState,
        coroutineScope,
        navController,
        snackbarHostState
    ) {
        PoopAppState(
            windowSizeClass,
            networkMonitor,
            userPrefs,
            authState,
            coroutineScope,
            navController,
            snackbarHostState
        )
    }
}


@OptIn(FlowPreview::class)
@Stable
class PoopAppState(
    val windowSizeClass: WindowSizeClass,
    networkMonitor: NetworkMonitor,
    private val userPrefs: UserPrefs,
    val authState: AuthState,
    private val coroutineScope: CoroutineScope,
    val navController: NavHostController,
    private val snackbarHostState: SnackbarHostState
) {
    val darkTheme: Boolean
        @Composable get() = userPrefs.darkThemePreference == UserTheme.DarkTheme || (userPrefs.darkThemePreference == UserTheme.DeviceTheme && isSystemInDarkTheme())

    val shouldShowBottomBar: Boolean
        get() = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val shouldShowNavRail: Boolean
        get() = !shouldShowBottomBar

    private val shouldShowLogInScreen: Boolean
        get() = authState !is AuthState.LoggedIn && authState !is AuthState.Offline

    val startRoute: String
        get () = if (shouldShowLogInScreen) { Screen.Auth.route }
        else { Screen.Home.route }

    val connected = networkMonitor.isOnline
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    suspend fun showSnackbarWithAction(
        message: String,
        duration: SnackbarDuration,
        actionLabel: String? = null,
        withDismissAction: Boolean = false
    ) = snackbarHostState.showSnackbar(
        message = message,
        duration = duration,
        actionLabel = actionLabel,
        withDismissAction = withDismissAction
    )



    fun showSnackbar(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
        actionLabel: String? = null,
        withDismissAction: Boolean = false
    ) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = duration,
                actionLabel = actionLabel,
                withDismissAction = withDismissAction,
            )
        }
    }
}