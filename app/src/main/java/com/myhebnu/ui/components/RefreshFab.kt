package com.myhebnu.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * 悬浮刷新图标（MD3 FAB 形态：primaryContainer 底 + shapes.large 圆角）。
 *
 * 与 Material3 的 FloatingActionButton 相比多支持长按——成绩页用短按刷新最新学期、
 * 长按强制全量刷新。长按生效时给一次轻微震动反馈（否则用户无从判断该按几秒）；
 * 刷新中显示进度圈并禁用点击。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingRefreshButton(
    isRefreshing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    contentDescription: String = "刷新"
) {
    val haptics = LocalHapticFeedback.current
    val shape = MaterialTheme.shapes.large
    Surface(
        modifier = modifier
            .size(56.dp)
            .clip(shape)
            .combinedClickable(
                enabled = !isRefreshing,
                onClick = onClick,
                onLongClick = onLongClick?.let { action ->
                    {
                        // 长按生效即震动，用户不必自行揣测按压时长
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        action()
                    }
                }
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
