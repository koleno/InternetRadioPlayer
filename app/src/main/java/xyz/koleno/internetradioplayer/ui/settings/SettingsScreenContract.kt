package xyz.koleno.internetradioplayer.ui.settings

import xyz.koleno.internetradioplayer.data.Station
import xyz.koleno.internetradioplayer.ui.base.BaseContract

class SettingsScreenContract : BaseContract() {

    data class State(
        val stations: List<Station>,
        val openBottomSheet: Boolean = false,
        val editingStation: Station? = null
    ) : UiState

    sealed class Event : UiEvent {
        data class OpenBottomSheet(val station: Station?) : Event()
        object DismissBottomSheet : Event()
        data class SaveClicked(val name: String, val uri: String) : Event()
        object AddClicked : Event()
        object DeleteClicked : Event()
    }

    sealed class Effect : UiEffect {
        object ShowSaveError : Effect()
    }

}