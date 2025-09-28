@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.koleno.internetradioplayer.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import xyz.koleno.internetradioplayer.R
import xyz.koleno.internetradioplayer.data.Station
import xyz.koleno.internetradioplayer.utils.rememberFlowWithLifecycle

// TODO: change position
// TODO: export
// TODO: import

@Composable
fun SettingsScreen(
    uiState: StateFlow<SettingsScreenContract.State>,
    uiEffect: Flow<SettingsScreenContract.Effect>,
    onEvent: (SettingsScreenContract.Event) -> Unit,
    onBackClicked: () -> Unit
) {
    val showDropDownMenu = remember { mutableStateOf(false) }
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                IconButton(onClick = { showDropDownMenu.value = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.menu)
                    )
                }
                DropdownMenu(
                    expanded = showDropDownMenu.value,
                    onDismissRequest = { showDropDownMenu.value = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.add_station)) },
                        leadingIcon = { Icon(Icons.Filled.Add, null) },
                        onClick = {
                            onEvent(SettingsScreenContract.Event.AddClicked)
                            showDropDownMenu.value = false
                        }
                    )
                }
            }
        )

    }) { innerPadding ->

        LaunchedEffect(effect) {
            effect.collect { action ->
                when (action) {
                    SettingsScreenContract.Effect.ShowSaveError -> {
                        Toast.makeText(localContext, R.string.save_error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        StationList(Modifier.padding(innerPadding), state.value.stations, onStationClicked = {
            onEvent(SettingsScreenContract.Event.OpenBottomSheet(it))
        })

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
fun StationList(modifier: Modifier, stations: List<Station>, onStationClicked: (Station) -> Unit) {
    val scrollState = rememberScrollState()

    Column(modifier.verticalScroll(scrollState)) {
        stations.forEach { station ->
            StationRow(station, onStationClicked)
        }
    }
}

@Composable
fun StationRow(station: Station, onStationClicked: (Station) -> Unit) {
    Box(
        Modifier
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
    HorizontalDivider(thickness = 1.dp)
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

            val stationName = remember { mutableStateOf(name) }
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                value = stationName.value,
                onValueChange = {
                    stationName.value = it
                },
                label = {
                    Text(stringResource(R.string.station_name))
                })

            val stationUri = remember { mutableStateOf(uri) }
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                value = stationUri.value,
                onValueChange = {
                    stationUri.value = it
                },
                label = {
                    Text(stringResource(R.string.station_uri))
                })

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                onClick = {
                    onSaveClicked.invoke(stationName.value, stationUri.value)
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

@Preview(device = "spec:parent=pixel_5,orientation=portrait")
@Composable
fun SettingsScreenPreview() {
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