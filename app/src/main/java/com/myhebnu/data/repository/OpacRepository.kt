package com.myhebnu.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.myhebnu.data.remote.opac.OpacApi
import com.myhebnu.data.remote.opac.OpacApiException
import com.myhebnu.data.remote.opac.OpacAuthManager
import com.myhebnu.data.remote.opac.OpacBibBase
import com.myhebnu.data.remote.opac.OpacBibDetail
import com.myhebnu.data.remote.opac.OpacCoversRequest
import com.myhebnu.data.remote.opac.OpacDistribution
import com.myhebnu.data.remote.opac.OpacDouban
import com.myhebnu.data.remote.opac.OpacEnvelope
import com.myhebnu.data.remote.opac.OpacHolding
import com.myhebnu.data.remote.opac.OpacHotBook
import com.myhebnu.data.remote.opac.OpacLoan
import com.myhebnu.data.remote.opac.OpacQueryField
import com.myhebnu.data.remote.opac.OpacRequest
import com.myhebnu.data.remote.opac.OpacSearchHistory
import com.myhebnu.data.remote.opac.OpacSearchItem
import com.myhebnu.data.remote.opac.OpacSearchPage
import com.myhebnu.data.remote.opac.OpacSearchRequest
import com.myhebnu.data.remote.opac.OpacStats
import com.myhebnu.data.remote.opac.OpacUserInfo
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 图书馆数据仓库。
 *
 * 统一负责三件事：
 * 1. 调用前确保 CAS 会话存在（[OpacAuthManager]）；
 * 2. 把 `{"code":1,"msg":"META-403xxx: 中文提示"}` 转成可直接展示的 [Result.failure]；
 * 3. 会话失效（302/401）时自动重登并重试一次。
 */
@Singleton
class OpacRepository @Inject constructor(
    private val api: OpacApi,
    private val authManager: OpacAuthManager
) {
    private val gson = Gson()

    /** 目前借出的图书（按到期日由服务端排序）。 */
    suspend fun loans(page: Int = 1, pageSize: Int = 20): Result<OpacLoanPage> =
        request { api.loans(page, pageSize) }.map { dto ->
            OpacLoanPage(
                items = dto.items.orEmpty(),
                total = dto.total ?: dto.items?.size ?: 0
            )
        }

    /** 即将到期（服务端按到期日筛选），用于首页提醒。 */
    suspend fun overdueSoon(): Result<List<OpacLoan>> =
        request { api.overdueSoon(1, 20) }.map { it.items.orEmpty() }

    suspend fun profile(): Result<OpacUserInfo> = request { api.userInfo() }

    suspend fun stats(): Result<OpacStats> = request { api.stats() }

    /** 借阅额度文本，形如 `"3 / 20"`。 */
    suspend fun loanQuota(): Result<String> = request { api.loanInfo() }

    /** 动态借书码。二维码与条形码使用**同一个**字符串。 */
    suspend fun readerCode(): Result<String> = request { api.elecCode() }

    suspend fun fineRecords(): Result<List<OpacRequest>> =
        request { api.fines(1, 20) }.map { it.items.orEmpty() }

    /** 近 12 个月借阅量，按月份升序。 */
    suspend fun loanTrend(): Result<List<Pair<String, Int>>> =
        request { api.loanRangeDistribution() }.map { map ->
            map.entries
                .sortedBy { it.key }
                .map { it.key to (it.value ?: 0) }
        }

    suspend fun loanCategories(): Result<List<OpacDistribution>> =
        request { api.loanCategoryDistribution() }

    suspend fun activeRequests(): Result<List<OpacRequest>> =
        request { api.requestList(1, 20, "doing", 0) }.map { it.items.orEmpty() }

    suspend fun allRequests(): Result<List<OpacRequest>> =
        request { api.requestList(1, 20, "all", 0) }.map { it.items.orEmpty() }

    suspend fun suggestions(): Result<List<OpacRequest>> =
        request { api.suggestions(1, 20) }.map { it.items.orEmpty() }

    /**
     * 检索图书。
     *
     * 请求体结构来自抓包：`queryFieldList` 里 `field` 取
     * `all`/`titles`/`authors`/`isbns`…，`operator` 取 `*`/`=`/`<=`/`~`。
     */
    suspend fun search(
        query: String,
        page: Int = 1,
        pageSize: Int = 20,
        field: String = "all",
        operator: String = "*"
    ): Result<OpacSearchResult> {
        val body = OpacSearchRequest(
            page = page,
            pageSize = pageSize,
            queryFieldList = listOf(
                OpacQueryField(field = field, values = listOf(query), operator = operator)
            )
        )
        return request { api.search(body) }.map { dto ->
            OpacSearchResult(
                items = dto.dataList.orEmpty(),
                total = dto.total ?: 0
            )
        }
    }

    suspend fun searchHistory(): Result<List<OpacSearchHistory>> =
        request { api.searchHistory() }.map { it.items.orEmpty() }

    suspend fun suggest(query: String): Result<List<String>> =
        request { api.suggest(query = query) }

    /**
     * 批量补封面：检索结果里的 `thumb` 恒为空串，封面要另外请求这个接口。
     *
     * 返回 `bibId -> 封面地址`，地址可能是协议相对形式（`//img3m1.ddimg.cn/..`），
     * 这里统一补成 `https:` 以便直接交给 Coil。
     */
    suspend fun covers(bibIds: List<String>, isbns: List<String> = emptyList()): Result<Map<String, String>> {
        val ids = bibIds.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return Result.success(emptyMap())
        return request {
            api.covers(body = OpacCoversRequest(isbns = isbns.filter { it.isNotBlank() }, bibIds = ids))
        }.map { dto ->
            dto.orEmpty().mapNotNull { (bibId, refs) ->
                val url = refs.firstNotNullOfOrNull { it.imageUrl?.takeIf(String::isNotBlank) }
                url?.let { bibId to normalizeImageUrl(it) }
            }.toMap()
        }
    }

    /** 书目详情：基础信息 + 编目字段（HTML 富文本）+ 统计。 */
    suspend fun bibDetail(bibId: String): Result<OpacBibDetailBundle> =
        request { api.bibInfo(bibId = bibId) }.map { dto ->
            val sections = dto.map
            OpacBibDetailBundle(
                base = sections?.baseInfo?.map,
                fields = sections?.detailInfo?.map.orEmpty(),
                clickCount = sections?.extraInfo?.map?._clickCount ?: 0
            )
        }

    /** 馆藏：服务端把列表塞进一个**字符串**字段 `holdings`，需要二次解析。 */
    suspend fun holdings(bibId: String): Result<List<OpacHolding>> =
        request { api.bibHoldings(bibId = bibId) }.map { map ->
            val raw = map["holdings"] ?: "[]"
            val type = object : TypeToken<List<OpacHolding>>() {}.type
            runCatching { gson.fromJson<List<OpacHolding>>(raw, type) }.getOrDefault(emptyList())
        }

    /** 豆瓣副数据（简介 / 目录 / 封面）；失败不影响详情页主体。 */
    suspend fun douban(isbn: String?, bibId: String): Result<OpacDouban> {
        if (isbn.isNullOrBlank()) return Result.failure(OpacApiException("缺少 ISBN"))
        return request { api.doubanInfo(isbn = isbn, bibId = bibId) }
    }

    suspend fun newBooks(pageSize: Int = 5): Result<OpacSearchResult> =
        request { api.newBooks(page = 1, pageSize = pageSize) }.map { dto ->
            OpacSearchResult(items = dto.dataList.orEmpty(), total = dto.total ?: 0)
        }

    suspend fun hotBooks(count: Int = 5): Result<List<OpacHotBook>> =
        request { api.hotBooks(count = count) }

    /** 预热会话：进入图书馆模块时先登录，后续请求就都命中已建立的 Cookie。 */
    suspend fun warmUp(): Result<Unit> = authManager.ensureLoggedIn()

    /** 豆瓣封面常给协议相对地址（`//host/x.jpg`），补成绝对地址。 */
    private fun normalizeImageUrl(url: String): String =
        if (url.startsWith("//")) "https:$url" else url

    // ------------------------------------------------------------------
    // 内部：统一错误处理
    // ------------------------------------------------------------------

    private suspend fun <T> request(
        retryOnAuthFailure: Boolean = true,
        block: suspend () -> OpacEnvelope<T>
    ): Result<T> {
        return try {
            authManager.ensureLoggedIn().getOrThrow()
            unwrap(block())
        } catch (e: Exception) {
            if (retryOnAuthFailure && isSessionError(e)) {
                authManager.invalidate()
                val relogin = runCatching { authManager.ensureLoggedIn(force = true).getOrThrow() }
                if (relogin.isSuccess) {
                    return request(retryOnAuthFailure = false, block = block)
                }
                Result.failure(OpacApiException("图书馆会话已失效，请重新登录"))
            } else {
                Result.failure(e)
            }
        }
    }

    private fun <T> unwrap(envelope: OpacEnvelope<T>): Result<T> {
        val code = envelope.code ?: -1
        if (code != 0) {
            val msg = envelope.msg?.trim().orEmpty()
            return Result.failure(
                OpacApiException(msg.ifBlank { "图书馆服务返回异常（code=$code）" })
            )
        }
        val data = envelope.data ?: return Result.failure(OpacApiException("图书馆未返回数据"))
        return Result.success(data)
    }

    private fun isSessionError(e: Throwable): Boolean = when (e) {
        is HttpException -> e.code() == 401 || e.code() == 403 || e.code() == 302
        else -> {
            val msg = e.message.orEmpty()
            msg.contains("未登录") || msg.contains("登录超时") || msg.contains("NOT_LOGIN") ||
                msg.contains("401") || msg.contains("302")
        }
    }
}

data class OpacLoanPage(val items: List<OpacLoan>, val total: Int)

data class OpacSearchResult(val items: List<OpacSearchItem>, val total: Int)

data class OpacBibDetailBundle(
    val base: OpacBibBase?,
    /** 编目字段：键是中文标签（如「题名/责任者」），值是可能含 `<a>` 的 HTML。 */
    val fields: Map<String, String>,
    val clickCount: Int
)
