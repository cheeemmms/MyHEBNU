package com.myhebnu.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.remote.opac.OpacHolding
import com.myhebnu.data.repository.OpacRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BibDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val pubYear: String = "",
    val isbn: String = "",
    val callNo: String = "",
    val coverUrl: String = "",
    val abstract: String = "",
    val marcFields: List<Pair<String, String>> = emptyList(),
    val holdings: List<OpacHolding> = emptyList(),
    val availableCount: Int = 0,
    val borrowedCount: Int = 0,
    val readingCount: Int = 0,
    val clickCount: Int = 0,
)

/**
 * 书目详情：基础信息 + 馆藏分布 + 编目字段。
 *
 * `POST search/` 的 `holdings` 与 `bibs/{id}/holdings` 结构同构，但前者随检索结果一起返回；
 * 这里从详情接口重新取一次，保证「可借册数」是当下的真实值。
 */
@HiltViewModel
class BibDetailViewModel @Inject constructor(
    private val repository: OpacRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BibDetailUiState())
    val uiState: StateFlow<BibDetailUiState> = _uiState.asStateFlow()

    private var loadedBibId: String? = null

    fun load(bibId: String, isbn: String?, fallbackTitle: String?) {
        if (bibId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "缺少书目标识") }
            return
        }
        if (loadedBibId == bibId && !_uiState.value.isLoading) return
        loadedBibId = bibId

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null, title = fallbackTitle.orEmpty())
            }

            val result = runCatching {
                coroutineScope {
                    val detail = async { repository.bibDetail(bibId) }
                    val holdings = async { repository.holdings(bibId) }
                    val douban = async { repository.douban(isbn, bibId) }

                    val bundle = detail.await().getOrThrow()
                    val copies = holdings.await().getOrNull().orEmpty()
                    val extra = douban.await().getOrNull()

                    Triple(bundle, copies, extra)
                }
            }

            _uiState.update { state ->
                result.fold(
                    onSuccess = { (bundle, copies, extra) ->
                        val base = bundle.base
                        val available = copies.count { it.circStatus == 0 }
                        val borrowed = copies.count { it.circStatus == 1 }
                        val reading = copies.size - available - borrowed

                        state.copy(
                            isLoading = false,
                            error = null,
                            title = base?.title?.takeIf { it.isNotBlank() }
                                ?: extra?.title?.takeIf { it.isNotBlank() }
                                ?: state.title,
                            author = base?.author.orEmpty(),
                            publisher = base?.publisher.orEmpty(),
                            pubYear = base?.pubYear.orEmpty(),
                            isbn = base?.isbn.orEmpty(),
                            callNo = base?.callno?.firstOrNull().orEmpty(),
                            coverUrl = resolveUrl(extra?.imageUrl).orEmpty(),
                            abstract = extra?.content.orEmpty(),
                            marcFields = buildMarcFields(bundle.fields),
                            holdings = copies,
                            availableCount = available,
                            borrowedCount = borrowed,
                            readingCount = reading,
                            clickCount = bundle.clickCount,
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            isLoading = false,
                            error = error.message ?: "书目详情加载失败",
                        )
                    },
                )
            }
        }
    }

    /** `imageUrl` 可能是协议相对地址（`//img3m1.ddimg.cn/...`），必须补全后才能交给 Coil。 */
    private fun resolveUrl(url: String?): String? = when {
        url.isNullOrBlank() -> null
        url.startsWith("//") -> "https:$url"
        else -> url
    }

    /**
     * 编目字段的 key 是中文自由文本，value 内含 HTML（`<a style='color:#006fcc' ...>`）。
     *
     * 三条清洗规则：
     * 1. 丢掉与顶部标题区重复的字段（题名/责任者、ISBN 与定价）；
     * 2. 丢掉内容简介已经完整展示过的提要/文摘字段，避免同一个段落出现两遍；
     * 3. 丢掉没有 key 的裸值（汇文偶尔把一条附注单独塞进 map，会渲染成没有标签的行）。
     */
    private fun buildMarcFields(raw: Map<String, String>): List<Pair<String, String>> {
        val skip = setOf("题名/责任者", "ISBN及定价")
        val duplicatedWithAbstract = listOf("提要", "文摘", "简介", "摘要")
        return raw.entries
            .mapNotNull { (key, value) ->
                val label = key.trim()
                val text = stripHtml(value)
                if (label.isEmpty() || text.isBlank()) null else label to text
            }
            .filterNot { it.first in skip }
            .filterNot { (label, _) -> duplicatedWithAbstract.any { label.contains(it) } }
            .distinctBy { it.first }
            // 汇文偶尔把每个责任者后面都跟一个「著」，合并成一个
            .map { (label, value) -> label to value.replace(Regex("(著\\s*){2,}"), "著").trim() }
    }

    private fun stripHtml(html: String?): String {
        if (html.isNullOrBlank()) return ""
        val withoutPlaceholder = html
            .replace("{meta}", "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
        return withoutPlaceholder.trim().replace(Regex("\n{2,}"), "\n")
    }
}
