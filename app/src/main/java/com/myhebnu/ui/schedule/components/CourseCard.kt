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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.ui.theme.CourseTonalPalette

@Composable
fun CourseCard(
    course: CourseEntity,
    isActive: Boolean,
    palette: CourseTonalPalette,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val borderWidth = if (isActive) 2.dp else 0.dp
    val gap = 3.dp

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
            // Course name — 11sp / 14sp
            Text(
                text = course.courseName,
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    lineBreak = LineBreak.Paragraph
                ),
                color = palette.onContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Classroom — 10sp / 12sp
            if (course.classroom.isNotBlank()) {
                Spacer(Modifier.height(gap))
                Text(
                    text = course.classroom,
                    style = TextStyle(
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        lineBreak = LineBreak.Paragraph
                    ),
                    color = palette.variant,
                    maxLines = 2
                )
            }

            // Teacher — 9.5sp / 12sp
            if (course.teacher.isNotBlank()) {
                Spacer(Modifier.height(gap))
                Text(
                    text = course.teacher,
                    style = TextStyle(fontSize = 9.5.sp, lineHeight = 12.sp),
                    color = palette.variant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
