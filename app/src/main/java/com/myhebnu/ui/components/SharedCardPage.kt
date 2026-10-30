package com.myhebnu.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
 * enter / exit / resizeMode 均用 Compose 默认值：容器变换的核心是「卡片原地长成整页」
 * 的空间连续性，两端内容以交叉淡化衔接是刻意的观感（曾试验过 fade through 错开与
 * RemeasureToBounds，前者拉长转场、后者每帧重测整页导致掉帧，均已否决）。
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
