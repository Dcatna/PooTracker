package my.packlol.pootracker.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import my.packlol.pootracker.PoopAppState
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthScreen(
    poopAppState: PoopAppState,
) {
    val authVM = koinViewModel<AuthVM>()

    LaunchedEffect(Unit) {
        authVM.loginEvent.collect { event ->
            when (event) {
                is LoginEvent.Failed -> poopAppState.showSnackbar(
                    event.error, SnackbarDuration.Short
                )
                LoginEvent.Success -> Unit
            }
        }
    }

    AuthScreen()
}

@Composable
private fun AuthScreen(
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Auth")
    }
}