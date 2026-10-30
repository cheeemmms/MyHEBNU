package com.myhebnu.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.myhebnu.data.local.db.entity.WishlistEntity
import kotlinx.coroutines.flow.Flow

/**
 * 「想看」列表的读写入口。
 *
 * 三个读方法都返回 Flow：界面只订阅，不持有收藏状态。
 * 这样在列表页移除一本书后返回详情页，爱心会自动变成描边——
 * 不存在「两份状态谁对谁错」的问题。
 */
@Dao
interface WishlistDao {

    /** 全部想看的书，最近加入的排前面。 */
    @Query("SELECT * FROM wishlist ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WishlistEntity>>

    /** 总数，驱动图书馆首页入口上的「N 本」。 */
    @Query("SELECT COUNT(*) FROM wishlist")
    fun observeCount(): Flow<Int>

    /** 某本书是否已在想看列表里。 */
    @Query("SELECT EXISTS(SELECT 1 FROM wishlist WHERE bibId = :bibId)")
    fun observeIsWished(bibId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist WHERE bibId = :bibId)")
    suspend fun isWished(bibId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WishlistEntity)

    @Query("DELETE FROM wishlist WHERE bibId = :bibId")
    suspend fun deleteById(bibId: String): Int

    /**
     * 切换收藏态，返回切换后的状态。
     *
     * 查询与写入放在同一个事务里，避免连点两次时两条协程都读到「未收藏」
     * 然后各插一次（REPLACE 虽然不会报错，但会刷新 addedAt，把书顶到列表最前）。
     */
    @Transaction
    suspend fun toggle(entry: WishlistEntity): Boolean {
        return if (isWished(entry.bibId)) {
            deleteById(entry.bibId)
            false
        } else {
            upsert(entry)
            true
        }
    }
}
