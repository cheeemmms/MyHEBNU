# handover-notes.md · 交接笔记

- **用途**：让没有上下文的新会话快速上手 MyHEBNU，知道代码在哪、哪些结论已验证、哪些仍是推测。
- **阅读时机**：刚接手、长时间未参与、准备交接给他人或其他会话。
- **最后更新**：2026-10-30
- **状态**：稳定（v1.7.0 已发布；main 在其上新增「想看」闭环与详情页重设计，未发版）

## 目录

- [项目来源与当前状态](#项目来源与当前状态)
- [五分钟上手路径](#五分钟上手路径)
- [关键文件地图](#关键文件地图)
- [构建、运行与调试](#构建运行与调试)
- [工作流约定](#工作流约定)
- [与上游/原版的差异](#与上游原版的差异)
- [未验证的假设与风险](#未验证的假设与风险)
- [待办与坑](#待办与坑)
- [交接检查清单](#交接检查清单)
- [历史记录](#历史记录)

## 项目来源与当前状态

| 项 | 内容 |
| --- | --- |
| 来源 | 自有仓库 `https://github.com/cheeemmms/MyHEBNU.git`，非 fork |
| 分支/提交 | **工作分支 `main`**（2026-10-30 `feat/md3-ui-refresh` 已快进并入并停更）；本轮维护 HEAD = `31c914e`，远端同步 |
| 历史规模 | 2026-06-04 起，110+ 次提交；`backup/2026-08-30`（`797e0ef`）保留一次回退前的未提交改动 |
| 当前版本 | **v1.7.0**（versionCode 9，2026-10-28 发布）；main 在其上已积累一整批未发版功能（见下） |
| v1.7.0 后新功能 | **「想看」闭环**（Room v6 wishlist → 详情页胶囊 → 列表页 → 首页入口/指标 → JSON 导出导入）；**详情页重设计**（沉浸头图 + 视差 + 滚动模糊淡出 + 状态栏反色）；**动画定案**（容器变换保留默认实现且最受重视；检索路径不走共享元素；内容动画用 M3 emphasized 曲线） |
| 当前可用程度 | **能构建、已上真机**：以上功能全部在 24129PN74C 真机验收通过 |
| 最近一次验证 | 2026-10-30 多轮 `assembleDebug` + `adb install -r`；`dumpsys gfxinfo` 实测确认首次进入页面的掉帧为首帧组合固有成本，与转场配置无关 |
| 工作区状态 | 代码与记忆库均已提交推送 |

## 五分钟上手路径

1. 读 [readme.md](readme.md) 的「当前快照」与「文件清单」，确认版本与下一步。
2. 读本文件「关键文件地图」，定位要改的模块；不要通读整个 `memory_bank/`。
3. 用 Android Studio 打开项目根目录，确认 `local.properties` 中 `sdk.dir` 与签名四项存在（文件已 gitignore，不会随仓库走）。
4. 核对本机代理 `127.0.0.1:7897`（`gradle.properties` 已对齐）。
5. 执行 `gradlew assembleDebug`，装到 Android 12+ 真机（当前验证机 24129PN74C；小米 15 / Android 16 是历史主要验证机）。
6. 首次启动：欢迎页 → 登录（学号/密码，可能带验证码）→ 首页五卡（下一节课/空教室/下一场考试/成绩/我的图书馆）。
7. 改完代码后按 [tech-stack.md](tech-stack.md) 的命令自测，并把构建/真机结果写回 [implementation-plan.md](implementation-plan.md) 的验证表。

## 关键文件地图

| 想改什么 | 去哪里改 | 注意点 |
| --- | --- | --- |
| 版本号/包名/签名 | `app/build.gradle.kts` | 同步 `CHANGELOG.md`、`strings.xml` 的 `changelog_content`、Git tag；三处版本号流程见 [tech-stack.md](tech-stack.md#版本与发布) |
| 入口/导航/会话门控 | `MainActivity.kt`、`ui/navigation/TopLevelRoute.kt` | 手写 backStack，不是 NavHost；启动只做本地判定 |
| 登录（CAS 单点） | `data/remote/sso/SsoLoginManager.kt`、`ZfsoftRsa.kt`、`data/repository/AuthRepository.kt` | 密码不是 PKCS#1；`execution` 与会话绑定；旧的 xtgl 直登已删除，不要复活。详见 [architecture/library-and-sso.md](architecture/library-and-sso.md#统一身份认证cas) |
| 我的图书馆 | `ui/library/`、`data/repository/OpacRepository.kt`、`data/remote/opac/`、`di/OpacModule.kt` | 只读 7 页；独立 OkHttp（共用 CookieJar）；返回阶梯与设计约定见 [architecture/library-and-sso.md](architecture/library-and-sso.md) |
| 想看功能 | `ui/library/Wishlist{Screen,ViewModel}.kt`、`data/repository/{WishlistRepository,WishlistBackup}.kt`、`data/local/db/{entity/WishlistEntity,dao/WishlistDao}.kt`、`ui/library/BookCovers.kt` | Room v6；封面缓存键分层（小图/大图）；备份 JSON 带 version；详见 [architecture/data-and-storage.md](architecture/data-and-storage.md) |
| 书籍详情页 | `ui/library/BibDetail{Screen,ViewModel,Header}.kt`、`HoldingCard.kt` | 沉浸头图 + 视差 + 滚动过渡；**换书闸门**：uiState.bibId 必须与路由 bibId 比对，否则封面/缓存串号；不使用 ScreenScaffold |
| 课表逻辑 | `ui/schedule/ScheduleViewModel.kt`、`data/repository/ScheduleRepository.kt` | 881 行；改前先看 `_currentYear/_currentTerm` vs `_viewYear/_viewTerm`；登录页判定不要用裸 `contains("登录")` |
| 课表 UI | `ui/schedule/components/WeekViewGrid.kt`、`CourseCard.kt`、`WeekSelector.kt` | 节次列宽 5 列 36dp / 7 列 28dp；卡片高度由父层传入 |
| 成绩 | `data/repository/GradeRepository.kt`、`ui/grade/GradeViewModel.kt`、`GpaCard.kt` | 成绩页只观察 Room；网络刷新写库；GPA 公式在 `GpaCalculator` |
| 空教室 | `data/repository/RoomRepository.kt`、`ui/room/RoomViewModel.kt`、`FilterPanel.kt`、`RoomList.kt` | 三步门控 + `queryModel.showCount` + 分页 |
| 考试 | `data/repository/ExamRepository.kt`、`ui/exam/` | 每次进页联网；分页项用户实测正常，暂不修改 |
| 首页 | `ui/home/HomeViewModel.kt`、`HomeScreen.kt`、`components/HomeCardPanel.kt` | 首页纯本地；卡片逐张挂共享元素 `home-card-<route>`；目标页必须接 `sharedScope`/`animatedScope` |
| 设计系统 | `ui/design/Components.kt`、`DesignTokens.kt`、`ui/theme/{Theme,Color,Shapes,SemanticColors}.kt` | 颜色/形状/间距只用令牌；成功色走 `LocalSemanticColors`；骨架屏 `SkeletonBox` |
| 数据库 | `data/local/db/AppDatabase.kt`、`entity/`、`dao/`、`migrations/Migrations.kt` | 改表必须写 Migration；**v6** 当前 5 张表（courses/exams/grades/wishlist + 索引） |
| DataStore | `data/local/preferences/UserPreferences.kt` | 新增键要提供默认值；跨进程读取要能被 Widget 使用 |
| Widget | `widget/ScheduleWidgetData.kt`、`widget/Schedule*Widget.kt`、`res/xml/schedule_widget_*.xml` | 尺寸/颜色/圆角一律用 `R.dimen`/`R.color`；见 [diagnostics/glance-miui-resources.md](diagnostics/glance-miui-resources.md) |
| 提醒 | `MyHebnuApplication.kt`、`worker/ReminderManager.kt`、`worker/*Worker.kt` | 周期任务唯一名不要改；异常要消化为 `Result.success()` |
| 更新检查/发版 | `data/repository/UpdateRepository.kt`、`ui/about/SystemUpdateScreen.kt` | 读 GitHub `releases/latest`；release 必须是正式版 |

## 构建、运行与调试

| 目的 | 命令/操作 | 说明 |
| --- | --- | --- |
| 构建 Debug | `.\gradlew.bat assembleDebug` | 产物 `app/build/outputs/apk/debug/MyHEBNU-<版本>-debug.apk` |
| 构建 Release | `.\gradlew.bat assembleRelease` | 需 `local.properties` 签名四项；R8 + shrinkResources |
| 快速编译检查 | `.\gradlew.bat compileDebugKotlin` | 不含打包，最快发现 Kotlin 错误 |
| 单元测试 | `.\gradlew.bat testDebugUnitTest` | 已有 `LoginPageDetectorTest`、`WidgetScheduleLogicTest`、`ZfsoftRsaTest` 等 |
| Lint | `.\gradlew.bat lintDebug` | 报告在 `app/build/reports/lint-results-debug.html` |
| 安装 | `adb install -r <apk>` | debug 包名 `com.myhebnu.debug`，可与正式版共存 |
| 应用日志 | `adb logcat -s MyHEBNU:*` | 登录/课表/成绩/考试均有 `MyHEBNU` 日志 |
| 图书馆日志 | `adb logcat -s SsoLogin:* OpacAuth:*` | CAS 逐跳与图书馆会话 |
| Widget 日志 | `adb logcat -s MyHEBNU-Widget:*` | Widget 独立进程 |
| 崩溃日志 | `adb logcat`（**不要加 tag 过滤**） | 需包含 `AndroidRuntime`；空教室闪退曾因过滤过严误诊 |
| 清除数据 | `adb shell pm clear com.myhebnu` | 会清课表/成绩/登录态；仅复现首次启动时用 |
| 截图 | `adb shell screencap -p /sdcard/x.png` 再 `adb pull` | `exec-out > x.png` 会写出损坏文件；屏幕超时会截全黑，先 `KEYCODE_WAKEUP` |

### 调试顺序建议

1. 先看崩溃堆栈（`AndroidRuntime`），再猜原因。
2. 请求失败先看 `AuthInterceptor` 日志：Cookie 是否携带、响应码是否 302/401/403/901；图书馆问题看 `SsoLogin`/`OpacAuth`。
3. 数据不显示先确认：Room 是否有数据 → DataStore 学期/周次是否正确 → 是否被 HTML 守卫当错误。
4. Widget 异常先看是否用了裸 Int（应全部资源化），再看 DataStore `period_times_json`。
5. 图书馆返回越级 → 查 `SearchScreen` 的 `handleBack`/`hasSearchState`，不要用 `DisposableEffect(onDispose)` 清检索态。

## 工作流约定

以下为项目既有约定（来自上一阶段协作记录与根目录 `AGENTS.md`），除非用户明确改变，否则沿用：

1. **任务一个一个来**：先出计划 → 用户确认 → 实施 → 构建 + 用户真机验收 → 通过后再推送。
2. **编码授权**：用户明确说「开始实现/按这个方案改/动手吧」才算授权；疑问句只当诊断请求。
3. **构建与验收**：2026-10-30 起的分工——代理可代跑 `gradlew` 与 `adb install -r` 推送到真机，**验收由用户完成**（代理不再代为截图验收）。
4. **回滚类指令先确认范围**：曾因把「回滚某条路径的动画」误读为整体回退，险些误伤用户最重视的容器变换动画；动手前必须确认回滚对象与粒度。
5. **推送**：`gh` 已登录 `cheeemmms`；如 git 凭据失效，用 `gh auth setup-git` 注册 credential helper。
6. **发布**：bump `versionCode`/`versionName` → 同步 `CHANGELOG.md` 与 `strings.xml` 的 `changelog_content`（只留当前版本）→ `assembleRelease` → 校验 `aapt2 badging` / `apksigner` → commit + push → `gh release create`。完整命令见 [tech-stack.md](tech-stack.md#版本与发布)。
7. **记忆库**：有实质改动后刷新 [readme.md](readme.md) 的当前快照与受影响文件；详细规则见 readme「维护约定」。

## 与上游/原版的差异

- 本项目是自有仓库，不存在上游 fork；无需同步上游。
- `backup/2026-08-30`（`797e0ef`）是 2026-08-30 一次 `git reset --hard v1.3.0` 前对未提交改动的备份分支。后续 main 已继续前进，**不要删除该分支**，直到确认不再需要追溯。
- 历史 `v1.3.0` 标签与名为 “bump to v1.3.0” 的提交不是同一个对象；做任何 reset/checkout 前用 `git log --oneline --decorate v1.3.0` 核对实际提交，避免再次误删改动。

## 未验证的假设与风险

| 项 | 状态 | 验证方法 |
| --- | --- | --- |
| 当前源码可构建 | **已验证**：2026-10-28 `assembleRelease`/`assembleDebug` 均通过 | 需要时重跑 `assembleDebug` |
| 本机代理可用 | **已验证**：7897 正在监听，项目已对齐 | `Test-NetConnection 127.0.0.1 -Port 7897` |
| 图书馆只读功能 | **已验证**（真机）：首页指标、检索、瀑布流、页码翻页、借书码、详情页 | 按 [implementation-plan.md](implementation-plan.md) 验证表逐项 |
| 图书馆 UI 最后一轮（骨架屏 + 220ms 卡片转场） | **部分验证**：已构建并装机，用户尚未给出反馈 | 用户真机点开「我的图书馆」看首载骨架与展开速度 |
| 启动耗时 | **部分验证**：2026-10-21 冷启动 `TotalTime 528ms`（无本地会话→登录页）；登录态首页路径待复测 | 见 [diagnostics/startup-latency.md](diagnostics/startup-latency.md) |
| 小学期成绩可查 | **待验证**：小学期未开始，接口/展示方式未知 | 用小学期账号验证或查教务网页 |
| i18n 可用 | **部分，已搁置**：仍有硬编码中文（图书馆全中文） | 切英文逐页检查 |
| 安全项 | **有风险**：明文 HTTP、Cookie 明文持久化、BODY 日志、备份暴露 | 见 [architecture/security.md](architecture/security.md) |
| README 与实际一致 | **基本一致**：版本徽章/变更日志/许可证/Widget 状态已更新；架构图与技术栈表仍有旧值 | 按 [implementation-plan.md](implementation-plan.md) 任务 16 修 |

> 已验证关闭：考试接口分页——用户实测未出现问题（2026-10-21），不再作为待办。

## 待办与坑

| # | 事项 | 类型 | 优先级 | 线索 |
| --- | --- | --- | --- | --- |
| 0 | **迁移 Compose 1.9**（BOM 2025.10.00 一档） | 待办 | **P0** | 解锁 `onVisibilityChanged`（瀑布流共享元素按可见性挂载）与 `Text(autoSize)`；前置：compileSdk 36、AGP/Lint 升级；已确认无自定义 Indication。详见 [implementation-plan.md](implementation-plan.md) |
| 1 | 图书馆写操作：续借 `POST users/renew` | 待办 | 高 | `data/remote/opac/OpacApi.kt`、`ui/library/Loans*` |
| 2 | 图书馆写操作：荐购提交 `users/requests?type=req.acq.suggest` | 待办 | 中 | `OpacRepository`、`ui/library/Requests*` |
| 3 | 图书馆写操作：预约/委托下单（接口未抓到） | 阻塞 | 中 | 需先抓包 `requests/item_copies` 之后的请求 |
| 4 | 想看数据无云端同步（仅本机 + 手动导出导入） | 设计决策 | — | 2026-10-30 管理员确认先做手动备份；如需自动同步另立项 |
| 5 | 发布构建关闭 BODY 日志并脱敏 Cookie 日志 | 安全·搁置 | — | `di/NetworkModule.kt`、`PersistentCookieJar.kt` |
| 6 | Cookie 持久化加密 | 安全·搁置 | — | `data/remote/SessionManager.kt` |
| 7 | 收紧 `network_security_config`，仅放行教务域名 | 安全·搁置 | — | `res/xml/network_security_config.xml` |
| 8 | 配置备份排除/关闭 `allowBackup` | 安全·搁置 | — | `AndroidManifest.xml` |
| 9 | 移除未使用的 `SCHEDULE_EXACT_ALARM` | 安全·搁置 | — | `AndroidManifest.xml` |
| 10 | 小学期 `16` 成绩接入调研 | 待办 | 中 | `GradeRepository.SEMESTER_TERMS` |
| 11 | 补测试/CI | 待办 | 中 | 已有 3 组 JVM 单测，无 CI |
| 12 | README 架构图/技术栈表同步 | 待办 | 中 | README 架构总览、技术栈表 |
| 13 | 课表刷新事务化 | 坑 | 中 | `ScheduleRepository.refreshSchedule` 删后插非事务 |
| 14 | 通知权限未引导 | 坑 | 中 | `ReminderManager` 捕获 `SecurityException` 后静默 |
| 15 | 自研滚轮禁用 | 约定 | 高 | 见 [design-document.md](design-document.md) UI/UX |
| 16 | Compose 嵌套滚动 | 坑 | 高 | 见 [diagnostics/compose-nested-scroll.md](diagnostics/compose-nested-scroll.md) |
| 17 | MIUI Widget 资源化 | 坑 | 高 | 见 [diagnostics/glance-miui-resources.md](diagnostics/glance-miui-resources.md) |
| 18 | 共享元素动画静默失效 | 坑 | 中 | 页面签名没接 `sharedScope`/`animatedScope` 时会退回淡入淡出，不报错 |
| 19 | 交错网格抖动 | 坑 | 中 | 封面高度必须在图片加载前定死（`coverRatioFor`），不要用 `onSuccess` 回写比例 |
| 20 | BlendMode 遮罩必须开离屏合成 | 坑 | 高 | `drawWithContent` 里用 `BlendMode.DstIn` 等时必须配 `graphicsLayer(compositingStrategy = Offscreen)`，否则与窗口背景混合整块变黑（2026-10-30 详情页实测） |
| 21 | 远程图片禁止无约束尺寸 | 坑 | 高 | `wrapContentHeight` 等无约束布局会让 Coil 推断不出目标尺寸、按原图解码；所有封面请求经 `bookCoverRequest` 显式给 size |
| 22 | 换书时 uiState 必须按 bibId 闸门 | 坑 | 高 | 详情页 ViewModel 是 Activity 级复用，换书首帧旧状态仍在；已用 `rawState.bibId == bibId` 闸门修复，改详情页时不可移除 |
| 23 | 容器变换动画已定案 | 约定 | 高 | sharedBounds 整页展开 + 默认 enter/exit/resizeMode 就是最受重视的动画；fade through / RemeasureToBounds / 封面 sharedElement 三个实验均已否决（2026-10-30），不要重新引入 |

> 安全项 2026-10-21 用户决定**暂时搁置**，保留记录不实施；恢复优先级见 [architecture/security.md](architecture/security.md)。

## 交接检查清单

- [ ] 已读 [readme.md](readme.md) 与本文
- [ ] 已确认 `local.properties`（SDK/签名）存在且未提交
- [ ] 已启动代理并确认端口 7897（项目已对齐）
- [ ] Debug 构建通过
- [ ] 关键路径手动验证：登录 → 首页 → 课表 → 成绩 → 空教室 → 考试
- [ ] 图书馆路径手动验证：首页指标 → 检索（瀑布流/翻页/封面）→ 书目详情（含滚动过渡）→ 当前借阅 → 借书码
- [ ] 想看路径手动验证：详情页收藏 → 首页指标/入口 → 列表 → 导出 → 清空 → 导入 → 幂等重复导入
- [ ] Widget/提醒至少各验证一次（真机）
- [ ] [tech-stack.md](tech-stack.md) 中的命令可复现
- [ ] 未验证项已明确标注，未把推测写成结论
- [ ] 改动已刷新 [readme.md](readme.md) 快照与受影响文件

## 历史记录

- **2026-10-30（晚）**：随「想看」功能闭环与详情页重设计同步——当前状态改为 main 工作线、HEAD `31c914e`；文件地图新增想看与书籍详情页两行、数据库更新为 v6；工作流约定补「验收由用户做」与「回滚指令先确认范围」；待办新增 P0 Compose 1.9 迁移与四个新坑（BlendMode 离屏、无约束图片尺寸、换书闸门、容器变换定案）；交接清单补想看路径。

- **2026-10-28**：记忆库随 v1.7.0 发版同步——原 `handover-library-sso.md`（一次性交接文档）的长期内容并入 [architecture/library-and-sso.md](architecture/library-and-sso.md)，未完成项并入 [implementation-plan.md](implementation-plan.md)，该文件删除；本文件刷新当前状态、文件地图（图书馆/CAS/设计系统）、发版流程与待办；`ea-api.md` 的登录章节从旧的 xtgl 表单直登改为 CAS 单点登录。
- **2026-10-21**：按 memory_bank 规范创建本文件。项目此前没有独立交接文档，状态散落在 `progress.md` 与 README；本次把“来源/上手/地图/约定/风险/待办”集中到此，`progress.md` 的变更过程归档到 [history/change-log.md](history/change-log.md)。同日对齐代理 7897、统一许可证 AGPL-3.0、关闭考试分页、把小学期成绩改为待验证、i18n 搁置、安全项搁置（保留 [architecture/security.md](architecture/security.md)）、修正课表长按发布说明，并跑通 `assembleDebug`；启动改为本地判定并移除启动网络校验（[startup-latency.md](diagnostics/startup-latency.md)）；修复课表 Session 误报（[schedule-login-false-positive.md](diagnostics/schedule-login-false-positive.md)）。
- **2026-08-30**：执行过 `git reset --hard v1.3.0` 回退；未提交改动保存在 `backup/2026-08-30`。后续 v1.3.1/v1.4.0/v1.5.0 已重新前进。
