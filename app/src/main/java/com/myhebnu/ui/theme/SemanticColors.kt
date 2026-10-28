package com.myhebnu.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * MD3 语义色之外、本项目自己需要的补充色。
 *
 * 目前只有一组「成功色」：Material3 的 `colorScheme` 在琥珀/蓝色调色板下
 * 没有可当作「通过 / 在架 / 入藏」的绿色角色，而 `StatusPill` 的
 * [com.myhebnu.ui.design.Tone.Positive] 必须和 [com.myhebnu.ui.design.Tone.Warning]
 * 在视觉上区分得开，否则「分级」在界面上读不出来。
 */
data class SemanticColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color
)

/** 由 [MyHEBNUTheme] 按深浅主题注入，不做默认值猜测。 */
val LocalSemanticColors = staticCompositionLocalOf {
    SemanticColors(
        success = Color.Unspecified,
        successContainer = Color.Unspecified,
        onSuccessContainer = Color.Unspecified
    )
}
