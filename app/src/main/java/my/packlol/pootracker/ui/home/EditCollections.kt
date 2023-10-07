package my.packlol.pootracker.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.DialogProperties
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonHighlightAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.compose.Balloon
import com.skydoves.balloon.compose.rememberBalloonBuilder
import com.skydoves.balloon.compose.setBackgroundColor
import com.skydoves.balloon.compose.setTextColor
import my.packlol.pootracker.SettingsDialogThemeChooserRow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditCollectionsButton(
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Balloon(
        builder = rememberBalloonBuilder {
            setText("Edit the collections that contain poop logs.")
            setArrowSize(10)
            setWidth(BalloonSizeSpec.WRAP)
            setHeight(BalloonSizeSpec.WRAP)
            setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            setArrowOrientation(ArrowOrientation.BOTTOM)
            setArrowPosition(0.5f)
            setBalloonAnimation(BalloonAnimation.FADE)
            setBalloonHighlightAnimation(BalloonHighlightAnimation.SHAKE)
            setPadding(8)
            setMarginHorizontal(8)
            setCornerRadius(8f)
            setTextColor(onPrimary)
            setBackgroundColor(primaryColor)
        }
    ) { balloonWindow ->
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(40.0.dp)
                .clip(CircleShape)
                .background(color = Color.Transparent)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { balloonWindow.showAlignTop() },
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(
                        bounded = false,
                        radius = 40.0.dp / 2
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                contentDescription = "edit",
                imageVector = Icons.Outlined.Edit,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditCollectionsDialog(
    visible: Boolean,
    signedIn: Boolean,
    onDismiss: () -> Unit,
    onCollectionAdd: (name: String, offline: Boolean) -> Unit,
    onCollectionDelete: (cid: String) -> Unit,
    onCollectionEdit: (cid: String, name: String) -> Unit,
    collections: List<UiCollection>,
) {
    if (visible) {
        val configuration = LocalConfiguration.current

        var selectedCollection by remember {
            mutableStateOf<UiCollection?>(null)
        }
        var addingCollection by remember {
            mutableStateOf(false)
        }

        var name by remember {
            mutableStateOf("")
        }

        var offlineCollection by remember {
            mutableStateOf(!signedIn)
        }

        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .padding(bottom = 128.dp)
                .widthIn(max = configuration.screenWidthDp.dp - 80.dp),
            onDismissRequest = onDismiss,
            text = {
                Column {
                    Divider()
                    AnimatedContent(
                        targetState = Pair(selectedCollection, addingCollection),
                        label = "collections-edit"
                    ) {(collection, adding) ->
                        Column {
                            if (collection != null) {

                                LaunchedEffect(key1 = Unit) {
                                    name = collection.name
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        maxLines = 1,
                                        label = { Text("collection name") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            onCollectionDelete(collection.id)
                                            onDismiss()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "delete"
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { selectedCollection = null }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "back"
                                    )
                                }
                            } else if (adding) {
                                LaunchedEffect(key1 = Unit) {
                                    name = ""
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        maxLines = 1,
                                        label = { Text("collection name") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = "Save Collection For",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                )
                                Column(Modifier.selectableGroup()) {
                                    if (signedIn) {
                                        SettingsDialogThemeChooserRow(
                                            text = "Current User",
                                            selected = !offlineCollection,
                                            onClick = { offlineCollection = false },
                                        )
                                    }
                                    SettingsDialogThemeChooserRow(
                                        text = "Offline",
                                        selected = offlineCollection,
                                        onClick = { offlineCollection = true },
                                    )
                                }
                                IconButton(
                                    onClick = { addingCollection = false }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "back"
                                    )
                                }
                            } else {
                                Text(
                                    text = "Select a Collection to Edit",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                )
                                FlowRow {
                                    collections.fastForEach { collection ->
                                        AssistChip(
                                            onClick = {
                                                selectedCollection = collection
                                            },
                                            label = {
                                                Text(collection.name)
                                            }
                                        )
                                    }
                                    IconButton(
                                        onClick = { addingCollection = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "add"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            title = {
                Text(
                    "Edit Collections",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            dismissButton = {
                Text(
                    text = "cancel",
                    Modifier.clickable {
                        onDismiss()
                    }
                )
            },
            confirmButton = {
                selectedCollection?.let {
                    Text(
                        text = "save",
                        Modifier.clickable {
                            onCollectionEdit(it.id, it.name)
                            onDismiss()
                        }
                    )
                }
                if (addingCollection) {
                    Text(
                        text = "save",
                        Modifier.clickable {
                            onCollectionAdd(name, offlineCollection)
                            onDismiss()
                        }
                    )
                }
            }
        )
    }
}