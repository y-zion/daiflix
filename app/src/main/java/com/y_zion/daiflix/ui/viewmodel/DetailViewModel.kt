package com.y_zion.daiflix.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.y_zion.daiflix.data.model.VodDetail
import com.y_zion.daiflix.data.model.VodItem
import com.y_zion.daiflix.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val detail: VodDetail? = null,
    val isFavorite: Boolean = false,
    val selectedSourceIndex: Int = 0,
    val selectedEpisodeIndex: Int = 0,
    val error: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: VodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vodId: String = savedStateHandle["vodId"] ?: ""

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        if (vodId.isNotBlank()) {
            loadDetail()
            checkFavorite()
        }
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getDetail(vodId)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(isLoading = false, detail = detail)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "加载失败")
                    }
                }
        }
    }

    private fun checkFavorite() {
        viewModelScope.launch {
            val isFav = repository.isFavorite(vodId)
            _uiState.update { it.copy(isFavorite = isFav) }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val detail = _uiState.value.detail ?: return@launch
            repository.toggleFavorite(detail)
            val isFav = repository.isFavorite(vodId)
            _uiState.update { it.copy(isFavorite = isFav) }
        }
    }

    fun selectSource(index: Int) {
        _uiState.update { it.copy(selectedSourceIndex = index, selectedEpisodeIndex = 0) }
    }

    fun selectEpisode(index: Int) {
        _uiState.update { it.copy(selectedEpisodeIndex = index) }
    }

    fun refresh() {
        loadDetail()
    }
}
