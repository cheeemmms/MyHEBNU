package com.myhebnu.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myhebnu.ui.theme.ColorPreset
import com.myhebnu.ui.theme.builtInPresets
import com.myhebnu.ui.theme.contrastRatio
import com.myhebnu.ui.theme.meetsWcagAA

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorThemeScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自由主题色彩") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Description
            Text(
                text = "自定义主题色将影响应用全局色彩，包括按钮、顶栏、导航栏等组件，" +
                       "课表卡片颜色也会随之变化。关闭后将恢复系统动态色（Material You）。",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Enable switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "启用自定义色彩",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = uiState.useCustomColors,
                    onCheckedChange = viewModel::setUseCustomColors
                )
            }

            // Built-in presets
            Text(
                "预设模板",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            builtInPresets().chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { preset ->
                        PresetChip(
                            preset = preset,
                            isSelected = uiState.activePresetId == preset.id,
                            onClick = { viewModel.selectPreset(preset) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill empty slots
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            HorizontalDivider()

            // Custom presets
            Text(
                "我的预设",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Existing custom presets
            uiState.customPresets.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { preset ->
                        PresetChip(
                            preset = preset,
                            isSelected = uiState.activePresetId == preset.id,
                            onClick = { viewModel.selectPreset(preset) },
                            onLongClick = { viewModel.deleteCustomPreset(preset) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Create new preset
            var showCreator by remember { mutableStateOf(false) }
            if (!showCreator) {
                OutlinedButton(
                    onClick = { showCreator = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("新建预设")
                }
            }

            // Creator panel
            AnimatedVisibility(
                visible = showCreator,
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically()
            ) {
                PresetCreator(
                    onSave = { name, hue ->
                        viewModel.createCustomPreset(name, hue)
                        showCreator = false
                    },
                    onCancel = { showCreator = false }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ============================================================
// Preset chip (clickable color dot row)
// ============================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetChip(
    preset: ColorPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val combined = if (onLongClick != null) {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier.combinedClickable(onClick = onClick)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            )
            .then(combined)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Color preview dot
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.hsl(preset.seedHue, 0.40f, 0.70f))
        )
        Text(
            text = preset.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// ============================================================
// Preset creator (HSL controls + name input)
// ============================================================

@Composable
private fun PresetCreator(
    onSave: (name: String, hue: Float) -> Unit,
    onCancel: () -> Unit
) {
    var hue by remember { mutableStateOf(210f) }
    var saturation by remember { mutableStateOf(0.40f) }
    var lightness by remember { mutableStateOf(0.70f) }
    var name by remember { mutableStateOf("") }

    val previewColor = Color.hsl(hue, saturation, lightness)
    val textColor = if (meetsWcagAA(previewColor, Color.White)) Color.White else Color.Black
    val contrastVal = remember(hue, saturation, lightness) {
        "%.1f".format(maxOf(contrastRatio(previewColor, Color.White), contrastRatio(previewColor, Color.Black)))
    }
    val meetsAA = remember(hue, saturation, lightness) {
        contrastRatio(previewColor, textColor) >= 4.5
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新建预设", style = MaterialTheme.typography.titleSmall)

            // Color preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(previewColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "预览 · 对比度 $contrastVal:1",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Hue slider — 全色谱彩虹渐变轨道
            Text("色相 (H)", style = MaterialTheme.typography.labelSmall)
            HueSliderBar(
                hue = hue,
                onHueChange = { hue = it }
            )

            // Saturation
            Text("饱和度 (S)", style = MaterialTheme.typography.labelSmall)
            Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f)

            // Lightness
            Text("亮度 (L)", style = MaterialTheme.typography.labelSmall)
            Slider(value = lightness, onValueChange = { lightness = it }, valueRange = 0.1f..0.95f)

            // WCAG badge
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val (badgeColor, badgeText) = if (meetsAA) {
                    Pair(MaterialTheme.colorScheme.primary, "✓ WCAG AA 达标")
                } else {
                    Pair(MaterialTheme.colorScheme.error, "✗ WCAG AA 未达标")
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor
                    )
                }
            }

            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("预设名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(name.ifBlank { "我的主题" }, hue) },
                    enabled = true,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("保存预设")
                }
            }
        }
    }
}

// ============================================================
// Rainbow gradient hue slider bar
// ============================================================

@Composable
private fun HueSliderBar(
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    val density = LocalDensity.current
    var sliderWidthPx by remember { mutableFloatStateOf(0f) }

    val rainbowBrush = Brush.horizontalGradient(
        (0..12).map { i -> Color.hsl(i * 30f, 1f, 0.5f) }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(rainbowBrush)
            .onSizeChanged { sliderWidthPx = it.width.toFloat() }
            .pointerInput(sliderWidthPx) {
                detectTapGestures { offset ->
                    if (sliderWidthPx > 0f) {
                        onHueChange((offset.x / sliderWidthPx * 360f).coerceIn(0f, 360f))
                    }
                }
            }
            .pointerInput(sliderWidthPx) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (sliderWidthPx > 0f) {
                        onHueChange(
                            (hue + dragAmount / sliderWidthPx * 360f)
                                .coerceIn(0f, 360f)
                        )
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Thumb indicator
        val thumbOffsetDp = with(density) { (hue / 360f * sliderWidthPx).toDp() - 12.dp }
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetDp)
                .size(24.dp)
                .border(2.5.dp, Color.White, CircleShape)
                .background(Color.hsl(hue, 1f, 0.5f), CircleShape)
        )
    }
}
