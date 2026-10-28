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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myhebnu.data.remote.opac.OpacLoan
import com.myhebnu.ui.components.FloatingRefreshButton
import com.myhebnu.ui.design.GroupedSurface
import com.myhebnu.ui.design.InlineNotice
import com.myhebnu.ui.design.LoadingBlock
import com.myhebnu.ui.design.MinTouchTarget
import com.myhebnu.ui.design.ScreenScaffold
import com.myhebnu.ui.design.SectionDivider
import com.myhebnu.ui.design.Spacing
import com.myhebnu.ui.design.StateBlock
import com.myhebnu.ui.design.StatusPill
import com.myhebnu.ui.design.Tone

/**
 * 当前借阅。
 *
 * 整页只有**一个**分组容器，书与书之间用发丝线分开——
 * 一本书一张卡片会让一屏出现十几个圆角矩形，视觉噪点远大于信息量。
 * 续借本期未做，所以整页没有可点击的动作，这是只读页面的正常形态。
 */
@Composable
fun LoansScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoansViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val nearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 2
        }
    }

    LaunchedEffect(nearBottom) {
        if (nearBottom) viewModel.loadMore()
    }

    ScreenScaffold(
        title = "当前借阅",
        modifier = modifier,
        onBack = onBack,
        floatingActionButton = {
            FloatingRefreshButton(
                isRefreshing = uiState.isLoading,
                onClick = viewModel::refresh,
                contentDescription = "刷新借阅记录"
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

                uiState.error != null && uiState.loans.isEmpty() -> StateBlock(
                    title = uiState.error.orEmpty(),
                    description = "借阅记录来自图书馆系统，需要联网获取。",
                    actionLabel = "重试",
                    onAction = viewModel::refresh
                )

                uiState.isEmpty -> StateBlock(
                    title = "当前没有在借图书",
                    description = "借到书以后，这里会显示每本书的应还日期。"
                )

                else -> LazyColumn(state = listState) {
                    // 册数与借阅额度已经在图书馆首页的指标条里，这页不再重复一遍
                    if (uiState.error != null) {
                        item {
                            InlineNotice(
                                text = uiState.error.orEmpty(),
                                actionLabel = "重试",
                                onAction = viewModel::refresh,
                                modifier = Modifier.padding(horizontal = Spacing.screen)
                            )
                        }
                    }

                    item {
                        GroupedSurface(
                            modifier = Modifier.padding(
                                start = Spacing.screen,
                                end = Spacing.screen,
                                top = Spacing.md
                            )
                        ) {
                            val lastIndex = uiState.loans.lastIndex
                            uiState.loans.forEachIndexed { index, loan ->
                                LoanRow(
                                    loan = loan,
                                    showDivider = index != lastIndex
                                )
                            }
                        }
                    }

                    if (uiState.isLoadingMore) {
                        item { LoadingBlock() }
                    }

                    item { Spacer(Modifier.height(Spacing.xxxl)) }
                }
            }
        }
    }
}

/**
 * 借阅行。
 *
 * 应还日期是次要行（带语气色），「还有几天」用 [StatusPill] 放在右上——
 * 真正需要一眼扫到的是紧迫程度，而不是日期本身。
 */
@Composable
private fun LoanRow(
    loan: OpacLoan,
    showDivider: Boolean
) {
    val tone = LibraryFormat.dueTone(loan.dueDate)
    val pillTone = when (tone) {
        LibraryFormat.DueTone.Overdue -> Tone.Critical
        LibraryFormat.DueTone.Soon -> Tone.Warning
        LibraryFormat.DueTone.Normal -> Tone.Neutral
    }
    val dueColor = when (tone) {
        LibraryFormat.DueTone.Overdue -> MaterialTheme.colorScheme.error
        LibraryFormat.DueTone.Soon -> MaterialTheme.colorScheme.tertiary
        LibraryFormat.DueTone.Normal -> MaterialTheme.colorScheme.onSurfaceVariant
    }

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
                )
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = loan.title.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val supporting = listOfNotNull(
                    loan.author?.takeIf { it.isNotBlank() },
                    loan.location?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (supporting.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = buildString {
                        append(LibraryFormat.shortDate(loan.dueDate))
                        append(" 应还")
                        val renews = loan.renewTimes ?: 0
                        if (renews > 0) append(" · 已续借 $renews 次")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = dueColor
                )
            }

            Spacer(Modifier.width(Spacing.md))

            StatusPill(
                text = LibraryFormat.dueLabel(loan.dueDate),
                tone = pillTone
            )
        }

        if (showDivider) {
            SectionDivider(startInset = Spacing.xl)
        }
    }
}
