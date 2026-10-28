package com.myhebnu.data.repository

import android.webkit.CookieManager
import com.myhebnu.data.local.preferences.CredentialManager
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.remote.PersistentCookieJar
import com.myhebnu.data.remote.interceptor.AuthInterceptor
import com.myhebnu.data.remote.sso.SsoLoginManager
import com.myhebnu.data.remote.sso.SsoOutcome
import com.myhebnu.data.remote.sso.SsoPrep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 教务系统认证。
 *
 * 学校已经把教务入口统一到 CAS（`sso/zfiotlogin`），旧的 `xtgl/login_slogin.html`
 * 自定义登录链路已废弃——现在教务与图书馆共用同一套统一身份认证，认证一次两边通用。
 *
 * 登录流程：
 * ```
 * GET  http://jwgl.hebtu.edu.cn/sso/zfiotlogin   → 302 CAS 登录页
 * （SsoLoginManager 负责 CAS 交互：execution / 验证码 / RSA 加密 / 提交）
 * 302 /sso/zfiotlogin?ticket=ST-...              → /ticketlogin?uid=..&verify=..
 *                                                → /xtgl/index_initMenu.html?jsdm=xs
 * ```
 * 本类只负责「认证 → 落盘凭证 → 校验教务会话 → 清状态」这一层编排。
 */
@Singleton
class AuthRepository @Inject constructor(
    private val cookieJar: PersistentCookieJar,
    private val sessionManager: com.myhebnu.data.remote.SessionManager,
    private val preferences: UserPreferences,
    private val authInterceptor: AuthInterceptor,
    private val credentialManager: CredentialManager,
    private val okHttpClient: OkHttpClient,
    private val sso: SsoLoginManager
) {
    companion object {
        private const val TAG = "MyHEBNU"

        const val JWGL_DOMAIN = "jwgl.hebtu.edu.cn"
        const val CAS_DOMAIN = "cas.hebtu.edu.cn"

        /** 教务系统根地址 */
        const val BASE_URL = "http://jwgl.hebtu.edu.cn/"

        /** 教务会话探针：未登录时会被 302 到登录页 */
        const val SESSION_PROBE_URL = "${BASE_URL}xtgl/index_initMenu.html?jsdm=xs"
    }

    /** Expose session expiry signal so UI can trigger auto-login or re-login prompt */
    val sessionExpired: StateFlow<Boolean> = authInterceptor.sessionExpired

    /** 最近一次登录页的 `.../cas/` 前缀，供「换一张验证码」复用 */
    @Volatile
    private var casBase: String? = null

    /**
     * 缓存已准备好的登录表单。
     *
     * 必须缓存：验证码是**会话绑定**的，如果提交前又去重开一次登录页，
     * 服务端会重新生成验证码，用户眼睛看到的和提交的那张就对不上了。
     * 反过来，缓存也绝不能按时间自动失效——那等于在用户提交前偷偷换掉验证码。
     * 只有「一次认证结束」（成功或拿到新的 execution）时才替换它。
     */
    @Volatile
    private var pendingForm: SsoPrep.LoginForm? = null

    private fun cacheForm(form: SsoPrep.LoginForm) {
        pendingForm = form
        casBase = form.casBase
    }

    // ============================================================
    // Session validation
    // ============================================================

    /**
     * Local-only startup check — no network.
     *
     * Use this to decide whether to show the cached home immediately or the login
     * screen. Server-side validity is checked lazily by [AuthInterceptor] on the
     * first API call, where the existing autoLogin() path can recover the session.
     */
    fun hasLocalCookieSession(): Boolean {
        return cookieJar.hasStoredSession()
    }

    /**
     * Full check: local cookie + server validation.
     *
     * Do NOT call this during startup: it performs a blocking network round-trip
     * (up to 15s) and is also responsible for clearing an expired session.
     */
    suspend fun hasValidSession(): Boolean {
        val hasCookieSession = cookieJar.hasStoredSession()
        val isLoggedInPref = preferences.isLoggedIn.first()
        if (!hasCookieSession || !isLoggedInPref) return false
        return verifySessionValidity()
    }

    /**
     * Verify session by checking if教务 system accepts our cookies.
     * With followRedirects=false we look at the Location header.
     */
    private suspend fun verifySessionValidity(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(SESSION_PROBE_URL).get().build()
            val response = okHttpClient.newCall(request).execute()
            val location = response.header("Location") ?: ""
            val code = response.code
            response.close()
            // 改走统一身份认证后，未登录不只跳 login_slogin，也可能跳 sso/zfiotlogin 或 cas/login
            val isExpired = (code == 302 || code == 301) && (
                location.contains("login_slogin") ||
                    location.contains("zfiotlogin") ||
                    location.contains("cas/login")
                )
            android.util.Log.w(TAG, "会话探针: code=$code, location=${location.take(80)}, 过期=$isExpired")
            if (isExpired) logout()
            !isExpired
        } catch (e: Exception) {
            android.util.Log.e(TAG, "验证 session 时出错", e)
            false
        }
    }

    // ============================================================
    // 登录页准备（验证码）
    // ============================================================

    /**
     * 登录页出现时调用一次：丢掉旧会话，取一份全新的表单并判断是否需要验证码。
     *
     * 登录页在进入时就应该知道要不要验证码——否则用户提交后才发现少填一项，
     * 体验很差。所以 UI 每次进入登录页都调一次这个方法。
     *
     * @return true 表示需要验证码
     */
    suspend fun startLoginSession(): Boolean = withContext(Dispatchers.IO) {
        pendingForm = null
        runCatching {
            when (val prep = sso.prepare(SsoLoginManager.EDU_SERVICE)) {
                is SsoPrep.AlreadySignedIn -> false
                is SsoPrep.LoginForm -> {
                    cacheForm(prep)
                    prep.needsCaptcha
                }
            }
        }.onFailure { android.util.Log.w(TAG, "预取登录页失败", it) }
            .getOrDefault(false)
    }

    /**
     * 取一份可用的登录表单：优先用已缓存的（保住用户看到的验证码），
     * 只有缓存为空时才重新打开登录页。
     */
    private suspend fun resolveForm(): SsoPrep {
        pendingForm?.let { return it }
        return when (val prep = sso.prepare(SsoLoginManager.EDU_SERVICE)) {
            is SsoPrep.LoginForm -> {
                cacheForm(prep)
                prep
            }

            is SsoPrep.AlreadySignedIn -> prep
        }
    }

    /**
     * Load captcha image bytes. 验证码与会话绑定，所以必须复用同一个 CookieJar
     * （[SsoLoginManager] 内部共享 [PersistentCookieJar]）。
     *
     * 这里**只取图，绝不重新打开登录页**：重开就会换会话、换验证码，
     * 用户看到的那张立刻作废。
     */
    suspend fun loadCaptchaImage(): ByteArray? = withContext(Dispatchers.IO) {
        val base = casBase ?: return@withContext null
        sso.refreshCaptcha(base)
    }

    // ============================================================
    // 登录
    // ============================================================

    /**
     * 执行一次完整的统一身份认证登录。
     *
     * @param captcha 验证码；不需要验证码时传 null
     */
    suspend fun performLogin(
        username: String,
        password: String,
        captcha: String? = null
    ): LoginResult = withContext(Dispatchers.IO) {
        try {
            if (password != password.trim()) {
                return@withContext LoginResult(
                    success = false,
                    errorMessage = "密码开头或结尾有空格，请检查后重试",
                    needsCaptcha = pendingForm?.needsCaptcha ?: false
                )
            }

            android.util.Log.w(
                TAG,
                "开始统一身份认证: 学号长度=${username.length}, 密码长度=${password.length}, " +
                    "带验证码=${!captcha.isNullOrBlank()}, 表单=${if (pendingForm == null) "重新获取" else "复用缓存"}"
            )

            when (val prep = resolveForm()) {
                // TGC 还有效，直接复用会话
                is SsoPrep.AlreadySignedIn -> return@withContext finishLogin(username, password)

                is SsoPrep.LoginForm -> {
                    if (prep.needsCaptcha && captcha.isNullOrBlank()) {
                        // 表单是刚取的，用户还没见过这张图，让 UI 立刻补一张
                        return@withContext LoginResult(false, "请输入验证码", needsCaptcha = true)
                    }
                    when (val outcome = sso.attempt(prep, username, password, captcha)) {
                        is SsoOutcome.Success -> finishLogin(username, password)
                        is SsoOutcome.Failure -> {
                            // CAS 每次失败都会换 execution，缓存也要跟着换
                            val retry = outcome.retry
                            if (retry is SsoPrep.LoginForm) cacheForm(retry) else pendingForm = null
                            LoginResult(
                                success = false,
                                errorMessage = outcome.message,
                                needsCaptcha = retry is SsoPrep.LoginForm
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "登录异常", e)
            LoginResult(false, "网络错误：${e.message}")
        }
    }

    /** CAS 已放行后的收尾：校验教务会话 → 存凭证 → 置登录态 */
    private suspend fun finishLogin(username: String, password: String): LoginResult {
        if (!verifySessionValidity()) {
            return LoginResult(
                false,
                "统一身份认证已通过，但教务系统未接受该会话，请稍后重试"
            )
        }
        credentialManager.saveCredentials(username, password)
        preferences.setLoggedIn(true)
        // 这之后保存的密码才是统一身份认证密码，静默重登才会被允许
        preferences.setSsoCredentialsMigrated(true)
        authInterceptor.resetExpiredFlag()
        return LoginResult(true, null)
    }

    /**
     * Auto-login using saved credentials. Called when session expiry is detected.
     *
     * @return true if auto-login succeeded, false otherwise
     */
    suspend fun autoLogin(): Boolean {
        // 升级到统一身份认证之前保存的密码来自教务旧登录页，可能已经不对。
        // 拿它静默重登只会白白消耗一次 CAS 失败次数（连续失败会开启验证码），
        // 因此这里直接放弃，交给登录页让用户输入正确密码。
        if (!preferences.ssoCredentialsMigrated.first()) {
            android.util.Log.w(TAG, "autoLogin: 凭证尚未迁移到统一身份认证，跳过静默重登")
            return false
        }

        val credentials = credentialManager.loadCredentials()
        if (credentials == null) {
            android.util.Log.w(TAG, "autoLogin: 无已保存凭证")
            return false
        }

        val (username, password) = credentials
        android.util.Log.w(TAG, "autoLogin: 尝试自动登录 $username")

        // 静默自动登录不能有验证码；学校目前是「连续失败才要求验证码」，
        // 正常使用时不会触发。万一真的要求了，如实报错而不是静默失败。
        val result = performLogin(username, password)
        if (result.success) {
            authInterceptor.resetExpiredFlag()
            return true
        }
        android.util.Log.w(TAG, "autoLogin: 失败 — ${result.errorMessage}")
        if (result.errorMessage?.contains("密码") == true ||
            result.errorMessage?.contains("用户") == true ||
            result.errorMessage?.contains("学号") == true
        ) {
            credentialManager.clearCredentials()
        }
        return false
    }

    suspend fun logout() {
        credentialManager.clearCredentials()
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        cookieJar.clearAll()
        preferences.setLoggedIn(false)
        authInterceptor.resetExpiredFlag()
    }

    suspend fun handleSessionExpired(): SessionExpiryAction {
        return if (cookieJar.hasStoredSession()) {
            SessionExpiryAction.SilentRetry
        } else {
            SessionExpiryAction.RequireLogin
        }
    }
}

data class LoginResult(
    val success: Boolean,
    val errorMessage: String?,
    val needsCaptcha: Boolean = false
)

enum class SessionExpiryAction {
    SilentRetry,
    RequireLogin
}
