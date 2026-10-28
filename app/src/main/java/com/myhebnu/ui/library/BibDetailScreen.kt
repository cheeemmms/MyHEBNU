package com.myhebnu.ui.library

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.myhebnu.data.remote.opac.OpacHolding
import com.myhebnu.ui.components.QuickCardBoundsTransform
import com.myhebnu.ui.components.sharedCardPageModifier
import com.myhebnu.ui.design.GroupRow
import com.myhebnu.ui.design.GroupedSurface
import com.myhebnu.ui.design.LoadingBlock
import com.myhebnu.ui.design.ScreenScaffold
import com.myhebnu.ui.design.SectionHeader
import com.myhebnu.ui.design.Spacing
import com.myhebnu.ui.design.StateBlock
import com.myhebnu.ui.design.StatusPill
import com.myhebnu.ui.design.Tone

/**
 * 书目详情。
 *
 * 结构按读者真正的决策顺序排：这本书是什么（题名/责任者/出版信息）→ 能不能借到（可借册数）
 * → 在哪个馆藏地（按位置分组，附带索书号）→ 内容简介 → 其余 MARC 字段。
 * 本页只读：预约/委托本期不落地，所以不放操作按钮。
 *
 * 从检索结果进来时，整页挂在共享元素 `search-card-<bibId>` 上，
 * 与卡片做容器变换（与首页卡片进各模块同一套动画）；从别的入口进来时没有配对元素，按普通转场处理。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BibDetailScreen(
    bibId: String,
    isbn: String?,
    fallbackTitle: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sharedScope: SharedTransitionScope? = null,
    animatedScope: AnimatedVisibilityScope? = null,
    viewModel: BibDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(bibId) {
        viewModel.load(bibId = bibId, isbn = isbn, fallbackTitle = fallbackTitle)
    }

    val pageTransform = sharedCardPageModifier(
        key = "search-card-$bibId",
        sharedScope = sharedScope,
        animatedScope = animatedScope,
        boundsTransform = QuickCardBoundsTransform
    )

    ScreenScaffold(
        title = "书目详情",
        modifier = modifier.then(pageTransform),
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> LoadingBlock()

                uiState.error != null && uiState.title.isBlank() -> StateBlock(
                    title = uiState.error.orEmpty(),
                    description = "书目详情需要联网获取。",
                    actionLabel = "重试",
                    onAction = {
                        viewModel.load(bibId = bibId, isbn = isbn, fallbackTitle = fallbackTitle)
                    }
                )

                else -> LazyColumn {
                    item {
                        BibHeader(
                            title = uiState.title,
                            author = uiState.author,
                            publisher = uiState.publisher,
                            pubYear = uiState.pubYear,
                            isbn = uiState.isbn,
                            coverUrl = uiState.coverUrl
                        )
                    }

                    item {
                        AvailabilitySummary(
                            available = uiState.availableCount,
                            borrowed = uiState.borrowedCount,
                            reading = uiState.readingCount
                        )
                    }

                    if (uiState.holdings.isNotEmpty()) {
                        item {
                            SectionHeader(
                                text = "馆藏位置",
                                modifier = Modifier.padding(horizontal = Spacing.screen)
                            )
                            GroupedSurface(
                                modifier = Modifier.padding(horizontal = Spacing.screen)
                            ) {
                                val holdings = uiState.holdings
                                holdings.forEachIndexed { index, holding ->
                                    HoldingRow(
                                        holding = holding,
                                        showDivider = index != holdings.lastIndex
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.abstract.isNotBlank()) {
                        item {
                            Spacer(Modifier.height(Spacing.sectionGap))
                            AbstractSection(text = uiState.abstract)
                        }
                    }

                    if (uiState.marcFields.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(Spacing.sectionGap))
                            MarcSection(fields = uiState.marcFields)
                        }
                    }

                    item {
                        Spacer(Modifier.height(Spacing.sectionGap))
                        Text(
                            text = buildString {
                                append("数据来自图书馆汇文系统")
                                if (uiState.clickCount > 0) {
                                    append(" · 已被查看 ${uiState.clickCount} 次")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.xl)
                        )
                        Spacer(Modifier.height(Spacing.xxxl))
                    }
                }
            }
        }
    }
}

@Composable
private fun BibHeader(
    title: String,
    author: String,
    publisher: String,
    pubYear: String,
    isbn: String,
    coverUrl: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.screen,
                end = Spacing.screen,
                top = Spacing.sm,
                bottom = Spacing.lg
            ),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(76.dp)
                .aspectRatio(0.72f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.width(Spacing.lg))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.ifBlank { "无题名" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (author.isNotBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val imprint = listOf(publisher, pubYear)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            if (imprint.isNotBlank()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = imprint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isbn.isNotBlank()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "ISBN $isbn",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AbstractSection(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    SectionHeader(
        text = "内容简介",
        modifier = Modifier.padding(horizontal = Spacing.screen)
    )
    Column(modifier = Modifier.padding(horizontal = Spacing.screen)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis
        )
        if (text.length > 120) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = Spacing.xs)
            ) {
                Text(
                    text = if (expanded) "收起" else "展开全文",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 编目字段区：默认只展示前几条最常看的，其余折起来。
 *
 * 汇文这里会给十几到二十条 MARC 字段，全部铺开会把页面变成一张表格，
 * 而绝大多数读者只关心出版项、载体形态和主题词。
 */
@Composable
private fun MarcSection(fields: List<Pair<String, String>>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val visible = if (expanded) fields else fields.take(COLLAPSED_MARC_COUNT)

    SectionHeader(
        text = "出版信息",
        modifier = Modifier.padding(horizontal = Spacing.screen)
    )
    GroupedSurface(modifier = Modifier.padding(horizontal = Spacing.screen)) {
        visible.forEachIndexed { index, (label, value) ->
            GroupRow(
                title = label,
                trailingText = value,
                showDivider = index != visible.lastIndex || fields.size > visible.size
            )
        }
        if (fields.size > visible.size) {
            GroupRow(
                title = "显示全部 ${fields.size} 项",
                titleColor = MaterialTheme.colorScheme.primary,
                onClick = { expanded = true }
            )
        } else if (expanded && fields.size > COLLAPSED_MARC_COUNT) {
            GroupRow(
                title = "收起",
                titleColor = MaterialTheme.colorScheme.primary,
                onClick = { expanded = false }
            )
        }
    }
}

private const val COLLAPSED_MARC_COUNT = 5

@Composable
private fun AvailabilitySummary(
    available: Int,
    borrowed: Int,
    reading: Int
) {
    val tone = when {
        available > 0 -> Tone.Positive
        borrowed > 0 || reading > 0 -> Tone.Warning
        else -> Tone.Neutral
    }
    val label = when {
        available > 0 -> "可借 $available 册"
        borrowed + reading > 0 -> "已全部借出"
        else -> "暂无馆藏信息"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusPill(text = label, tone = tone)
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = listOfNotNull(
                borrowed.takeIf { it > 0 }?.let { "借出 $it" },
                reading.takeIf { it > 0 }?.let { "阅览 $it" }
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HoldingRow(
    holding: OpacHolding,
    showDivider: Boolean
) {
    val dueDate = holding.dueDate.orEmpty()
    val available = dueDate.isBlank()
    val location = holding.locationName
        ?: holding.location
        ?: holding.library
        ?: "未知馆藏地"

    GroupRow(
        title = location,
        supporting = listOfNotNull(
            holding.callNo?.takeIf { it.isNotBlank() }?.let { "索书号 $it" },
            holding.barCode?.takeIf { it.isNotBlank() }?.let { "条码 $it" },
            dueDate.takeIf { it.isNotBlank() }?.let { "${LibraryFormat.shortDate(it)} 归还" }
        ).joinToString(" · "),
        trailingText = if (available) null else "借出",
        showDivider = showDivider
    )
}
