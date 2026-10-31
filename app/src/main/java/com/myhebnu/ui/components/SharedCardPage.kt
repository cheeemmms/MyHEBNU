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
 * **刻意不给 enter / exit**：这个转场的全部观感来自两件事——「卡片原地长成整页」的空间连续性，
 * 以及两端内容随容器一起缩放、在同一时刻同屏交接。给 `sharedBounds` 传 enter/exit 会再叠一层
 * 内容透明度，而且它只作用于容器内的内容、管不到覆盖层里的共享封面（[sharedCoverModifier]），
 * 两侧节奏就不同步了：旧内容提前消失、新内容迟到，中间露出一段底色。
 * 页面级的淡化统一由 `MainActivity` 的 `AnimatedContent` transitionSpec 负责。
 *
 * **缩放与裁切都用 `sharedBounds` 的默认值**（`ScaleToBounds(ContentScale.FillWidth, Center)` +
 * `ParentClip`）：录屏（v1.7.0）里的观感就是这么来的——按矩形宽度等比放大、垂直居中、超出部分
 * 由父级裁掉。任何对 `resizeMode` / `clipInOverlayDuringTransition` 的改动都会改变飞行途中的
 * 「裁切窗口」形状，从而改变题名、渐变遮罩、返回按钮露头的时机，必须先用录屏抽帧对照再动。
 *
 * 曾试验过 fade through 错开（把 [ContainerEnterTransition] / [ContainerExitTransition] 挂到
 * `sharedBounds` 上）与 `RemeasureToBounds`：前者把一次连续放大拆成「先退干净、再进」两个动作，
 * 丢掉了文字放大过程中的交接感；后者每帧重测整页导致掉帧。均已否决。
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
        // 录屏对齐版：缩放与裁切一律用 `sharedBounds` 的默认值
        // （ScaleToBounds(FillWidth, Center) + ParentClip）。
        if (boundsTransform == null) {
            Modifier.sharedBounds(
                sharedContentState = state,
                animatedVisibilityScope = animatedScope
            )
        } else {
            Modifier.sharedBounds(
                sharedContentState = state,
                animatedVisibilityScope = animatedScope,
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
            sharedContentState = rememberSharedContentState(key = key),
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

/** 容器变换的尺寸时长：与最顺的那一版录屏一致，220ms。 */
const val ContainerTransformMillis = 220

/** 页面级内容淡出时长：旧页面必须在容器刚起动的这一小段里让开。 */
private const val ContentFadeOutMillis = 90

/** 页面级内容淡入起点：等旧页面让开一段，新页面再进场。 */
private const val ContentFadeDelayMillis = 90

/** 页面级内容淡入时长：90 + 210 = 300，收在与容器同一个尾部。 */
private const val ContentFadeInMillis = 210

/** M3 emphasized：cubic-bezier(0.2, 0, 0, 1)，起步快、末段收得干净。 */
internal val M3EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * 页面级内容进场：延迟 90ms、210ms 内减速淡入（`MainActivity` 的图书馆详情路由用它）。
 *
 * 只负责「整页」的透明度，不挂到 `sharedBounds` 上——容器内的内容靠自己随容器缩放交接，
 * 再叠一层按容器走的透明度会把交接拆成两段。
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
 * 容器变换的尺寸补间：220ms，展开与收起共用同一条 M3 emphasized。
 *
 * 两个方向刻意共用一条曲线：去程与回程在设备上是前后脚看到的，用不同曲线会让回程
 * 像另一套动画；统一之后回程只是去程倒放。
 *
 * 220ms 而不是容器级规范更长的时长，是因为封面必须在页面内容淡入完成之前就位，
 * 否则收尾会拖出一段「只有封面在动」的空档。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val ContainerTransformBoundsTransform: BoundsTransform =
    BoundsTransform { _, _ ->
        tween(durationMillis = ContainerTransformMillis, easing = M3EmphasizedEasing)
    }
