package com.myhebnu.data.remote.sso

import android.util.Log
import com.myhebnu.data.remote.PersistentCookieJar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 校园统一身份认证（CAS）客户端。
 *
 * 学校把教务系统的入口换成了 CAS 标准的 `sso/zfiotlogin`，图书馆（汇文 OPAC）同样走这套
 * 认证。两者共用同一个 `cas.hebtu.edu.cn` 域下的 TGC，因此**只需要成功认证一次**，
 * 另一个系统就能凭 TGC 直接拿到 ticket，无需再次输入密码。
 *
 * 认证流程（逐跳抓包确认）：
 * ```
 * GET  <service-url>                     → 302 cas.hebtu.edu.cn/cas/login?service=...
 * GET  /cas/login?service=...            → 登录页，隐藏字段 execution / _eventId
 * GET  /cas/v2/getKaptchaStatus          → "true" / "false"，是否需要图形验证码
 * GET  /cas/kaptcha?time=<ms>            → 验证码图片（jpeg，4 位数字）
 * GET  /cas/v2/getPubKey                 → {"modulus": .., "exponent": ..}
 * POST /cas/login?v=<random>             → 表单提交，password 经 RSA 加密
 *      302 → <service-url>?ticket=ST-... → 302 目标系统 → 会话建立
 * ```
 *
 * 两个必须注意的细节：
 * 1. CAS 的密码**不是**标准 PKCS#1，而是正方 `RSAUtils` 那套自定义封装（先整串反转、
 *    再按 16 位小端字打包、无 PKCS#1 填充头、输出 hex）。详见 [ZfsoftRsa]。用错算法
 *    会一直提示「密码错误，请重新输入」。
 * 2. 登录失败时 CAS 返回 **200 + 完整登录页**（带新的 execution 和 `<span id="msg">`
 *    错误文案），所以失败后必须重新解析 execution 才能重试。
 */
@Singleton
class SsoLoginManager @Inject constructor(
    private val cookieJar: PersistentCookieJar
) {
    companion object {
        private const val TAG = "SsoLogin"

        /** 教务系统（智慧校园 SSO 入口，不是 /xtgl/login_slogin.html） */
        const val EDU_SERVICE = "http://jwgl.hebtu.edu.cn/sso/zfiotlogin"

        /** 图书馆（汇文 OPAC 的 CAS 入口） */
        const val LIBRARY_SERVICE = "https://hwdw.hebtu.edu.cn/meta-local/opac/cas/rosetta"

        private const val UA =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/148.0.0.0 Mobile Safari/537.36"

        private const val MAX_HOPS = 10

        /** 统一身份认证所在的域名，重新登录前要先清掉这里的旧会话 */
        private const val SSO_HOST = "cas.hebtu.edu.cn"
    }

    /**
     * 独立于教务客户端的 OkHttp：不能带 [com.myhebnu.data.remote.interceptor.AuthInterceptor]
     * （它会强塞教务 Referer，并把 CAS 的 302 误判成教务会话过期），也不自动跟随跳转
     * （每一跳都要让 CookieJar 接管 Set-Cookie）。
     */
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // ------------------------------------------------------------------
    // 第一步：准备登录表单（或发现已有会话）
    // ------------------------------------------------------------------

    /**
     * 打开 [service] 对应的登录页，解析出本次认证需要的 execution 与验证码状态。
     *
     * 如果 CAS 域下已有有效的 TGC，服务端会直接放行，此时返回 [SsoPrep.AlreadySignedIn]。
     */
    suspend fun prepare(service: String): SsoPrep = withContext(Dispatchers.IO) {
        val first = prepareOnce(service)
        if (first is SsoPrep.AlreadySignedIn) {
            return@withContext first
        }

        // 走到这里说明 CAS 域下没有可用的 TGC，必须重新输密码。此时若沿用上一轮
        // 失败/过期留下的 JSESSIONID，服务端会认为这份流程早已结束，表现为
        // 「登录页面闲置时间过长，请打开新页面」或干脆回「密码错误」。
        // 所以先丢掉该域的旧 Cookie，再用干净会话取一份新表单。
        val cleared = cookieJar.removeHost(SSO_HOST)
        if (!cleared) {
            return@withContext first
        }

        val second = prepareOnce(service)
        if (second is SsoPrep.LoginForm) {
            Log.i(TAG, "已丢弃过期的 CAS 会话，改用新会话的表单")
            second
        } else {
            first
        }
    }

    private suspend fun prepareOnce(service: String): SsoPrep {
        val response = follow(
            Request.Builder().url(service).get().header("User-Agent", UA).build()
        )
        response.use { page ->
            val html = page.body?.string().orEmpty()
            val finalUrl = page.request.url
            val casBase = casBaseOf(finalUrl)
            val execution = Jsoup.parse(html).selectFirst("input[name=execution]")?.attr("value")

            if (execution.isNullOrBlank() || casBase == null) {
                Log.i(TAG, "已有统一身份认证会话，直接复用: $finalUrl")
                return SsoPrep.AlreadySignedIn
            }

            val needsCaptcha = kaptchaRequired(casBase)
            Log.i(TAG, "登录页就绪: base=$casBase, 需要验证码=$needsCaptcha")
            return SsoPrep.LoginForm(
                service = service,
                casBase = casBase,
                execution = execution,
                needsCaptcha = needsCaptcha
            )
        }
    }

    // ------------------------------------------------------------------
    // 第二步：提交账号密码
    // ------------------------------------------------------------------

    /**
     * 用 [prep] 里的 execution 提交一次认证。
     *
     * 成功时返回 [SsoOutcome.Success]（TGC 已在 CookieJar 里）；
     * 失败时返回 [SsoOutcome.Failure]，其中的 [SsoOutcome.Failure.retry] 是**新的**
     * 登录表单状态（CAS 每次失败都会换 execution，并可能刚刚开启验证码）。
     */
    suspend fun attempt(
        prep: SsoPrep.LoginForm,
        username: String,
        password: String,
        authcode: String?
    ): SsoOutcome = withContext(Dispatchers.IO) {
        val encrypted = try {
            val pubJson = fetchText("${prep.casBase}v2/getPubKey")
            val pub = JSONObject(pubJson)
            // 只记录指纹，不记录密码本身：用来核对「App 发出的明文」与「用户在别处的密码」是否同一个串
            Log.i(TAG, "提交认证: 明文长度=${password.length}, 指纹=${fingerprint(password)}")
            ZfsoftRsa.encrypt(
                plainText = password,
                modulusHex = pub.getString("modulus"),
                exponentHex = pub.optString("exponent", "10001")
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取 RSA 公钥失败", e)
            return@withContext SsoOutcome.Failure(
                "无法获取加密密钥，请检查网络后重试",
                resetForm(prep)
            )
        }

        val form = FormBody.Builder()
            .add("username", username)
            .add("password", encrypted)
            .add("authcode", authcode.orEmpty())
            .add("rememberMe", "true")
            .add("execution", prep.execution)
            .add("_eventId", "submit")
            .build()

        val origin = prep.casBase.toHttpUrlOrNull()?.let { "${it.scheme}://${it.host}" }
        val request = Request.Builder()
            .url("${prep.casBase}login?v=0.${System.nanoTime()}")
            .post(form)
            .header("User-Agent", UA)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .apply {
                if (origin != null) header("Origin", origin)
                header("Referer", "${prep.casBase}login")
            }
            .build()

        val response = follow(request)
        response.use { result ->
            val finalUrl = result.request.url
            val body = result.body?.string().orEmpty()

            // 仍停在 CAS 登录页 = 被拒绝（账号密码错 / 验证码错 / 验证码过期）
            if (isCasLoginPage(finalUrl)) {
                val hint = Jsoup.parse(body)
                    .selectFirst("#msg, #errormsg, .error, .alert-danger")
                    ?.text()
                    ?.trim()
                    ?.ifBlank { null }
                Log.w(TAG, "统一身份认证失败: ${hint ?: "未知原因"}")
                return@use SsoOutcome.Failure(
                    hint ?: "统一身份认证失败，请检查学号与密码",
                    resetForm(prep)
                )
            }

            Log.i(TAG, "统一身份认证成功: $finalUrl")
            SsoOutcome.Success
        }
    }

    // ------------------------------------------------------------------
    // 辅助：验证码 / 文本抓取
    // ------------------------------------------------------------------

    /** 明文指纹（SHA-256 前 12 位 hex），用于排查「到底发出去的是哪个串」而不泄露密码 */
    private fun fingerprint(text: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
        return digest.take(6).joinToString("") { "%02x".format(it) }
    }

    /** 重新拉一张验证码图（用户点「换一张」时用） */
    suspend fun refreshCaptcha(casBase: String): ByteArray? = withContext(Dispatchers.IO) {
        fetchKaptcha(casBase)
    }

    /** 抓取一个地址的响应文本，自动跟随跳转 */
    suspend fun fetchText(url: String): String = withContext(Dispatchers.IO) {
        val response = follow(Request.Builder().url(url).get().header("User-Agent", UA).build())
        response.use { it.body?.string().orEmpty() }
    }

    /**
     * 失败后重新准备一份表单。正常路径下直接重新访问 service；
     * 若服务端此时已放行（极少见），退回一个空表单让调用方按「已登录」处理。
     */
    private suspend fun resetForm(previous: SsoPrep.LoginForm): SsoPrep {
        val fresh = runCatching { prepare(previous.service) }.getOrNull()
        return if (fresh is SsoPrep.LoginForm) fresh else SsoPrep.AlreadySignedIn
    }

    private suspend fun kaptchaRequired(casBase: String): Boolean = runCatching {
        val raw = fetchText("${casBase}v2/getKaptchaStatus").trim()
        Log.i(TAG, "getKaptchaStatus=$raw")
        raw == "true"
    }.getOrDefault(false)

    private fun fetchKaptcha(casBase: String): ByteArray? = runCatching {
        val request = Request.Builder()
            .url("${casBase}kaptcha?time=${System.currentTimeMillis()}")
            .get()
            .header("User-Agent", UA)
            .header("Accept", "image/*")
            .build()
        client.newCall(request).execute().use { it.body?.bytes() }
    }.onFailure { Log.w(TAG, "加载验证码失败", it) }.getOrNull()

    /** 从 CAS 的 URL 推出 `.../cas/` 前缀，例如 http://cas.hebtu.edu.cn/cas/ */
    private fun casBaseOf(url: HttpUrl): String? {
        val path = url.encodedPath
        val index = path.indexOf("/cas/")
        if (index < 0) return null
        val prefix = path.substring(0, index + "/cas/".length)
        return url.newBuilder().encodedPath(prefix).query(null).build().toString()
    }

    private fun isCasLoginPage(url: HttpUrl): Boolean =
        url.encodedPath.contains("/cas/login")

    /** 手动跟随跳转：每一跳都要经过 CookieJar，且需要在中途读到 200 的登录页 */
    private fun follow(request: Request, maxHops: Int = MAX_HOPS): Response {
        var current = request
        var hops = 0
        while (true) {
            val response = client.newCall(current).execute()
            // 只记录跳转路径与状态码，不记录查询串（里面有 ticket / execution 这类一次性令牌）
            Log.w(
                TAG,
                "HTTP ${response.code} ${current.method} ${current.url.encodedPath}" +
                    (response.header("Location")?.let { " → ${it.substringBefore('?')}" } ?: "")
            )
            if (!response.isRedirect || hops >= maxHops) return response
            val location = response.header("Location") ?: return response
            val next = current.url.resolve(location) ?: return response
            response.close()
            current = Request.Builder()
                .url(next)
                .get()
                .header("User-Agent", UA)
                .build()
            hops++
        }
    }
}

/** [SsoLoginManager.prepare] 的结果 */
sealed interface SsoPrep {
    /** CAS 域下已有有效会话（TGC），无需输入密码 */
    data object AlreadySignedIn : SsoPrep

    /** 需要填写账号密码的表单状态 */
    class LoginForm(
        val service: String,
        val casBase: String,
        val execution: String,
        val needsCaptcha: Boolean
    ) : SsoPrep
}

/** [SsoLoginManager.attempt] 的结果 */
sealed interface SsoOutcome {
    data object Success : SsoOutcome

    data class Failure(
        val message: String,
        /** 失败后重新解析出的表单状态，可直接用于下一次重试 */
        val retry: SsoPrep
    ) : SsoOutcome
}
