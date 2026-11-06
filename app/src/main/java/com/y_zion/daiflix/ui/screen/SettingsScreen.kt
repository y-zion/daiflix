package com.y_zion.daiflix.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import com.y_zion.daiflix.ui.theme.DaiflixColors
import com.y_zion.daiflix.ui.theme.DaiflixSpacing
import com.y_zion.daiflix.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                verticalAlignment = Alignment.CenterVertically
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
                Spacer(modifier = Modifier.width(DaiflixSpacing.md))
                Text(
                    text = "\u8bbe\u7f6e",
                    style = androidx.tv.material3.MaterialTheme.typography.headlineMedium,
                    color = DaiflixColors.White
                )
            }

            Spacer(modifier = Modifier.height(DaiflixSpacing.xl))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.md)
            ) {
                SettingsSection(title = "\u6570\u636e\u6e90") {
                    var draftUrl by remember(uiState.baseUrl) { mutableStateOf(uiState.baseUrl) }
                    SettingsTextField(
                        label = "VidHub \u5730\u5740",
                        value = draftUrl,
                        onValueChange = { draftUrl = it }
                    )
                    if (uiState.baseUrlError != null) {
                        Text(
                            text = uiState.baseUrlError ?: "",
                            color = DaiflixColors.Red,
                            style = androidx.tv.material3.MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = DaiflixSpacing.xxs)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm),
                        modifier = Modifier.padding(top = DaiflixSpacing.xs)
                    ) {
                        Surface(
                            onClick = { viewModel.saveBaseUrl(draftUrl) },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = DaiflixColors.Red,
                                focusedContainerColor = DaiflixColors.DarkRed
                            )
                        ) {
                            Text(
                                text = "\u4fdd\u5b58\u5730\u5740",
                                color = DaiflixColors.White,
                                modifier = Modifier.padding(
                                    horizontal = DaiflixSpacing.md,
                                    vertical = DaiflixSpacing.xs
                                )
                            )
                        }
                        Surface(
                            onClick = {
                                viewModel.resetBaseUrl()
                                draftUrl = viewModel.uiState.value.baseUrl
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = DaiflixColors.DarkCard
                            )
                        ) {
                            Text(
                                text = "\u6062\u590d\u9ed8\u8ba4",
                                color = DaiflixColors.White,
                                modifier = Modifier.padding(
                                    horizontal = DaiflixSpacing.md,
                                    vertical = DaiflixSpacing.xs
                                )
                            )
                        }
                    }
                    Text(
                        text = "\u63d0\u793a\uff1a\u90e8\u5206\u7535\u89c6\u53ef\u7528\u7cfb\u7edf\u8f93\u5165\u6cd5\u7f16\u8f91\u5730\u5740",
                        color = DaiflixColors.MediumGray,
                        style = androidx.tv.material3.MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = DaiflixSpacing.xxs)
                    )
                }

                SettingsSection(title = "\u5f39\u5e55\u8bbe\u7f6e") {
                    SettingsToggle(
                        label = "\u542f\u7528\u5f39\u5e55",
                        checked = uiState.danmakuEnabled,
                        onCheckedChange = { viewModel.setDanmakuEnabled(it) }
                    )
                    SettingsSlider(
                        label = "\u5f39\u5e55\u900f\u660e\u5ea6",
                        value = uiState.danmakuOpacity,
                        onValueChange = { viewModel.setDanmakuOpacity(it) },
                        valueRange = 0.2f..1.0f
                    )
                    SettingsSlider(
                        label = "\u5f39\u5e55\u5927\u5c0f",
                        value = uiState.danmakuSize,
                        onValueChange = { viewModel.setDanmakuSize(it) },
                        valueRange = 0.5f..2.0f
                    )
                }

                SettingsSection(title = "\u64ad\u653e\u8bbe\u7f6e") {
                    SettingsToggle(
                        label = "\u81ea\u52a8\u64ad\u653e\u4e0b\u4e00\u96c6",
                        checked = uiState.autoPlayNext,
                        onCheckedChange = { viewModel.setAutoPlayNext(it) }
                    )
                    SettingsSlider(
                        label = "\u9ed8\u8ba4\u64ad\u653e\u901f\u5ea6",
                        value = uiState.playbackSpeed,
                        onValueChange = { viewModel.setPlaybackSpeed(it) },
                        valueRange = 0.5f..2.0f,
                        displayValue = "${uiState.playbackSpeed}x"
                    )
                }

                SettingsSection(title = "\u5173\u4e8e") {
                    SettingsInfo(label = "\u5e94\u7528\u540d\u79f0", value = "Daiflix")
                    SettingsInfo(label = "\u7248\u672c", value = "1.0.0")
                    SettingsInfo(label = "\u6570\u636e\u6e90", value = "VidHub \u4ee3\u7406\u5c42")
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DaiflixColors.DarkSurface, RoundedCornerShape(12.dp))
            .padding(DaiflixSpacing.md)
    ) {
        Text(
            text = title,
            style = androidx.tv.material3.MaterialTheme.typography.titleMedium,
            color = DaiflixColors.Red,
            modifier = Modifier.padding(bottom = DaiflixSpacing.sm)
        )
        content()
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DaiflixSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = DaiflixColors.White,
            style = androidx.tv.material3.MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DaiflixColors.Red,
                checkedTrackColor = DaiflixColors.DarkRed,
                uncheckedThumbColor = DaiflixColors.MediumGray,
                uncheckedTrackColor = DaiflixColors.DarkCard
            )
        )
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String = "%.1f".format(value)
) {
    Column(modifier = Modifier.padding(vertical = DaiflixSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = DaiflixColors.White,
                style = androidx.tv.material3.MaterialTheme.typography.bodyLarge
            )
            Text(
                text = displayValue,
                color = DaiflixColors.LightGray,
                style = androidx.tv.material3.MaterialTheme.typography.bodyMedium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = DaiflixColors.Red,
                activeTrackColor = DaiflixColors.Red,
                inactiveTrackColor = DaiflixColors.DarkCard
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = DaiflixSpacing.xs)) {
        Text(
            text = label,
            color = DaiflixColors.LightGray,
            style = androidx.tv.material3.MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = DaiflixSpacing.xxs)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DaiflixColors.White,
                unfocusedBorderColor = DaiflixColors.DarkGray,
                cursorColor = DaiflixColors.White,
                focusedTextColor = DaiflixColors.White,
                unfocusedTextColor = DaiflixColors.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingsInfo(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DaiflixSpacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = DaiflixColors.LightGray,
            style = androidx.tv.material3.MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = DaiflixColors.White,
            style = androidx.tv.material3.MaterialTheme.typography.bodyMedium
        )
    }
}
