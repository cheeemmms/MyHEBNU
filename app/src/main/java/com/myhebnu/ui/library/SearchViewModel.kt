package com.myhebnu.ui.library

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.remote.opac.OpacHotBook
import com.myhebnu.data.remote.opac.OpacSearchItem
import com.myhebnu.data.repository.OpacRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val results: List<OpacSearchItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val hasMore: Boolean = false,
    /** 总页数（按每页 20 条算），供页码翻页控件用。 */
    val totalPage: Int = 0,
    val suggestions: List<String> = emptyList(),
    val lastSearched: String = "",
    val history: List<String> = emptyList(),
    val hotBooks: List<OpacHotBook> = emptyList(),
    val newBooks: List<OpacSearchItem> = emptyList(),
    val idleLoading: Boolean = true,
    /** bibId → 封面地址；检索结果自带的 `thumb` 是空串，只能事后批量补。 */
    val covers: Map<String, String> = emptyMap(),
    /**
     * 一次性事件：新检索 / 翻页把结果换掉时置 true，列表滚回顶部后由 [SearchViewModel.onScrolledToTop] 清掉。
     * 用一次性标记而不是「记住上次的 page」——后者在页面被销毁重建后会误判，一进详情返回就跳回顶部。
     */
    val pendingScrollToTop: Boolean = false,
) {
    /** 还没搜过、输入框为空时显示「发现」区（历史 / 热门 / 新书）。 */
    val showDiscovery: Boolean get() = results.isEmpty() && !isSearching && query.isBlank()

    /** 正在改关键词（与上次已执行检索不同）时显示联想词。 */
    val isTyping: Boolean get() = query.isNotBlank() && query.trim() != lastSearched

    /** 只有真正执行过检索且确实没有命中，才说「没有找到」。 */
    val isEmptyResult: Boolean
        get() = results.isEmpty() && !isSearching && lastSearched.isNotBlank() &&
            query.trim() == lastSearched
}

/**
 * 图书检索。
 *
 * 命中结果来自 `POST search/`（ES 查询），`total` 是命中总数；
 * 输入过程中的联想词来自 `search/_suggest`，与正式检索是两条独立的请求。
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: OpacRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /**
     * 结果瀑布流的滚动状态。
     *
     * 放在 ViewModel 里而不是 `remember`：进书目详情时检索页会被销毁再重建，
     * 状态留在 VM 里，返回时才能停在离开时那一条，而不是回到顶部。
     */
    val gridState = LazyStaggeredGridState()

    private var suggestJob: Job? = null

    init {
        loadDiscovery()
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value, error = null) }

        suggestJob?.cancel()
        val keyword = value.trim()
        if (keyword.isEmpty()) {
            _uiState.update { it.copy(suggestions = emptyList()) }
            return
        }

        // 停顿 220ms 再请求，避免逐字打字把联想接口打爆
        suggestJob = viewModelScope.launch {
            delay(220)
            val list = repository.suggest(keyword).getOrNull().orEmpty()
            _uiState.update { current ->
                // 结果回来时用户可能已经改词了，只接受仍然匹配的那一份
                if (current.query.trim() == keyword) current.copy(suggestions = list) else current
            }
        }
    }

    fun clearQuery() {
        suggestJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                results = emptyList(),
                total = 0,
                page = 1,
                hasMore = false,
                totalPage = 0,
                suggestions = emptyList(),
                lastSearched = "",
                error = null,
            )
        }
    }

    /** 点联想词或历史记录直接发起检索。 */
    fun searchWith(query: String) {
        suggestJob?.cancel()
        _uiState.update { it.copy(query = query, suggestions = emptyList()) }
        search()
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return

        suggestJob?.cancel()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    error = null,
                    suggestions = emptyList(),
                    lastSearched = query
                )
            }

            val result = repository.search(query = query, page = 1, pageSize = PAGE_SIZE)

            _uiState.update { state ->
                result.fold(
                    onSuccess = { page ->
                        state.copy(
                            isSearching = false,
                            error = null,
                            results = page.items,
                            total = page.total,
                            page = 1,
                            hasMore = page.items.size < page.total,
                            totalPage = pagesOf(page.total),
                            pendingScrollToTop = true,
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            isSearching = false,
                            error = error.message ?: "检索失败",
                            results = emptyList(),
                            total = 0,
                            totalPage = 0,
                        )
                    },
                )
            }
            if (result.isSuccess) {
                loadCovers(_uiState.value.results)
                loadHistory()
            }
        }
    }

    /**
     * 翻到第 [target] 页（搜索引擎式页码翻页）。
     *
     * 每页只保留该页的 20 条，不再向前累加——累加式的「上滑加载更多」在服务端
     * 页数与 `total` 不一致时，会一直转圈却再也取不到新条目。
     */
    fun goToPage(target: Int) {
        val state = _uiState.value
        if (state.isSearching || state.isLoadingMore) return
        if (target < 1) return
        if (target == state.page && state.results.isNotEmpty()) return
        if (state.totalPage > 0 && target > state.totalPage) return
        requestPage(target)
    }

    /** 失败后的「重试」：重新拉当前页，而不是退回第一页。 */
    fun reloadCurrentPage() {
        val state = _uiState.value
        if (state.isSearching || state.isLoadingMore) return
        if (state.page <= 1) {
            search()
        } else {
            requestPage(state.page)
        }
    }

    private fun requestPage(target: Int) {
        val state = _uiState.value
        val query = state.lastSearched.ifBlank { state.query.trim() }
        if (query.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, error = null) }
            val result = repository.search(query = query, page = target, pageSize = PAGE_SIZE)

            _uiState.update { current ->
                result.fold(
                    onSuccess = { page ->
                        if (page.items.isEmpty() && target > 1) {
                            // 服务端没有这一页：保留当前结果，只提示一句，别把列表清空
                            current.copy(isLoadingMore = false, error = "这一页没有更多结果了")
                        } else {
                            current.copy(
                                isLoadingMore = false,
                                error = null,
                                results = page.items,
                                total = page.total,
                                page = target,
                                hasMore = page.items.size < page.total,
                                totalPage = pagesOf(page.total),
                                pendingScrollToTop = true,
                            )
                        }
                    },
                    onFailure = { error ->
                        current.copy(
                            isLoadingMore = false,
                            error = error.message ?: "翻页失败",
                        )
                    },
                )
            }
            if (result.isSuccess) loadCovers(_uiState.value.results)
        }
    }

    private fun pagesOf(total: Int): Int =
        if (total <= 0) 0 else (total + PAGE_SIZE - 1) / PAGE_SIZE

    /** 列表已经滚回顶部，消费掉一次性滚顶请求。 */
    fun onScrolledToTop() {
        if (_uiState.value.pendingScrollToTop) {
            _uiState.update { it.copy(pendingScrollToTop = false) }
        }
    }

    /** 离开检索页时清空检索状态，下次进来重新开始。 */
    fun reset() {
        suggestJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                isSearching = false,
                isLoadingMore = false,
                error = null,
                results = emptyList(),
                total = 0,
                page = 1,
                hasMore = false,
                totalPage = 0,
                suggestions = emptyList(),
                lastSearched = "",
            )
        }
        loadDiscovery()
    }

    private fun loadDiscovery() {
        viewModelScope.launch {
            _uiState.update { it.copy(idleLoading = true) }
            coroutineScope {
                val history = async { repository.searchHistory() }
                val hot = async { repository.hotBooks(5) }
                val newBooks = async { repository.newBooks(5) }

                val h = history.await().getOrNull().orEmpty().mapNotNull { it.query }
                val hotList = hot.await().getOrNull().orEmpty()
                val fresh = newBooks.await().getOrNull()?.items.orEmpty()

                _uiState.update {
                    it.copy(
                        idleLoading = false,
                        history = h,
                        hotBooks = hotList,
                        newBooks = fresh,
                    )
                }
                loadCovers(fresh)
            }
        }
    }

    /** 检索结果里的 `thumb` 恒为空串，封面只能按 bibId 事后批量补一次。 */
    private suspend fun loadCovers(items: List<OpacSearchItem>) {
        val bibIds = items.mapNotNull { it.bibId }.filter { it.isNotBlank() }.distinct()
        if (bibIds.isEmpty()) return
        val isbns = items.mapNotNull { it.isbn }.filter { it.isNotBlank() }
        val fetched = repository.covers(bibIds = bibIds, isbns = isbns).getOrNull().orEmpty()
        if (fetched.isEmpty()) return
        _uiState.update { it.copy(covers = it.covers + fetched) }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val history = repository.searchHistory().getOrNull().orEmpty().mapNotNull { it.query }
            _uiState.update { it.copy(history = history) }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
