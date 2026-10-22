package com.myhebnu.ui.schedule.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhebnu.ui.theme.CourseTonalPalette
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * 课表详情层：一镜到底的两面翻转。
 *
 * 同一个 0→1 的进度驱动一切，中间没有断点：
 *
 * 0 – 0.5   正面（就是被点的那张格子）原地绕 Y 轴翻到 90°，位置与尺寸不变，
 *           投影逐渐加深、卡片轻微抬起 —— 像从桌面上把这张卡拿起来立住；
 * 0.5       正好侧立、两面都看不见，此刻换成背面；
 * 0.5 – 1   背面继续翻 90°→180°（背面自身即 90°→0°，所以铺开时是正的、不镜像），
 *           同时矩形从格子尺寸/位置平滑插值到屏幕居中的大卡片；
 *           缩放严格等于「当前矩形 ÷ 最终矩形」，排版不重排、字不跳。
 *
 * 关闭时进度反向播回：背面翻回 90° 缩回格子，正面再落回原位。
 */
/** iOS 面板手感：起步快、尾段长时间缓慢收尾（cubic-bezier(0.32, 0.72, 0, 1)） */
private val AppleEase = CubicBezierEasing(0.35f, 0f, 0.25f, 1f)

@Composable
fun CourseDetailOverlay(
    snapshot: CourseCellSnapshot,
    originRect: Rect?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val course = snapshot.course
    val palette = snapshot.palette

    val density = LocalDensity.current
    val pxDensity = density.density
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }

    var rootTopLeft by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var backSize by remember { mutableStateOf(IntSize.Zero) }
    var closing by remember { mutableStateOf(false) }

    val measured = rootSize.width > 0 && backSize.width > 0

    LaunchedEffect(measured) {
        if (!measured) return@LaunchedEffect
        if (originRect == null) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 660, easing = AppleEase)
            )
        }
    }

    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            if (originRect != null && progress.value > 0f) {
                progress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 420, easing = AppleEase)
                )
            }
            onDismiss()
        }
    }

    val p = progress.value
    val liftPx = 8.dp.value * pxDensity
    val cellCornerPx = 12.dp.value * pxDensity

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                rootTopLeft = it.boundsInRoot().topLeft
                rootSize = it.size
            }
    ) {
        // 透明命中层：点空白 = 把卡片放回去
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = p > 0.88f,
                    onClick = { close() }
                )
        )

        val rootCx = rootSize.width / 2f
        val rootCy = rootSize.height / 2f
        // 格子矩形换算到本层坐标（两边都取 boundsInRoot，差值即可）
        val cellRect = originRect?.translate(-rootTopLeft.x, -rootTopLeft.y)

        // 整段动画共用的"长大"进度（与翻面并行）
        val grow = p * p * (3f - 2f * p)

        // ---------- 正面：被点下的那张格子 ----------
        if (cellRect != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(snapshot.cardWidth, snapshot.cardHeight)
                    .graphicsLayer {
                        val frontP = (p.coerceAtMost(0.5f) / 0.5f)
                        // 只在前半段出现，最后 8% 淡出，避免被放大的小卡文字被看见；
                        // 几何还没测完时一帧都不画，避免"未缩放"的一帧造成闪烁
                        alpha = when {
                            !measured -> 0f
                            p < 0.42f -> 1f
                            p < 0.5f -> (0.5f - p) / 0.08f
                            else -> 0f
                        }
                        rotationY = frontP * 90f
                        cameraDistance = 20f * pxDensity

                        // 翻面与放大同时进行：整段动画共用一个"长大"进度
                        val restW = backSize.width.toFloat()
                        val restH = backSize.height.toFloat()
                        val cellW = cellRect.width
                        val cellH = cellRect.height
                        if (restW > 0f && restH > 0f && cellW > 0f) {
                            val rectW = cellW + (restW - cellW) * grow
                            val rectH = cellH + (restH - cellH) * grow
                            val cx = cellRect.center.x + (rootCx - cellRect.center.x) * grow
                            val cy = cellRect.center.y + (rootCy - cellRect.center.y) * grow
                            // 松手那一下卡片会从 0.94 弹回 1；这里接手这个回弹，
                            // 免得格子弹回与详情层出现同帧重叠造成闪烁
                            val settle = (p / 0.18f).coerceIn(0f, 1f)
                            val press = snapshot.pressScale + (1f - snapshot.pressScale) * settle
                            val baseScaleX = rectW / cellW
                            val baseScaleY = rectH / cellH
                            scaleX = baseScaleX * press
                            scaleY = baseScaleY * press
                            translationX = cx - rootCx
                            translationY = cy - rootCy - liftPx * sin(frontP * Math.PI).toFloat()
                        } else {
                            scaleX = snapshot.pressScale
                            scaleY = snapshot.pressScale
                            translationX = cellRect.center.x - rootCx
                            translationY = cellRect.center.y - rootCy
                        }
                    }
            ) {
                CourseCard(
                    course = course,
                    isActive = snapshot.isActive,
                    palette = palette,
                    cardHeight = snapshot.cardHeight,
                    cardWidth = snapshot.cardWidth,
                    modifier = Modifier.fillMaxSize(),
                    onClick = null
                )
            }
        }

        // ---------- 背面：详情内容（按最终尺寸排版，只做变换） ----------
        val backP = ((p - 0.5f) / 0.5f).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f)
                .widthIn(max = 340.dp)
                .onGloballyPositioned { backSize = it.size }
                .graphicsLayer {
                    val restW = backSize.width.toFloat()
                    val restH = backSize.height.toFloat()
                    val ready = measured && cellRect != null && restW > 0f && restH > 0f

                    alpha = if (measured && p >= 0.5f) 1f else 0f

                    if (ready && cellRect != null) {
                        // 整段共用同一个长大进度：换面时已经长到一半
                        val rectW = cellRect.width + (restW - cellRect.width) * grow
                        val rectH = cellRect.height + (restH - cellRect.height) * grow
                        val cx = cellRect.center.x + (rootCx - cellRect.center.x) * grow
                        val cy = cellRect.center.y + (rootCy - cellRect.center.y) * grow

                        scaleX = rectW / restW
                        scaleY = rectH / restH
                        translationX = cx - rootCx
                        translationY = cy - rootCy
                    } else {
                        scaleX = 1f
                        scaleY = 1f
                    }

                    // 背面自身：90° → 0°，与正面拼成连续的 0° → 180°
                    rotationY = -90f * (1f - backP)
                    cameraDistance = 20f * pxDensity

                    // 圆角：保持与格子的"曲率比例"一致（视觉圆度相同），
                    // 换算到本层局部坐标后正好是常量 restW/cellW 倍。
                    val cornerPx = if (ready && cellRect != null && cellRect.width > 0f) {
                        cellCornerPx * (restW / cellRect.width)
                    } else {
                        cellCornerPx
                    }
                    shape = RoundedCornerShape(cornerPx / pxDensity)
                    clip = true
                }
                .background(palette.container, RoundedCornerShape(cellCornerPx / pxDensity))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 26.dp)
            ) {
                // 课程名打头
                Text(
                    text = course.courseName,
                    fontSize = 29.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.onContainer,
                    maxLines = 4
                )

                Spacer(Modifier.height(30.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    MetaBlock(
                        label = "节次",
                        value = "${weekdayOf(course.dayOfWeek)} 第 ${course.startPeriod}–${course.endPeriod} 节",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                    MetaBlock(
                        label = "地点",
                        value = course.classroom.ifBlank { "—" },
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(20.dp))

                MetaBlock(
                    label = "教师",
                    value = course.teacher.ifBlank { "—" },
                    palette = palette,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(32.dp))

                // 页脚：周次与类别
                FooterLine(label = "周次", value = course.weekText.ifBlank { "—" }, palette = palette)
                Spacer(Modifier.height(9.dp))
                FooterLine(label = "类别", value = course.category.ifBlank { "—" }, palette = palette)
            }
        }
    }
}

private fun weekdayOf(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    7 -> "周日"
    else -> ""
}

@Composable
private fun MetaBlock(
    label: String,
    value: String,
    palette: CourseTonalPalette,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.8.sp,
            color = palette.variant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 23.sp,
            color = palette.onContainer,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FooterLine(
    label: String,
    value: String,
    palette: CourseTonalPalette
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.8.sp,
            color = palette.variant,
            modifier = Modifier.width(44.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 18.sp,
            color = palette.onContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
