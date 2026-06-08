package com.myhebnu.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val nextClassTitle = when (uiState.nextClassState) {
        NextClassState.IN_CLASS -> "正在上课"
        else -> stringResource(R.string.home_next_class)
    }
    val cards = listOf(
        HomeCardData(
            title = nextClassTitle,
            info = buildNextClassInfo(uiState),
            route = "schedule",
            icon = Icons.Filled.CalendarToday
        ),
        HomeCardData(
            title = stringResource(R.string.home_empty_room),
            info = stringResource(R.string.home_empty_room_hint),
            route = "empty_room",
            icon = Icons.Filled.MeetingRoom
        ),
        HomeCardData(
            title = stringResource(R.string.home_next_exam),
            info = buildNextExamInfo(uiState),
            route = "exam",
            icon = Icons.AutoMirrored.Filled.Assignment
        ),
        HomeCardData(
            title = stringResource(R.string.home_grade),
            info = buildGradeInfo(uiState),
            route = "grade",
            icon = Icons.Filled.Grade
        )
    )

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(cards) { card ->
            HomeInfoCard(card = card, onClick = { onNavigate(card.route) })
        }
    }
}

private data class HomeCardData(
    val title: String, val info: String, val route: String, val icon: ImageVector
)

@Composable
private fun HomeInfoCard(card: HomeCardData, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = card.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = card.info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
private fun buildNextClassInfo(state: HomeUiState): String {
    return when (state.nextClassState) {
        NextClassState.IN_CLASS -> {
            buildString {
                append(state.nextClassCourse)
                if (state.nextClassRoom.isNotEmpty()) { append(" · "); append(state.nextClassRoom) }
                if (state.nextClassTeacher.isNotEmpty()) { append(" · "); append(state.nextClassTeacher) }
                if (state.nextClassTime.isNotEmpty()) { append(" · "); append(state.nextClassTime) }
                if (state.nextClassRemaining.isNotEmpty()) { append("\n"); append(state.nextClassRemaining) }
            }
        }
        NextClassState.HAS_CLASS -> {
            buildString {
                append(state.nextClassCourse)
                if (state.nextClassRoom.isNotEmpty()) { append(" · "); append(state.nextClassRoom) }
                if (state.nextClassTeacher.isNotEmpty()) { append(" · "); append(state.nextClassTeacher) }
                if (state.nextClassTime.isNotEmpty()) { append("\n"); append(state.nextClassTime) }
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
    return "${state.nextExamCourse} · ${state.nextExamDate}\n${state.nextExamLocation} · 座位:${state.nextExamSeat} · 剩${state.nextExamDays}天"
}

@Composable
private fun buildGradeInfo(state: HomeUiState): String {
    if (!state.hasGrades || state.weightedAvg == null) return stringResource(R.string.home_no_grade)
    return "本学期加权平均: ${"%.1f".format(state.weightedAvg)}"
}
