package com.myhebnu.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

// Success（语义「通过 / 在架 / 入藏」）—— 调色板本身只有蓝-青-琥珀，
// 没有绿色角色，所以单独给一组固定的成功色，深浅主题各一套。
val SuccessLight = Color(0xFF1F6B33)
val SuccessContainerLight = Color(0xFFD3EFD8)
val OnSuccessContainerLight = Color(0xFF0C3315)
val SuccessDark = Color(0xFF9BD8A8)
val SuccessContainerDark = Color(0xFF23432B)
val OnSuccessContainerDark = Color(0xFFBDEEC8)

// ---- MD3 容器 / 描边角色（与上面的色阶同源，全部不透明） ----

/** `surfaceContainer*`、`surfaceDim`、`surfaceBright`、`outlineVariant` 一组派生色。 */
data class SurfaceRoles(
    val containerLowest: Color,
    val containerLow: Color,
    val container: Color,
    val containerHigh: Color,
    val containerHighest: Color,
    val dim: Color,
    val bright: Color,
    val outlineVariant: Color
)

/** 浅色主题的五档容器：越往上越暗，全部跟随主色相、保持低饱和。 */
fun lightSurfaceRoles(hue: Float, saturation: Float = 0.10f): SurfaceRoles = SurfaceRoles(
    containerLowest = Color.hsl(hue, saturation * 0.4f, 1.000f),
    containerLow = Color.hsl(hue, saturation * 0.8f, 0.975f),
    container = Color.hsl(hue, saturation, 0.945f),
    containerHigh = Color.hsl(hue, saturation, 0.915f),
    containerHighest = Color.hsl(hue, saturation, 0.885f),
    dim = Color.hsl(hue, saturation * 0.8f, 0.855f),
    bright = Color.hsl(hue, saturation * 0.6f, 0.990f),
    outlineVariant = Color.hsl(hue, saturation, 0.800f)
)

/** 深色主题的五档容器：越往上越亮。 */
fun darkSurfaceRoles(hue: Float, saturation: Float = 0.12f): SurfaceRoles = SurfaceRoles(
    containerLowest = Color.hsl(hue, saturation, 0.050f),
    containerLow = Color.hsl(hue, saturation, 0.100f),
    container = Color.hsl(hue, saturation, 0.130f),
    containerHigh = Color.hsl(hue, saturation, 0.170f),
    containerHighest = Color.hsl(hue, saturation, 0.210f),
    dim = Color.hsl(hue, saturation * 0.8f, 0.060f),
    bright = Color.hsl(hue, saturation, 0.240f),
    outlineVariant = Color.hsl(hue, saturation, 0.300f)
)

/** 把容器角色一次性写进 ColorScheme（`ColorScheme.copy` 参数太多，集中在此处）。 */
fun ColorScheme.withSurfaces(roles: SurfaceRoles): ColorScheme = copy(
    surfaceContainerLowest = roles.containerLowest,
    surfaceContainerLow = roles.containerLow,
    surfaceContainer = roles.container,
    surfaceContainerHigh = roles.containerHigh,
    surfaceContainerHighest = roles.containerHighest,
    surfaceDim = roles.dim,
    surfaceBright = roles.bright,
    outlineVariant = roles.outlineVariant
)

/** 品牌固有色相（师大蓝）；自定义配色用用户选的 seed hue 覆盖。 */
private const val BRAND_HUE = 210f

/** 既未开启动态取色、也未启用自定义配色时的浅色方案。 */
fun defaultLightColorScheme(): ColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = Teal40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal10,
    tertiary = Amber40,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
    error = Red40,
    onError = Color(0xFFFFFFFF),
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF79747E),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Blue80,
    surfaceTint = Blue40,
    scrim = Color(0xFF000000)
).withSurfaces(lightSurfaceRoles(BRAND_HUE))

/** 既未开启动态取色、也未启用自定义配色时的深色方案。 */
fun defaultDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    secondary = Teal80,
    onSecondary = Teal20,
    secondaryContainer = Teal30,
    onSecondaryContainer = Teal90,
    tertiary = Amber80,
    onTertiary = Amber20,
    tertiaryContainer = Amber30,
    onTertiaryContainer = Amber90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Blue40,
    surfaceTint = Blue80,
    scrim = Color(0xFF000000)
).withSurfaces(darkSurfaceRoles(BRAND_HUE))

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

/** Assign hues evenly spaced across 360°, rotated by [seedOffset]. */
fun assignCourseHues(courseNames: List<String>, seedOffset: Float = 0f): Map<String, Float> {
    val result = mutableMapOf<String, Float>()
    val step = 360f / maxOf(courseNames.size, 1)
    courseNames.sorted().forEachIndexed { i, name ->
        result[name] = ((seedOffset + i * step) % 360f)
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

// ============================================================
// Custom color preset (seed-hue driven, persists to UserPreferences)
// ============================================================

data class ColorPreset(
    val id: String,
    val name: String,
    val seedHue: Float,
    val isBuiltIn: Boolean = false
)

/** 6 built-in presets with stable IDs — keyed by a representative seed hue. */
fun builtInPresets(): List<ColorPreset> = listOf(
    ColorPreset("builtin_rainbow", "彩虹", 210f, isBuiltIn = true),
    ColorPreset("builtin_morandi", "莫兰迪", 45f, isBuiltIn = true),
    ColorPreset("builtin_warmred", "暖红", 5f, isBuiltIn = true),
    ColorPreset("builtin_coolblue", "冷蓝", 220f, isBuiltIn = true),
    ColorPreset("builtin_forest", "森林", 120f, isBuiltIn = true),
    ColorPreset("builtin_sunset", "日落", 25f, isBuiltIn = true)
)

/**
 * Generate a full MD3 [ColorScheme] from a single seed hue.
 *
 * Light scheme: moderate saturation, medium-high lightness for containers.
 * Dark scheme: lower saturation, lower lightness for containers.
 */
fun seedColorScheme(seedHue: Float, isDark: Boolean): ColorScheme {
    val onVariant = if (isDark) Color.hsl(seedHue, 0.05f, 0.80f) else Color.hsl(seedHue, 0.05f, 0.30f)
    return if (isDark) {
        darkColorScheme(
            primary = Color.hsl(seedHue, 0.45f, 0.75f),
            onPrimary = Color.hsl(seedHue, 0.15f, 0.15f),
            primaryContainer = Color.hsl(seedHue, 0.30f, 0.25f),
            onPrimaryContainer = Color.hsl(seedHue, 0.35f, 0.88f),
            secondary = Color.hsl((seedHue + 30) % 360, 0.25f, 0.70f),
            onSecondary = Color.hsl((seedHue + 30) % 360, 0.10f, 0.15f),
            secondaryContainer = Color.hsl((seedHue + 30) % 360, 0.20f, 0.22f),
            onSecondaryContainer = Color.hsl((seedHue + 30) % 360, 0.25f, 0.85f),
            tertiary = Color.hsl((seedHue + 60) % 360, 0.35f, 0.65f),
            onTertiary = Color.hsl((seedHue + 60) % 360, 0.10f, 0.12f),
            tertiaryContainer = Color.hsl((seedHue + 60) % 360, 0.25f, 0.20f),
            onTertiaryContainer = Color.hsl((seedHue + 60) % 360, 0.30f, 0.85f),
            error = Red80, onError = Red20, errorContainer = Red30, onErrorContainer = Red90,
            background = darkSurfaceRoles(seedHue).containerLow,
            onBackground = Neutral90,
            surface = darkSurfaceRoles(seedHue).containerLow,
            onSurface = Neutral90,
            surfaceVariant = Color.hsl(seedHue, 0.06f, 0.26f),
            onSurfaceVariant = onVariant,
            outline = Color.hsl(seedHue, 0.05f, 0.60f),
            inverseSurface = Color.hsl(seedHue, 0.04f, 0.90f),
            inverseOnSurface = Color.hsl(seedHue, 0.06f, 0.20f),
            inversePrimary = Color.hsl(seedHue, 0.40f, 0.45f),
            surfaceTint = Color.hsl(seedHue, 0.45f, 0.75f),
            scrim = Color(0xFF000000)
        ).withSurfaces(darkSurfaceRoles(seedHue))
    } else {
        lightColorScheme(
            primary = Color.hsl(seedHue, 0.40f, 0.38f),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color.hsl(seedHue, 0.30f, 0.90f),
            onPrimaryContainer = Color.hsl(seedHue, 0.50f, 0.10f),
            secondary = Color.hsl((seedHue + 30) % 360, 0.25f, 0.42f),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color.hsl((seedHue + 30) % 360, 0.20f, 0.90f),
            onSecondaryContainer = Color.hsl((seedHue + 30) % 360, 0.35f, 0.10f),
            tertiary = Color.hsl((seedHue + 60) % 360, 0.30f, 0.40f),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color.hsl((seedHue + 60) % 360, 0.25f, 0.90f),
            onTertiaryContainer = Color.hsl((seedHue + 60) % 360, 0.40f, 0.10f),
            error = Red40, onError = Color(0xFFFFFFFF), errorContainer = Red90, onErrorContainer = Red10,
            background = Neutral99,
            onBackground = Neutral10,
            surface = Neutral99,
            onSurface = Neutral10,
            surfaceVariant = Color.hsl(seedHue, 0.08f, 0.90f),
            onSurfaceVariant = onVariant,
            outline = Color.hsl(seedHue, 0.05f, 0.48f),
            inverseSurface = Color.hsl(seedHue, 0.04f, 0.20f),
            inverseOnSurface = Color.hsl(seedHue, 0.04f, 0.92f),
            inversePrimary = Color.hsl(seedHue, 0.40f, 0.80f),
            surfaceTint = Color.hsl(seedHue, 0.40f, 0.38f),
            scrim = Color(0xFF000000)
        ).withSurfaces(lightSurfaceRoles(seedHue))
    }
}

/**
 * Load a [ColorPreset] by ID, searching both built-in and custom presets.
 */
fun findPresetById(id: String, customPresets: List<ColorPreset>): ColorPreset? {
    return builtInPresets().find { it.id == id } ?: customPresets.find { it.id == id }
}
