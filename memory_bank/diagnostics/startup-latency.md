# startup-latency.md · 启动转圈排查与优化

- **用途**：记录启动时转圈加载的内容、根因、修复方式与后续可选优化。
- **阅读时机**：再次出现启动等待、修改启动/会话逻辑、做性能验收时。
- **最后更新**：2026-10-21
- **状态**：已修复（代码已改，构建通过；真机耗时待验收）

## 目录

- [结论](#结论)
- [启动链路](#启动链路)
- [根因](#根因)
- [修复内容](#修复内容)
- [验证方式](#验证方式)
- [后续可选优化](#后续可选优化)
- [风险与取舍](#风险与取舍)

## 结论

启动转圈**不是首页数据加载**，而是 `MainActivity` 在显示界面前等待**教务会话网络校验**：

```text
currentScreen == null → CircularProgressIndicator
        ↓
authRepository.hasValidSession()
        ↓
GET http://jwgl.hebtu.edu.cn/xtgl/index_initMenu.html?jsdm=xs
        ↓
最多 15s 超时 / 校园网往返
```

同时 `LoginViewModel` 在 `MyHEBNUApp` 顶部被无条件创建，`init` 里又调用了一次 `hasValidSession()`，造成**同一启动路径上两次会话校验请求**。

首页 `HomeViewModel.loadHomeData()` 本身只读 DataStore + Room，不发教务请求；`checkForUpdateOnStart()` 是 fire-and-forget 的 GitHub 请求，不阻塞首帧。

## 启动链路

### 修复前

```text
MyHebnuApplication.onCreate()
  ├─ 创建通知渠道
  └─ 调度 3 个 WorkManager 周期任务
MainActivity.onCreate()
  └─ MyHEBNUApp()
       ├─ 创建 LoginViewModel（无条件）
       │    └─ init → checkLoginState() → hasValidSession() → 网络
       └─ LaunchedEffect
            ├─ preferences.isFirstLaunch.first()
            └─ hasValidSession()
                 ├─ cookieJar.hasStoredSession()（本地）
                 ├─ preferences.isLoggedIn.first()（本地）
                 └─ verifySessionValidity()（网络，15s 超时）
       └─ currentScreen == null → 转圈
```

### 修复后

```text
MainActivity.onCreate()
  └─ MyHEBNUApp()
       └─ LaunchedEffect
            ├─ combine(isFirstLaunch, isLoggedIn).first()（一次本地 DataStore 读取）
            └─ hasLocalCookieSession()（同步 SharedPreferences，无网络）
       └─ Welcome / Main / Login 立即决定
LoginViewModel 仅在进入 Login 页时创建；不再做服务端校验
```

## 根因

| # | 原因 | 影响 |
| --- | --- | --- |
| 1 | `verifySessionValidity()` 在首帧前请求 `index_initMenu.html` | 教务/校园网慢时转圈数秒；最坏 15s 超时 |
| 2 | `LoginViewModel` 无条件创建并重复调用 `hasValidSession()` | 启动时并发两次校验请求，互相竞争 |
| 3 | 会话过期时 `verifySessionValidity()` 内部 `logout()` 与 `sessionExpired` → `autoLogin()` 存在竞态 | 可能清掉凭证后再自动登录，导致失败回登录页 |
| 4 | `currentScreen=null` 没有品牌占位，只有转圈 | 等待被放大为“卡住”的感知 |

> 首页 Room/DataStore 读取通常在几十毫秒级，不是“数秒”的主要来源；更新检查也不阻塞首帧。

## 修复内容

### 1. 启动只做本地判断

`AuthRepository` 新增：

```kotlin
fun hasLocalCookieSession(): Boolean = cookieJar.hasStoredSession()
```

`MainActivity` 改为：

```kotlin
val (isFirstLaunch, isLoggedIn) = combine(
    preferences.isFirstLaunch,
    preferences.isLoggedIn
) { first, logged -> first to logged }.first()

currentScreen = when {
    isFirstLaunch -> AppScreen.Welcome
    isLoggedIn && authRepository.hasLocalCookieSession() -> AppScreen.Main
    else -> AppScreen.Login
}
```

- 不再在启动时调用 `hasValidSession()`。
- `hasValidSession()` 保留给显式校验场景，并在注释中标注“不要在启动调用”。

### 2. 移除 LoginViewModel 的重复校验

- `LoginViewModel` 不再调用 `hasValidSession()`，init 只回填加密凭证（本地）。
- `LoginViewModel` 只在 `AppScreen.Login` 分支由 `LoginScreen` 创建，Main 路径不再创建它。
- 登录成功仍由 `LoginScreen` 的 `onLoginSuccess` 回调切换；新增 `consumeLoginSuccess()` 清除 `isLoggedIn`，避免 Activity 级 ViewModel 在下次会话过期回登录页时立刻再次触发成功回调（附带修复）。

### 3. 会话校验改为惰性

不主动请求教务验证；当用户进入课表/成绩/考试等页面触发 API 时：

```text
AuthInterceptor 识别 302/401/403/901
    → sessionExpired
    → autoLogin()（用加密凭证）
    → 成功无感恢复 / 失败回登录页
```

这与 v1.3.1 的 901 自愈链路一致。首页/缓存页在离线或会话失效时仍可先展示本地数据。

## 验证方式

| 项 | 命令/步骤 | 结果 |
| --- | --- | --- |
| 编译 | `.\gradlew.bat assembleDebug` | 2026-10-21 通过（增量 18s，APK 71,509,476 B） |
| 冷启动计时 | `adb shell am start -W -n com.myhebnu.debug/com.myhebnu.MainActivity` | 2026-10-21：设备 24129PN74C / Android 17；冷启动 `TotalTime 528ms`、`WaitTime 554ms`（debug 首次安装、无本地会话 → 登录页） |
| 首帧日志 | `adb logcat -s MyHEBNU:*` | 启动阶段只有 `restoreFromStorage`，**没有** `hasValidSession`/`verifySessionValidity`；随后用户登录并正常访问空教室/考试/成绩 |
| 会话失效 | 使 Cookie 失效后进考试/成绩 | 待真机专项验证 |
| 离线启动 | 断网冷启动 | 待真机专项验证 |
| 登录态冷启动 | 登录后 force-stop 再 `am start -W` | 待复测（预期直接进首页，不经登录页） |

> 2026-10-21 已在真机安装并启动 debug 包，冷启动 528ms；本次样本是“无本地会话 → 登录页”，登录态首页路径仍需复测。启动日志已确认不再出现 `hasValidSession` 网络校验。

## 后续可选优化

只有真机测量仍显示明显等待时再做，避免过度设计：

1. **DataStore 单次快照**：`HomeViewModel` 目前顺序读取 7 个 DataStore 键；可合并为一次 `data.first()` 快照。预计节省几十毫秒。
2. **Room 查询并行**：`computeNextClass` 与 `computeNextExam` 可 `async` 并行，减少串行等待。
3. **启动占位**：把 `null` 分支的转圈改为静态品牌/骨架，纯感知优化。
4. **SplashScreen API**：用 `androidx.core:core-splashscreen` 保持系统启动图到本地判断完成，避免转圈。
5. **WorkManager 调度延后**：把 3 个周期任务的入队移到首帧后/IO 线程，减少 `Application.onCreate` 主线程工作。
6. **会话后台校验**：首页显示后静默校验并在失效时 autoLogin；会多一次网络请求，当前不采用，优先保持零启动网络。

## 风险与取舍

- **启动不再主动验证会话**：本地 Cookie 存在但服务端已失效时，首页会先显示缓存，直到首次 API 请求才自愈；这是离线优先的取舍。
- **Cookie 7 天年龄限制**：`SessionManager.hasValidSession()` 仍限制 7 天，超过后走登录页；不是本次改动引入。
- **登出竞态**：`hasValidSession()` 内部 `logout()` 的旧行为仍存在，但已不在启动路径；后续若恢复主动校验，应先拆分“验证”和“清理”。
- **安全项**：Cookie 明文、BODY 日志等仍按用户决定搁置，见 [security.md](../architecture/security.md)。
