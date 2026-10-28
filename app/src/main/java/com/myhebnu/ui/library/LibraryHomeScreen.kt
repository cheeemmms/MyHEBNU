package com.myhebnu.ui.library

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myhebnu.ui.components.sharedCardPageModifier
import com.myhebnu.ui.design.GroupRow
import com.myhebnu.ui.design.GroupedSurface
import com.myhebnu.ui.design.InlineNotice
import com.myhebnu.ui.design.MetricItem
import com.myhebnu.ui.design.MetricStrip
import com.myhebnu.ui.design.MinTouchTarget
import com.myhebnu.ui.design.ScreenScaffold
import com.myhebnu.ui.design.SectionDivider
import com.myhebnu.ui.design.SectionHeader
import com.myhebnu.ui.design.SkeletonBox
import com.myhebnu.ui.design.Spacing
import com.myhebnu.ui.design.StateBlock
import com.myhebnu.ui.design.Tone

/**
 * 图书馆首页。
 *
 * 只回答三个问题：我是谁、我欠着什么、我要去哪里。
 * 因此页面结构是「身份 → 数字 → 分组入口 → 足迹」，全部用排版和留白分层，不用卡片堆叠。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LibraryHomeScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    sharedScope: SharedTransitionScope? = null,
    animatedScope: AnimatedVisibilityScope? = null,
    viewModel: LibraryHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 与首页「我的图书馆」卡片共享元素：卡片在原位展开成整页，返回时收回
    val pageModifier = sharedCardPageModifier(
        key = "home-card-library",
        sharedScope = sharedScope,
        animatedScope = animatedScope
    )

    ScreenScaffold(
        title = "我的图书馆",
        modifier = modifier.then(pageModifier),
        onBack = onBack
    ) { padding ->
        val hasContent = uiState.name.isNotBlank() || uiState.loanCount > 0

        when {
            uiState.isLoading && !hasContent -> LibraryHomeSkeleton(
                modifier = Modifier.padding(padding)
            )

            uiState.error != null && !hasContent -> StateBlock(
                title = uiState.error.orEmpty(),
                description = "图书馆系统暂时没有响应，稍后重试即可。",
                actionLabel = "重试",
                onAction = viewModel::refresh,
                modifier = Modifier.padding(padding)
            )

            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screen)
            ) {
                Spacer(Modifier.height(Spacing.sm))

                if (uiState.error != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    InlineNotice(
                        text = "部分数据未能加载：${uiState.error}",
                        actionLabel = "重试",
                        onAction = viewModel::refresh
                    )
                }

                Spacer(Modifier.height(Spacing.xxl))

                MetricStrip(
                    items = listOf(
                        MetricItem(
                            value = uiState.loanCount.toString(),
                            label = "在借",
                            tone = Tone.Neutral,
                            onClick = { onNavigate(LibraryRoutes.LOANS) }
                        ),
                        MetricItem(
                            value = uiState.requestCount.toString(),
                            label = "预约",
                            onClick = { onNavigate(LibraryRoutes.REQUESTS) }
                        ),
                        MetricItem(
                            value = uiState.expireCount.toString(),
                            label = "逾期",
                            tone = if (uiState.expireCount > 0) Tone.Critical else Tone.Neutral
                        ),
                        MetricItem(
                            value = LibraryFormat.money(uiState.fineSum),
                            label = "罚金",
                            tone = if (uiState.fineSum > 0) Tone.Warning else Tone.Neutral,
                            onClick = { onNavigate(LibraryRoutes.FINES) }
                        )
                    )
                )

                QuotaBar(
                    loanCount = uiState.loanCount,
                    loanLimit = uiState.loanLimit,
                    progress = uiState.quotaProgress
                )

                uiState.nextDue?.let { loan ->
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = "最近应还：《${loan.title.orEmpty()}》" +
                            LibraryFormat.shortDate(loan.dueDate) + "，" +
                            LibraryFormat.dueLabel(loan.dueDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (LibraryFormat.dueTone(loan.dueDate)) {
                            LibraryFormat.DueTone.Overdue -> MaterialTheme.colorScheme.error
                            LibraryFormat.DueTone.Soon -> MaterialTheme.colorScheme.tertiary
                            LibraryFormat.DueTone.Normal -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Spacer(Modifier.height(Spacing.xxl))

                SectionHeader(text = "常用")
                GroupedSurface {
                    GroupRow(
                        title = "动态借书码",
                        leadingIcon = Icons.Filled.AccountBox,
                        onClick = { onNavigate(LibraryRoutes.READER_CODE) },
                        showDivider = true
                    )
                    GroupRow(
                        title = "馆藏检索",
                        leadingIcon = Icons.Filled.Search,
                        onClick = { onNavigate(LibraryRoutes.SEARCH) },
                        showDivider = true
                    )
                    GroupRow(
                        title = "当前借阅",
                        leadingIcon = Icons.Filled.List,
                        onClick = { onNavigate(LibraryRoutes.LOANS) },
                        showDivider = true
                    )
                    GroupRow(
                        title = "预约与荐购",
                        leadingIcon = Icons.Filled.Star,
                        onClick = { onNavigate(LibraryRoutes.REQUESTS) },
                        showDivider = true
                    )
                    GroupRow(
                        title = "罚金与违约",
                        leadingIcon = Icons.Filled.Warning,
                        onClick = { onNavigate(LibraryRoutes.FINES) }
                    )
                }

                if (uiState.categories.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.xxl))
                    SectionHeader(text = "借阅偏好", trailing = "近一年")
                    GroupedSurface {
                        uiState.categories.forEachIndexed { index, (category, count) ->
                            GroupRow(
                                title = category,
                                trailingText = "$count 册",
                                showDivider = index != uiState.categories.lastIndex
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xxl))

                Text(
                    text = "数据来自河北师范大学图书馆汇文系统，仅供本人查询。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(Spacing.scrollBottom))
                Spacer(Modifier.height(Spacing.xxxl))
            }
        }
    }
}

/**
 * 借阅额度：一行文字 + 一条 2dp 进度线。
 *
 * 不用进度环或色块——额度是背景信息，只需一眼看出「还剩多少」。
 */
@Composable
private fun QuotaBar(
    loanCount: Int,
    loanLimit: Int,
    progress: Float?
) {
    if (loanLimit <= 0) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "借阅额度",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$loanCount / $loanLimit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        LinearProgressIndicator(
            progress = { progress ?: 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {}
        )
    }
}

/**
 * 图书馆首页的骨架屏。
 *
 * 形状完全照抄真实布局（四个指标、额度线、最近应还、常用入口、借阅偏好），
 * 数据到达时只是把灰块换成文字，位置不移动——这也是页面打开动画不再卡顿的原因：
 * 转场过程中页面已经有稳定结构，不会从「一个居中的圈」突然变成「一整页」。
 */
@Composable
private fun LibraryHomeSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen)
    ) {
        Spacer(Modifier.height(Spacing.xxl))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            repeat(4) { index ->
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(if (index == 3) 0.85f else 0.5f)
                            .height(30.dp)
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .height(12.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SkeletonBox(
                modifier = Modifier
                    .width(64.dp)
                    .height(14.dp)
            )
            SkeletonBox(
                modifier = Modifier
                    .width(48.dp)
                    .height(14.dp)
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        )

        Spacer(Modifier.height(Spacing.md))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(12.dp)
        )
        Spacer(Modifier.height(Spacing.xs))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .height(12.dp)
        )

        Spacer(Modifier.height(Spacing.xxl))
        SectionHeader(text = "常用")
        GroupedSurface {
            repeat(5) { index ->
                SkeletonGroupRow(showDivider = index != 4)
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
        SectionHeader(text = "借阅偏好", trailing = "近一年")
        GroupedSurface {
            repeat(3) { index ->
                SkeletonGroupRow(showDivider = index != 2, withIcon = false)
            }
        }

        Spacer(Modifier.height(Spacing.scrollBottom))
        Spacer(Modifier.height(Spacing.xxxl))
    }
}

/** 与 [GroupRow] 同尺寸的占位行，保证骨架到真内容不跳位。 */
@Composable
private fun SkeletonGroupRow(
    showDivider: Boolean,
    withIcon: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (withIcon) {
                SkeletonBox(modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(Spacing.md))
            }
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.46f)
                    .height(16.dp)
            )
            Spacer(Modifier.weight(1f))
            if (withIcon) {
                SkeletonBox(modifier = Modifier.size(16.dp))
            } else {
                SkeletonBox(
                    modifier = Modifier
                        .width(40.dp)
                        .height(14.dp)
                )
            }
        }
        if (showDivider) SectionDivider()
    }
}
