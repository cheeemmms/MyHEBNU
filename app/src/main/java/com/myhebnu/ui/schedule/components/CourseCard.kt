package com.myhebnu.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            // Course name — Title Medium 16sp Medium
            Text(
                text = course.courseName,
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    lineBreak = LineBreak.Paragraph
                ),
                color = palette.onContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Classroom — Body Medium 14sp + inline place icon
            if (course.classroom.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = palette.variant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = course.classroom,
                        style = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            lineBreak = LineBreak.Paragraph
                        ),
                        color = palette.variant,
                        maxLines = 2
                    )
                }
            }

            // Teacher — Label Large 12sp, subdued
            if (course.teacher.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = course.teacher,
                    style = TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = palette.variant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
