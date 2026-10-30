package com.myhebnu.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 快一档的尺寸变换：220ms，M3 的 emphasized 曲线（cubic-bezier(0.2, 0, 0, 1)）。
 *
 * 默认的 BoundsTransform 是个偏软的弹簧，瀑布流卡片张开、收回时会显得慢半拍；
 * 需要更利落的页面显式传这个，其它页面继续用默认值，节奏不受影响。
 * 曲线用 M3 的 emphasized 而不是 FastOutSlowIn：前者起步更快，观感更「跟手」。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val QuickCardBoundsTransform: BoundsTransform =
    BoundsTransform { _, _ ->
        tween(durationMillis = 220, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
    }

/**
 * 卡片 → 目标页面 的共享元素修饰符。
 *
 * 每张首页卡片都用 `home-card-<route>` 作为 key；目标页面把它挂到 Scaffold 根上，
 * 卡片就会在原地展开成整页（container transform），返回时再收回卡片。
 *
 * @param boundsTransform 尺寸补间，为空时用 Compose 的默认弹簧。
 */
/**
 * 卡片 → 目标页面 的共享元素修饰符（容器变换）。
 *
 * 每张首页卡片都用 `home-card-<route>` 作为 key；目标页面把它挂到根容器上，
 * 卡片就会在原地展开成整页，返回时再收回。
 *
 * 2. `enter` / `exit` 刻意**错开**：旧内容先淡出、新内容后淡入，中间不重叠。
 *    默认的「同时交叉淡化」会让卡片内容和详情页内容在半透明状态下叠在一起，
 *    观感就是「闪」——Material 官方把这种情况称为 overlapping frames，
 *    并明确建议改用 fade through 来消除。
 *
 * **刻意不用 `ResizeMode.RemeasureToBounds`**：Material 规范推荐它来避免内容被拉伸，
 * 但它要求每帧重新测量内容子树，而我们的目标页是整页 LazyColumn ——
 * 冷启动后首次进入时，首帧组合 + 每帧重测叠加，直接表现为"其他动画都变卡"。
 * 在 fade through 的顺序里，拉伸发生在旧内容已淡出、新内容尚未显现的空档，
 * 肉眼基本看不到，所以用默认的 ScaleToBounds 即可。
 *
 * @param boundsTransform 尺寸补间，为空时用 Compose 的默认弹簧。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun sharedCardPageModifier(
    key: String,
    sharedScope: SharedTransitionScope?,
    animatedScope: AnimatedVisibilityScope?,
    boundsTransform: BoundsTransform? = null
): Modifier {
    if (sharedScope == null || animatedScope == null) return Modifier
    return with(sharedScope) {
        val state = rememberSharedContentState(key = key)
        if (boundsTransform == null) {
            Modifier.sharedBounds(
                sharedContentState = state,
                animatedVisibilityScope = animatedScope,
                enter = ContentFadeIn,
                exit = ContentFadeOut
            )
        } else {
            Modifier.sharedBounds(
                sharedContentState = state,
                animatedVisibilityScope = animatedScope,
                boundsTransform = boundsTransform,
                enter = ContentFadeIn,
                exit = ContentFadeOut
            )
        }
    }
}

/**
 * M3 emphasized-accelerate：退场专用，前段缓、后段快，像被"吸走"。
 * 官方 token：cubic-bezier(0.3, 0, 0.8, 0.15)
 */
internal val M3AccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/**
 * M3 emphasized-decelerate：进场专用，起步很快、末段轻缓，观感"迅速到位"。
 * 官方 token：cubic-bezier(0.05, 0.7, 0.1, 1)
 */
internal val M3DecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/**
 * M3 的 fade through 标准时序：退场 90ms，入场 210ms 且延后 90ms 开始，合计约 300ms。
 * 退场用 accelerate、入场用 decelerate，这是规范里明确的分工。
 */
private const val ExitMillis = 90
private const val EnterMillis = 210
private const val EnterDelayMillis = 90

/** 新页面内容：等旧内容退场后再进场（fade through 的「进」）。 */
private val ContentFadeIn: EnterTransition = fadeIn(
    animationSpec = tween(
        durationMillis = EnterMillis,
        delayMillis = EnterDelayMillis,
        easing = M3DecelerateEasing
    )
)

/** 旧页面内容：先快速退场，避免与被新内容重叠（fade through 的「出」）。 */
private val ContentFadeOut: ExitTransition = fadeOut(
    animationSpec = tween(durationMillis = ExitMillis, easing = M3AccelerateEasing)
)
