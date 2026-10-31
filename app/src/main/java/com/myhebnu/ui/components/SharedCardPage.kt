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
 * 快一档的尺寸变换：220ms 缓出补间。
 *
 * 默认的 BoundsTransform 是个偏软的弹簧，瀑布流卡片张开、收回时会显得慢半拍；
 * 需要更利落的页面显式传这个，其它页面继续用默认值，节奏不受影响。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val QuickCardBoundsTransform: BoundsTransform =
    BoundsTransform { _, _ ->
        tween(durationMillis = 220, easing = FastOutSlowInEasing)
    }

/**
 * 卡片 → 目标页面 的共享元素修饰符。
 *
 * 每张首页卡片都用 `home-card-<route>` 作为 key；目标页面把它挂到 Scaffold 根上，
 * 卡片就会在原地展开成整页（container transform），返回时再收回卡片。
 *
 * enter / exit / resizeMode 默认用 Compose 默认值：容器变换的核心是「卡片原地长成整页」
 * 的空间连续性，两端内容以交叉淡化衔接是刻意的观感。需要「一镜到底」那种不重叠的
 * 衔接时传 `contentFadeThrough = true`，旧内容先退干净、新内容再进来。
 *
 * 曾试验过全局 fade through 错开与 `RemeasureToBounds`，前者把整个 App 的转场都拉长、
 * 后者每帧重测整页导致掉帧，均已否决；现在只对明确需要连续性的路径按需开启。
 *
 * @param boundsTransform 尺寸补间，为空时用 Compose 的默认弹簧。
 * @param contentFadeThrough 内容淡入淡出是否错开（true 时新内容延迟进场，避免两端内容同屏）。
 * @param settleContentAfterTransform 内容等容器走完全程再进场。目标页挂共享封面
 *   （[sharedCoverModifier]）时用它：共享封面在覆盖层绘制、位于页面内容之上，内容若在
 *   容器还在扩张时就淡入，会被封面压住并在覆盖层撤走的那一帧突然冒出来。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun sharedCardPageModifier(
    key: String,
    sharedScope: SharedTransitionScope?,
    animatedScope: AnimatedVisibilityScope?,
    boundsTransform: BoundsTransform? = null,
    contentFadeThrough: Boolean = false,
    settleContentAfterTransform: Boolean = false
): Modifier {
    if (sharedScope == null || animatedScope == null) return Modifier
    // 不开启时显式传 Compose 默认的 fadeIn()/fadeOut()，行为与省略这两个参数一致
    val enter: EnterTransition = when {
        settleContentAfterTransform -> ContainerSettleEnterTransition
        contentFadeThrough -> ContainerEnterTransition
        else -> fadeIn()
    }
    val exit: ExitTransition =
        if (contentFadeThrough || settleContentAfterTransform) ContainerExitTransition else fadeOut()
    return with(sharedScope) {
        val state = rememberSharedContentState(key = key)
        if (boundsTransform == null) {
            Modifier.sharedBounds(
                sharedContentState = state,
                animatedVisibilityScope = animatedScope,
                enter = enter,
                exit = exit
            )
        } else {
            Modifier.sharedBounds(
                sharedContentState = state,
                animatedVisibilityScope = animatedScope,
                enter = enter,
                exit = exit,
                boundsTransform = boundsTransform
            )
        }
    }
}

/**
 * 封面 → 头图 的共享元素修饰符（与 [sharedCardPageModifier] 配套使用）。
 *
 * 容器变换只保证「外框」连续，里面的封面仍是两端各画一张再交叉淡化：卡片是预取的小图、
 * 详情是整屏解码的大图，两张图在换手的那几帧里会错位、发虚。把封面单独挂成共享元素之后，
 * 转场期间它是一张持续存在、不透明、被容器裁切的图层，从卡片原位长到头图原位——
 * 「封面从卡片里长出来」的连续感来自这里，而不是来自两个渐变的重叠。
 *
 * key 与挂载位置有两处约定：
 * 1. 卡片、想看行、详情页头图三处必须同一个 key（`search-cover-<bibId>`），否则对不上；
 * 2. 详情页的头图节点要嵌在页面节点（[sharedCardPageModifier]）内部，靠它的裁切边界
 *    跟着容器一起长大；共享元素默认 `ParentClip`，不额外传 clip。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun sharedCoverModifier(
    key: String,
    sharedScope: SharedTransitionScope?,
    animatedScope: AnimatedVisibilityScope?
): Modifier {
    if (sharedScope == null || animatedScope == null) return Modifier
    return with(sharedScope) {
        Modifier.sharedElement(
            state = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedScope,
            boundsTransform = ContainerTransformBoundsTransform
        )
    }
}

/**
 * M3 emphasized-accelerate：前段缓、后段快，像被"吸走"。
 * 官方 token：cubic-bezier(0.3, 0, 0.8, 0.15)。
 * 用于内容级的收拢/退出动画（如馆藏卡收起）。
 */
internal val M3AccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/**
 * M3 emphasized-decelerate：起步很快、末段轻缓，观感"迅速到位"。
 * 官方 token：cubic-bezier(0.05, 0.7, 0.1, 1)。
 * 用于内容级的展开/进入动画（如馆藏卡展开）。
 */
internal val M3DecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** 容器变换的总时长：M3 container transform 的标准 300ms。 */
const val ContainerTransformMillis = 300

/** 内容淡出时长：旧内容必须在容器刚起动的这一小段里让开。 */
private const val ContentFadeOutMillis = 90

/** 内容淡入起点：等容器先走出去一段，新内容再进场。 */
private const val ContentFadeDelayMillis = 90

/** 内容淡入时长：90 + 210 = 300，与容器的总时长收在同一个尾部。 */
private const val ContentFadeInMillis = 210

/** 内容落定时长：容器走完之后，页面内容再用这么长的时间铺上来。 */
private const val ContentSettleInMillis = 160

/**
 * 容器变换的内容进场：延迟 90ms、210ms 内减速淡入。
 *
 * 与 [ContainerExitTransition] 合计正好占满 300ms，且两段不重叠——卡片上左对齐的
 * 书名先退场，详情页居中标题才出现，中间不会出现两套标题叠在一起的「重影」帧。
 */
internal val ContainerEnterTransition: EnterTransition = fadeIn(
    animationSpec = tween(
        durationMillis = ContentFadeInMillis,
        delayMillis = ContentFadeDelayMillis,
        easing = M3DecelerateEasing
    )
)

/** 容器变换的内容退场：90ms 加速淡出，给进场内容让出位置。 */
internal val ContainerExitTransition: ExitTransition = fadeOut(
    animationSpec = tween(
        durationMillis = ContentFadeOutMillis,
        easing = M3AccelerateEasing
    )
)

/**
 * 目标页的内容落定：等容器走完（[ContainerTransformMillis]）再淡入。
 *
 * 目标页挂着共享封面时用它：共享封面在覆盖层绘制，压在同一页的内容之上，内容若在
 * 容器还在扩张时就淡入，会被封面挡住；覆盖层撤走的那一刻（300ms）才「冒出来」。
 * 让内容在覆盖层撤走之后进场，观感是「封面先长到位，正文再铺开」。
 */
internal val ContainerSettleEnterTransition: EnterTransition = fadeIn(
    animationSpec = tween(
        durationMillis = ContentSettleInMillis,
        delayMillis = ContainerTransformMillis,
        easing = M3DecelerateEasing
    )
)

/**
 * 容器变换的尺寸补间：300ms，展开与收起用同一条 emphasized-decelerate。
 *
 * 两个方向刻意共用一条曲线。按 M3 原始 token，扩张走 decelerate、收缩走
 * accelerate，但同一次「点开 / 返回」在设备上是前后脚看到的：去程 300ms
 * 「冲出去再收住」、回程 300ms「先缓后快撤走」，回程看起来像另一套动画。
 * 统一成 decelerate 之后，回程只是去程的倒放，收尾节奏一致。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val ContainerTransformBoundsTransform: BoundsTransform =
    BoundsTransform { _, _ ->
        tween(durationMillis = ContainerTransformMillis, easing = M3DecelerateEasing)
    }
