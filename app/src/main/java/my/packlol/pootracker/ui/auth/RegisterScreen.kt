package my.packlol.pootracker.ui.auth

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

const val pooEmoji = "\uD83D\uDCA9"

@Composable
fun RegisterScreen(
    register: (email: String, password: String) -> Unit,
    navigateToLogin: () -> Unit,
    useOffline: () -> Unit,
) {

    val emailState = rememberHidableTextState(startHidden = false)
    val passwordState = rememberHidableTextState()

    val authTextState = rememberAuthTextState(
        passwordState = passwordState,
        emailState = emailState
    ) { email, password ->
        register(email, password)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(authTextState.emailFocus),
            value = emailState.textFieldValue,
            singleLine = true,
            onValueChange = {  emailState.onValueChange(it) },
            label = { Text("email") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "email"
                )
            },
            keyboardActions = KeyboardActions(
                onDone = { authTextState.onDone() }
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            visualTransformation = emailState.visualTransformation,
            trailingIcon = emailState.trailingIcon
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(authTextState.passwordFocus),
            singleLine = true,
            value = passwordState.textFieldValue,
            onValueChange = {  passwordState.onValueChange(it) },
            label = { Text("password") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.HideSource,
                    contentDescription = "password"
                )
            },
            keyboardActions = KeyboardActions(
                onDone = { authTextState.onDone() }
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            visualTransformation = passwordState.visualTransformation,
            trailingIcon = passwordState.trailingIcon
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ){
                Text(
                    "have an account",
                    Modifier.clickable {
                        navigateToLogin()
                    }
                )
                Text(
                    "use offline",
                    Modifier.clickable {
                        useOffline()
                    }
                )
            }
            Button(
                onClick = { register(emailState.text, passwordState.text) },
                enabled = passwordState.text.isNotEmpty() && emailState.text.isNotEmpty()
            ) {
                Text("create")
            }
        }
    }
}