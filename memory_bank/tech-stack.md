# tech-stack.md · 技术栈与环境

- **用途**：记录技术选型、精确版本、可执行命令、本机环境与编码规范。
- **阅读时机**：配环境、跑构建/测试、新增依赖、判断该用什么写法。
- **最后更新**：2026-10-31
- **状态**：稳定（工具链与 v1.7.0 一致；Room 表数按当前代码更新为 v6）

## 目录

- [技术栈](#技术栈)
- [版本兼容矩阵](#版本兼容矩阵)
- [构建与运行](#构建与运行)
- [本机环境](#本机环境)
- [目录与命名约定](#目录与命名约定)
- [编码规范要点](#编码规范要点)
- [依赖管理与升级](#依赖管理与升级)
- [环境变量与签名](#环境变量与签名)
- [版本与发布](#版本与发布)
- [历史记录](#历史记录)

## 技术栈

| 层 | 技术 | 版本 | 用途 | 备注 |
| --- | --- | --- | --- | --- |
| 语言 | Kotlin | 2.2.21 | 全项目 | `kotlin.code.style=official` |
| 构建 | Gradle Wrapper | 9.5.1 | 构建 | `gradle/wrapper/gradle-wrapper.properties`；AGP 8.13 要求 ≥8.13，本轮迁移未动 |
| 构建 | AGP | 8.13.2 | Android Gradle Plugin | 8.x 最后一条稳定线；2026-10-31 由 8.7.3 迁入，原生支持 compileSdk 36（9.x 不动：会删 `applicationVariants`/`BaseVariantOutputImpl`） |
| 构建 | KSP | 2.2.21-2.0.5 | Hilt/Room 注解处理 | 精确匹配 Kotlin 2.2.21 |
| JDK | Android Studio JBR | 21.0.10 | Gradle 运行时 | 编译目标 Java 17 |
| UI | Jetpack Compose BOM | 2025.11.01 | 声明式 UI | 解析出 ui 1.9.5 / material3 1.4.0 / icons 1.7.8（版本取自该 BOM 的 pom，不是试错出来的） |
| UI | Material 3 | 1.4.0（随 BOM） | MD3 组件 | `material3`；`material-icons-extended` 被 BOM 钉在 1.7.8——上游 2025-02 起停更，不是我们落后 |
| 导航 | Compose Navigation | 2.8.5 | 已引入 | **当前代码未使用 `NavHost`** |
| 状态 | Lifecycle / ViewModel | 2.8.7 | `StateFlow` + `collectAsState` | `lifecycle-runtime-compose` |
| DI | Hilt | 2.57.2 | 全应用注入 | `hilt-navigation-compose` 1.2.0 |
| 数据库 | Room | 2.7.2 | 课程/考试/成绩/想看缓存 | v6，`exportSchema=false` |
| 网络 | Retrofit | 3.0.0 | 教务/GitHub 接口 | Gson converter |
| 网络 | OkHttp | 5.3.2 | HTTP 引擎 | CookieJar + AuthInterceptor |
| 网络 | Jsoup | 1.22.2 | 解析登录页/错误提示 | `csrftoken`、`#tips` |
| 存储 | DataStore Preferences | 1.1.1 | 偏好/节次/首页均分 | 跨进程可读 |
| 安全 | security-crypto | 1.1.0-alpha06 | 加密凭证 | `EncryptedSharedPreferences` |
| 图片 | Coil | 2.7.0 | 图片加载 | `coil-compose`；图书馆封面走 `AsyncImage` |
| 条码/二维码 | ZXing | 3.5.3 | 图书馆借书码 | `zxing-core`，`ui/library/ReaderCodeImage.kt` 自行渲染 |
| 图书馆接口 | 汇文 OPAC | — | `https://hwdw.hebtu.edu.cn/meta-local/opac/` | 独立 `@Named("opac")` client，共用 CookieJar，见 [architecture/library-and-sso.md](architecture/library-and-sso.md) |
| Widget | Glance | 1.2.0 | 桌面课表 | 3 种尺寸；与 Compose 1.9 同代（1.1.1 配 1.9 runtime 会运行时 `NoSuchMethodError`） |
| 后台 | WorkManager | 2.10.0 | 上课/考试提醒、Widget 刷新 | 提醒用周期任务；Widget 用一次性 tick 链（`WidgetTickWorker`），见 [architecture/widget-and-reminders.md](architecture/widget-and-reminders.md) |
| 协程 | kotlinx-coroutines | 1.11.0 | 异步/Flow | `coroutines-android` |
| 测试 | JUnit | 4.13.2 | 单元测试 | 3 组 JVM 单测：`LoginPageDetectorTest`、`ZfsoftRsaTest`、`WidgetScheduleLogicTest` |
| 资源 | Android SDK | compile 36 / target 35，min 31 | API 级别 | compileSdk 36 是 Compose 1.9 的要求；targetSdk 留 35（36 会强制 edge-to-edge，属独立改造）；16KB Page 对齐开启 |

> 版本目录中还有 `vico 2.0.0-beta.2`、`colorpicker-compose 1.2.0` 等条目，但 `app/build.gradle.kts` 未引用；成绩趋势图实际用 Canvas 手绘。

## 版本兼容矩阵

```text
AGP 8.13.2  ←→  Gradle 9.5.1  ←→  Kotlin 2.2.21  ←→  KSP 2.2.21-2.0.5
                    ↕
  Compose BOM 2025.11.01（ui 1.9.5 / m3 1.4.0 / icons 1.7.8）  ←→  Glance 1.2.0
                    ↕
              Hilt 2.57.2  ←→  Room 2.7.2  ←→  compileSdk 36
```

关键约束（历史踩坑，升级前必读）：

- Hilt 2.59.x 要求 AGP 9.0+，而阿里云镜像没有 AGP 9.x 稳定版 → 停在 2.57.2。
- KSP 版本格式为 `X.Y.Z`（如 `2.2.21-2.0.5`）；必须与 Kotlin 精确匹配。
- Room 2.6.x 处理 Kotlin 2.2.x 的 `? super Continuation` 签名会崩溃 → 需 ≥2.7.0。
- Kotlin ≥2.2.0 才能读取 OkHttp 5.3.2 / Coroutines 1.11.0 的 metadata 2.2.0。
- compileSdk 由 Compose 的 AAR 元数据（`META-INF/.../aar-metadata.properties` 里的 `minCompileSdk`）决定，不是拍脑袋：Compose 1.9 要 36。AGP 8.13.2 支持 36，`platforms/android-36` 缺失时 AGP 会自动下载（本机是从 android-35/36.1 自动补的）。**升级前先读 AAR 元数据，不要等构建报错。**
- 小米 15 / Android 16 的 16KB Page 需要 `android.experimental.enable16KbPageAlignment=true`。
- 阿里云 Maven 镜像缺部分最新版 → 不能盲目追新。

## 构建与运行

在项目根目录（`D:\Personal_file\VibeCoding\Program\My-University`）执行：

| 目的 | 命令 | 产物/说明 | 最近结果 |
| --- | --- | --- | --- |
| 查看工具链 | `.\gradlew.bat --version` | Gradle/JVM 版本 | 2026-10-21 通过：Gradle 9.5.1，Launcher JVM 21.0.10（Android Studio JBR） |
| Debug 构建 | `.\gradlew.bat assembleDebug` | `app/build/outputs/apk/debug/MyHEBNU-v1.7.0-debug.apk`；包名 `com.myhebnu.debug`（另一个 `com.myhebnu` 是 release 包，两个包在真机上并存） | **2026-10-31 11:37 通过**（Compose 1.9 迁移版，77,042,315 B，已 `adb install -r` 到 `db072f50`）；上一版 2026-10-30 21:54 亦通过 |
| Release 构建 | `.\gradlew.bat assembleRelease` | `app/build/outputs/apk/release/MyHEBNU-v1.7.0-release.apk`；包名 `com.myhebnu`；需签名（R8 + shrinkResources） | **2026-10-28 通过**：7,933,712 B，versionCode 9，`apksigner verify` 通过 |
| 发版 | `gh release create v<版本> <apk> --target <分支> --title … --notes-file …` | GitHub Release；`releases/latest` 是应用内更新检查的数据源 | 2026-10-28 发布 v1.7.0，见「版本与发布」 |
| 仅编译 Kotlin | `.\gradlew.bat compileDebugKotlin` | 快速语法/类型检查 | 通过（2026-10-21 随 `assembleDebug` 执行） |
| 单元测试 | `.\gradlew.bat testDebugUnitTest` | JVM 单测 | **2026-10-28 通过**：`LoginPageDetectorTest`、`WidgetScheduleLogicTest`、`ZfsoftRsaTest` 全部通过 |
| 静态检查 | `.\gradlew.bat lintDebug` | Lint 报告 | 未执行 |
| 安装 Debug | `adb install -r app\build\outputs\apk\debug\MyHEBNU-v1.7.0-debug.apk` | 需连接设备 | **2026-10-30 21:54 通过**：24129PN74C（`db072f50`），`Success`；debug 包名 `com.myhebnu.debug`（应用名「MyHEBNU D」） |
| 应用日志 | `adb logcat -s MyHEBNU:*` | 主要链路日志 | 未执行 |
| Widget 日志 | `adb logcat -s MyHEBNU-Widget:*` | Widget 进程日志 | 未执行 |

> **构建约定**：2026-10-21 用户在会话中要求直接构建，`assembleDebug` 已由代理执行并通过（含 `clean` 全量构建）。后续默认仍建议由用户在 Android Studio 做真机验收；代理在获得明确授权时可代跑 Gradle。上表“未执行”不代表命令不可用。
>
> **编译警告（2026-10-21 clean build）**：2 条 Kotlin 警告——`ScheduleViewModel.kt:266` 的 `flatMapLatest` 需要 `@OptIn(ExperimentalCoroutinesApi::class)`；`WidgetUpdateManager.kt:17` 的 `@ApplicationContext` 建议写成 `@param:ApplicationContext`。另有 2 个 native 库无法 strip（正常警告）。不影响构建，待清理。

## 本机环境

| 项 | 值/说明 |
| --- | --- |
| 操作系统 | Windows（PowerShell 7） |
| 项目路径 | `D:\Personal_file\VibeCoding\Program\My-University` |
| Git remote | `https://github.com/cheeemmms/MyHEBNU.git`，工作分支 `main`（`feat/md3-ui-refresh` 已快进并入并停更） |
| Git 状态 | 2026-10-31：`main` 在 `8085d6b`（Compose 1.9 迁移 + 详情转场按录屏对齐），本地领先 `origin/main` 4 个提交（**未推送**）；迁移基线 tag `pre-compose19` = `dda8017`；v1.7.0 发版提交为 `673fed7`；`backup/2026-08-30` 备份分支指向 `797e0ef` |
| GitHub CLI | `gh 2.89.0`，已登录 `cheeemmms`；`git credential.helper=manager` |
| Android Studio（JAVA_HOME） | `C:\Program Files\Android\Android Studio`，JBR 21.0.10 |
| 另一处 Android Studio | `D:\Software\Android\bin\studio64.exe`（也存在） |
| Android SDK | `D:\Software_data\Android Studio\SDK`（`local.properties` 的 `sdk.dir`） |
| SDK 平台 | `android-35`、`android-36.1`；build-tools `34.0.0`、`36.0.0` |
| adb | `D:\Software_data\Android Studio\SDK\platform-tools\adb.exe` |
| Gradle 用户目录 | `GRADLE_USER_HOME=D:\Software_data\DevCache\gradle`（已有 9.5.1/9.7.1 发行版与依赖缓存） |
| 代理（项目配置） | `gradle.properties` 已改为 `127.0.0.1:7897`（HEAD 版本为 `7892`，工作区已对齐） |
| 代理（本机约定） | `127.0.0.1:7897`（HTTP/SOCKS5，Clash Verge） |
| 代理现状 | 2026-10-21 检查：`7897` 正在监听；项目已对齐该端口，联网构建可直接使用 |
| 沙箱限制 | DSH `workspace-write` 下 Gradle 无法写 `GRADLE_USER_HOME`；需要时用 `danger-full-access` 或由用户在本机终端构建 |

## 目录与命名约定

- 包名 `com.myhebnu`；debug 后缀 `.debug`。
- 源码：`app/src/main/java/com/myhebnu/{data,domain,ui,widget,worker,di,util}`。
- UI 组件：页面放 `ui/<feature>/`，可复用子组件放 `ui/<feature>/components/` 或 `ui/components/`。
- 资源：字符串进 `strings.xml`；Widget 尺寸/颜色必须进 `dimens.xml`/`colors.xml`。
- 文件名：类名 `.kt`；Composable 文件用 PascalCase；多实体可放 `*Models.kt`。
- 版本号：`app/build.gradle.kts` 的 `versionCode/versionName` 是唯一来源；发布时同步 `CHANGELOG.md` 与 `strings.xml` 的 `changelog_content`。

## 编码规范要点

- 只写会影响实现选择的规则；格式化交给 Kotlin 官方风格。
- **状态**：ViewModel 持有 `MutableStateFlow`，对外暴露不可变 `StateFlow<UiState>`；更新用 `_uiState.update { ... }`。
- **错误**：Repository 返回 `Result<T>`；不要跨层抛异常；ViewModel 负责转成 UI 文案。
- **Compose**：状态上提；不要嵌套垂直滚动容器（见 [diagnostics/compose-nested-scroll.md](diagnostics/compose-nested-scroll.md)）。
- **选择器**：仅用 M3 原生控件；禁止自研 iOS 滚轮。
- **文案**：新增用户可见文案走 `stringResource`，不要继续增加硬编码中文；全量英文已搁置，恢复时同步 `values-en`。
- **状态表达**：加载用 `SkeletonBox` 或进度指示；空态给明确文案；错误给可重试提示，不把裸 `HTTP xxx` 直接抛给用户。
- **数据**：学期码用 `3/12/16` 常量；校区读 `preferences.campusId`；列表接口显式传 `queryModel.showCount`（考试接口当前未传，用户实测正常，暂不改）。
- **日志**：TAG 用 `MyHEBNU`，Widget 用 `MyHEBNU-Widget`；不要打印密码/完整 Cookie；发布构建应关闭 BODY 日志。
- **Git**：Conventional Commits（`feat:`/`fix:`/`chore:`/`docs:`）；发布提交同时 bump 版本与 CHANGELOG；推送前确认工作区无敏感文件。
- **测试**：新增纯逻辑（GPA、周次解析、日期）时优先补 JVM 单测；当前无测试是已知缺口。

## 依赖管理与升级

- 包管理：Gradle Version Catalog（`gradle/libs.versions.toml`）；插件别名在根 `build.gradle.kts`。
- 仓库顺序：阿里云镜像优先，`google()`/`mavenCentral()` 兜底；`FAIL_ON_PROJECT_REPOS`。
- 升级流程：改 catalog → `compileDebugKotlin` → 真机验收 → 更新本文件「版本兼容矩阵」与「历史记录」。
- 不要同时升级 AGP/Kotlin/KSP/Hilt/Room；一次只动一个，失败时容易定位。
- 已知不可用组合见上；新增依赖前先确认阿里云镜像有对应版本。
- 锁文件：无 dependency lock；依赖解析依赖缓存与镜像。需要可复现构建时可考虑启用 Gradle dependency verification/lock。

## 环境变量与签名

| 名称 | 用途 | 必需 | 存放位置 |
| --- | --- | --- | --- |
| `sdk.dir` | Android SDK 路径 | 是 | `local.properties`（已 gitignore） |
| `KEYSTORE_PATH` | Release 签名文件路径 | Release 构建必需 | `local.properties`（已 gitignore） |
| `KEYSTORE_PASSWORD` | Keystore 密码 | Release 构建必需 | `local.properties`（已 gitignore） |
| `KEY_ALIAS` | 签名别名 | Release 构建必需 | `local.properties`（已 gitignore） |
| `KEY_PASSWORD` | 别名密码 | Release 构建必需 | `local.properties`（已 gitignore） |
| `GRADLE_USER_HOME` | Gradle 缓存目录 | 否 | 本机环境变量（`D:\Software_data\DevCache\gradle`） |
| `JAVA_HOME` | JDK | 是 | `C:\Program Files\Android\Android Studio\jbr` |

> 密钥与密码**不得**写入记忆库、README、提交或聊天记录；`local.properties` 已被 `.gitignore` 排除。`app/build.gradle.kts` 从 `local.properties` 读取签名配置。

## 版本与发布

发版三处必须一起改，漏一处就会出现「应用内版本号与 Release 不一致」：

1. `app/build.gradle.kts` 的 `versionCode`（+1）与 `versionName`；
2. `CHANGELOG.md`（面向开发者的发布记录）与仓库根 `README.md` 的版本徽章/版本历史表；
3. 应用内更新日志 `app/src/main/res/values/strings.xml` 的 `changelog_content`——**只保留当前版本**，旧版本条目不再累积。

发布流程（已走通一次，v1.7.0）：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleRelease
# 产物：app/build/outputs/apk/release/MyHEBNU-<版本>-release.apk（signingConfigs.release + R8 + shrinkResources）

git add -A
git commit -m "feat(...): ...（v<版本>）"
git push origin HEAD

gh release create v<版本> app/build/outputs/apk/release/MyHEBNU-<版本>-release.apk `
  --target <分支> --title "MyHEBNU v<版本>" --notes-file <notes.md>
```

- 应用内「检查更新」读 `GET /repos/cheeemmms/MyHEBNU/releases/latest`，所以 release **必须是正式版**（不能勾「预发布」）。
- 校验方式：`aapt2 dump badging <apk>` 看 `versionCode/versionName/package`，`apksigner verify --print-certs` 看签名，`gh api repos/cheeemmms/MyHEBNU/releases/latest` 看 latest 指向与资产大小。
- tag 打在**功能提交**上（v1.7.0 = `673fed7`），随后的文档提交不重新打 tag。

## 历史记录

| 日期 | 变更 |
| --- | --- |
| 2026-06-05 | 锁定 AGP 8.7.3 / Kotlin 2.2.21 / KSP 2.2.21-2.0.5 / Hilt 2.57.2 / Room 2.7.2 组合；Gradle 9.5.1 |
| 2026-06-09 | Glance 1.1.1 Widget 全量改为资源引用（`R.dimen`/`R.color`） |
| 2026-06-10 | 引入 `values-night/colors.xml`，Widget 深色模式 |
| 2026-09-13 | v1.5.0：新增 Room 成绩表（DB v3→v4）、`RefreshFab`；版本 1.4.0→1.5.0 |
| 2026-10-21 | 本文件按 memory_bank 规范创建；记录当前工具链、代理与沙箱限制；标注未使用依赖 |
| 2026-10-28 | v1.7.0：补 ZXing 3.5.3 与汇文 OPAC 依赖行；构建/安装/单测结果更新到 v1.7.0；新增「版本与发布」小节（三处版本号 + gh release 流程）；工作分支改为 `feat/md3-ui-refresh` |
| 2026-10-30 | 记忆库整理：Room v4→v6（含 `wishlist` 表）、测试行改为 3 组 JVM 单测（原来误写“无测试源码”）、工作分支改回 `main`；编码规范补「状态表达」一条 |
