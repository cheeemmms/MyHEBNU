package com.myhebnu.data.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.myhebnu.data.local.db.entity.WishlistEntity

/**
 * 「想看」备份文件的格式与读写。
 *
 * 选 JSON 而不是 CSV：带 `version` 字段，将来加字段时旧文件仍能读；
 * 条目里有嵌套结构，CSV 表达起来别扭；而且用户可以拿文本编辑器直接打开看。
 *
 * 反序列化走一层**全可空的中间结构**，而不是直接把 JSON 映射到 [WishlistEntity]：
 * Gson 用 Unsafe 分配对象，缺字段时给非空属性塞的是 null，等到使用时才崩。
 * 中间结构把「字段缺失」变成「显式判空 + 跳过坏行」，坏文件不会拖垮整个导入。
 */
data class WishlistBackupFile(
    val version: Int? = null,
    val exportedAt: Long? = null,
    val items: List<BackupItem>? = null
)

data class BackupItem(
    val bibId: String? = null,
    val title: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val pubYear: String? = null,
    val isbn: String? = null,
    @SerializedName("coverUrl") val coverUrl: String? = null,
    val addedAt: Long? = null
)

/** 导入结果：新增了几条、跳过了几条（本地已有）。 */
data class WishlistImportResult(
    val added: Int,
    val skippedInFile: Int
)

object WishlistBackup {

    const val CURRENT_VERSION = 1

    private val gson = Gson()

    fun encode(entries: List<WishlistEntity>, now: Long): String {
        val file = WishlistBackupFile(
            version = CURRENT_VERSION,
            exportedAt = now,
            items = entries.map { entry ->
                BackupItem(
                    bibId = entry.bibId,
                    title = entry.title,
                    author = entry.author,
                    publisher = entry.publisher,
                    pubYear = entry.pubYear,
                    isbn = entry.isbn,
                    coverUrl = entry.coverUrl,
                    addedAt = entry.addedAt
                )
            }
        )
        return gson.toJson(file)
    }

    /**
     * 解析备份文件。
     *
     * 只要求 `bibId` 非空——它是主键，缺了就无法导入；其余字段缺失时补空串，
     * 至少不会让这一行变得不可用。
     */
    fun decode(json: String): Result<Pair<List<WishlistEntity>, Int>> = runCatching {
        val file = gson.fromJson(json, WishlistBackupFile::class.java)
            ?: error("文件内容为空")
        val rawItems = file.items ?: error("文件里没有书籍数据")

        val entries = rawItems.mapNotNull { item ->
            val bibId = item.bibId?.trim().orEmpty()
            if (bibId.isEmpty()) return@mapNotNull null
            WishlistEntity(
                bibId = bibId,
                title = item.title.orEmpty(),
                author = item.author.orEmpty(),
                publisher = item.publisher.orEmpty(),
                pubYear = item.pubYear.orEmpty(),
                isbn = item.isbn.orEmpty(),
                coverUrl = item.coverUrl.orEmpty(),
                // 没有时间戳的条目排在最后，而不是抢到最前面
                addedAt = item.addedAt ?: 0L
            )
        }

        val dropped = rawItems.size - entries.size
        entries to dropped
    }
}
