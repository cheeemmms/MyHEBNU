package com.myhebnu.ui.library

/**
 * 图书馆模块的路由常量。
 *
 * 项目使用手写返回栈（`MainActivity.MainAppContent` 里的 `currentRoute` + `backStack`），
 * 所以这里不做 Navigation 图的参数占位符，而是把参数编码进 route 字符串，
 * 由 `MainActivity` 的 `when (route)` 分支里调用 [isBibDetail] / [parseBibDetail] 还原。
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

    /** 书目详情路由前缀，后面用 [SEP] 拼接参数。 */
    private const val BIB_DETAIL_PREFIX = "library_bib"

    /** 用不可见控制字符做分隔符，避免书名里出现 `|` 之类的字符导致解析错位。 */
    private const val SEP = "\u0001"

    /** 书目详情页的参数。 */
    data class BibDetailArgs(
        val bibId: String,
        val isbn: String,
        val title: String,
    )

    fun bibDetail(bibId: String, isbn: String?, title: String?): String =
        BIB_DETAIL_PREFIX + SEP + bibId + SEP + isbn.orEmpty() + SEP + title.orEmpty()

    fun isBibDetail(route: String): Boolean = route.startsWith(BIB_DETAIL_PREFIX + SEP)

    fun parseBibDetail(route: String): BibDetailArgs? {
        if (!isBibDetail(route)) return null
        val parts = route.split(SEP)
        if (parts.size < 4) return null
        return BibDetailArgs(
            bibId = parts[1],
            isbn = parts[2],
            title = parts[3],
        )
    }
}
