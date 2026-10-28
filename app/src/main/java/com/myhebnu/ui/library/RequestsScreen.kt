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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myhebnu.data.remote.opac.OpacRequest
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
 * 预约与荐购。
 *
 * 两类记录共用一个列表形态，用顶部分段切换——它们都是「我提交的、等图书馆处理」的事，
 * 区别只在字段：预约有到书与取书截止，荐购有馆员回复。
 * 本期只读：提交预约/荐购都没有成功抓包样本，所以不给入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = "预约与荐购",
        modifier = modifier,
        onBack = onBack,
        floatingActionButton = {
            FloatingRefreshButton(
                isRefreshing = uiState.isLoading,
                onClick = viewModel::refresh,
                contentDescription = "刷新预约记录"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            PrimaryTabRow(
                selectedTabIndex = uiState.tab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                RequestsTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.tab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = when (tab) {
                                    RequestsTab.Reservations -> "预约/委托"
                                    RequestsTab.Suggestions -> "荐购"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    )
                }
            }

            when {
                uiState.isLoading -> LoadingBlock()

                uiState.error != null && uiState.current.isEmpty() -> StateBlock(
                    title = uiState.error.orEmpty(),
                    description = "预约记录来自图书馆系统，需要联网获取。",
                    actionLabel = "重试",
                    onAction = viewModel::refresh
                )

                uiState.isEmpty -> StateBlock(
                    title = when (uiState.tab) {
                        RequestsTab.Reservations -> "没有进行中的预约"
                        RequestsTab.Suggestions -> "没有荐购记录"
                    },
                    description = when (uiState.tab) {
                        RequestsTab.Reservations -> "在检索结果里提交预约后，会在这里显示到书与取书截止时间。"
                        RequestsTab.Suggestions -> "向图书馆推荐采购的书目会显示在这里。"
                    }
                )

                else -> LazyColumn {
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
                        Spacer(Modifier.height(Spacing.sm))
                        GroupedSurface(
                            modifier = Modifier.padding(horizontal = Spacing.screen)
                        ) {
                            val items = uiState.current
                            items.forEachIndexed { index, request ->
                                RequestRow(
                                    request = request,
                                    tab = uiState.tab,
                                    showDivider = index != items.lastIndex
                                )
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
private fun RequestRow(
    request: OpacRequest,
    tab: RequestsTab,
    showDivider: Boolean
) {
    val status = request.processStatusDesc.orEmpty()
    val flag = request.requestFlagDesc.orEmpty()
    val tone = when {
        flag.contains("取消") -> Tone.Neutral
        // 荐购的馆员处理结果：不订购 / 退订 是否决（红），入藏 / 订购 是通过（绿）。
        // 「不订购」里含有「订购」，所以否决分支必须排在前面的检查里。
        status.contains("不订购") || status.contains("退订") -> Tone.Critical
        status.contains("入藏") || status.contains("订购") -> Tone.Positive
        status.contains("已到") || status.contains("可借") || status.contains("可取消") -> Tone.Positive
        status.isBlank() -> Tone.Neutral
        else -> Tone.Warning
    }

    val supporting = buildString {
        append(LibraryFormat.shortDateTime(request.requestDate))
        append(" 提交")
        if (tab == RequestsTab.Reservations) {
            request.arrivalExpireDate?.takeIf { it.isNotBlank() }?.let {
                append(" · ")
                append(LibraryFormat.shortDate(it))
                append(" 前取书")
            }
            request.note?.takeIf { it.isNotBlank() }?.let {
                append(" · ")
                append(it)
            }
        } else {
            request.replyContent?.takeIf { it.isNotBlank() }?.let {
                append(" · 馆员回复：")
                append(it)
            }
        }
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
                ),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.objectName.orEmpty().ifBlank { "未命名书目" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (status.isNotBlank() || flag.isNotBlank()) {
                Spacer(Modifier.width(Spacing.md))
                StatusPill(
                    text = status.ifBlank { flag },
                    tone = tone
                )
            }
        }

        if (showDivider) {
            SectionDivider(startInset = Spacing.xl)
        }
    }
}
