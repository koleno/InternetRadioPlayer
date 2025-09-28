package xyz.koleno.internetradioplayer.ui.main

import xyz.koleno.internetradioplayer.data.Station
import xyz.koleno.internetradioplayer.ui.base.BaseContract

class MainScreenContract : BaseContract() {

    data class State(
        val stationTitle: String,
        val stationText: String,
        val isPlaying: Boolean,
        val stations: List<Station>
    ) : UiState {

        companion object {
            fun initial(): State {
                return State("", "", false, listOf())
            }
        }

    }

    sealed class Event : UiEvent {
        object PlayButtonClicked : Event()
        object PrevButtonClicked : Event()
        object NextButtonClicked : Event()
        object PlayStarted : Event()
        data class StationButtonClicked(val station: Station) : Event()
        data class RadioTextReceived(val text: String) : Event()
    }

    sealed class Effect : UiEffect {
        data class StartPlaying(val station: Station) : Effect()
        object StopPlaying : Effect()
    }

}