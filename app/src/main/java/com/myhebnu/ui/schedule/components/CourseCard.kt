package com.myhebnu.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun CourseCard(
    course: CourseEntity,
    isActive: Boolean,
    palette: CourseTonalPalette,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val borderWidth = if (isActive) 2.dp else 0.dp
    val gap = 3.dp

    // 卡片内可用高度 = 总高 - 上下 padding（各 6dp）
    val innerH = (cardHeight - 12.dp).coerceAtLeast(0.dp)

    // 行高（像素高度计算用 Dp）
    val nameLineDp = 14.dp
    val roomLineDp = 12.dp
    val teacherLineDp = 12.dp

    // 非课名部分预算：教室空间充足才 2 行，否则 1 行；老师始终 1 行；各含上方 gap
    var fixedH = 0.dp
    val roomMaxLines = if (innerH >= 70.dp) 2 else 1
    if (course.classroom.isNotBlank()) {
        fixedH += gap + roomLineDp * roomMaxLines
    }
    if (course.teacher.isNotBlank()) {
        fixedH += gap + teacherLineDp
    }

    // 课名可用高度 → 动态行数；空间足够即填满长条，仅快溢出才限行
    val nameBudget = (innerH - fixedH).coerceAtLeast(nameLineDp)
    val nameMaxLines = max(1, (nameBudget.value / nameLineDp.value).toInt())

    // 极矮卡片（单节且信息密集）略微缩小课名，避免单行溢出
    val nameFontSize = if (innerH < 40.dp) 10.sp else 11.sp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.container)
            .then(
                if (isActive) Modifier.border(borderWidth, palette.onContainer, RoundedCornerShape(12.dp))
                else Modifier
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Course name — 动态行数 / 11sp（极矮 10sp）
            Text(
                text = course.courseName,
                fontWeight = FontWeight.Medium,
                fontSize = nameFontSize,
                lineHeight = 14.sp,
                color = palette.onContainer,
                maxLines = nameMaxLines,
                overflow = TextOverflow.Ellipsis
            )

            // Classroom — 10sp
            if (course.classroom.isNotBlank()) {
                Spacer(Modifier.height(gap))
                Text(
                    text = course.classroom,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = palette.variant,
                    maxLines = roomMaxLines
                )
            }

            // Teacher — 9.5sp
            if (course.teacher.isNotBlank()) {
                Spacer(Modifier.height(gap))
                Text(
                    text = course.teacher,
                    fontSize = 9.5.sp,
                    lineHeight = 12.sp,
                    color = palette.variant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
