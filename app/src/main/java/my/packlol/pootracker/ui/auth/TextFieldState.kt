package my.packlol.pootracker.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun rememberHidableTextState(
    startHidden: Boolean = true
): HideableTextState {

    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    var hidden by rememberSaveable {
        mutableStateOf(startHidden)
    }

    return remember(
        textFieldValue,
        hidden
    ) {
        HideableTextState(
            textFieldValue,
            hidden,
            hide = {
                hidden = !hidden
            },
            changeText = {
                textFieldValue = it
            }
        )
    }
}

class HideableTextState(
    val textFieldValue: TextFieldValue,
    val hidden: Boolean,
    private val hide: () -> Unit,
    private val changeText: (textFieldValue: TextFieldValue) -> Unit
) {
    val text = textFieldValue.text

    fun hide() = hide

    fun onValueChange(textFieldValue: TextFieldValue) {
        changeText(textFieldValue)
    }

    val visualTransformation = if(hidden) {
        PasswordVisualTransformation()
    } else {
        VisualTransformation.None
    }

    val trailingIcon: @Composable () -> Unit
        @Composable get() {
            return {
                IconButton(
                    onClick = hide()
                ) {
                    Icon(
                        imageVector = if (hidden) {
                            Icons.Filled.Visibility
                        } else {
                            Icons.Filled.VisibilityOff
                        },
                        contentDescription = null
                    )
                }
            }
        }
}

@Composable
fun rememberAuthTextState(
    passwordFocus: FocusRequester = remember { FocusRequester() },
    emailFocus: FocusRequester = remember { FocusRequester() },
    passwordState: HideableTextState = rememberHidableTextState(),
    emailState: HideableTextState = rememberHidableTextState(startHidden = false),
    onDone: (email: String, password: String) -> Unit
): AuthTextState {

    return remember(
        passwordFocus,
        emailFocus,
        passwordState,
        emailState,
    ) {
        AuthTextState(
            passwordFocus,
            emailFocus,
            passwordState.text,
            emailState.text,
            onDone = onDone
        )
    }
}

class AuthTextState(
    val passwordFocus: FocusRequester,
    val emailFocus: FocusRequester,
    private val password: String,
    private val email: String,
    private val onDone: (email: String, password: String) -> Unit
) {
    fun onDone() {
        if (password.isNotBlank() && email.isNotBlank()) {
            onDone(email, password)
        }
        if (email.isBlank()) {
            emailFocus.requestFocus()
        } else {
            passwordFocus.requestFocus()
        }
    }
}