package com.yzion.daiflix.tv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yzion.daiflix.tv.data.model.Category
import com.yzion.daiflix.tv.data.model.VodItem
import com.yzion.daiflix.tv.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 分类页排序方式。全部是客户端本地排序：只重排已加载的 [VodItem]，
 * 不重新请求网络、不进入 loading（审计 P2-8）。
 */
enum class CategorySort(val label: String) {
    DEFAULT("默认"),
    YEAR_DESC("年份新→旧"),
    RATING_DESC("评分高→低")
}

/**
 * 一个年份筛选档位。[years] 是该档位覆盖的具体年份集合；空集合表示「全部」。
 *
 * 档位只由已加载数据里**真实出现过的年份**生成，因此不会出现点了必然为空的
 * 档位；超过 [MAX_EXPLICIT_YEARS] 种年份时，最老的若干年份合并进「更早」。
 */
data class YearFilterOption(
    val id: String,
    val label: String,
    val years: Set<String> = emptySet()
) {
    companion object {
        const val ALL_ID = "all"
        const val OLDER_ID = "older"

        /** 明面上单独展示的年份档位数量，其余更老的年份合并进「更早」。 */
        const val MAX_EXPLICIT_YEARS = 4
    }
}

data class CategoryUiState(
    val isLoading: Boolean = true,
    val category: Category = Category.MOVIE,
    /** 仓库返回的原始全量列表；年份筛选/排序永远不会改写它。 */
    val items: List<VodItem> = emptyList(),
    /** items 经年份筛选 + 排序后的派生列表，UI 只渲染它。 */
    val visibleItems: List<VodItem> = emptyList(),
    /** 可用的年份档位，首项恒为「全部」。 */
    val yearOptions: List<YearFilterOption> = emptyList(),
    val selectedYearId: String = YearFilterOption.ALL_ID,
    val sort: CategorySort = CategorySort.DEFAULT,
    val error: String? = null
) {
    /** 已加载总条数（全量）。 */
    val totalCount: Int get() = items.size

    /** 筛选后仍可见的条数。 */
    val filteredCount: Int get() = visibleItems.size

    /** 当前是否启用了年份筛选（排序不算筛选，不改变条数）。 */
    val isYearFiltered: Boolean get() = selectedYearId != YearFilterOption.ALL_ID
}

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
                    visibleItems = emptyList(),
                    yearOptions = emptyList(),
                    // 换分类/重新加载时回到「全部 + 默认排序」，避免沿用上一个分类的筛选。
                    selectedYearId = YearFilterOption.ALL_ID,
                    sort = CategorySort.DEFAULT,
                    error = null
                )
            }
            repository.getCategory(category)
                .onSuccess { data ->
                    _uiState.update { derive(it.copy(isLoading = false, items = data.items)) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "加载失败")
                    }
                }
        }
    }

    /** 选择年份档位：纯本地过滤，不重新请求网络、不闪 loading。 */
    fun selectYear(optionId: String) {
        _uiState.update { state ->
            if (optionId == state.selectedYearId) {
                state
            } else {
                derive(state.copy(selectedYearId = optionId))
            }
        }
    }

    /** 选择排序方式：纯本地排序。 */
    fun selectSort(sort: CategorySort) {
        _uiState.update { state ->
            if (sort == state.sort) state else derive(state.copy(sort = sort))
        }
    }

    /** 重置为「全部年份 + 默认排序」，用于筛选空态里的「重置筛选」。 */
    fun resetFilters() {
        _uiState.update { state ->
            if (state.selectedYearId == YearFilterOption.ALL_ID && state.sort == CategorySort.DEFAULT) {
                state
            } else {
                derive(
                    state.copy(
                        selectedYearId = YearFilterOption.ALL_ID,
                        sort = CategorySort.DEFAULT
                    )
                )
            }
        }
    }

    fun refresh() {
        loadCategory(_uiState.value.category)
    }
}

/**
 * 由原始 [CategoryUiState.items] 派生年份档位、筛选项与排序结果。
 *
 * 纯函数、无副作用：网络数据只在这里被读取一次，筛选/排序不会改动 items。
 */
private fun derive(state: CategoryUiState): CategoryUiState {
    val yearOptions = buildYearOptions(state.items)
    // 数据变化（重新加载）后旧选择可能已不存在，回落到「全部」。
    val selected = yearOptions.firstOrNull { it.id == state.selectedYearId }
        ?: yearOptions.firstOrNull { it.id == YearFilterOption.ALL_ID }
    val wantedYears = selected?.years.orEmpty()
    val filtered = if (wantedYears.isEmpty()) {
        state.items
    } else {
        state.items.filter { item ->
            val year = parseYear(item.year)
            year != null && wantedYears.contains(year.toString())
        }
    }
    val sorted = when (state.sort) {
        CategorySort.DEFAULT -> filtered
        // sortedWith 是稳定排序：同一年份/评分的条目保持原顺序，空年份/评分排到最后。
        CategorySort.YEAR_DESC ->
            filtered.sortedWith(compareByDescending<VodItem> { parseYear(it.year) ?: 0 })
        CategorySort.RATING_DESC ->
            filtered.sortedWith(compareByDescending<VodItem> { parseRating(it.rating) })
    }
    return state.copy(
        yearOptions = yearOptions,
        selectedYearId = selected?.id ?: YearFilterOption.ALL_ID,
        visibleItems = sorted
    )
}

/**
 * 生成年份档位：全部 → 最新的 [YearFilterOption.MAX_EXPLICIT_YEARS] 个年份 →
 * （若还有更老的年份）「更早」。数据里没有年份时只保留「全部」。
 */
private fun buildYearOptions(items: List<VodItem>): List<YearFilterOption> {
    val options = mutableListOf(YearFilterOption(YearFilterOption.ALL_ID, "全部"))
    val years = items.mapNotNull { parseYear(it.year) }.distinct().sortedDescending()
    if (years.isEmpty()) return options

    years.take(YearFilterOption.MAX_EXPLICIT_YEARS).forEach { year ->
        options += YearFilterOption(
            id = year.toString(),
            label = year.toString(),
            years = setOf(year.toString())
        )
    }
    val olderYears = years.drop(YearFilterOption.MAX_EXPLICIT_YEARS)
    if (olderYears.isNotEmpty()) {
        options += YearFilterOption(
            id = YearFilterOption.OLDER_ID,
            label = "更早",
            years = olderYears.map { it.toString() }.toSet()
        )
    }
    return options
}

/** 年份字段可能带日期（如 "2023-05-01"），取其中第一段四位数字。 */
private fun parseYear(raw: String): Int? =
    Regex("\\d{4}").find(raw)?.value?.toIntOrNull()

/** 评分可能是 "8.2" / "8.2分" / 空串，解析失败按 0 处理（排到最后）。 */
private fun parseRating(raw: String): Double =
    Regex("\\d+(?:\\.\\d+)?").find(raw)?.value?.toDoubleOrNull() ?: 0.0
