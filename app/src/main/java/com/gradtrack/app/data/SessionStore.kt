package com.gradtrack.app.data

import android.content.Context
import com.google.gson.Gson
import com.gradtrack.app.data.model.User
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("gradtrack_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()

    val token: String? get() = prefs.getString(KEY_TOKEN, null)

    private val _currentUser = MutableStateFlow(readUser())

    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    var user: User?
        get() = _currentUser.value
        set(value) {
            prefs.edit().putString(KEY_USER, value?.let(gson::toJson)).apply()
            _currentUser.value = value
        }

    private fun readUser(): User? = prefs.getString(KEY_USER, null)?.let { runCatching { gson.fromJson(it, User::class.java) }.getOrNull() }

    val isSignedIn: Boolean get() = token != null && user != null

    fun save(token: String, user: User) {
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_USER, gson.toJson(user)).apply()
        _currentUser.value = user
    }

    fun clear() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    fun onUnauthorized() {
        clear()
        _expired.tryEmit(Unit)
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER = "user"
    }
}
