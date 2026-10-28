package com.myhebnu.data.remote.opac

import android.util.Log
import com.myhebnu.data.local.preferences.CredentialManager
import com.myhebnu.data.remote.sso.SsoLoginManager
import com.myhebnu.data.remote.sso.SsoOutcome
import com.myhebnu.data.remote.sso.SsoPrep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 图书馆（汇文 OPAC）会话管理。
 *
 * 学校统一身份认证后，教务与图书馆共用同一个 `cas.hebtu.edu.cn` 域下的 TGC：
 * 只要 [SsoLoginManager] 认证成功过一次，进入图书馆时 CAS 会直接下发 ticket，
 * **不需要再次输入密码**。所以这里不再自己实现 CAS 流程，只做三件事：
 *
 * 1. 用 [SsoLoginManager] 确保 CAS 会话存在（已有 TGC 时零交互）；
 * 2. 走一次 service 落地页，把 ticket 换成 OPAC 的会话 Cookie；
 * 3. 用 `users/info` 校验会话真的可用。
 */
@Singleton
class OpacAuthManager @Inject constructor(
    @Named("opac") private val client: OkHttpClient,
    private val credentialManager: CredentialManager,
    private val sso: SsoLoginManager
) {
    companion object {
        private const val TAG = "OpacAuth"
        private const val OPAC_BASE = "https://hwdw.hebtu.edu.cn/meta-local/opac/"
        private const val DEFAULT_BOARD_URL = "https://hwdw.hebtu.edu.cn/mspace/casBoard"
    }

    private val _state = MutableStateFlow(OpacAuthState.Unknown)
    val state: StateFlow<OpacAuthState> = _state.asStateFlow()

    /** 串行化登录，避免并发首屏请求同时触发多次 CAS 登录把会话踢掉 */
    private val loginMutex = Mutex()

    /** 供 UI 展示的一次性失败原因（CAS 页面的中文提示直接透传） */
    @Volatile
    var lastError: String? = null
        private set

    /**
     * 确保拥有可用的 OPAC 会话。
     *
     * @param force 忽略已知状态强制重登（接口返回未登录时使用）
     */
    suspend fun ensureLoggedIn(force: Boolean = false): Result<Unit> {
        if (!force && _state.value == OpacAuthState.LoggedIn) return Result.success(Unit)
        return loginMutex.withLock {
            if (!force && _state.value == OpacAuthState.LoggedIn) return@withLock Result.success(Unit)
            withContext(Dispatchers.IO) {
                runCatching { performLogin() }
                    .onSuccess {
                        _state.value = OpacAuthState.LoggedIn
                        lastError = null
                        Log.i(TAG, "图书馆会话建立成功")
                    }
                    .onFailure { throwable ->
                        _state.value = OpacAuthState.LoggedOut
                        lastError = throwable.message
                        Log.w(TAG, "图书馆登录失败: ${throwable.message}", throwable)
                    }
                    .map { }
            }
        }
    }

    /** 会话被判失效（接口返回未登录 / 401）时调用，下次请求会重新登录 */
    fun invalidate() {
        _state.value = OpacAuthState.LoggedOut
    }

    // ------------------------------------------------------------------
    // 登录实现
    // ------------------------------------------------------------------

    private suspend fun performLogin() {
        val credentials = credentialManager.loadCredentials()
            ?: throw OpacAuthException("尚未保存统一身份认证账号，请先在登录页登录一次")
        val (username, password) = credentials

        when (val prep = sso.prepare(SsoLoginManager.LIBRARY_SERVICE)) {
            // 教务那边已经登录过 → CAS 直接下发 ticket，这里无需再做任何事
            is SsoPrep.AlreadySignedIn -> Log.i(TAG, "复用已存在的统一身份认证会话")

            is SsoPrep.LoginForm -> {
                if (prep.needsCaptcha) {
                    throw OpacAuthException("统一身份认证当前要求输入验证码，请先在登录页登录一次")
                }
                when (val outcome = sso.attempt(prep, username, password, null)) {
                    is SsoOutcome.Success -> Unit
                    is SsoOutcome.Failure -> throw OpacAuthException(outcome.message)
                }
            }
        }

        // 落地页：CAS 回跳后返回带 board_url 的 HTML，访问一次 casBoard 建立 OPAC 会话
        val landing = sso.fetchText(SsoLoginManager.LIBRARY_SERVICE)
        val boardUrl = Jsoup.parse(landing).selectFirst("#board_url")?.attr("value")
            ?: DEFAULT_BOARD_URL
        sso.fetchText(boardUrl)

        verify()
    }

    /** 校验：users/info 返回 code=0 才算真的登录成功 */
    private suspend fun verify() {
        val request = Request.Builder()
            .url("${OPAC_BASE}users/info")
            .get()
            .header("Accept", "application/json, text/plain, */*")
            .header("X-Hw-Lang", "i18n.zh-cn")
            .header("X-Hw-explorer", "m")
            .build()
        val body = client.newCall(request).execute().use { it.body?.string().orEmpty() }
        val code = runCatching { JSONObject(body).optInt("code", -1) }.getOrDefault(-1)
        if (code != 0) {
            throw OpacAuthException("图书馆会话校验失败（code=$code），请稍后重试")
        }
    }
}

/** 统一身份认证失败时抛出，message 直接展示给用户 */
class OpacAuthException(message: String) : Exception(message)

enum class OpacAuthState { Unknown, LoggedIn, LoggedOut }
