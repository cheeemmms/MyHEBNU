package com.myhebnu.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.myhebnu.ui.theme.ColorTemplate
import com.myhebnu.ui.theme.contrastRatio
import com.myhebnu.ui.theme.meetsWcagAA

@Composable
fun ColorTemplatePicker(
    currentTemplate: ColorTemplate,
    onTemplateSelected: (ColorTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "色彩模板",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Template grid
        ColorTemplate.templates().forEach { template ->
            TemplateRow(
                template = template,
                isSelected = template == currentTemplate,
                onClick = { onTemplateSelected(template) }
            )
        }

        // Preview swatches
        Spacer(Modifier.height(8.dp))
        Text(
            text = "预览",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            currentTemplate.hues.take(8).forEach { hue ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.hsl(hue, 0.35f, 0.70f))
                )
            }
        }

        // HSL custom color picker
        Spacer(Modifier.height(16.dp))
        Text(
            text = "自定义颜色",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        HSLColorPicker()
    }
}

@Composable
private fun TemplateRow(
    template: ColorTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Color dots
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            template.hues.take(5).forEach { hue ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.hsl(hue, 0.35f, 0.70f))
                )
            }
        }
        Text(
            text = template.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun HSLColorPicker() {
    var hue by remember { mutableStateOf(210f) }
    var saturation by remember { mutableStateOf(0.35f) }
    var lightness by remember { mutableStateOf(0.70f) }
    var hexInput by remember { mutableStateOf("#3A7EC8") }

    val previewColor = Color.hsl(hue, saturation, lightness)
    val textColor = if (meetsWcagAA(previewColor, Color.White)) Color.White else Color.Black
    val contrast = remember(hue, saturation, lightness) {
        "%.1f".format(maxOf(contrastRatio(previewColor, Color.White), contrastRatio(previewColor, Color.Black)))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Preview box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(previewColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "预览文字 · 对比度 $contrast:1",
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Hue slider
        Text("色相 (H)", style = MaterialTheme.typography.labelSmall)
        Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f,
            colors = SliderDefaults.colors(thumbColor = Color.hsl(hue, 1f, 0.5f)))

        // Saturation slider
        Text("饱和度 (S)", style = MaterialTheme.typography.labelSmall)
        Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f)

        // Lightness slider
        Text("亮度 (L)", style = MaterialTheme.typography.labelSmall)
        Slider(value = lightness, onValueChange = { lightness = it }, valueRange = 0.1f..0.95f)

        // HEX input
        OutlinedTextField(
            value = hexInput,
            onValueChange = { hexInput = it },
            label = { Text("色号 (HEX)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true
        )

        // WCAG compliance
        val meetsAA = contrastRatio(previewColor, textColor) >= 4.5
        val wcagLabel = if (meetsAA) "✓ WCAG AA 达标" else "✗ WCAG AA 未达标"
        val wcagColor = if (meetsAA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        Text(wcagLabel, style = MaterialTheme.typography.labelMedium, color = wcagColor)
    }
}
