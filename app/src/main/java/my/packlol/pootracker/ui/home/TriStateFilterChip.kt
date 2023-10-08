package my.packlol.pootracker.ui.home

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriStateFilterChip(
    state: ToggleableState,
    toggleState: (ToggleableState) -> Unit,
    name: String,
    modifier: Modifier = Modifier,
    hideIcons: Boolean = false,
    labelTextStyle: TextStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
) {
    FilterChip(
        modifier = modifier,
        selected = state == ToggleableState.On || state == ToggleableState.Indeterminate,
        onClick = { toggleStateIfAble(false, state, toggleState) },
        leadingIcon = {
            if (!hideIcons) {
                if (state == ToggleableState.On) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                } else if (state == ToggleableState.Indeterminate) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                }
            }
        },
        shape = RoundedCornerShape(100),
        label = { Text(text = name, style = labelTextStyle) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            selectedBorderColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
        ),
    )
}

private fun toggleStateIfAble(disabled: Boolean, state: ToggleableState, toggleState: (ToggleableState) -> Unit) {
    if (!disabled) {
        val newState = when (state) {
            ToggleableState.On -> ToggleableState.Indeterminate
            ToggleableState.Indeterminate -> ToggleableState.Off
            ToggleableState.Off -> ToggleableState.On
        }
        toggleState(newState)
    }
}