package com.myhebnu.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MyHEBNUTheme(
    themeMode: String = "system",
    useCustomColors: Boolean = false,
    seedHue: Float? = null,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val colorScheme = when {
        // 自定义 seed hue 优先
        useCustomColors && seedHue != null -> seedColorScheme(seedHue, darkTheme)
        // 其次跟随系统动态取色（Android 12+）
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> defaultDarkColorScheme()
        else -> defaultLightColorScheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        // 边缘到边缘（enableEdgeToEdge）下状态栏由系统合成，MD3 也不再给状态栏涂品牌色；
        // 这里只同步图标明暗，容器色交给各页 TopAppBar / Surface。
        DisposableEffect(colorScheme.primary, darkTheme) {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            onDispose { }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
