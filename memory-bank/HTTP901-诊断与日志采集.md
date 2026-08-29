# HTTP 901 报错 — 根因诊断与日志采集指南

> 勘察日期：2026-08-30 | 范围：考试页 / 成绩页偶发「HTTP 901」
> 状态：**已通过 adb 实测确认**（设备 `db072f50` 小米 24129PN74C / Android 16）

## 一、现象
- 考试页、成绩页偶发报错「HTTP 901」（其他页面少见）。
- 清除应用数据 + 重新登录后消失。
- 901 **不是标准 HTTP 状态码**（标准最大 599），是**教务系统（ZFSOFT 新方正）对 AJAX 接口返回的自定义响应码**，含义为「会话无效 / 未登录 / 权限校验未通过」。

## 二、根因（已定位，置信度 100%，有实测日志佐证）
完整链路：
1. 教务系统对**带 `X-Requested-With: XMLHttpRequest` 的 AJAX 接口**，会话失效时**不返回 302 跳转**（AJAX 不会被浏览器自动跳登录页），而是直接返回自定义状态码 **901** + JSON/HTML body。
2. `AuthInterceptor.isSessionExpired()` 只识别 `302(→cas/login|login_slogin)`、`401`、`403`，**完全没有识别 901** → 不会置 `_sessionExpired = true`。
3. `MainActivity` 靠 `sessionExpired` 流触发 `autoLogin()`（无感重登）。901 没触发该流 → **自动重登机制完全不生效**。
4. 901 透传到数据层：`ExamRepository.getExams():71-72` / `GradeRepository.getGrades():41` / `getGradeDetail():73` 的 `else` 分支 `Result.failure("HTTP ${code}")` → 「HTTP 901」。
5. 用户输入「清除数据重登」→ 重新 `performLogin` 拿新 `JSESSIONID/jw` → 901 消失。

**结论：根因是认证拦截器漏识别 901，导致会话失效时既不自动重登、也不提示，只能手动清数据。** 这是认证逻辑缺口，与具体业务页面无关。

## 三、实测日志证据（2026-08-30，adb 抓取，设备 db072f50）
复现 901 时 `adb logcat -s MyHEBNU:*` 抓到的关键循环（考试接口 N358105）：

```
loadForRequest:  .../kwgl/kscx_cxXsksxxIndex.html?doType=query&gnmkdm=N358105,
                 cookies=[rememberMe=deleteMe, JSESSIONID=E9FBF872E1F9272343DB462CCD990B90, jw=37559.57470.21071.0000]
saveFromResponse: .../N358105, cookies=[jw=37559.57470.21071.0000]   ← 服务端未续期 JSESSIONID
[Exam] HTTP 901
（同一循环每秒重复一次，持续刷屏）
```

**三铁证：**
1. **cookie 是带着的**——每次请求都发送 `JSESSIONID + jw`，客户端以为会话仍有效。
2. **服务端判定它已失效**——N358105 返回 901 且响应里**只回 `jw`、不续期 `JSESSIONID`**，即服务端已不认此会话。
3. **全程没有任何 `session 过期` / `autoLogin` 日志**——证明 901 未被 `AuthInterceptor` 识别，自愈链路（`MainActivity` → `autoLogin()`）**对 901 完全失效**，于是无限循环 901，直到清数据重登拿新 `JSESSIONID`。

## 四、为何偏偏考试/成绩页
- 课表/空教室接口在会话失效时，教务系统返回的是 302（被拦截器捕获 → autoLogin 恢复）或 HTML 错误页（被 HTML 守卫捕获 → 提示重登），走「可自愈」路径。
- 考试（N358105）/成绩（N305007）接口的未登录请求走的是 **901 路径**，且这两个页面是「实时拉取、无 Room 缓存兜底」，所以用户只能在此看到 901。
- 另：#30 后 `getAllGrades` 由查 4 学期增至 6 学期（3 学年 × [3,12]），成绩页请求次数变多，901 触发概率可能随之升高（但根因不变）。

## 五、重要提醒：回退不会消除 901
检查所有 release 版本（v1.0.0 ~ v1.3.0），`AuthInterceptor` 从未识别 901（#14 只修了 302 的 autoLogin）。因此**回退到任何已有 release，901 依旧会出现**。回退只能得到一个功能更少的稳定版，并不能根治此问题。
真正的修复是让 `AuthInterceptor` 把 901 当作会话失效处理（详见第六节，供后续修复参考，本次勘察未改代码）。

## 六、后续修复方向（参考，本次未改代码）
在 `AuthInterceptor.isSessionExpired()` 增补 901 识别（必要时用 `response.peekBody()` 读取 body 判断「登录 / 无功能权限」字样），命中即 `_sessionExpired = true`，复用现有 `autoLogin()` 流程即可一处修复所有模块。

## 七、日志采集指南（复现时抓取）
App 代码已内置详尽日志，**TAG 全为 `MyHEBNU`**（含 cookie 读写、各请求步骤、响应码、autoLogin 结果），只需过滤此 TAG 即可。

### 方式 A：adb 命令行（信息最全，推荐）
1. 手机开启「开发者选项 → USB 调试」，连电脑。
2. 打开终端，先清空缓冲区：`adb logcat -c`
3. 开始录制（重定向到文件）：
   - bash / cmd：`adb logcat -s MyHEBNU:* > 901.log`
   - PowerShell：`adb logcat -s MyHEBNU:* | Out-File 901.log -Encoding utf8`
4. 打开 App → 进入考试/成绩页 → 复现「HTTP 901」。
5. `Ctrl+C` 停止，把 `901.log` 发给我。

### 方式 B：Android Studio Logcat 面板
1. 连手机，AS 打开本项目，底部切到 **Logcat**。
2. 过滤框输入 `MyHEBNU`（包名可填 `com.myhebnu`）。
3. 点清空图标 → 复现 901 → 全选复制日志发我。

### 无电脑时
Android 11+ 限制第三方 logcat App，基本无法抓到；建议在有电脑时按方式 A 抓取。

### 复现要点
- 建议**先清 logcat 再进页面**，保证日志里能同时看到：cookie 加载（`loadForRequest`）→ 请求步骤（`[Exam]`/`[Grade]` Step1/2/3）→ 响应码 901 → 是否有「session 过期 / autoLogin」字样。
- 若日志里 `loadForRequest` 的 cookie 为空，说明会话根本没保存；若有 cookie 但仍是 901，说明 cookie 已失效——本次实测属于后者，正好印证根因。
