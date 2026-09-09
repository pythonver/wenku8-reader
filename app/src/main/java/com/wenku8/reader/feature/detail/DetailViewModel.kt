package com.wenku8.reader.feature.detail

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenku8.reader.core.data.WenkuRepository
import com.wenku8.reader.core.data.model.NovelDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repo: WenkuRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val aid: Int = savedStateHandle.get<Int>("aid") ?: 0

    private val _detail = MutableStateFlow<NovelDetail?>(null)
    val detail: StateFlow<NovelDetail?> = _detail.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _cover = MutableStateFlow<ImageBitmap?>(null)
    val cover: StateFlow<ImageBitmap?> = _cover.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    /** Chapter name of the saved progress, or null when there is none. */
    private val _resumeHint = MutableStateFlow<String?>(null)
    val resumeHint: StateFlow<String?> = _resumeHint.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _isFavorite.value = repo.isFavorite(aid)
            val detail = repo.fetchDetail(aid)
            _detail.value = detail
            _resumeHint.value = detail?.let { d ->
                val p = repo.getProgress(aid)
                if (p != null && p.cid > 0) {
                    d.volumes.flatMap { it.chapters }.firstOrNull { it.cid == p.cid }?.name ?: "上次阅读位置"
                } else null
            }
            _loading.value = false
            _cover.value = repo.fetchCoverBitmap(aid)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val d = _detail.value ?: return@launch
            if (_isFavorite.value) {
                repo.removeFavorite(aid)
                _isFavorite.value = false
            } else {
                repo.addFavorite(d.meta, d.intro)
                _isFavorite.value = true
            }
        }
    }
}
