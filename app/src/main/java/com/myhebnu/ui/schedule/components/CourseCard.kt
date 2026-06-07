package com.myhebnu.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhebnu.data.local.db.entity.CourseEntity

/**
 * A compact course card designed to fit inside the week view grid cells.
 * Shows course name, teacher, and classroom with a colored left border.
 */
@Composable
fun CourseCard(
    course: CourseEntity,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val baseColor = Color(course.color)
    val bgColor = baseColor.copy(alpha = 0.12f)
    val borderColor = if (isActive) baseColor else baseColor.copy(alpha = 0.5f)
    val borderWidth = if (isActive) 3.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor, RoundedCornerShape(6.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // Colored left indicator bar
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .align(androidx.compose.ui.Alignment.CenterStart)
                .offset(x = (-6).dp)
                .background(baseColor, RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            // Course name
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 14.sp
                ),
                color = baseColor.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Teacher
            if (course.teacher.isNotBlank()) {
                Text(
                    text = course.teacher,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = baseColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Classroom — allow wrapping, no truncation
            if (course.classroom.isNotBlank()) {
                Text(
                    text = course.classroom,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = baseColor.copy(alpha = 0.6f),
                    maxLines = 2
                )
            }
        }
    }
}
