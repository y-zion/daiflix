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
     * Hero copy and artwork must come from the same source. The site's carousel
     * mixes real title tiles with generic promo tiles (「动作片」「电视剧」…), and the
     * old code pasted a *random* row item's title/synopsis over a promo tile's
     * artwork — that is why the hero showed A's poster with B's copy, and why it
     * changed on every refresh.
     *
     * Rules:
     *  - carousel tile points at a title we also list → promote that title's
     *    metadata onto the tile (same vod, same artwork);
     *  - generic promo tile → it has no honest copy, so drop it instead of
     *    inventing a pairing;
     *  - nothing left → fall back to real content cards (art + copy already match).
     */
    private fun buildHeroBanners(data: HomePageData): List<VodItem> {
        val contentItems = data.rows
            .flatMap { it.items }
            .filter { it.title.isNotBlank() && it.coverUrl.isNotBlank() }
            .distinctBy { it.id }
        val contentById = contentItems.associateBy { it.id }

        val matched = data.banners.mapNotNull { banner ->
            val rich = contentById[banner.id] ?: return@mapNotNull null
            if (isGenericHeroTitle(rich.title)) return@mapNotNull null
            banner.copy(
                title = rich.title,
                coverUrl = banner.coverUrl.ifBlank { rich.coverUrl },
                year = rich.year,
                description = rich.description,
                episodeCount = rich.episodeCount,
                rating = rich.rating
            )
        }.distinctBy { it.id }

        val hero = matched.ifEmpty {
            contentItems.filterNot { isGenericHeroTitle(it.title) }.ifEmpty { contentItems }
        }
        return hero.take(HERO_COUNT)
    }

    private fun isGenericHeroTitle(title: String): Boolean {
        if (title in GENERIC_HERO_TITLES) return true
        if (title.length > 8) return false
        return GENERIC_HERO_SUFFIXES.any { title.endsWith(it) }
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
