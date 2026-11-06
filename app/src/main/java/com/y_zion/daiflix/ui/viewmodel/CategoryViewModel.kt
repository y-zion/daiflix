package com.y_zion.daiflix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.y_zion.daiflix.data.model.Category
import com.y_zion.daiflix.data.model.VodItem
import com.y_zion.daiflix.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val isLoading: Boolean = true,
    val category: Category = Category.MOVIE,
    val items: List<VodItem> = emptyList(),
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
                    error = null
                )
            }
            repository.getCategory(category)
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(isLoading = false, items = data.items)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "加载失败")
                    }
                }
        }
    }

    fun refresh() {
        loadCategory(_uiState.value.category)
    }
}
