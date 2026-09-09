package com.wenku8.reader.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wenku8.reader.core.designsystem.theme.ReaderThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.readerDataStore by preferencesDataStore(name = "reader_prefs")

/** Reader + app preferences persisted via DataStore. */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val readerMode = stringPreferencesKey("reader_mode")
        val fontSize = intPreferencesKey("reader_font_size")
        val lineHeight = floatPreferencesKey("reader_line_height")
        val pageFlip = booleanPreferencesKey("reader_page_flip")
        val eink = booleanPreferencesKey("reader_eink")
    }

    val readerMode: Flow<ReaderThemeMode> = context.readerDataStore.data.map { prefs ->
        runCatching { ReaderThemeMode.valueOf(prefs[Keys.readerMode] ?: "DAY") }.getOrDefault(ReaderThemeMode.DAY)
    }
    val fontSize: Flow<Int> = context.readerDataStore.data.map { prefs -> prefs[Keys.fontSize] ?: 18 }
    val lineHeight: Flow<Float> = context.readerDataStore.data.map { prefs -> prefs[Keys.lineHeight] ?: 1.8f }
    val pageFlip: Flow<Boolean> = context.readerDataStore.data.map { prefs -> prefs[Keys.pageFlip] ?: true }
    val eink: Flow<Boolean> = context.readerDataStore.data.map { prefs -> prefs[Keys.eink] ?: false }

    suspend fun setReaderMode(mode: ReaderThemeMode) {
        context.readerDataStore.edit { it[Keys.readerMode] = mode.name }
    }

    suspend fun setFontSize(size: Int) {
        context.readerDataStore.edit { it[Keys.fontSize] = size }
    }

    suspend fun setLineHeight(value: Float) {
        context.readerDataStore.edit { it[Keys.lineHeight] = value }
    }

    suspend fun setPageFlip(enabled: Boolean) {
        context.readerDataStore.edit { it[Keys.pageFlip] = enabled }
    }

    suspend fun setEink(enabled: Boolean) {
        context.readerDataStore.edit { it[Keys.eink] = enabled }
    }
}
