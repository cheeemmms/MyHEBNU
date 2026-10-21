# security.md · 安全项清单

- **用途**：集中记录 MyHEBNU 的威胁模型、已识别安全问题、影响与修复优先级。
- **阅读时机**：发布前安全检查、处理凭证/日志/备份/网络问题、评估隐私风险。
- **最后更新**：2026-10-21
- **状态**：初稿（基于 v1.5.0 代码静态审查；未做渗透测试；**修复已暂时搁置**）

> 2026-10-21 用户决定暂时搁置安全项修复。本文仅作备查与恢复依据，不代表当前迭代任务。

## 目录

- [威胁模型](#威胁模型)
- [结论摘要](#结论摘要)
- [详细发现](#详细发现)
- [优先级与修复建议](#优先级与修复建议)
- [发布前检查清单](#发布前检查清单)
- [历史记录](#历史记录)

## 威胁模型

| 项 | 说明 |
| --- | --- |
| 保护对象 | 学号/密码、教务会话 Cookie、成绩/课表/姓名等学业数据、签名密钥 |
| 信任边界 | 设备本地（应用私有目录）、教务 HTTP 链路、GitHub HTTPS、用户主动分享的日志/备份 |
| 攻击者 | 同网段中间人（校园网/公共 Wi-Fi）、拿到设备或备份的人、root/调试设备上的其他进程、日志接收者、恶意 App |
| 不在范围 | 教务服务端被攻破、学校账号本身被盗、物理拆机取证、国家级攻击 |

> 本项目是单用户客户端，不运营服务器；最大风险来自**明文 HTTP**、**本地持久化**和**日志/备份泄露**。

## 结论摘要

| # | 问题 | 严重度 | 现状 |
| --- | --- | --- | --- |
| 1 | 教务全链路明文 HTTP，RSA 公钥也经 HTTP 获取 | 高 | 受学校系统限制；可收紧配置与提示，无法在客户端根治 |
| 2 | 会话 Cookie 以普通 SharedPreferences 明文保存 | 高 | 可修：加密存储或不再持久化 |
| 3 | `HttpLoggingInterceptor.Level.BODY` 全构建开启，且代码打印完整 Cookie | 高 | 可修：仅 Debug / 脱敏 |
| 4 | `allowBackup=true` 且无备份排除规则 | 中 | 可修：关闭或排除敏感文件 |
| 5 | `network_security_config` 全局允许明文，范围过宽 | 中 | 可修：默认禁止，仅放行教务域名 |
| 6 | Room/DataStore 中学业数据明文 | 低-中 | 应用私有目录；可接受，视隐私要求 |
| 7 | 密码可逆存储用于自动重登 | 中 | 设计取舍；依赖 Keystore，无生物识别 |
| 8 | 登出不清课表/成绩缓存 | 低 | 共享设备隐私风险 |
| 9 | `SCHEDULE_EXACT_ALARM` 权限未使用 | 低 | 可移除 |
| 10 | 任意 App 可通过深链拉起内部路由 | 低 | 仅导航，无数据泄露；可加白名单 |
| 11 | 未启用 `FLAG_SECURE` | 低 | 可选：登录/成绩页防截屏 |
| 12 | 无依赖锁定/签名校验/CI 安全扫描 | 低 | 工程项 |

## 详细发现

### 1. 明文 HTTP（高）

- `NetworkModule.BASE_URL = "http://jwgl.hebtu.edu.cn/"`，CAS 也是 HTTP；`AndroidManifest.xml` 声明 `usesCleartextTraffic="true"`。
- 登录流程先用 HTTP 取 RSA 公钥，再加密密码。**公钥本身可被中间人替换**，因此 RSA 加密不能替代 TLS：攻击者可返回自己的公钥，解出密码。
- 会话 Cookie（`JSESSIONID`/`jw`）同样明文传输，可被同网段嗅探/劫持。
- `network_security_config.xml` 的 `<base-config cleartextTrafficPermitted="true" />` 让全应用允许明文，范围大于教务域名。

**可做**：把 `base-config` 改为 `false`，仅对 `jwgl.hebtu.edu.cn`/`cas.hebtu.edu.cn` 开明文；确认教务是否支持 HTTPS（若支持则直接切换）；对用户提示避免公共 Wi-Fi。**不能做**：在 HTTP 上实现可靠的证书固定。

### 2. Cookie 明文持久化（高）

- `PersistentCookieJar` 注释称“encrypted storage”，实际 `SessionManager` 使用普通 `SharedPreferences`（`myhebnu_session`），保存 `JSESSIONID`、`jw`、`rememberMe` 与时间戳。
- 拿到设备备份、root 权限或调试权限的人可读取 Cookie，在有效期内冒充会话。
- `SessionManager.hasValidSession()` 只看 7 天年龄，不做服务端校验；服务端失效前本地仍认为有效。

**可做**：迁移到 `EncryptedSharedPreferences`（与 `CredentialManager` 同套 MasterKey）；或改为只存“是否登录”标记，Cookie 不落盘（代价是重启后需重新登录）；迁移时兼容旧键并清理明文文件。

### 3. 敏感日志（高）

- `NetworkModule` 无条件 `HttpLoggingInterceptor.Level.BODY`，会输出请求/响应体（含登录 HTML、成绩、Cookie）。
- `PersistentCookieJar.loadForRequest/saveFromResponse` 打印完整 Cookie 列表；`AuthRepository` 打印 `csrftoken` 前缀、公钥响应、登录响应头。
- Android 现代版本限制第三方 App 读 logcat，但 **adb/USB 调试、bugreport、用户主动分享日志、崩溃采集** 都可能带走这些内容。

**可做**：`level = if (BuildConfig.DEBUG) BODY else NONE`；删除或脱敏 Cookie/`csrftoken` 日志；日志分享前提示脱敏。

### 4. 备份暴露（中）

- `AndroidManifest.xml`：`android:allowBackup="true"`，未配置 `dataExtractionRules`/`fullBackupContent`。
- 云备份/`adb backup` 可能包含 DataStore（姓名、学期、首页均分）、Room（课表/成绩）、`myhebnu_session`（Cookie）。`myhebnu_credentials` 是密文，且 Keystore 主密钥通常不随备份走，恢复后可能不可解密，但密文仍会进入备份。
- 换机恢复可能把旧会话/数据带到新设备。

**可做**：`allowBackup=false`，或添加规则排除 `myhebnu_session`、`myhebnu_credentials`、`user_preferences`、`databases/`；至少排除凭证与会话。

### 5. 明文配置范围过宽（中）

见第 1 条：`base-config` 全开 + 域配置重复。建议：

```xml
<base-config cleartextTrafficPermitted="false" />
<domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">jwgl.hebtu.edu.cn</domain>
    <domain includeSubdomains="true">cas.hebtu.edu.cn</domain>
</domain-config>
```

同时评估是否还需要 `android:usesCleartextTraffic="true"`（有 network config 时通常可去掉）。

### 6. 本地学业数据明文（低-中）

- Room（课程/考试/成绩）与 DataStore（姓名、学期、均分）未加密；位于应用私有目录。
- 未 root 的设备上其他 App 无法直接读；root、备份、调试、取证可读。
- 这些数据敏感度低于密码/Cookie；是否加密取决于隐私要求。若加密，Room 可用 SQLCipher，但会增加体积与迁移成本。

### 7. 密码可逆存储（中，设计取舍）

- 自动重登必须拿到明文密码（教务没有 OAuth/Token），因此 `CredentialManager` 用 EncryptedSharedPreferences 保存可逆密码。
- 安全性依赖 Android Keystore + 设备锁屏；没有生物识别门控、没有 StrongBox 要求。
- 设备已解锁且被攻破时，攻击者可解密。可选：自动重登前要求生物识别（会增加使用摩擦）；或提供“不保存密码”开关（关闭自动重登）。

### 8. 登出不清缓存（低）

- `logout()` 清凭证/Cookie/登录标记，但保留 Room/DataStore 的课表、成绩、姓名。
- 共享设备上，下一位用户打开 App 可能看到上一位的学业数据（需重新登录才能刷新，但缓存仍在）。
- **可做**：登出时提供“同时清除本地数据”，或默认清 Room；至少文档说明。

### 9. 权限最小化（低）

- `SCHEDULE_EXACT_ALARM` 已声明但代码未使用（提醒走 WorkManager 周期任务），可移除。
- `POST_NOTIFICATIONS` 必需；`INTERNET`/`ACCESS_NETWORK_STATE` 必需。
- 无存储/定位/通讯录等敏感权限。

### 10. 深链与导出组件（低）

- `MainActivity` `exported=true`（启动器必需），并接收 `myhebnu://navigate/{route}`；任意 App 可拉起内部页面。
- 目前只按字符串路由导航，无敏感参数、无越权数据读取；但仍建议对 route 做白名单校验，避免未来加入带参路由时被滥用。
- Widget receiver 均 `exported=false`，正确。

### 11. 截屏/最近任务（低）

- 未设置 `FLAG_SECURE`；登录页、成绩页可能被截屏或出现在最近任务预览。
- 可按需对登录/成绩页加 `FLAG_SECURE`，代价是用户无法截图分享。

### 12. 供应链与发布（低-中）

- Release 开启 `minifyEnabled`/`shrinkResources`，方向正确；确认 release 不带 `debuggable=true`。
- `local.properties` 含 keystore 路径/密码，已 gitignore；`git ls-files local.properties` 为空，未入库。
- 无依赖锁定/校验、无 CI 安全扫描；建议后续加 Dependabot/依赖校验。
- 更新检查只比较版本号，下载 APK 由用户手动完成；不校验 APK 签名/哈希，存在钓鱼/降级风险（属分发流程，不是 App 运行时漏洞）。

## 优先级与修复建议

| 优先级 | 动作 | 影响 | 工作量 |
| --- | --- | --- | --- |
| P0 | 发布构建关闭 BODY 日志；移除 Cookie/`csrftoken` 明文日志 | 直接消除日志泄露 | 小 |
| P0 | Cookie 改加密存储或不持久化 | 防止会话被备份/root 读取 | 中 |
| P0 | 收紧 `network_security_config`（base=false，仅教务域名） | 缩小明文面 | 小 |
| P1 | 配置备份排除/关闭 `allowBackup` | 防止云备份带走数据 | 小 |
| P1 | 移除未使用的 `SCHEDULE_EXACT_ALARM` | 权限最小化 | 小 |
| P2 | 登出可选清除本地数据 | 共享设备隐私 | 小 |
| P2 | 深链 route 白名单 | 防御未来带参路由 | 小 |
| P2 | 登录/成绩页 `FLAG_SECURE` | 防截屏 | 小 |
| P3 | Room 加密/生物识别门控 | 高隐私场景 | 大 |

> 明文 HTTP 是学校系统限制，客户端只能收紧配置与提示；不要在 HTTP 上假装“已加密”。密码 RSA 加密不解决公钥被替换的问题。

## 发布前检查清单

- [ ] `BuildConfig.DEBUG=false` 时 HTTP 日志为 `NONE`/`BASIC`
- [ ] 日志中不再出现完整 Cookie、`JSESSIONID`、`jw`、密码、`csrftoken`
- [ ] `myhebnu_session` 不再明文保存 Cookie（或确认接受风险）
- [ ] `network_security_config` 的 `base-config` 为 `false`
- [ ] `allowBackup` 与备份规则已评审
- [ ] 未使用权限已移除
- [ ] `local.properties`/keystore 未入库；keystore 有离线备份
- [ ] Release `debuggable=false`、签名校验通过
- [ ] 版本发布说明提醒用户从官方 GitHub 下载

## 历史记录

- **2026-10-21**：首次系统盘点（基于 v1.5.0 静态审查）。确认：凭证加密、release minify、`local.properties` 未入库、Widget receiver 未导出是做得好的部分；主要风险集中在明文 HTTP、Cookie 明文持久化、BODY 日志、备份规则。未做动态渗透测试。
