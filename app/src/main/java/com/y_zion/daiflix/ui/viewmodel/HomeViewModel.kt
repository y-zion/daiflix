package com.y_zion.daiflix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.y_zion.daiflix.data.model.Category
import com.y_zion.daiflix.data.model.VodItem
import com.y_zion.daiflix.data.model.WatchHistory
import com.y_zion.daiflix.data.parser.HomePageData
import com.y_zion.daiflix.data.parser.VodRow
import com.y_zion.daiflix.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val banners: List<VodItem> = emptyList(),
    val rows: List<VodRow> = emptyList(),
    val watchHistory: List<WatchHistory> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: VodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
        observeWatchHistory()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getHomePage()
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            banners = data.banners,
                            rows = data.rows
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "加载失败"
                        )
                    }
                }
        }
    }

    private fun observeWatchHistory() {
        viewModelScope.launch {
            repository.getWatchHistory().collect { history ->
                _uiState.update { it.copy(watchHistory = history.take(10)) }
            }
        }
    }

    fun refresh() {
        loadHomeData()
    }
}
