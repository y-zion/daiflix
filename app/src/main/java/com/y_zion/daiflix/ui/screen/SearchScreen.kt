package com.y_zion.daiflix.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import com.y_zion.daiflix.ui.component.VodCard
import com.y_zion.daiflix.ui.theme.DaiflixColors
import com.y_zion.daiflix.ui.theme.DaiflixSpacing
import com.y_zion.daiflix.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    onVodClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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

                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    placeholder = {
                        Text(
                            "\u641c\u7d22\u7535\u5f71\u3001\u7535\u89c6\u5267\u3001\u52a8\u6f2b...",
                            color = DaiflixColors.MediumGray
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DaiflixColors.White,
                        unfocusedBorderColor = DaiflixColors.DarkGray,
                        cursorColor = DaiflixColors.White,
                        focusedTextColor = DaiflixColors.White,
                        unfocusedTextColor = DaiflixColors.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
            }

            Spacer(modifier = Modifier.height(DaiflixSpacing.lg))

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
