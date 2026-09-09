package com.wenku8.reader.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenku8.reader.core.data.SearchHistoryEntity
import com.wenku8.reader.core.data.WenkuRepository
import com.wenku8.reader.core.data.model.NovelSearchItem
import com.wenku8.reader.core.util.AppLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: WenkuRepository,
) : ViewModel() {

    val history: StateFlow<List<SearchHistoryEntity>> = repo.observeSearchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _results = MutableStateFlow<List<NovelSearchItem>>(emptyList())
    val results: StateFlow<List<NovelSearchItem>> = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    fun search(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _loading.value = true
            _hasSearched.value = true
            repo.addSearchHistory(trimmed)
            AppLog.i("Search", "发起搜索「$trimmed」")
            val list = repo.search(trimmed)
            AppLog.i("Search", "「$trimmed」→ ${list.size} 条")
            _results.value = list
            _loading.value = false
        }
    }

    fun removeHistory(keyword: String) {
        viewModelScope.launch { repo.removeSearchHistory(keyword) }
    }

    fun clearHistory() {
        viewModelScope.launch { repo.clearSearchHistory() }
    }
}
