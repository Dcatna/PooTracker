package my.packlol.pootracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import my.packlol.pootracker.repository.AuthState
import my.packlol.pootracker.ui.MainUiState
import my.packlol.pootracker.ui.MainVM
import my.packlol.pootracker.ui.navigation.AppNavigator
import my.packlol.pootracker.ui.navigation.Screen
import my.packlol.pootracker.ui.shared.PullRefreshIndicator
import my.packlol.pootracker.ui.shared.pullRefresh
import my.packlol.pootracker.ui.shared.rememberPullRefreshState
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
                    PoopApp(
                        state = mainUiState,
                        logout = {
                            mainVM.logout()
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PoopApp(
        state: MainUiState.Success,
        logout: () -> Unit,
    ) {
        @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
        val windowSizeClass = calculateWindowSizeClass(activity = this)

        val snackbarHostState = remember { SnackbarHostState() }

        val poopAppState = rememberPoopAppState(
            networkMonitor = networkMonitor,
            windowSizeClass = windowSizeClass,
            userPrefs = state.userPrefs,
            authState = state.authState,
            snackbarHostState = snackbarHostState
        )
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        when(val poopState = poopAppState.authState) {
                            is AuthState.LoggedIn -> Text("logged in as ${poopState.user.name}")
                            AuthState.LoggedOut -> Text("signed out")
                            AuthState.Offline -> Text("using offline")
                        }
                    },
                    actions = {
                        when(poopAppState.authState) {
                            is AuthState.LoggedIn -> IconButton(onClick = { logout() }) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "logout"
                                )
                            }
                            AuthState.LoggedOut -> Text("signed out")
                            AuthState.Offline -> IconButton(onClick = { poopAppState.navController.navigate(Screen.Auth.route) }) {
                                Icon(
                                    imageVector = Icons.Default.Login,
                                    contentDescription = "login"
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Surface(
                Modifier
                    .padding(paddingValues)
            ) {
                AppNavigator(poopAppState = poopAppState)
            }
        }
    }
}


@Composable
fun PullRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    indicatorOffset: Dp = 0.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = MaterialTheme.colorScheme.onSecondary,
    content: @Composable () -> Unit,
) {
    val state = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = onRefresh,
        refreshingOffset = indicatorOffset,
    )


    Box(Modifier.pullRefresh(state, !refreshing)) {
        content()

        Box(
            Modifier
                .matchParentSize()
                .clipToBounds(),
        ) {
            PullRefreshIndicator(
                refreshing = refreshing,
                state = state,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = backgroundColor,
                contentColor = contentColor,
            )
        }
    }
}





