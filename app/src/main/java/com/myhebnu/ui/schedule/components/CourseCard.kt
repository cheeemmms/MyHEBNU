package com.myhebnu.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.ui.theme.CourseTonalPalette
import kotlin.math.max

/**
 * 课表格子。
 *
 * 排版约定（2026-10-21 用户确认）：
 * - 课名在顶部，尽量完整展示（行数由格子高度决定，不再一味压行数）；
 * - 教室紧随其后；
 * - 教师固定在卡片最底部（用 weight 顶到下方），长格子不会出现「教师飘在中间」的观感。
 */
@Composable
fun CourseCard(
    course: CourseEntity,
    isActive: Boolean,
    palette: CourseTonalPalette,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 0.dp,
    onClick: (() -> Unit)? = null
) {
    val borderWidth = if (isActive) 2.dp else 0.dp
    val gap = 3.dp
    val shape = MaterialTheme.shapes.medium

    // 卡片内可用高度 = 总高 - 上下 padding（各 6dp）
    val innerH = (cardHeight - 12.dp).coerceAtLeast(0.dp)

    val nameLineDp = 14.dp
    val metaLineDp = 12.dp
    val teacherLineDp = 14.dp

    val hasRoom = course.classroom.isNotBlank()
    val hasTeacher = course.teacher.isNotBlank()

    // 课名优先完整展示：按「可用宽度能放几个字」估行数，再受格子高度约束
    val textWidthDp = (if (cardWidth > 0.dp) cardWidth else 96.dp) - 16.dp
    val nameLinesNeeded = estimateLines(course.courseName, textWidthDp, 11.dp)
    val reservedBottom =
        (if (hasRoom) gap + metaLineDp else 0.dp) +
        (if (hasTeacher) gap + metaLineDp + teacherLineDp else 0.dp)
    val nameBudgetLines = max(1, ((innerH - reservedBottom).value / nameLineDp.value).toInt())
    val nameMaxLines = nameLinesNeeded.coerceAtMost(nameBudgetLines).coerceAtLeast(1)

    // 教室：不设两行上限，占满剩余高度；教师之前至少留一行空白，
    // 超长时在留白前折叠（用户 2026-10-21 指定）。
    val roomBudgetDp = (
        innerH -
        nameLineDp * nameMaxLines -
        (if (hasTeacher) metaLineDp * 2 + teacherLineDp else 0.dp)
    ).coerceAtLeast(0.dp)
    val roomMaxLines = if (!hasRoom) 0
        else max(1, (roomBudgetDp.value / metaLineDp.value).toInt())

    // 极矮格子（单节且信息密集）略微缩小课名，避免单行溢出
    val nameFontSize = if (innerH < 40.dp) 10.sp else 11.sp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.container)
            .then(
                if (isActive) Modifier.border(borderWidth, palette.onContainer, shape)
                else Modifier
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 课名
            Text(
                text = course.courseName,
                fontWeight = FontWeight.Medium,
                fontSize = nameFontSize,
                lineHeight = 14.sp,
                color = palette.onContainer,
                maxLines = nameMaxLines,
                overflow = TextOverflow.Ellipsis
            )

            // 教室
            if (course.classroom.isNotBlank()) {
                Spacer(Modifier.height(gap))
                Text(
                    text = course.classroom,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = palette.variant,
                    maxLines = roomMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 教师固定在底部
            Spacer(Modifier.weight(1f))

            if (course.teacher.isNotBlank()) {
                Text(
                    text = course.teacher,
                    fontSize = 9.5.sp,
                    lineHeight = 12.sp,
                    color = palette.variant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 粗估一段中文/中英混排在给定宽度下会占几行（1 个汉字 ≈ 1em）。 */
private fun estimateLines(text: String, widthDp: Dp, charDp: Dp): Int {
    if (text.isBlank() || widthDp <= 0.dp || charDp <= 0.dp) return 1
    val perLine = max(1, (widthDp.value / charDp.value).toInt())
    return max(1, (text.length + perLine - 1) / perLine)
}
