# http901-session-expiry.md · HTTP 901 会话失效诊断

- **用途**：记录「HTTP 901」的根因、实测证据、修复方式与日志采集步骤。
- **阅读时机**：出现 901、会话反复失效、自动重登不生效、需要抓取会话日志时。
- **最后更新**：2026-10-21
- **状态**：已修复（v1.3.1）；本文保留诊断方法与证据

## 目录

- [结论](#结论)
- [现象](#现象)
- [根因链路](#根因链路)
- [实测日志证据](#实测日志证据)
- [为何偏偏考试/成绩页](#为何偏偏考试成绩页)
- [回退不能消除 901](#回退不能消除-901)
- [修复方式](#修复方式)
- [日志采集指南](#日志采集指南)

## 结论

901 **不是标准 HTTP 状态码**，而是 ZFSOFT 教务系统对 AJAX 接口返回的**自定义「会话失效」码**。根因是 `AuthInterceptor.isSessionExpired()` 只识别 302/401/403，漏掉 901，导致既不触发 `autoLogin()`、也不提示用户，只能清数据重登。

**修复（v1.3.1，commit `78a0f26`）**：在 `AuthInterceptor.isSessionExpired()` 增补 `response.code == 901`，命中即置 `sessionExpired=true`，复用现有自动重登链路。改动一处，覆盖所有模块。

## 现象

- 考试页、成绩页偶发报错「HTTP 901」（其他页面少见）。
- 清除应用数据 + 重新登录后消失。
- 该码不符合 HTTP 标准范围，但 OkHttp 会原样暴露为 `response.code`。

## 根因链路

1. 教务系统对带 `X-Requested-With: XMLHttpRequest` 的 AJAX 接口，会话失效时**不返回 302 跳转**（AJAX 不会被浏览器自动跳登录页），而是直接返回 **901** + JSON/HTML body。
2. `AuthInterceptor.isSessionExpired()` 只识别：
   - `302` 且 `Location` 含 `cas/login` 或 `login_slogin`
   - `401` / `403`
   - **没有 901** → 不置 `_sessionExpired = true`。
3. `MainActivity` 依赖 `sessionExpired` 流触发 `autoLogin()`；901 不触发 → 自动重登完全不生效。
4. 901 透传到数据层：`ExamRepository.getExams()` / `GradeRepository.getGrades()` / `getGradeDetail()` 的 `else` 分支拼成 `Result.failure("HTTP 901")`。
5. 用户只能「清除数据重登」拿新 `JSESSIONID/jw`，901 才消失。

## 实测日志证据

2026-08-30，adb 抓取，设备 `db072f50`（小米 24129PN74C / Android 16）。复现时考试接口（N358105）循环：

```text
loadForRequest:  .../kwgl/kscx_cxXsksxxIndex.html?doType=query&gnmkdm=N358105,
                 cookies=[rememberMe=deleteMe, JSESSIONID=E9FBF872E1F9272343DB462CCD990B90, jw=37559.57470.21071.0000]
saveFromResponse: .../N358105, cookies=[jw=37559.57470.21071.0000]   ← 服务端未续期 JSESSIONID
[Exam] HTTP 901
（同一循环每秒重复一次，持续刷屏）
```

三条证据：

1. Cookie 是带着的：每次请求都发送 `JSESSIONID + jw`，客户端以为会话有效。
2. 服务端判定已失效：N358105 返回 901，且响应只回 `jw`、不续期 `JSESSIONID`。
3. 全程没有 `session 过期` / `autoLogin` 日志：证明 901 未被拦截器识别，自愈链路对 901 完全失效。

## 为何偏偏考试/成绩页

- 课表/空教室接口在会话失效时，返回 302（被拦截器捕获 → autoLogin）或 HTML 错误页（被 HTML 守卫捕获 → 提示重登），走可自愈路径。
- 考试（N358105）/成绩（N305007）接口的未登录请求走 901 路径，且这两个页面在 v1.3.1 前是实时拉取、无 Room 缓存兜底。
- #30 后 `getAllGrades` 查询学期数增加，901 触发概率升高，但根因不变。

## 回退不能消除 901

v1.0.0 ~ v1.3.0 的 `AuthInterceptor` 均未识别 901；回退到任何旧 release 仍会出现 901。真正的修复是把 901 当作会话失效处理。

## 修复方式

```kotlin
// AuthInterceptor.isSessionExpired()
if (response.code == 901) {
    return true
}
```

- 命中后复用 `_sessionExpired` → `MainActivity` → `AuthRepository.autoLogin()`。
- 验证：使会话失效后进入考试/成绩页，日志应出现「检测到 session 过期，尝试自动登录」→「自动登录成功」，页面恢复而非 901。
- 相关约定：验证码重试时不要先 `logoutAccount`，否则会销毁验证码所属会话。

## 日志采集指南

App 日志 TAG 全为 `MyHEBNU`（含 Cookie 读写、请求步骤、响应码、autoLogin 结果）。

### 方式 A：adb 命令行（推荐）

1. 手机开启「开发者选项 → USB 调试」，连接电脑。
2. `adb logcat -c` 清空缓冲。
3. 开始录制：
   - PowerShell：`adb logcat -s MyHEBNU:* | Out-File 901.log -Encoding utf8`
   - cmd：`adb logcat -s MyHEBNU:* > 901.log`
4. 打开 App → 进考试/成绩页 → 复现 901。
5. `Ctrl+C` 停止，分析 `901.log`。

### 方式 B：Android Studio Logcat

1. 连接手机，AS 打开项目，底部 Logcat。
2. 过滤 `MyHEBNU`（包名 `com.myhebnu`）。
3. 清空 → 复现 → 复制日志。

### 复现要点

- 先清 logcat 再进页面，确保能同时看到：cookie 加载（`loadForRequest`）→ 请求步骤（`[Exam]`/`[Grade]` Step1/2/3）→ 响应码 → 是否有「session 过期 / autoLogin」。
- 若 `loadForRequest` 的 cookie 为空：会话根本没保存；若有 cookie 但仍是 901：cookie 已失效（本次实测属后者）。
- 无电脑时：Android 11+ 限制第三方 logcat App，基本抓不到；建议有电脑时抓。

> 安全提醒：日志会输出 Cookie 值。分享日志前先脱敏 `JSESSIONID`/`jw`/`rememberMe`。
