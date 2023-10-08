package my.packlol.pootracker.ui.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import keys.webClientId
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

    val oneTapSignInState = rememberOneTapSignInState()

    LaunchedEffect(Unit) {
        oneTapSignInState.open()
    }

    OneTapSignInWithGoogle(
        state = oneTapSignInState,
        clientId = webClientId,
        onTokenIdReceived = {
            authVM.oneTapSignIn(it)
        },
        onDialogDismissed = {
            oneTapSignInState.close()
        }
    )

    var authScreen by remember { mutableStateOf(Login) }

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


class OneTapSignInState {
    var opened by mutableStateOf(false)
        private set

    fun open() {
        opened = true
    }

    internal fun close() {
        opened = false
    }
}

@Composable
fun rememberOneTapSignInState(): OneTapSignInState {
    return remember { OneTapSignInState() }
}


/**
 * Composable that allows you to easily integrate One-Tap Sign in with Google
 * in your project.
 * @param state - One-Tap Sign in State.
 * @param clientId - CLIENT ID (Web) of your project, that you can obtain from
 * a Google Cloud Platform.
 * @param nonce - Optional nonce that can be used when generating a Google Token ID.
 * @param onTokenIdReceived - Lambda that will be triggered after a successful
 * authentication. Returns a Token ID.
 * @param onDialogDismissed - Lambda that will be triggered when One-Tap dialog
 * disappears. Returns a message in a form of a string.
 * */
@Composable
fun OneTapSignInWithGoogle(
    state: OneTapSignInState,
    clientId: String,
    nonce: String? = null,
    onTokenIdReceived: (String) -> Unit,
    onDialogDismissed: (String) -> Unit,
) {
    val activity = LocalContext.current as Activity
    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val oneTapClient = Identity.getSignInClient(activity)
                val credentials = oneTapClient.getSignInCredentialFromIntent(result.data)
                val tokenId = credentials.googleIdToken
                if (tokenId != null) {
                    onTokenIdReceived(tokenId)
                    state.close()
                }
            } else {
                Log.d("Auth", result.data.toString())
                onDialogDismissed("Dialog Closed.")
                state.close()
            }
        } catch (e: ApiException) {
            Log.e("Auth", "${e.message}")
            when (e.statusCode) {
                CommonStatusCodes.CANCELED -> {
                    onDialogDismissed("Dialog Canceled.")
                    state.close()
                }
                CommonStatusCodes.NETWORK_ERROR -> {
                    onDialogDismissed("Network Error.")
                    state.close()
                }
                else -> {
                    onDialogDismissed(e.message.toString())
                    state.close()
                }
            }
        }
    }

    LaunchedEffect(key1 = state.opened) {
        if (state.opened) {
            signIn(
                activity = activity,
                clientId = clientId,
                nonce = nonce,
                launchActivityResult = { intentSenderRequest ->
                    activityLauncher.launch(intentSenderRequest)
                },
                onError = {
                    onDialogDismissed(it)
                    state.close()
                }
            )
        }
    }
}

private fun signIn(
    activity: Activity,
    clientId: String,
    nonce: String?,
    launchActivityResult: (IntentSenderRequest) -> Unit,
    onError: (String) -> Unit
) {
    runCatching {
        val oneTapClient = Identity.getSignInClient(activity)
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setNonce(nonce)
                    .setServerClientId(clientId)
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    launchActivityResult(
                        IntentSenderRequest.Builder(
                            result.pendingIntent.intentSender
                        ).build()
                    )
                } catch (e: Exception) {
                    onError(e.message.toString())
                }
            }
            .addOnFailureListener {
                signUp(
                    activity = activity,
                    clientId = clientId,
                    nonce = nonce,
                    launchActivityResult = launchActivityResult,
                    onError = onError
                )
                Log.e("Auth", "${it.message} sign in")
            }
    }
        .onFailure { onError("") }
}

private fun signUp(
    activity: Activity,
    clientId: String,
    nonce: String?,
    launchActivityResult: (IntentSenderRequest) -> Unit,
    onError: (String) -> Unit
) {
    runCatching {
        val oneTapClient = Identity.getSignInClient(activity)
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setNonce(nonce)
                    .setServerClientId(clientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    launchActivityResult(
                        IntentSenderRequest.Builder(
                            result.pendingIntent.intentSender
                        ).build()
                    )
                } catch (e: Exception) {
                    onError(e.message.toString())
                    Log.e("Auth", "${e.message}")
                }
            }
            .addOnFailureListener {
                onError("Google Account not Found.")
                Log.e("Auth", "sign up ${it.message}")
            }
    }
}
