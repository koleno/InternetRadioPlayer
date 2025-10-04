package xyz.koleno.internetradioplayer.ui.main

import xyz.koleno.internetradioplayer.data.Station
import xyz.koleno.internetradioplayer.ui.base.BaseContract

class MainScreenContract : BaseContract() {

    data class State(
        val stationTitle: String,
        val stationText: String,
        val isPlaying: Boolean,
        val stations: List<Station>,
        val rows: Int,
        val columns: Int,
        val showNotificationsDialog: Boolean
    ) : UiState {

        companion object {
            fun initial(): State {
                return State("", "", false, listOf(), 1, 1, false)
            }
        }

    }

    sealed class Event : UiEvent {
        object PlayButtonClicked : Event()
        object PrevButtonClicked : Event()
        object NextButtonClicked : Event()
        object PlayStarted : Event()
        data class PlayError(val error: String?) : Event()
        data class StationButtonClicked(val station: Station) : Event()
        data class RadioTextReceived(val text: String) : Event()
        object ScreenResumed : Event()
        object DismissNotifDialogClicked: Event()
        object AskForNotificationPermissionsClicked: Event()
        object PermissionsGrantedClicked: Event()
        object PermissionsDeniedClicked: Event()
    }

    sealed class Effect : UiEffect {
        data class StartPlaying(val station: Station) : Effect()
        object StopPlaying : Effect()
        data class ShowToast(val message: Int, val sound: Boolean) : Effect()
        object AskForNotificationPermissions : Effect()
        object RestartService : Effect()
    }

}