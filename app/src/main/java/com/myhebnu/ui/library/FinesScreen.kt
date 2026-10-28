package com.myhebnu.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myhebnu.data.remote.opac.OpacRequest
import com.myhebnu.ui.components.FloatingRefreshButton
import com.myhebnu.ui.design.GroupedSurface
import com.myhebnu.ui.design.LoadingBlock
import com.myhebnu.ui.design.MinTouchTarget
import com.myhebnu.ui.design.ScreenScaffold
import com.myhebnu.ui.design.SectionDivider
import com.myhebnu.ui.design.Spacing
import com.myhebnu.ui.design.StateBlock
import com.myhebnu.ui.design.StatusPill
import com.myhebnu.ui.design.Tone

/**
 * 罚金与违约。
 *
 * 抓包样本里这个列表恒为空（读者没有欠费），所以页面的重点是「空态也要说明白」：
 * 明确告诉用户「没有欠费」和「逾期册数」，而不是一片空白。
 */
@Composable
fun FinesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FinesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = "罚金与违约",
        modifier = modifier,
        onBack = onBack,
        floatingActionButton = {
            FloatingRefreshButton(
                isRefreshing = uiState.isLoading,
                onClick = viewModel::refresh,
                contentDescription = "刷新罚金记录"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> LoadingBlock()

                uiState.error != null && uiState.records.isEmpty() -> StateBlock(
                    title = uiState.error.orEmpty(),
                    description = "罚金数据来自图书馆系统，需要联网获取。",
                    actionLabel = "重试",
                    onAction = viewModel::refresh
                )

                else -> LazyColumn {
                    item {
                        FineSummary(
                            total = uiState.totalFine,
                            overdueCount = uiState.overdueCount,
                            recordCount = uiState.records.size
                        )
                    }

                    if (uiState.isEmpty) {
                        item {
                            StateBlock(
                                title = "没有罚金或违约记录",
                                description = "按时还书就不会产生罚金。若有逾期未还，这里会显示金额与原因。"
                            )
                        }
                    } else {
                        item {
                            GroupedSurface(
                                modifier = Modifier.padding(horizontal = Spacing.screen)
                            ) {
                                uiState.records.forEachIndexed { index, record ->
                                    FineRow(
                                        record = record,
                                        showDivider = index != uiState.records.lastIndex
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(Spacing.xxxl)) }
                }
            }
        }
    }
}

@Composable
private fun FineSummary(
    total: Double,
    overdueCount: Int,
    recordCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.screen,
                end = Spacing.screen,
                top = Spacing.md,
                bottom = Spacing.xl
            )
    ) {
        Text(
            text = if (total > 0) "¥ ${LibraryFormat.money(total)}" else "¥ 0.00",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (total > 0) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = when {
                overdueCount > 0 -> "待缴金额 · $overdueCount 册逾期"
                recordCount > 0 -> "待缴金额 · $recordCount 条记录"
                else -> "待缴金额 · 没有未缴罚金"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FineRow(
    record: OpacRequest,
    showDivider: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .padding(
                    start = Spacing.xl,
                    end = Spacing.lg,
                    top = Spacing.md,
                    bottom = Spacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.objectName.orEmpty().ifBlank { "罚金记录" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = listOfNotNull(
                        record.processStatusDesc?.takeIf { it.isNotBlank() },
                        LibraryFormat.shortDateTime(record.requestDate)?.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(Spacing.md))

            StatusPill(
                text = record.requestFlagDesc.orEmpty().ifBlank { "未缴" },
                tone = Tone.Critical
            )
        }

        if (showDivider) {
            SectionDivider(startInset = Spacing.xl)
        }
    }
}
