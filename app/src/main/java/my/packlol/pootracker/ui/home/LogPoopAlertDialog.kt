package my.packlol.pootracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.DialogProperties
import my.packlol.pootracker.ui.theme.conditional
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LogPoopAlertDialog(
    selectedDateTime: LocalDateTime,
    collections: List<UiCollection>,
    onDismiss: () -> Unit,
    vertical: Boolean,
    onConfirmButtonClick: (UiCollection) -> Unit,
) {
    var selectedCollection by remember {
        mutableStateOf(collections.firstOrNull())
    }

    val configuration = LocalConfiguration.current

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .conditional(vertical) {
                padding(bottom = 120.dp)
            }
            .widthIn(max = configuration.screenWidthDp.dp - 80.dp),
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Log Poop At",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = formatDate(selectedDateTime.toLocalDate()),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatTime(selectedDateTime.toLocalTime()),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Divider()
                Text(
                    text = "Log To Collection",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                FlowRow {
                    collections.fastForEach { collection ->
                        FilterChip(
                            onClick = {
                                selectedCollection = collection
                            },
                            selected = selectedCollection == collection,
                            label = {
                                Text(collection.name)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Text(
                text = "Log",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable {
                        selectedCollection?.let {
                            onConfirmButtonClick(it)
                        }
                    },
            )
        }
    )
}


private fun formatDate(d: LocalDate): String {
    return "${d.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}," +
            " ${d.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${d.dayOfMonth}, ${d.year}"
}