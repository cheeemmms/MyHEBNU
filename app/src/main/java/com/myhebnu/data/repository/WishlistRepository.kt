package com.myhebnu.data.repository

import com.myhebnu.data.local.db.dao.WishlistDao
import com.myhebnu.data.local.db.entity.WishlistEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 「想看」列表仓库。
 *
 * 这里刻意只做一件事：把 DAO 的读写编排成界面能直接用的形式。
 * 不做缓存策略、不碰网络——想看列表是纯本地数据，离线也要能用。
 *
 * 界面侧的约束：**收藏状态永远从 [observeIsWished] 读，不要在 ViewModel 里另存一个布尔**。
 * 否则从列表页移除一本书后回到详情页，那份本地状态会与数据库不一致。
 */
@Singleton
class WishlistRepository @Inject constructor(
    private val dao: WishlistDao
) {

    /** 全部想看的书，最近加入的在前。 */
    fun observeAll(): Flow<List<WishlistEntity>> = dao.observeAll()

    /** 总数，用于图书馆首页入口的「N 本」。 */
    fun observeCount(): Flow<Int> = dao.observeCount()

    /** 某本书是否已在列表中。 */
    fun observeIsWished(bibId: String): Flow<Boolean> = dao.observeIsWished(bibId)

    /**
     * 切换收藏态，返回切换后的状态（true = 已加入）。
     *
     * `addedAt` 由调用方传入而不是在这里取当前时间，是为了让详情页可以在
     * 一次点击里完成「构建快照 + 落库」，避免两次读时钟产生不一致。
     */
    suspend fun toggle(entry: WishlistEntity): Boolean = dao.toggle(entry)

    /** 从列表里移除。返回删除的行数，0 表示本来就不在。 */
    suspend fun remove(bibId: String): Int = dao.deleteById(bibId)

    /** 导出用：一次性取全部条目。 */
    suspend fun allEntries(): List<WishlistEntity> = dao.getAll()

    /**
     * 备份导入用：按 bibId 合并，本地已有的跳过。
     *
     * 返回实际新增的条数（重复的被数据库忽略，对应返回 -1）。
     */
    suspend fun mergeAll(entries: List<WishlistEntity>): Int {
        if (entries.isEmpty()) return 0
        return dao.insertIgnoringDuplicates(entries).count { it != -1L }
    }
}
