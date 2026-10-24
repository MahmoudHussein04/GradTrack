package com.gradtrack.app.ui.common

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.gradtrack.app.AppContainer
import com.gradtrack.app.GradTrackApp
import com.gradtrack.app.R
import com.gradtrack.app.data.AppError
import com.gradtrack.app.data.ErrorKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

val Fragment.container: AppContainer
    get() = (requireActivity().application as GradTrackApp).container

/** `private val vm by appViewModels { MyViewModel(it.team) }` — the factory receives the app's dependency graph. */
inline fun <reified VM : ViewModel> Fragment.appViewModels(crossinline create: (AppContainer) -> VM): Lazy<VM> =
    viewModels<VM> {
        viewModelFactory { initializer { create(container) } }
    }

/** Collects [flow] only while the fragment's view is started, so nothing leaks across view recreation. */
fun <T> Fragment.collect(flow: Flow<T>, block: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collect { block(it) } }
    }
}

/** Shows [UiEvent.Message]/[UiEvent.Failure] as snackbars; [onDone] handles [UiEvent.Done] (close a dialog, navigate...). */
fun Fragment.observeEvents(vm: BaseViewModel, onDone: (UiEvent.Done) -> Unit = {}) {
    collect(vm.events) { event ->
        when (event) {
            is UiEvent.Message -> {
                haptic(success = true)
                showMessage(getString(event.res))
            }
            is UiEvent.Failure -> {
                haptic(success = false)
                showMessage(event.error.message(requireContext()))
            }
            is UiEvent.Done -> {
                haptic(success = true)
                event.message?.let { showMessage(getString(it)) }
                onDone(event)
            }
        }
    }
}

/** A short confirm / reject tick on supported devices, so results are felt as well as seen. */
fun Fragment.haptic(success: Boolean) {
    val view = view ?: return
    val constant =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (success) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
    view.performHapticFeedback(constant)
}

fun Fragment.showMessage(text: String) {
    view?.let { Snackbar.make(it, text, Snackbar.LENGTH_LONG).show() }
}

fun Fragment.hideKeyboard() {
    val view = view ?: return
    (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
}

/** The sentence the user reads for a failure. The backend's own message wins for errors the user can act on. */
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

fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}
