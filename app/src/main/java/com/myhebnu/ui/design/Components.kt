package com.myhebnu.ui.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myhebnu.ui.theme.LocalSemanticColors

// ============================================================================
// 设计系统 · 通用组件
//
// 这些组件构成应用的视觉语法，图书馆模块之外的页面也应逐步改用它们，
// 以保证「同一套设计系统」而不是「风格各异的独立页面」。
//
// 三条硬规则：
// 1. 颜色只从 MaterialTheme.colorScheme 取，不写色值；形状只从 shapes 取。
// 2. 间距只从 Spacing 取，不写字面量。
// 3. 可点击元素的可触控高度不低于 MinTouchTarget。
// ============================================================================

/**
 * 页面骨架：极简顶栏（返回 + 行内标题）+ 内容区。
 *
 * 顶栏不加阴影、不与内容做色差，只承担「我在哪」和「怎么退回去」两件事，
 * 让页面本身成为视觉主体。
 *
 * @param title 行内标题。模块首页传空串并配合 [LargeTitleHeader] 使用——此时顶栏整条不渲染
 *              （只留系统栏内边距），避免出现「顶栏一个返回箭头 + 大标题又一个返回箭头」。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    // 空顶栏不渲染：Scaffold 在 topBar 为空时会把状态栏高度算进 contentPadding，
    // 内容依旧从状态栏下方开始。
    val hasTopBar = title.isNotEmpty() || onBack != null

    Scaffold(
        modifier = modifier,
        topBar = {
            if (hasTopBar) {
                TopAppBar(
                    title = {
                        if (title.isNotEmpty()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = floatingActionButton,
        containerColor = MaterialTheme.colorScheme.background,
        content = content
    )
}

/**
 * 大标题页头：用于模块首页。
 *
 * 与行内标题的分工：大标题只出现在「用户刚进入某个模块」的位置，随内容一起滚走；
 * 二级页面一律用 [ScreenScaffold] 的行内标题，避免每页都顶着巨大的标题。
 */
@Composable
fun LargeTitleHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (onBack != null) {
            // 负偏移让返回图标的视觉左缘与下方标题对齐（图标自带内边距）
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .offset(x = (-12).dp)
                    .padding(start = Spacing.xs)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(Spacing.xs))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (trailing != null) {
                Spacer(Modifier.width(Spacing.sm))
                trailing()
            }
        }
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 分组标题：比正文更小的字号 + 弱化颜色 + 少量缩进。
 *
 * 用排版而非容器来表达层级——这是本应用替代「一张卡片包一个标题」的主要手段。
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Spacing.xs, end = Spacing.xs, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 分组容器：只在确实需要「一组同类内容」时使用。
 *
 * 浅表面色 + 大圆角 + 内部发丝分隔线，替代「每个条目一张卡片」的堆叠。
 * 不在容器上加阴影——层级由颜色深浅表达，而不是投影。
 */
@Composable
fun GroupedSurface(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/**
 * 分组内的单行。
 *
 * 三种形态自动成立：纯展示（无 onClick，无右侧箭头）、可进入（有 onClick，右侧细箭头）、
 * 带状态（右侧传入 [trailingContent]，例如 [StatusPill]）。
 */
@Composable
fun GroupRow(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
    trailingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = false,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = MinTouchTarget)
                .padding(
                    start = if (leadingIcon != null) Spacing.lg else Spacing.xl,
                    end = Spacing.lg,
                    top = Spacing.md,
                    bottom = Spacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(Spacing.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!supporting.isNullOrBlank()) {
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailingContent != null) {
                Spacer(Modifier.width(Spacing.sm))
                trailingContent()
            } else if (trailingText != null) {
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onClick != null) {
                Spacer(Modifier.width(Spacing.xs))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (showDivider) {
            SectionDivider(
                startInset = if (leadingIcon != null) {
                    Spacing.lg + 22.dp + Spacing.md
                } else {
                    Spacing.xl
                }
            )
        }
    }
}

/** 发丝分隔线，左侧内缩以与文字起始位置对齐。 */
@Composable
fun SectionDivider(
    modifier: Modifier = Modifier,
    startInset: androidx.compose.ui.unit.Dp = DividerSpec.inset
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startInset),
        thickness = DividerSpec.thickness,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

/** 语义色调。状态只用这四种，避免给每种状态发明一个新颜色。 */
enum class Tone { Neutral, Positive, Warning, Critical }

/**
 * 状态标签：小圆角（8dp）而非胶囊形，避免所有元素都长成圆滚滚一片。
 *
 * 容器色优先用 MD3 的语义容器色（tertiary/error container），
 * 只有「通过类」的 Positive 走项目自带的成功色（[LocalSemanticColors]）——
 * 琥珀色调色板里没有绿色角色，`secondaryContainer` 与 `Warning` 的
 * `tertiaryContainer` 视觉上分不开，分级就读不出来。
 */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Neutral
) {
    val (container, content) = when (tone) {
        Tone.Neutral -> MaterialTheme.colorScheme.surfaceVariant to
                MaterialTheme.colorScheme.onSurfaceVariant
        Tone.Positive -> LocalSemanticColors.current.let {
            it.successContainer to it.onSuccessContainer
        }
        Tone.Warning -> MaterialTheme.colorScheme.tertiaryContainer to
                MaterialTheme.colorScheme.onTertiaryContainer
        Tone.Critical -> MaterialTheme.colorScheme.errorContainer to
                MaterialTheme.colorScheme.onErrorContainer
    }
    Box(
        modifier = modifier
            .background(color = container, shape = MaterialTheme.shapes.small)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1
        )
    }
}

/** 指标条目的数据形状。 */
data class MetricItem(
    val value: String,
    val label: String,
    val tone: Tone = Tone.Neutral,
    val onClick: (() -> Unit)? = null
)

/**
 * 指标条：一行并列数字 + 说明。
 *
 * 不使用卡片包裹——数字本身就是内容，容器只会削弱它。组内等分、左对齐，
 * 只有需要警示的数值（临期、欠费）才染色。
 */
@Composable
fun MetricStrip(
    items: List<MetricItem>,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        items.forEach { item ->
            val valueColor = when (item.tone) {
                Tone.Neutral -> MaterialTheme.colorScheme.onSurface
                Tone.Positive -> MaterialTheme.colorScheme.primary
                Tone.Warning -> MaterialTheme.colorScheme.tertiary
                Tone.Critical -> MaterialTheme.colorScheme.error
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (item.onClick != null) Modifier.clickable(onClick = item.onClick)
                        else Modifier
                    )
                    .padding(vertical = Spacing.xs),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 空状态 / 错误状态 / 无结果的统一样式。
 *
 * 每页自行拼 `Column + Text + Button` 会迅速走形；所有「没有内容」的时刻都走这里。
 */
@Composable
fun StateBlock(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(Spacing.lg))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
            textAlign = TextAlign.Center
        )
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.lg))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * 骨架屏色块：加载中占位的形状。
 *
 * 为什么不用进度圈：进度圈是「转起来才有内容」的暗示，页面已经有骨架时它反而
 * 和共享元素转场抢注意力——一圈一圈重绘的进度指示器和卡片展开动画同帧竞争，
 * 在低端机上就是肉眼可见的掉帧。骨架块只做一次很淡的高光扫过，
 * 既说明「在加载」，又让页面结构一开始就定型、数据到达时不会整页跳一下。
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeleton-shimmer"
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                // 一次高光从左扫到右：起点完全在左侧之外，终点完全移出右侧
                val band = size.width
                val sweepLeft = -band + progress * (size.width + band)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(base, highlight, base),
                        start = Offset(sweepLeft, 0f),
                        end = Offset(sweepLeft + band, size.height)
                    )
                )
            }
    )
}

/** 首次加载的统一样式：居中细进度圈，不遮挡页面结构性内容。 */
@Composable
fun LoadingBlock(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxxl),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 行内提示：用于「已有内容，但这次操作失败了」这类不该整页替换的错误。
 *
 * 与 Toast 的分工：需要用户看见并可能重试的错误用这里，一次性成功提示才用 Toast。
 */
@Composable
fun InlineNotice(
    text: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Critical,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val content = when (tone) {
        Tone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        Tone.Positive -> MaterialTheme.colorScheme.primary
        Tone.Warning -> MaterialTheme.colorScheme.tertiary
        Tone.Critical -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = content,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
