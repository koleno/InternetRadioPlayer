package xyz.koleno.internetradioplayer.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import xyz.koleno.internetradioplayer.R
import xyz.koleno.internetradioplayer.data.Station
import kotlin.math.min

// TODO: next / prev buttons
@Composable
fun MainScreen(
    uiState: StateFlow<MainScreenContract.State>,
    onEvent: (MainScreenContract.Event) -> Unit,
    onSettingsClicked: () -> Unit
) {
    val state = uiState.collectAsStateWithLifecycle()
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        MainScreenContent(
            modifier = Modifier.padding(innerPadding),
            stationTitle = state.value.stationTitle,
            stationText = state.value.stationText,
            isPlaying = state.value.isPlaying,
            stations = state.value.stations,
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
}

@Composable
fun MainScreenContent(
    modifier: Modifier,
    stationTitle: String,
    stationText: String,
    isPlaying: Boolean,
    stations: List<Station>,
    onPlayButtonClicked: () -> Unit,
    onPrevButtonClicked: () -> Unit,
    onNextButtonClicked: () -> Unit,
    onStationButtonClicked: (station: Station) -> Unit,
    onSettingsClicked: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier
            .padding(24.dp)
            .fillMaxHeight()
    ) {
        Box {
            Text(
                text = stationTitle.ifEmpty { stringResource(R.string.app_name) },
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(bottom = 16.dp, top = 4.dp, end = 52.dp, start = 52.dp)
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
                    Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Text(
            text = stationText.ifEmpty { stringResource(R.string.app_description) },
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(bottom = 36.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )

        MainScreenControls(
            isPlaying,
            onPlayButtonClicked,
            onPrevButtonClicked,
            onNextButtonClicked
        )

        MainScreenPager(modifier = Modifier, stations, onStationButtonClicked)
    }

}

@Composable
fun MainScreenControls(
    isPlaying: Boolean,
    onPlayButtonClicked: () -> Unit,
    onPrevButtonClicked: () -> Unit,
    onNextButtonClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(bottom = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(
            space = 24.dp,
            alignment = Alignment.CenterHorizontally
        ),
    ) {
        MainScreenButton(R.drawable.ic_prev, stringResource(R.string.previous_station)) {
            onPrevButtonClicked.invoke()
        }
        MainScreenButton(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
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
    onStationButtonClicked: (Station) -> Unit
) {
    val configuration = LocalWindowInfo.current.containerSize
    val landscape = configuration.width > configuration.height

    val columns: Int = if (landscape) configuration.width / 500 else configuration.width / 640
    val rows: Int = if (landscape) configuration.height / 640 else configuration.height / 500


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
                        .size(24.dp)
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

@Preview(device = "spec:parent=pixel_5,orientation=portrait")
@Composable
fun MainScreenPreview() {
    val stations = mutableListOf<Station>()

    repeat(15) { iteration ->
        stations.add(Station(iteration, "Station $iteration", ""))
    }

    val stateFlow = MutableStateFlow(
        MainScreenContract.State("Sample Radio", "Artist - Song", false, stations)
    )

    MainScreen(
        uiState = stateFlow,
        onEvent = { }
    ) {
        // not used
    }
}