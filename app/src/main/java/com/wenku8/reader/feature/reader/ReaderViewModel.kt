package com.wenku8.reader.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenku8.reader.core.data.BookmarkEntity
import com.wenku8.reader.core.data.WenkuRepository
import com.wenku8.reader.core.data.model.NovelContentItem
import com.wenku8.reader.core.designsystem.theme.ReaderThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChapterRef(val vid: Int, val cid: Int, val name: String)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repo: WenkuRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val aid: Int = savedStateHandle.get<Int>("aid") ?: 0
    private val targetCid: Int? = (savedStateHandle.get<Int>("cid") ?: -1).takeIf { it > 0 }

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterRef>>(emptyList())
    val chapters: StateFlow<List<ChapterRef>> = _chapters.asStateFlow()

    private val _chapterIndex = MutableStateFlow(0)
    val chapterIndex: StateFlow<Int> = _chapterIndex.asStateFlow()

    /** Normalized chapter body (TEXT paragraphs prefixed with two-space indent). */
    private val _content = MutableStateFlow<List<NovelContentItem>>(emptyList())
    val content: StateFlow<List<NovelContentItem>> = _content.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** The anchor to restore (and keep current) for the loaded chapter. */
    private val _anchor = MutableStateFlow(ReaderAnchor(0, 0))
    val anchor: StateFlow<ReaderAnchor> = _anchor.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookmarks.asStateFlow()

    // ---- settings (DataStore) ----
    val fontSize: StateFlow<Int> = repo.preferences.fontSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, 18)
    val lineHeight: StateFlow<Float> = repo.preferences.lineHeight
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.8f)
    val readerMode: StateFlow<ReaderThemeMode> = repo.preferences.readerMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReaderThemeMode.DAY)
    val pageFlip: StateFlow<Boolean> = repo.preferences.pageFlip
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val eink: StateFlow<Boolean> = repo.preferences.eink
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            viewModelScope.launch {
                repo.observeBookmarks(aid).collect { _bookmarks.value = it }
            }
            val detail = repo.fetchDetail(aid)
            if (detail == null) {
                _loading.value = false
                return@launch
            }
            _title.value = detail.meta.title
            val flat = detail.volumes.flatMap { v ->
                v.chapters.map { ChapterRef(v.vid, it.cid, it.name) }
            }
            _chapters.value = flat
            if (flat.isEmpty()) {
                _loading.value = false
                return@launch
            }

            var index = 0
            var restore = ReaderAnchor(0, 0)
            if (targetCid != null) {
                index = flat.indexOfFirst { it.cid == targetCid }.coerceAtLeast(0)
            } else {
                val progress = repo.getProgress(aid)
                if (progress != null && progress.cid > 0) {
                    index = flat.indexOfFirst { it.cid == progress.cid }.coerceAtLeast(0)
                    restore = ReaderAnchor(progress.elementIndex, progress.charOffset)
                }
            }
            _chapterIndex.value = index
            loadChapter(index, restore)
        }
    }

    private fun loadChapter(index: Int, restoreAnchor: ReaderAnchor) {
        viewModelScope.launch {
            _loading.value = true
            val chapter = _chapters.value.getOrNull(index) ?: run {
                _loading.value = false
                return@launch
            }
            val raw = repo.fetchChapter(aid, chapter.cid)
            _content.value = raw.map { item ->
                if (item.type == NovelContentItem.ContentType.TEXT &&
                    !item.content.startsWith("　")
                ) item.copy(content = "　　" + item.content) else item
            }
            _anchor.value = restoreAnchor
            _loading.value = false
        }
    }

    // ---- page / chapter navigation ----

    /** Called when the pager settles on [pageIndex]; persists the stable anchor. */
    fun onPageSelected(pages: List<ReaderPage>, pageIndex: Int) {
        val page = pages.getOrNull(pageIndex) ?: return
        _anchor.value = page.anchor
        viewModelScope.launch {
            val chapter = _chapters.value.getOrNull(_chapterIndex.value) ?: return@launch
            repo.saveProgress(
                aid = aid,
                title = _title.value,
                vid = chapter.vid,
                cid = chapter.cid,
                elementIndex = page.anchor.elementIndex,
                charOffset = page.anchor.charOffset,
            )
        }
    }

    fun nextChapter() = jumpChapter(_chapterIndex.value + 1)

    fun previousChapter() = jumpChapter(_chapterIndex.value - 1)

    fun openChapter(index: Int, startAnchor: ReaderAnchor = ReaderAnchor(0, 0)) {
        if (index in _chapters.value.indices) {
            _chapterIndex.value = index
            loadChapter(index, startAnchor)
        }
    }

    private fun jumpChapter(index: Int) {
        openChapter(index, ReaderAnchor(0, 0))
    }

    // ---- bookmarks ----

    fun addBookmark(name: String, note: String) {
        viewModelScope.launch {
            val chapter = _chapters.value.getOrNull(_chapterIndex.value) ?: return@launch
            val a = _anchor.value
            val excerpt = _content.value.getOrNull(a.elementIndex)?.content
                ?.substring(a.charOffset.coerceAtMost(60))
                ?.trim()?.take(40) ?: ""
            repo.addBookmark(
                BookmarkEntity(
                    aid = aid,
                    vid = chapter.vid,
                    cid = chapter.cid,
                    elementIndex = a.elementIndex,
                    charOffset = a.charOffset,
                    name = name.ifBlank { chapter.name },
                    note = note,
                    excerpt = excerpt,
                )
            )
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch { repo.removeBookmark(id) }
    }

    // ---- settings ----

    fun setFontSize(size: Int) = viewModelScope.launch { repo.preferences.setFontSize(size) }
    fun setLineHeight(value: Float) = viewModelScope.launch { repo.preferences.setLineHeight(value) }
    fun setReaderMode(mode: ReaderThemeMode) = viewModelScope.launch { repo.preferences.setReaderMode(mode) }
    fun setPageFlip(enabled: Boolean) = viewModelScope.launch { repo.preferences.setPageFlip(enabled) }
    fun setEink(enabled: Boolean) = viewModelScope.launch { repo.preferences.setEink(enabled) }
}
