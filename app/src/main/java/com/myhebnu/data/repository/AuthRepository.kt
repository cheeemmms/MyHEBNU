package com.myhebnu.data.repository

import android.webkit.CookieManager
import com.google.gson.JsonObject
import com.myhebnu.data.local.preferences.CredentialManager
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.remote.CryptoUtil
import com.myhebnu.data.remote.PersistentCookieJar
import com.myhebnu.data.remote.interceptor.AuthInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles SSO authentication — both WebView-based (fallback) and custom login.
 *
 * Custom login flow (from HAR reverse-engineering):
 *   1. GET  login_slogin.html          → parse csrftoken from HTML
 *   2. GET  login_getPublicKey.html    → {modulus(base64), exponent(base64)}
 *   3. RSA encrypt password: base64(modulus)→hex→BigInteger→encrypt→hex→base64
 *   4. POST login_slogin.html          → yhm + mm(encrypted) + csrftoken
 *   5. 302 → success (cookies in response) / 200 → failure (HTML with error/captcha)
 */
@Singleton
class AuthRepository @Inject constructor(
    private val cookieJar: PersistentCookieJar,
    private val sessionManager: com.myhebnu.data.remote.SessionManager,
    private val preferences: UserPreferences,
    private val authInterceptor: AuthInterceptor,
    private val credentialManager: CredentialManager,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        // Mobile-friendly login page
        const val LOGIN_URL = "http://jwgl.hebtu.edu.cn/xtgl/login_slogin.html?ydType=0"

        // Success indicator URL after login completes
        const val LOGIN_SUCCESS_PATH = "/xtgl/index_initMenu.html"

        val LOGIN_SUCCESS_INDICATORS = listOf(LOGIN_SUCCESS_PATH)

        // Domain for cookie transfer
        const val JWGL_DOMAIN = "jwgl.hebtu.edu.cn"
        const val CAS_DOMAIN = "cas.hebtu.edu.cn"

        // Base URL for教务 system
        const val BASE_URL = "http://jwgl.hebtu.edu.cn/"
    }

    /** Expose session expiry signal so UI can trigger auto-login or re-login prompt */
    val sessionExpired: StateFlow<Boolean> = authInterceptor.sessionExpired

    /**
     * Separate OkHttpClient for login requests — no AuthInterceptor (avoids recursion),
     * follows redirects=false so we can manually extract cookies from 302 responses.
     * Shares the same CookieJar as the main client.
     */
    private val loginClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // ============================================================
    // Session validation
    // ============================================================

    suspend fun hasValidSession(): Boolean {
        val hasCookieSession = cookieJar.hasStoredSession()
        val isLoggedInPref = preferences.isLoggedIn.first()
        android.util.Log.w("MyHEBNU", "hasValidSession check: hasCookieSession=$hasCookieSession, isLoggedInPref=$isLoggedInPref")

        if (!hasCookieSession || !isLoggedInPref) {
            return false
        }

        return verifySessionValidity()
    }

    /**
     * Verify session by checking if教务 system accepts our cookies.
     * Fixed: with followRedirects=false, we check the Location header rather
     * than response.request.url (which always equals the original request URL).
     */
    private suspend fun verifySessionValidity(): Boolean = withContext(Dispatchers.IO) {
        try {
            android.util.Log.w("MyHEBNU", "正在验证 session 有效性...")
            val request = Request.Builder()
                .url("http://$JWGL_DOMAIN/xtgl/index_initMenu.html?jsdm=xs")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val location = response.header("Location") ?: ""
            // If session is expired, server redirects to login_slogin.html
            val isExpired = response.code == 302 && location.contains("login_slogin")
            val isSuccess = !isExpired

            android.util.Log.w("MyHEBNU",
                "Session 验证结果: isSuccess=$isSuccess, code=${response.code}, location=$location")

            if (!isSuccess) {
                logout()
            }

            response.close()
            isSuccess
        } catch (e: Exception) {
            android.util.Log.e("MyHEBNU", "验证 session 时出错", e)
            false
        }
    }

    // ============================================================
    // Custom login (new)
    // ============================================================

    /**
     * Parse csrftoken from the login page HTML.
     * The form contains: <input type="hidden" id="csrftoken" name="csrftoken" value="..."/>
     */
    fun parseCsrfToken(html: String): String? {
        return try {
            val doc = Jsoup.parse(html)
            val csrfInput = doc.selectFirst("input[name=csrftoken]")
            csrfInput?.attr("value")
        } catch (e: Exception) {
            android.util.Log.e("MyHEBNU", "解析 csrftoken 失败", e)
            null
        }
    }

    /**
     * Parse error message from a failed login response (200 with login page HTML).
     */
    fun parseLoginError(html: String): String? {
        return try {
            val doc = Jsoup.parse(html)
            val tipsElement = doc.selectFirst("#tips")
            if (tipsElement != null && tipsElement.text().isNotBlank()) {
                tipsElement.text()
            } else {
                // Fallback: check for alert-danger
                val alertElement = doc.selectFirst(".alert-danger")
                alertElement?.text()?.takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load captcha image bytes using loginClient (which shares our CookieJar),
     * so the server can associate the captcha answer with our login session.
     */
    suspend fun loadCaptchaImage(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${BASE_URL}kaptcha?time=${System.currentTimeMillis()}")
                .get()
                .build()
            val response = loginClient.newCall(request).execute()
            val bytes = response.body?.bytes()
            response.close()
            bytes
        } catch (e: Exception) {
            android.util.Log.e("MyHEBNU", "加载验证码失败", e)
            null
        }
    }

    /**
     * Execute a full custom login flow.
     *
     * @return LoginResult with success flag and optional error message
     */
    suspend fun performLogin(
        username: String,
        password: String,
        captcha: String? = null
    ): LoginResult = withContext(Dispatchers.IO) {
        try {
            // Step 0: Logout previous account on first attempt only.
            // Skip when retrying with captcha — logoutAccount destroys the session
            // that the captcha was generated under, causing "验证码错误".
            if (captcha == null) {
                android.util.Log.w("MyHEBNU", "Step 0: 退出上一个账号...")
                val logoutRequest = Request.Builder()
                    .url("${BASE_URL}xtgl/login_logoutAccount.html")
                    .header("User-Agent",
                        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/148.0.0.0 Mobile Safari/537.36")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Accept", "*/*")
                    .header("Origin", BASE_URL.trimEnd('/'))
                    .header("Referer", "${BASE_URL}xtgl/login_slogin.html")
                    .post(okhttp3.FormBody.Builder().build())
                    .build()
                val logoutResp = loginClient.newCall(logoutRequest).execute()
                logoutResp.close()
            } else {
                android.util.Log.w("MyHEBNU", "Step 0: 跳过 logoutAccount（验证码重试）")
            }

            // Step 1: GET login page → parse csrftoken
            android.util.Log.w("MyHEBNU", "Step 1: 获取登录页...")
            val loginPageRequest = Request.Builder()
                .url(LOGIN_URL)
                .get()
                .build()
            val loginPageResponse = loginClient.newCall(loginPageRequest).execute()
            val loginHtml = loginPageResponse.body?.string() ?: ""
            loginPageResponse.close()

            val csrfToken = parseCsrfToken(loginHtml)
            if (csrfToken == null) {
                android.util.Log.e("MyHEBNU", "无法解析 csrftoken")
                return@withContext LoginResult(false, "无法解析登录凭证，请用浏览器登录")
            }
            android.util.Log.w("MyHEBNU", "csrftoken: ${csrfToken.take(20)}...")

            // Step 2: GET RSA public key
            android.util.Log.w("MyHEBNU", "Step 2: 获取RSA公钥...")
            val pubKeyRequest = Request.Builder()
                .url("${BASE_URL}xtgl/login_getPublicKey.html?time=${System.currentTimeMillis()}")
                .get()
                .build()
            val pubKeyResponse = loginClient.newCall(pubKeyRequest).execute()
            val pubKeyJson = pubKeyResponse.body?.string() ?: ""
            pubKeyResponse.close()

            android.util.Log.w("MyHEBNU", "公钥响应: ${pubKeyJson.take(100)}")

            // Parse JSON manually (avoid Gson dependency for simple case)
            val modulus = extractJsonString(pubKeyJson, "modulus")
            val exponent = extractJsonString(pubKeyJson, "exponent")
            if (modulus == null || exponent == null) {
                android.util.Log.e("MyHEBNU", "无法解析RSA公钥")
                return@withContext LoginResult(false, "无法获取加密密钥，请重试")
            }

            // Step 3: Encrypt password
            val modulusHex = CryptoUtil.base64ToHex(modulus)
            val exponentHex = CryptoUtil.base64ToHex(exponent)
            val publicKey = CryptoUtil.parsePublicKey(modulusHex, exponentHex)
            val encryptedPassword = CryptoUtil.encryptPasswordToBase64(password, publicKey)
            android.util.Log.w("MyHEBNU", "密码加密完成")

            // Step 4: POST login
            val formBody = okhttp3.FormBody.Builder()
                .add("csrftoken", csrfToken)
                .add("language", "zh_CN")
                .add("ydType", "")
                .add("yhm", username)
                .add("mm", encryptedPassword)
                .apply {
                    if (!captcha.isNullOrBlank()) {
                        add("yzm", captcha)
                    }
                }
                .build()

            val loginRequest = Request.Builder()
                .url("${BASE_URL}xtgl/login_slogin.html?time=${System.currentTimeMillis()}")
                .header("User-Agent",
                    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/148.0.0.0 Mobile Safari/537.36")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "*/*")
                .header("Origin", BASE_URL.trimEnd('/'))
                .header("Referer", "${BASE_URL}xtgl/login_slogin.html")
                .post(formBody)
                .build()

            android.util.Log.w("MyHEBNU", "Step 4: 提交登录...")
            val loginResponse = loginClient.newCall(loginRequest).execute()

            android.util.Log.w("MyHEBNU",
                "登录响应: code=${loginResponse.code}, " +
                "headers=${loginResponse.headers.names()}")

            // Check result
            if (loginResponse.code == 302) {
                // Success! Cookies are already saved by CookieJar.saveFromResponse()
                val location = loginResponse.header("Location") ?: ""
                android.util.Log.w("MyHEBNU", "登录成功! 302 → $location")

                // Follow one more redirect to get final cookies
                loginResponse.close()
                completeLoginRedirectChain()

                // Persist credentials for auto-login
                credentialManager.saveCredentials(username, password)

                // Update login state
                preferences.setLoggedIn(true)
                authInterceptor.resetExpiredFlag()

                return@withContext LoginResult(true, null)
            } else {
                // Failure — parse error from HTML
                val responseBody = loginResponse.body?.string() ?: ""
                loginResponse.close()

                val errorMsg = parseLoginError(responseBody)
                    ?: "用户名或密码错误"
                android.util.Log.w("MyHEBNU", "登录失败: $errorMsg")

                // Check if captcha is now required
                val needsCaptcha = responseBody.contains("yzmDiv") ||
                    responseBody.contains("id=\"yzm\"")

                return@withContext LoginResult(false, errorMsg, needsCaptcha)
            }
        } catch (e: Exception) {
            android.util.Log.e("MyHEBNU", "登录异常", e)
            return@withContext LoginResult(false, "网络错误：${e.message}")
        }
    }

    /**
     * After a successful login POST (302), follow the redirect chain to
     * ensure all cookies are properly set in the CookieJar.
     *
     * HAR shows: POST login → 302 → /xtgl/login_slogin.html → 302 → /xtgl/index_initMenu.html
     */
    private fun completeLoginRedirectChain() {
        try {
            // Follow: GET login_slogin.html → should get 302 to index_initMenu.html
            val request = Request.Builder()
                .url("${BASE_URL}xtgl/login_slogin.html")
                .get()
                .build()
            val response = loginClient.newCall(request).execute()
            android.util.Log.w("MyHEBNU",
                "Redirect chain: code=${response.code}, location=${response.header("Location")}")
            response.close()
        } catch (e: Exception) {
            android.util.Log.w("MyHEBNU", "完成重定向链时出错（可能不影响登录）", e)
        }
    }

    /**
     * Auto-login using saved credentials. Called when session expiry (302) is detected.
     *
     * @return true if auto-login succeeded, false otherwise
     */
    suspend fun autoLogin(): Boolean {
        val credentials = credentialManager.loadCredentials()
        if (credentials == null) {
            android.util.Log.w("MyHEBNU", "autoLogin: 无已保存凭证")
            return false
        }

        val (username, password) = credentials
        android.util.Log.w("MyHEBNU", "autoLogin: 尝试自动登录 $username")

        val result = performLogin(username, password)
        if (result.success) {
            android.util.Log.w("MyHEBNU", "autoLogin: 成功!")
            authInterceptor.resetExpiredFlag()
            return true
        } else {
            android.util.Log.w("MyHEBNU", "autoLogin: 失败 — ${result.errorMessage}")
            // If auto-login fails due to wrong password, clear stored credentials
            if (result.errorMessage?.contains("密码") == true ||
                result.errorMessage?.contains("用户") == true
            ) {
                credentialManager.clearCredentials()
            }
            return false
        }
    }

    // ============================================================
    // WebView login (existing — fallback)
    // ============================================================

    suspend fun onWebViewLoginSuccess() {
        android.util.Log.w("MyHEBNU", "=== onWebViewLoginSuccess 开始 ===")
        val cookieManager = CookieManager.getInstance()

        val jwglCookies = cookieManager.getCookie("http://$JWGL_DOMAIN/") ?: ""
        val casCookies = cookieManager.getCookie("http://$CAS_DOMAIN/") ?: ""

        android.util.Log.w("MyHEBNU", "从WebView获取的cookies:")
        android.util.Log.w("MyHEBNU", "  jwglCookies=$jwglCookies")
        android.util.Log.w("MyHEBNU", "  casCookies=$casCookies")

        transferCookies(casCookies, CAS_DOMAIN)
        transferCookies(jwglCookies, JWGL_DOMAIN)

        val allCookies = mutableMapOf<String, String>()
        parseCookieString(jwglCookies).forEach { (name, value) ->
            allCookies["${JWGL_DOMAIN}_$name"] = value
        }
        parseCookieString(casCookies).forEach { (name, value) ->
            allCookies["${CAS_DOMAIN}_$name"] = value
        }

        android.util.Log.w("MyHEBNU", "准备保存到SessionManager的cookies: $allCookies")
        sessionManager.saveCookies(allCookies)

        preferences.setLoggedIn(true)
        authInterceptor.resetExpiredFlag()
        android.util.Log.w("MyHEBNU", "=== onWebViewLoginSuccess 完成 ===")
    }

    fun isLoginSuccessUrl(url: String): Boolean {
        return LOGIN_SUCCESS_INDICATORS.any { url.contains(it) }
    }

    fun getLoginUrl(): String = LOGIN_URL

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

    // ============================================================
    // Helpers
    // ============================================================

    /**
     * Extract a string value from a simple JSON object like {"key":"value"}.
     * Uses indexOf to avoid regex escaping issues.
     */
    private fun extractJsonString(json: String, key: String): String? {
        val searchKey = "\"$key\""
        val keyIndex = json.indexOf(searchKey)
        if (keyIndex < 0) return null
        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex < 0) return null
        val startQuote = json.indexOf('"', colonIndex)
        if (startQuote < 0) return null
        val endQuote = json.indexOf('"', startQuote + 1)
        if (endQuote < 0) return null
        return json.substring(startQuote + 1, endQuote)
    }

    private fun transferCookies(cookieString: String, domain: String) {
        val cookies = parseCookieString(cookieString)
        val url = "http://$domain/".toHttpUrl()
        for ((name, value) in cookies) {
            val cookie = Cookie.Builder()
                .name(name)
                .value(value)
                .domain(domain)
                .path("/")
                .build()
            cookieJar.saveFromResponse(url, listOf(cookie))
        }
    }

    private fun parseCookieString(cookieString: String): Map<String, String> {
        if (cookieString.isBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        cookieString.split(";").forEach { part ->
            val trimmed = part.trim()
            val eqIndex = trimmed.indexOf('=')
            if (eqIndex > 0) {
                val name = trimmed.substring(0, eqIndex)
                val value = trimmed.substring(eqIndex + 1)
                result[name] = value
            }
        }
        return result
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
