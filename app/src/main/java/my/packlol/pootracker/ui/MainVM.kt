package my.packlol.pootracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.UserPrefs
import my.packlol.pootracker.local.UserTheme
import my.packlol.pootracker.repository.AuthRepository
import my.packlol.pootracker.repository.AuthState
import my.packlol.pootracker.sync.FirebaseSyncManager
import my.packlol.pootracker.ui.MainUiState.Loading
import my.packlol.pootracker.ui.MainUiState.Success

class MainVM(
    private val dataStore: DataStore,
    private val authRepository: AuthRepository,
    private val firebaseSyncManager: FirebaseSyncManager,
) : ViewModel() {

    private val authState = authRepository.authState()
        .distinctUntilChanged()
        .onEach {
            if (it is AuthState.LoggedIn) {
                firebaseSyncManager.requestSync(collectionId = null)
            }
        }
    
    val mainUiState = combine(
        dataStore.userPrefs(),
        authState,
    ) { prefs, authState ->
        Success(prefs, authState)
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )


    fun changeDarkThemePref(darkThemePreference: UserTheme) {
        viewModelScope.launch {
            dataStore.updateUserPrefs { userPrefs ->
                userPrefs.copy(
                    darkThemePreference = darkThemePreference
                )
            }
        }
    }

    fun changeDynamicThemePref(useDynamicTheme: Boolean) {
        viewModelScope.launch {
            dataStore.updateUserPrefs { userPrefs ->
                userPrefs.copy(
                    useDynamicTheme = useDynamicTheme
                )
            }
        }
    }

    fun changeUseOfflinePref(useOffline: Boolean) {
        viewModelScope.launch {
            dataStore.updateUserPrefs { userPrefs ->
                userPrefs.copy(
                    useOffline = useOffline
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Success(
        val userPrefs: UserPrefs,
        val authState: AuthState,
    ) : MainUiState

    val success: Success?
        get() = this as? Success
}