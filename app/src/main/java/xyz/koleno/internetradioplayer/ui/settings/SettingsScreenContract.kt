package xyz.koleno.internetradioplayer.ui.settings

import android.content.Intent
import android.net.Uri
import xyz.koleno.internetradioplayer.data.Station
import xyz.koleno.internetradioplayer.ui.base.BaseContract

class SettingsScreenContract : BaseContract() {

    data class  State(
        val stations: List<Station>,
        val openBottomSheet: Boolean = false,
        val editingStation: Station? = null,
        val isIgnoreSecurityEnabled: Boolean = false,
        val columns: Int = 2,
        val rows: Int = 2
    ) : UiState

    sealed class Event : UiEvent {
        data class OpenBottomSheet(val station: Station?) : Event()
        object DismissBottomSheet : Event()
        data class SaveClicked(val name: String, val uri: String) : Event()
        object AddClicked : Event()
        object DeleteClicked : Event()
        data class IgnoreSecurityChanged(val enabled: Boolean) : Event()
        data class MoveItem(val fromIndex: Int, val toIndex: Int) : Event()
        data class ColumnsChanged(val count: Int) : Event()
        data class RowsChanged(val count: Int): Event()
        object ExportClicked : Event()
        object ImportClicked : Event()
        data class ImportFilePicked(val fileUri: Uri) : Event()
    }

    sealed class Effect : UiEffect {
        object ShowSaveError : Effect()
        data class ShareExport(val intent: Intent) : Effect()
        data class ShareImport(val intent: Intent) : Effect()

        object ShowExportError : Effect()
        object ShowImportError : Effect()
    }

}