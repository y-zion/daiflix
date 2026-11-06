package com.yzion.daiflix.tv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yzion.daiflix.tv.data.model.Category
import com.yzion.daiflix.tv.data.model.VodItem
import com.yzion.daiflix.tv.data.model.WatchHistory
import com.yzion.daiflix.tv.data.parser.HomePageData
import com.yzion.daiflix.tv.data.parser.VodRow
import com.yzion.daiflix.tv.data.repository.VodRepository
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
                            banners = buildHeroBanners(data),
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

    /**
     * The site's carousel contains generic promo tiles (「动作片」「电视剧」…) that
     * carry no year/synopsis, while the content rows hold real metadata. Prefer the
     * carousel artwork but back it with a real title/metadata item so the hero can
     * render a Netflix-style copy block; fall back to content cards when needed.
     */
    private fun buildHeroBanners(data: HomePageData): List<VodItem> {
        val contentItems = data.rows
            .flatMap { it.items }
            .filter { it.title.isNotBlank() && it.coverUrl.isNotBlank() }
            .distinctBy { it.id }
        val contentById = contentItems.associateBy { it.id }

        val enriched = data.banners.mapNotNull { banner ->
            val rich = contentById[banner.id]
            when {
                rich != null && !isGenericHeroTitle(banner.title) ->
                    banner.copy(
                        title = rich.title,
                        year = rich.year,
                        description = rich.description,
                        episodeCount = rich.episodeCount,
                        rating = rich.rating
                    )

                isGenericHeroTitle(banner.title) -> {
                    val pick = contentItems.randomOrNullVod(banner.id)
                    pick?.copy(coverUrl = banner.coverUrl.ifBlank { pick.coverUrl })
                }

                else -> banner
            }
        }.filter { !isGenericHeroTitle(it.title) }

        val hero = enriched.ifEmpty { contentItems.take(HERO_COUNT) }
        return hero.take(HERO_COUNT)
    }

    private fun isGenericHeroTitle(title: String): Boolean {
        if (title in GENERIC_HERO_TITLES) return true
        if (title.length > 8) return false
        return GENERIC_HERO_SUFFIXES.any { title.endsWith(it) }
    }

    private fun List<VodItem>.randomOrNullVod(excludeId: String): VodItem? =
        filter { it.id != excludeId }.let { list ->
            if (list.isEmpty()) firstOrNull() else list[(list.size / 2).coerceIn(0, list.lastIndex)]
        }

    private companion object {
        const val HERO_COUNT = 5
        val GENERIC_HERO_TITLES = setOf(
            "首页", "电影", "电视剧", "综艺", "动漫", "纪录片", "全部",
            "动作片", "喜剧片", "爱情片", "科幻片", "恐怖片", "剧情片",
            "战争片", "动画片", "冒险片", "悬疑片", "奇幻片", "犯罪片",
            "热门", "推荐", "最新", "排行榜"
        )
        val GENERIC_HERO_SUFFIXES = listOf(
            "电影", "电视剧", "综艺", "动漫", "纪录片", "片", "剧"
        )
    }
}
