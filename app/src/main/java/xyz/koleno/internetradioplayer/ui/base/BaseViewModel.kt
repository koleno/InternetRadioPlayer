package xyz.koleno.internetradioplayer.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * BaseViewModel
 *
 * Originally based on https://proandroiddev.com/mvi-architecture-with-kotlin-flows-and-channels-d36820b2028d &
 * https://github.com/worldline/Compose-MVI/blob/main/presentation/src/main/java/com/worldline/composemvi/presentation/ui/base/BaseViewModel.kt
 */
abstract class  BaseViewModel<State : BaseContract.UiState, Event : BaseContract.UiEvent, Effect : BaseContract.UiEffect>(
    val initialState: State
) : ViewModel() {
    val currentState: State
        get() = state.value

    private val _uiState : MutableStateFlow<State> = MutableStateFlow(initialState)

    val state: StateFlow<State> by lazy {
        _uiState.onStart {
            viewModelScope.launch {
                initialLoad()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = initialState
        )
    }

    private val _event : MutableSharedFlow<Event> = MutableSharedFlow()
    val event = _event.asSharedFlow()

    private val _effect : Channel<Effect> = Channel()
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            event.collect { handleEvent(it) }
        }
    }

    fun setEvent(event : Event) {
        val newEvent = event
        viewModelScope.launch { _event.emit(newEvent) }
    }

    protected fun setState(reduce: State.() -> State) {
        val newState = currentState.reduce()
        _uiState.value = newState
    }


    protected fun setEffect(builder: () -> Effect) {
        val effectValue = builder()
        viewModelScope.launch { _effect.send(effectValue) }
    }

    protected abstract fun handleEvent(event: Event)

    protected abstract suspend fun  initialLoad()
}