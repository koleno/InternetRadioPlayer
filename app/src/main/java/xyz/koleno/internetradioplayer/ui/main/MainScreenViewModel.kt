package xyz.koleno.internetradioplayer.ui.main

import android.os.Build
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.koleno.internetradioplayer.R
import xyz.koleno.internetradioplayer.data.Station
import xyz.koleno.internetradioplayer.data.StationRepository
import xyz.koleno.internetradioplayer.ui.base.BaseViewModel
import xyz.koleno.internetradioplayer.utils.Preferences

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val preferences: Preferences
) :
    BaseViewModel<MainScreenContract.State, MainScreenContract.Event, MainScreenContract.Effect>(
        initialState = MainScreenContract.State.initial()
    ) {

    private var lastPlayed: Station? = null

    override fun handleEvent(event: MainScreenContract.Event) {

        when (event) {
            MainScreenContract.Event.NextButtonClicked ->
                nextButtonClicked()


            MainScreenContract.Event.PlayButtonClicked -> playButtonClicked()


            MainScreenContract.Event.PrevButtonClicked ->
                prevButtonClicked()


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

            is MainScreenContract.Event.PlayError -> {
                event.error?.let {
                    setEffect { MainScreenContract.Effect.ShowToast(R.string.play_error, true) }
                }
                setState {
                    currentState.copy(isPlaying = isPlaying)
                }
            }

            MainScreenContract.Event.ScreenResumed -> viewModelScope.launch(Dispatchers.IO) {
                initialLoad()
            }

            MainScreenContract.Event.AskForNotificationPermissionsClicked -> {
                setState {
                    currentState.copy(showNotificationsDialog = false)
                }

                setEffect {
                    MainScreenContract.Effect.AskForNotificationPermissions
                }
            }

            MainScreenContract.Event.DismissNotifDialogClicked -> {
                setState {
                    currentState.copy(showNotificationsDialog = false)
                }
            }

            MainScreenContract.Event.PermissionsDeniedClicked -> {
                setEffect {
                    MainScreenContract.Effect.ShowToast(R.string.permissions_denied, false)
                }
            }

            MainScreenContract.Event.PermissionsGrantedClicked -> viewModelScope.launch {
                setEffect {
                    MainScreenContract.Effect.RestartService
                }

                lastPlayed?.let {
                    delay(500)

                    setEffect {
                        MainScreenContract.Effect.StartPlaying(it)
                    }
                }
            }
        }
    }

    private fun nextButtonClicked() {
        lastPlayed?.let {
            val stations = currentState.stations
            val indexOf = stations.indexOf(it)
            val next = stations[(indexOf + 1) % stations.size]
            stationButtonClicked(next)
        }
    }

    private fun prevButtonClicked() {
        lastPlayed?.let {
            val stations = currentState.stations
            val indexOf = stations.indexOf(it)
            val prev =
                stations[if (indexOf == 0) stations.size - 1 else (indexOf - 1) % stations.size]
            stationButtonClicked(prev)
        }
    }

    override suspend fun initialLoad() {
        withContext(Dispatchers.IO) {
            stationRepository.getAll().collect {
                setState {
                    currentState.copy(
                        stations = it,
                        columns = preferences.getGridColumns(2),
                        rows = preferences.getGridRows(2),
                        showNotificationsDialog = false
                    )
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
            currentState.copy(
                isPlaying = isPlaying,
                showNotificationsDialog = checkIfNeededToAskForPermissions()
            )
        }
    }

    private fun stationButtonClicked(station: Station) {
        lastPlayed = station

        setState {
            currentState.copy(
                isPlaying = true,
                stationTitle = station.name,
                stationText = " ",
                showNotificationsDialog = checkIfNeededToAskForPermissions()
            )
        }
        setEffect { MainScreenContract.Effect.StartPlaying(station) }
    }

    private fun checkIfNeededToAskForPermissions(): Boolean {
        return if (preferences.hasAskedForNotificationPermissions() || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            false
        } else {
            preferences.setAskedForNotificationPermissions(true)
            true
        }
    }

}