package com.myhebnu.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.myhebnu.ui.design.Spacing

/** 书名的最大行数：超过两行会把首屏的馆藏卡挤走。 */
private const val MaxTitleLines = 2

/** 自适应字号的候选档位，从大到小。范围刻意收窄——字号跳变太明显会显得廉价。 */
private val TitleFontSizes = listOf(32.sp, 30.sp, 28.sp, 26.sp)

/** 标题块总高：书名两行 + 作者 + 间距 + 胶囊按钮。固定值，供首屏计算留白。 */
internal val TitleBlockHeight = 160.dp

/** 书名区域高度（两行的容量），单行时底部对齐，保证作者与按钮的位置不跳动。 */
internal val TitleAreaHeight = 76.dp

/** 顶栏浮动按钮的底色。压在图上的元素需要自己的背景才可辨，深浅主题都用深色半透明。 */
private val TopBarScrim = Color.Black.copy(alpha = 0.32f)

/** 模糊半径。固定值，只改透明度——动态改半径会每次重建离屏缓冲。 */
private val BlurRadius = 24.dp

/** 未滚动时渐变开始的位置（容器高度的比例）。 */
private const val ScrimEnd = 0.70f

/**
 * 顶部背景：清晰封面 + 模糊封面 + 渐变压暗，三层叠加。
 *
 * 高度**跟着图片走**而不是固定成屏高的比例：`FillWidth` 之后图片高度取决于封面自身比例
 * （方形封面只占容器的一半，竖长封面会超出）。如果渐变按固定容器高度铺，方形封面的下边缘
 * 会落在渐变还很浅的位置，出现一条「图片突然被切断」的硬边。
 *
 * 模糊半径是固定的（24dp），滚动只改透明度——动态改半径每次都要重建离屏缓冲，
 * 滚动时会明显掉帧。这是本效果能跑顺的关键。
 *
 * @param progress 滚动加深进度 0..1，必须只在绘制阶段读取（不要在组合期读，否则每帧重组）
 */
@Composable
fun BookBackdrop(
    coverUrl: String,
    maxHeaderHeight: Dp,
    progress: () -> Float,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val hasCover = coverUrl.isNotBlank()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (hasCover) {
                    Modifier.heightIn(max = maxHeaderHeight)
                } else {
                    // 没有封面时保留一块底色，避免头部塌成 0 高
                    Modifier.height(maxHeaderHeight)
                }
            )
            .clipToBounds()
            // 有封面时给一块底色垫底，避免图片加载过程中露出透明；
            // 没有封面时直接用页面背景色——否则会变成一条突兀的深灰渐变带
            .background(
                if (hasCover) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.background
            )
    ) {
        if (hasCover) {
            // 第一层：清晰封面。宽度优先，左右一定完整（原图自带白边的另说）。
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )

            // 第二层：模糊版。**整幅**一起模糊，不做「上清下糊」——
            // 封面本身印着书名，若顶部一直保持清晰，它就会和页面标题叠在一起变成双重标题。
            // 透明度跟着滚动进度走，只在绘制阶段读 progress()。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .graphicsLayer { alpha = progress() }
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .blur(BlurRadius, BlurredEdgeTreatment.Rectangle)
                )
            }
        }

        // 第三层：静止时的底部柔化。固定不动，只为了让书名压在图上时不糊在一起。
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            ScrimEnd to Color.Transparent,
                            1f to backgroundColor
                        )
                    )
                }
        )

        // 第四层：滚动时整幅淡向背景色。全部一起变，而不是从下往上追，
        // 进度到 1 时整块就是纯背景色——读者不会再看到任何封面残留。
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    drawContent()
                    val p = progress()
                    if (p > 0.001f) {
                        drawRect(backgroundColor.copy(alpha = p))
                    }
                }
        )
    }
}

/**
 * 居中的书名 / 作者 / 想看胶囊。
 *
 * 高度是固定的 [TitleBlockHeight]，这样首屏「卡片贴底」的留白可以精确算出，
 * 不同书名长度不会让整页版式上下浮动。
 */
@Composable
fun BookTitleBlock(
    title: String,
    author: String,
    wished: Boolean,
    wishEnabled: Boolean,
    onToggleWish: () -> Unit,
    onTitleClick: (() -> Unit)?,
    titleMaxWidth: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(TitleBlockHeight)
            .padding(horizontal = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val fitted = rememberFittedTitle(title, titleMaxWidth)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TitleAreaHeight)
                // 点击区做成整个书名区，而不只是文字本身：单行书名时文字只占一半高度，
                // 只给文字加点击会让上半部分「点了没反应」。
                .then(
                    if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = title.ifBlank { "无题名" },
                style = fitted.style,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = MaxTitleLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        // 作者为空也占位，避免按钮忽上忽下
        Text(
            text = author,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(20.dp)
        )

        Spacer(Modifier.height(Spacing.lg))

        WishPill(
            wished = wished,
            enabled = wishEnabled,
            onClick = onToggleWish
        )
    }
}

/** 定宽胶囊：状态靠「实心/描边 + 文案」两处同时变化，比只换图标更容易一眼看清。 */
@Composable
private fun WishPill(
    wished: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val container = if (wished) MaterialTheme.colorScheme.primary else Color.Transparent
    val content = if (wished) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val strokeColor = if (wished) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = Modifier
            .width(140.dp)
            .height(40.dp)
            .clip(CircleShape)
            .background(container)
            .border(1.dp, strokeColor, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (wished) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = content.copy(alpha = if (enabled) 1f else 0.38f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = if (wished) "已想看" else "想看",
            style = MaterialTheme.typography.labelLarge,
            color = content.copy(alpha = if (enabled) 1f else 0.38f)
        )
    }
}

/**
 * 浮动返回键。只有一枚圆形按钮，**不画任何背景条**——
 * 滚动时若让顶栏逐渐变实色，会从无到有「长出一条横带」，打断整页的沉浸感。
 *
 * 按钮自身带半透明深色底，压在封面或正文上都能辨清。
 */
@Composable
fun DetailTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(start = Spacing.lg, top = Spacing.sm, bottom = Spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TopBarScrim)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 书名自适应字号
// ---------------------------------------------------------------------------

private data class FittedTitle(
    val style: TextStyle,
    val truncated: Boolean
)

/**
 * 在 26~32sp 之间挑一个能把书名装进两行的最大字号。
 *
 * Compose 1.7 没有官方的 `Text(autoSize=)`，这里用 [rememberTextMeasurer] 在组合期
 * 一次性测量（结果随 title / 宽度缓存），不会每帧测量。
 *
 * 返回的 `truncated` 表示即使最小字号也放不下——此时界面才允许点书名看全文，
 * 「出现省略号 = 可以点」本身就是提示。
 */
@Composable
private fun rememberFittedTitle(title: String, maxWidth: Dp): FittedTitle {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val base = MaterialTheme.typography.headlineMedium
    val widthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)

    return remember(title, base, widthPx) {
        if (title.isBlank()) {
            return@remember FittedTitle(base.copy(lineHeight = 38.sp), false)
        }

        for (size in TitleFontSizes) {
            val style = base.copy(fontSize = size, lineHeight = (size.value * 1.2f).sp)
            val result = measurer.measure(
                text = AnnotatedString(title),
                style = style,
                maxLines = MaxTitleLines,
                constraints = Constraints(maxWidth = widthPx)
            )
            if (!result.hasVisualOverflow) {
                return@remember FittedTitle(style, false)
            }
        }

        val smallest = TitleFontSizes.last()
        FittedTitle(
            style = base.copy(
                fontSize = smallest,
                lineHeight = (smallest.value * 1.2f).sp
            ),
            truncated = true
        )
    }
}
