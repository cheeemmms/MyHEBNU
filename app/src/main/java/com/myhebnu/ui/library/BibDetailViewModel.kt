package com.myhebnu.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.db.entity.WishlistEntity
import com.myhebnu.data.remote.opac.OpacHolding
import com.myhebnu.data.repository.BibDetailLoader
import com.myhebnu.data.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BibDetailUiState(
    /**
     * 当前这份数据属于哪本书。
     *
     * 界面必须靠它来判断「封面等信息是不是当前这本的」：换书的那一帧里，
     * 路由的 bibId 已经是 B，而 uiState 还停在 A，两者混用会把 A 的封面
     * 请求成 B 的封面（缓存键也跟着错）。
     */
    val bibId: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val title: String = "",
    val author: String = "",
    /** 出版者，展示在「出版信息」板块顶部。 */
    val publisher: String = "",
    val pubYear: String = "",
    val isbn: String = "",
    val coverUrl: String = "",
    val abstract: String = "",
    /** 编目字段（已剔除与顶部重复的项，见 [BibDetailViewModel.buildMarcFields]）。 */
    val marcFields: List<Pair<String, String>> = emptyList(),
    /** 馆藏，可借的排在前面。 */
    val holdings: List<OpacHolding> = emptyList(),
    val clickCount: Int = 0,
)

/**
 * 书目详情：书籍身份 + 馆藏分布 + 编目字段。
 *
 * `POST search/` 的 `holdings` 与 `bibs/{id}/holdings` 结构同构，但前者随检索结果一起返回；
 * 这里从详情接口重新取一次，保证「能不能借」是当下的真实值。
 *
 * 想看状态**不在这里保存**：它由 Room 的 Flow 驱动（[isWished]），
 * 这样从列表页移除一本书后回到本页，爱心会自动变回描边。
 */
@HiltViewModel
class BibDetailViewModel @Inject constructor(
    private val detailLoader: BibDetailLoader,
    private val wishlistRepository: WishlistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BibDetailUiState())
    val uiState: StateFlow<BibDetailUiState> = _uiState.asStateFlow()

    private val _isWished = MutableStateFlow(false)
    val isWished: StateFlow<Boolean> = _isWished.asStateFlow()

    private var loadedBibId: String? = null
    private var wishJob: Job? = null

    /**
     * 加载一本书。
     *
     * `hiltViewModel()` 在手写返回栈下拿到的始终是 Activity 级的同一个实例，
     * 所以换一本书进来时必须做两件事：
     * 1. **把上一本的状态整个丢掉**——否则界面会先按旧数据渲染一帧，
     *    表现为「打开 B 书时先闪一下 A 书的页面」；
     * 2. 重新订阅 [isWished]，爱心才不会是上一本的状态。
     *
     * @param initialCover 列表卡片上正在显示的封面地址。换书时不清掉它，
     *   详情接口回来之前页面就有图可画，容器变换不会中途露出底色。
     */
    fun load(
        bibId: String,
        isbn: String?,
        fallbackTitle: String?,
        initialCover: String = ""
    ) {
        if (bibId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "缺少书目标识") }
            return
        }

        val switchedBook = loadedBibId != bibId
        if (!switchedBook && !_uiState.value.isLoading) return
        loadedBibId = bibId

        if (switchedBook) {
            // 只保留路由带来的标题与封面做占位，其余全部清空
            _uiState.value = BibDetailUiState(
                isLoading = true,
                title = fallbackTitle.orEmpty(),
                coverUrl = initialCover
            )
            _isWished.value = false
        }

        observeWished(bibId)
        loadDetail(bibId, isbn, fallbackTitle)
    }

    /** 重新拉取（错误态下的「重试」）。 */
    fun retry() {
        val bibId = loadedBibId ?: return
        // 重试要绕开缓存，否则刚失败的 60 秒里会一直把同一条失败结果还回来
        loadDetail(bibId, null, _uiState.value.title, forceRefresh = true)
    }

    /**
     * 切换「想看」。
     *
     * 标题为空时直接返回——此时详情还没加载出来，落库只会得到一条没有书名的空记录，
     * 列表页会显示成「无题名」。界面上按钮同样是禁用的。
     */
    fun toggleWish() {
        val state = _uiState.value
        val bibId = loadedBibId ?: return
        if (state.title.isBlank()) return

        val entry = WishlistEntity(
            bibId = bibId,
            title = state.title,
            author = state.author,
            publisher = state.publisher,
            pubYear = state.pubYear,
            isbn = state.isbn,
            // 豆瓣那一路没给封面时，用 ISBN 拼图床地址兜底，与检索页同一套规则
            coverUrl = state.coverUrl.ifBlank { LibraryFormat.coverUrl(state.isbn) },
            addedAt = System.currentTimeMillis(),
        )
        viewModelScope.launch { wishlistRepository.toggle(entry) }
    }

    private fun observeWished(bibId: String) {
        wishJob?.cancel()
        wishJob = viewModelScope.launch {
            wishlistRepository.observeIsWished(bibId).collect { wished ->
                _isWished.value = wished
            }
        }
    }

    private fun loadDetail(
        bibId: String,
        isbn: String?,
        fallbackTitle: String?,
        forceRefresh: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    title = it.title.ifBlank { fallbackTitle.orEmpty() }
                )
            }

            // 预取在点击卡片时就发起过，这里大概率直接命中，转场期间内容已经齐了；
            // 没命中也只是复用同一条请求（BibDetailLoader 按 bibId 去重）。
            val result = detailLoader.load(bibId = bibId, isbn = isbn, forceRefresh = forceRefresh)

            _uiState.update { state ->
                result.fold(
                    onSuccess = { payload ->
                        val base = payload.bundle.base
                        val extra = payload.douban
                        state.copy(
                            bibId = bibId,
                            isLoading = false,
                            error = null,
                            title = base?.title?.takeIf { it.isNotBlank() }
                                ?: extra?.title?.takeIf { it.isNotBlank() }
                                ?: state.title,
                            author = base?.author.orEmpty(),
                            publisher = base?.publisher.orEmpty(),
                            pubYear = base?.pubYear.orEmpty(),
                            isbn = base?.isbn.orEmpty(),
                            // 豆瓣封面优先（尺寸大、适合做整屏背景），
                            // 缺失时退到汇文图床按 ISBN 拼的图，最后才留空
                            coverUrl = resolveUrl(extra?.imageUrl)
                                ?: LibraryFormat.coverUrl(base?.isbn).ifBlank { null }
                                ?: "",
                            abstract = extra?.content.orEmpty(),
                            marcFields = buildMarcFields(payload.bundle.fields),
                            // 可借排前面：读者打开这页只想先看到「哪儿能借到」
                            holdings = payload.holdings
                                .sortedByDescending { if (it.circStatus == 0) 1 else 0 },
                            clickCount = payload.bundle.clickCount,
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            // 失败也要认领这本书，否则界面的闸门会一直把它当成"别人的状态"，
                            // 错误提示就永远显示不出来
                            bibId = bibId,
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
     * 清洗规则：
     * 1. 丢掉已经在页面上单独展示过的字段：题名/责任者、ISBN及定价、出版发行项、载体形态项；
     * 2. 丢掉内容简介已经完整展示过的提要/文摘字段，避免同一个段落出现两遍；
     * 3. 丢掉没有 key 的裸值（汇文偶尔把一条附注单独塞进 map，会渲染成没有标签的行）。
     */
    private fun buildMarcFields(raw: Map<String, String>): List<Pair<String, String>> {
        val duplicatedWithAbstract = listOf("提要", "文摘", "简介", "摘要")
        return raw.entries
            .mapNotNull { (key, value) ->
                val label = key.trim()
                val text = stripHtml(value)
                if (label.isEmpty() || text.isBlank()) null else label to text
            }
            .filterNot { it.first in SKIP_LABELS }
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

    private companion object {
        /** 已在页面上单独渲染，不再重复出现在编目字段区。 */
        val SKIP_LABELS = setOf("题名/责任者", "ISBN及定价", "出版发行项", "载体形态项")
    }
}
