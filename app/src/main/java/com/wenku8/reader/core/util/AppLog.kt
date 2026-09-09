package com.wenku8.reader.core.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small in-memory log for the in-app developer page (no debugger needed).
 * Mirrors every line to Logcat too, so `adb logcat` still works.
 */
object AppLog {
    private const val MAX_LINES = 500
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun d(tag: String, msg: String) = append(Log.DEBUG, tag, msg)
    fun i(tag: String, msg: String) = append(Log.INFO, tag, msg)
    fun w(tag: String, msg: String) = append(Log.WARN, tag, msg)
    fun e(tag: String, msg: String, t: Throwable? = null) {
        val suffix = t?.let { " | ${it.javaClass.simpleName}: ${it.message}" } ?: ""
        append(Log.ERROR, tag, msg + suffix)
    }

    fun clear() {
        _lines.value = emptyList()
    }

    fun allText(): String = _lines.value.joinToString("\n")

    private fun append(level: Int, tag: String, msg: String) {
        val line = "${formatter.format(Date())} ${levelChar(level)}/$tag: $msg"
        _lines.value = (_lines.value + line).takeLast(MAX_LINES)
        Log.println(level, "Wenku8-$tag", msg)
    }

    private fun levelChar(level: Int): String = when (level) {
        Log.ERROR -> "E"
        Log.WARN -> "W"
        Log.INFO -> "I"
        else -> "D"
    }
}
