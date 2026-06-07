package com.myhebnu.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

// Primary - School Blue (河北师范大学)
val Blue10 = Color(0xFF001A40)
val Blue20 = Color(0xFF002D6E)
val Blue30 = Color(0xFF00419E)
val Blue40 = Color(0xFF1B5FCE)
val Blue80 = Color(0xFFB3CCFF)
val Blue90 = Color(0xFFDAE5FF)

// Secondary
val Teal10 = Color(0xFF002B2B)
val Teal20 = Color(0xFF004848)
val Teal30 = Color(0xFF006767)
val Teal40 = Color(0xFF008787)
val Teal80 = Color(0xFF4DD9D9)
val Teal90 = Color(0xFF6EFFFE)

// Tertiary
val Amber10 = Color(0xFF2B1700)
val Amber20 = Color(0xFF482900)
val Amber30 = Color(0xFF683D00)
val Amber40 = Color(0xFF8A5200)
val Amber80 = Color(0xFFFFB95B)
val Amber90 = Color(0xFFFFDEB8)

// Neutral
val Neutral10 = Color(0xFF1B1B1F)
val Neutral90 = Color(0xFFE4E2E6)
val Neutral95 = Color(0xFFF3F0F4)
val Neutral99 = Color(0xFFFDFBFF)

// Error
val Red10 = Color(0xFF410002)
val Red20 = Color(0xFF690005)
val Red30 = Color(0xFF93000A)
val Red40 = Color(0xFFBA1A1A)
val Red80 = Color(0xFFFFB4AB)
val Red90 = Color(0xFFFFDAD6)

// Course card colors — MD3 Tonal Palette per course (assigned by hue)
data class CourseTonalPalette(
    val container: Color,    // Tonal 90 (light) / 30 (dark)
    val onContainer: Color,  // Tonal 10 (light) / 90 (dark)
    val variant: Color,      // Tonal 30 (light) / 80 (dark)
    val outline: Color       // Tonal 50
)

fun coursePaletteForHue(hue: Float, isDark: Boolean): CourseTonalPalette {
    val sat = 0.35f
    return if (isDark) {
        CourseTonalPalette(
            container = Color.hsl(hue, sat, 0.30f),
            onContainer = Color.hsl(hue, sat, 0.90f),
            variant = Color.hsl(hue, 0.25f, 0.80f),
            outline = Color.hsl(hue, 0.20f, 0.60f)
        )
    } else {
        CourseTonalPalette(
            container = Color.hsl(hue, sat, 0.90f),
            onContainer = Color.hsl(hue, 0.55f, 0.10f),
            variant = Color.hsl(hue, 0.25f, 0.30f),
            outline = Color.hsl(hue, 0.20f, 0.50f)
        )
    }
}

/** Assign hues evenly spaced across 360°, min 30° apart. */
fun assignCourseHues(courseNames: List<String>): Map<String, Float> {
    val result = mutableMapOf<String, Float>()
    val step = 360f / maxOf(courseNames.size, 1)
    courseNames.sorted().forEachIndexed { i, name ->
        result[name] = (i * step) % 360f
    }
    return result
}

/** WCAG relative luminance. */
fun Color.wcagLuminance(): Double {
    fun srgb(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * srgb(red) + 0.7152 * srgb(green) + 0.0722 * srgb(blue)
}

/** WCAG contrast ratio between two colors. */
fun contrastRatio(a: Color, b: Color): Double {
    val l1 = a.wcagLuminance() + 0.05
    val l2 = b.wcagLuminance() + 0.05
    return if (l1 > l2) l1 / l2 else l2 / l1
}

/** True if contrast meets WCAG AA (4.5:1 for normal text). */
fun meetsWcagAA(bg: Color, fg: Color): Boolean = contrastRatio(bg, fg) >= 4.5

// Preset color templates
enum class ColorTemplate(val label: String, val hues: List<Float>) {
    RAINBOW("彩虹色系", (0..330 step 30).map { it.toFloat() }),
    MORANDI("莫兰迪系", listOf(30f, 60f, 100f, 160f, 200f, 270f, 320f, 350f)),
    WARM_RED("暖红系", (0..30 step 6).map { it.toFloat() }),
    COOL_BLUE("冷蓝系", (200..260 step 12).map { it.toFloat() }),
    FOREST("森林系", listOf(80f, 100f, 120f, 140f, 160f)),
    SUNSET("日落系", listOf(10f, 20f, 30f, 40f, 290f, 310f, 330f));

    companion object {
        fun templates(): List<ColorTemplate> = entries.toList()
    }
}
