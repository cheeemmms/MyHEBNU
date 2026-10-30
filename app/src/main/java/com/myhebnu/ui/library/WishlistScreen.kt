package com.myhebnu.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.myhebnu.data.local.db.entity.WishlistEntity
import com.myhebnu.ui.design.GroupedSurface
import com.myhebnu.ui.design.ScreenScaffold
import com.myhebnu.ui.design.SectionDivider
import com.myhebnu.ui.design.Spacing
import com.myhebnu.ui.design.StateBlock

/** 封面缩略尺寸，按书封常见的 1:1.4 取。 */
private val CoverWidth = 44.dp
private val CoverHeight = 62.dp

/**
 * 我想看的书。
 *
 * 数据全部来自本地数据库，断网也能完整展示——这是「存书籍快照而不是只存 bibId」
 * 换来的效果。封面会额外联网补一次（更清晰），失败就用快照里的那张。
 */
@Composable
fun WishlistScreen(
    onBack: () -> Unit,
    onOpenBib: (bibId: String, isbn: String, title: String) -> Unit,
    onGoSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WishlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = "我想看的书",
        modifier = modifier,
        onBack = onBack
    ) { padding ->
        if (uiState.items.isEmpty()) {
            StateBlock(
                title = "还没有想看的书",
                description = "在馆藏检索里打开一本书，点「想看」就能加进来。",
                actionLabel = "去检索",
                onAction = onGoSearch,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
            return@ScreenScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Spacer(Modifier.height(Spacing.md))
                GroupedSurface(modifier = Modifier.padding(horizontal = Spacing.screen)) {
                    val lastIndex = uiState.items.lastIndex
                    uiState.items.forEachIndexed { index, item ->
                        WishlistRow(
                            item = item,
                            showDivider = index != lastIndex,
                            onClick = {
                                onOpenBib(
                                    item.entry.bibId,
                                    item.entry.isbn,
                                    item.entry.title
                                )
                            },
                            onRemove = { viewModel.remove(item.entry.bibId) }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(Spacing.xxl))
                Text(
                    text = "想看列表保存在本机，不随图书馆账号同步。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.xl)
                )
                Spacer(Modifier.height(Spacing.xxxl))
            }
        }
    }
}

@Composable
private fun WishlistRow(
    item: WishlistItemUi,
    showDivider: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val entry: WishlistEntity = item.entry

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    start = Spacing.lg,
                    end = Spacing.sm,
                    top = Spacing.md,
                    bottom = Spacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(CoverWidth)
                    .height(CoverHeight)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (item.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title.ifBlank { "无题名" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = listOfNotNull(
                    entry.author.takeIf { it.isNotBlank() },
                    entry.pubYear.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "从想看移除",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showDivider) SectionDivider()
    }
}
