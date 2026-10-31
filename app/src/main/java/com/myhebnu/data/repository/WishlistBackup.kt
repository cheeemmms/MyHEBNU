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
 *
 * ## JSON 键名是显式契约，不依赖字段名
 *
 * 每个字段都写了 `@SerializedName`：release 包会跑 R8，模型类的字段名被重命名
 * （`bibId` → `a`）以后，导出的文件就与 debug 包互相读不了。`@SerializedName` 的值
 * 混淆后保持原样，键名从此与字段名解耦。与之配套的两道护栏：
 *
 * - `app/proguard-rules.pro` 对本文件的模型类做了字段级 keep。否则字段会被裁剪
 *   （`version` 曾整个消失），`items` 的泛型签名退化成裸 `ArrayList`，Gson 只能把元素
 *   解析成 `LinkedTreeMap`，导入时抛 `cannot be cast to BackupItem`；
 * - `app/src/test/java/com/myhebnu/data/repository/WishlistBackupTest.kt` 把键名钉死，
 *   日后改字段名会立刻失败。
 */
data class WishlistBackupFile(
    @SerializedName("version") val version: Int? = null,
    @SerializedName("exportedAt") val exportedAt: Long? = null,
    @SerializedName("items") val items: List<BackupItem>? = null
)

data class BackupItem(
    @SerializedName("bibId") val bibId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("publisher") val publisher: String? = null,
    @SerializedName("pubYear") val pubYear: String? = null,
    @SerializedName("isbn") val isbn: String? = null,
    @SerializedName("coverUrl") val coverUrl: String? = null,
    @SerializedName("addedAt") val addedAt: Long? = null
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
     * 至少不会让这一行变得不可用。元素不是对象（例如数组里混进了数字）时跳过该行，
     * 而不是让整个文件导入失败。
     */
    fun decode(json: String): Result<Pair<List<WishlistEntity>, Int>> = runCatching {
        val file = gson.fromJson(json, WishlistBackupFile::class.java)
            ?: error("文件内容为空")
        val rawItems: List<*> = file.items
            ?: error("文件里没有书籍数据：可能是旧版本导出的文件，请重新导出后再导入")

        val entries = rawItems.mapNotNull { raw ->
            val item = raw as? BackupItem ?: return@mapNotNull null
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
