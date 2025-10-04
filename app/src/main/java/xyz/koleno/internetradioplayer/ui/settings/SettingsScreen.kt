@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.koleno.internetradioplayer.ui.settings

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import sh.calvin.reorderable.ReorderableColumn
import xyz.koleno.internetradioplayer.R
import xyz.koleno.internetradioplayer.data.Station
import xyz.koleno.internetradioplayer.utils.rememberFlowWithLifecycle

@Composable
fun SettingsScreen(
    uiState: StateFlow<SettingsScreenContract.State>,
    uiEffect: Flow<SettingsScreenContract.Effect>,
    onEvent: (SettingsScreenContract.Event) -> Unit,
    onBackClicked: () -> Unit
) {
    var showDropDownMenu by remember { mutableStateOf(false) }
    val state = uiState.collectAsStateWithLifecycle()
    val effect = rememberFlowWithLifecycle(uiEffect)
    val sheetState = rememberModalBottomSheetState()
    val localContext = LocalContext.current

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            title = {
                Text(stringResource(R.string.settings))
            },
            navigationIcon = {
                IconButton(onClick = {
                    onBackClicked()
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                IconButton(onClick = { showDropDownMenu = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.menu)
                    )
                }
                DropdownMenu(
                    expanded = showDropDownMenu,
                    onDismissRequest = { showDropDownMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.add_station)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onEvent(SettingsScreenContract.Event.AddClicked)
                            showDropDownMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.export)) },
                        onClick = {
                            onEvent(SettingsScreenContract.Event.ExportClicked)
                            showDropDownMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.import_settings)) },
                        onClick = {
                            onEvent(SettingsScreenContract.Event.ImportClicked)
                            showDropDownMenu = false
                        }
                    )
                }
            }
        )

    }) { innerPadding ->
        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                it.data?.data?.let { fileUri ->
                    onEvent(SettingsScreenContract.Event.ImportFilePicked(fileUri))
                }
            }

        LaunchedEffect(effect) {

            effect.collect { action ->
                when (action) {
                    SettingsScreenContract.Effect.ShowSaveError -> {
                        Toast.makeText(localContext, R.string.save_error, Toast.LENGTH_SHORT).show()
                    }

                    is SettingsScreenContract.Effect.ShareExport -> {
                        localContext.startActivity(action.intent)
                    }

                    is SettingsScreenContract.Effect.ShareImport -> {
                        launcher.launch(action.intent)
                    }

                    SettingsScreenContract.Effect.ShowExportError -> {
                        Toast.makeText(localContext, R.string.export_error, Toast.LENGTH_SHORT)
                            .show()
                    }

                    SettingsScreenContract.Effect.ShowImportError -> {
                        Toast.makeText(localContext, R.string.import_error, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

        val modifier = Modifier.padding(innerPadding)

        // show different layouts for different orientations
        val configuration = LocalConfiguration.current
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                LandscapeLayout(modifier, state, onEvent)
            }

            else -> {
                PortraitLayout(modifier, state, onEvent)
            }
        }

        StationBottomSheet(
            state.value.openBottomSheet,
            sheetState,
            state.value.editingStation?.name.orEmpty(),
            state.value.editingStation?.uri.orEmpty(),
            onBottomSheetDismiss = {
                onEvent(SettingsScreenContract.Event.DismissBottomSheet)
            },
            onDeleteClicked = {
                onEvent(SettingsScreenContract.Event.DeleteClicked)
            })
        { name, uri ->
            onEvent(SettingsScreenContract.Event.SaveClicked(name, uri))
        }
    }
}

@Composable
fun LandscapeLayout(
    modifier: Modifier,
    state: androidx.compose.runtime.State<SettingsScreenContract.State>,
    onEvent: (SettingsScreenContract.Event) -> Unit
) {
    Row(modifier) {
        StationList(modifier = Modifier.weight(0.5f), state.value.stations, onStationClicked = {
            onEvent(SettingsScreenContract.Event.OpenBottomSheet(it))
        }, onMoveItem = { fromIndex, toIndex ->
            onEvent(SettingsScreenContract.Event.MoveItem(fromIndex, toIndex))
        })

        VerticalDivider(thickness = 3.dp)

        Options(
            modifier = Modifier
                .padding(16.dp)
                .weight(0.5f),
            isIgnoreSecurityEnabled = state.value.isIgnoreSecurityEnabled,
            columns = state.value.columns,
            rows = state.value.rows,
            onIgnoreSecurityChanged = {
                onEvent(SettingsScreenContract.Event.IgnoreSecurityChanged(it))
            }, onRowsChanged = {
                onEvent(SettingsScreenContract.Event.RowsChanged(it))
            }, onColumnsChanged = {
                onEvent(SettingsScreenContract.Event.ColumnsChanged(it))
            })

    }
}

@Composable
fun PortraitLayout(
    modifier: Modifier,
    state: androidx.compose.runtime.State<SettingsScreenContract.State>,
    onEvent: (SettingsScreenContract.Event) -> Unit,
) {
    Column(modifier) {
        Options(
            modifier = Modifier.padding(16.dp),
            isIgnoreSecurityEnabled = state.value.isIgnoreSecurityEnabled,
            columns = state.value.columns,
            rows = state.value.rows,
            onIgnoreSecurityChanged = {
                onEvent(SettingsScreenContract.Event.IgnoreSecurityChanged(it))
            }, onRowsChanged = {
                onEvent(SettingsScreenContract.Event.RowsChanged(it))
            }, onColumnsChanged = {
                onEvent(SettingsScreenContract.Event.ColumnsChanged(it))
            })

        HorizontalDivider(thickness = 3.dp)

        StationList(modifier = Modifier, state.value.stations, onStationClicked = {
            onEvent(SettingsScreenContract.Event.OpenBottomSheet(it))
        }, onMoveItem = { fromIndex, toIndex ->
            onEvent(SettingsScreenContract.Event.MoveItem(fromIndex, toIndex))
        })
    }
}


@Composable
fun StationList(
    modifier: Modifier, stations: List<Station>,
    onStationClicked: (Station) -> Unit,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit
) {

    Column(modifier) {
        val scrollState = rememberScrollState()
        val hapticFeedback = LocalHapticFeedback.current

        Text(
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 8.dp),
            text = "Stations",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            modifier = Modifier.padding(top = 0.dp, bottom = 16.dp, start = 16.dp, end = 8.dp),
            text = "Tap to edit or remove station. Drag and drop to reorder stations.",
            style = MaterialTheme.typography.bodyMedium
        )


        ReorderableColumn(
            modifier = Modifier.verticalScroll(scrollState),
            list = stations,
            onSettle = onMoveItem
        )
        { index, item, isDragging ->
            ReorderableItem {
                val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

                val color =
                    if (index % 2 == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                Surface(shadowElevation = elevation) {
                    StationRow(
                        modifier = Modifier
                            .background(color = color)
                            .longPressDraggableHandle(
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.GestureThresholdActivate
                                    )
                                },
                                onDragStopped = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.GestureEnd
                                    )
                                },
                            ),
                        station = item,
                        onStationClicked = onStationClicked
                    )
                }
            }
        }
    }
}

@Composable
fun Options(
    modifier: Modifier,
    isIgnoreSecurityEnabled: Boolean,
    columns: Int,
    rows: Int,
    onIgnoreSecurityChanged: (enabled: Boolean) -> Unit,
    onColumnsChanged: (count: Int) -> Unit,
    onRowsChanged: (count: Int) -> Unit
) {
    Column(modifier) {
        SwitchOption(
            isIgnoreSecurityEnabled,
            stringResource(R.string.pref_ssl_title),
            stringResource(R.string.pref_ssl_desc)
        ) {
            onIgnoreSecurityChanged.invoke(it)
        }

        GridOption(
            name = stringResource(R.string.pref_columns),
            initialCount = columns,
            onCountChanged = onColumnsChanged
        )
        GridOption(
            stringResource(R.string.pref_rows),
            initialCount = rows,
            onCountChanged = onRowsChanged
        )
    }
}


@Composable
fun SwitchOption(
    enabled: Boolean,
    name: String,
    description: String,
    onSwitchChanged: (Boolean) -> Unit
) {
    Row {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            Text(modifier = Modifier.padding(bottom = 8.dp, end = 8.dp), text = name)
            Text(
                modifier = Modifier.padding(end = 8.dp),
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Switch(checked = enabled, onCheckedChange = {
            onSwitchChanged.invoke(it)
        })
    }
}

@Composable
fun GridOption(
    name: String,
    initialCount: Int,
    max: Int = 5,
    onCountChanged: (count: Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            var count = initialCount

            IconButton(enabled = count > 0, onClick = {
                count = count - 1
                onCountChanged(count)
            }) {
                Icon(
                    painterResource(R.drawable.ic_minus),
                    contentDescription = stringResource(R.string.decrease),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                modifier = Modifier
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(vertical = 6.dp, horizontal = 12.dp), text = count.toString()
            )

            IconButton(enabled = count < max, onClick = {
                count = count + 1
                onCountChanged(count)
            }) {
                Icon(
                    painterResource(R.drawable.ic_plus),
                    contentDescription = stringResource(R.string.increase),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun StationRow(modifier: Modifier, station: Station, onStationClicked: (Station) -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clickable(onClick = {
                onStationClicked(station)
            })
    ) {
        Text(
            modifier = Modifier
                .padding(16.dp),
            text = station.name
        )
    }
}

@Composable
fun StationBottomSheet(
    showBottomSheet: Boolean,
    sheetState: SheetState,
    name: String,
    uri: String,
    onBottomSheetDismiss: () -> Unit,
    onDeleteClicked: () -> Unit,
    onSaveClicked: (name: String, uri: String) -> Unit,
) {
    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = {
            onBottomSheetDismiss.invoke()
        }, sheetState = sheetState) {

            var stationName by remember { mutableStateOf(name) }
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                value = stationName,
                onValueChange = {
                    stationName = it
                },
                label = {
                    Text(stringResource(R.string.station_name))
                })

            var stationUri by remember { mutableStateOf(uri) }
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                value = stationUri,
                onValueChange = {
                    stationUri = it
                },
                label = {
                    Text(stringResource(R.string.station_uri))
                })

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                onClick = {
                    onSaveClicked.invoke(stationName, stationUri)
                }) {
                Text(stringResource(R.string.save))
            }

            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                onClick = {
                    onDeleteClicked.invoke()
                }) {
                Text(
                    color = MaterialTheme.colorScheme.error,
                    text = stringResource(R.string.delete)
                )
            }


        }
    }
}

@Composable
private fun SettingsScreenPreview() {
    val stations = mutableListOf<Station>()

    repeat(35) { iteration ->
        stations.add(Station(iteration, "Station $iteration", ""))
    }

    val stateFlow = MutableStateFlow(
        SettingsScreenContract.State(stations)
    )

    val effectFlow = MutableSharedFlow<SettingsScreenContract.Effect>()

    SettingsScreen(stateFlow, effectFlow, onEvent = {}) {
        // nothing for preview
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=portrait")
@Composable
fun SettingsScreenPreviewPortrait() {
    SettingsScreenPreview()
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
fun SettingsScreenPreviewLandscape() {
    SettingsScreenPreview()
}

@Preview(device = "spec:parent=pixel_5,orientation=portrait")
@Composable
fun BottomSheetPreview() {
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Expanded
    )

    StationBottomSheet(true, sheetState, name = "", uri = "", {
        // nothing for preview
    }, {
        // nothing for preview
    }) { name, uri ->
        // nothing for preview
    }
}