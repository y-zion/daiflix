package com.y_zion.daiflix.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.y_zion.daiflix.ui.component.EpisodeSelector
import com.y_zion.daiflix.ui.component.VodRow
import com.y_zion.daiflix.ui.theme.DaiflixColors
import com.y_zion.daiflix.ui.theme.DaiflixSpacing
import com.y_zion.daiflix.ui.viewmodel.DetailViewModel

@Composable
fun DetailScreen(
    vodId: String,
    onPlayClick: (String, Int, Int) -> Unit,
    onVodClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(onBack = onBack)

    // 从播放页返回时刷新“继续播放”进度
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshWatchHistory()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(vodId) {
        // vodId comes from navigation, ViewModel already has it via SavedStateHandle
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Black)
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("加载中...", color = DaiflixColors.LightGray)
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.error ?: "加载失败", color = DaiflixColors.Red)
                    Spacer(modifier = Modifier.height(DaiflixSpacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm)) {
                        Surface(
                            onClick = { viewModel.refresh() },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = DaiflixColors.Red,
                                focusedContainerColor = DaiflixColors.DarkRed
                            )
                        ) {
                            Text(
                                text = "重试",
                                color = DaiflixColors.White,
                                modifier = Modifier.padding(
                                    horizontal = DaiflixSpacing.lg,
                                    vertical = DaiflixSpacing.xs
                                )
                            )
                        }
                        Surface(
                            onClick = onBack,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = DaiflixColors.DarkCard
                            )
                        ) {
                            Text(
                                text = "返回",
                                color = DaiflixColors.White,
                                modifier = Modifier.padding(
                                    horizontal = DaiflixSpacing.lg,
                                    vertical = DaiflixSpacing.xs
                                )
                            )
                        }
                    }
                }
            }
        } else {
            val detail = uiState.detail ?: return

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 背景大图
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    AsyncImage(
                        model = detail.backdropUrl.ifBlank { detail.coverUrl },
                        contentDescription = detail.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 渐变遮罩
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        DaiflixColors.Black
                                    )
                                )
                            )
                    )

                    // 返回按钮
                    Surface(
                        onClick = onBack,
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = DaiflixColors.Black.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .padding(DaiflixSpacing.lg)
                            .size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("←", color = DaiflixColors.White, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }

                // 详情内容
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DaiflixSpacing.xl)
                        .offset(y = (-80).dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.lg)
                    ) {
                        // 海报
                        AsyncImage(
                            model = detail.coverUrl,
                            contentDescription = detail.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(180.dp)
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        // 信息
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = detail.title,
                                style = MaterialTheme.typography.displaySmall,
                                color = DaiflixColors.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(DaiflixSpacing.xs))

                            // 元数据
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm)
                            ) {
                                if (detail.rating.isNotBlank()) {
                                    Text(
                                        text = "★ ${detail.rating}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DaiflixColors.Gold
                                    )
                                }
                                if (detail.year.isNotBlank()) {
                                    Text(
                                        text = detail.year,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DaiflixColors.LightGray
                                    )
                                }
                                if (detail.region.isNotBlank()) {
                                    Text(
                                        text = detail.region,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DaiflixColors.LightGray
                                    )
                                }
                            }

                            if (detail.director.isNotBlank()) {
                                Spacer(modifier = Modifier.height(DaiflixSpacing.xxs))
                                Text(
                                    text = "导演: ${detail.director}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DaiflixColors.LightGray
                                )
                            }

                            if (detail.actors.isNotBlank()) {
                                Spacer(modifier = Modifier.height(DaiflixSpacing.xxs))
                                Text(
                                    text = "主演: ${detail.actors}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DaiflixColors.LightGray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(DaiflixSpacing.sm))

                            // 播放按钮
                            val history = uiState.watchHistory
                            val canResume = history != null &&
                                history.sourceIndex == uiState.selectedSourceIndex &&
                                history.episodeIndex == uiState.selectedEpisodeIndex &&
                                history.position > 0
                            val playLabel = if (canResume) {
                                val minutes = (history!!.position / 60_000).coerceAtLeast(0)
                                "▶  继续播放  ${minutes}分"
                            } else {
                                "▶  播放"
                            }
                            Surface(
                                onClick = {
                                    if (detail.playSources.isNotEmpty()) {
                                        onPlayClick(
                                            vodId,
                                            uiState.selectedSourceIndex,
                                            uiState.selectedEpisodeIndex
                                        )
                                    }
                                },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = DaiflixColors.Red,
                                    focusedContainerColor = DaiflixColors.DarkRed
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = DaiflixSpacing.xl,
                                        vertical = DaiflixSpacing.sm
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs)
                                ) {
                                    Text(
                                        text = playLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DaiflixColors.White
                                    )
                                }
                            }

                            // 收藏按钮
                            Spacer(modifier = Modifier.height(DaiflixSpacing.xs))
                            Surface(
                                onClick = { viewModel.toggleFavorite() },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (uiState.isFavorite) DaiflixColors.Gold else DaiflixColors.DarkCard,
                                    focusedContainerColor = DaiflixColors.DarkSurface
                                )
                            ) {
                                Text(
                                    text = if (uiState.isFavorite) "★ 已收藏" else "☆ 收藏",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DaiflixColors.White,
                                    modifier = Modifier.padding(
                                        horizontal = DaiflixSpacing.md,
                                        vertical = DaiflixSpacing.xs
                                    )
                                )
                            }
                        }
                    }

                    // 简介
                    if (detail.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(DaiflixSpacing.lg))
                        Text(
                            text = "剧情简介",
                            style = MaterialTheme.typography.titleLarge,
                            color = DaiflixColors.White
                        )
                        Spacer(modifier = Modifier.height(DaiflixSpacing.xs))
                        Text(
                            text = detail.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = DaiflixColors.LightGray,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                        )
                    }

                    // 选集
                    if (detail.playSources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(DaiflixSpacing.xl))
                        Text(
                            text = "选集播放",
                            style = MaterialTheme.typography.titleLarge,
                            color = DaiflixColors.White
                        )
                        Spacer(modifier = Modifier.height(DaiflixSpacing.sm))
                        EpisodeSelector(
                            sources = detail.playSources,
                            selectedSourceIndex = uiState.selectedSourceIndex,
                            selectedEpisodeIndex = uiState.selectedEpisodeIndex,
                            onSourceChange = { viewModel.selectSource(it) },
                            onEpisodeClick = { epIdx ->
                                viewModel.selectEpisode(epIdx)
                                onPlayClick(vodId, uiState.selectedSourceIndex, epIdx)
                            }
                        )
                    }

                    // 相关推荐
                    if (detail.relatedItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(DaiflixSpacing.xl))
                        VodRow(
                            title = "相关推荐",
                            items = detail.relatedItems,
                            onVodClick = onVodClick
                        )
                    }

                    Spacer(modifier = Modifier.height(DaiflixSpacing.xxxl))
                }
            }
        }
    }
}
