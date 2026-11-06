package com.y_zion.daiflix.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import com.y_zion.daiflix.ui.component.VodCard
import com.y_zion.daiflix.ui.theme.DaiflixColors
import com.y_zion.daiflix.ui.theme.DaiflixSpacing
import com.y_zion.daiflix.ui.viewmodel.SearchViewModel

/**
 * TV 搜索页：遥控器没有实体键盘，使用屏幕软键盘输入。
 * 站点 /ajax/suggest 同时支持中文与拼音（如 liulang），因此软键盘只提供字母数字。
 */
@Composable
fun SearchScreen(
    onVodClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val firstKeyFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { firstKeyFocus.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DaiflixSpacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.md)
            ) {
                Surface(
                    onClick = onBack,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("\u2190", color = DaiflixColors.White, style = MaterialTheme.typography.titleLarge)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    colors = SurfaceDefaults.colors(containerColor = DaiflixColors.DarkCard),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = DaiflixSpacing.md),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = uiState.query.ifBlank { "输入片名或拼音，例如 liulang" },
                            color = if (uiState.query.isBlank()) DaiflixColors.MediumGray else DaiflixColors.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = { viewModel.search() },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = DaiflixColors.Red,
                        focusedContainerColor = DaiflixColors.DarkRed
                    )
                ) {
                    Text(
                        text = "搜索",
                        color = DaiflixColors.White,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = DaiflixSpacing.lg, vertical = DaiflixSpacing.xs)
                    )
                }
            }

            Spacer(modifier = Modifier.height(DaiflixSpacing.sm))

            OnScreenKeyboard(
                firstKeyFocus = firstKeyFocus,
                onKey = { viewModel.updateQuery(uiState.query + it) },
                onBackspace = {
                    viewModel.updateQuery(uiState.query.dropLast(1))
                },
                onClear = { viewModel.updateQuery("") },
                onSearch = { viewModel.search() }
            )

            Spacer(modifier = Modifier.height(DaiflixSpacing.md))

            when {
                uiState.isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\u641c\u7d22\u4e2d...", color = DaiflixColors.LightGray)
                    }
                }

                uiState.query.isBlank() && uiState.searchHistory.isNotEmpty() -> {
                    Text(
                        "\u641c\u7d22\u5386\u53f2",
                        style = MaterialTheme.typography.titleMedium,
                        color = DaiflixColors.White
                    )
                    Spacer(modifier = Modifier.height(DaiflixSpacing.sm))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs)
                    ) {
                        items(uiState.searchHistory) { history ->
                            Surface(
                                onClick = {
                                    viewModel.updateQuery(history.keyword)
                                    viewModel.search(history.keyword)
                                },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = DaiflixColors.DarkCard,
                                    focusedContainerColor = DaiflixColors.DarkSurface
                                )
                            ) {
                                Text(
                                    text = history.keyword,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DaiflixColors.LightGray,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 4.dp
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(DaiflixSpacing.md))
                    Surface(
                        onClick = { viewModel.clearHistory() },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = DaiflixColors.DarkCard
                        )
                    ) {
                        Text(
                            text = "\u6e05\u9664\u5386\u53f2",
                            style = MaterialTheme.typography.labelMedium,
                            color = DaiflixColors.MediumGray,
                            modifier = Modifier.padding(
                                horizontal = DaiflixSpacing.sm,
                                vertical = DaiflixSpacing.xxs
                            )
                        )
                    }
                }

                uiState.results.isEmpty() && uiState.query.isNotBlank() && !uiState.isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "\u672a\u627e\u5230\u76f8\u5173\u5185\u5bb9",
                            color = DaiflixColors.LightGray
                        )
                    }
                }

                uiState.results.isNotEmpty() -> {
                    Text(
                        text = "\u641c\u7d22\u7ed3\u679c (${uiState.results.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = DaiflixColors.White
                    )
                    Spacer(modifier = Modifier.height(DaiflixSpacing.sm))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.md)
                    ) {
                        items(uiState.results, key = { it.id }) { vod ->
                            VodCard(item = vod, onClick = { onVodClick(vod.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnScreenKeyboard(
    firstKeyFocus: FocusRequester,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit
) {
    val rows = listOf(
        "1234567890",
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.xxs),
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xxs),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEachIndexed { index, ch ->
                    val keyModifier = if (rowIndex == 0 && index == 0) {
                        Modifier.width(52.dp).focusRequester(firstKeyFocus)
                    } else {
                        Modifier.width(52.dp)
                    }
                    KeyboardKey(
                        label = ch.toString(),
                        onClick = { onKey(ch.toString()) },
                        modifier = keyModifier
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs),
            modifier = Modifier.fillMaxWidth()
        ) {
            KeyboardKey(label = "空格", onClick = { onKey(" ") }, modifier = Modifier.weight(1f))
            KeyboardKey(label = "\u232b 删除", onClick = onBackspace, modifier = Modifier.weight(1f))
            KeyboardKey(label = "清空", onClick = onClear, modifier = Modifier.weight(1f))
            KeyboardKey(label = "搜索", onClick = onSearch, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun KeyboardKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DaiflixColors.DarkCard,
            focusedContainerColor = DaiflixColors.DarkSurface
        ),
        modifier = modifier.height(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = DaiflixColors.White
            )
        }
    }
}
