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

    // 课名按可用高度在 14 / 13 / 12sp 之间退让，不低于 MD3 bodySmall(12sp)
    val nameFontSize = when {
        innerH >= 76.dp -> 14.sp
        innerH >= 56.dp -> 13.sp
        else -> 12.sp
    }
    val nameLineSp = (nameFontSize.value * 1.35f).sp
    val nameLineDp = nameLineSp.value.dp
    // 次信息（教室 / 教师）统一按 labelSmall(11sp) 处理
    val metaLineSp = 14.sp
    val metaLineDp = metaLineSp.value.dp

    // 非课名部分预算：教室空间充足才 2 行，否则 1 行；老师始终 1 行；各含上方 gap
    var fixedH = 0.dp
    val roomMaxLines = if (innerH >= 72.dp) 2 else 1
    if (course.classroom.isNotBlank()) {
        fixedH += gap + metaLineDp * roomMaxLines
    }
    if (course.teacher.isNotBlank()) {
        fixedH += gap + metaLineDp
    }

    // 课名可用高度 → 动态行数；空间足够即填满长条，仅快溢出才限行
    val nameBudget = (innerH - fixedH).coerceAtLeast(nameLineDp)
    val nameMaxLines = max(1, (nameBudget.value / nameLineDp.value).toInt())

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(palette.container)
            .then(
                if (isActive) Modifier.border(borderWidth, palette.onContainer, MaterialTheme.shapes.medium)
                else Modifier
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = course.courseName,
                fontWeight = FontWeight.Medium,
                fontSize = nameFontSize,
                lineHeight = nameLineSp,
                color = palette.onContainer,
                maxLines = nameMaxLines,
                overflow = TextOverflow.Ellipsis
            )

            if (course.classroom.isNotBlank()) {
                Spacer(Modifier.height(gap))
                Text(
                    text = course.classroom,
                    fontSize = 11.sp,
                    lineHeight = metaLineSp,
                    color = palette.variant,
                    maxLines = roomMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (course.teacher.isNotBlank()) {
                Spacer(Modifier.height(gap))
                Text(
                    text = course.teacher,
                    fontSize = 11.sp,
                    lineHeight = metaLineSp,
                    color = palette.variant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
