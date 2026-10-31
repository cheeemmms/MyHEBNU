package com.myhebnu.data.repository

import com.myhebnu.data.remote.opac.OpacDouban
import com.myhebnu.data.remote.opac.OpacHolding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 书目详情的数据装配器：**点击卡片时预取、详情页直接复用**。
 *
 * 为什么需要它：详情页原先是在进入组合之后才发请求，于是转场那 220ms 里页面只有
 * 路由带来的题名/作者/封面，馆藏与简介要等接口回来才「长」出来，观感上就是落位后
 * 一帧生硬弹出。这里把同一批请求提前到点击时刻，让转场期间内容就已经齐了。
 *
 * 三条护栏（都是为了不给汇文服务器加压力）：
 * 1. **同一本书只飞一次**：[inFlight] 按 bibId 去重，预取与页面 `load()` 共用同一条请求，
 *    所以一次点击仍然只产生一组请求，绝不会有「预取一次 + 页面再要一次」；
 * 2. **并发上限 1**：已有别的书在飞时，新的预取直接放弃（不排队、不堆积）；
 * 3. **60 秒内存缓存**：来回翻同一本书只请求一次，常态下比改动前更省。
 *
 * 缓存只在内存里，进程结束即失效；不做磁盘持久化，避免展示陈旧的「可借」状态。
 */
@Singleton
class BibDetailLoader @Inject constructor(
    private val repository: OpacRepository,
) {

    /** 一次详情所需的全部数据，字段语义与原先 ViewModel 里的三元组一致。 */
    data class Payload(
        val bundle: OpacBibDetailBundle,
        val holdings: List<OpacHolding>,
        val douban: OpacDouban?,
    )

    private class Cached(val at: Long, val result: Result<Payload>)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = ConcurrentHashMap<String, Cached>()
    private val inFlight = ConcurrentHashMap<String, Deferred<Result<Payload>>>()

    /**
     * 预取一本书的详情。命中缓存或已有同书请求在飞时什么都不做；
     * 已有**别的**书在飞时直接放弃，保证同一时刻最多一条请求。
     */
    fun prefetch(bibId: String) {
        if (bibId.isBlank()) return
        if (isFresh(bibId)) return
        if (inFlight.containsKey(bibId)) return
        if (inFlight.isNotEmpty()) return
        start(bibId, isbn = null)
    }

    /**
     * 取一本书的详情：新鲜缓存直接返回，否则复用/发起请求并等待。
     *
     * @param isbn 路由带来的 ISBN，详情接口没给 ISBN 时用它去请求豆瓣副数据。
     * @param forceRefresh 错误态「重试」时置 true，丢掉缓存重新请求。
     */
    suspend fun load(
        bibId: String,
        isbn: String? = null,
        forceRefresh: Boolean = false,
    ): Result<Payload> {
        if (forceRefresh) {
            cache.remove(bibId)
        } else {
            cache[bibId]?.let { if (isFresh(it)) return it.result }
        }
        return start(bibId, isbn).await()
    }

    private fun start(bibId: String, isbn: String?): Deferred<Result<Payload>> =
        inFlight.computeIfAbsent(bibId) { id ->
            scope.async {
                try {
                    val result = runCatching {
                        coroutineScope {
                            // 详情先回：豆瓣那一路要它的 ISBN，而路由带来的 ISBN 可能为空
                            val bundle = async { repository.bibDetail(id) }.await().getOrThrow()
                            val effectiveIsbn = bundle.base?.isbn?.takeIf { it.isNotBlank() } ?: isbn
                            val holdings = async { repository.holdings(id) }
                            val douban = async { repository.douban(effectiveIsbn, id) }
                            Payload(
                                bundle = bundle,
                                holdings = holdings.await().getOrNull().orEmpty(),
                                douban = douban.await().getOrNull(),
                            )
                        }
                    }
                    cache[id] = Cached(System.currentTimeMillis(), result)
                    result
                } finally {
                    inFlight.remove(id)
                }
            }
        }

    private fun isFresh(bibId: String): Boolean = cache[bibId]?.let(::isFresh) == true

    private fun isFresh(cached: Cached): Boolean =
        System.currentTimeMillis() - cached.at < TtlMillis

    private companion object {
        /** 60 秒：够覆盖「点开 → 返回 → 再点开」的往返，又不至于让「可借」状态过期太久。 */
        const val TtlMillis = 60_000L
    }
}
