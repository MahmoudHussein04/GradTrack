package com.gradtrack.app.ui.common

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gradtrack.app.AppContainer
import com.gradtrack.app.R
import com.gradtrack.app.data.AppError
import com.gradtrack.app.data.ErrorKind

val LocalContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("SnackbarHostState not provided") }

val LocalTabBarSpace = staticCompositionLocalOf<Dp> { 0.dp }

@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalContainer.current
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}

@Composable
fun EventsEffect(vm: BaseViewModel, onDone: (UiEvent.Done) -> Unit = {}) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = LocalSnackbar.current
    val haptics = LocalHapticFeedback.current
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(vm) {
        vm.events.collect { event ->
            val (text, success) =
                when (event) {
                    is UiEvent.Message -> resources.getString(event.res) to true
                    is UiEvent.Failure -> event.error.message(context) to false
                    is UiEvent.Done -> event.message?.let(resources::getString) to true
                }
            haptics.performHapticFeedback(if (success) HapticFeedbackType.Confirm else HapticFeedbackType.Reject)
            if (event is UiEvent.Done) done(event)
            text?.let { snackbar.showSnackbar(it) }
        }
    }
}

fun AppError.message(context: Context): String =
    when (kind) {
        ErrorKind.NO_CONNECTION -> context.getString(R.string.error_no_connection)
        ErrorKind.TIMEOUT -> context.getString(R.string.error_timeout)
        ErrorKind.UNAUTHORIZED -> serverMessage ?: context.getString(R.string.error_unauthorized)
        ErrorKind.FORBIDDEN -> serverMessage ?: context.getString(R.string.error_forbidden)
        ErrorKind.NOT_FOUND -> serverMessage ?: context.getString(R.string.error_not_found)
        ErrorKind.CONFLICT, ErrorKind.VALIDATION -> serverMessage ?: context.getString(R.string.error_validation)
        ErrorKind.SERVER -> context.getString(R.string.error_server)
        ErrorKind.BAD_RESPONSE -> context.getString(R.string.error_bad_response)
        ErrorKind.UNKNOWN -> context.getString(R.string.error_unknown)
    }
