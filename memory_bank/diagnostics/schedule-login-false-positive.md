# schedule-login-false-positive.md · 课表 Session 失效误报

- **用途**：记录“进入课表页报 Session 已失效”的根因、证据与修复方案。
- **阅读时机**：课表加载失败、重试/清数据无效、检查教务 HTML 守卫时。
- **最后更新**：2026-10-21
- **状态**：已修复（2026-10-21）；真机课表已正常加载

## 目录

- [现象](#现象)
- [证据](#证据)
- [根因](#根因)
- [修复内容](#修复内容)
- [波及范围](#波及范围)
- [验证方式](#验证方式)

## 现象

- 进入课表页后转圈多次，最终报：
  `Session 已失效，页面重定向到登录页`
- 点击重试无效。
- 清除应用数据、重新登录后仍然复现。
- 同一会话下，空教室/考试/成绩可正常访问（日志显示这些请求 200）。

## 证据

Logcat：

```text
loadSchedulePage -> 200, isLoginPage=true, size=31541
```

用手机自身出口 + App 当前 Cookie 重放同一请求（手机 `/system/bin/curl`）：

```text
HTTP=200 SIZE=33560
LEN=31541 LOGIN_COUNT=5
LOGIN_SLOGIN=False
HAS_KEBIAO=True
HAS_YHM=False
```

即：HTTP 200、页面是课表页（含“课表”）、没有登录表单、没有 `login_slogin`。页面里的 5 处“登录”全部来自 JavaScript 注释：

```javascript
if (ct.indexOf('text/html') >= 0) { // 会话过期返回登录页
    alert('会话可能已过期，请重新登录后再下载');
}
// ③ 非钉钉（普通浏览器）：同浏览器 Cookie 共享，直接打开即可，不会重新登录
```

## 根因

[ScheduleRepository.kt](../../app/src/main/java/com/myhebnu/data/repository/ScheduleRepository.kt) 第 72 行：

```kotlin
val isLogin = pageBody.contains("登录") || pageBody.contains("login_slogin")
```

教务课表页的静态 JavaScript 注释里包含“登录”二字，导致**正常课表页被误判为登录页**。因此：

- 重试 = 再次发同一请求 → 再次误判；
- 清数据/重新登录 = 会话已正常工作，但页面文本仍含“登录” → 仍误判；
- 同一会话下其他模块正常，因为它们的页面文本没有这类注释（但存在同样风险，见下）。

## 修复内容

新增 `LoginPageDetector`（`data/remote/LoginPageDetector.kt`），不使用裸 `"登录"`：

```kotlin
internal object LoginPageDetector {
    private val YHM_INPUT = Regex("""(?i)\b(?:name|id)\s*=\s*["']?yhm\b""")

    fun isLoginPage(html: String): Boolean {
        if (html.isBlank()) return false
        return html.contains("login_slogin") || YHM_INPUT.containsMatchIn(html)
    }
}
```

依据：登录页包含 `login_slogin`、`name="yhm"`、`id="yhm"`；课表页三者都没有，只有 JS 注释里的“登录”。

已替换：

1. `ScheduleRepository.refreshSchedule()` 的 `isLogin` 判定（本次故障点）。
2. `GradeRepository.getGrades()` 的同款判定。
3. `RoomRepository.queryEmptyRooms()` step 2 的同款判定。
4. `RoomRepository.isHtmlResponse()` 中的裸 `"登录"` 改为调用 detector。

同时新增单元测试 `LoginPageDetectorTest`（5 个用例），覆盖：

- 课表页 JS 注释含“登录” → false
- 登录表单 `name="yhm"` → true
- `login_slogin` 跳转 → true
- 无引号 `name=yhm` → true
- 空响应 → false

## 波及范围

| 位置 | 当前行为 | 风险 |
| --- | --- | --- |
| `ScheduleRepository.kt:72` | 已实际误报 | 高：清数据后课表不可用 |
| `GradeRepository.kt:56` | 当前正常 | 中：页面文本变化即可能误报 |
| `RoomRepository.kt:142` | 当前正常 | 中：同上 |
| `RoomRepository.kt:19` | HTML 守卫 | 低：`<html` 已覆盖，裸“登录”冗余 |

## 验证方式

1. 修复后安装 debug 包。
2. 保留当前会话，进入课表页：应显示课表，不再报 Session 失效。
3. logcat 应出现 `isLoginPage=false`。
4. 清除数据 → 重新登录 → 进课表，确认首次加载正常。
5. 进入成绩/空教室，确认原功能未回归。

### 实际结果（2026-10-21）

- `testDebugUnitTest`：5 tests / 0 failures / 0 errors。
- `assembleDebug`：`BUILD SUCCESSFUL in 27s`，APK 71,509,296 B。
- `adb install -r`：`Success`。
- Deep link 打开课表：`loadSchedulePage -> 200, isLoginPage=false, size=31541`，随后 `getSchedule -> 200, size=29242`，课表正常渲染（截图见 `logcat/schedule_fixed.png`，gitignore）。
- 保留当前会话已验证；清数据重登路径未重复执行（避免打断已登录会话），由 detector 单测覆盖页面判定。
