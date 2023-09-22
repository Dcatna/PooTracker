package my.packlol.pootracker.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import my.packlol.pootracker.ui.navigation.Screen
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    navigate: (Screen) -> Unit
) {
    val homeVM = koinViewModel<HomeVM>()

    val state by homeVM.homeUiState.collectAsState()

    HomeScreen(
        navigate = navigate,
        state = state,
        onLogButtonClick = homeVM::logPoop
    )
}

@Composable
private fun HomeScreen(
    navigate: (Screen) -> Unit = {},
    state: HomeUiState,
    onLogButtonClick: () -> Unit,
) {
    val flattenedItems = remember(state.logsByUser) {
        state.logsByUser.flatMap { it.value }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item { Button(onClick = onLogButtonClick) {} }
        items(flattenedItems) { log ->
            Text(log.toString())
        }
    }
}