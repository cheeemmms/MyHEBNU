package com.myhebnu.data.remote

/**
 * Detects whether an EA HTML response is the login page.
 *
 * IMPORTANT: do not use a bare `contains("登录")`. The real schedule page
 * contains that word in JavaScript comments such as
 * `// 会话过期返回登录页`, which previously caused a false
 * "Session 已失效" failure even though the response was HTTP 200 and the
 * schedule body was valid.
 *
 * The login page uses `login_slogin` in its form action and has an `yhm`
 * username input. These markers are absent from the schedule/grade/room
 * pages, so they are a safe discriminator.
 */
internal object LoginPageDetector {

    private val YHM_INPUT = Regex("""(?i)\b(?:name|id)\s*=\s*["']?yhm\b""")

    fun isLoginPage(html: String): Boolean {
        if (html.isBlank()) return false
        return html.contains("login_slogin") || YHM_INPUT.containsMatchIn(html)
    }
}
