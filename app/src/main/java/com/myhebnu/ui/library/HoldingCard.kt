package com.myhebnu.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myhebnu.data.remote.opac.OpacHolding
import com.myhebnu.ui.design.GroupRow
import com.myhebnu.ui.design.GroupedSurface
import com.myhebnu.ui.design.Motion
import com.myhebnu.ui.design.SectionDivider
import com.myhebnu.ui.design.Spacing
import com.myhebnu.ui.design.StatusPill
import com.myhebnu.ui.design.Tone

/**
 * 馆藏卡片。
 *
 * 视觉重心放在**索书号**（读者到馆找书真正要看的东西），馆藏地退为辅助行，
 * 条码号不展示——它只在馆员侧有意义。
 */
internal val HoldingRowHeight = 60.dp

/** 折叠态最多展示几条馆藏；超出部分折进「展开全部」。 */
internal const val CollapsedHoldingCount = 2

@Composable
fun HoldingCard(
    holdings: List<OpacHolding>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (holdings.isEmpty()) return

    val hasMore = holdings.size > CollapsedHoldingCount
    val head = holdings.take(CollapsedHoldingCount)
    // 折叠态收进这个尾部；展开时由 AnimatedVisibility 逐行落下
    val tail = if (hasMore) holdings.drop(CollapsedHoldingCount) else emptyList()

    GroupedSurface(modifier = modifier.fillMaxWidth()) {
        head.forEach { holding ->
            HoldingRowItem(holding = holding, showDivider = true)
        }

        // 展开/收起各有一条曲线：展开稍慢（给人「内容长出来」的从容感），
        // 收起更快——收拢是结束动作，拖长了会显得迟钝。
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = tween(Motion.emphasized, easing = FastOutSlowInEasing)
            ),
            exit = shrinkVertically(
                animationSpec = tween(Motion.standard, easing = FastOutSlowInEasing)
            )
        ) {
            Column {
                tail.forEach { holding ->
                    HoldingRowItem(holding = holding, showDivider = true)
                }
            }
        }

        if (hasMore) {
            GroupRow(
                title = if (expanded) {
                    "收起"
                } else {
                    "展开全部 ${holdings.size} 处"
                },
                titleColor = MaterialTheme.colorScheme.primary,
                onClick = onToggleExpand
            )
        }
    }
}

@Composable
private fun HoldingRowItem(
    holding: OpacHolding,
    showDivider: Boolean
) {
    // 索书号缺失时不能留空，否则这一行就没有主信息了
    val callNo = holding.callNo?.takeIf { it.isNotBlank() } ?: "索书号待查"
    val location = holding.locationName
        ?.takeIf { it.isNotBlank() }
        ?: holding.location?.takeIf { it.isNotBlank() }
        ?: holding.library?.takeIf { it.isNotBlank() }
        ?: "未知馆藏地"
    val available = holding.circStatus == 0

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(HoldingRowHeight)
                .padding(start = Spacing.xl, end = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = callNo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(Spacing.md))

            StatusPill(
                text = if (available) "可借" else "借出",
                tone = if (available) Tone.Positive else Tone.Neutral
            )
        }

        if (showDivider) SectionDivider()
    }
}
