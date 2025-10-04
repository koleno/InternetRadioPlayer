package xyz.koleno.internetradioplayer.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.koleno.internetradioplayer.R
import xyz.koleno.internetradioplayer.data.Station
import xyz.koleno.internetradioplayer.data.StationRepository
import xyz.koleno.internetradioplayer.ui.base.BaseViewModel
import xyz.koleno.internetradioplayer.utils.Preferences
import xyz.koleno.internetradioplayer.utils.restoreDefaultCertificateValidation
import xyz.koleno.internetradioplayer.utils.validateAllCertificates
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter


@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val appContext: Context,
    private val stationRepository: StationRepository,
    private val preferences: Preferences
) :
    BaseViewModel<SettingsScreenContract.State, SettingsScreenContract.Event, SettingsScreenContract.Effect>(
        initialState = SettingsScreenContract.State(listOf())
    ) {

    override fun handleEvent(event: SettingsScreenContract.Event) {
        when (event) {
            SettingsScreenContract.Event.DismissBottomSheet -> {
                setState {
                    currentState.copy(openBottomSheet = false, editingStation = null)
                }
            }

            is SettingsScreenContract.Event.OpenBottomSheet -> {
                setState {
                    currentState.copy(openBottomSheet = true, editingStation = event.station)
                }
            }

            is SettingsScreenContract.Event.SaveClicked -> {
                saveClicked(event.name, event.uri)
            }

            SettingsScreenContract.Event.AddClicked -> {
                setState {
                    currentState.copy(openBottomSheet = true, editingStation = null)
                }
            }

            SettingsScreenContract.Event.DeleteClicked -> {
                deleteClicked()
            }

            is SettingsScreenContract.Event.IgnoreSecurityChanged -> {
                preferences.setIgnoreSecurityEnabled(event.enabled)

                if (event.enabled) {
                    validateAllCertificates()
                } else {
                    restoreDefaultCertificateValidation()
                }

                setState {
                    currentState.copy(isIgnoreSecurityEnabled = event.enabled)
                }
            }

            is SettingsScreenContract.Event.MoveItem -> {
                moveItem(event.fromIndex, event.toIndex)
            }

            is SettingsScreenContract.Event.ColumnsChanged -> {
                preferences.setGridColumns(event.count)

                setState {
                    currentState.copy(columns = event.count)
                }
            }

            is SettingsScreenContract.Event.RowsChanged -> {
                preferences.setGridRows(event.count)

                setState {
                    currentState.copy(rows = event.count)
                }
            }

            SettingsScreenContract.Event.ExportClicked -> {
                exportClicked()
            }

            SettingsScreenContract.Event.ImportClicked -> {
                importClicked()
            }

            is SettingsScreenContract.Event.ImportFilePicked -> {
                importFilePicked(event.fileUri)
            }
        }
    }

    private fun exportClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            val data = state.value.let {
                ImportExport(
                    isIgnoreSecurityEnabled = it.isIgnoreSecurityEnabled,
                    stations = it.stations,
                    columns = it.columns,
                    rows = it.rows
                )
            }

            val json = Json { ignoreUnknownKeys = true }

            try {
                val fileToShare = File(appContext.cacheDir, "export.txt")
                val fos = FileOutputStream(fileToShare)
                val osw = OutputStreamWriter(fos)
                osw.write(json.encodeToString(data))
                osw.close()

                val fileUri =
                    FileProvider.getUriForFile(
                        appContext,
                        appContext.packageName + ".provider",
                        fileToShare
                    )

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    setType("text/plain")
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val shareIntent = Intent.createChooser(
                    sendIntent,
                    appContext.getString(R.string.export)
                )

                setEffect {
                    SettingsScreenContract.Effect.ShareExport(shareIntent)
                }
            } catch (_: IOException) {
                setEffect {
                    SettingsScreenContract.Effect.ShowExportError
                }
            }

        }
    }

    private fun importClicked() {
        setEffect {
            SettingsScreenContract.Effect.ShareImport(
                Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        setType("text/plain")
                    })
        }
    }

    private fun importFilePicked(fileUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            appContext.contentResolver.openInputStream(fileUri).use { inputStream ->
                if (inputStream != null) {
                    try {
                        val bytes = inputStream.readBytes()
                        val rawData = String(bytes)

                        val json = Json { ignoreUnknownKeys = true }
                        val data = json.decodeFromString<ImportExport>(rawData)

                        // load new data
                        stationRepository.deleteAll()
                        stationRepository.insertAll(data.stations)

                        preferences.setGridRows(data.rows)
                        preferences.setGridColumns(data.columns)
                        preferences.setIgnoreSecurityEnabled(data.isIgnoreSecurityEnabled)

                        // make sure to refresh everything
                        initialLoad()
                    } catch (_: Exception) {
                        setEffect {
                            SettingsScreenContract.Effect.ShowImportError
                        }
                    }
                }
            }
        }
    }

    private fun moveItem(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val newlyOrderedList = mutableListOf<Station>().apply {
                addAll(currentState.stations)
                add(toIndex, removeAt(fromIndex))
            }

            // update positions
            val iterator = newlyOrderedList.listIterator()
            while (iterator.hasNext()) {
                val station = iterator.next()
                iterator.set(station.copy(position = iterator.previousIndex()))
            }

            stationRepository.update(newlyOrderedList)
        }
    }

    private fun saveClicked(name: String, uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isNotEmpty() && uri.isNotEmpty() && Patterns.WEB_URL.matcher(
                    uri
                ).matches()
            ) {
                if (currentState.editingStation == null) { // add
                    stationRepository.insert(
                        Station(
                            0,
                            name = name,
                            uri = uri,
                            position = stationRepository.getCount()
                        )
                    )
                } else { // edit
                    currentState.editingStation?.let {
                        stationRepository.update(it.copy(name = name, uri = uri))
                    }
                }

                setState {
                    currentState.copy(openBottomSheet = false, editingStation = null)
                }

            } else {
                setEffect {
                    SettingsScreenContract.Effect.ShowSaveError
                }
            }
        }
    }

    private fun deleteClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            currentState.editingStation?.let {
                stationRepository.delete(it.uid)
            }

            setState {
                currentState.copy(openBottomSheet = false, editingStation = null)
            }
        }
    }

    override suspend fun initialLoad() {
        withContext(Dispatchers.IO) {
            stationRepository.getAll().collect {
                setState {
                    currentState.copy(
                        stations = it,
                        isIgnoreSecurityEnabled = preferences.isIgnoreSecurityEnabled(),
                        columns = preferences.getGridColumns(2),
                        rows = preferences.getGridRows(2)
                    )
                }
            }
        }
    }

    @Serializable
    data class ImportExport(
        val stations: List<Station>,
        val columns: Int,
        val rows: Int,
        val isIgnoreSecurityEnabled: Boolean
    )
}