package my.packlol.pootracker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.repository.AuthRepository
import my.packlol.pootracker.ui.auth.LoginEvent.Failed
import my.packlol.pootracker.ui.auth.LoginEvent.Success

class AuthVM(
    private val authRepository: AuthRepository,
    private val userDataStore: DataStore
): ViewModel() {

    private val _loginEvent = Channel<LoginEvent>()
    val loginEvent = _loginEvent.receiveAsFlow()

    private var loginJob: Job? = null
    private var registerJob: Job? = null

    fun useOffline() {
        CoroutineScope(Dispatchers.Default).launch {
            userDataStore.updateUserPrefs { prev ->
                prev.copy(useOffline = true)
            }
        }
        _loginEvent.trySend(Success)
    }

    fun oneTapSignIn(token: String) {
        viewModelScope.launch {
            runCatching {
                authRepository.oneTapSignIn(token)
            }
                .onSuccess { result ->
                    _loginEvent.send(
                        if(result.user != null)
                            Success
                        else
                            Failed(error = "unable to create account")
                    )
                }
                .onFailure {
                    _loginEvent.send(
                        Failed(it.localizedMessage ?: "unable to login")
                    )
                }
        }
    }

    fun register(email: String, password: String) {
        if (registerJob?.isActive == true) {
            return
        }
        registerJob = viewModelScope.launch {
            authRepository.register(email, password)
                .onSuccess { result ->
                    _loginEvent.send(
                        if(result.user != null)
                            Success
                        else
                            Failed(error = "unable to create account")
                    )
                }
                .onFailure {
                    _loginEvent.send(
                        Failed(it.localizedMessage ?: "unable to login")
                    )
                }
            registerJob = null
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            runCatching {
                authRepository.resetPassword(email)
            }
                .onFailure {
                    _loginEvent.trySend(
                        Failed(it.localizedMessage ?: "error sending reset.")
                    )
                }
        }
    }

    fun login(email: String, password: String) {
        if (loginJob?.isActive == true) {
            return
        }
        loginJob = viewModelScope.launch {
            authRepository.login(email, password)
                .onSuccess { result ->
                    _loginEvent.send(
                        if(result.user != null)
                            Success
                        else
                            Failed(error = "unable to login")
                    )
                }
                .onFailure {
                    _loginEvent.send(
                        Failed(it.localizedMessage ?: "unable to login")
                    )
                }
            loginJob = null
        }
    }
}


