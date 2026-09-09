package com.wenku8.reader.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenku8.reader.core.data.FavoriteNovelEntity
import com.wenku8.reader.core.data.WenkuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: WenkuRepository,
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteNovelEntity>> = repo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun remove(aid: Int) {
        viewModelScope.launch { repo.removeFavorite(aid) }
    }
}
