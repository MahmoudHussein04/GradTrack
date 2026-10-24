package com.gradtrack.app.ui.common

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gradtrack.app.data.AppError
import com.gradtrack.app.data.Resource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** What a screen shows while loading its data: a spinner, the data, or an error with a retry button. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class Success<T>(val data: T) : UiState<T>

    data class Error(val error: AppError) : UiState<Nothing>
}

fun <T> Resource<T>.toUiState(): UiState<T> =
    when (this) {
        is Resource.Success -> UiState.Success(data)
        is Resource.Failure -> UiState.Error(error)
    }

/** One-shot results of user actions (a snackbar, a navigation step). Delivered once, never replayed on rotation. */
sealed interface UiEvent {
    data class Message(@StringRes val res: Int) : UiEvent

    data class Failure(val error: AppError) : UiEvent

    data class Done(@StringRes val message: Int? = null) : UiEvent
}

abstract class BaseViewModel : ViewModel() {
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _busy = MutableStateFlow(false)

    /** True while an action (save, submit, ...) is running: screens disable their buttons. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    protected fun send(event: UiEvent) {
        _events.trySend(event)
    }

    /** Runs a write call with the busy flag; a failure becomes a [UiEvent.Failure], success goes to [onSuccess]. */
    protected fun <T> act(
        block: suspend () -> Resource<T>,
        onSuccess: suspend (T) -> Unit = {},
    ) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            when (val result = block()) {
                is Resource.Success -> onSuccess(result.data)
                is Resource.Failure -> send(UiEvent.Failure(result.error))
            }
            _busy.value = false
        }
    }
}

/** A screen backed by one `GET`: loads on creation, can be retried, and refreshed without losing what is on screen. */
abstract class LoadableViewModel<T>(private val fetch: suspend () -> Resource<T>) : BaseViewModel() {
    private val _state = MutableStateFlow<UiState<T>>(UiState.Loading)
    val state: StateFlow<UiState<T>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch { _state.value = fetch().toUiState() }
    }

    /** Pull-to-refresh: keeps the current content, reports a failure as a snackbar instead of an error screen. */
    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            when (val result = fetch()) {
                is Resource.Success -> _state.value = UiState.Success(result.data)
                is Resource.Failure -> if (_state.value !is UiState.Success) _state.value = UiState.Error(result.error) else send(UiEvent.Failure(result.error))
            }
            _refreshing.value = false
        }
    }

    /** Applies a local change to the loaded data (e.g. a task whose status was just saved). */
    protected fun patch(transform: (T) -> T) {
        val current = _state.value
        if (current is UiState.Success) _state.value = UiState.Success(transform(current.data))
    }
}
