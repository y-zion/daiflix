package com.y_zion.daiflix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.y_zion.daiflix.data.model.SearchHistory
import com.y_zion.daiflix.data.model.VodItem
import com.y_zion.daiflix.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<VodItem> = emptyList(),
    val searchHistory: List<SearchHistory> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: VodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeSearchHistory()
    }

    private fun observeSearchHistory() {
        viewModelScope.launch {
            repository.getSearchHistory().collect { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun search(query: String? = null) {
        val keyword = (query ?: _uiState.value.query).trim()
        if (keyword.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(query = keyword, isSearching = true, error = null) }

            // 防抖：连续输入时只保留最后一次请求
            delay(300)

            repository.search(keyword)
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(isSearching = false, results = results)
                    }
                    // 只有真正搜到结果才写入历史，避免把无效关键词也存下来
                    if (results.isNotEmpty()) {
                        repository.saveSearchHistory(keyword)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSearching = false, error = e.message ?: "搜索失败")
                    }
                }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun clearResults() {
        _uiState.update { it.copy(results = emptyList(), query = "", error = null) }
    }
}
