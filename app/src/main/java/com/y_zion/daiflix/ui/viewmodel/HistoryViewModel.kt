package com.y_zion.daiflix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.y_zion.daiflix.data.model.WatchHistory
import com.y_zion.daiflix.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: VodRepository
) : ViewModel() {
    val watchHistory: StateFlow<List<WatchHistory>> = repository.getWatchHistory()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
}
