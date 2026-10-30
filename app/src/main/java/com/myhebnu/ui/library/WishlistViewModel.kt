package com.myhebnu.ui.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.db.entity.WishlistEntity
import com.myhebnu.data.repository.OpacRepository
import com.myhebnu.data.repository.WishlistBackup
import com.myhebnu.data.repository.WishlistImportResult
import com.myhebnu.data.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WishlistItemUi(
    val entry: WishlistEntity,
    /** 优先用刚刷新到的封面；刷新失败或还没回来时退回收藏时的快照。 */
    val coverUrl: String
)

data class WishlistUiState(
    val isLoading: Boolean = true,
    val items: List<WishlistItemUi> = emptyList()
)

/**
 * 「我想看的书」。
 *
 * 列表本身完全来自本地数据库（Room 的 Flow），进页面即可渲染，不等网络。
 * 封面另走一条支路：拿到全部 bibId 后批量查一次汇文的封面接口，
 * 结果**只放内存不回写数据库**——封面地址是第三方图床的临时链接，
 * 回写等于把易变数据当永久数据存下来，而且失败时也没有副作用。
 */
@HiltViewModel
class WishlistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wishlistRepository: WishlistRepository,
    private val opacRepository: OpacRepository,
) : ViewModel() {

    /** bibId → 最近一次联网取到的封面地址。 */
    private val _refreshedCovers = MutableStateFlow<Map<String, String>>(emptyMap())

    private val _isLoading = MutableStateFlow(true)

    /** 导入/导出的结果提示，由界面转成 Toast。 */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val uiState: StateFlow<WishlistUiState> = combine(
        wishlistRepository.observeAll(),
        _refreshedCovers,
        _isLoading
    ) { entries, covers, loading ->
        WishlistUiState(
            isLoading = loading,
            items = entries.map { entry ->
                WishlistItemUi(
                    entry = entry,
                    coverUrl = covers[entry.bibId]?.takeIf { it.isNotBlank() }
                        ?: entry.coverUrl
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WishlistUiState()
    )

    private var coversRequestedFor: Set<String> = emptySet()

    init {
        /**
         * 封面刷新挂在数据流上，而不是在界面里调一次。
         *
         * 因为进页面那一刻 Room 的 Flow 还没发出第一帧，此时若直接读列表会是空的，
         * 批量请求就不会发生。放在这里还能顺带覆盖「新加入的书」。
         */
        viewModelScope.launch {
            wishlistRepository.observeAll().collect { entries ->
                val ids = entries.map { it.bibId }
                _isLoading.value = false
                if (ids.isEmpty() || ids.toSet() == coversRequestedFor) return@collect
                coversRequestedFor = ids.toSet()
                // 失败静默：封面只是锦上添花，断网时列表照样能读
                opacRepository.covers(bibIds = ids).onSuccess { map ->
                    _refreshedCovers.value = _refreshedCovers.value + map
                }
            }
        }
    }

    fun remove(bibId: String) {
        viewModelScope.launch { wishlistRepository.remove(bibId) }
    }

    /**
     * 导出到用户选定的文件。
     *
     * 用 SAF 给的 `content://` 直接写，不申请存储权限、也不碰具体路径。
     */
    fun export(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val entries = wishlistRepository.allEntries()
                val json = WishlistBackup.encode(entries, System.currentTimeMillis())
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri)
                        ?: error("无法写入所选文件")
                    stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
                entries.size
            }
            _messages.tryEmit(
                result.fold(
                    onSuccess = { count -> "已导出 $count 本" },
                    onFailure = { "导出失败：${it.message ?: "无法写入文件"}" }
                )
            )
        }
    }

    /**
     * 从用户选定的文件导入，按 bibId 合并（本地已有的跳过）。
     *
     * 整个流程包在 runCatching 里：文件被改坏、不是本应用导出的 JSON、
     * 权限被回收……都只会得到一条提示，不会把页面炸掉。
     */
    fun import(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val json = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: error("无法读取所选文件")
                    stream.bufferedReader().use { it.readText() }
                }
                val decoded = WishlistBackup.decode(json).getOrThrow()
                val (entries, dropped) = decoded
                WishlistImportResult(
                    added = wishlistRepository.mergeAll(entries),
                    skippedInFile = dropped
                )
            }
            _messages.tryEmit(
                result.fold(
                    onSuccess = { r ->
                        if (r.added > 0) "已导入 ${r.added} 本" else "没有新增条目（都已在列表中）"
                    },
                    onFailure = { "导入失败：${it.message ?: "文件格式不正确"}" }
                )
            )
        }
    }
}
