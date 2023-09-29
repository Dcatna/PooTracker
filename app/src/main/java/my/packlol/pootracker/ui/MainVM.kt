package my.packlol.pootracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.UserPrefs
import my.packlol.pootracker.local.UserTheme
import my.packlol.pootracker.repository.AuthRepository
import my.packlol.pootracker.repository.AuthState
import my.packlol.pootracker.ui.MainUiState.Loading
import my.packlol.pootracker.ui.MainUiState.Success

class MainVM(
    private val dataStore: DataStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val mainUiState = combine(
        dataStore.userPrefs(),
        authRepository.authState(),
    ) { prefs, authState ->
        Success(prefs, authState)
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )


    fun changeDarkThemePref(pref: UserTheme) {
        viewModelScope.launch {
            dataStore.updateUserPrefs { userPrefs ->
                userPrefs.copy(
                    darkThemePreference = pref
                )
            }
        }
    }

    fun changeDynamicThemePref(pref: Boolean) {
        viewModelScope.launch {
            dataStore.updateUserPrefs { userPrefs ->
                userPrefs.copy(
                    useDynamicTheme = pref
                )
            }
        }
    }

    fun logout() {
        CoroutineScope(Dispatchers.Default).launch {
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