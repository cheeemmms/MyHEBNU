package com.myhebnu.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myhebnu.R
import com.myhebnu.ui.home.HomeUiState
import com.myhebnu.ui.home.NextClassState

@Composable
fun HomeCardPanel(
    uiState: HomeUiState,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Row 1: Next Class
            HomeCardRow(
                title = stringResource(R.string.home_next_class),
                info = buildNextClassInfo(uiState),
                onClick = { onNavigate("schedule") }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            // Row 2: Empty Room
            HomeCardRow(
                title = stringResource(R.string.home_empty_room),
                info = stringResource(R.string.home_empty_room_hint),
                onClick = { onNavigate("empty_room") }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            // Row 3: Next Exam
            HomeCardRow(
                title = stringResource(R.string.home_next_exam),
                info = buildNextExamInfo(uiState),
                onClick = { onNavigate("exam") }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            // Row 4: Grade
            HomeCardRow(
                title = stringResource(R.string.home_grade),
                info = buildGradeInfo(uiState),
                onClick = { onNavigate("grade") }
            )
        }
    }
}

@Composable
private fun HomeCardRow(
    title: String,
    info: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = info,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.65f),
            maxLines = 3
        )
    }
}

@Composable
private fun buildNextClassInfo(state: HomeUiState): String {
    return when (state.nextClassState) {
        NextClassState.HAS_CLASS -> {
            buildString {
                append(state.nextClassCourse)
                if (state.nextClassRoom.isNotEmpty()) {
                    append(" · ")
                    append(state.nextClassRoom)
                }
                if (state.nextClassTeacher.isNotEmpty()) {
                    append(" · ")
                    append(state.nextClassTeacher)
                }
                if (state.nextClassTime.isNotEmpty()) {
                    append("\n")
                    append(state.nextClassTime)
                }
            }
        }
        NextClassState.ALL_DONE -> stringResource(R.string.home_class_done)
        NextClassState.WEEKEND -> stringResource(R.string.home_weekend)
        NextClassState.HOLIDAY -> stringResource(R.string.home_holiday)
    }
}

@Composable
private fun buildNextExamInfo(state: HomeUiState): String {
    if (!state.hasExam) return stringResource(R.string.home_no_exam)
    return buildString {
        append(state.nextExamCourse)
        if (state.nextExamDate.isNotEmpty()) {
            append(" · ")
            append(state.nextExamDate)
        }
        if (state.nextExamLocation.isNotEmpty()) {
            append("\n")
            append(state.nextExamLocation)
        }
        if (state.nextExamSeat.isNotEmpty()) {
            append(" · 座位:")
            append(state.nextExamSeat)
        }
        if (state.nextExamDays > 0) {
            append(" · 剩")
            append(state.nextExamDays)
            append("天")
        }
    }
}

@Composable
private fun buildGradeInfo(state: HomeUiState): String {
    if (!state.hasGrades || state.weightedAvg == null) {
        return stringResource(R.string.home_no_grade)
    }
    return "本学期加权平均: ${"%.1f".format(state.weightedAvg)}"
}
