package com.myhebnu.ui.design

import androidx.compose.ui.unit.dp

/**
 * 设计令牌 · 间距体系
 *
 * 全应用唯一的间距来源。页面不直接写 `16.dp` 这类字面量，而是取这里的值，
 * 从而保证「同组元素紧凑、不同组之间留白明确」这一空间秩序在任何页面都成立。
 *
 * 取值遵循 4dp 基准网格；[screen] 与 [sectionGap] 是两条最常用的规则：
 * - [screen]：页面左右留白（比旧页面的 16dp 略宽，换来更从容的边距感）
 * - [sectionGap]：内容组之间的垂直间隔，明显大于组内 [itemGap]
 */
object Spacing {
    /** 2dp · 紧贴的图文间距（如标签与数值） */
    val xxs = 2.dp

    /** 4dp · 同一行内的小间隔 */
    val xs = 4.dp

    /** 8dp · 相关元素之间 */
    val sm = 8.dp

    /** 12dp · 列表项之间 */
    val md = 12.dp

    /** 16dp · 容器内部标准内边距 */
    val lg = 16.dp

    /** 20dp · 页面左右留白 / 卡片内边距 */
    val xl = 20.dp

    /** 28dp · 组与组之间 */
    val xxl = 28.dp

    /** 40dp · 页面顶部大标题之上、页面底部的呼吸空间 */
    val xxxl = 40.dp

    /** 页面左右留白 */
    val screen = xl

    /** 内容组之间的垂直间隔 */
    val sectionGap = xxl

    /** 同组列表项之间的间隔 */
    val itemGap = md

    /** 列表底部额外留白，避免最后一项贴住导航条 / FAB */
    val scrollBottom = 32.dp
}

/**
 * 设计令牌 · 最小触控尺寸
 *
 * 无障碍要求：任何可点击元素的可触控高度不低于 48dp。
 * 视觉上可以更矮（例如用 `Modifier.heightIn` + 内边距控制），但触控区域必须满足这个值。
 */
val MinTouchTarget = 48.dp

/**
 * 设计令牌 · 动效时长（毫秒）
 *
 * 动画只服务于三件事：交互反馈、状态变化、空间连续性。
 * 超过 [emphasized] 的时长在这个应用里都属于「让用户等」。
 */
object Motion {
    /** 120ms · 按下、抬起的即时反馈 */
    const val fast = 120

    /** 200ms · 展开/收起、淡入淡出 */
    const val standard = 200

    /** 300ms · 页面转场、共享元素 */
    const val emphasized = 300
}

/**
 * 设计令牌 · 分隔线
 *
 * 「用细微分隔线表达结构」是本应用的基本手段：0.5dp 的发丝线 + 半透明轮廓色，
 * 在不制造明确边界感的前提下标示分组结束。
 */
object DividerSpec {
    val thickness = 0.5.dp

    /** 分隔线相对容器左右边缘的内缩，与文字起始位置对齐 */
    val inset = Spacing.lg
}
