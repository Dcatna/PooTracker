package my.packlol.pootracker.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import my.packlol.pootracker.PoopAppState
import my.packlol.pootracker.ui.auth.AuthScreen.*
import org.koin.androidx.compose.koinViewModel

enum class AuthScreen {
    Register, Login
}

@Composable
fun LoginScreen(
    poopAppState: PoopAppState,
    onLogin: () -> Unit
) {
    val authVM = koinViewModel<AuthVM>()

    LaunchedEffect(Unit) {
        authVM.loginEvent.collect { event ->
            when (event) {
                is LoginEvent.Failed -> poopAppState.showSnackbar(
                    event.error, SnackbarDuration.Short
                )
                LoginEvent.Success -> onLogin()
            }
        }
    }

    var authScreen by remember {
        mutableStateOf(Login)
    }

    AnimatedContent(
        targetState = authScreen,
        label = "auth-transition",
    ) { screen ->
        when(screen) {
            Register -> RegisterScreen(
                register = { email, password ->
                    authVM.register(email, password)
                },
                navigateToLogin = {
                    authScreen = Login
                },
                useOffline = {
                    authVM.useOffline()
                }
            )
            Login -> LoginScreen(
                login = { email, password ->
                    authVM.login(email, password)
                },
                navigateToRegister = {
                     authScreen = Register
                },
                useOffline = {
                    authVM.useOffline()
                }
            )
        }
    }
}

