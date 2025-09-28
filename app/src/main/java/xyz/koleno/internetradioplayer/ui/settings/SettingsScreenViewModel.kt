package xyz.koleno.internetradioplayer.ui.settings

import android.util.Patterns
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.koleno.internetradioplayer.data.Station
import xyz.koleno.internetradioplayer.data.StationRepository
import xyz.koleno.internetradioplayer.ui.base.BaseViewModel

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val stationRepository: StationRepository
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
                    currentState.copy(stations = it)
                }
            }
        }
    }
}