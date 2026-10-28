package com.myhebnu.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.myhebnu.ui.components.QuickCardBoundsTransform
import com.myhebnu.ui.components.sharedCardPageModifier
import com.myhebnu.ui.design.GroupRow
import com.myhebnu.ui.design.GroupedSurface
import com.myhebnu.ui.design.InlineNotice
import com.myhebnu.ui.design.LoadingBlock
import com.myhebnu.ui.design.ScreenScaffold
import com.myhebnu.ui.design.SectionHeader
import com.myhebnu.ui.design.Spacing
import com.myhebnu.ui.design.StateBlock

/**
 * 馆藏检索。
 *
 * 四个区域：搜索框常驻、联想词（输入中）、结果列表、以及「还没搜」时的发现区（历史 / 热门 / 新书）。
 * 结果行克制到只留封面、题名、责任者、出版信息，以及右侧可借册数——
 * 详情都收进 [BibDetailScreen]，列表不承载决策。
 *
 * 退出本页时清空检索状态：检索是一次性动作，不该在下次进来时冒充「我的当前状态」。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenBib: (bibId: String, isbn: String?, title: String?) -> Unit,
    modifier: Modifier = Modifier,
    sharedScope: SharedTransitionScope? = null,
    animatedScope: AnimatedVisibilityScope? = null,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    // 有检索态（输入过 / 搜过）时，返回先退回到「搜索框」，再返回才离开本页；
    // 这样从书目详情回来能先看到结果列表，而不是一步跳回图书馆首页。
    val hasSearchState = uiState.query.isNotBlank() ||
        uiState.results.isNotEmpty() ||
        uiState.lastSearched.isNotBlank()

    val handleBack: () -> Unit = {
        keyboardController?.hide()
        if (hasSearchState) {
            viewModel.reset()
        } else {
            onBack()
        }
    }

    BackHandler(enabled = hasSearchState) { handleBack() }

    ScreenScaffold(
        title = "馆藏检索",
        modifier = modifier,
        onBack = handleBack
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            SearchField(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = {
                    keyboardController?.hide()
                    viewModel.search()
                },
                onClear = viewModel::clearQuery
            )

            when {
                uiState.isSearching && uiState.results.isEmpty() -> LoadingBlock()

                uiState.isTyping && uiState.suggestions.isNotEmpty() -> SuggestionList(
                    suggestions = uiState.suggestions,
                    onPick = {
                        keyboardController?.hide()
                        viewModel.searchWith(it)
                    }
                )

                uiState.error != null && uiState.results.isEmpty() -> StateBlock(
                    title = uiState.error.orEmpty(),
                    description = "检索服务需要联网，稍后再试。",
                    actionLabel = "重试",
                    onAction = { viewModel.searchWith(uiState.lastSearched) }
                )

                uiState.isEmptyResult -> StateBlock(
                    title = "没有找到相关图书",
                    description = "试试只输入书名里的几个关键词，或换作者名、ISBN。"
                )

                uiState.results.isEmpty() -> DiscoverySection(
                    history = uiState.history,
                    hotBooks = uiState.hotBooks,
                    newBooks = uiState.newBooks,
                    isLoading = uiState.idleLoading,
                    onPickQuery = {
                        keyboardController?.hide()
                        viewModel.searchWith(it)
                    },
                    onOpenBib = onOpenBib
                )

                else -> ResultList(
                    uiState = uiState,
                    gridState = viewModel.gridState,
                    onScrolledToTop = viewModel::onScrolledToTop,
                    onGoToPage = viewModel::goToPage,
                    onRetry = viewModel::reloadCurrentPage,
                    onOpenBib = onOpenBib,
                    sharedScope = sharedScope,
                    animatedScope = animatedScope
                )
            }
        }
    }
}

/**
 * 结果区：双列交错瀑布流。
 *
 * 每张卡片只承载「封面 + 题名 + 作者 + 出版者」，决策信息（可借册数、馆藏地）留给详情页；
 * 卡片本身挂在共享元素 `search-card-<bibId>` 上，点开后与 [BibDetailScreen] 做容器变换，
 * 与首页卡片进各模块的动画是同一套。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ResultList(
    uiState: SearchUiState,
    gridState: LazyStaggeredGridState,
    onScrolledToTop: () -> Unit,
    onGoToPage: (Int) -> Unit,
    onRetry: () -> Unit,
    onOpenBib: (bibId: String, isbn: String?, title: String?) -> Unit,
    sharedScope: SharedTransitionScope?,
    animatedScope: AnimatedVisibilityScope?
) {
    // 只在「换关键词 / 翻页」这类一次性请求后滚回顶部；从书目详情返回时保留离场位置，
    // 滚动状态本身由 ViewModel 持有（见 SearchViewModel.gridState），页面重建也不会丢。
    LaunchedEffect(uiState.pendingScrollToTop) {
        if (uiState.pendingScrollToTop) {
            gridState.scrollToItem(0)
            onScrolledToTop()
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            bottom = Spacing.xxxl
        ),
        verticalItemSpacing = Spacing.md,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            ResultStatsRow(
                total = uiState.total,
                page = uiState.page,
                totalPage = uiState.totalPage,
                shown = uiState.results.size
            )
        }

        if (uiState.error != null) {
            item(span = StaggeredGridItemSpan.FullLine) {
                InlineNotice(
                    text = uiState.error,
                    actionLabel = "重试",
                    onAction = onRetry
                )
            }
        }

        itemsIndexed(
            items = uiState.results,
            key = { index, item -> item.resultKey(index) }
        ) { index, item ->
            BookCard(
                title = item.title.orEmpty(),
                author = item.author.orEmpty(),
                publisher = item.publisher.orEmpty(),
                pubYear = item.pub_year.orEmpty(),
                cover = coverOf(item, uiState.covers),
                sharedKey = item.resultKey(index),
                onClick = { onOpenBib(item.bibId.orEmpty(), item.isbn, item.title) },
                sharedScope = sharedScope,
                animatedScope = animatedScope
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            ResultPager(
                page = uiState.page,
                totalPage = uiState.totalPage,
                total = uiState.total,
                isLoading = uiState.isLoadingMore,
                onPrev = { onGoToPage(uiState.page - 1) },
                onNext = { onGoToPage(uiState.page + 1) },
            )
        }
    }
}

/** 瀑布流条目的稳定标识：优先 bibId，缺失时退回页内序号。 */
private fun com.myhebnu.data.remote.opac.OpacSearchItem.resultKey(index: Int): String =
    bibId?.takeIf { it.isNotBlank() } ?: "result-$index"

@Composable
private fun ResultStatsRow(
    total: Int,
    page: Int,
    totalPage: Int,
    shown: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xs, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "找到 $total 种",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = if (totalPage > 1) "第 $page / $totalPage 页" else "已显示 $shown",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 封面比例的固定档位（宽 / 高）。
 *
 * 卡片高度必须在图片到达前就定下来：一旦图片加载完成再改比例，交错网格会把它下面
 * 所有卡片重新排一遍，滚动中就会看到抖动。这里按书目标识取一个稳定档位，
 * 图片统一 `ContentScale.Crop` 裁进这个框，滚动期间高度恒定。
 */
private val COVER_RATIOS = floatArrayOf(0.62f, 0.68f, 0.74f, 0.8f)

private fun coverRatioFor(seed: String): Float =
    COVER_RATIOS[(seed.hashCode().ushr(1)) % COVER_RATIOS.size]

/**
 * 单张瀑布流卡片。
 *
 * 题名 16sp 半粗、作者 14sp、出版者与出版年 12sp 单行，靠字号与颜色一起分主次。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun BookCard(
    title: String,
    author: String,
    publisher: String,
    pubYear: String,
    cover: String,
    sharedKey: String,
    onClick: () -> Unit,
    sharedScope: SharedTransitionScope?,
    animatedScope: AnimatedVisibilityScope?
) {
    val cardTransform = sharedCardPageModifier(
        key = "search-card-$sharedKey",
        sharedScope = sharedScope,
        animatedScope = animatedScope,
        boundsTransform = QuickCardBoundsTransform
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(cardTransform)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
    ) {
        BookCover(
            cover = cover,
            title = title,
            seed = sharedKey,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Spacing.md,
                    end = Spacing.md,
                    top = Spacing.sm,
                    bottom = Spacing.md
                )
        ) {
            Text(
                text = title.ifBlank { "无题名" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (author.isNotBlank()) {
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (publisher.isNotBlank() || pubYear.isNotBlank()) {
                Spacer(Modifier.height(Spacing.xxs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (publisher.isNotBlank()) {
                        // 带 weight 的出版者最后测量，空间不够时先牺牲它，年份不受影响
                        Text(
                            text = publisher,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (pubYear.isNotBlank()) {
                        Text(
                            text = if (publisher.isNotBlank()) " · $pubYear" else pubYear,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * 封面图，高度按 [coverRatioFor] 的稳定档位撑开（瀑布流的错落来自档位差异，不来自图片实际尺寸）。
 *
 * 没有封面地址、或者加载失败，都退回同一张占位图：书本图标 + 「暂无封面」，
 * 底色用 `surfaceContainerHighest`，在深浅主题下都不会突兀。
 */
@Composable
private fun BookCover(
    cover: String,
    title: String,
    seed: String,
    modifier: Modifier = Modifier
) {
    var failed by remember(cover) { mutableStateOf(false) }
    val placeholder = cover.isBlank() || failed

    Box(
        modifier = modifier
            .aspectRatio(coverRatioFor(seed))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (placeholder) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "暂无封面",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            AsyncImage(
                model = cover,
                contentDescription = title.takeIf { it.isNotBlank() },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { failed = true }
            )
        }
    }
}

/**
 * 页码翻页控件（搜索引擎式）。
 *
 * 「上滑加载更多」换成整页翻页：每页 20 条、共 [totalPage] 页，
 * 上一页 / 下一页即时切换，切完由调用方把列表滚回顶部。
 */
@Composable
private fun ResultPager(
    page: Int,
    totalPage: Int,
    total: Int,
    isLoading: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    if (total <= 0) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onPrev,
            enabled = !isLoading && page > 1
        ) {
            Text("上一页", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                text = if (totalPage > 1) "$page / $totalPage" else "共 $total 种",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.weight(1f))

        TextButton(
            onClick = onNext,
            enabled = !isLoading && totalPage > 0 && page < totalPage
        ) {
            Text("下一页", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SuggestionList(
    suggestions: List<String>,
    onPick: (String) -> Unit
) {
    LazyColumn {
        item {
            GroupedSurface(
                modifier = Modifier.padding(horizontal = Spacing.screen)
            ) {
                suggestions.forEachIndexed { index, text ->
                    GroupRow(
                        title = text,
                        leadingIcon = Icons.Filled.Search,
                        showDivider = index != suggestions.lastIndex,
                        onClick = { onPick(text) }
                    )
                }
            }
        }
        item { Spacer(Modifier.height(Spacing.xxxl)) }
    }
}

/**
 * 封面优先级：批量封面接口（`search/extend3`）→ 结果内 `thumb` → ISBN 拼汇文图床。
 *
 * 检索结果的 `thumb` 恒为空串，所以绝大多数情况都靠 [covers] 里那份补回来的地址；
 * 批量接口只覆盖馆藏里存在封面的书，缺失时退回按 ISBN 拼的地址，再没有就显示占位。
 */
private fun coverOf(
    item: com.myhebnu.data.remote.opac.OpacSearchItem,
    covers: Map<String, String>
): String {
    item.bibId?.let { bibId -> covers[bibId]?.takeIf { it.isNotBlank() }?.let { return it } }
    val direct = item.thumb.orEmpty()
    if (direct.isNotBlank()) return direct
    return LibraryFormat.coverUrl(item.isbn)
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.screen,
                end = Spacing.screen,
                top = Spacing.sm,
                bottom = Spacing.sm
            ),
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        placeholder = {
            Text(
                text = "书名、作者或 ISBN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "清空",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

@Composable
private fun DiscoverySection(
    history: List<String>,
    hotBooks: List<com.myhebnu.data.remote.opac.OpacHotBook>,
    newBooks: List<com.myhebnu.data.remote.opac.OpacSearchItem>,
    isLoading: Boolean,
    onPickQuery: (String) -> Unit,
    onOpenBib: (bibId: String, isbn: String?, title: String?) -> Unit
) {
    if (isLoading && history.isEmpty() && hotBooks.isEmpty() && newBooks.isEmpty()) {
        LoadingBlock()
        return
    }

    LazyColumn {
        if (history.isNotEmpty()) {
            item {
                Spacer(Modifier.height(Spacing.sm))
                SectionHeader(
                    text = "最近搜索",
                    modifier = Modifier.padding(horizontal = Spacing.screen)
                )
                GroupedSurface(
                    modifier = Modifier.padding(horizontal = Spacing.screen)
                ) {
                    history.forEachIndexed { index, query ->
                        GroupRow(
                            title = query,
                            showDivider = index != history.lastIndex,
                            onClick = { onPickQuery(query) }
                        )
                    }
                }
            }
        }

        if (hotBooks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(Spacing.sectionGap))
                SectionHeader(
                    text = "热门借阅",
                    modifier = Modifier.padding(horizontal = Spacing.screen)
                )
                GroupedSurface(
                    modifier = Modifier.padding(horizontal = Spacing.screen)
                ) {
                    hotBooks.forEachIndexed { index, book ->
                        GroupRow(
                            title = book.title.orEmpty(),
                            supporting = book.author.orEmpty(),
                            showDivider = index != hotBooks.lastIndex,
                            onClick = {
                                onOpenBib(book.bibId.orEmpty(), book.isbn, book.title)
                            }
                        )
                    }
                }
            }
        }

        if (newBooks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(Spacing.sectionGap))
                SectionHeader(
                    text = "新书上架",
                    modifier = Modifier.padding(horizontal = Spacing.screen)
                )
                GroupedSurface(
                    modifier = Modifier.padding(horizontal = Spacing.screen)
                ) {
                    newBooks.forEachIndexed { index, item ->
                        GroupRow(
                            title = item.title.orEmpty(),
                            supporting = listOfNotNull(
                                item.author?.takeIf { it.isNotBlank() },
                                item.publisher?.takeIf { it.isNotBlank() }
                            ).joinToString(" · "),
                            showDivider = index != newBooks.lastIndex,
                            onClick = {
                                onOpenBib(item.bibId.orEmpty(), item.isbn, item.title)
                            }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(Spacing.xxxl)) }
    }
}
