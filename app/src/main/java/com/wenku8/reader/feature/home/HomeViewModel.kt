package com.wenku8.reader.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenku8.reader.core.data.ReadingProgressEntity
import com.wenku8.reader.core.data.WenkuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    repo: WenkuRepository,
) : ViewModel() {

    /** Novels read recently (most recent first) for the "最近阅读" section. */
    val recentReads: StateFlow<List<ReadingProgressEntity>> = repo.observeRecentReads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
