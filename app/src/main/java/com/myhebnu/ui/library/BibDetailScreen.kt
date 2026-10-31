package com.myhebnu.ui.library

import android.app.Activity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myhebnu.ui.components.ContainerTransformBoundsTransform
import com.myhebnu.ui.components.sharedCardPageModifier
import com.myhebnu.ui.design.GroupRow
import com.myhebnu.ui.design.GroupedSurface
import com.myhebnu.ui.design.Spacing
import com.myhebnu.ui.design.StateBlock

/** 头图的最大高度占屏高比例。实际高度跟着封面比例走，这只是上限。 */
private const val HeaderRatio = 0.55f

/** 标题块起点（相对头图最大高度）：让书名落在头图下缘、渐变较深的一段。 */
private const val TitleAnchor = 0.62f

/** 视差系数：背景只跟随 45% 的滚动量，与内容形成纵深差。 */
private const val ParallaxFactor = 0.45f

/**
 * 模糊开始的提前量：书名顶部距屏幕顶端还剩这么多时开始过渡。
 *
 * 180dp 约等于 28~30mm（取决于屏幕像素密度），即「书名离顶端三厘米左右」。
 */
private val BlurLead = 180.dp

/** 过渡完成点：书名顶部距屏幕顶端还剩这么多时，背景已经是纯色。 */
private val BlurTail = 90.dp

/** 头图解码尺寸相对显示尺寸的缩放：背景大半被渐变吃掉，不需要 1:1 解码。 */
private const val CoverDecodeScale = 0.8f

/** 编目字段折叠时展示的条数。 */
private const val CollapsedMarcCount = 5

/**
 * 书目详情。
 *
 * 版式遵循两个原则：
 * 1. **杂志式留白**——首屏只有书籍身份与馆藏，靠留白分层，不用标题切开页面；
 * 2. **渐进式披露**——简介与出版信息全部推到首屏之外，下拉才出现。
 *
 * 本页不使用 [com.myhebnu.ui.design.ScreenScaffold]：封面要铺到状态栏下面，
 * 而 Scaffold 的 topBar 会占掉头部空间，所以顶栏改成浮在内容上的独立组件。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BibDetailScreen(
    bibId: String,
    isbn: String?,
    fallbackTitle: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    fallbackCover: String? = null,
    sharedScope: SharedTransitionScope? = null,
    animatedScope: AnimatedVisibilityScope? = null,
    viewModel: BibDetailViewModel = hiltViewModel()
) {
    val rawState by viewModel.uiState.collectAsStateWithLifecycle()

    // ViewModel 是 Activity 级的同一个实例，换书时它的状态会晚一帧更新。
    // 这一帧里若直接用旧状态，封面/编者信息都会串成上一本的
    // （更糟的是封面请求会拿「上一本的 URL + 这一本的 bibId」去缓存）。
    // 所以这里按「状态属于当前这本书」做一次闸门。
    //
    // 闸门里的封面不是空的：列表卡片点进来时把那张图的地址带了过来，
    // 缺了才退回按 ISBN 拼的图床地址。容器变换过程中页面因此始终有图，
    // 不会等详情接口回来才把封面补上。
    val immediateCover = fallbackCover.orEmpty().ifBlank { LibraryFormat.coverUrl(isbn) }
    val uiState = if (rawState.bibId == bibId) {
        rawState
    } else {
        BibDetailUiState(
            bibId = bibId,
            isLoading = true,
            title = fallbackTitle.orEmpty(),
            coverUrl = immediateCover
        )
    }
    val isWished by viewModel.isWished.collectAsStateWithLifecycle()

    LaunchedEffect(bibId) {
        viewModel.load(
            bibId = bibId,
            isbn = isbn,
            fallbackTitle = fallbackTitle,
            initialCover = immediateCover
        )
    }

    val pageTransform = sharedCardPageModifier(
        key = "search-card-$bibId",
        sharedScope = sharedScope,
        animatedScope = animatedScope,
        boundsTransform = ContainerTransformBoundsTransform
    )

    // uiState 已经在上面按 bibId 做过闸门，这里拿到的就是当前这本书的数据
    val coverUrl = uiState.coverUrl

    var showTitleDialog by rememberSaveable(bibId) { mutableStateOf(false) }
    var holdingsExpanded by rememberSaveable(bibId) { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current

    // 状态栏图标：进页面用浅色（压在封面上），滚到纯背景色之后换回深色。
    val view = LocalView.current
    val context = LocalContext.current
    val insetsController = remember(view) {
        (context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
    }
    DisposableEffect(view) {
        val previous = insetsController?.isAppearanceLightStatusBars
        onDispose {
            if (previous != null) insetsController?.isAppearanceLightStatusBars = previous
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .then(pageTransform)
            // 不透明底色：容器变换期间整页在放大，没有底色就会露出 Activity 的 window 背景，
            // 观感是一段「透明矩形在扩张」。
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        val headerMaxHeight = screenHeight * HeaderRatio
        val titleTop = headerMaxHeight * TitleAnchor
        val titleMaxWidth = screenWidth - Spacing.xxl * 2
        val headerPx = with(density) { headerMaxHeight.toPx() }

        /** 首屏已滚动像素：整页一次布局，直接读滚动量即可。 */
        val scrolled = remember {
            derivedStateOf { scrollState.value.toFloat() }
        }

        /**
         * 模糊与淡出进度：书名顶部距屏幕顶端还剩 [BlurLead] 时开始，
         * 还剩 [BlurTail] 时就已完成——此时书名仍在屏幕里，但背景已经是纯色。
         *
         * 终点不等到书名完全离开，是因为封面本身印着书名：背景滞后于内容（视差 45%），
         * 若让封面拖到最后一刻，封面上印的名字会和页面标题在同一屏里叠成双重标题。
         * 早点收掉，画面才干净。
         *
         * 进度只在绘制阶段读取（`graphicsLayer` / `drawWithContent` 的 lambda），
         * 滚动时只让图层失效，不触发重组。
         */
        val blurStartPx = with(density) { (titleTop - BlurLead).toPx() }.coerceAtLeast(0f)
        val blurEndPx = with(density) { (titleTop - BlurTail).toPx() }.coerceAtLeast(blurStartPx + 1f)
        val depthProgress = remember(blurStartPx, blurEndPx) {
            derivedStateOf {
                ((scrolled.value - blurStartPx) / (blurEndPx - blurStartPx)).coerceIn(0f, 1f)
            }
        }

        /**
         * 状态栏图标颜色。
         *
         * 只有「封面已经盖住顶部、且还没滚动淡出」时才用浅色；
         * 骨架屏阶段或滚到底之后背景都是浅色，必须用深色图标。
         */
        val darkStatusIcons by remember(headerPx) {
            derivedStateOf {
                uiState.coverUrl.isBlank() || depthProgress.value > 0.65f
            }
        }
        LaunchedEffect(darkStatusIcons) {
            insetsController?.isAppearanceLightStatusBars = darkStatusIcons
        }

        // 模糊层只在真的开始过渡后才参与绘制：首次进入时省掉一帧 24dp 高斯模糊
        val blurVisible by remember(headerPx) {
            derivedStateOf { depthProgress.value > 0.02f }
        }

        /**
         * 整页（头图 + 题名 + 作者 + 想看 + 馆藏 + 简介 + 返回按钮）都挂在这个共享节点里，
         * 由容器统一缩放进场：第 0 帧就是「整页被装进卡片矩形」，随后按 220ms 长到满屏，
         * 所有元素同一条曲线、同一个缩放比例，一起到、一起落位。
         *
         * 内容不再分帧露头的关键在下面：正文用 `Column + verticalScroll` 而不是
         * `LazyColumn`——懒列表的视口只有「当前矩形」那么大，矩形没长过去的条目不会被
         * 组合和绘制，于是靠上的题名/作者/想看先出现、靠下的馆藏要等矩形长过去才冒头。
         * 换成一次性布局的滚动列之后，整页从第一帧就存在，只是被整体缩小了。
         */
        BookBackdrop(
            coverUrl = uiState.coverUrl,
            bibId = bibId,
            headerHeight = headerMaxHeight,
            // 背景会被渐变吃掉大半、还叠着一层模糊，不需要按 1:1 解码。
            // 取 0.8 倍：视觉几乎没有差别，解码耗时和内存都降三成。
            imageSizePx = with(density) {
                (screenWidth * CoverDecodeScale).roundToPx() to
                    (headerMaxHeight * CoverDecodeScale).roundToPx()
            },
            blurVisible = blurVisible,
            progress = { depthProgress.value },
            modifier = Modifier
                .align(Alignment.TopCenter)
                // 背景只跟 45% 的滚动量：内容全速上移、封面慢半拍，产生纵深。
                .graphicsLayer {
                    translationY = -scrolled.value * ParallaxFactor
                }
        )

        when {
            // 不做骨架屏：封面已经通过预取/占位缓存先出来了，
            // 再插一段「骨架 → 内容」的替换反而多一次跳变。
            // 加载期间直接按已有数据渲染（标题用路由带过来的），数据到了原地补齐。
            uiState.error != null && uiState.title.isBlank() -> StateBlock(
                title = uiState.error.orEmpty(),
                description = "书目详情需要联网获取。",
                actionLabel = "重试",
                onAction = viewModel::retry,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = Spacing.screen)
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = bottomInset)
            ) {
                Spacer(Modifier.height(titleTop))

                BookTitleBlock(
                    title = uiState.title,
                    author = uiState.author,
                    wished = isWished,
                    // 书名还没到手时不写库：否则会落一条没有标题的空记录
                    wishEnabled = uiState.title.isNotBlank(),
                    onToggleWish = viewModel::toggleWish,
                    onTitleClick = { showTitleDialog = true },
                    titleMaxWidth = titleMaxWidth
                )

                // 按钮下方给一个呼吸间距后就接馆藏——这一块是首屏的第二主角，
                // 离得太远会让上面的留白显得空。
                Spacer(Modifier.height(Spacing.xxl))

                HoldingCard(
                    holdings = uiState.holdings,
                    expanded = holdingsExpanded,
                    onToggleExpand = { holdingsExpanded = !holdingsExpanded },
                    modifier = Modifier.padding(horizontal = Spacing.screen)
                )

                Spacer(Modifier.height(Spacing.xxl))

                if (uiState.abstract.isNotBlank()) {
                    AbstractSection(
                        text = uiState.abstract,
                        modifier = Modifier.padding(horizontal = Spacing.screen)
                    )
                    Spacer(Modifier.height(Spacing.xxl))
                }

                if (hasPublishInfo(uiState)) {
                    PublishInfoSection(
                        publisher = uiState.publisher,
                        pubYear = uiState.pubYear,
                        isbn = uiState.isbn,
                        marcFields = uiState.marcFields,
                        modifier = Modifier.padding(horizontal = Spacing.screen)
                    )
                    Spacer(Modifier.height(Spacing.xxl))
                }

                Text(
                    text = buildString {
                        append("数据来自图书馆汇文系统")
                        if (uiState.clickCount > 0) {
                            append(" · 已被查看 ${uiState.clickCount} 次")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.xl)
                )
                Spacer(Modifier.height(Spacing.xxxl))
            }
        }

        DetailTopBar(
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }

    if (showTitleDialog) {
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            title = { Text("完整书名") },
            text = { Text(uiState.title) },
            confirmButton = {
                TextButton(onClick = { showTitleDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }
}

private fun hasPublishInfo(state: BibDetailUiState): Boolean =
    state.publisher.isNotBlank() ||
        state.pubYear.isNotBlank() ||
        state.isbn.isNotBlank() ||
        state.marcFields.isNotEmpty()

/**
 * 内容简介。
 *
 * 不再有「内容简介」小标题：靠留白与字体层级区分即可，
 * 每块都顶一个小标题会把页面切成表格。
 */
@Composable
private fun AbstractSection(
    text: String,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
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
 * 出版信息。
 *
 * 前三条是书籍身份（出版者 / 年份 / ISBN），后面接编目字段。
 * `出版发行项` 与 `载体形态项` 已在 ViewModel 里剔除——它们与前三项重复。
 */
@Composable
private fun PublishInfoSection(
    publisher: String,
    pubYear: String,
    isbn: String,
    marcFields: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val visible = if (expanded) marcFields else marcFields.take(CollapsedMarcCount)

    GroupedSurface(modifier = modifier) {
        var rowIndex = 0
        val fixedRows = listOfNotNull(
            publisher.takeIf { it.isNotBlank() }?.let { "出版者" to it },
            pubYear.takeIf { it.isNotBlank() }?.let { "出版年份" to it },
            isbn.takeIf { it.isNotBlank() }?.let { "ISBN" to it }
        )
        // 末尾那一行（显示全部 / 收起）也要计入总行数，否则最后一条编目字段会漏掉分隔线
        val hasTrailingRow = marcFields.size > visible.size ||
            (expanded && marcFields.size > CollapsedMarcCount)
        val totalRows = fixedRows.size + visible.size + if (hasTrailingRow) 1 else 0

        fixedRows.forEach { (label, value) ->
            rowIndex += 1
            GroupRow(
                title = label,
                trailingText = value,
                showDivider = rowIndex != totalRows
            )
        }

        visible.forEach { (label, value) ->
            rowIndex += 1
            GroupRow(
                title = label,
                trailingText = value,
                showDivider = rowIndex != totalRows
            )
        }

        if (marcFields.size > visible.size) {
            GroupRow(
                title = "显示全部 ${marcFields.size} 项",
                titleColor = MaterialTheme.colorScheme.primary,
                onClick = { expanded = true }
            )
        } else if (expanded && marcFields.size > CollapsedMarcCount) {
            GroupRow(
                title = "收起",
                titleColor = MaterialTheme.colorScheme.primary,
                onClick = { expanded = false }
            )
        }
    }
}
