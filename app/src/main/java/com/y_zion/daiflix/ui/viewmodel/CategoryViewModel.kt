package com.y_zion.daiflix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.y_zion.daiflix.data.model.Category
import com.y_zion.daiflix.data.model.VodItem
import com.y_zion.daiflix.data.parser.CategoryData
import com.y_zion.daiflix.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val isLoading: Boolean = true,
    val category: Category = Category.MOVIE,
    val items: List<VodItem> = emptyList(),
    val hasMore: Boolean = false,
    val currentPage: Int = 1,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: VodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    fun loadCategory(category: Category) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    category = category,
                    items = emptyList(),
                    currentPage = 1,
                    hasMore = false,
                    error = null
                )
            }
            repository.getCategory(category, page = 1)
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = data.items,
                            hasMore = data.hasMore,
                            currentPage = 1
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "加载失败")
                    }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = state.currentPage + 1
            repository.getCategory(state.category, page = nextPage)
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            items = it.items + data.items,
                            hasMore = data.hasMore,
                            currentPage = nextPage
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoadingMore = false, error = e.message)
                    }
                }
        }
    }

    fun refresh() {
        loadCategory(_uiState.value.category)
    }
}
