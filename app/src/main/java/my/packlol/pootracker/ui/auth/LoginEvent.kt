package my.packlol.pootracker.ui.auth

sealed interface LoginEvent {
    data object Success: LoginEvent
    data class Failed(val error: String) : LoginEvent
}