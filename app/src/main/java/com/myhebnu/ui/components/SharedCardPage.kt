package com.myhebnu.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
