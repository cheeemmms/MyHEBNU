package com.myhebnu.ui.library

import android.net.Uri

/**
 * 图书馆模块的路由常量。
 *
 * 路由由 androidx.navigation 的 `NavHost` 驱动（`MainActivity.MainAppContent`），
 * 简单页面用固定路由；书目详情页带参数，用 query 参数写法
 * [BIB_DETAIL_PATTERN]，由 [bibDetail] 拼装、NavHost 按 `navArgument` 还原。
 *
 * 用 query 参数而不是路径段，是因为书名里可能出现 `/`、`?`、`&`、`#` 等字符，
 * 路径段会被导航框架当成结构解析；query 参数由 [Uri.encode] 逐个转义，安全。
 */
object LibraryRoutes {

    const val HOME = "library"
    const val READER_CODE = "library_reader_code"
    const val LOANS = "library_loans"
    const val REQUESTS = "library_requests"
    const val FINES = "library_fines"
    const val SEARCH = "library_search"

    /** 「我想看的书」，本地数据，不联网。 */
    const val WISHLIST = "library_wishlist"

    /**
     * 书目详情路由的前缀。
     *
     * 页面级转场靠它判定「这次切换是否牵涉详情页」（见 `MainActivity` 的
     * `NavBackStackEntry.isBibDetailEntry()`），因此模式串必须以它开头。
     */
    const val BIB_DETAIL_PREFIX = "library_bib"

    /** 书目详情路由模式：注册目的地时用，四个参数都可选、缺省空串。 */
    const val BIB_DETAIL_PATTERN =
        "$BIB_DETAIL_PREFIX?bibId={bibId}&isbn={isbn}&title={title}&cover={cover}"

    /** 书目详情页的入参（与 [BIB_DETAIL_PATTERN] 的参数一一对应）。 */
    data class BibDetailArgs(
        val bibId: String,
        val isbn: String,
        val title: String,
        /**
         * 进入这一页之前，列表卡片上正在显示的那张封面地址。
         *
         * 详情接口回来之前，详情页靠它在首帧就画出同一张图（缓存键按 bibId，同一份位图），
         * 容器变换过程中不会出现「封面先消失、接口回来再长出来」的空窗。
         */
        val cover: String = "",
    )

    /** 拼一个书目详情路由。四个参数都会做 URI 转义。 */
    fun bibDetail(
        bibId: String,
        isbn: String?,
        title: String?,
        cover: String? = null,
    ): String = buildString {
        append(BIB_DETAIL_PREFIX)
        append("?bibId=").append(Uri.encode(bibId))
        append("&isbn=").append(Uri.encode(isbn.orEmpty()))
        append("&title=").append(Uri.encode(title.orEmpty()))
        append("&cover=").append(Uri.encode(cover.orEmpty()))
    }
}
