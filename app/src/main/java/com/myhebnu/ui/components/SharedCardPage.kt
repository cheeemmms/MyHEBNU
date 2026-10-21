package com.myhebnu.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 首页卡片 → 目标页面 的共享元素修饰符。
 *
 * 每张首页卡片都用 `home-card-<route>` 作为 key；目标页面把它挂到 Scaffold 根上，
 * 卡片就会在原地展开成整页（container transform），返回时再收回卡片。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun sharedCardPageModifier(
    key: String,
    sharedScope: SharedTransitionScope?,
    animatedScope: AnimatedVisibilityScope?
): Modifier {
    if (sharedScope == null || animatedScope == null) return Modifier
    return with(sharedScope) {
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedScope
        )
    }
}
