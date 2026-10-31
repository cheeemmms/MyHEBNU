package com.myhebnu.data.repository

import com.myhebnu.data.local.db.entity.WishlistEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 把「想看」备份文件的 JSON 键名钉死。
 *
 * release 包跑 R8，模型类的字段名会被重命名；一旦键名跟着字段名走，导出的文件就与
 * debug 包、或与另一个版本的 release 包互相读不了（v1.7.1 首发事故）。
 * 这些断言保证：键名由 `@SerializedName` 决定，改字段名不会悄悄改变文件格式。
 */
class WishlistBackupTest {

    private fun entity(bibId: String) = WishlistEntity(
        bibId = bibId,
        title = "挪威的森林",
        author = "村上春树",
        publisher = "上海译文出版社",
        pubYear = "2018",
        isbn = "9787532776771",
        coverUrl = "https://example.com/cover.jpg",
        addedAt = 1_700_000_000_000L
    )

    @Test
    fun `encode 输出固定的可读键名`() {
        val json = WishlistBackup.encode(listOf(entity("bib-1")), now = 123L)

        val expectedKeys = listOf(
            "\"version\"", "\"exportedAt\"", "\"items\"",
            "\"bibId\"", "\"title\"", "\"author\"", "\"publisher\"",
            "\"pubYear\"", "\"isbn\"", "\"coverUrl\"", "\"addedAt\""
        )
        expectedKeys.forEach { key ->
            assertTrue("备份文件缺少键 $key，实际内容：$json", json.contains(key))
        }
        // 混淆后的单字母键名绝不能出现在文件里
        assertFalse("备份文件里出现了混淆键名：$json", json.contains("\"a\":"))
        assertFalse("备份文件里出现了混淆键名：$json", json.contains("\"b\":"))
    }

    @Test
    fun `encode 后 decode 原样还原`() {
        val entries = listOf(entity("bib-1"), entity("bib-2"))

        val json = WishlistBackup.encode(entries, now = 123L)
        val decoded = WishlistBackup.decode(json).getOrThrow()

        assertEquals(entries, decoded.first)
        assertEquals(0, decoded.second)
    }

    @Test
    fun `decode 能读另一构建导出的可读键名文件`() {
        // 模拟 debug 包（不做混淆）导出的文件，缺的字段应当补空串而不是崩
        val json = """
            {
              "version": 1,
              "exportedAt": 1700000000000,
              "items": [
                {"bibId": "bib-9", "title": "海边的卡夫卡", "coverUrl": "https://example.com/c.jpg"},
                {"bibId": "  bib-10  "},
                {"title": "没有 bibId，应当被跳过"},
                {"bibId": "   "}
              ]
            }
        """.trimIndent()

        val (entries, dropped) = WishlistBackup.decode(json).getOrThrow()

        assertEquals(2, entries.size)
        assertEquals(2, dropped)
        assertEquals("bib-9", entries[0].bibId)
        assertEquals("海边的卡夫卡", entries[0].title)
        assertEquals("https://example.com/c.jpg", entries[0].coverUrl)
        assertEquals("", entries[0].author)
        assertEquals(0L, entries[0].addedAt)
        // bibId 前后的空白被裁掉
        assertEquals("bib-10", entries[1].bibId)
    }

    @Test
    fun `decode 缺少 items 时返回可读的错误而不是空指针`() {
        val result = WishlistBackup.decode("""{"version":1,"exportedAt":123}""")

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue("错误信息应当说明缺少书籍数据，实际：$message", message.contains("没有书籍数据"))
    }

    @Test
    fun `decode 空文件内容返回失败`() {
        assertTrue(WishlistBackup.decode("").isFailure)
        assertTrue(WishlistBackup.decode("null").isFailure)
    }
}
