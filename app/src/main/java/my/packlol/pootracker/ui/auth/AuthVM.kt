package my.packlol.pootracker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import my.packlol.pootracker.repository.AuthRepository
import my.packlol.pootracker.ui.auth.LoginEvent.*

class AuthVM(
    private val authRepository: AuthRepository,
): ViewModel() {

    private val _loginEvent = Channel<LoginEvent>()
    val loginEvent = _loginEvent.receiveAsFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            authRepository.loginWithEmailAndPassword(email, password)
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
        }
    }
}


