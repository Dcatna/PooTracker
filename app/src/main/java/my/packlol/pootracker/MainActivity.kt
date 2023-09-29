package my.packlol.pootracker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import my.packlol.pootracker.local.UserPrefs
import my.packlol.pootracker.local.UserTheme
import my.packlol.pootracker.repository.AuthState
import my.packlol.pootracker.ui.MainUiState
import my.packlol.pootracker.ui.MainVM
import my.packlol.pootracker.ui.navigation.AppNavigator
import my.packlol.pootracker.ui.navigation.Screen
import my.packlol.pootracker.ui.shared.PullRefreshIndicator
import my.packlol.pootracker.ui.shared.pullRefresh
import my.packlol.pootracker.ui.shared.rememberPullRefreshState
import my.packlol.pootracker.ui.theme.PooTrackerTheme
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
                        logout = mainVM::logout,
                        onChangeDynamicColorPreference = mainVM::changeDynamicThemePref,
                        onChangeDarkThemeConfig = mainVM::changeDarkThemePref
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
    @Composable
    fun PoopApp(
        state: MainUiState.Success,
        logout: () -> Unit,
        onChangeDarkThemeConfig: (darkThemeConfig: UserTheme) -> Unit,
        onChangeDynamicColorPreference: (useDynamicColor: Boolean) -> Unit
    ) {
        @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
        val windowSizeClass = calculateWindowSizeClass(activity = this)

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            var signedInBefore = false
            snapshotFlow { state.authState }.collect { authState ->
                if (authState is AuthState.LoggedIn) {
                    signedInBefore = true
                    return@collect
                }
                if (state.userPrefs.useOffline || !signedInBefore) {
                    return@collect
                }
                snackbarHostState.showSnackbar(
                    message = "logged out",
                    duration = SnackbarDuration.Short
                )
            }
        }

        val poopAppState = rememberPoopAppState(
            networkMonitor = networkMonitor,
            windowSizeClass = windowSizeClass,
            userPrefs = state.userPrefs,
            authState = state.authState,
            snackbarHostState = snackbarHostState
        )

        val connected by poopAppState.connected.collectAsState()

        LaunchedEffect(Unit) {
            var prevConnected = true
            snapshotFlow { connected }
                .debounce(5_000)
                .collectLatest { connected ->
                    if (!connected) {
                        prevConnected = false
                        snackbarHostState.showSnackbar(
                            message = "not connected to a network",
                            duration = SnackbarDuration.Short
                        )
                    } else if (!prevConnected) {
                        prevConnected = true
                        snackbarHostState.showSnackbar(
                            message = "reconnected to a network",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
        }

        var settingsDialogVisible by rememberSaveable {
            mutableStateOf(false)
        }

        if (settingsDialogVisible) {
            SettingsDialog(
                userPrefs = state.userPrefs,
                onDismiss = { settingsDialogVisible = false },
                supportDynamicColor = supportsDynamicTheming(),
                onChangeDarkThemeConfig = onChangeDarkThemeConfig,
                onChangeDynamicColorPreference = onChangeDynamicColorPreference
            )
        }

        PooTrackerTheme(
            darkTheme = poopAppState.darkTheme,
            dynamicColor = state.userPrefs.useDynamicTheme
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            when(poopAppState.authState) {
                                is AuthState.LoggedIn -> IconButton(onClick = { logout() }) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "logout"
                                    )
                                }
                                AuthState.LoggedOut -> Text("signed out")
                                AuthState.Offline -> IconButton(onClick = { poopAppState.navController.navigate(
                                    Screen.Auth.route) }) {
                                    Icon(
                                        imageVector = Icons.Default.Login,
                                        contentDescription = "login"
                                    )
                                }
                            }
                        },
                        title = {
                            when(poopAppState.authState) {
                                is AuthState.LoggedIn -> Text("logged in")
                                AuthState.LoggedOut -> Text("signed out")
                                AuthState.Offline -> Text("using offline")
                            }
                        },
                        actions = {
                            IconButton(onClick = { settingsDialogVisible = !settingsDialogVisible }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "settings"
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Surface(
                    modifier = Modifier
                        .padding(paddingValues),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigator(poopAppState = poopAppState)
                }
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

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun SettingsDialog(
    userPrefs: UserPrefs,
    supportDynamicColor: Boolean = supportsDynamicTheming(),
    onDismiss: () -> Unit,
    onChangeDynamicColorPreference: (useDynamicColor: Boolean) -> Unit,
    onChangeDarkThemeConfig: (darkThemeConfig: UserTheme) -> Unit,
) {
    val configuration = LocalConfiguration.current

    /**
     * usePlatformDefaultWidth = false is use as a temporary fix to allow
     * height recalculation during recomposition. This, however, causes
     * Dialog's to occupy full width in Compact mode. Therefore max width
     * is configured below. This should be removed when there's fix to
     * https://issuetracker.google.com/issues/221643630
     */
    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(bottom = 32.dp).widthIn(max = configuration.screenWidthDp.dp - 80.dp),
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Divider()
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SettingsPanel(
                    settings = userPrefs,
                    supportDynamicColor = supportDynamicColor,
                    onChangeDynamicColorPreference = onChangeDynamicColorPreference,
                    onChangeDarkThemeConfig = onChangeDarkThemeConfig,
                )
                Divider(Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            Text(
                text = "Ok",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable { onDismiss() },
            )
        },
    )
}


@Composable
private fun ColumnScope.SettingsPanel(
    settings: UserPrefs,
    supportDynamicColor: Boolean,
    onChangeDynamicColorPreference: (useDynamicColor: Boolean) -> Unit,
    onChangeDarkThemeConfig: (darkThemeConfig: UserTheme) -> Unit,
) {
    SettingsDialogSectionTitle(text = "Theme")
    Column(Modifier.selectableGroup()) {
        SettingsDialogThemeChooserRow(
            text = "Default",
            selected = true,
            onClick = {  },
        )
        SettingsDialogThemeChooserRow(
            text = "Android",
            selected = false,
            onClick = { },
        )
    }
    AnimatedVisibility(visible = supportDynamicColor) {
        Column {
            SettingsDialogSectionTitle(text = "Use Dynamic Color")
            Column(Modifier.selectableGroup()) {
                SettingsDialogThemeChooserRow(
                    text = "Yes",
                    selected = settings.useDynamicTheme,
                    onClick = { onChangeDynamicColorPreference(true) },
                )
                SettingsDialogThemeChooserRow(
                    text = "No",
                    selected = !settings.useDynamicTheme,
                    onClick = { onChangeDynamicColorPreference(false) },
                )
            }
        }
    }
    SettingsDialogSectionTitle(text = "Dark mode preference")
    Column(Modifier.selectableGroup()) {
        SettingsDialogThemeChooserRow(
            text = "System Default",
            selected = settings.darkThemePreference == UserTheme.DeviceTheme,
            onClick = { onChangeDarkThemeConfig(UserTheme.DeviceTheme) },
        )
        SettingsDialogThemeChooserRow(
            text = "Light",
            selected = settings.darkThemePreference == UserTheme.LightTheme,
            onClick = { onChangeDarkThemeConfig(UserTheme.LightTheme) },
        )
        SettingsDialogThemeChooserRow(
            text = "Dark",
            selected = settings.darkThemePreference == UserTheme.DarkTheme,
            onClick = { onChangeDarkThemeConfig(UserTheme.DarkTheme) },
        )
    }
}

@Composable
fun SettingsDialogThemeChooserRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun SettingsDialogSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

