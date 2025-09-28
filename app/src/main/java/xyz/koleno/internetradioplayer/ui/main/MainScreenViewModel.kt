package xyz.koleno.internetradioplayer.ui.main

import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.koleno.internetradioplayer.data.Station
import xyz.koleno.internetradioplayer.data.StationRepository
import xyz.koleno.internetradioplayer.ui.base.BaseViewModel

@HiltViewModel
class   MainScreenViewModel @Inject constructor(
    private val stationRepository: StationRepository
) :
    BaseViewModel<MainScreenContract.State, MainScreenContract.Event, MainScreenContract.Effect>(
        initialState = MainScreenContract.State.initial()
    ) {

    private var lastPlayed: Station? = null

    override fun handleEvent(event: MainScreenContract.Event) {

        when (event) {
            MainScreenContract.Event.NextButtonClicked -> {

            }

            MainScreenContract.Event.PlayButtonClicked -> playButtonClicked()


            MainScreenContract.Event.PrevButtonClicked -> {

            }

            is MainScreenContract.Event.StationButtonClicked -> {
                stationButtonClicked(event.station)
            }

            is MainScreenContract.Event.RadioTextReceived -> {
                setState {
                    currentState.copy(stationText = event.text)
                }
            }

            MainScreenContract.Event.PlayStarted -> {

            }
        }
    }

    override suspend fun initialLoad() {
        withContext(Dispatchers.IO) {
            stationRepository.getAll().collect {
                setState {
                    currentState.copy(stations = it)
                }
            }
        }
    }

    private fun playButtonClicked() {
        var isPlaying = false

        if (currentState.isPlaying) {
            setEffect { MainScreenContract.Effect.StopPlaying }
        } else {
            lastPlayed?.let {
                isPlaying = true
                setEffect { MainScreenContract.Effect.StartPlaying(it) }
            }
        }

        setState {
            currentState.copy(isPlaying = isPlaying)
        }
    }

    private fun stationButtonClicked(station: Station) {
        lastPlayed = station

        setState {
            currentState.copy(
                isPlaying = true,
                stationTitle = station.name,
                stationText = " "
            )
        }
        setEffect { MainScreenContract.Effect.StartPlaying(station) }
    }

}