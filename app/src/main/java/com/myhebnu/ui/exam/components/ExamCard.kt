package com.myhebnu.ui.exam.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myhebnu.domain.Exam
import java.time.LocalDate

/**
 * MD3 ElevatedCard displaying a single exam entry.
 *
 * Layout: Row split into left info column (weight 1f) and right countdown badge.
 * Countdown color follows the urgency rules:
 *   ≤3 days → error, 4-7 → tertiary, 8-14 → onSurfaceVariant, >14 → faded, <0 → outline + "已结束"
 */
@Composable
fun ExamCard(
    exam: Exam,
    modifier: Modifier = Modifier
) {
    val days = exam.daysRemaining
    val isExpired = days < 0

    val countdownColor = when {
        isExpired -> MaterialTheme.colorScheme.outline
        days <= 3 -> MaterialTheme.colorScheme.error
        days <= 7 -> MaterialTheme.colorScheme.tertiary
        days <= 14 -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    val accessibilityLabel = buildString {
        append(exam.courseName)
        append("，")
        append(Exam.formatDate(exam.examDate))
        append("，")
        append(exam.location)
        append("，")
        if (isExpired) append("考试已结束") else append("距考试${days}天")
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilityLabel },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // --- Left column: course info ---
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Course name
                Text(
                    text = exam.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                // Date and time
                Text(
                    text = buildString {
                        append(Exam.formatDate(exam.examDate))
                        val timeStr = Exam.formatTimeRange(exam.startTime, exam.endTime)
                        if (timeStr.isNotEmpty()) {
                            append(" · ")
                            append(timeStr)
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Location and seat
                Text(
                    text = buildString {
                        if (exam.location.isNotEmpty()) {
                            append(exam.location)
                        }
                        if (exam.seatNumber.isNotEmpty()) {
                            if (exam.location.isNotEmpty()) append("  ")
                            append("座位: ")
                            append(exam.seatNumber)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            // --- Right column: countdown badge ---
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.width(64.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isExpired) "已结束" else "还剩",
                        style = MaterialTheme.typography.labelSmall,
                        color = countdownColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isExpired) "" else days.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = countdownColor,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    if (!isExpired) {
                        Text(
                            text = "天",
                            style = MaterialTheme.typography.labelSmall,
                            color = countdownColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
