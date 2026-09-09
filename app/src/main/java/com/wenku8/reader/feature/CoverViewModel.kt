package com.wenku8.reader.feature

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenku8.reader.core.data.WenkuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Loads novel covers on demand and caches them per aid (delegates to a shared LruCache). */
@HiltViewModel
class CoverViewModel @Inject constructor(
    private val repo: WenkuRepository,
) : ViewModel() {

    private val covers = mutableMapOf<Int, MutableStateFlow<ImageBitmap?>>()

    fun cover(aid: Int): StateFlow<ImageBitmap?> = covers.getOrPut(aid) {
        MutableStateFlow<ImageBitmap?>(null).also { flow ->
            viewModelScope.launch { flow.value = repo.loadCoverBitmap(aid) }
        }
    }
}
