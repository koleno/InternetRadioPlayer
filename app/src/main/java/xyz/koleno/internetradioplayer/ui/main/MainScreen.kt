package xyz.koleno.internetradioplayer.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import xyz.koleno.internetradioplayer.R
import xyz.koleno.internetradioplayer.data.Station
import kotlin.math.min

@Composable
fun MainScreen(
    uiState: StateFlow<MainScreenContract.State>,
    onEvent: (MainScreenContract.Event) -> Unit,
    onSettingsClicked: () -> Unit
) {
    val state = uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        onEvent(MainScreenContract.Event.ScreenResumed)

        onPauseOrDispose { // ignored
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        MainScreenContent(
            modifier = Modifier.padding(innerPadding),
            stationTitle = state.value.stationTitle,
            stationText = state.value.stationText,
            isPlaying = state.value.isPlaying,
            stations = state.value.stations,
            rows = state.value.rows,
            columns = state.value.columns,
            {
                onEvent(MainScreenContract.Event.PlayButtonClicked)
            },
            onPrevButtonClicked = { onEvent(MainScreenContract.Event.PrevButtonClicked) },
            onNextButtonClicked = { onEvent(MainScreenContract.Event.NextButtonClicked) },
            onStationButtonClicked = {
                onEvent(
                    MainScreenContract.Event.StationButtonClicked(
                        it
                    )
                )
            },
            onSettingsClicked
        )
    }

    if (state.value.showNotificationsDialog) {
        NotificationPermissionModal({
            onEvent(MainScreenContract.Event.DismissNotifDialogClicked)
        }) {
            onEvent(MainScreenContract.Event.AskForNotificationPermissionsClicked)
        }
    }
}

@Composable
fun MainScreenContent(
    modifier: Modifier,
    stationTitle: String,
    stationText: String,
    isPlaying: Boolean,
    stations: List<Station>,
    rows: Int,
    columns: Int,
    onPlayButtonClicked: () -> Unit,
    onPrevButtonClicked: () -> Unit,
    onNextButtonClicked: () -> Unit,
    onStationButtonClicked: (station: Station) -> Unit,
    onSettingsClicked: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier
            .padding(16.dp)
            .fillMaxHeight()
    ) {
        Box {
            Text(
                text = stationTitle.ifEmpty { stringResource(R.string.app_name) },
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(bottom = 12.dp, top = 4.dp, end = 52.dp, start = 52.dp)
                    .clickable(onClick = {
                        Toast.makeText(context, "Dušanko ❤\uFE0F loves Janulka", Toast.LENGTH_SHORT)
                            .show()
                    }),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge
            )

            IconButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .align(Alignment.TopEnd),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                onClick = onSettingsClicked
            ) {
                Icon(
                    painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.settings),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Text(
            text = stationText.ifEmpty { stringResource(R.string.station_default_text) },
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )

        MainScreenControls(
            Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp),
            isPlaying,
            onPlayButtonClicked,
            onPrevButtonClicked,
            onNextButtonClicked
        )

        MainScreenPager(
            Modifier,
            stations,
            rows,
            columns,
            onStationButtonClicked
        )
    }

}

@Composable
fun MainScreenControls(
    modifier: Modifier,
    isPlaying: Boolean,
    onPlayButtonClicked: () -> Unit,
    onPrevButtonClicked: () -> Unit,
    onNextButtonClicked: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            space = 24.dp,
            alignment = Alignment.CenterHorizontally
        ),
    ) {
        MainScreenButton(R.drawable.ic_prev, stringResource(R.string.previous_station)) {
            onPrevButtonClicked.invoke()
        }
        MainScreenButton(
            if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play,
            stringResource(if (isPlaying) R.string.pause else R.string.play)
        ) {
            onPlayButtonClicked.invoke()
        }
        MainScreenButton(R.drawable.ic_next, stringResource(R.string.next_station)) {
            onNextButtonClicked.invoke()
        }
    }
}

@Composable
fun MainScreenButton(drawable: Int, contentDescription: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Icon(
            painter = painterResource(id = drawable),
            contentDescription = contentDescription
        )
    }
}

@Composable
fun MainScreenPager(
    modifier: Modifier,
    stations: List<Station>,
    rows: Int,
    columns: Int,
    onStationButtonClicked: (Station) -> Unit
) {
    val onePage = columns * rows
    val pageCount = Math.ceilDiv(stations.size, onePage)

    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = {
        pageCount
    })

    Column(modifier) {
        HorizontalPager(
            modifier = Modifier
                .weight(0.9f)
                .padding(bottom = 16.dp),
            state = pagerState
        ) { page ->
            MainScreenGrid(
                Modifier,
                stations.subList(
                    (page * onePage),
                    min((page * onePage) + onePage, stations.size)
                ),
                columns,
                rows,
                onStationButtonClicked
            )
        }

        Row(
            Modifier
                .weight(0.1f)
                .wrapContentHeight()
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {

            repeat(pagerState.pageCount) { iteration ->
                val color =
                    if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else ButtonDefaults.filledTonalButtonColors().containerColor
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(CircleShape)
                        .background(color)
                        .heightIn(max = 36.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clickable(onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(iteration)
                            }
                        })
                )
            }
        }
    }

}

@Composable
fun MainScreenGrid(
    modifier: Modifier,
    stations: List<Station>,
    columns: Int,
    rows: Int,
    onStationButtonClicked: (Station) -> Unit
) {

    Row(
        modifier
            .fillMaxWidth(1f)
            .fillMaxHeight(1f),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        var stationIndex = 0

        repeat(columns) { colum ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(rows) { row ->
                    if (stationIndex < stations.size) {
                        val currentStation = stations[stationIndex]
                        MainScreenItem(
                            currentStation.name,
                            Modifier.weight(1f)
                        ) {
                            onStationButtonClicked.invoke(currentStation)
                        }
                    } else {
                        return@repeat
                    }

                    stationIndex++
                }
            }
        }
    }

}

@Composable
fun MainScreenItem(name: String, modifier: Modifier, onClick: () -> Unit) {

    FilledTonalButton(
        onClick = onClick, modifier = modifier
            .fillMaxWidth(), shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 24.sp
        )
    }


}

@Composable
fun NotificationPermissionModal(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit
) {
    AlertDialog(
        title = {
            Text(text = "Notifications")
        },
        text = {
            Text(text = "Do you want to show status notifications with currently playing radio information?")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("No")
            }
        }
    )
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
fun MainScreenPreview() {
    val stations = mutableListOf<Station>()

    repeat(15) { iteration ->
        stations.add(Station(iteration, "Station $iteration", ""))
    }

    val stateFlow = MutableStateFlow(
        MainScreenContract.State("Sample Radio", "Artist - Song", false, stations, 2, 2, false)
    )

    MainScreen(
        uiState = stateFlow,
        onEvent = { }
    ) {
        // not used
    }
}