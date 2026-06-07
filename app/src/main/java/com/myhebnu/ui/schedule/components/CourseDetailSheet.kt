package com.myhebnu.ui.schedule.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myhebnu.data.local.db.entity.CourseEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailSheet(
    course: CourseEntity,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Course name
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Detail rows
            if (course.teacher.isNotBlank()) {
                DetailRow(label = "教师", value = course.teacher)
            }
            if (course.classroom.isNotBlank()) {
                DetailRow(label = "教室", value = course.classroom)
            }
            if (course.weekText.isNotBlank()) {
                DetailRow(label = "上课周次", value = course.weekText)
            }
            val dayLabel = when (course.dayOfWeek) {
                1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
                5 -> "周五"; 6 -> "周六"; 7 -> "周日"; else -> ""
            }
            DetailRow(label = "时间", value = "$dayLabel ${course.startPeriod}-${course.endPeriod}节")
            if (course.category.isNotBlank()) {
                DetailRow(label = "类别", value = course.category)
            }
            if (course.semesterName.isNotBlank()) {
                DetailRow(label = "学期", value = course.semesterName)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
