package com.myhebnu.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 「想看」列表的本地快照。
 *
 * 存的是**收藏那一刻的书籍字段**，而不只是一个 bibId：列表页要能在完全离线的情况下
 * 把标题、作者、封面画出来。如果只存 ID，进列表就得为每本书发一次详情请求，
 * 断网一片空白，联网也慢。
 *
 * 主键用 `bibId`（图书馆给每条书目记录的哈希式 ID），不是 ISBN——
 * 同一本书的不同版本、不同馆藏是不同 bibId，ISBN 反而可能重复。
 */
@Entity(tableName = "wishlist")
data class WishlistEntity(
    @PrimaryKey
    val bibId: String,
    val title: String,
    val author: String,
    val publisher: String,
    val pubYear: String,
    val isbn: String,
    val coverUrl: String,
    /** 加入时间（epoch 毫秒），列表按它倒序，最近想看的排最前。 */
    val addedAt: Long
)
