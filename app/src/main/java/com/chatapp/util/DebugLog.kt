package com.chatapp.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {
    private val _entries = MutableStateFlow<List<String>>(emptyList())
    val entries: StateFlow<List<String>> = _entries.asStateFlow()

    private const val MAX_ENTRIES = 200
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val lock = Any()

    fun log(tag: String, message: String) {
        val ts = timeFormat.format(Date())
        val entry = "$ts [$tag] $message"
        android.util.Log.i("ChatApp_$tag", message)
        synchronized(lock) {
            val updated = (_entries.value + entry).takeLast(MAX_ENTRIES)
            _entries.value = updated
        }
    }

    fun clear() {
        synchronized(lock) {
            _entries.value = emptyList()
        }
    }
}
