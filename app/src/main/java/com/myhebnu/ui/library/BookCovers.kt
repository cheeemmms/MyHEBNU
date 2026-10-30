package com.myhebnu.ui.library

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest

/**
 * 书封的统一取图入口。
 *
 * 这里存在的唯一理由是**让同一个 bibId 在不同页面命中同一份内存缓存**：
 * 检索卡片、想看列表、详情页背景取的是同一本书的封面，只是显示尺寸不同。
 *
 * 按 Coil / Android 官方对共享元素转场的建议：
 * - 内存缓存键从实体 ID 派生，而不是直接拿 URL 当键（URL 会变、且不同图文源不同）；
 * - 详情页额外设置 `placeholderMemoryCacheKey`，于是它在加载大图期间会先用
 *   列表里已缓存的那张低清图顶着——这就是「第一次打开也不空白」的关键；
 * - 任何请求都**必须给出目标尺寸**，否则 Coil 推断不出目标边界，会按原图分辨率解码。
 *   详情页背景早先用 `wrapContentHeight` 正是踩了这个坑。
 */
fun bookCoverCacheKey(bibId: String): String = "book-cover-$bibId"

/**
 * 详情页整屏背景用的缓存键。
 *
 * 与大图分开存：否则一张 1179×1296 的详情图会把列表用的小图从缓存里顶掉，
 * 返回检索页时又得重新解码。分键之后，大图占位改从 [bookCoverCacheKey] 取。
 */
private fun bookCoverLargeCacheKey(bibId: String): String = "book-cover-lg-$bibId"

/**
 * 构造一个书封请求。
 *
 * @param size 目标解码尺寸（像素）。不传则交给 Coil 按布局尺寸推断——只在该 Composable
 *             尺寸已经确定（例如带 `aspectRatio` 的卡片）时才可靠。
 * @param large 详情页整屏背景用 true：结果存到独立的"大图"键下
 * @param placeholderFromCache 允许把列表缓存里那张小图当作占位，大图到了再替换
 */
fun bookCoverRequest(
    context: Context,
    url: String,
    bibId: String,
    size: Pair<Int, Int>? = null,
    large: Boolean = false,
    placeholderFromCache: Boolean = false
): ImageRequest {
    val key = if (large) bookCoverLargeCacheKey(bibId) else bookCoverCacheKey(bibId)
    return ImageRequest.Builder(context)
        .data(url)
        .memoryCacheKey(key)
        .apply {
            if (placeholderFromCache) placeholderMemoryCacheKey(bookCoverCacheKey(bibId))
            size?.let { (width, height) -> this.size(width, height) }
        }
        .build()
}

/**
 * 预取列表里的封面。
 *
 * 只按**卡片尺寸**预取：一页 20 张在 540×720 这个量级约 30MB，内存缓存完全吃得下；
 * 若按详情页的整屏尺寸预取，单张就接近 6MB、一页 120MB，反而会把堆打满。
 * 详情页的大图不预取，靠 [bookCoverRequest] 的占位键做无缝过渡。
 */
fun prefetchBookCovers(
    context: Context,
    imageLoader: ImageLoader,
    bibIds: List<String>,
    urlOf: (String) -> String,
    widthPx: Int,
    heightPx: Int,
    limit: Int = 20
) {
    bibIds.asSequence()
        .distinct()
        .take(limit)
        .forEach { bibId ->
            val url = urlOf(bibId)
            if (url.isBlank()) return@forEach
            imageLoader.enqueue(
                bookCoverRequest(
                    context = context,
                    url = url,
                    bibId = bibId,
                    size = widthPx to heightPx
                )
            )
        }
}
