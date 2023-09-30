package my.packlol.pootracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    visible: Boolean,
    sheetState: SheetState,
    reverseChartDirection: Boolean,
    onChartDirectionChange: (Boolean) -> Unit,
    byDateAsc: Boolean,
    onByDateAscChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = {}
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth()
                        .clickable {
                            onByDateAscChange(!byDateAsc)
                        }
                ) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { onByDateAscChange(!byDateAsc) }) {
                            Icon(
                                imageVector = if (byDateAsc)
                                    Icons.Filled.ArrowUpward
                                else Icons.Filled.ArrowDownward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text("Sort by Date")
                }
            }
            Column(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth()
                        .clickable {
                            onChartDirectionChange(!reverseChartDirection)
                        }
                ) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = {
                                onChartDirectionChange(!reverseChartDirection)
                            }
                        ) {
                            Icon(
                                imageVector = if (reverseChartDirection)
                                    Icons.Filled.ArrowBack
                                else Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text("Poop Chart Direction")
                }
            }
        }
    }
}