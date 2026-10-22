package com.myhebnu.ui.schedule.components

import androidx.compose.ui.unit.Dp
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.ui.theme.CourseTonalPalette

/**
 * 被点下的课表格子的完整快照。
 *
 * 详情层要「从格子原位翻开」，就必须能在同一个位置、
 * 用同一套排版画出这张格子（正面），所以把绘制格子所需的一切都带出来：
 * 课程、是否高亮、配色、格子宽高。
 */
data class CourseCellSnapshot(
    val course: CourseEntity,
    val isActive: Boolean,
    val palette: CourseTonalPalette,
    val cardWidth: Dp,
    val cardHeight: Dp,
    /** 点击瞬间格子处于「按下」缩放的哪一档（0.94→1），详情层从这一档接着回到 1，避免松手闪一下 */
    val pressScale: Float = 1f
)
