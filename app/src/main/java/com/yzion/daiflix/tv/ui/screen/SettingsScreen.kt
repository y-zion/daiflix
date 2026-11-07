package com.yzion.daiflix.tv.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import com.yzion.daiflix.tv.data.local.AppPreferences
import com.yzion.daiflix.tv.data.repository.VodRepository
import com.yzion.daiflix.tv.ui.component.DaiflixBackButton
import com.yzion.daiflix.tv.ui.component.DaiflixDangerButton
import com.yzion.daiflix.tv.ui.component.DaiflixPrimaryButton
import com.yzion.daiflix.tv.ui.component.DaiflixSecondaryButton
import com.yzion.daiflix.tv.ui.component.RequestFocusWhenReady
import com.yzion.daiflix.tv.ui.component.focusBorder
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import com.yzion.daiflix.tv.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Repository-backed actions that the settings screen needs but SettingsViewModel
 * (preferences only) does not own.
 */
@HiltViewModel
class SettingsActionsViewModel @Inject constructor(
    private val repository: VodRepository
) : ViewModel() {
    fun clearWatchHistory() {
        viewModelScope.launch { repository.clearWatchHistory() }
    }
}

private val PLAYBACK_SPEED_OPTIONS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private val DANMAKU_OPACITY_OPTIONS = listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f)
private val DANMAKU_SIZE_OPTIONS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private val DANMAKU_SPEED_OPTIONS = listOf(1, 2, 3, 4, 5)

/**
 * TV-friendly settings: grouped focusable rows instead of phone text fields,
 * sliders and switches.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    actionsViewModel: SettingsActionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showClearConfirm by remember { mutableStateOf(false) }
    var editingUrl by remember { mutableStateOf(false) }
    var draftUrl by remember { mutableStateOf("") }

    val urlFieldFocusRequester = remember { FocusRequester() }
    val firstRowFocusRequester = remember { FocusRequester() }

    // 首帧之后一帧再请求焦点：等第一行 SettingsToggleRow 的聚焦订阅先建立，
    // 焦点样式（白底黑字 / 白焦点环）第一帧即可见（审计 P1-3）。
    RequestFocusWhenReady(firstRowFocusRequester)

    // 进入「数据源地址」编辑态时把光标落到输入框；退出编辑态时 enabled=false，
    // 不会在返回列表时误抢焦点。
    RequestFocusWhenReady(
        focusRequester = urlFieldFocusRequester,
        key = editingUrl,
        enabled = editingUrl
    )

    BackHandler {
        if (showClearConfirm) {
            showClearConfirm = false
        } else if (editingUrl) {
            editingUrl = false
        } else {
            onBack()
        }
    }

    if (showClearConfirm) {
        ClearHistoryConfirm(
            onCancel = { showClearConfirm = false },
            onConfirm = {
                actionsViewModel.clearWatchHistory()
                showClearConfirm = false
            }
        )
        return
    }

    val packageInfo = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    }
    // 品牌字标固定大写 DAIFLIX，不跟随包名/系统语言的显示名（debug 变体也保持一致，
    // 避免「关于」页出现 Daiflix / DAIFLIX (Debug) 等大小写混用）。
    val appName = "DAIFLIX"
    val versionName = packageInfo?.versionName ?: "未知"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = DaiflixSpacing.screenPadding,
                        end = DaiflixSpacing.screenPadding,
                        top = DaiflixSpacing.lg,
                        bottom = DaiflixSpacing.md
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回圆钮统一走共享组件，与收藏页/历史页同款（审计 P3-4）。
                // 不传 focusRequester：首屏焦点仍由 RequestFocusWhenReady(firstRowFocusRequester) 决定。
                DaiflixBackButton(onBack = onBack)

                Spacer(modifier = Modifier.width(DaiflixSpacing.lg))

                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineLarge,
                    color = DaiflixColors.White
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "方向键浏览 · 确定键切换",
                    style = MaterialTheme.typography.labelMedium,
                    color = DaiflixColors.MediumGray
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DaiflixSpacing.screenPadding)
            ) {
                SettingsSection(title = "播放设置") {
                    SettingsToggleRow(
                        label = "自动播放下一集",
                        checked = uiState.autoPlayNext,
                        onCheckedChange = { viewModel.setAutoPlayNext(it) },
                        modifier = Modifier.focusRequester(firstRowFocusRequester)
                    )
                    SettingsRow(
                        label = "默认播放速度",
                        value = speedLabel(uiState.playbackSpeed),
                        onClick = {
                            viewModel.setPlaybackSpeed(
                                nextInCycle(uiState.playbackSpeed, PLAYBACK_SPEED_OPTIONS)
                            )
                        }
                    )
                    SettingsToggleRow(
                        label = "启用弹幕",
                        checked = uiState.danmakuEnabled,
                        onCheckedChange = { viewModel.setDanmakuEnabled(it) }
                    )
                    // 说明文字：弹幕功能是接好线的，部分片源本身没有弹幕数据，
                    // 避免用户误以为开关失效。
                    Text(
                        text = "部分片源没有弹幕数据，播放页会显示弹幕数量",
                        style = MaterialTheme.typography.bodySmall,
                        color = DaiflixColors.MediumGray,
                        modifier = Modifier.padding(
                            start = DaiflixSpacing.md,
                            end = DaiflixSpacing.md,
                            bottom = DaiflixSpacing.xxs
                        )
                    )
                    SettingsRow(
                        label = "弹幕透明度",
                        value = "${(uiState.danmakuOpacity * 100).toInt()}%",
                        onClick = {
                            viewModel.setDanmakuOpacity(
                                nextInCycle(uiState.danmakuOpacity, DANMAKU_OPACITY_OPTIONS)
                            )
                        }
                    )
                    SettingsRow(
                        label = "弹幕大小",
                        value = "${uiState.danmakuSize}x",
                        onClick = {
                            viewModel.setDanmakuSize(
                                nextInCycle(uiState.danmakuSize, DANMAKU_SIZE_OPTIONS)
                            )
                        }
                    )
                    SettingsRow(
                        label = "弹幕速度",
                        value = "${uiState.danmakuSpeed}x",
                        onClick = {
                            viewModel.setDanmakuSpeed(
                                nextIntInCycle(uiState.danmakuSpeed, DANMAKU_SPEED_OPTIONS)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(DaiflixSpacing.md))

                SettingsSection(title = "数据源") {
                    SettingsRow(
                        label = "数据源地址",
                        value = uiState.baseUrl,
                        onClick = {
                            draftUrl = uiState.baseUrl
                            editingUrl = true
                        }
                    )

                    if (editingUrl) {
                        Column(modifier = Modifier.padding(top = DaiflixSpacing.sm)) {
                            Text(
                                text = "输入 VidHub 代理地址（http:// 或 https://）",
                                style = MaterialTheme.typography.bodySmall,
                                color = DaiflixColors.MediumGray,
                                modifier = Modifier.padding(bottom = DaiflixSpacing.xs)
                            )
                            OutlinedTextField(
                                value = draftUrl,
                                onValueChange = { draftUrl = it },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(urlFieldFocusRequester),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DaiflixColors.White,
                                    unfocusedBorderColor = DaiflixColors.DarkGray,
                                    cursorColor = DaiflixColors.White,
                                    focusedTextColor = DaiflixColors.White,
                                    unfocusedTextColor = DaiflixColors.White
                                )
                            )
                            if (uiState.baseUrlError != null) {
                                Text(
                                    text = uiState.baseUrlError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DaiflixColors.Red,
                                    modifier = Modifier.padding(top = DaiflixSpacing.xxs)
                                )
                            }
                            Row(
                                modifier = Modifier.padding(top = DaiflixSpacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm)
                            ) {
                                DaiflixPrimaryButton(
                                    text = "保存",
                                    onClick = {
                                        viewModel.saveBaseUrl(draftUrl)
                                        if (viewModel.uiState.value.baseUrlError == null) {
                                            editingUrl = false
                                        }
                                    }
                                )
                                DaiflixSecondaryButton(
                                    text = "取消",
                                    onClick = { editingUrl = false }
                                )
                                DaiflixSecondaryButton(
                                    text = "恢复默认",
                                    onClick = {
                                        viewModel.resetBaseUrl()
                                        draftUrl = AppPreferences.DEFAULT_BASE_URL
                                        editingUrl = false
                                    }
                                )
                            }
                        }
                    } else if (uiState.baseUrlError != null) {
                        Text(
                            text = uiState.baseUrlError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = DaiflixColors.Red,
                            modifier = Modifier.padding(top = DaiflixSpacing.xxs)
                        )
                    }

                    SettingsRow(
                        label = "清除观看记录",
                        danger = true,
                        onClick = { showClearConfirm = true }
                    )
                }

                Spacer(modifier = Modifier.height(DaiflixSpacing.md))

                SettingsSection(title = "关于") {
                    SettingsInfoRow(label = "应用名称", value = appName)
                    SettingsInfoRow(label = "版本", value = versionName)
                    SettingsInfoRow(label = "数据来源", value = "VidHub 代理层")
                    SettingsInfoRow(label = "项目主页", value = "github.com/y-zion/daiflix")
                }

                Spacer(modifier = Modifier.height(DaiflixSpacing.xxxl))
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
            .padding(DaiflixSpacing.md),
        verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            // 分区标题用中性色：品牌红只留给危险操作（清除观看记录），避免语义冲突。
            color = DaiflixColors.LightGray,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = DaiflixSpacing.xxs)
        )
        content()
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String? = null,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val contentColor = if (focused) DaiflixColors.Black else DaiflixColors.White
    val labelColor = when {
        focused -> DaiflixColors.Black
        danger -> DaiflixColors.Red
        else -> DaiflixColors.White
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DaiflixColors.DarkCard,
            contentColor = DaiflixColors.White,
            focusedContainerColor = DaiflixColors.White,
            focusedContentColor = DaiflixColors.Black
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
        border = ClickableSurfaceDefaults.border(focusedBorder = focusBorder())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = DaiflixSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = labelColor
            )
            Spacer(modifier = Modifier.weight(1f))
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (focused) DaiflixColors.Black else DaiflixColors.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = DaiflixSpacing.xs)
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = contentColor
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DaiflixColors.DarkCard,
            contentColor = DaiflixColors.White,
            focusedContainerColor = DaiflixColors.White,
            focusedContentColor = DaiflixColors.Black
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
        border = ClickableSurfaceDefaults.border(focusedBorder = focusBorder())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = DaiflixSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (focused) DaiflixColors.Black else DaiflixColors.White
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors()
            )
        }
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DaiflixSpacing.xs)
            .padding(horizontal = DaiflixSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = DaiflixColors.LightGray
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = DaiflixColors.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(DaiflixSpacing.xs))
            trailing()
        }
    }
}

@Composable
private fun ClearHistoryConfirm(
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val cancelFocusRequester = remember { FocusRequester() }
    // 默认焦点落在「取消」上（破坏性操作不预聚焦确认键）；延后一帧请求，
    // 确保按钮的聚焦样式第一帧就可见。
    RequestFocusWhenReady(cancelFocusRequester)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .background(DaiflixColors.DarkSurface, RoundedCornerShape(16.dp))
                .padding(DaiflixSpacing.xl)
        ) {
            Text(
                text = "清除观看记录？",
                style = MaterialTheme.typography.headlineSmall,
                color = DaiflixColors.White
            )
            Spacer(modifier = Modifier.height(DaiflixSpacing.sm))
            Text(
                text = "将清空全部观看进度与续播记录，此操作无法撤销。",
                style = MaterialTheme.typography.bodyLarge,
                color = DaiflixColors.LightGray
            )
            Spacer(modifier = Modifier.height(DaiflixSpacing.xl))
            Row(horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.md)) {
                DaiflixSecondaryButton(
                    text = "取消",
                    onClick = onCancel,
                    modifier = Modifier.focusRequester(cancelFocusRequester)
                )
                DaiflixDangerButton(
                    text = "清除",
                    onClick = onConfirm
                )
            }
        }
    }
}

private fun nextInCycle(current: Float, options: List<Float>): Float {
    val index = options.indexOfFirst { kotlin.math.abs(it - current) < 0.001f }
    return if (index < 0) options.first() else options[(index + 1) % options.size]
}

private fun nextIntInCycle(current: Int, options: List<Int>): Int {
    val index = options.indexOf(current)
    return if (index < 0) options.first() else options[(index + 1) % options.size]
}

private fun speedLabel(speed: Float): String {
    return "${speed}x"
}
