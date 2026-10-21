package com.myhebnu.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * MD3 形状体系。
 *
 * 全应用只从这里取圆角，避免每个文件各写一个 `RoundedCornerShape`：
 * 卡片用 [medium]/[large]、按钮与 chip 用 `shapes.full`、底部弹层用 [extraLarge]。
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
