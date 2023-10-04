package my.packlol.pootracker.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import my.packlol.pootracker.PoopAppState
import my.packlol.pootracker.ui.auth.AuthScreen.Login
import my.packlol.pootracker.ui.auth.AuthScreen.Register
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

    Column {

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pooEmoji,
                fontSize = 120.sp
            )
        }

        Text(
            text = "Poo Tracker",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        var resetDialogVisible by rememberSaveable {
            mutableStateOf(false)
        }
        val configuration = LocalConfiguration.current

        var email by rememberSaveable {
            mutableStateOf("")
        }

        if (resetDialogVisible) {
            AlertDialog(
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier
                    .padding(bottom = 128.dp)
                    .widthIn(max = configuration.screenWidthDp.dp - 80.dp),
                onDismissRequest = { resetDialogVisible = false },
                text = {
                    Column {
                        Divider()
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            maxLines = 1,
                            label = { Text("email") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                title = {
                    Text(
                        "Reset Password",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                dismissButton = {
                    Text(
                        "cancel",
                        Modifier.clickable {
                            resetDialogVisible = false
                        }
                    )
                },
                confirmButton = {
                    Text(
                        "send reset",
                        Modifier.clickable {
                            authVM.resetPassword(email)
                            resetDialogVisible = false
                        }
                    )
                }
            )
        }

        AnimatedContent(
            targetState = authScreen,
            label = "auth-transition",
            modifier = Modifier.weight(1f)
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

        Row(
            Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Button(onClick = {
                resetDialogVisible = true
            }) {
                Text(text = "Reset password")
            }
        }
    }
}

