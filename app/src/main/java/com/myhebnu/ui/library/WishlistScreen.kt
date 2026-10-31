package com.myhebnu.ui.library

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import com.myhebnu.data.local.db.entity.WishlistEntity
import com.myhebnu.ui.components.ContainerTransformBoundsTransform
import com.myhebnu.ui.components.sharedCardPageModifier
import com.myhebnu.ui.design.GroupedSurface
import com.myhebnu.ui.design.ScreenScaffold
import com.myhebnu.ui.design.SectionDivider
import com.myhebnu.ui.design.Spacing
import com.myhebnu.ui.design.StateBlock
import java.time.LocalDate

/** 封面缩略尺寸，按书封常见的 1:1.4 取。 */
private val CoverWidth = 44.dp
private val CoverHeight = 62.dp

/**
 * 备份文件名。
 *
 * 用英文而不是「想看-20261030.json」：部分文件管理器与云盘对中文文件名的处理不稳定，
 * 英文名在各处都省心。
 */
private fun backupFileName(): String {
    val today = LocalDate.now()
    val month = today.monthValue.toString().padStart(2, '0')
    val day = today.dayOfMonth.toString().padStart(2, '0')
    return "myhebnu-wishlist-${today.year}$month$day.json"
}

/**
 * 我想看的书。
 *
 * 数据全部来自本地数据库，断网也能完整展示——这是「存书籍快照而不是只存 bibId」
 * 换来的效果。封面会额外联网补一次（更清晰），失败就用快照里的那张。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WishlistScreen(
    onBack: () -> Unit,
    onOpenBib: (bibId: String, isbn: String, title: String, cover: String) -> Unit,
    onGoSearch: () -> Unit,
    modifier: Modifier = Modifier,
    sharedScope: SharedTransitionScope? = null,
    animatedScope: AnimatedVisibilityScope? = null,
    viewModel: WishlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 待确认移除的条目：行尾爱心是紧挨着「进详情」的按钮，容易误触，
    // 但移除本身不可撤销，所以先弹一次确认。
    var pendingRemoval by remember { mutableStateOf<WishlistItemUi?>(null) }

    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    // 导出走系统「保存到…」，导入走系统文件选择器：都用 SAF，不申请存储权限
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::export) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::import) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 列表一出现就按卡片尺寸把封面预热进内存缓存：滑到哪都不会白一下。
    // 只按缩略图尺寸解码，20 本约 30MB，内存缓存完全吃得下。
    val imageLoader = context.imageLoader
    val density = LocalDensity.current
    LaunchedEffect(uiState.items.size) {
        val origins = uiState.items.associate { it.entry.bibId to it.coverUrl }
        prefetchBookCovers(
            context = context,
            imageLoader = imageLoader,
            bibIds = uiState.items.map { it.entry.bibId },
            urlOf = { bibId -> origins[bibId].orEmpty() },
            widthPx = with(density) { CoverWidth.roundToPx() },
            heightPx = with(density) { CoverHeight.roundToPx() }
        )
    }

    ScreenScaffold(
        title = "我想看的书",
        modifier = modifier,
        onBack = onBack,
        actions = {
            // 菜单放在顶栏而不是列表底部：清空数据后列表是空的，
            // 导入入口不能跟着空态一起消失。
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "更多"
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("导出到文件") },
                        onClick = {
                            menuOpen = false
                            exportLauncher.launch(backupFileName())
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("从文件导入") },
                        onClick = {
                            menuOpen = false
                            // 不限 MIME：部分文件管理器会把 .json 识别成 text/plain 或 octet-stream
                            importLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }
            }
        }
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
                            sharedScope = sharedScope,
                            animatedScope = animatedScope,
                            onClick = {
                                onOpenBib(
                                    item.entry.bibId,
                                    item.entry.isbn,
                                    item.entry.title,
                                    item.coverUrl
                                )
                            },
                            onRemove = { pendingRemoval = item }
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

    pendingRemoval?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("从想看移除") },
            text = { Text("《${target.entry.title.ifBlank { "无题名" }}》将被移出列表。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.remove(target.entry.bibId)
                        pendingRemoval = null
                    }
                ) {
                    Text("好的")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text("算了")
                }
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun WishlistRow(
    item: WishlistItemUi,
    showDivider: Boolean,
    sharedScope: SharedTransitionScope?,
    animatedScope: AnimatedVisibilityScope?,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val entry: WishlistEntity = item.entry

    // 与「检索卡片点开详情」「首页卡片点开目标页」同一套容器变换：整行作为共享节点，
    // 由 sharedBounds 把行矩形连续放大成详情页矩形，行内封面/书名/作者随页面一起被矩形裁切着长开。
    // 缩放比是「目标页宽 ÷ 行宽」，行本身基本满宽，比值≈1.0，所以文字不会被放大，只是随矩形展开。
    val cardTransform = sharedCardPageModifier(
        key = "bib-card-${entry.bibId}",
        sharedScope = sharedScope,
        animatedScope = animatedScope,
        boundsTransform = ContainerTransformBoundsTransform
    )

    Column(modifier = cardTransform) {
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
                        // 与检索卡片、详情页共用同一缓存键
                        model = bookCoverRequest(
                            context = LocalContext.current,
                            url = item.coverUrl,
                            bibId = entry.bibId
                        ),
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
