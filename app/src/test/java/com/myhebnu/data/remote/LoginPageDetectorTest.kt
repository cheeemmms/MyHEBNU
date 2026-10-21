package com.myhebnu.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginPageDetectorTest {

    @Test
    fun `schedule page with login word in JS comments is not a login page`() {
        val schedulePage = """
            <html>
            <head><title>学生课表</title></head>
            <body>
              <div>课表</div>
              <script>
                if (ct.indexOf('text/html') >= 0) { // 会话过期返回登录页
                  alert('会话可能已过期，请重新登录后再下载');
                }
                // 非钉钉（普通浏览器）：同浏览器 Cookie 共享，直接打开即可，不会重新登录
              </script>
            </body>
            </html>
        """.trimIndent()

        assertFalse(LoginPageDetector.isLoginPage(schedulePage))
    }

    @Test
    fun `login form with yhm input is detected`() {
        val loginPage = """
            <form class="form-horizontal" action="/xtgl/login_slogin.html" method="post">
              <input type="text" class="form-control" name="yhm" id="yhm" value="">
            </form>
        """.trimIndent()

        assertTrue(LoginPageDetector.isLoginPage(loginPage))
    }

    @Test
    fun `login_slogin action alone is detected`() {
        val loginRedirect = "<script>location.href='/xtgl/login_slogin.html';</script>"

        assertTrue(LoginPageDetector.isLoginPage(loginRedirect))
    }

    @Test
    fun `unquoted yhm attribute is detected`() {
        assertTrue(LoginPageDetector.isLoginPage("<input name=yhm id=yhm>"))
    }

    @Test
    fun `empty body is not a login page`() {
        assertFalse(LoginPageDetector.isLoginPage(""))
    }
}
